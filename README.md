# A Machine Learning Approach for Predicting Developmental Delays Associated with Malnutrition Among Children Under Five in Northern Kenya

**Bachelor of Science in Informatics and Computer Science — Capstone Project**  
School of Computing and Engineering Sciences, Strathmore University, Nairobi, Kenya  
**Author:** Riang’a Ravine Kerubo (Admission No. 168669 | ICS 4C)  
**Supervisor:** Mr. Kevin Ochieng’ Omondi  

---

## 1. Overview & Problem Statement

In the Arid and Semi-Arid Lands (ASAL) of Northern Kenya (*Turkana, Marsabit, Wajir, and Mandera*), recurrent drought cycles and severe food insecurity subject children under five to high rates of chronic stunting ($>42\%$) and acute wasting ($GAM > 15\%$). 

Frontline Community Health Workers (CHWs) currently rely almost exclusively on anthropometric measurements (MUAC, WHZ), which detect acute wasting but miss silent neurocognitive stalling. Meanwhile, standard developmental batteries like the Bayley-4 or ASQ-3 take 30–60 minutes and require psychologists, which is fundamentally impractical in nomadic pastoralist field settings.

This repository provides an **explainable ensemble machine learning framework** that predicts developmental delay risk from structured anthropometric, dietary, and health history data, calibrated with **Isotonic Regression** and explained via **SHAP** (Shapley Additive exPlanations) for point-of-care mobile decision support.

---

## 2. Key Performance Metrics vs. Acceptance Criteria

| Metric | Project Acceptance Target | Model Evaluation Result |
| :--- | :--- | :--- |
| **AUC-ROC** | $\ge 0.80$ | **$0.9548$** (PASSED) |
| **Sensitivity (Recall)** | $\ge 0.85$ | **$0.9212$** (PASSED) |
| **Specificity** | High screening specificity | **$0.8551$** |
| **Precision** | Positive Predictive Value | **$0.7327$** |
| **F1-Score** | Harmonic Mean | **$0.8162$** |
| **Overall Accuracy** | Held-Out Test Set ($N=800$) | **$87.50\%$** |
| **Calibration Reliability** | Brier Score / Slope | **Brier: $0.0885$ \| Slope: $0.802$** |
| **Inference Latency** | Offline Mobile ($\le 2$ GB RAM) | **Sub-second edge execution via ONNX** |

---

## 3. Architecture & Project Structure

The project strictly implements an Object-Oriented Analysis and Design (OOAD) architecture:

```
Project/
├── data/
│   ├── raw/
│   │   └── asal_child_development_dataset.csv  # 4,000 ASAL pediatric records
│   └── processed/
│       ├── train_features.csv                  # 3,200 training samples (50 features)
│       └── test_features.csv                   # 800 test samples (50 features)
├── src/
│   ├── who_anthro.py                           # Local WHO 2006 LMS Z-score engine (FR-03)
│   ├── dataset_generator.py                    # Regional ASAL epidemiological synthesizer
│   ├── data_cleaner.py                         # DataCleaner: Winsorization & MICE Imputation
│   ├── feature_engineer.py                     # FeatureEngineer: DDI, interactions & scaling
│   ├── ensemble_model.py                       # EnsembleClassifier: RF, XGBoost, LightGBM, CatBoost
│   ├── calibration.py                          # CalibrationModule: Isotonic probability mapping
│   ├── explainability.py                       # SHAPExplainer: Global plots & CHW bullet generator
│   └── export_onnx.py                          # ONNX Mobile export & runtime verification
├── models_saved/
│   ├── ensemble_classifier.joblib              # Full soft-voting ensemble
│   ├── baseline_classifier.joblib              # Baseline Logistic Regression
│   ├── data_cleaner.joblib                     # Fitted MICE imputer
│   ├── feature_engineer.joblib                 # Fitted feature transformer & scalers
│   ├── calibration_module.joblib               # Fitted probability calibrator
│   └── developmental_delay_classifier.onnx     # Production ONNX Mobile model (3.2 MB)
├── reports/
│   ├── evaluation_metrics.json                 # Comprehensive metrics report
│   ├── roc_comparison.png                      # ROC Curves (Ensemble vs Baseline)
│   ├── calibration_curve.png                   # Probability calibration curve
│   └── shap_summary.png                        # Global SHAP beeswarm feature importance
├── requirements.txt                            # Pinned Python dependencies
├── run_pipeline.py                             # Master training and evaluation pipeline
└── test_pipeline.py                            # Automated unit & integration test suite
```

---

## 4. Explainable AI & CHW Decision Support (FR-05)

To build clinical trust and prevent algorithmic opacity, the framework pairs every prediction with an instance-level SHAP explanation converted into plain-language clinical bullet points:

```
==================================================
POINT-OF-CARE SCREENING OUTPUT (Child ID: WA-4676)
Calibrated Risk Probability: 100.0%
Clinical Risk Category:     HIGH RISK
Recommended Action:         Urgent referral to sub-county health facility / paediatrician
                            for comprehensive clinical and developmental evaluation.

Plain-Language Contributing Factors:
  * Factor #1: Weight-for-Height (Acute Wasting Status) (INCREASED risk by ~160.1%).
  * Factor #2: Acute Wasting Compounded by Recent Sickness (INCREASED risk by ~114.7%).
  * Factor #3: Childhood Vaccination Completion (DECREASED risk (protective) by ~79.6%).
==================================================
```

---

## 5. Getting Started

### Installation
Clone the repository and install dependencies:
```bash
git clone https://github.com/RAVINERIANGA/<repo-name>.git
cd <repo-name>
pip install -r requirements.txt
```

### Running the End-to-End Pipeline
To run data cleaning, feature engineering, 5-fold cross-validation, model training, probability calibration, and report generation:
```bash
python run_pipeline.py
```

### Exporting to ONNX Mobile
To export the trained model for Android on-device inference:
```bash
python src/export_onnx.py
```

### Running Tests
Execute the unit and integration test suite:
```bash
python test_pipeline.py
```

---

## 6. Citation & Academic Declaration
This project was undertaken in partial fulfillment of the requirements for the award of a Bachelor of Science in Informatics and Computer Science at Strathmore University, Nairobi, Kenya.
