import unittest

from him_trainer.evidence_projection_v2 import project_evidence_v2, project_to_input_v2
from him_trainer.input_representation_v2 import build_input_representation_v2
from him_trainer.partition_leakage_v2 import (
    HEURISTIC_SEMANTIC_LEAKAGE_MATCHING,
    IsolationUnitV2,
    LeakageValidationAuthorityV2,
    PARTITION_STRATEGY_V2,
    PartitionAuthorityV2,
    PartitionLeakageExampleV2,
    PartitionLeakageV2Error,
    REQUIRED_ISOLATION_UNITS_V2,
    component_statistics,
    connected_components,
    decision_context_reference_v2,
    expected_representation_binding_v2,
    isolation_group_reference_v2,
    validate_partition_assignments,
    validate_representation_binding_v2,
)


class PartitionLeakageV2Test(unittest.TestCase):
    candidate = {"itemname": "Pomelos", "normalized": "pomelos", "taxonomyPaths": [["fruit"]]}

    def example(
        self,
        reference: str,
        partition: str,
        family: str,
        source: str,
        label: str = "Pomelo",
        target: object = "IDENTITY",
    ) -> PartitionLeakageExampleV2:
        projection = project_evidence_v2(
            "OPEN_FOOD_FACTS",
            {"identity": {"productName": label, "productType": "food"}},
            source_record_identity=source,
        )
        model_input = project_to_input_v2("Pomelo", self.candidate, projection)
        return PartitionLeakageExampleV2(
            record_reference=reference,
            partition=partition,
            family_reference=family,
            source_record_identity=source,
            evidence_projection_reference=projection.reference,
            model_input=model_input,
            target=target,
        )

    def test_each_required_crossing_is_rejected(self) -> None:
        pairs = (
            (IsolationUnitV2.FAMILY, self.example("a", "TRAIN", "family-a", "source-a"), self.example("b", "VALIDATION", "family-a", "source-b")),
            (IsolationUnitV2.SOURCE_RECORD, self.example("a", "TRAIN", "family-a", "source-a"), self.example("b", "VALIDATION", "family-b", "source-a")),
            (IsolationUnitV2.EVIDENCE_PROJECTION, self.example("a", "TRAIN", "family-a", "source-a", "same"), self.example("b", "VALIDATION", "family-b", "source-b", "same")),
            (IsolationUnitV2.DECISION_CONTEXT, self.example("a", "TRAIN", "family-a", "source-a"), self.example("b", "VALIDATION", "family-b", "source-b")),
        )
        for unit, left, right in pairs:
            result = validate_partition_assignments((left, right), (unit,))
            self.assertFalse(result.valid, unit)
            self.assertEqual(result.diagnostics[0].isolation_unit, unit)

    def test_legitimate_shared_vocabulary_is_not_a_false_positive(self) -> None:
        left = self.example("a", "TRAIN", "family-a", "source-a", "same word A")
        right = self.example("b", "VALIDATION", "family-b", "source-b", "same word B")
        result = validate_partition_assignments((left, right))
        self.assertTrue(result.valid)
        self.assertEqual(HEURISTIC_SEMANTIC_LEAKAGE_MATCHING, "FORBIDDEN")

    def test_targets_do_not_change_leakage_identity(self) -> None:
        left = self.example("a", "TRAIN", "family-a", "source-a", target="TARGET_A")
        right = self.example("a-copy", "VALIDATION", "family-a", "source-a", target="TARGET_B")
        self.assertEqual(left.decision_context_reference, right.decision_context_reference)
        self.assertEqual(isolation_group_reference_v2(left), isolation_group_reference_v2(right))

    def test_source_provenance_is_audit_only(self) -> None:
        projection = project_evidence_v2(
            "OPEN_FOOD_FACTS", {"identity": {"productName": "Pomelo"}}, source_record_identity="off:record"
        )
        self.assertEqual(projection.provenance.source_record_identity, "off:record")
        representation = project_to_input_v2("Pomelo", self.candidate, projection)
        self.assertNotIn("off:record", representation.serialize())
        self.assertNotIn("OPEN_FOOD_FACTS", representation.serialize())

    def test_transitive_components_are_not_split(self) -> None:
        a = self.example("a", "TRAIN", "family-a", "source-a", "A")
        b = self.example("b", "TRAIN", "family-a", "source-b", "B")
        c = self.example("c", "VALIDATION", "family-c", "source-b", "B")
        groups = connected_components((a, b, c), (IsolationUnitV2.FAMILY, IsolationUnitV2.SOURCE_RECORD))
        self.assertEqual([len(group) for group in groups], [3])
        result = validate_partition_assignments((a, b, c), (IsolationUnitV2.FAMILY, IsolationUnitV2.SOURCE_RECORD))
        self.assertFalse(result.valid)

    def test_component_stats_and_reloadable_authorities_are_deterministic(self) -> None:
        a = self.example("a", "TRAIN", "family-a", "source-a")
        b = self.example("b", "VALIDATION", "family-b", "source-b", "Other")
        self.assertEqual(component_statistics((a, b)), {"componentCount": 2, "largestComponentSize": 1, "medianComponentSize": 1})
        binding = tuple(sorted(expected_representation_binding_v2().items()))
        authority = PartitionAuthorityV2(
            corpus_identity="corpus:v2:test",
            partition_strategy=PARTITION_STRATEGY_V2,
            isolation_units=REQUIRED_ISOLATION_UNITS_V2,
            split_membership=(("a", "TRAIN"), ("b", "VALIDATION")),
            grouping_identities=(("a", isolation_group_reference_v2(a)), ("b", isolation_group_reference_v2(b))),
            representation_binding=binding,
        )
        self.assertEqual(authority.logical_digest, PartitionAuthorityV2(
            corpus_identity="corpus:v2:test",
            partition_strategy=PARTITION_STRATEGY_V2,
            isolation_units=REQUIRED_ISOLATION_UNITS_V2,
            split_membership=(("a", "TRAIN"), ("b", "VALIDATION")),
            grouping_identities=(("a", isolation_group_reference_v2(a)), ("b", isolation_group_reference_v2(b))),
            representation_binding=binding,
        ).logical_digest)
        validation = LeakageValidationAuthorityV2("partition:v2:test", REQUIRED_ISOLATION_UNITS_V2, (("FAMILY", 0),), (), "PASS")
        self.assertTrue(validation.reference.startswith("him-p2-leakage-validation:v2:"))

    def test_representation_and_projection_binding_mismatches_fail_closed(self) -> None:
        validate_representation_binding_v2(expected_representation_binding_v2())
        for key in ("inputRepresentationVersion", "evidenceProjectionVersion"):
            binding = expected_representation_binding_v2()
            binding[key] = "changed"
            with self.assertRaisesRegex(PartitionLeakageV2Error, "REPRESENTATION_BINDING_MISMATCH"):
                validate_representation_binding_v2(binding)

    def test_record_reference_duplicates_and_empty_units_fail_closed(self) -> None:
        example = self.example("same", "TRAIN", "family-a", "source-a")
        duplicate = self.example("same", "VALIDATION", "family-b", "source-b", "Other")
        self.assertFalse(validate_partition_assignments((example, duplicate), (IsolationUnitV2.FAMILY,)).valid)
        with self.assertRaisesRegex(PartitionLeakageV2Error, "ISOLATION_UNITS_EMPTY"):
            validate_partition_assignments((example,), ())

    def test_input_identity_uses_canonical_v2_content(self) -> None:
        representation = build_input_representation_v2(
            "Pomelo", self.candidate, {"sourceFaithfulLabel": "Pomelo", "productType": "food"}
        )
        reference = decision_context_reference_v2(representation)
        self.assertEqual(reference.split(":")[0:2], ["him-p2-decision-context", "v2"])
        self.assertEqual(len(reference.split(":")[2]), 64)


if __name__ == "__main__":
    unittest.main()
