from __future__ import annotations

import copy
import json
import unittest
from dataclasses import replace
from pathlib import Path

from him_trainer.productive_training_p2 import (
    P2ProductiveTrainingError,
    P2TrainingConfiguration,
    build_p2_dry_run,
    validate_p2_authority_identity,
    validate_p2_configuration,
    validate_p2_inputs,
    validate_p2_trajectory,
)


ROOT = Path(__file__).resolve().parents[3]
CORPUS = ROOT / "data/knowledge/him/training/p2/canonical-catalog-expansion/v1/mixed-supervision-corpus-after-secondary-only-negatives.p2.json"
PARTITION = ROOT / "data/knowledge/him/training/p2/canonical-catalog-expansion/v1/mixed-supervision-family-aware-partition.p2.json"


class ProductiveTrainingP2Test(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.corpus = json.loads(CORPUS.read_text(encoding="utf-8"))
        cls.partition = json.loads(PARTITION.read_text(encoding="utf-8"))

    def test_exact_frozen_counts_and_trajectory(self) -> None:
        result = build_p2_dry_run(self.corpus, self.partition, P2TrainingConfiguration.current())
        self.assertEqual(26, result.counts["total"])
        self.assertEqual(17, result.counts["primaryActive"])
        self.assertEqual(26, result.counts["secondaryActive"])
        self.assertEqual(9, result.counts["secondaryOnly"])
        self.assertEqual(17, result.counts["compatible"])
        self.assertEqual(9, result.counts["reject"])
        self.assertEqual(4, result.trajectory["batchesPerEpoch"])
        self.assertEqual(12, result.trajectory["totalTrainBatches"])
        self.assertEqual(12, result.trajectory["totalOptimizerSteps"])
        self.assertEqual(1, result.validation_batch_count)
        self.assertEqual(1, result.expected_validation_forward_count)
        self.assertEqual(0, result.expected_holdout_forward_count)
        self.assertEqual(0, result.holdout_prediction_count)
        self.assertEqual(0, result.holdout_metric_count)
        self.assertEqual((8, 8, 8, 2), result.train_batch_sizes)
        fields = P2TrainingConfiguration.current().identity()
        self.assertEqual("HIM_P2_PRODUCTIVE_TRAINING_RUN_EVIDENCE_V1", fields["runEvidenceContract"])
        self.assertEqual("HIM_P2_VALIDATION_EVIDENCE_V1", fields["validationEvidenceContract"])
        self.assertEqual("NO", fields["holdoutEvidenceArtifactRequired"])

    def test_configuration_is_explicit_and_deterministic(self) -> None:
        first = P2TrainingConfiguration.current()
        second = P2TrainingConfiguration.current()
        self.assertEqual(first, second)
        self.assertEqual(3, first.epochs)
        self.assertEqual(8, first.micro_batch_size)
        self.assertEqual("0.0001", first.learning_rate)
        self.assertEqual("0.01", first.weight_decay)
        self.assertEqual(7, first.seed)
        self.assertEqual("FP32", first.precision)
        self.assertEqual(first.logical_digest, second.logical_digest)

    def test_mixed_supervision_and_pure_secondary_semantics_are_validated(self) -> None:
        result = validate_p2_inputs(self.corpus, self.partition)
        self.assertEqual(9, result["splits"]["TRAIN"]["secondaryOnly"])
        self.assertEqual(0, result["splits"]["HOLDOUT"]["total"])

    def test_p2_configuration_authority_is_explicit_and_exact(self) -> None:
        authority = P2TrainingConfiguration.current()
        self.assertEqual(authority, validate_p2_configuration(authority))
        with self.assertRaises(P2ProductiveTrainingError):
            build_p2_dry_run(self.corpus, self.partition, None)

    def test_trajectory_and_partial_batch_authority_are_exact(self) -> None:
        validate_p2_trajectory(P2TrainingConfiguration.current().trajectory())
        with self.assertRaises(P2ProductiveTrainingError):
            validate_p2_trajectory({**P2TrainingConfiguration.current().trajectory(), "totalOptimizerSteps": 11})

    def test_full_negative_matrix_fails_closed(self) -> None:
        def mutate_corpus(mutation):
            corpus = copy.deepcopy(self.corpus)
            partition = copy.deepcopy(self.partition)
            mutation(corpus, partition)
            validate_p2_inputs(corpus, partition)

        def mutate_configuration(mutation):
            configuration = mutation(P2TrainingConfiguration.current())
            build_p2_dry_run(self.corpus, self.partition, configuration)

        def mutate_identity(mutation):
            identity = copy.deepcopy(P2TrainingConfiguration.current().identity())
            mutation(identity)
            validate_p2_authority_identity(identity)

        def assignment(partition, *, partition_name=None, role=None):
            return next(item for item in partition["exampleAssignments"] if (partition_name is None or item["partition"] == partition_name) and (role is None or item["role"] == role))

        cases = [
            ("total != 32", lambda: mutate_corpus(lambda corpus, _: corpus["examples"].pop())),
            ("train != 26", lambda: mutate_corpus(lambda _, partition: assignment(partition, partition_name="TRAIN").update(partition="VALIDATION"))),
            ("validation != 6", lambda: mutate_corpus(lambda _, partition: assignment(partition, partition_name="VALIDATION").update(partition="TRAIN"))),
            ("holdout != 0", lambda: mutate_corpus(lambda _, partition: assignment(partition, partition_name="TRAIN").update(partition="HOLDOUT"))),
            ("train secondary-only != 9", lambda: mutate_corpus(lambda _, partition: assignment(partition, partition_name="TRAIN", role="SECONDARY_ONLY").update(role="PRIMARY_AND_SECONDARY"))),
            ("train reject != 9", lambda: mutate_corpus(lambda _, partition: assignment(partition, partition_name="TRAIN", role="SECONDARY_ONLY").update(candidateCompatibility="COMPATIBLE"))),
            ("train compatible != 17", lambda: mutate_corpus(lambda _, partition: assignment(partition, partition_name="TRAIN", role="PRIMARY_AND_SECONDARY").update(candidateCompatibility="REJECT"))),
            ("train primary-active != 17", lambda: mutate_corpus(lambda _, partition: assignment(partition, partition_name="TRAIN", role="PRIMARY_AND_SECONDARY").update(primaryMask=0))),
            ("train secondary-active != 26", lambda: mutate_corpus(lambda _, partition: assignment(partition, partition_name="TRAIN", role="PRIMARY_AND_SECONDARY").update(secondaryMask=0))),
            ("validation reject != 0", lambda: mutate_corpus(lambda _, partition: (lambda item: item.update(candidateCompatibility="REJECT", secondaryTarget=1))(assignment(partition, partition_name="VALIDATION")))),
            ("validation primary-active != 6", lambda: mutate_corpus(lambda _, partition: assignment(partition, partition_name="VALIDATION").update(primaryMask=0))),
            ("validation secondary-active != 6", lambda: mutate_corpus(lambda _, partition: assignment(partition, partition_name="VALIDATION").update(secondaryMask=0))),
            ("synthetic holdout added", lambda: mutate_corpus(lambda _, partition: partition["exampleAssignments"].append(copy.deepcopy(partition["exampleAssignments"][0])))),
            ("unresolved compatibility present", lambda: mutate_corpus(lambda _, partition: assignment(partition, partition_name="TRAIN").update(candidateCompatibility="UNRESOLVED"))),
            ("secondary-only has semantic primary target", lambda: mutate_corpus(lambda _, partition: assignment(partition, partition_name="TRAIN", role="SECONDARY_ONLY").update(targetKind="VARIANT"))),
            ("secondary-only primaryMask != 0", lambda: mutate_corpus(lambda _, partition: assignment(partition, partition_name="TRAIN", role="SECONDARY_ONLY").update(primaryMask=1))),
            ("secondary-only secondaryMask != 1", lambda: mutate_corpus(lambda _, partition: assignment(partition, partition_name="TRAIN", role="SECONDARY_ONLY").update(secondaryMask=0))),
            ("secondary-only target != REJECT", lambda: mutate_corpus(lambda _, partition: assignment(partition, partition_name="TRAIN", role="SECONDARY_ONLY").update(candidateCompatibility="COMPATIBLE", secondaryTarget=0))),
            ("missing P2 configuration authority", lambda: build_p2_dry_run(self.corpus, self.partition, None)),
            ("wrong P2 configuration reference", lambda: mutate_configuration(lambda config: replace(config, lineage_reference="p1-lineage:v2:stale"))),
            ("P1 authority supplied to P2 runner", lambda: validate_p2_authority_identity({"contractId": "HIM_P1_PRODUCTIVE_TRAINING_CONFIGURATION_V2"})),
            ("wrong runner/configuration version", lambda: mutate_identity(lambda identity: identity.update(version=2))),
            ("wrong seed", lambda: mutate_configuration(lambda config: replace(config, seed=8))),
            ("wrong learning rate", lambda: mutate_configuration(lambda config: replace(config, learning_rate="0.0002"))),
            ("wrong weight decay", lambda: mutate_configuration(lambda config: replace(config, weight_decay="0.02"))),
            ("wrong precision", lambda: mutate_configuration(lambda config: replace(config, precision="FP16"))),
            ("wrong epoch count", lambda: mutate_configuration(lambda config: replace(config, epochs=4))),
            ("wrong micro-batch size", lambda: mutate_configuration(lambda config: replace(config, micro_batch_size=4))),
            ("wrong gradient accumulation", lambda: mutate_configuration(lambda config: replace(config, gradient_accumulation_steps=2))),
            ("trajectory optimizer-step mismatch", lambda: validate_p2_trajectory({**P2TrainingConfiguration.current().trajectory(), "totalOptimizerSteps": 11})),
        ]
        self.assertGreaterEqual(len(cases), 30)
        for name, operation in cases:
            with self.subTest(name=name):
                with self.assertRaises(P2ProductiveTrainingError):
                    operation()

    def test_invalid_partition_counts_fail_closed(self) -> None:
        mutated = copy.deepcopy(self.partition)
        mutated["exampleAssignments"] = mutated["exampleAssignments"][:-1]
        with self.assertRaises(P2ProductiveTrainingError):
            validate_p2_inputs(self.corpus, mutated)

    def test_secondary_only_primary_mask_fails_closed(self) -> None:
        mutated = copy.deepcopy(self.partition)
        item = next(x for x in mutated["exampleAssignments"] if x["role"] == "SECONDARY_ONLY")
        item["primaryMask"] = 1
        with self.assertRaises(P2ProductiveTrainingError):
            validate_p2_inputs(self.corpus, mutated)

    def test_fake_holdout_fails_closed(self) -> None:
        mutated = copy.deepcopy(self.partition)
        item = mutated["exampleAssignments"][0]
        item["partition"] = "HOLDOUT"
        with self.assertRaises(P2ProductiveTrainingError):
            validate_p2_inputs(self.corpus, mutated)


if __name__ == "__main__":
    unittest.main()
