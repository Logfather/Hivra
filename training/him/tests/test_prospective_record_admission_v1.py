from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from him_trainer.prospective_record_admission_v1 import build_ledger, logical_digest, persist_and_reload, validate_ledger


class ProspectiveRecordAdmissionV1Test(unittest.TestCase):
    def test_exact_fifteen_records_and_stable_ids(self) -> None:
        value = build_ledger(); validate_ledger(value)
        records = value["payload"]["records"]
        self.assertEqual(len(records), 15)
        self.assertEqual(len({r["intakeRecordId"] for r in records}), 15)
        self.assertEqual(len({r["rawEvidenceLogicalDigest"] for r in records}), 15)

    def test_temporal_evidence_and_scopes_are_preserved(self) -> None:
        payload = build_ledger()["payload"]
        self.assertEqual(payload["intakeCreatedAtUtc"], "2026-09-16T09:16:46Z")
        self.assertEqual(payload["sourceIntakeNotBeforeUtc"], "2026-09-16T09:02:22Z")
        self.assertTrue(all(r["evidenceSufficiency"] == "INSUFFICIENT_FOR_LATER_REVIEW" for r in payload["records"]))
        self.assertTrue(all(r["groundTruthState"] == "NOT_ASSIGNED" for r in payload["records"]))

    def test_no_collisions_or_model_access_and_empty_accepted_population(self) -> None:
        payload = build_ledger()["payload"]
        self.assertEqual(payload["acceptedRecordCount"], 0)
        self.assertEqual(payload["pendingEvidenceRecordCount"], 15)
        self.assertTrue(all(not r["trainCollision"] and not r["validationCollision"] and r["modelExposureCount"] == 0 for r in payload["records"]))

    def test_persist_reload_digest_and_clean_reconstruction(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "ledger.json"
            first = persist_and_reload(path)
            second = json.loads(path.read_text(encoding="utf-8"))
            self.assertEqual(first, second)
            self.assertEqual(first["logicalDigest"], logical_digest(first["payload"]))

    def test_rejects_tampering(self) -> None:
        value = build_ledger(); value["payload"]["recordCount"] = 16
        with self.assertRaises(ValueError): validate_ledger(value)


if __name__ == "__main__":
    unittest.main()
