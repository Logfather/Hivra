"""Synthetic numerical tests for real Productive Training V2 checkpoints."""

from __future__ import annotations

import copy
import hashlib
import json
import tempfile
import unittest
from pathlib import Path

import torch

from him_trainer.checkpoint_v2 import (
    CHECKPOINT_MANIFEST_RELATIVE_PATH,
    CheckpointRuntimeError,
    MODEL_STATE_RELATIVE_PATH,
    OPTIMIZER_STATE_RELATIVE_PATH,
    _cpu_copy,
    persist_checkpoint,
    reload_checkpoint,
)


def _model_and_optimizer() -> tuple[torch.nn.Module, torch.optim.Optimizer]:
    torch.manual_seed(17)
    model = torch.nn.Sequential(torch.nn.Linear(3, 4), torch.nn.Tanh(), torch.nn.Linear(4, 2))
    optimizer = torch.optim.AdamW(model.parameters(), lr=0.002, weight_decay=0.01)
    x = torch.tensor([[0.1, -0.2, 0.3], [0.4, 0.5, -0.6]], dtype=torch.float32)
    target = torch.tensor([[0.2, -0.1], [0.7, 0.8]], dtype=torch.float32)
    loss = (model(x) - target).square().mean()
    loss.backward()
    optimizer.step()
    optimizer.zero_grad(set_to_none=True)
    return model, optimizer


def _bindings() -> dict[str, object]:
    return {
        "requestLogicalDigest": "a" * 64,
        "requestReference": "productive-training-request:v2:test",
        "manifestLogicalDigest": "b" * 64,
        "manifestReference": "productive-training-manifest:v2:test",
        "trainingInputAuthorityLogicalDigest": "c" * 64,
        "trainingInputAuthorityReference": "training-input-authority:v2:test",
        "observedTermAuthorityLogicalDigest": "d" * 64,
        "observedTermAuthorityReference": "observed-term-authority:v2:test",
        "completeTrainingExamplesLogicalDigest": "e" * 64,
        "completeTrainingExamplesReference": "complete-training-examples:v2:test",
        "corpusLogicalDigest": "f" * 64,
        "corpusReference": "training-corpus:v2:test",
        "partitionLogicalDigest": "1" * 64,
        "partitionReference": "training-partition:v2:test",
        "modelBindingDigest": "2" * 64,
        "modelId": "synthetic-model-v1",
        "modelRevision": "synthetic-revision-v1",
        "tokenizerId": "synthetic-tokenizer-v1",
        "tokenizerArtifactSha256": "3" * 64,
        "runtimeIdentity": {"device": "cpu", "runtimeImage": "synthetic-runtime-v1"},
        "optimizerIdentity": {"optimizerId": "adamw:v1", "learningRate": "0.002"},
        "trainingConfiguration": {"epochs": "1", "trainingSteps": "1", "trainingSeed": "17"},
    }


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _assert_state_equal(test: unittest.TestCase, left: object, right: object) -> None:
    if isinstance(left, torch.Tensor) or isinstance(right, torch.Tensor):
        test.assertIsInstance(left, torch.Tensor)
        test.assertIsInstance(right, torch.Tensor)
        test.assertEqual(left.dtype, right.dtype)
        test.assertEqual(tuple(left.shape), tuple(right.shape))
        test.assertTrue(torch.equal(left, right))
    elif isinstance(left, dict) or isinstance(right, dict):
        test.assertIsInstance(left, dict)
        test.assertIsInstance(right, dict)
        test.assertEqual(set(left), set(right))
        for key in left:
            _assert_state_equal(test, left[key], right[key])
    elif isinstance(left, (list, tuple)) or isinstance(right, (list, tuple)):
        test.assertIsInstance(left, (list, tuple))
        test.assertIsInstance(right, (list, tuple))
        test.assertEqual(len(left), len(right))
        for left_item, right_item in zip(left, right):
            _assert_state_equal(test, left_item, right_item)
    else:
        test.assertEqual(left, right)


class CheckpointV2Test(unittest.TestCase):
    def _persist(self, directory: str):
        model, optimizer = _model_and_optimizer()
        result = persist_checkpoint(
            directory,
            model=model,
            optimizer=optimizer,
            run_reference="productive-training-run:v2:synthetic",
            optimizer_step=1,
            authority_bindings=_bindings(),
            runtime_identity={"device": "cpu", "runtimeImage": "synthetic-runtime-v1"},
            optimizer_identity={"optimizerId": "adamw:v1", "learningRate": "0.002"},
        )
        return model, optimizer, result

    def test_real_model_and_optimizer_round_trip(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            model, optimizer, result = self._persist(directory)
            self.assertTrue(result.reload.model_state_equivalent)
            self.assertTrue(result.reload.optimizer_state_equivalent)
            self.assertTrue(result.reload.step_equivalent)
            self.assertEqual(1, result.manifest["optimizerStep"])
            self.assertTrue(optimizer.state_dict()["state"])
            self.assertEqual(result.model_state_sha256, _sha256(Path(directory) / MODEL_STATE_RELATIVE_PATH))
            self.assertEqual(result.optimizer_state_sha256, _sha256(Path(directory) / OPTIMIZER_STATE_RELATIVE_PATH))
            self.assertEqual(result.manifest_sha256, _sha256(Path(directory) / CHECKPOINT_MANIFEST_RELATIVE_PATH))
            self.assertEqual([], list(Path(directory).rglob("*.tmp")))
            self.assertEqual("CHECKPOINT_PERSISTED", result.manifest["state"])
            self.assertEqual("HIM_TRAINING_CHECKPOINT_CONTRACT_V1", result.manifest["contractId"])
            self.assertTrue(result.evidence_fields()["reload"]["passed"])

    def test_reload_preserves_optimizer_moments_and_next_step(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            model, optimizer, result = self._persist(directory)
            resumed_model, resumed_optimizer = _model_and_optimizer()
            reload_checkpoint(result.manifest_path, model=resumed_model, optimizer=resumed_optimizer, expected_bindings=_bindings(), expected_optimizer_step=1)
            x = torch.tensor([[0.2, -0.1, 0.4], [-0.3, 0.6, 0.1]], dtype=torch.float32)
            target = torch.tensor([[0.1, 0.2], [-0.2, 0.3]], dtype=torch.float32)
            for resumed in (model, resumed_model):
                resumed.train()
                resumed_optimizer_for_model = optimizer if resumed is model else resumed_optimizer
                resumed_optimizer_for_model.zero_grad(set_to_none=True)
                (resumed(x) - target).square().mean().backward()
                resumed_optimizer_for_model.step()
            for key, value in model.state_dict().items():
                self.assertTrue(torch.equal(value, resumed_model.state_dict()[key]), key)
            _assert_state_equal(self, _cpu_copy(optimizer.state_dict(), torch), _cpu_copy(resumed_optimizer.state_dict(), torch))

    def test_wrong_authority_binding_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            _, _, result = self._persist(directory)
            wrong = dict(_bindings())
            wrong["partitionLogicalDigest"] = "9" * 64
            with self.assertRaises(CheckpointRuntimeError):
                reload_checkpoint(result.manifest_path, model=_model_and_optimizer()[0], optimizer=_model_and_optimizer()[1], expected_bindings=wrong, expected_optimizer_step=1)

    def test_corrupt_model_state_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            _, _, result = self._persist(directory)
            path = Path(directory) / MODEL_STATE_RELATIVE_PATH
            data = bytearray(path.read_bytes())
            data[-1] ^= 1
            path.write_bytes(data)
            model, optimizer = _model_and_optimizer()
            with self.assertRaisesRegex(CheckpointRuntimeError, "CHECKPOINT_STATE_DIGEST_MISMATCH"):
                reload_checkpoint(result.manifest_path, model=model, optimizer=optimizer, expected_bindings=_bindings(), expected_optimizer_step=1)

    def test_corrupt_optimizer_state_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            _, _, result = self._persist(directory)
            path = Path(directory) / OPTIMIZER_STATE_RELATIVE_PATH
            data = bytearray(path.read_bytes())
            data[-1] ^= 1
            path.write_bytes(data)
            model, optimizer = _model_and_optimizer()
            with self.assertRaisesRegex(CheckpointRuntimeError, "CHECKPOINT_STATE_DIGEST_MISMATCH"):
                reload_checkpoint(result.manifest_path, model=model, optimizer=optimizer, expected_bindings=_bindings(), expected_optimizer_step=1)

    def test_missing_state_or_manifest_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            model, optimizer = _model_and_optimizer()
            with self.assertRaisesRegex(CheckpointRuntimeError, "CHECKPOINT_MANIFEST_MISSING"):
                reload_checkpoint(Path(directory) / CHECKPOINT_MANIFEST_RELATIVE_PATH, model=model, optimizer=optimizer, expected_bindings=_bindings(), expected_optimizer_step=1)
            _, _, result = self._persist(directory)
            (Path(directory) / MODEL_STATE_RELATIVE_PATH).unlink()
            with self.assertRaisesRegex(CheckpointRuntimeError, "CHECKPOINT_STATE_FILE_MISSING"):
                reload_checkpoint(result.manifest_path, model=model, optimizer=optimizer, expected_bindings=_bindings(), expected_optimizer_step=1)

    def test_partial_checkpoint_without_manifest_is_not_complete(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            state_path = Path(directory) / MODEL_STATE_RELATIVE_PATH
            state_path.parent.mkdir(parents=True)
            torch.save({"weight": torch.ones(1)}, state_path)
            with self.assertRaisesRegex(CheckpointRuntimeError, "CHECKPOINT_MANIFEST_MISSING"):
                reload_checkpoint(Path(directory) / CHECKPOINT_MANIFEST_RELATIVE_PATH, model=_model_and_optimizer()[0], optimizer=_model_and_optimizer()[1], expected_bindings=_bindings(), expected_optimizer_step=1)

    def test_equivalence_mismatch_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            model, optimizer, result = self._persist(directory)
            expected_model = copy.deepcopy(model.state_dict())
            expected_model["0.weight"][0, 0] += 1
            with self.assertRaisesRegex(CheckpointRuntimeError, "STATE_VALUE_MISMATCH"):
                reload_checkpoint(result.manifest_path, model=model, optimizer=optimizer, expected_bindings=_bindings(), expected_optimizer_step=1, expected_model_state=expected_model, expected_optimizer_state=optimizer.state_dict())

    def test_malformed_optimizer_payload_is_rejected_before_use(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            _, _, result = self._persist(directory)
            path = Path(directory) / OPTIMIZER_STATE_RELATIVE_PATH
            torch.save({"state": [], "param_groups": []}, path)
            with self.assertRaises(CheckpointRuntimeError):
                reload_checkpoint(result.manifest_path, model=_model_and_optimizer()[0], optimizer=_model_and_optimizer()[1], expected_bindings=_bindings(), expected_optimizer_step=1)

    def test_manifest_is_canonical_json_and_contains_identity(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            _, _, result = self._persist(directory)
            manifest = json.loads(result.manifest_path.read_text(encoding="utf-8"))
            self.assertEqual(manifest["identity"]["authorityBindings"], _bindings())
            self.assertEqual(manifest["identity"]["modelState"], manifest["modelState"])
            self.assertEqual(manifest["identity"]["optimizerState"], manifest["optimizerState"])
            self.assertEqual(manifest, json.loads(result.manifest_path.read_text(encoding="utf-8")))


if __name__ == "__main__":
    unittest.main()
