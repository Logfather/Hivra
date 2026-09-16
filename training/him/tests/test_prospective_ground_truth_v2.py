from __future__ import annotations

import unittest
from him_trainer.prospective_ground_truth_v2 import DECISIONS, build_corpus, build_ground_truth, build_lineage, build_result, validate_result


class ProspectiveGroundTruthV2Test(unittest.TestCase):
    def test_exact_human_control_totals_and_garnelen_binding(self) -> None:
        result = validate_result(build_result())
        rows = result["payload"]["records"]
        self.assertEqual(len(rows), 15)
        self.assertEqual({r["targetKind"] for r in rows}, {"EXISTING_CANONICAL", "IDENTITY", "VARIANT", "NEW_CANONICAL"})
        self.assertEqual(sum(r["targetKind"] == "EXISTING_CANONICAL" for r in rows), 8)
        self.assertEqual(sum(r["targetKind"] == "IDENTITY" for r in rows), 2)
        self.assertEqual(sum(r["targetKind"] == "VARIANT" for r in rows), 3)
        self.assertEqual(sum(r["targetKind"] == "NEW_CANONICAL" for r in rows), 2)
        garnelen = next(r for r in rows if r["observationKey"] == "OBS-011")
        self.assertEqual(garnelen["targetReference"], "canonical-family:v1:canonical:9axGGb")

    def test_lineage_ground_truth_and_corpus_are_model_independent(self) -> None:
        result = build_result(); lineage = build_lineage(result); truth = build_ground_truth(result, lineage); corpus = build_corpus(result, lineage, truth)
        self.assertEqual(lineage["payload"]["recordCount"], 15)
        self.assertEqual(truth["payload"]["resolvedRecordCount"], 15)
        self.assertTrue(truth["payload"]["modelIndependent"])
        self.assertEqual(corpus["payload"]["recordCount"], 15)
        self.assertFalse(corpus["payload"]["artificiallyTrimmed"])
        self.assertEqual(corpus["payload"]["finalHoldoutSelectionCount"], 0)


if __name__ == "__main__": unittest.main()
