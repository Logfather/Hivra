from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from him_trainer.final_evaluation_authority_v1 import (
    FINAL_CHECKPOINT_LOGICAL_DIGEST,
    FINAL_CHECKPOINT_REFERENCE,
    FINAL_HOLDOUT_AUTHORITY_LOGICAL_DIGEST,
    FINAL_HOLDOUT_AUTHORITY_REFERENCE,
    FINAL_MODEL_STATE_SHA256,
    build_final_evaluation_authority_v1,
    persist_final_evaluation_authority_v1,
    preflight_final_evaluation_authority_v1,
    reload_final_evaluation_authority_v1,
    validate_final_evaluation_authority_v1,
)


ROOT = Path(__file__).resolve().parents[3]


class FinalEvaluationAuthorityV1Test(unittest.TestCase):
    def test_authority_binds_final_checkpoint_and_opaque_holdout_only(self) -> None:
        value = build_final_evaluation_authority_v1()
        validate_final_evaluation_authority_v1(value)
        payload = value["authorityPayload"]
        self.assertEqual(FINAL_CHECKPOINT_REFERENCE, payload["evaluatedCheckpoint"]["checkpointReference"])
        self.assertEqual(FINAL_CHECKPOINT_LOGICAL_DIGEST, payload["evaluatedCheckpoint"]["checkpointLogicalDigest"])
        self.assertEqual(FINAL_MODEL_STATE_SHA256, payload["evaluatedCheckpoint"]["modelStateSha256"])
        self.assertEqual(FINAL_HOLDOUT_AUTHORITY_REFERENCE, payload["holdoutAuthority"]["reference"])
        self.assertEqual(FINAL_HOLDOUT_AUTHORITY_LOGICAL_DIGEST, payload["holdoutAuthority"]["logicalDigest"])
        self.assertFalse(payload["holdoutAuthority"]["membershipIncluded"])
        self.assertFalse(payload["holdoutAuthority"]["labelsIncluded"])
        self.assertNotIn("records", payload["holdoutAuthority"])
        self.assertNotIn("examples", payload["holdoutAuthority"])
        self.assertEqual(0, payload["holdoutAccess"]["holdoutFileOpenCount"])
        self.assertEqual(0, payload["holdoutAccess"]["holdoutContentReadCount"])
        self.assertEqual(0, payload["holdoutAccess"]["holdoutDeserializationCount"])
        self.assertEqual(0, payload["holdoutAccess"]["holdoutExposureCount"])
        self.assertFalse(payload["holdoutAccess"]["holdoutOpened"])

    def test_rejects_any_new_semantic_component(self) -> None:
        value = build_final_evaluation_authority_v1()
        value["authorityPayload"]["componentReuse"]["newSemanticComponentRequiredCount"] = 1
        with self.assertRaises(ValueError):
            validate_final_evaluation_authority_v1(value)

    def test_persist_reload_and_model_free_preflight(self) -> None:
        value = build_final_evaluation_authority_v1()
        with tempfile.TemporaryDirectory(prefix="him-final-evaluation-authority-") as directory:
            path = Path(directory) / "final-evaluation-authority.v1.json"
            persist_final_evaluation_authority_v1(path, value)
            self.assertEqual(value, reload_final_evaluation_authority_v1(path))
            result = preflight_final_evaluation_authority_v1(ROOT, path)
            self.assertEqual("FINAL_EVALUATION_PREFLIGHT_PASS", result["state"])
            self.assertEqual(0, result["missingInputCount"])
            self.assertEqual(0, result["modelDeserializationCount"])
            self.assertEqual(0, result["forwardCount"])
            self.assertEqual(0, result["inferenceCount"])
            self.assertEqual(0, result["trainingCount"])
            self.assertFalse(result["holdoutOpened"])
            self.assertEqual(0, result["holdoutExposureCount"])


if __name__ == "__main__":
    unittest.main()
