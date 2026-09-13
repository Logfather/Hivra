import unittest

from him_trainer.corpus_coverage_v2 import (
    CORPUS_COVERAGE_V2_ID,
    CorpusCoverageV2Error,
    CorpusExampleV2,
    assess_coverage,
    build_coverage_report,
    candidate_shortcut_diagnostic,
    corpus_coverage_contract_v2,
    validate_contract_bindings_v2,
)
from him_trainer.evidence_projection_v2 import HimEvidenceProjectionV2
from him_trainer.input_representation_v2 import build_input_representation_v2
from him_trainer.partition_leakage_v2 import REQUIRED_ISOLATION_UNITS_V2


def example(
    ref: str,
    family: str,
    candidate: str,
    *,
    primary: str | None = None,
    secondary: str | None = None,
    boundary: str | None = None,
    group: str | None = None,
    partition: str = "TRAIN",
    term: str | None = None,
    label: str | None = None,
    length: int = 32,
) -> CorpusExampleV2:
    projection = HimEvidenceProjectionV2(
        source_faithful_label=label or term or ref,
        categories=("en:food",),
        food_groups=("en:group",),
        product_type="food",
    )
    candidate_mapping = {
        "itemname": candidate,
        "normalized": candidate.lower(),
        "taxonomyPaths": (("food", "candidate"),),
    }
    model_input = build_input_representation_v2(term or ref, candidate_mapping, projection.model_input_mapping())
    return CorpusExampleV2(
        example_reference=ref,
        family_reference=family,
        source_record_identity=f"source:{ref}",
        evidence_projection_reference=projection.reference,
        model_input=model_input,
        candidate_reference=f"candidate:{candidate}",
        source="OPEN_FOOD_FACTS",
        partition=partition,
        primary_target=primary,
        secondary_target=secondary,
        boundary_type=boundary,
        contrast_group_reference=group,
        sequence_length=length,
    )


class CorpusCoverageV2Test(unittest.TestCase):
    def test_contract_is_bound_and_deterministic(self):
        first = corpus_coverage_contract_v2()
        second = corpus_coverage_contract_v2()
        self.assertEqual(first.contract_id, CORPUS_COVERAGE_V2_ID)
        self.assertEqual(first.reference, second.reference)
        self.assertEqual(tuple(first.required_isolation_units), tuple(unit.value for unit in REQUIRED_ISOLATION_UNITS_V2))
        validate_contract_bindings_v2(first)

    def test_target_and_family_coverage_is_explicit(self):
        values = [
            example("i1", "family:i", "candidate:i", primary="IDENTITY", secondary="COMPATIBLE", group="p1"),
            example("v1", "family:v", "candidate:v", primary="VARIANT", secondary="COMPATIBLE", group="p1"),
            example("r1", "family:r", "candidate:r", secondary="REJECT", boundary="WRONG_SCOPE", group="s1"),
        ]
        report = build_coverage_report(values)
        self.assertEqual(report.primary_counts, {"IDENTITY": 1, "VARIANT": 1})
        self.assertEqual(report.secondary_counts, {"COMPATIBLE": 2, "REJECT": 1})
        self.assertEqual(len(report.family_counts), 3)

    def test_primary_contrast_degeneracy_is_rejected(self):
        values = [
            example("i1", "family:i", "candidate:i", primary="IDENTITY", group="identity-only"),
            example("v1", "family:v", "candidate:v", primary="VARIANT", group="variant-only"),
        ]
        assessment = assess_coverage(values)
        self.assertFalse(assessment.contrastive_coverage_valid)
        self.assertEqual(assessment.quality_status, "REQUIRES_ADDITIONAL_REVIEWED_EXAMPLES")

    def test_secondary_contrast_degeneracy_is_rejected(self):
        values = [
            example("c1", "family:c", "candidate:c", secondary="COMPATIBLE", group="compatible-only"),
            example("r1", "family:r", "candidate:r", secondary="REJECT", boundary="WRONG_SCOPE", group="reject-only"),
        ]
        assessment = assess_coverage(values)
        self.assertFalse(assessment.contrastive_coverage_valid)

    def test_candidate_shortcut_is_diagnostic(self):
        values = [
            example(f"r{i}", f"family:r{i}", "candidate:austern", secondary="REJECT", boundary="WRONG_RELATION_LEVEL")
            for i in range(6)
        ] + [
            example("r6", "family:ricotta", "candidate:ricotta", secondary="REJECT", boundary="WRONG_RELATION_LEVEL")
        ]
        self.assertEqual(candidate_shortcut_diagnostic(build_coverage_report(values)), "HIGH")

    def test_primary_conflicting_input_is_hard_blocker(self):
        shared = example("a", "family:a", "candidate:a", primary="IDENTITY", group="same")
        conflicting = example("b", "family:a", "candidate:a", primary="VARIANT", group="same")
        # Force exact model-facing equality while preserving distinct records.
        conflicting = CorpusExampleV2(
            **{**conflicting.__dict__, "model_input": shared.model_input}
        )
        assessment = assess_coverage([shared, conflicting])
        self.assertEqual(assessment.report.primary_conflicting_target_group_count, 1)
        self.assertEqual(assessment.quality_status, "BLOCKED_BY_CONFLICTING_INPUT")

    def test_secondary_conflicting_input_is_hard_blocker(self):
        compatible = example("c", "family:c", "candidate:c", secondary="COMPATIBLE")
        reject = CorpusExampleV2(
            **{**example("r", "family:r", "candidate:r", secondary="REJECT", boundary="WRONG_SCOPE").__dict__, "model_input": compatible.model_input}
        )
        assessment = assess_coverage([compatible, reject])
        self.assertEqual(assessment.report.secondary_conflicting_target_group_count, 1)

    def test_partition_leakage_is_integrated(self):
        first = example("a", "family:a", "candidate:a", primary="IDENTITY")
        second = CorpusExampleV2(
            **{**example("b", "family:b", "candidate:b", primary="VARIANT", partition="VALIDATION").__dict__, "family_reference": "family:a"}
        )
        assessment = assess_coverage([first, second])
        self.assertFalse(assessment.leakage_valid)

    def test_validation_must_measure_all_four_targets(self):
        train = [
            example("i", "family:i", "candidate:i", primary="IDENTITY", secondary="COMPATIBLE", group="p"),
            example("v", "family:v", "candidate:v", primary="VARIANT", secondary="COMPATIBLE", group="p"),
            example("r", "family:r", "candidate:r", secondary="REJECT", boundary="WRONG_SCOPE", group="s"),
        ]
        validation = [example("vi", "family:vi", "candidate:vi", primary="IDENTITY", secondary="COMPATIBLE")]
        self.assertFalse(assess_coverage(train, validation_examples=validation).validation_measurable)

    def test_sequence_overflow_fails_closed(self):
        value = example("long", "family:long", "candidate:long", primary="IDENTITY", length=257)
        assessment = assess_coverage([value])
        self.assertFalse(assessment.sequence_valid)
        self.assertEqual(assessment.quality_status, "BLOCKED_BY_SEQUENCE_LENGTH")

    def test_target_leakage_is_not_model_visible(self):
        value = example("safe", "family:safe", "candidate:safe", primary="IDENTITY", secondary="COMPATIBLE")
        report = build_coverage_report([value])
        self.assertTrue(all(count == 0 for count in report.input_scan.values()))
        self.assertNotIn("IDENTITY", value.model_input.serialize())
        self.assertNotIn("COMPATIBLE", value.model_input.serialize())

    def test_deterministic_context_identity(self):
        first = example("same", "family:same", "candidate:same", primary="IDENTITY")
        second = example("same", "family:same", "candidate:same", primary="IDENTITY")
        self.assertEqual(first.decision_context_reference, second.decision_context_reference)
        self.assertEqual(first.evidence_projection_reference, second.evidence_projection_reference)

    def test_invalid_boundary_is_rejected(self):
        with self.assertRaises(CorpusCoverageV2Error):
            example("bad", "family:bad", "candidate:bad", secondary="COMPATIBLE", boundary="INVENTED")

    def test_missing_reject_boundary_remains_non_constructible(self):
        report = build_coverage_report([example("r", "family:r", "candidate:r", secondary="REJECT")])
        wrong_scope = next(item for item in report.boundary_coverage if item.boundary_type == "WRONG_SCOPE")
        self.assertEqual(wrong_scope.example_count, 0)
        self.assertFalse(wrong_scope.constructible)


if __name__ == "__main__":
    unittest.main()
