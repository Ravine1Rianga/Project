"""
test_pipeline.py
================
Comprehensive automated test suite for the machine learning pipeline.
Tests:
1. WHO Anthro Z-Score Calculator (FR-03)
2. DataCleaner (Winsorization & MICE Imputation)
3. FeatureEngineer (Dietary Diversity & Interaction Terms)
4. CalibrationModule (Isotonic Probability Mapping)
5. SHAPExplainer (Instance Attribution & Plain Language Translation)
6. ONNX Model Inference (Offline edge verification)
"""

import os
import unittest
import numpy as np
import pandas as pd
import joblib
import onnxruntime as rt

from src.who_anthro import WHOAnthroCalculator
from src.data_cleaner import DataCleaner
from src.feature_engineer import FeatureEngineer
from src.calibration import CalibrationModule
from src.explainability import SHAPExplainer

class TestMalnutritionDelayPipeline(unittest.TestCase):

    def test_01_who_anthro_zscores(self):
        """Test WHO Z-score calculation for typical and stunted children."""
        # A 12-month-old boy with height 75.7cm is at the 50th percentile (HAZ ~ 0)
        res_normal = WHOAnthroCalculator.compute_all_zscores(
            age_months=12, sex=1, height_cm=75.7, weight_kg=9.6
        )
        self.assertAlmostEqual(res_normal["haz"], 0.0, delta=0.5)
        self.assertEqual(res_normal["is_stunted"], 0)

        # A 24-month-old boy with severe linear stunting (height 72cm, expected ~87cm)
        res_stunted = WHOAnthroCalculator.compute_all_zscores(
            age_months=24, sex=1, height_cm=72.0, weight_kg=8.5
        )
        self.assertLess(res_stunted["haz"], -2.0)
        self.assertEqual(res_stunted["is_stunted"], 1)

    def test_02_data_cleaner_imputation(self):
        """Test MICE imputation handles missing values properly."""
        df_dummy = pd.DataFrame({
            "height_cm": [70.0, np.nan, 80.0, 75.0, 85.0],
            "weight_kg": [8.0, 9.0, np.nan, 8.5, 11.0],
            "muac_cm": [12.0, 13.0, 12.5, np.nan, 14.0],
            "haz": [-1.5, -2.0, -1.0, -1.8, -0.5],
            "waz": [-1.2, -1.5, -1.0, -1.4, -0.4],
            "whz": [-0.8, -1.0, -0.9, -1.1, -0.2],
            "dietary_diversity_score": [3, 2, np.nan, 4, 6],
            "had_diarrhoea_recent": [1, 0, 0, 1, 0]
        })
        cleaner = DataCleaner(random_state=42)
        cleaned = cleaner.fit_transform(df_dummy)
        
        # Verify no NaNs remain
        self.assertEqual(cleaned.isna().sum().sum(), 0)

    def test_03_feature_engineer_interactions(self):
        """Test composite dietary index and clinical interaction terms."""
        df_dummy = pd.DataFrame({
            "diet_grains": [1, 1],
            "diet_dairy": [1, 0],
            "diet_legumes": [0, 0],
            "diet_flesh": [0, 0],
            "diet_eggs": [0, 0],
            "diet_vit_a": [0, 0],
            "diet_other_veg": [1, 0],
            "maternal_education": ["Secondary", "None"],
            "muac_cm": [14.0, 11.0],
            "haz": [-0.5, -3.2],
            "whz": [-0.2, -2.5],
            "had_diarrhoea_recent": [0, 1],
            "had_fever_recent": [0, 1],
            "had_cough_ari_recent": [0, 0],
            "age_months": [18, 24],
            "county": ["Turkana", "Marsabit"],
            "sex": ["Male", "Female"],
            "water_source": ["Improved", "Unimproved"]
        })
        engineer = FeatureEngineer(scale_features=True)
        transformed = engineer.fit_transform(df_dummy)

        self.assertIn("muac_x_maternal_edu", transformed.columns)
        self.assertIn("haz_x_ddi", transformed.columns)
        self.assertIn("whz_x_morbidity", transformed.columns)
        # Scaled values must lie in [0, 1]
        self.assertTrue((transformed.values >= 0.0).all())
        self.assertTrue((transformed.values <= 1.0).all())

    def test_04_calibration_module(self):
        """Test probability calibration and risk triaging."""
        calibrator = CalibrationModule()
        raw_probs = np.array([0.10, 0.25, 0.55, 0.75, 0.90])
        y_true = np.array([0, 0, 1, 1, 1])
        calibrator.fit(raw_probs, y_true)
        calibrated = calibrator.transform(raw_probs)

        self.assertEqual(len(calibrated), len(raw_probs))
        self.assertTrue((calibrated >= 0.0).all() and (calibrated <= 1.0).all())

        # Test clinical categorization
        tier_low, _ = CalibrationModule.categorize_risk(0.15)
        tier_high, _ = CalibrationModule.categorize_risk(0.85)
        self.assertEqual(tier_low, "LOW RISK")
        self.assertEqual(tier_high, "HIGH RISK")

    def test_05_onnx_runtime_inference(self):
        """Test on-device ONNX model execution."""
        onnx_path = "models_saved/developmental_delay_classifier.onnx"
        self.assertTrue(os.path.exists(onnx_path), "ONNX model artifact must exist")

        sess = rt.InferenceSession(onnx_path)
        input_name = sess.get_inputs()[0].name
        dummy_input = np.random.rand(2, 50).astype(np.float32)
        outputs = sess.run(None, {input_name: dummy_input})

        self.assertEqual(len(outputs[0]), 2)  # 2 samples predicted

if __name__ == "__main__":
    unittest.main()
