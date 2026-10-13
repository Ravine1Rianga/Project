"""
data_cleaner.py
===============
Implements the DataCleaner class under the Object-Oriented Analysis and Design (OOAD)
paradigm as specified in Chapter 3 (Section 3.2.2) and Chapter 4 (Class Diagram 4.4).

Responsibilities:
1. Biological plausibility checking and validation of input records (FR-02).
2. Outlier Winsorization at 1st and 99th percentiles to mitigate extreme recording errors.
3. Multiple Imputation by Chained Equations (MICE) using Scikit-Learn's IterativeImputer.
"""

from typing import List, Optional, Tuple, Union
import numpy as np
import pandas as pd
from sklearn.experimental import enable_iterative_imputer  # noqa: F401
from sklearn.impute import IterativeImputer

class DataCleaner:
    """
    Cleans raw pediatric records, applies biological outlier capping (Winsorization),
    and systematically imputes missing values using MICE.
    """

    def __init__(
        self,
        winsorize_cols: Optional[List[str]] = None,
        impute_cols: Optional[List[str]] = None,
        random_state: int = 42
    ):
        self.winsorize_cols = winsorize_cols or [
            "height_cm", "weight_kg", "muac_cm", "haz", "waz", "whz", "dietary_diversity_score"
        ]
        self.impute_cols = impute_cols or [
            "height_cm", "weight_kg", "muac_cm", "haz", "waz", "whz",
            "dietary_diversity_score", "had_diarrhoea_recent"
        ]
        self.random_state = random_state
        self.imputer: Optional[IterativeImputer] = None
        self.winsor_bounds: dict = {}
        self.is_fitted: bool = False

    def validate_record(self, record: dict) -> Tuple[bool, List[str]]:
        """
        Validates individual field values against biological feasibility boundaries.
        Returns (is_valid, list_of_validation_errors).
        """
        errors = []
        age = record.get("age_months")
        if age is None or age < 0 or age > 59:
            errors.append(f"Invalid age_months: {age}. Must be between 0 and 59.")

        muac = record.get("muac_cm")
        if muac is not None and (muac < 7.0 or muac > 25.0):
            errors.append(f"Biologically implausible MUAC: {muac} cm (plausible range: 7.0 - 25.0 cm).")

        weight = record.get("weight_kg")
        if weight is not None and (weight < 1.5 or weight > 35.0):
            errors.append(f"Biologically implausible weight: {weight} kg (plausible range: 1.5 - 35.0 kg).")

        height = record.get("height_cm")
        if height is not None and (height < 35.0 or height > 130.0):
            errors.append(f"Biologically implausible height/length: {height} cm.")

        return (len(errors) == 0, errors)

    def fit(self, df: pd.DataFrame) -> "DataCleaner":
        """
        Learns Winsorization percentile bounds and trains the MICE IterativeImputer.
        """
        df_clean = df.copy()

        # 1. Compute 1st and 99th percentile bounds for continuous features
        for col in self.winsorize_cols:
            if col in df_clean.columns:
                valid_vals = df_clean[col].dropna()
                if len(valid_vals) > 0:
                    low = float(np.percentile(valid_vals, 1))
                    high = float(np.percentile(valid_vals, 99))
                    self.winsor_bounds[col] = (low, high)

        # Apply temporary clipping before fitting imputer
        for col, (low, high) in self.winsor_bounds.items():
            if col in df_clean.columns:
                df_clean[col] = df_clean[col].clip(lower=low, upper=high)

        # 2. Fit MICE IterativeImputer on numerical subset
        available_impute_cols = [c for c in self.impute_cols if c in df_clean.columns]
        if available_impute_cols:
            self.imputer = IterativeImputer(
                max_iter=10,
                random_state=self.random_state,
                sample_posterior=False,
                skip_complete=True
            )
            self.imputer.fit(df_clean[available_impute_cols])

        self.is_fitted = True
        return self

    def transform(self, df: pd.DataFrame) -> pd.DataFrame:
        """
        Applies learned Winsorization bounds and MICE imputation to the dataset.
        """
        if not self.is_fitted:
            raise RuntimeError("DataCleaner must be fitted before calling transform.")

        df_out = df.copy()

        # 1. Apply Winsorization
        for col, (low, high) in self.winsor_bounds.items():
            if col in df_out.columns:
                df_out[col] = df_out[col].clip(lower=low, upper=high)

        # 2. Apply MICE Imputation
        available_impute_cols = [c for c in self.impute_cols if c in df_out.columns]
        if self.imputer is not None and available_impute_cols:
            imputed_vals = self.imputer.transform(df_out[available_impute_cols])
            df_out[available_impute_cols] = imputed_vals

            # Clean boundary sanity
            if "dietary_diversity_score" in df_out.columns:
                df_out["dietary_diversity_score"] = df_out["dietary_diversity_score"].round().clip(0, 8)
            if "had_diarrhoea_recent" in df_out.columns:
                df_out["had_diarrhoea_recent"] = (df_out["had_diarrhoea_recent"] >= 0.5).astype(int)

        return df_out

    def fit_transform(self, df: pd.DataFrame) -> pd.DataFrame:
        """Convenience method to fit and transform in one step."""
        return self.fit(df).transform(df)
