"""
explainability.py
=================
Implements the SHAPExplainer class as specified in Chapter 3 (Section 3.6.4)
and Chapter 4 (Class Diagram 4.4, FR-05).

Responsibilities:
1. Calculates global feature importance using SHAP TreeExplainer.
2. Generates and saves visual summary plots (beeswarm / bar).
3. Decomposes individual child predictions into exact additive feature attributions.
4. Translates raw SHAP values into plain-language clinical explanations for CHWs.
"""

from typing import Any, Dict, List, Optional
import os
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import shap

class SHAPExplainer:
    """
    Computes game-theoretic SHAP attributions and renders plain-language
    clinical rationales for frontline Community Health Workers.
    """

    # Feature label translation dictionary to plain, low-literacy English
    PLAIN_LANGUAGE_MAP: Dict[str, str] = {
        "muac_cm": "Mid-Upper Arm Circumference (MUAC)",
        "haz": "Height-for-Age (Linear Growth / Stunting Status)",
        "whz": "Weight-for-Height (Acute Wasting Status)",
        "waz": "Weight-for-Age (Underweight Status)",
        "dietary_diversity_score": "Dietary Diversity (Food Group Variety)",
        "is_severely_diet_monotonous": "Severe Dietary Monotony (<= 2 food groups)",
        "had_diarrhoea_recent": "Recent Diarrhoea Illness (Past 2 Weeks)",
        "had_fever_recent": "Recent Fever Episode",
        "had_cough_ari_recent": "Recent Cough / Respiratory Infection",
        "vaccination_complete_for_age": "Childhood Vaccination Completion",
        "vitamin_a_supplementation": "Vitamin A Supplementation",
        "dewormed_last_6m": "Deworming Treatment Received",
        "muac_x_maternal_edu": "Caregiver Education Combined with MUAC",
        "haz_x_ddi": "Chronic Stunting Compounded by Poor Diet",
        "whz_x_morbidity": "Acute Wasting Compounded by Recent Sickness",
        "age_x_haz": "Age-Adjusted Duration of Stunting",
        "water_source_Unimproved": "Unprotected Drinking Water Source",
        "water_source_Improved": "Clean Protected Drinking Water",
        "maternal_education_None": "No Maternal Formal Schooling",
        "county_Turkana": "High-Vulnerability ASAL Zone (Turkana)",
        "edema": "Bilateral Pitting Edema (Kwashiorkor Sign)",
    }

    def __init__(self, model: Any, feature_names: List[str]):
        """
        model: A tree-based model (e.g. fitted XGBoost or LightGBM classifier).
        feature_names: List of column names corresponding to input feature matrix.
        """
        self.model = model
        self.feature_names = feature_names
        # Initialize shap TreeExplainer
        self.explainer = shap.TreeExplainer(self.model)

    def explain_global(
        self,
        X: pd.DataFrame,
        save_path: Optional[str] = "reports/shap_summary.png",
        max_display: int = 15
    ) -> np.ndarray:
        """
        Computes global SHAP values across a cohort and exports a summary plot.
        """
        X_df = X if isinstance(X, pd.DataFrame) else pd.DataFrame(X, columns=self.feature_names)
        shap_values = self.explainer(X_df)

        # Handle binary classification slice if needed
        values = shap_values.values
        if len(values.shape) == 3:
            # Positive class slice (index 1)
            values = values[:, :, 1]

        if save_path:
            os.makedirs(os.path.dirname(save_path), exist_ok=True)
            plt.figure(figsize=(10, 7))
            shap.summary_plot(
                values,
                X_df,
                feature_names=self.feature_names,
                max_display=max_display,
                show=False
            )
            plt.title("Global SHAP Feature Importance: Malnutrition-Associated Delay", fontsize=12)
            plt.tight_layout()
            plt.savefig(save_path, dpi=300, bbox_inches="tight")
            plt.close()
            print(f"Global SHAP summary plot saved to: {save_path}")

        return values

    def explain_instance(
        self,
        instance_features: np.ndarray,
        top_k: int = 3
    ) -> Dict[str, Any]:
        """
        Computes SHAP feature attributions for a single patient record and
        generates top-K plain-language clinical bullet statements (FR-05).
        """
        inst = np.asarray(instance_features).reshape(1, -1)
        raw_shap = self.explainer.shap_values(inst)

        # Extract 1D array of SHAP values
        if isinstance(raw_shap, list):
            sv = raw_shap[1][0]  # Positive class for binary classifier
        elif len(raw_shap.shape) == 3:
            sv = raw_shap[0, :, 1]
        elif len(raw_shap.shape) == 2:
            sv = raw_shap[0]
        else:
            sv = raw_shap

        # Rank features by absolute attribution impact
        top_indices = np.argsort(np.abs(sv))[::-1][:top_k]

        plain_explanations = []
        feature_breakdown = []

        for rank, idx in enumerate(top_indices, 1):
            feat_name = self.feature_names[idx]
            impact = float(sv[idx])
            val = float(inst[0, idx])

            friendly_name = self.PLAIN_LANGUAGE_MAP.get(feat_name, feat_name.replace("_", " ").title())
            
            # Formulate plain-language rationale
            if impact > 0:
                direction = "INCREASED risk"
                reason = f"{friendly_name} significantly elevated developmental delay risk"
            else:
                direction = "DECREASED risk (protective)"
                reason = f"{friendly_name} provided protective buffering against delay"

            percentage_impact = round(abs(impact) * 100, 1)
            statement = f"Factor #{rank}: {friendly_name} ({direction} by ~{percentage_impact}%)."

            plain_explanations.append(statement)
            feature_breakdown.append({
                "rank": rank,
                "feature": feat_name,
                "display_name": friendly_name,
                "feature_value": round(val, 3),
                "shap_impact": round(impact, 4),
                "direction": "risk_factor" if impact > 0 else "protective_factor",
                "plain_statement": statement
            })

        return {
            "top_features": feature_breakdown,
            "plain_language_bullets": plain_explanations,
        }
