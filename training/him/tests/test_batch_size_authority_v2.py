from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from him_trainer.batch_size_authority_v2 import (
    build_batch_size_authority_v2,
    decode_batch_size_authority_v2,
    persist_batch_size_authority_v2,
    reload_batch_size_authority_v2,
    validate_batch_size_authority_v2,
)


class BatchSizeAuthorityV2Test(unittest.TestCase):
    def test_recommendation_and_trajectory(self) -> None:
        authority = build_batch_size_authority_v2()
        validate_batch_size_authority_v2(authority)
        self.assertEqual(8, authority.physical_batch_size)
        self.assertEqual(1, authority.gradient_accumulation_steps)
        self.assertEqual(8, authority.effective_batch_size)
        self.assertEqual([32, 16, 8, 4, 2], [x["batchesPerEpoch"] for x in authority.candidate_batches])
        self.assertEqual(12, authority.candidate_batches[3]["totalOptimizerSteps"])
        self.assertFalse(authority.batch_size_authorized)

    def test_digest_is_deterministic_and_round_trips(self) -> None:
        first = build_batch_size_authority_v2()
        second = build_batch_size_authority_v2()
        self.assertEqual(first.logical_digest, second.logical_digest)
        self.assertEqual(first.wire, decode_batch_size_authority_v2(first.wire).wire)
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "batch-authority.v2.json"
            persist_batch_size_authority_v2(path, first)
            self.assertEqual(first.wire, reload_batch_size_authority_v2(path).wire)
            self.assertFalse(list(Path(directory).glob("*.tmp")))

    def test_rejects_binding_drift(self) -> None:
        value = build_batch_size_authority_v2().wire
        value["corpusReference"] = "foreign"
        with self.assertRaises(ValueError):
            decode_batch_size_authority_v2(value)

    def test_rejects_sequence_and_partition_binding_drift(self) -> None:
        for field in ("sequenceReference", "partitionReference"):
            value = build_batch_size_authority_v2().wire
            value[field] = "foreign"
            with self.subTest(field=field), self.assertRaises(ValueError):
                decode_batch_size_authority_v2(value)

    def test_zero_active_policy_is_explicitly_safe_in_v1_loss(self) -> None:
        authority = build_batch_size_authority_v2()
        self.assertEqual("BLOCKED_TRAINER_V2_INPUT_BINDING", authority.trainer_shape_compatibility)
        self.assertEqual("STATIC_CONSERVATIVE_UPPER_BOUND_NOT_MEASURED", authority.candidate_batches[3]["estimateKind"])


if __name__ == "__main__":
    unittest.main()
