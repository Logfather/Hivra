import json
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
AUTH = ROOT / "data/knowledge/him/training/p2/canonical-catalog-expansion/v3/controlled-pair-supervision-authority-v1.json"


class ControlledPairSupervisionAuthorityV1Test(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.authority = json.loads(AUTH.read_text(encoding="utf-8"))

    def test_version_and_schema_are_frozen(self):
        self.assertEqual(self.authority["version"], 1)
        self.assertEqual(len(self.authority["recordSchema"]), 10)

    def test_evidence_is_required(self):
        self.assertTrue(self.authority["evidenceRequired"])
        self.assertTrue(self.authority["lexicalOnlyLabelAssignmentForbidden"])

    def test_all_relation_classes_are_allowed(self):
        self.assertEqual(set(self.authority["allowedRelationClasses"]), {"IDENTITY", "VARIANT", "COMPATIBLE", "REJECT"})

    def test_pairwise_swap_is_required(self):
        self.assertTrue(self.authority["sameObservedMultiCandidateRequired"])
        self.assertTrue(self.authority["controlledCompatibleRejectSwapRequired"])

    def test_grouping_key_is_present(self):
        self.assertTrue(self.authority["groupingKey"])

    def test_future_development_requirements_are_frozen(self):
        self.assertTrue(self.authority["futureDevelopmentControlledSwapRequired"])
        self.assertTrue(self.authority["futureDevelopmentNonSelfPairRequired"])
        self.assertEqual(self.authority["visibleContradictionsRequired"], 0)

    def test_no_speculative_records_materialized(self):
        self.assertEqual(self.authority["materializedRecordCount"], 0)

    def test_required_lineage_fields_exist(self):
        for field in ("evidenceReferences", "semanticGroupingKey", "partitionGroupingKey", "lineageDigest"):
            self.assertIn(field, self.authority["recordSchema"])

    def test_authority_is_json_object(self):
        self.assertIsInstance(self.authority, dict)

    def test_authority_id_is_versioned(self):
        self.assertEqual(self.authority["authorityId"], "CONTROLLED_PAIR_SUPERVISION_AUTHORITY_V1")
