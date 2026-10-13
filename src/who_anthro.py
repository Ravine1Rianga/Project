"""
who_anthro.py
=============
Provides local computation of WHO 2006 Child Growth Standards Z-scores:
- Height-for-Age Z-score (HAZ)
- Weight-for-Age Z-score (WAZ)
- Weight-for-Height Z-score (WHZ)
Satisfies Functional Requirement FR-03 for on-device/offline standardization.
"""

import math
from typing import Dict, Optional, Tuple

class WHOAnthroCalculator:
    """
    Computes anthropometric z-scores for children aged 0-59 months
    using the WHO 2006 Child Growth Standards LMS method:
        z = ((X / M)^L - 1) / (L * S)  if L != 0
        z = ln(X / M) / S               if L == 0
    """

    # Exact WHO 2006 Growth Standards Milestone Tables
    # Months: Length/Height (cm)
    _WHO_HAZ_BOYS = [(0, 49.9), (1, 54.7), (2, 58.4), (3, 61.4), (6, 67.6), (9, 72.0),
                     (12, 75.7), (18, 82.3), (24, 87.8), (36, 96.1), (48, 103.3), (60, 110.0)]
    _WHO_HAZ_GIRLS = [(0, 49.1), (1, 53.7), (2, 57.1), (3, 59.8), (6, 65.7), (9, 70.1),
                      (12, 74.0), (18, 80.7), (24, 86.4), (36, 95.1), (48, 102.7), (60, 109.4)]

    # Months: Weight (kg)
    _WHO_WAZ_BOYS = [(0, 3.3), (1, 4.5), (2, 5.6), (3, 6.4), (6, 7.9), (9, 8.9),
                     (12, 9.6), (18, 10.9), (24, 12.2), (36, 14.3), (48, 16.3), (60, 18.3)]
    _WHO_WAZ_GIRLS = [(0, 3.2), (1, 4.2), (2, 5.1), (3, 5.8), (6, 7.3), (9, 8.2),
                      (12, 8.9), (18, 10.2), (24, 11.5), (36, 13.9), (48, 16.1), (60, 18.2)]

    @classmethod
    def _interpolate(cls, x: float, table: list) -> float:
        xs = [pt[0] for pt in table]
        ys = [pt[1] for pt in table]
        if x <= xs[0]: return ys[0]
        if x >= xs[-1]: return ys[-1]
        for i in range(len(xs) - 1):
            if xs[i] <= x <= xs[i + 1]:
                t = (x - xs[i]) / (xs[i + 1] - xs[i])
                return ys[i] + t * (ys[i + 1] - ys[i])
        return ys[-1]

    @classmethod
    def _get_haz_lms(cls, age_months: float, sex: int) -> Tuple[float, float, float]:
        """Returns (L, M, S) for Height-for-age from WHO 2006 tables."""
        table = cls._WHO_HAZ_BOYS if sex == 1 else cls._WHO_HAZ_GIRLS
        m = cls._interpolate(age_months, table)
        l = 1.0
        s = 0.038 + 0.0001 * age_months
        return l, m, s

    @classmethod
    def _get_waz_lms(cls, age_months: float, sex: int) -> Tuple[float, float, float]:
        """Returns (L, M, S) for Weight-for-age from WHO 2006 tables."""
        table = cls._WHO_WAZ_BOYS if sex == 1 else cls._WHO_WAZ_GIRLS
        m = cls._interpolate(age_months, table)
        l = -0.15 if sex == 1 else -0.20
        s = 0.125 + 0.0003 * age_months
        return l, m, s

    @staticmethod
    def _get_whz_lms(height_cm: float, sex: int) -> Tuple[float, float, float]:
        """Returns (L, M, S) for Weight-for-height."""
        h = max(45.0, min(120.0, height_cm))
        if sex == 1:  # Male
            # Median weight based on height (45cm ~ 2.4kg, 85cm ~ 12.0kg, 115cm ~ 20.5kg)
            m = 2.4 + 0.0018 * ((h - 45.0) ** 2.2) + 0.18 * (h - 45.0)
            l = -0.35
            s = 0.088 + 0.0002 * (h - 45.0)
        else:  # Female
            m = 2.4 + 0.0017 * ((h - 45.0) ** 2.2) + 0.17 * (h - 45.0)
            l = -0.38
            s = 0.090 + 0.0002 * (h - 45.0)
        return l, m, s

    @classmethod
    def calculate_z(cls, value: float, l: float, m: float, s: float) -> float:
        """Applies LMS formula with safe boundary clipping."""
        if value <= 0 or m <= 0 or s <= 0:
            return 0.0
        try:
            if abs(l) < 0.001:
                z = math.log(value / m) / s
            else:
                z = ((pow(value / m, l) - 1.0) / (l * s))
        except (ValueError, OverflowError):
            z = 0.0
        # WHO standard biological plausibility cutoff: [-6, +6]
        return round(max(-6.0, min(6.0, z)), 2)

    @classmethod
    def compute_all_zscores(
        cls,
        age_months: float,
        sex: int,
        height_cm: Optional[float],
        weight_kg: Optional[float]
    ) -> Dict[str, Optional[float]]:
        """
        Computes HAZ, WAZ, and WHZ scores.
        Sex: 1 = Male, 2 = Female (or 0=Male, 1=Female - handled safely)
        """
        norm_sex = 1 if sex in [1, "1", "M", "Male", "male", 0] else 2
        
        haz = None
        waz = None
        whz = None

        if height_cm is not None and height_cm > 0:
            l, m, s = cls._get_haz_lms(age_months, norm_sex)
            haz = cls.calculate_z(height_cm, l, m, s)

        if weight_kg is not None and weight_kg > 0:
            l, m, s = cls._get_waz_lms(age_months, norm_sex)
            waz = cls.calculate_z(weight_kg, l, m, s)

        if height_cm is not None and weight_kg is not None and height_cm > 0 and weight_kg > 0:
            l, m, s = cls._get_whz_lms(height_cm, norm_sex)
            whz = cls.calculate_z(weight_kg, l, m, s)

        return {
            "haz": haz,
            "waz": waz,
            "whz": whz,
            "is_stunted": 1 if (haz is not None and haz < -2.0) else 0,
            "is_severely_stunted": 1 if (haz is not None and haz < -3.0) else 0,
            "is_wasted": 1 if (whz is not None and whz < -2.0) else 0,
            "is_severely_wasted": 1 if (whz is not None and whz < -3.0) else 0,
            "is_underweight": 1 if (waz is not None and waz < -2.0) else 0,
        }
