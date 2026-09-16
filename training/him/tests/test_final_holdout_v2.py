import unittest

from him_trainer.final_holdout_v2 import (
    REQUIRED_FAMILY_COUNT,
    REQUIRED_RECORD_COUNT,
    build_execution_contract,
    load_frozen_corpus,
    persist_all,
    reload_all,
    select_records,
)


class FinalHoldoutV2Test(unittest.TestCase):
    def test_frozen_corpus_is_complete_and_eligible(self):
        corpus, _, _, truth = load_frozen_corpus()
        self.assertEqual(15, len(corpus["payload"]["records"]))
        self.assertEqual(13, corpus["payload"]["distinctFamilyCount"])
        self.assertEqual(0, truth["payload"]["unresolvedRecordCount"])

    def test_selection_is_deterministic_and_family_covered(self):
        corpus, _, _, _ = load_frozen_corpus()
        first = select_records(corpus); second = select_records(corpus)
        self.assertEqual([r["recordId"] for r in first], [r["recordId"] for r in second])
        self.assertEqual(REQUIRED_RECORD_COUNT, len(first))
        self.assertGreaterEqual(len({r["familyId"] for r in first}), REQUIRED_FAMILY_COUNT)

    def test_materialization_reload_and_fixture_closure(self):
        result = persist_all(); reloaded = reload_all()
        self.assertEqual(6, result["holdout"]["payload"]["recordCount"])
        self.assertEqual("SEALED_UNEXPOSED", reloaded["seal"]["payload"]["state"])
        self.assertEqual("PASS", result["fixture"]["fixtureExecutionPath"])
        self.assertEqual("0", result["fixture"]["unresolvedDependencyCount"])

    def test_current_execution_contract_is_17_inputs_3_outputs(self):
        contract = build_execution_contract()
        self.assertEqual(17, len(contract["payload"]["inputs"]))
        self.assertEqual(3, len(contract["payload"]["outputs"]))

    def test_exposure_remains_zero(self):
        persist_all()
        value = reload_all()
        for name in ("holdout", "authority", "packet", "seal"):
            self.assertFalse(value[name]["payload"].get("holdoutOpened"))
            self.assertEqual(0, value[name]["payload"].get("exposureCount"))


if __name__ == "__main__":
    unittest.main()
