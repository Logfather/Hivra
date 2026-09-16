from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from him_trainer.prospective_visual_evidence_supplement_v1 import build_supplement, build_updated_ledger
from him_trainer.prospective_record_admission_v1 import build_ledger
from him_trainer.prospective_human_review_packet_v1 import build_packet, logical_digest, validate_packet


class ProspectiveHumanReviewPacketV1Test(unittest.TestCase):
    def test_packet_is_complete_model_blind_and_has_no_decisions(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); images = root / "images"; durable = root / "durable"; images.mkdir(); durable.mkdir()
            for i in range(1, 16): (images / f"pic_{i:02d}.jpg").write_bytes(b"image" + bytes([i]))
            supplement = build_supplement(images, durable); updated = build_updated_ledger(build_ledger(), supplement)
            packet = build_packet(updated, supplement); validate_packet(packet)
            self.assertEqual(packet["payload"]["recordCount"], 15)
            self.assertEqual(packet["payload"]["familyResolutionModeCounts"]["HUMAN_ADJUDICATION_REQUIRED"], 15)
            self.assertTrue(all(u["reviewCompleteness"] == "PENDING_HUMAN_REVIEW" for u in packet["payload"]["units"]))
            self.assertFalse(packet["payload"]["modelOutputVisible"])
            self.assertEqual(packet["logicalDigest"], logical_digest(packet["payload"]))

    def test_packet_rejects_scientific_decision_injection(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); images = root / "images"; durable = root / "durable"; images.mkdir(); durable.mkdir()
            for i in range(1, 16): (images / f"pic_{i:02d}.jpg").write_bytes(b"image" + bytes([i]))
            packet = build_packet(build_updated_ledger(build_ledger(), build_supplement(images, durable)), build_supplement(images, durable))
            packet["payload"]["units"][0]["familyReview"]["decision"] = "family"
            with self.assertRaises(ValueError): validate_packet(packet)


if __name__ == "__main__": unittest.main()
