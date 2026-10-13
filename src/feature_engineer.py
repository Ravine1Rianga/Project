"""
feature_engineer.py
===================
Implements the FeatureEngineer class under the OOAD paradigm as specified in
Chapter 3 (Section 3.2.2) and Chapter 4 (Class Diagram 4.4).

Responsibilities:
1. Derivation of composite nutrition indicators (Dietary Diversity Index, MAD).
2. Construction of clinical and biological interaction terms (e.g. MUAC x Maternal Edu).
3. One-hot encoding of categorical features (County, Water source, Sex, Education).
4. Min-Max scaling of continuous features into [0, 1].
"""

from typing import Dict, List, Optional, Tuple
import numpy as np
import pandas as pd
from sklearn.preprocessing import MinMaxScaler

class FeatureEngineer:
    """
    Transforms cleaned pediatric records into high-dimensional feature vectors
    ready for ensemble classification and SHAP explainability.
    """

    def __init__(self, scale_features: bool = True):
        self.scale_features = scale_features
        self.scaler = MinMaxScaler()
        self.feature_columns: List[str] = []
        self.edu_numeric_map = {"None": 0, "Primary": 1, "Secondary": 2, "Higher": 3}
        self.is_fitted: bool = False

    def _engineer_domain_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """Derives clinical, nutritional, and interaction indicators."""
        df_out = df.copy()

        # 1. Dietary Diversity Index (if food group flags present)
        diet_cols = [
            "diet_grains", "diet_dairy", "diet_legumes", "diet_flesh",
            "diet_eggs", "diet_vit_a", "diet_other_veg"
        ]
        present_diet_cols = [c for c in diet_cols if c in df_out.columns]
        if len(present_diet_cols) >= 5 and "dietary_diversity_score" not in df_out.columns:
            df_out["dietary_diversity_score"] = df_out[present_diet_cols].sum(axis=1)

        if "dietary_diversity_score" in df_out.columns:
            df_out["is_minimum_acceptable_diet"] = (df_out["dietary_diversity_score"] >= 5).astype(int)
            df_out["is_severely_diet_monotonous"] = (df_out["dietary_diversity_score"] <= 2).astype(int)

        # 2. Anthropometric clinical flags
        if "haz" in df_out.columns:
            df_out["is_stunted"] = (df_out["haz"] < -2.0).astype(int)
            df_out["is_severely_stunted"] = (df_out["haz"] < -3.0).astype(int)
        if "whz" in df_out.columns:
            df_out["is_wasted"] = (df_out["whz"] < -2.0).astype(int)
            df_out["is_severely_wasted"] = (df_out["whz"] < -3.0).astype(int)
        if "muac_cm" in df_out.columns:
            df_out["is_sam_by_muac"] = (df_out["muac_cm"] < 11.5).astype(int)
            df_out["is_mam_by_muac"] = ((df_out["muac_cm"] >= 11.5) & (df_out["muac_cm"] < 12.5)).astype(int)

        # 3. Composite Morbidity Burden
        morbidity_cols = ["had_diarrhoea_recent", "had_fever_recent", "had_cough_ari_recent"]
        present_morb = [c for c in morbidity_cols if c in df_out.columns]
        if present_morb:
            df_out["morbidity_score"] = df_out[present_morb].sum(axis=1)

        # 4. Numerical Maternal Education
        if "maternal_education" in df_out.columns:
            df_out["maternal_edu_level"] = df_out["maternal_education"].map(
                lambda x: self.edu_numeric_map.get(str(x), 0)
            )

        # 5. Synergistic Clinical Interactions (Proposal Section 3.2.2)
        # MUAC x Maternal Education: Amplification of nutritional reserve by caregiver literacy
        if "muac_cm" in df_out.columns and "maternal_edu_level" in df_out.columns:
            df_out["muac_x_maternal_edu"] = df_out["muac_cm"] * (df_out["maternal_edu_level"] + 1)

        # HAZ x DDI: Chronic linear deficit compound with micronutrient starvation
        if "haz" in df_out.columns and "dietary_diversity_score" in df_out.columns:
            df_out["haz_x_ddi"] = df_out["haz"] * df_out["dietary_diversity_score"]

        # Age x Stunting: Chronic duration vulnerability
        if "age_months" in df_out.columns and "haz" in df_out.columns:
            df_out["age_x_haz"] = (df_out["age_months"] / 12.0) * df_out["haz"]

        # Wasting x Morbidity: Dangerous acute spiral
        if "whz" in df_out.columns and "morbidity_score" in df_out.columns:
            df_out["whz_x_morbidity"] = df_out["whz"] * (df_out["morbidity_score"] + 1)

        return df_out

    def _get_candidate_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """Encodes categoricals and selects valid model features."""
        # Non-feature identifiers or target columns to exclude
        exclude_cols = [
            "child_id", "developmental_delay_risk",
            # Exclude ground-truth test labels if present in raw table
            "asq_communication_score", "asq_gross_motor_score",
            "asq_fine_motor_score", "asq_problem_solving_score",
            "asq_personal_social_score", "delay_communication",
            "delay_gross_motor", "delay_fine_motor",
            "delay_problem_solving", "delay_personal_social"
        ]

        feature_df = df.drop(columns=[c for c in exclude_cols if c in df.columns], errors="ignore")

        # One-Hot Encode categorical columns
        cat_cols = ["county", "sex", "water_source", "maternal_education"]
        present_cats = [c for c in cat_cols if c in feature_df.columns]
        
        feature_df = pd.get_dummies(feature_df, columns=present_cats, drop_first=False, dtype=float)
        return feature_df

    def fit(self, df: pd.DataFrame) -> "FeatureEngineer":
        """Fits feature encoders and scalers on the training set."""
        engineered = self._engineer_domain_features(df)
        feature_df = self._get_candidate_features(engineered)
        self.feature_columns = list(feature_df.columns)

        if self.scale_features:
            self.scaler.fit(feature_df[self.feature_columns])

        self.is_fitted = True
        return self

    def transform(self, df: pd.DataFrame) -> pd.DataFrame:
        """Transforms a dataframe into the fitted feature matrix."""
        if not self.is_fitted:
            raise RuntimeError("FeatureEngineer must be fitted before calling transform.")

        engineered = self._engineer_domain_features(df)
        feature_df = self._get_candidate_features(engineered)

        # Ensure column alignment with training set
        for col in self.feature_columns:
            if col not in feature_df.columns:
                feature_df[col] = 0.0

        # Keep strictly the fitted columns in identical order
        aligned_df = feature_df[self.feature_columns].copy()

        if self.scale_features:
            scaled_vals = self.scaler.transform(aligned_df)
            scaled_vals = np.clip(scaled_vals, 0.0, 1.0)
            aligned_df = pd.DataFrame(scaled_vals, columns=self.feature_columns, index=aligned_df.index)

        return aligned_df

    def fit_transform(self, df: pd.DataFrame) -> pd.DataFrame:
        """Convenience method to fit and transform in one step."""
        return self.fit(df).transform(df)
