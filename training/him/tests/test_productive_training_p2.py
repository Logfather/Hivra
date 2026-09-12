from __future__ import annotations

import copy
import json
import unittest
from pathlib import Path

from him_trainer.productive_training_p2 import (
    P2ProductiveTrainingError,
    P2TrainingConfiguration,
    build_p2_dry_run,
    validate_p2_inputs,
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
        result = build_p2_dry_run(self.corpus, self.partition)
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
