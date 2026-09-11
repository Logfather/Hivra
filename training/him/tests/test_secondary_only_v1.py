import json
import unittest

import torch

from him_trainer.point13_loss_contract_v1 import build_him_masked_multi_objective_loss_contract_v1
from him_trainer.secondary_only_v1 import (
    HimP2PrimaryTargetV1,
    HimP2TrainingExampleV1,
    ROLE_PRIMARY_AND_SECONDARY,
    ROLE_SECONDARY_ONLY,
    SecondaryOnlyContractError,
    build_execution_plan_v1,
    build_dry_run_report_v1,
    build_evaluation_evidence_v1,
    build_him_p2_tensor_batch_v1,
    build_run_evidence_v1,
    compute_him_p2_masked_loss_v1,
    supervision_counts,
    validate_secondary_only_readiness,
)


def example(reference: str, role: str = ROLE_PRIMARY_AND_SECONDARY, target: str = "COMPATIBLE") -> HimP2TrainingExampleV1:
    return HimP2TrainingExampleV1(
        example_reference=reference,
        observed_term="Tortelloni ricotta e spinaci" if role == ROLE_SECONDARY_ONLY else "Ravioli",
        candidate_canonical_id="7B7beg" if role == ROLE_SECONDARY_ONLY else "0efHM5",
        candidate_canonical_name="Ricotta" if role == ROLE_SECONDARY_ONLY else "Ravioli",
        primary_target=None if role == ROLE_SECONDARY_ONLY else HimP2PrimaryTargetV1("VARIANT", "canonical:v1:0efHM5"),
        primary_mask=0.0 if role == ROLE_SECONDARY_ONLY else 1.0,
        secondary_target=target,
        secondary_mask=1.0,
        training_role=role,
        relation="COMPONENT_OR_DERIVED_PRODUCT_OF" if role == ROLE_SECONDARY_ONLY else "VARIANT_OF",
        relation_authority_reference="p2-human-relation:v1:" + "a" * 64,
        candidate_compatibility_authority_reference="p2-human-cc:v1:" + "b" * 64,
        source_evidence_references=("off:product:row:1:code:2",),
        family_reference="family:v1:canonical:7B7beg",
        lineage_reference="p2-lineage:v1:" + "c" * 64,
        catalog_reference="canonical-catalog:v1:" + "d" * 64,
    )


class SecondaryOnlyV1Test(unittest.TestCase):
    def test_secondary_only_contract_is_explicit_and_round_trips(self):
        item = example("secondary-only", ROLE_SECONDARY_ONLY, "REJECT")
        decoded = HimP2TrainingExampleV1.from_dict(json.loads(item.serialize()))
        self.assertEqual(decoded, item)
        self.assertIsNone(decoded.primary_target)
        self.assertEqual(decoded.primary_mask, 0.0)
        self.assertEqual(decoded.secondary_mask, 1.0)
        self.assertEqual(decoded.training_role, ROLE_SECONDARY_ONLY)

    def test_invalid_secondary_only_states_fail_closed(self):
        with self.assertRaises(SecondaryOnlyContractError):
            example("bad", ROLE_SECONDARY_ONLY, "COMPATIBLE")
        with self.assertRaises(SecondaryOnlyContractError):
            example("bad", ROLE_SECONDARY_ONLY, "REJECT").__class__(**{**example("bad", ROLE_SECONDARY_ONLY, "REJECT").__dict__, "primary_target": HimP2PrimaryTargetV1("VARIANT", "x")})

    def test_invalid_state_matrix_has_twenty_fail_closed_cases(self):
        valid = example("matrix", ROLE_SECONDARY_ONLY, "REJECT")
        invalid = (
            ("secondary_only_primary_target", {"primary_target": HimP2PrimaryTargetV1("VARIANT", "x")}),
            ("secondary_only_primary_mask_active", {"primary_mask": 1.0}),
            ("secondary_only_secondary_mask_inactive", {"secondary_mask": 0.0}),
            ("secondary_only_unresolved_cc", {"secondary_target": "UNRESOLVED"}),
            ("missing_candidate_id", {"candidate_canonical_id": ""}),
            ("missing_observed_term", {"observed_term": ""}),
            ("missing_source_evidence", {"source_evidence_references": ()}),
            ("missing_human_cc_authority", {"candidate_compatibility_authority_reference": ""}),
            ("missing_candidate_name", {"candidate_canonical_name": ""}),
            ("missing_relation", {"relation": ""}),
            ("missing_relation_authority", {"relation_authority_reference": ""}),
            ("missing_family", {"family_reference": ""}),
            ("missing_lineage", {"lineage_reference": ""}),
            ("missing_catalog", {"catalog_reference": ""}),
            ("non_finite_primary_mask", {"primary_mask": float("nan")}),
            ("non_binary_secondary_mask", {"secondary_mask": 0.5}),
            ("invalid_role", {"training_role": "INVALID_V1_ROLE"}),
            ("invalid_primary_kind", {"training_role": ROLE_PRIMARY_AND_SECONDARY, "primary_mask": 1.0, "primary_target": {"kind": "INVALID", "reference": "x"}}),
            ("invalid_primary_reference", {"training_role": ROLE_PRIMARY_AND_SECONDARY, "primary_mask": 1.0, "primary_target": {"kind": "VARIANT", "reference": ""}}),
            ("blank_source_evidence_item", {"source_evidence_references": ("",)}),
        )
        self.assertEqual(len(invalid), 20)
        for name, updates in invalid:
            with self.subTest(name=name):
                with self.assertRaises(SecondaryOnlyContractError):
                    valid.__class__(**{**valid.__dict__, **updates})

    def test_mixed_and_pure_batches_encode_absent_primary_without_real_class(self):
        ordinary = example("ordinary")
        negative = example("negative", ROLE_SECONDARY_ONLY, "REJECT")
        batch = build_him_p2_tensor_batch_v1((ordinary, negative), ((4, 5), (6, 7, 8)))
        self.assertEqual(batch.primary_target.tolist(), [3, 0])
        self.assertEqual(batch.primary_mask.tolist(), [1.0, 0.0])
        self.assertEqual(batch.secondary_target.tolist(), [0, 1])
        self.assertEqual(batch.secondary_only_count, 1)

    def test_loss_normalization_and_backward_for_mixed_and_pure_secondary_batches(self):
        contract = build_him_masked_multi_objective_loss_contract_v1()
        shared = torch.nn.Linear(3, 3, bias=False)
        primary = torch.nn.Linear(3, 5)
        secondary = torch.nn.Linear(3, 2)
        negative = example("negative", ROLE_SECONDARY_ONLY, "REJECT")
        batch = build_him_p2_tensor_batch_v1((negative,), ((4, 5, 6),))
        representation = shared(torch.ones((1, 3)))
        result = compute_him_p2_masked_loss_v1(primary_logits=primary(representation), secondary_logits=secondary(representation), batch=batch, loss_contract=contract)
        self.assertEqual(result.primary_active_count, 0)
        self.assertEqual(result.primary_loss.item(), 0.0)
        self.assertTrue(torch.isfinite(result.secondary_loss))
        result.total_loss.backward()
        self.assertIsNone(primary.weight.grad)
        self.assertIsNotNone(secondary.weight.grad)
        self.assertIsNotNone(shared.weight.grad)

    def test_readiness_metrics_and_evidence_keep_secondary_only_visible(self):
        values = (example("ordinary"), example("negative", ROLE_SECONDARY_ONLY, "REJECT"))
        validate_secondary_only_readiness(values)
        self.assertEqual(supervision_counts(values), {"total": 2, "primaryActive": 1, "secondaryActive": 2, "secondaryOnly": 1, "compatible": 1, "reject": 1})
        self.assertEqual(build_execution_plan_v1(values)["counts"]["secondaryOnly"], 1)
        evidence = build_run_evidence_v1(values)
        self.assertTrue(evidence["primaryMetricExcludesSecondaryOnly"])
        self.assertTrue(evidence["secondaryMetricIncludesSecondaryOnly"])
        self.assertEqual(len(build_evaluation_evidence_v1(values)), 2)
        self.assertEqual(build_dry_run_report_v1(values)["counts"]["secondaryOnly"], 1)


if __name__ == "__main__":
    unittest.main()
