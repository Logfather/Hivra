import json, tempfile, unittest
from pathlib import Path
from him_trainer.productive_development_v3 import evaluate_v3_development

AUTH = "data/knowledge/him/training/v2/prospective/prospective-relational-development-v1.json"

class Model:
    def predict_v3(self, serialized, checkpoint):
        self.seen = getattr(self, "seen", 0) + 1
        return {"targetKind": "VARIANT", "candidateCompatibility": "REJECT"}

class V3DevelopmentEvaluatorTest(unittest.TestCase):
    def test_six_records_and_persistence(self):
        with tempfile.TemporaryDirectory() as d:
            result=evaluate_v3_development(authority_path=AUTH, model=Model(), checkpoint=object(), output_path=Path(d)/"result.json")
            self.assertEqual(result["recordCount"], 6)
            self.assertEqual(result["holdoutAccessCount"], 0)
            self.assertTrue(Path(result["persistedPath"]).is_file())
            self.assertIn("developmentCompatibilityAccuracy", result["metrics"])
    def test_serializer_and_masking_contract(self):
        model=Model()
        result=evaluate_v3_development(authority_path=AUTH, model=model, checkpoint=object())
        self.assertEqual(len(result["records"]), 6)
        self.assertTrue(all(r["serializedInput"].startswith("<HIMV2V3>") for r in result["records"]))
        self.assertGreaterEqual(result["metrics"]["developmentTargetKindApplicableCount"], 0)

if __name__ == "__main__": unittest.main()
