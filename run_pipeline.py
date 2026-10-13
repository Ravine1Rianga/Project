"""
run_pipeline.py
===============
End-to-end execution pipeline for:
"A Machine Learning Approach for Predicting Developmental Delays Associated
with Malnutrition Among Children Under Five in Northern Kenya"

Pipeline Workflow:
1. Data Ingestion & Splitting (Stratified 80/20 train/test split)
2. Data Preprocessing (Winsorization + MICE Imputation via DataCleaner)
3. Feature Engineering (Nutrition composite scores, clinical interactions via FeatureEngineer)
4. Stratified 5-Fold Cross Validation Benchmarking
5. Ensemble Training (Random Forest + XGBoost + LightGBM + CatBoost with soft-voting)
6. Comparative evaluation vs Baseline Logistic Regression
7. Probability Calibration via CalibrationModule (Isotonic Regression)
8. Explainability via SHAPExplainer (Global summary + Local CHW plain-language explanations)
9. Artifact serialization & Report generation (ROC curves, calibration curves, metrics JSON)
"""

import os
import json
import joblib
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from sklearn.model_selection import train_test_split, StratifiedKFold
from sklearn.metrics import roc_curve, auc, brier_score_loss
from sklearn.calibration import calibration_curve

# Import local OOAD modules
from src.dataset_generator import generate_asal_pediatric_dataset
from src.data_cleaner import DataCleaner
from src.feature_engineer import FeatureEngineer
from src.ensemble_model import BaselineClassifier, EnsembleClassifier
from src.calibration import CalibrationModule
from src.explainability import SHAPExplainer

def run_end_to_end_pipeline():
    print("=" * 80)
    print("STARTING MACHINE LEARNING PIPELINE: ASAL PEDIATRIC DEVELOPMENTAL DELAY")
    print("=" * 80)

    # 1. DATA INGESTION
    raw_data_path = "data/raw/asal_child_development_dataset.csv"
    if not os.path.exists(raw_data_path):
        print("[1/8] Generating raw ASAL pediatric cohort dataset...")
        df_raw = generate_asal_pediatric_dataset(n_samples=4000, seed=42, output_path=raw_data_path)
    else:
        print(f"[1/8] Loading existing dataset from {raw_data_path}...")
        df_raw = pd.read_csv(raw_data_path)

    target_col = "developmental_delay_risk"
    print(f"Total cohort size: {len(df_raw)} records")
    print(f"Prevalence of developmental delay: {df_raw[target_col].mean()*100:.1f}%")

    # 2. STRATIFIED TRAIN / TEST SPLIT (80% Train, 20% Held-Out Test)
    print("\n[2/8] Creating Stratified 80/20 Train/Test split...")
    df_train, df_test = train_test_split(
        df_raw,
        test_size=0.20,
        random_state=42,
        stratify=df_raw[target_col]
    )
    df_train = df_train.reset_index(drop=True)
    df_test = df_test.reset_index(drop=True)
    y_train = df_train[target_col].values
    y_test = df_test[target_col].values
    print(f"Train set: {len(df_train)} samples | Held-out Test set: {len(df_test)} samples")

    # 3. DATA CLEANING & MICE IMPUTATION (Fit on train, transform both)
    print("\n[3/8] Applying Winsorization & MICE Imputation via DataCleaner...")
    cleaner = DataCleaner(random_state=42)
    cleaner.fit(df_train)
    df_train_clean = cleaner.transform(df_train)
    df_test_clean = cleaner.transform(df_test)

    # 4. FEATURE ENGINEERING & SCALING (Fit on train, transform both)
    print("\n[4/8] Deriving nutrition indicators & interaction terms via FeatureEngineer...")
    engineer = FeatureEngineer(scale_features=True)
    X_train_df = engineer.fit_transform(df_train_clean)
    X_test_df = engineer.transform(df_test_clean)

    X_train = X_train_df.values
    X_test = X_test_df.values
    feature_names = engineer.feature_columns
    print(f"Final feature dimension: {len(feature_names)} engineered features")

    # Save processed datasets
    os.makedirs("data/processed", exist_ok=True)
    X_train_df.assign(target=y_train).to_csv("data/processed/train_features.csv", index=False)
    X_test_df.assign(target=y_test).to_csv("data/processed/test_features.csv", index=False)

    # 5. STRATIFIED 5-FOLD CROSS VALIDATION
    print("\n[5/8] Running Stratified 5-Fold Cross-Validation on training set...")
    cv = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
    
    cv_models = {
        "Baseline (Logistic Reg)": BaselineClassifier(random_state=42),
        "Random Forest": EnsembleClassifier(random_state=42).models["RandomForest"],
        "XGBoost": EnsembleClassifier(random_state=42).models["XGBoost"],
        "LightGBM": EnsembleClassifier(random_state=42).models["LightGBM"],
        "CatBoost": EnsembleClassifier(random_state=42).models["CatBoost"],
        "Soft-Voting Ensemble": EnsembleClassifier(random_state=42),
    }

    cv_results = {}
    for name, model in cv_models.items():
        fold_aucs, fold_recalls, fold_specs = [], [], []
        for train_idx, val_idx in cv.split(X_train, y_train):
            X_tr, y_tr = X_train[train_idx], y_train[train_idx]
            X_val, y_val = X_train[val_idx], y_train[val_idx]

            # Fit and evaluate fold
            if name == "Soft-Voting Ensemble":
                fold_model = EnsembleClassifier(random_state=42)
                fold_model.fit(X_tr, y_tr)
                probs = fold_model.predict_proba(X_val)[:, 1]
            elif name == "Baseline (Logistic Reg)":
                fold_model = BaselineClassifier(random_state=42)
                fold_model.fit(X_tr, y_tr)
                probs = fold_model.predict_proba(X_val)[:, 1]
            else:
                fold_model = model.__class__(**model.get_params())
                fold_model.fit(X_tr, y_tr)
                probs = fold_model.predict_proba(X_val)[:, 1]

            fold_aucs.append(auc(*roc_curve(y_val, probs)[:2]))
            preds = (probs >= 0.50).astype(int)
            recall = np.sum((y_val == 1) & (preds == 1)) / max(1, np.sum(y_val == 1))
            spec = np.sum((y_val == 0) & (preds == 0)) / max(1, np.sum(y_val == 0))
            fold_recalls.append(recall)
            fold_specs.append(spec)

        cv_results[name] = {
            "mean_auc": round(float(np.mean(fold_aucs)), 4),
            "std_auc": round(float(np.std(fold_aucs)), 4),
            "mean_sensitivity": round(float(np.mean(fold_recalls)), 4),
            "mean_specificity": round(float(np.mean(fold_specs)), 4),
        }
        print(f"  {name:25s} | 5-Fold AUC: {np.mean(fold_aucs):.4f} (+/- {np.std(fold_aucs):.4f}) | Sens: {np.mean(fold_recalls):.4f} | Spec: {np.mean(fold_specs):.4f}")

    # 6. FULL MODEL FITTING & HELD-OUT TEST EVALUATION
    print("\n[6/8] Training Full Soft-Voting Ensemble on complete train set...")
    ensemble = EnsembleClassifier(random_state=42)
    ensemble.fit(X_train, y_train)

    baseline = BaselineClassifier(random_state=42)
    baseline.fit(X_train, y_train)

    # Predictions on held-out 20% test set
    y_prob_baseline = baseline.predict_proba(X_test)[:, 1]
    y_prob_raw = ensemble.predict_proba(X_test)[:, 1]
    
    # Decision threshold tuned to satisfy Sensitivity >= 0.85 (Acceptance Criteria D2)
    target_thresh = 0.40
    test_metrics = ensemble.evaluate(X_test, y_test, threshold=target_thresh)
    baseline_metrics = {
        "auc_roc": round(float(auc(*roc_curve(y_test, y_prob_baseline)[:2])), 4),
        "sensitivity": round(float(np.sum((y_test == 1) & (y_prob_baseline >= 0.50)) / np.sum(y_test == 1)), 4),
    }

    print("\n--- HELD-OUT TEST PERFORMANCE RESULTS ---")
    print(f"Baseline Logistic Regression AUC : {baseline_metrics['auc_roc']:.4f}")
    print(f"Ensemble Classifier AUC-ROC      : {test_metrics['auc_roc']:.4f}  (Target >= 0.80 -> {'PASSED' if test_metrics['auc_roc'] >= 0.80 else 'FAIL'})")
    print(f"Ensemble Classifier Sensitivity  : {test_metrics['sensitivity']:.4f}  (Target >= 0.85 -> {'PASSED' if test_metrics['sensitivity'] >= 0.85 else 'FAIL'})")
    print(f"Ensemble Classifier Specificity  : {test_metrics['specificity']:.4f}")
    print(f"Ensemble Classifier Precision    : {test_metrics['precision']:.4f}")
    print(f"Ensemble Classifier F1-Score     : {test_metrics['f1_score']:.4f}")
    print(f"Ensemble Classifier Accuracy     : {test_metrics['accuracy']:.4f}")

    auc_delta = (test_metrics['auc_roc'] - baseline_metrics['auc_roc']) * 100
    print(f"Performance Advantage over Baseline: +{auc_delta:.1f} percentage points")

    # 7. PROBABILITY CALIBRATION (Isotonic Regression)
    print("\n[7/8] Fitting Isotonic Probability Calibration Module...")
    # Generate out-of-fold or train predictions for fitting calibrator
    y_train_raw_prob = ensemble.predict_proba(X_train)[:, 1]
    calibrator = CalibrationModule()
    calibrator.fit(y_train_raw_prob, y_train)

    y_prob_calibrated = calibrator.transform(y_prob_raw)
    calib_metrics = calibrator.evaluate_calibration(y_test, y_prob_calibrated)
    print(f"Calibration Brier Score : {calib_metrics['brier_score']:.4f}")
    print(f"Calibration Curve Slope : {calib_metrics['calibration_slope']:.3f} (Ideal: 0.90 - 1.10)")

    # 8. SHAP EXPLAINABILITY & PLAIN-LANGUAGE SUMMARY
    print("\n[8/8] Computing SHAP Explainability on constituent gradient booster...")
    # Use the XGBoost or LightGBM model from the ensemble for exact TreeExplainer
    shap_explainer = SHAPExplainer(model=ensemble.models["XGBoost"], feature_names=feature_names)
    
    os.makedirs("reports", exist_ok=True)
    shap_explainer.explain_global(X_test_df, save_path="reports/shap_summary.png")

    # Generate sample instance explanation for a high-risk child
    high_risk_idx = np.where((y_test == 1) & (y_prob_calibrated >= 0.65))[0][0]
    sample_instance = X_test[high_risk_idx]
    sample_child_id = df_test.loc[high_risk_idx, "child_id"]
    explanation = shap_explainer.explain_instance(sample_instance, top_k=3)
    tier_label, triage_action = CalibrationModule.categorize_risk(y_prob_calibrated[high_risk_idx])

    print("\n" + "=" * 50)
    print(f"EXAMPLE CHW POINT-OF-CARE SCREENING OUTPUT (Child ID: {sample_child_id})")
    print(f"Calibrated Risk Probability: {y_prob_calibrated[high_risk_idx]*100:.1f}%")
    print(f"Clinical Risk Category:     {tier_label}")
    print(f"Recommended Action:         {triage_action}")
    print("Plain-Language Contributing Factors (FR-05):")
    for bullet in explanation["plain_language_bullets"]:
        print(f"  * {bullet}")
    print("=" * 50)

    # 9. EXPORT VISUALIZATIONS
    print("\nExporting evaluation diagnostic plots...")
    # ROC Curves Comparison
    plt.figure(figsize=(8, 6))
    fpr_ens, tpr_ens, _ = roc_curve(y_test, y_prob_raw)
    fpr_base, tpr_base, _ = roc_curve(y_test, y_prob_baseline)
    plt.plot(fpr_ens, tpr_ens, color="#1f77b4", lw=2.5, label=f"Ensemble Classifier (AUC = {test_metrics['auc_roc']:.3f})")
    plt.plot(fpr_base, tpr_base, color="#7f7f7f", lw=1.8, linestyle="--", label=f"Baseline Logistic Regression (AUC = {baseline_metrics['auc_roc']:.3f})")
    plt.plot([0, 1], [0, 1], color="black", linestyle=":", lw=1)
    plt.xlim([0.0, 1.0])
    plt.ylim([0.0, 1.05])
    plt.xlabel("False Positive Rate (1 - Specificity)", fontsize=11)
    plt.ylabel("True Positive Rate (Sensitivity)", fontsize=11)
    plt.title("ROC Curve: Developmental Delay Prediction in Northern Kenya", fontsize=12)
    plt.legend(loc="lower right")
    plt.grid(alpha=0.3)
    plt.tight_layout()
    plt.savefig("reports/roc_comparison.png", dpi=300)
    plt.close()

    # Calibration Curve
    plt.figure(figsize=(8, 6))
    prob_true, prob_pred = calibration_curve(y_test, y_prob_calibrated, n_bins=10)
    plt.plot(prob_pred, prob_true, marker="o", color="#2ca02c", lw=2, label=f"Calibrated Ensemble (Slope: {calib_metrics['calibration_slope']})")
    plt.plot([0, 1], [0, 1], linestyle="--", color="gray", label="Perfect Calibration")
    plt.xlabel("Mean Predicted Risk Probability", fontsize=11)
    plt.ylabel("Observed Fraction of Delays", fontsize=11)
    plt.title("Isotonic Probability Calibration Curve", fontsize=12)
    plt.legend(loc="upper left")
    plt.grid(alpha=0.3)
    plt.tight_layout()
    plt.savefig("reports/calibration_curve.png", dpi=300)
    plt.close()

    # 10. SAVE METRICS AND SERIALIZED ARTIFACTS
    report_dict = {
        "held_out_test_metrics": test_metrics,
        "baseline_comparison": {
            "baseline_auc": baseline_metrics["auc_roc"],
            "ensemble_auc": test_metrics["auc_roc"],
            "improvement_percentage_points": round(auc_delta, 2)
        },
        "calibration_metrics": calib_metrics,
        "cross_validation_results": cv_results,
        "sample_chw_explanation": {
            "child_id": sample_child_id,
            "calibrated_risk": round(float(y_prob_calibrated[high_risk_idx]), 3),
            "tier": tier_label,
            "action": triage_action,
            "bullets": explanation["plain_language_bullets"]
        }
    }

    with open("reports/evaluation_metrics.json", "w") as f:
        json.dump(report_dict, f, indent=4)
    print("Full metrics report saved to: reports/evaluation_metrics.json")

    # Serialize trained models, cleaner, engineer, calibrator
    os.makedirs("models_saved", exist_ok=True)
    joblib.dump(ensemble, "models_saved/ensemble_classifier.joblib")
    joblib.dump(baseline, "models_saved/baseline_classifier.joblib")
    joblib.dump(cleaner, "models_saved/data_cleaner.joblib")
    joblib.dump(engineer, "models_saved/feature_engineer.joblib")
    joblib.dump(calibrator, "models_saved/calibration_module.joblib")
    print("Serialized models successfully saved to models_saved/")

    print("\n" + "=" * 80)
    print("MODEL PIPELINE EXECUTION COMPLETED SUCCESSFULLY!")
    print("=" * 80)

if __name__ == "__main__":
    run_end_to_end_pipeline()
