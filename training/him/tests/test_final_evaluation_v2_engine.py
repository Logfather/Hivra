import tempfile
import unittest
from pathlib import Path

from him_trainer.final_evaluation_v2_engine import (
    ExactlyOnceState,
    FinalEvaluationV2Error,
    build_derived_execution_artifact,
    build_model_input_projection_authority,
    mask_secondary,
    persist_write_once,
    project_model_input,
    score_predictions,
    validate_envelope,
)


class FinalEvaluationV2EngineTest(unittest.TestCase):
    def _record(self, record_id="r1"):
        return {
            "recordId": record_id,
            "sourceRecordId": "source:" + record_id,
            "inputRepresentationV2": {
                "observedTerm": "Example",
                "sourceFaithfulEvidence": {"sourceFaithfulLabel": "Example", "productType": "food"},
            },
            "groundTruth": {"targetKind": "IDENTITY", "candidateCompatibility": "NOT_APPLICABLE"},
            "groundTruthReference": "truth:v1:digest",
            "humanReviewResultReference": "review:v1:digest",
            "familyLineageReference": "family:v1:digest",
        }

    def _candidate(self):
        return {"canonicalName": "Example", "normalizedName": "example", "taxonomyPaths": [["food"]]}

    def test_projection_and_reconstruction_are_deterministic(self):
        row = self._record()
        first = project_model_input(row, self._candidate())
        second = project_model_input(row, self._candidate())
        self.assertEqual(first, second)
        authority = build_model_input_projection_authority("holdout:v1:d", "d", [first])
        artifact = build_derived_execution_artifact(
            {"reference": "holdout:v1:d", "logicalDigest": "d", "payload": {"records": [row]}},
            authority,
            [first],
        )
        self.assertEqual(artifact["payload"]["recordCount"], 1)
        validate_envelope(artifact)

    def test_masked_secondary_is_excluded(self):
        self.assertFalse(mask_secondary("NOT_APPLICABLE"))
        result = score_predictions(
            [self._record()],
            [{"primaryPrediction": "IDENTITY", "secondaryPrediction": "COMPATIBLE"}],
        )
        self.assertEqual(result["primaryCorrectCount"], 1)
        self.assertEqual(result["secondaryActiveCount"], 0)

    def test_exactly_once_and_write_once(self):
        state = ExactlyOnceState()
        state.authorize_invocation()
        state.expose_once()
        with self.assertRaises(FinalEvaluationV2Error):
            state.authorize_invocation()
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "result.json"
            persist_write_once(path, "result", {"state": "COMPLETE"})
            with self.assertRaises(FinalEvaluationV2Error):
                persist_write_once(path, "result", {"state": "COMPLETE"})


if __name__ == "__main__":
    unittest.main()
