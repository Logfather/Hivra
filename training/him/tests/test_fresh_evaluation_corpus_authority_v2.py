from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from him_trainer.fresh_evaluation_corpus_authority_v2 import (
    EXECUTION_CONTRACT_DIGEST,
    REQUIREMENT_SET_DIGEST,
    build_design_authority,
    logical_digest,
    persist_and_reload,
    validate_design_authority,
)


class FreshEvaluationCorpusAuthorityV2Test(unittest.TestCase):
    def test_design_authority_is_model_free_and_selection_empty(self) -> None:
        value = build_design_authority()
        validate_design_authority(value)
        payload = value["payload"]
        self.assertEqual(payload["executionContractLogicalDigest"], EXECUTION_CONTRACT_DIGEST)
        self.assertEqual(payload["requirementSetLogicalDigest"], REQUIREMENT_SET_DIGEST)
        self.assertEqual(payload["selection"], {"finalHoldoutSelectionInThisAuthority": False, "recordCount": 0, "familyCount": 0})
        self.assertFalse(payload["scientificIndependence"]["usesModelOutput"])
        self.assertFalse(payload["scientificIndependence"]["usesConsumedHoldoutContent"])
        self.assertEqual(len(payload["admissibilityRules"]), 5)
        self.assertEqual(len(payload["forbiddenSourceTypes"]), 9)

    def test_persist_reload_digest_and_clean_environment(self) -> None:
        with tempfile.TemporaryDirectory(prefix="him-fresh-eval-authority-") as directory:
            path = Path(directory) / "fresh-evaluation-corpus-design-authority.v2.json"
            loaded = persist_and_reload(path)
            self.assertTrue(path.is_file())
            self.assertEqual(loaded["logicalDigest"], logical_digest(loaded["payload"]))
            self.assertEqual(json.loads(path.read_text(encoding="utf-8")), loaded)

    def test_rejects_selection_or_digest_tampering(self) -> None:
        value = build_design_authority()
        value["payload"]["selection"]["recordCount"] = 6
        with self.assertRaises(ValueError):
            validate_design_authority(value)


if __name__ == "__main__":
    unittest.main()

