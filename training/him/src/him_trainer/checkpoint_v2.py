"""Real numerical checkpoint persistence for Productive Training V2.

The checkpoint is deliberately separate from the semantic/data authorities. It
stores the actual PyTorch model and optimizer states, while the JSON manifest
binds those files to the already-frozen V2 identities. A manifest is published
only after both state files have been written, fsynced, and digest-verified.
"""

from __future__ import annotations

import hashlib
import json
import os
import tempfile
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Mapping


CHECKPOINT_CONTRACT_ID = "HIM_TRAINING_CHECKPOINT_CONTRACT_V1"
CHECKPOINT_VERSION = "1"
CHECKPOINT_STATE = "CHECKPOINT_PERSISTED"
CHECKPOINT_DIR = "checkpoint"
MODEL_STATE_RELATIVE_PATH = "checkpoint/model-state.pt"
OPTIMIZER_STATE_RELATIVE_PATH = "checkpoint/optimizer-state.pt"
CHECKPOINT_MANIFEST_RELATIVE_PATH = "checkpoint/checkpoint-manifest.json"
MODEL_STATE_FORMAT = "PYTORCH_TORCH_SAVE_ZIP"
OPTIMIZER_STATE_FORMAT = "PYTORCH_TORCH_SAVE_ZIP"


class CheckpointRuntimeError(ValueError):
    """Fail-closed checkpoint persistence or reload error."""


def _canonical(value: object) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"), allow_nan=False).encode("utf-8")


def _sha256_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def _sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def _fail(condition: bool, code: str) -> None:
    if not condition:
        raise CheckpointRuntimeError(code)


def _cpu_copy(value: Any, torch: Any) -> Any:
    if isinstance(value, torch.Tensor):
        return value.detach().to(device="cpu").clone()
    if isinstance(value, Mapping):
        return {key: _cpu_copy(item, torch) for key, item in value.items()}
    if isinstance(value, list):
        return [_cpu_copy(item, torch) for item in value]
    if isinstance(value, tuple):
        return tuple(_cpu_copy(item, torch) for item in value)
    return value


def _device_copy(value: Any, device: Any, torch: Any) -> Any:
    if isinstance(value, torch.Tensor):
        return value.to(device=device)
    if isinstance(value, Mapping):
        return {key: _device_copy(item, device, torch) for key, item in value.items()}
    if isinstance(value, list):
        return [_device_copy(item, device, torch) for item in value]
    if isinstance(value, tuple):
        return tuple(_device_copy(item, device, torch) for item in value)
    return value


def _exact_equal(left: Any, right: Any, torch: Any, path: str = "root") -> None:
    if isinstance(left, torch.Tensor) or isinstance(right, torch.Tensor):
        _fail(isinstance(left, torch.Tensor) and isinstance(right, torch.Tensor), f"STATE_VALUE_TYPE_MISMATCH:{path}")
        _fail(left.dtype == right.dtype, f"STATE_DTYPE_MISMATCH:{path}")
        _fail(tuple(left.shape) == tuple(right.shape), f"STATE_SHAPE_MISMATCH:{path}")
        _fail(bool(torch.equal(left, right)), f"STATE_VALUE_MISMATCH:{path}")
        return
    if isinstance(left, Mapping) or isinstance(right, Mapping):
        _fail(isinstance(left, Mapping) and isinstance(right, Mapping), f"STATE_VALUE_TYPE_MISMATCH:{path}")
        _fail(set(left) == set(right), f"STATE_KEY_MISMATCH:{path}")
        for key in left:
            _exact_equal(left[key], right[key], torch, f"{path}.{key}")
        return
    if isinstance(left, (list, tuple)) or isinstance(right, (list, tuple)):
        _fail(isinstance(left, (list, tuple)) and isinstance(right, (list, tuple)), f"STATE_VALUE_TYPE_MISMATCH:{path}")
        _fail(len(left) == len(right), f"STATE_LENGTH_MISMATCH:{path}")
        for index, (left_item, right_item) in enumerate(zip(left, right)):
            _exact_equal(left_item, right_item, torch, f"{path}[{index}]")
        return
    _fail(left == right, f"STATE_VALUE_MISMATCH:{path}")


def _atomic_bytes_write(path: Path, payload: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    descriptor, temporary_name = tempfile.mkstemp(prefix=f".{path.name}.", suffix=".tmp", dir=str(path.parent))
    temporary = Path(temporary_name)
    try:
        with os.fdopen(descriptor, "wb") as handle:
            handle.write(payload)
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temporary, path)
    except Exception:
        try:
            temporary.unlink()
        except OSError:
            pass
        raise


def _atomic_torch_save(path: Path, value: Any, torch: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    descriptor, temporary_name = tempfile.mkstemp(prefix=f".{path.name}.", suffix=".tmp", dir=str(path.parent))
    temporary = Path(temporary_name)
    try:
        with os.fdopen(descriptor, "wb") as handle:
            torch.save(value, handle, _use_new_zipfile_serialization=True)
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temporary, path)
    except Exception:
        try:
            temporary.unlink()
        except OSError:
            pass
        raise


def _read_manifest(path: Path) -> dict[str, Any]:
    _fail(not path.is_symlink() and path.is_file(), "CHECKPOINT_MANIFEST_MISSING")
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError) as error:
        raise CheckpointRuntimeError("CHECKPOINT_MANIFEST_INVALID") from error
    _fail(isinstance(value, dict), "CHECKPOINT_MANIFEST_OBJECT_REQUIRED")
    return value


def _relative_artifact(manifest_path: Path, relative_path: str, expected: str) -> Path:
    _fail(relative_path == expected, "CHECKPOINT_STATE_PATH_INVALID")
    root = manifest_path.parent.parent
    artifact = root / relative_path
    _fail(not artifact.is_symlink() and artifact.is_file(), "CHECKPOINT_STATE_FILE_MISSING")
    _fail(artifact.resolve().parent == (root / CHECKPOINT_DIR).resolve(), "CHECKPOINT_STATE_PATH_ESCAPES_ROOT")
    return artifact


def _authority_bindings(prepared: Any, runtime_identity: Mapping[str, Any], optimizer_identity: Mapping[str, Any]) -> dict[str, Any]:
    packet = prepared.packet
    request = packet["request"]
    return {
        "requestLogicalDigest": request["logicalDigest"],
        "requestReference": request["requestReference"],
        "manifestLogicalDigest": packet["manifest"]["logicalDigest"],
        "manifestReference": packet["manifest"]["manifestReference"],
        "trainingInputAuthorityLogicalDigest": packet["input_authority"]["logicalDigest"],
        "trainingInputAuthorityReference": packet["input_authority"]["authorityReference"],
        "observedTermAuthorityLogicalDigest": packet["observed"]["logicalDigest"],
        "observedTermAuthorityReference": packet["observed"]["reference"],
        "completeTrainingExamplesLogicalDigest": packet["examples"]["logicalDigest"],
        "completeTrainingExamplesReference": packet["examples"]["reference"],
        "corpusLogicalDigest": packet["corpus"]["logicalDigest"],
        "corpusReference": packet["corpus"]["reference"],
        "partitionLogicalDigest": packet["partition"]["logicalDigest"],
        "partitionReference": packet["partition"]["partitionReference"],
        "modelBindingDigest": request["modelBindingDigest"],
        "modelId": request["modelId"],
        "modelRevision": request["modelRevision"],
        "tokenizerId": request["tokenizerId"],
        "tokenizerArtifactSha256": request["tokenizerArtifactSha256"],
        "runtimeIdentity": dict(runtime_identity),
        "optimizerIdentity": dict(optimizer_identity),
        "trainingConfiguration": {
            "epochs": request["epochs"],
            "trainingSteps": request["trainingSteps"],
            "microBatchSize": request["microBatchSize"],
            "sequenceLength": request["sequenceLength"],
            "trainingSeed": request["trainingSeed"],
            "learningRate": request["learningRate"],
            "gradientAccumulationSteps": request["gradientAccumulationSteps"],
            "validationPolicy": request["validationPolicy"],
        },
    }


def _manifest_identity(
    *,
    run_reference: str,
    optimizer_step: int,
    authority_bindings: Mapping[str, Any],
    model_state: Mapping[str, Any],
    optimizer_state: Mapping[str, Any],
    state_digest: str,
) -> dict[str, Any]:
    return {
        "runReference": run_reference,
        "authorityBindings": dict(authority_bindings),
        "optimizerStep": optimizer_step,
        "modelState": dict(model_state),
        "optimizerState": dict(optimizer_state),
        "stateDigest": state_digest,
        "modelStateFormat": MODEL_STATE_FORMAT,
        "optimizerStateFormat": OPTIMIZER_STATE_FORMAT,
    }


@dataclass(frozen=True)
class CheckpointReloadValidation:
    model_state_equivalent: bool
    optimizer_state_equivalent: bool
    step_equivalent: bool
    authority_bindings_equivalent: bool


@dataclass(frozen=True)
class CheckpointResult:
    manifest: dict[str, Any]
    manifest_path: Path
    model_state_path: Path
    optimizer_state_path: Path
    manifest_sha256: str
    model_state_sha256: str
    optimizer_state_sha256: str
    reload: CheckpointReloadValidation

    def evidence_fields(self) -> dict[str, Any]:
        return {
            "reference": self.manifest["checkpointReference"],
            "logicalDigest": self.manifest["checkpointLogicalDigest"],
            "manifestPath": CHECKPOINT_MANIFEST_RELATIVE_PATH,
            "manifestSha256": self.manifest_sha256,
            "modelStatePath": MODEL_STATE_RELATIVE_PATH,
            "modelStateSha256": self.model_state_sha256,
            "optimizerStatePath": OPTIMIZER_STATE_RELATIVE_PATH,
            "optimizerStateSha256": self.optimizer_state_sha256,
            "optimizerStep": self.manifest["optimizerStep"],
            "reload": {
                "passed": self.reload.model_state_equivalent and self.reload.optimizer_state_equivalent and self.reload.step_equivalent,
                "modelStateEquivalence": self.reload.model_state_equivalent,
                "optimizerStateEquivalence": self.reload.optimizer_state_equivalent,
                "stepEquivalence": self.reload.step_equivalent,
                "authorityBindingsEquivalence": self.reload.authority_bindings_equivalent,
            },
        }


def _strict_manifest_gate(manifest_path: Path, expected_bindings: Mapping[str, Any], expected_step: int, torch: Any) -> tuple[dict[str, Any], Path, Path]:
    manifest = _read_manifest(manifest_path)
    _fail(manifest.get("contractId") == CHECKPOINT_CONTRACT_ID and manifest.get("version") == CHECKPOINT_VERSION, "CHECKPOINT_SCHEMA_INVALID")
    _fail(manifest.get("state") == CHECKPOINT_STATE, "CHECKPOINT_STATE_INVALID")
    identity = manifest.get("identity")
    _fail(isinstance(identity, dict), "CHECKPOINT_IDENTITY_MISSING")
    _fail(manifest.get("checkpointLogicalDigest") == _sha256_bytes(_canonical(identity)), "CHECKPOINT_LOGICAL_DIGEST_INVALID")
    expected_reference = f"him-training-checkpoint:v2:{_sha256_bytes(_canonical({'runReference': identity.get('runReference'), 'checkpointLogicalDigest': manifest.get('checkpointLogicalDigest')}))}"
    _fail(manifest.get("checkpointReference") == expected_reference, "CHECKPOINT_REFERENCE_INVALID")
    _fail(identity.get("authorityBindings") == dict(expected_bindings), "CHECKPOINT_AUTHORITY_BINDING_MISMATCH")
    _fail(manifest.get("authorityBindings") == dict(expected_bindings), "CHECKPOINT_TOP_LEVEL_AUTHORITY_BINDING_MISMATCH")
    _fail(manifest.get("runReference") == identity.get("runReference"), "CHECKPOINT_RUN_REFERENCE_MISMATCH")
    _fail(identity.get("optimizerStep") == expected_step and manifest.get("optimizerStep") == expected_step, "CHECKPOINT_STEP_MISMATCH")
    _fail(manifest.get("runtimeIdentity") == identity.get("authorityBindings", {}).get("runtimeIdentity"), "CHECKPOINT_RUNTIME_IDENTITY_MISMATCH")
    _fail(manifest.get("optimizerIdentity") == identity.get("authorityBindings", {}).get("optimizerIdentity"), "CHECKPOINT_OPTIMIZER_IDENTITY_MISMATCH")
    _fail(identity.get("modelStateFormat") == MODEL_STATE_FORMAT and identity.get("optimizerStateFormat") == OPTIMIZER_STATE_FORMAT, "CHECKPOINT_FORMAT_INVALID")
    model_meta = manifest.get("modelState")
    optimizer_meta = manifest.get("optimizerState")
    _fail(isinstance(model_meta, dict) and isinstance(optimizer_meta, dict), "CHECKPOINT_STATE_METADATA_MISSING")
    _fail(model_meta == identity.get("modelState") and optimizer_meta == identity.get("optimizerState"), "CHECKPOINT_STATE_METADATA_MISMATCH")
    _fail(manifest.get("stateDigest") == identity.get("stateDigest"), "CHECKPOINT_STATE_DIGEST_BINDING_MISMATCH")
    model_path = _relative_artifact(manifest_path, model_meta.get("relativePath", ""), MODEL_STATE_RELATIVE_PATH)
    optimizer_path = _relative_artifact(manifest_path, optimizer_meta.get("relativePath", ""), OPTIMIZER_STATE_RELATIVE_PATH)
    for path, metadata in ((model_path, model_meta), (optimizer_path, optimizer_meta)):
        _fail(path.stat().st_size == metadata.get("size"), "CHECKPOINT_STATE_SIZE_MISMATCH")
        _fail(_sha256_file(path) == metadata.get("sha256"), "CHECKPOINT_STATE_DIGEST_MISMATCH")
    _fail(isinstance(manifest.get("stateDigest"), str) and len(manifest["stateDigest"]) == 64, "CHECKPOINT_STATE_DIGEST_MISSING")
    expected_state_digest = _sha256_bytes(_canonical({"modelStateSha256": model_meta["sha256"], "optimizerStateSha256": optimizer_meta["sha256"], "optimizerStep": expected_step}))
    _fail(manifest["stateDigest"] == expected_state_digest, "CHECKPOINT_STATE_DIGEST_INVALID")
    return manifest, model_path, optimizer_path


def reload_checkpoint(
    manifest_path: str | Path,
    *,
    model: Any,
    optimizer: Any,
    expected_bindings: Mapping[str, Any],
    expected_optimizer_step: int,
    expected_model_state: Any | None = None,
    expected_optimizer_state: Any | None = None,
) -> CheckpointReloadValidation:
    """Verify, load, and deeply compare a real model+optimizer checkpoint."""

    import torch

    manifest, model_path, optimizer_path = _strict_manifest_gate(Path(manifest_path), expected_bindings, expected_optimizer_step, torch)
    try:
        loaded_model_state = torch.load(model_path, map_location="cpu", weights_only=True)
        loaded_optimizer_state = torch.load(optimizer_path, map_location="cpu", weights_only=True)
    except Exception as error:
        raise CheckpointRuntimeError("CHECKPOINT_STATE_DESERIALIZATION_FAILED") from error
    _fail(isinstance(loaded_model_state, Mapping), "MODEL_STATE_STRUCTURE_INVALID")
    _fail(isinstance(loaded_optimizer_state, Mapping), "OPTIMIZER_STATE_STRUCTURE_INVALID")
    try:
        model.load_state_dict(_device_copy(loaded_model_state, next(model.parameters()).device, torch), strict=True)
        optimizer.load_state_dict(_device_copy(loaded_optimizer_state, next(model.parameters()).device, torch))
    except Exception as error:
        raise CheckpointRuntimeError("CHECKPOINT_STATE_RELOAD_FAILED") from error
    actual_model_state = _cpu_copy(model.state_dict(), torch)
    actual_optimizer_state = _cpu_copy(optimizer.state_dict(), torch)
    expected_model_state = actual_model_state if expected_model_state is None else expected_model_state
    expected_optimizer_state = actual_optimizer_state if expected_optimizer_state is None else expected_optimizer_state
    _exact_equal(expected_model_state, actual_model_state, torch, "model")
    _exact_equal(expected_optimizer_state, actual_optimizer_state, torch, "optimizer")
    return CheckpointReloadValidation(True, True, manifest["optimizerStep"] == expected_optimizer_step, True)


def persist_checkpoint(
    output_root: str | Path,
    *,
    model: Any,
    optimizer: Any,
    run_reference: str,
    optimizer_step: int,
    authority_bindings: Mapping[str, Any],
    runtime_identity: Mapping[str, Any],
    optimizer_identity: Mapping[str, Any],
) -> CheckpointResult:
    """Persist actual numerical state and publish a strict success manifest last."""

    import torch

    output = Path(output_root)
    checkpoint_dir = output / CHECKPOINT_DIR
    checkpoint_dir.mkdir(parents=True, exist_ok=True)
    model_path = output / MODEL_STATE_RELATIVE_PATH
    optimizer_path = output / OPTIMIZER_STATE_RELATIVE_PATH
    manifest_path = output / CHECKPOINT_MANIFEST_RELATIVE_PATH
    if manifest_path.is_symlink() or manifest_path.exists():
        manifest_path.unlink()
    model_state = _cpu_copy(model.state_dict(), torch)
    optimizer_state = _cpu_copy(optimizer.state_dict(), torch)
    try:
        _atomic_torch_save(model_path, model_state, torch)
        _atomic_torch_save(optimizer_path, optimizer_state, torch)
        model_sha = _sha256_file(model_path)
        optimizer_sha = _sha256_file(optimizer_path)
        model_meta = {"relativePath": MODEL_STATE_RELATIVE_PATH, "size": model_path.stat().st_size, "sha256": model_sha}
        optimizer_meta = {"relativePath": OPTIMIZER_STATE_RELATIVE_PATH, "size": optimizer_path.stat().st_size, "sha256": optimizer_sha}
        state_digest = _sha256_bytes(_canonical({"modelStateSha256": model_sha, "optimizerStateSha256": optimizer_sha, "optimizerStep": optimizer_step}))
        identity = _manifest_identity(run_reference=run_reference, optimizer_step=optimizer_step, authority_bindings=authority_bindings, model_state=model_meta, optimizer_state=optimizer_meta, state_digest=state_digest)
        logical_digest = _sha256_bytes(_canonical(identity))
        reference = f"him-training-checkpoint:v2:{_sha256_bytes(_canonical({'runReference': run_reference, 'checkpointLogicalDigest': logical_digest}))}"
        manifest = {
            "contractId": CHECKPOINT_CONTRACT_ID,
            "version": CHECKPOINT_VERSION,
            "state": CHECKPOINT_STATE,
            "checkpointReference": reference,
            "checkpointLogicalDigest": logical_digest,
            "identity": identity,
            "runReference": run_reference,
            "optimizerStep": optimizer_step,
            "authorityBindings": dict(authority_bindings),
            "runtimeIdentity": dict(runtime_identity),
            "optimizerIdentity": dict(optimizer_identity),
            "modelState": model_meta,
            "optimizerState": optimizer_meta,
            "stateDigest": state_digest,
        }
        _atomic_bytes_write(manifest_path, _canonical(manifest) + b"\n")
        manifest_sha = _sha256_file(manifest_path)
        reload_validation = reload_checkpoint(
            manifest_path,
            model=model,
            optimizer=optimizer,
            expected_bindings=authority_bindings,
            expected_optimizer_step=optimizer_step,
            expected_model_state=model_state,
            expected_optimizer_state=optimizer_state,
        )
        return CheckpointResult(manifest, manifest_path, model_path, optimizer_path, manifest_sha, model_sha, optimizer_sha, reload_validation)
    except CheckpointRuntimeError:
        for path in (manifest_path, model_path, optimizer_path):
            try:
                path.unlink()
            except OSError:
                pass
        raise
    except Exception as error:
        for path in (manifest_path, model_path, optimizer_path):
            try:
                path.unlink()
            except OSError:
                pass
        raise CheckpointRuntimeError("CHECKPOINT_PERSISTENCE_FAILED") from error


def build_prepared_checkpoint_bindings(prepared: Any, *, runtime_identity: Mapping[str, Any], optimizer_identity: Mapping[str, Any]) -> dict[str, Any]:
    """Build the exact authority map used by the V2 runner and reload gate."""

    return _authority_bindings(prepared, runtime_identity, optimizer_identity)
