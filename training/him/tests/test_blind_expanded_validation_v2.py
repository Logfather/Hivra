import copy
import json
import pathlib
import shutil
import tempfile
import unittest

from him_trainer import blind_expanded_validation_v2 as evaluator


ROOT = pathlib.Path(__file__).resolve().parents[3]
SEALED_ROOT = ROOT / "build/knowledge/reports/him/training/p2/corpus-v2/blind-expanded-validation-v2"


class BlindExpandedValidationV2Test(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.sealed = evaluator.load_sealed_v2_set(SEALED_ROOT)

    def test_sealed_authority_and_counts(self):
        self.assertEqual(len(self.sealed.examples), 21)
        self.assertEqual(len({item["familyReference"] for item in self.sealed.examples}), 9)
        self.assertEqual(sum(item["primaryTarget"] == "IDENTITY" for item in self.sealed.examples), 6)
        self.assertEqual(sum(item["primaryTarget"] == "VARIANT" for item in self.sealed.examples), 7)
        self.assertEqual(sum(item["primaryTarget"] == "NOT_APPLICABLE" for item in self.sealed.examples), 8)
        self.assertEqual(sum(item["candidateCompatibility"] == "COMPATIBLE" for item in self.sealed.examples), 13)
        self.assertEqual(sum(item["candidateCompatibility"] == "REJECT" for item in self.sealed.examples), 8)

    def test_synthetic_pipeline_persists_and_reloads_result(self):
        with tempfile.TemporaryDirectory(prefix="him-p2-v2-test-output-") as output:
            result = evaluator.synthetic_end_to_end(SEALED_ROOT, output)
            payload = result["result"]["resultPayload"]
            self.assertEqual(payload["evaluationExampleCount"], 21)
            self.assertEqual(payload["primary"]["evaluated"], 13)
            self.assertEqual(payload["secondary"]["evaluated"], 21)
            self.assertEqual(payload["primary"]["correct"], 13)
            self.assertEqual(payload["secondary"]["correct"], 21)
            self.assertEqual(payload["perFamily"].__len__(), 9)
            self.assertEqual(payload["perUnitErrors"], [])
            self.assertEqual(payload["executionCounters"]["modelDeserializationCount"], 0)
            self.assertEqual(payload["executionCounters"]["forwardCount"], 0)
            self.assertEqual(payload["holdout"]["exposureCount"], 0)
            self.assertTrue(result["rawPath"].is_file())
            self.assertTrue(result["resultPath"].is_file())
            self.assertFalse((SEALED_ROOT / evaluator.RAW_PREDICTIONS_FILENAME).exists())
            self.assertFalse((SEALED_ROOT / evaluator.RESULT_FILENAME).exists())

    def test_v1_binding_remains_historical(self):
        from him_trainer import expanded_validation_p2

        self.assertEqual(expanded_validation_p2.EXPECTED_TOTAL, 19)
        self.assertNotEqual(expanded_validation_p2.EXPECTED_TOTAL, len(self.sealed.examples))

    def test_fail_closed_prediction_membership(self):
        predictions = evaluator.build_synthetic_predictions(self.sealed)
        duplicate = copy.deepcopy(predictions)
        duplicate["payload"]["predictions"][-1]["evaluationExampleReference"] = duplicate["payload"]["predictions"][0]["evaluationExampleReference"]
        with self.assertRaises(evaluator.BlindExpandedV2Error):
            evaluator._validate_prediction_vectors(duplicate["payload"], self.sealed)

        missing = copy.deepcopy(predictions)
        missing["payload"]["predictions"] = missing["payload"]["predictions"][:-1]
        with self.assertRaises(evaluator.BlindExpandedV2Error):
            evaluator._validate_prediction_vectors(missing["payload"], self.sealed)

        wrong_count = copy.deepcopy(predictions)
        wrong_count["payload"]["predictions"].append(copy.deepcopy(wrong_count["payload"]["predictions"][0]))
        with self.assertRaises(evaluator.BlindExpandedV2Error):
            evaluator._validate_prediction_vectors(wrong_count["payload"], self.sealed)

    def test_fail_closed_outer_authority_digests(self):
        for filename, payload_key in (
            ("ground-truth.v2.json", "authorityPayload"),
            ("sealed-evaluation-authority.v2.json", "authorityPayload"),
        ):
            with tempfile.TemporaryDirectory(prefix="him-p2-v2-authority-") as directory:
                copied = pathlib.Path(directory)
                shutil.copytree(SEALED_ROOT, copied / "sealed")
                target = copied / "sealed" / filename
                value = json.loads(target.read_text(encoding="utf-8"))
                value[payload_key]["__tampered__"] = True
                target.write_text(json.dumps(value, ensure_ascii=False), encoding="utf-8")
                with self.assertRaises(evaluator.BlindExpandedV2Error):
                    evaluator.load_sealed_v2_set(copied / "sealed")

    def test_fail_closed_checkpoint_binding(self):
        with tempfile.TemporaryDirectory(prefix="him-p2-v2-checkpoint-") as directory:
            path = pathlib.Path(directory) / "checkpoint-manifest.json"
            path.write_text(json.dumps({"checkpointReference": "wrong", "checkpointLogicalDigest": "0" * 64}), encoding="utf-8")
            with self.assertRaises(evaluator.BlindExpandedV2Error):
                evaluator.validate_checkpoint_manifest(path)
            path.write_text(json.dumps({"checkpointReference": evaluator.EXPECTED_CHECKPOINT_REFERENCE, "checkpointLogicalDigest": "0" * 64}), encoding="utf-8")
            with self.assertRaises(evaluator.BlindExpandedV2Error):
                evaluator.validate_checkpoint_manifest(path)


if __name__ == "__main__":
    unittest.main()
