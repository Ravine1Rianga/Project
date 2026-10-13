"""
calibration.py
==============
Implements the CalibrationModule specified in Chapter 3 (Section 3.6.4)
and Chapter 4 (Class Diagram 4.4, FR-04).

Responsibilities:
1. Applies Isotonic Regression to map raw ensemble risk scores to calibrated probabilities.
2. Evaluates calibration reliability (Brier Score, Calibration Curves).
3. Triages calibrated probabilities into clinical risk tiers (Low, Moderate, High).
"""

from typing import Dict, Tuple
import numpy as np
from sklearn.isotonic import IsotonicRegression
from sklearn.metrics import brier_score_loss
from sklearn.calibration import calibration_curve

class CalibrationModule:
    """
    Calibrates raw ensemble probabilities using non-parametric Isotonic Regression
    to guarantee empirical probability reliability for frontline CHWs.
    """

    def __init__(self):
        self.calibrator = IsotonicRegression(out_of_bounds="clip", y_min=0.0, y_max=1.0)
        self.is_fitted: bool = False

    def fit(self, raw_probs: np.ndarray, y_true: np.ndarray) -> "CalibrationModule":
        """
        Fits isotonic regression on validation predictions.
        raw_probs: 1D array of uncalibrated positive class probabilities.
        """
        raw = np.asarray(raw_probs, dtype=float)
        targets = np.asarray(y_true, dtype=float)
        self.calibrator.fit(raw, targets)
        self.is_fitted = True
        return self

    def transform(self, raw_probs: np.ndarray) -> np.ndarray:
        """Transforms uncalibrated risk probabilities into calibrated probabilities."""
        if not self.is_fitted:
            raise RuntimeError("CalibrationModule must be fitted before calling transform.")
        raw = np.asarray(raw_probs, dtype=float)
        calibrated = self.calibrator.transform(raw)
        return np.clip(calibrated, 0.0, 1.0)

    def fit_transform(self, raw_probs: np.ndarray, y_true: np.ndarray) -> np.ndarray:
        return self.fit(raw_probs, y_true).transform(raw_probs)

    def evaluate_calibration(
        self,
        y_true: np.ndarray,
        calibrated_probs: np.ndarray,
        n_bins: int = 10
    ) -> Dict[str, float]:
        """Calculates Brier Score and calibration slope/intercept metrics."""
        y_t = np.asarray(y_true, dtype=float)
        p_c = np.asarray(calibrated_probs, dtype=float)

        brier = brier_score_loss(y_t, p_c)
        prob_true, prob_pred = calibration_curve(y_t, p_c, n_bins=n_bins, strategy="uniform")

        # Linear regression fit on calibration curve points to get slope
        if len(prob_pred) > 1:
            slope, intercept = np.polyfit(prob_pred, prob_true, deg=1)
        else:
            slope, intercept = 1.0, 0.0

        return {
            "brier_score": round(float(brier), 4),
            "calibration_slope": round(float(slope), 3),
            "calibration_intercept": round(float(intercept), 3),
        }

    @staticmethod
    def categorize_risk(calibrated_prob: float) -> Tuple[str, str]:
        """
        Maps calibrated risk probability into CHW action tiers:
        - Low Risk (Green): < 0.30
        - Moderate Risk (Yellow): 0.30 - 0.65
        - High Risk (Red): >= 0.65
        Returns (tier_label, triage_action)
        """
        p = float(calibrated_prob)
        if p < 0.30:
            return (
                "LOW RISK",
                "Maintain routine growth monitoring and standard infant feeding counselling."
            )
        elif p < 0.65:
            return (
                "MODERATE RISK",
                "Schedule home visit within 14 days, assess dietary diversity, provide targeted stimulation tips."
            )
        else:
            return (
                "HIGH RISK",
                "Urgent referral to sub-county health facility / paediatrician for comprehensive clinical and developmental evaluation."
            )
