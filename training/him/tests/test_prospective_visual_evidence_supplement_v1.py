from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from him_trainer.prospective_record_admission_v1 import build_ledger
from him_trainer.prospective_visual_evidence_supplement_v1 import (
    PARENT_REFERENCE,
    build_supplement,
    build_updated_ledger,
    logical_digest,
    persist_and_reload,
    pre_ground_truth_population,
    validate_supplement,
    validate_updated_ledger,
)


class ProspectiveVisualEvidenceSupplementV1Test(unittest.TestCase):
    def _fixtures(self, root: Path) -> tuple[Path, Path, Path]:
        images = root / "images"; durable = root / "durable"; ledger = root / "ledger.json"
        images.mkdir(); durable.mkdir()
        for i in range(1, 16):
            (images / f"pic_{i:02d}.jpg").write_bytes(b"fake-jpeg-" + str(i).encode())
        original = build_ledger()
        ledger.write_text(json.dumps(original), encoding="utf-8")
        return images, durable, ledger

    def test_exact_mapping_hashes_and_no_unsupported_enrichment(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            images, durable, ledger = self._fixtures(Path(directory))
            supplement = build_supplement(images, durable)
            validate_supplement(supplement)
            self.assertEqual([r["observationKey"] for r in supplement["payload"]["records"]], [f"OBS-{i:03d}" for i in range(1, 16)])
            self.assertEqual([r["imageFilename"] for r in supplement["payload"]["records"]], [f"pic_{i:02d}.jpg" for i in range(1, 16)])
            self.assertTrue(all(not r["ocrUsed"] and not r["externalEnrichmentUsed"] for r in supplement["payload"]["records"]))
            self.assertEqual(len({r["sha256"] for r in supplement["payload"]["records"]}), 15)

    def test_parent_ledger_is_unchanged_and_updated_state_is_pre_ground_truth(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            images, durable, ledger_path = self._fixtures(Path(directory))
            original = json.loads(ledger_path.read_text())
            supplement = build_supplement(images, durable)
            updated = build_updated_ledger(original, supplement)
            validate_updated_ledger(updated)
            self.assertEqual(original["reference"], PARENT_REFERENCE)
            self.assertEqual(original["logicalDigest"], PARENT_REFERENCE.rsplit(":", 1)[1])
            self.assertEqual(updated["payload"]["recordCount"], 15)
            self.assertEqual(updated["payload"]["acceptedRecordCount"], 15)
            self.assertEqual(updated["payload"]["familyAssignmentCount"], 0)
            self.assertEqual(updated["payload"]["groundTruthAssignmentCount"], 0)
            self.assertEqual(pre_ground_truth_population(updated)["recordCount"], 15)

    def test_persist_reload_and_reconstruction_are_deterministic(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); images, durable, ledger = self._fixtures(root)
            first = persist_and_reload(images, durable, ledger, root / "supplement.json", root / "updated.json")
            second = persist_and_reload(images, durable, ledger, root / "supplement.json", root / "updated.json")
            self.assertEqual(first, second)
            self.assertEqual(first[0]["logicalDigest"], logical_digest(first[0]["payload"]))
            self.assertEqual(first[1]["logicalDigest"], logical_digest(first[1]["payload"]))
            self.assertEqual(first[2], second[2])


if __name__ == "__main__":
    unittest.main()
