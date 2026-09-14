"""Runtime identity authority for the upcoming HIM P2 V2 qualification run.

The authority is intentionally blocked while the current image lacks the V2
trainer source closure.  It records the exact target runtime and source audit
without pretending that an old evaluation image can execute the new corpus.
"""

from __future__ import annotations

import hashlib
import json
import os
import tempfile
from pathlib import Path
from typing import Any, Mapping

from .batch_size_authority_v2 import (
    BatchSizeAuthorityV2,
    build_batch_size_authority_v2,
    validate_batch_size_authority_v2,
)


RUNTIME_IMAGE_DIGEST = "sha256:87b74e2b58b3918840890209c42f1a0bf468135cc37156e00d2d3591dd20723a"
MODEL_REFERENCE = "FacebookAI/xlm-roberta-base@e73636d4f797dec63c3081bb6ed5c7b0bb3f2089"
TOKENIZER_REFERENCE = "xlm-roberta-base-tokenizer@a898ea75433890f6610f4e470b8ebeb0c21dce5c8dd61f892eb09eb5919d2e2c"
REAL_EXECUTION_RUNNER_MODULE = "him_trainer.productive_training_p2_v2"


class TrainingRuntimeAuthorityV2Error(ValueError):
    """Raised when runtime authority identity or source closure drifts."""


def _canonical(value: object) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"), allow_nan=False).encode("utf-8")


def _digest(value: object) -> str:
    return hashlib.sha256(_canonical(value)).hexdigest()


def _require(condition: bool, code: str) -> None:
    if not condition:
        raise TrainingRuntimeAuthorityV2Error(code)


RUNTIME_CORE_IDENTITY = {
    "runtimeImageDigest": RUNTIME_IMAGE_DIGEST,
    "python": "3.13.14",
    "pytorch": "2.14.0+cu130",
    "cuda": "13.0",
    "gpu": "NVIDIA_A100_80GB_PCIE",
    "gpuCount": 1,
    "driverMinimum": "580.65.06",
    "tokenizers": "0.23.1",
    "sentencepiece": "0.2.2",
    "transformers": "ABSENT",
    "modelReference": MODEL_REFERENCE,
    "tokenizerReference": TOKENIZER_REFERENCE,
}
RUNTIME_CORE_REFERENCE = f"training-runtime-authority-core:v2:{_digest(RUNTIME_CORE_IDENTITY)}"


def _source_audit() -> tuple[dict[str, str], ...]:
    entries = (
        ("productive_training_p2_real.py", "65053c70353ff58f3bfdb8000f03a3d557372b0aa5b6b3cdb180a451ca6f56d4", "NOT_IN_IMAGE"),
        ("productive_training_p2.py", "46b1e522df9499f1a4f16562cdf60c3442cd0e6844d4bdc3464d7f1b5814d59d", "NOT_IN_IMAGE"),
        ("corpus_assembly_v2.py", "3a03ea7bbc6ab5aa84bb1dd62fb1fc666b1279b76b569db17f2423c9a071a44a", "NOT_IN_IMAGE"),
        ("partition_v2.py", "60666657742ebb8cfc26303defee54f2757916eaa20b8d9f12557db266789478", "NOT_IN_IMAGE"),
        ("input_representation_v2.py", "49a35415014ed63d2cb85d008b51ae6104eec8fb7801c08edff4086096d928bb", "NOT_IN_IMAGE"),
        ("evidence_projection_v2.py", "e60708bdcfbf103bce84473d42cb1493477e56988ecde6204c72ec13a1b65348", "NOT_IN_IMAGE"),
        ("sequence_length_authority_v2.py", "56847883bfd3dee67ad32548e81f4838140b7104c3f34b75bcc78d377362e777", "NOT_IN_IMAGE"),
        ("partition_leakage_v2.py", "3d9ec67ce11e09263f4c1a8fee2ded94f2704996ba3ef848af1ce215fdf7f583", "NOT_IN_IMAGE"),
        ("point13_model_forward_v1.py", "5e0c9dc3dd5570ffdee24d1da721ff764edc38fb87fff932e3b1ad470befff97", "IN_IMAGE_AND_MATCHES"),
        ("point13_loss_contract_v1.py", "06fd5d1693f0a0bd2eec4c1630611582f66e77a4dd9ba46bab1b4057cd8e6427", "IN_IMAGE_AND_MATCHES"),
        ("point13_loss_v1.py", "bcab2366e044714cc1e838e24f7205bc069836a82495c7570d84fba71c175c49", "NOT_IN_IMAGE"),
        ("point13_optimizer_execution_policy_v1.py", "ede1e90237d07d70f5aeab95f2cb7ec1f982105164f831845d28da9b1292ca74", "IN_IMAGE_AND_MATCHES"),
        ("point13_trainability_policy_v1.py", "b3ffd6568af8fc38f6372f54f6dc011ead80ebe25f5fffde1d09bcf3e03cb7f4", "IN_IMAGE_AND_MATCHES"),
    )
    return tuple({"module": name, "localSha256": digest, "runtimeStatus": status} for name, digest, status in entries)


def build_training_runtime_authority_v2(batch: BatchSizeAuthorityV2 | None = None) -> dict[str, Any]:
    batch = batch or build_batch_size_authority_v2()
    validate_batch_size_authority_v2(batch)
    identity = {
        "contractId": "HIM_P2_TRAINING_RUNTIME_AUTHORITY_V2",
        "version": "2",
        "runtimeCoreReference": RUNTIME_CORE_REFERENCE,
        "batchAuthorityReference": batch.reference,
        "corpusReference": batch.corpus_reference,
        "partitionReference": batch.partition_reference,
        "sequenceReference": batch.sequence_reference,
        "runtime": dict(RUNTIME_CORE_IDENTITY),
        "trainerSourceAudit": list(_source_audit()),
        "trainerV2InputBinding": "NOT_IMPLEMENTED",
        "trainingRuntimeRebuildRequired": True,
        "status": "BLOCKED_IMAGE_REBUILD",
        "trainingRuntimeAuthorized": False,
    }
    return {
        **identity,
        "logicalDigest": _digest(identity),
        "reference": f"him-p2-training-runtime-authority:v2:{_digest(identity)}",
    }


def build_execution_enabled_training_runtime_authority_v2(
    *,
    runtime_image_digest: str,
    runtime_source_logical_digest: str,
    source_git_head: str,
    training_input_authority_reference: str,
    batch_authority_reference: str,
    sequence_reference: str,
    corpus_reference: str,
    partition_reference: str,
    leakage_reference: str,
    model_binding_digest: str,
    runtime_source_module_count: int = 31,
) -> dict[str, Any]:
    """Issue a new immutable execution-enabled runtime binding.

    The historical blocked authority is never edited.  This constructor is
    intentionally data-only so it can be used before model execution and
    after an OCI build when the final digest becomes known.
    """

    _require(isinstance(runtime_image_digest, str) and runtime_image_digest.startswith("sha256:") and len(runtime_image_digest) == 71, "RUNTIME_DIGEST_INVALID")
    _require(isinstance(runtime_source_logical_digest, str) and len(runtime_source_logical_digest) == 64, "RUNTIME_SOURCE_DIGEST_INVALID")
    _require(isinstance(source_git_head, str) and len(source_git_head) == 40, "SOURCE_HEAD_INVALID")
    _require(runtime_source_module_count == 31, "RUNTIME_SOURCE_MODULE_COUNT_INVALID")
    identity = {
        "contractId": "HIM_P2_TRAINING_RUNTIME_AUTHORITY_V2",
        "version": "2",
        "referencePrefix": "him-p2-training-runtime-authority-execution-enabled:v2",
        "runtimeImageDigest": runtime_image_digest,
        "runtimeImageReference": f"ghcr.io/logfather/him-a100-reference-runtime@{runtime_image_digest}",
        "runtime": dict(RUNTIME_CORE_IDENTITY, runtimeImageDigest=runtime_image_digest),
        "modelBindingDigest": model_binding_digest,
        "modelReference": MODEL_REFERENCE,
        "tokenizerReference": TOKENIZER_REFERENCE,
        "runtimeSourceLogicalDigest": runtime_source_logical_digest,
        "runtimeSourceModuleCount": runtime_source_module_count,
        "sourceGitHead": source_git_head,
        "trainer": {"runnerModule": REAL_EXECUTION_RUNNER_MODULE, "supportsRealExecution": True, "supportsModelDeserialization": True},
        "trainingInputAuthorityReference": training_input_authority_reference,
        "batchAuthorityReference": batch_authority_reference,
        "sequenceReference": sequence_reference,
        "corpusReference": corpus_reference,
        "partitionReference": partition_reference,
        "leakageReference": leakage_reference,
        "holdoutOpened": False,
        "trainingRuntimeAuthorized": True,
        "realTrainingExecutionAuthorized": True,
        "status": "AUTHORIZED",
    }
    digest = _digest(identity)
    return {**identity, "logicalDigest": digest, "reference": f"him-p2-training-runtime-authority-execution-enabled:v2:{digest}"}


def validate_execution_enabled_training_runtime_authority_v2(value: Mapping[str, Any]) -> None:
    """Validate the reissued authority without accepting the old blocked one."""

    _require(value.get("contractId") == "HIM_P2_TRAINING_RUNTIME_AUTHORITY_V2", "RUNTIME_AUTHORITY_CONTRACT_MISMATCH")
    _require(value.get("status") == "AUTHORIZED", "RUNTIME_AUTHORITY_STATUS_INVALID")
    _require(value.get("trainingRuntimeAuthorized") is True, "RUNTIME_AUTHORITY_NOT_AUTHORIZED")
    _require(value.get("realTrainingExecutionAuthorized") is True, "REAL_EXECUTION_NOT_AUTHORIZED")
    _require(value.get("holdoutOpened") is False, "HOLDOUT_MUST_REMAIN_CLOSED")
    _require(value.get("runtimeSourceModuleCount") == 31, "RUNTIME_SOURCE_MODULE_COUNT_INVALID")
    _require(value.get("trainer", {}).get("runnerModule") == REAL_EXECUTION_RUNNER_MODULE, "RUNNER_MODULE_MISMATCH")
    _require(value.get("trainer", {}).get("supportsRealExecution") is True, "REAL_EXECUTION_SUPPORT_MISSING")
    _require(value.get("runtimeImageReference") == f"ghcr.io/logfather/him-a100-reference-runtime@{value.get('runtimeImageDigest')}", "RUNTIME_IMAGE_REFERENCE_MISMATCH")
    identity = {key: item for key, item in value.items() if key not in {"logicalDigest", "reference"}}
    _require(value.get("logicalDigest") == _digest(identity), "RUNTIME_AUTHORITY_DIGEST_MISMATCH")
    _require(value.get("reference") == f"him-p2-training-runtime-authority-execution-enabled:v2:{value.get('logicalDigest')}", "RUNTIME_AUTHORITY_REFERENCE_MISMATCH")


def persist_execution_enabled_training_runtime_authority_v2(path: str | Path, value: Mapping[str, Any]) -> None:
    """Persist one reissued authority without overwriting a different one."""

    validate_execution_enabled_training_runtime_authority_v2(value)
    target = Path(path)
    payload = _canonical(dict(value)) + b"\n"
    if target.exists() or target.is_symlink():
        _require(not target.is_symlink() and target.read_bytes() == payload, "EXECUTION_AUTHORITY_IMMUTABLE_COLLISION")
        return
    target.parent.mkdir(parents=True, exist_ok=True)
    fd, temporary = tempfile.mkstemp(prefix=f".{target.name}.", suffix=".tmp", dir=target.parent)
    try:
        with os.fdopen(fd, "wb") as handle:
            handle.write(payload)
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temporary, target)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


def reload_execution_enabled_training_runtime_authority_v2(path: str | Path) -> dict[str, Any]:
    value = json.loads(Path(path).read_text(encoding="utf-8"))
    _require(isinstance(value, Mapping), "EXECUTION_AUTHORITY_OBJECT_REQUIRED")
    validate_execution_enabled_training_runtime_authority_v2(value)
    return dict(value)
def validate_training_runtime_authority_v2(value: Mapping[str, Any], batch: BatchSizeAuthorityV2 | None = None) -> None:
    expected = build_training_runtime_authority_v2(batch)
    _require(dict(value) == expected, "TRAINING_RUNTIME_AUTHORITY_MISMATCH")
    identity = {key: value[key] for key in value if key not in {"logicalDigest", "reference"}}
    _require(value["logicalDigest"] == _digest(identity), "TRAINING_RUNTIME_AUTHORITY_DIGEST_MISMATCH")
    _require(value["reference"] == f"him-p2-training-runtime-authority:v2:{value['logicalDigest']}", "TRAINING_RUNTIME_AUTHORITY_REFERENCE_MISMATCH")
    _require(value["runtime"]["runtimeImageDigest"] == RUNTIME_IMAGE_DIGEST, "RUNTIME_DIGEST_MISMATCH")
    _require(value["trainingRuntimeAuthorized"] is False, "UNAUTHORIZED_RUNTIME_STATUS")


def persist_training_runtime_authority_v2(path: str | Path, value: Mapping[str, Any] | None = None) -> None:
    value = value or build_training_runtime_authority_v2()
    validate_training_runtime_authority_v2(value)
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    fd, temporary = tempfile.mkstemp(prefix=f".{target.name}.", suffix=".tmp", dir=target.parent)
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as handle:
            json.dump(value, handle, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
            handle.write("\n")
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temporary, target)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


def reload_training_runtime_authority_v2(path: str | Path) -> dict[str, Any]:
    value = json.loads(Path(path).read_text(encoding="utf-8"))
    validate_training_runtime_authority_v2(value)
    return value
