"""
export_onnx.py
==============
Exports trained models to Open Neural Network Exchange (ONNX) format for
offline edge inference using ONNX Runtime Mobile on Android devices (FR-04, Deliverable D2).

Validates exported ONNX models by running a test inference pass with onnxruntime
and comparing outputs against the native Python model.
"""

import os
import sys
import joblib

# Ensure project root is in sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))
import numpy as np
import pandas as pd
from skl2onnx import convert_sklearn
from skl2onnx.common.data_types import FloatTensorType
import onnxruntime as rt

def export_model_to_onnx(
    model_path: str = "models_saved/ensemble_classifier.joblib",
    feature_path: str = "data/processed/test_features.csv",
    output_onnx_path: str = "models_saved/developmental_delay_classifier.onnx"
):
    print("=" * 70)
    print("EXPORTING TRAINED MODEL TO ONNX FOR MOBILE DEPLOYMENT (DELIVERABLE D2)")
    print("=" * 70)

    if not os.path.exists(model_path):
        raise FileNotFoundError(f"Trained model not found at {model_path}. Run pipeline first.")

    ensemble = joblib.load(model_path)
    test_df = pd.read_csv(feature_path).drop(columns=["target"], errors="ignore")
    n_features = test_df.shape[1]
    feature_names = list(test_df.columns)
    print(f"Input feature vector dimensions: {n_features}")

    # Random Forest sub-model from the ensemble provides clean, native skl2onnx conversion
    rf_model = ensemble.models["RandomForest"]

    # Define initial input type for ONNX: FloatTensor of shape [None, n_features]
    initial_type = [("float_input", FloatTensorType([None, n_features]))]

    print("Converting model to ONNX format via skl2onnx...")
    onx = convert_sklearn(
        rf_model,
        initial_types=initial_type,
        target_opset=14,
        options={type(rf_model): {"zipmap": False}}
    )

    os.makedirs(os.path.dirname(output_onnx_path), exist_ok=True)
    with open(output_onnx_path, "wb") as f:
        f.write(onx.SerializeToString())

    print(f"ONNX model saved successfully to: {output_onnx_path}")
    print(f"Model file size: {os.path.getsize(output_onnx_path) / 1024:.1f} KB")

    # VERIFY ONNX RUNTIME INFERENCE
    print("\nValidating on-device inference using ONNX Runtime...")
    sess = rt.InferenceSession(output_onnx_path)
    input_name = sess.get_inputs()[0].name
    
    # Run test inference on first 5 samples
    test_sample = test_df.iloc[:5].values.astype(np.float32)
    onnx_preds = sess.run(None, {input_name: test_sample})

    # Compare probabilities with native Python model
    native_probs = rf_model.predict_proba(test_sample)[:, 1]
    onnx_probs = onnx_preds[1][:, 1] if len(onnx_preds) > 1 else onnx_preds[0][:, 1]

    print("Inference Verification (Sample 1-5):")
    for i in range(5):
        print(f"  Child {i+1}: Native Py={native_probs[i]:.4f} | ONNX Mobile={onnx_probs[i]:.4f} | Diff={abs(native_probs[i] - onnx_probs[i]):.6f}")

    max_diff = np.max(np.abs(native_probs - onnx_probs))
    assert max_diff < 1e-4, f"ONNX output deviation too high: {max_diff}"
    print(f"\nSUCCESS: Maximum ONNX discrepancy is {max_diff:.8f} (well within float precision).")
    print("Ready for embedding in Android assets/ directory via ONNX Runtime Mobile.")

if __name__ == "__main__":
    export_model_to_onnx()
