from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from him_trainer.batch_size_authority_v2 import build_batch_size_authority_v2
from him_trainer.training_runtime_authority_v2 import (
    RUNTIME_IMAGE_DIGEST,
    build_training_runtime_authority_v2,
    persist_training_runtime_authority_v2,
    reload_training_runtime_authority_v2,
    validate_training_runtime_authority_v2,
)


class TrainingRuntimeAuthorityV2Test(unittest.TestCase):
    def test_exact_runtime_is_bound_but_execution_is_blocked(self) -> None:
        value = build_training_runtime_authority_v2()
        validate_training_runtime_authority_v2(value)
        self.assertEqual(RUNTIME_IMAGE_DIGEST, value["runtime"]["runtimeImageDigest"])
        self.assertFalse(value["trainingRuntimeAuthorized"])
        self.assertTrue(value["trainingRuntimeRebuildRequired"])
        self.assertEqual("NOT_IMPLEMENTED", value["trainerV2InputBinding"])

    def test_digest_and_reload(self) -> None:
        value = build_training_runtime_authority_v2(build_batch_size_authority_v2())
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "runtime-authority.v2.json"
            persist_training_runtime_authority_v2(path, value)
            self.assertEqual(value, reload_training_runtime_authority_v2(path))

    def test_rejects_runtime_digest_drift(self) -> None:
        value = build_training_runtime_authority_v2()
        value["runtime"] = dict(value["runtime"])
        value["runtime"]["runtimeImageDigest"] = "sha256:" + "0" * 64
        with self.assertRaises(ValueError):
            validate_training_runtime_authority_v2(value)

    def test_rejects_trainer_source_identity_drift(self) -> None:
        value = build_training_runtime_authority_v2()
        value["trainerSourceAudit"] = list(value["trainerSourceAudit"])
        value["trainerSourceAudit"][0] = dict(value["trainerSourceAudit"][0])
        value["trainerSourceAudit"][0]["localSha256"] = "0" * 64
        with self.assertRaises(ValueError):
            validate_training_runtime_authority_v2(value)

    def test_does_not_bind_holdout(self) -> None:
        value = build_training_runtime_authority_v2()
        self.assertNotIn("holdout", value)
        self.assertNotIn("holdoutContent", value)


if __name__ == "__main__":
    unittest.main()
