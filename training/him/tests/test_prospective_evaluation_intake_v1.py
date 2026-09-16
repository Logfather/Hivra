from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from him_trainer.prospective_evaluation_intake_v1 import build_authority, logical_digest, persist_and_reload, validate_authority


class ProspectiveEvaluationIntakeV1Test(unittest.TestCase):
    def test_empty_authority_serializes_reloads_and_is_digest_valid(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            value = persist_and_reload(Path(directory) / "authority.json")
            self.assertEqual(value["logicalDigest"], logical_digest(value["payload"]))
            self.assertEqual(value["payload"]["initialState"]["recordCount"], 0)

    def test_temporal_and_holdout_separation_are_frozen(self) -> None:
        value = build_authority(); validate_authority(value)
        payload = value["payload"]
        self.assertEqual(payload["sourceCreatedAtUtc"], payload["sourceIntakeNotBeforeUtc"])
        self.assertTrue(payload["isolationPolicy"]["consumedHoldoutSourceLevelTemporalSeparation"])
        self.assertEqual(payload["isolationPolicy"]["consumedHoldoutRecordLevelIsolation"], "PENDING_FUTURE_RECORD_ADMISSION")

    def test_identity_and_duplicate_policy_are_model_and_ground_truth_free(self) -> None:
        payload = build_authority()["payload"]
        self.assertTrue(payload["recordIdentityPolicy"]["modelIndependent"])
        self.assertTrue(payload["recordIdentityPolicy"]["groundTruthIndependent"])
        self.assertFalse(payload["duplicatePolicy"]["modelDecision"])
        self.assertIn("GTIN", payload["recordIdentityPolicy"]["distinctFrom"])

    def test_isolation_model_and_evidence_policies(self) -> None:
        p = build_authority()["payload"]
        self.assertTrue(p["isolationPolicy"]["trainCollisionScreenRequired"])
        self.assertTrue(p["isolationPolicy"]["validationCollisionScreenRequired"])
        self.assertFalse(p["modelPolicy"]["accessBeforeGroundTruthFreezeAllowed"])
        self.assertTrue(p["evidencePolicy"]["sourceFaithfulEvidenceRequired"])

    def test_authority_is_immutable_empty_root_and_does_not_bind_delta(self) -> None:
        p = build_authority()["payload"]
        self.assertFalse(p["modelPolicy"]["inferenceDuringIntakeAllowed"])
        self.assertIsNone(p["freshDeltaBinding"])
        self.assertFalse(p["minimumFutureEvaluation"]["artificialRecordCreationAllowed"])

    def test_rejects_tampered_authority(self) -> None:
        value = build_authority(); value["payload"]["initialState"]["recordCount"] = 1
        with self.assertRaises(ValueError): validate_authority(value)

    def test_clean_environment_reloads_the_same_deterministic_authority(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "authority.json"
            first = persist_and_reload(path)
            second = json.loads(path.read_text(encoding="utf-8"))
            self.assertEqual(first, second)
            self.assertEqual(first["logicalDigest"], build_authority()["logicalDigest"])


if __name__ == "__main__":
    unittest.main()
