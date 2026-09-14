"""Deterministic, model-free batch authority for the HIM P2 V2 corpus.

This authority records a conservative recommendation separately from the
execution gate.  It does not load a model, inspect Holdout data, or authorize
training.  The execution gate remains closed until the V2 input binding and
runtime source closure are complete.
"""

from __future__ import annotations

import hashlib
import json
import os
import tempfile
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Mapping


CORPUS_REFERENCE = "him-p2-corpus:v2:a822a387a4368448190b1743a232b5bedb2ee5617835fad0151235f98060e5f8"
PARTITION_REFERENCE = "him-p2-partition-authority:v2:0a159f9a66a1d3bf283cd41bee3cea4794e526616758a6b8c27db74b39a4e3b8"
LEAKAGE_REFERENCE = "him-p2-leakage-validation:v2:737935048cd73f2d82e321ec293cd16ca9ad6fbe640151aec2672d524f50e682"
SEQUENCE_REFERENCE = "sequence-length-authority:v2:97130457decd4f283492da2d09a0faddb4bb99fc70f7d79732fbd390794cc509"
TRAINER_CONTRACT_REFERENCE = "p2-training-configuration:v1:9cb2bb6a338afb66a84fe7a7a60d10515e080d917c72c86019d00032955d22e0"
RUNTIME_CORE_REFERENCE = "training-runtime-authority-core:v2:687eeda5f2053224288cc87ecfa37e3469e2e0839fe03ca04d4abf456efe6613"

_PARAMETER_COUNT = 277_458_439
_PARAMETER_BYTES = _PARAMETER_COUNT * 4
_GIB = 1024**3


class BatchSizeAuthorityV2Error(ValueError):
    """Raised when a batch authority is missing, altered, or unsafe."""


def _canonical(value: object) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"), allow_nan=False).encode("utf-8")


def _digest(value: object) -> str:
    return hashlib.sha256(_canonical(value)).hexdigest()


def _require(condition: bool, code: str) -> None:
    if not condition:
        raise BatchSizeAuthorityV2Error(code)


def _candidate(batch_size: int) -> dict[str, Any]:
    batches = 32 // batch_size
    # These are deliberately conservative static upper bounds, not measured
    # peaks.  They include the fixed model/optimizer footprint and a generous
    # runtime/safety allowance at L=256.
    activation = {1: 0.80, 2: 1.20, 4: 2.00, 8: 3.60, 16: 6.80}[batch_size]
    attention = {1: 0.10, 2: 0.20, 4: 0.40, 8: 0.80, 16: 1.60}[batch_size]
    estimate = round(1.04 + 1.04 + 2.07 + activation + attention + 0.01 + 2.00 + 1.00 + 4.00, 2)
    return {
        "physicalBatchSize": batch_size,
        "gradientAccumulationSteps": 1,
        "effectiveBatchSize": batch_size,
        "batchesPerEpoch": batches,
        "optimizerStepsPerEpoch": batches,
        "totalOptimizerSteps": 3 * batches,
        "lastBatchSize": batch_size,
        "dropLast": False,
        "estimatedPeakGiB": estimate,
        "estimatedFreeHeadroomGiB": round(80.0 - estimate, 2),
        "estimateKind": "STATIC_CONSERVATIVE_UPPER_BOUND_NOT_MEASURED",
        "scientificNote": "Exact divisor of 32; preserves deterministic order and sufficient updates.",
    }


@dataclass(frozen=True)
class BatchSizeAuthorityV2:
    """Serialized P2 V2 batch recommendation and its closed/open gates."""

    corpus_reference: str
    partition_reference: str
    leakage_reference: str
    sequence_reference: str
    trainer_contract_reference: str
    runtime_core_reference: str
    physical_batch_size: int
    gradient_accumulation_steps: int
    validation_batch_size: int
    epochs: int
    seed: int
    drop_last: bool
    shuffle: bool
    max_sequence_length: int
    trainer_shape_compatibility: str
    runtime_source_authority_resolved: bool
    batch_size_authorized: bool
    status: str
    candidate_batches: tuple[dict[str, Any], ...]
    memory_components: Mapping[str, Any]
    batch_composition: tuple[dict[str, Any], ...]

    @property
    def effective_batch_size(self) -> int:
        return self.physical_batch_size * self.gradient_accumulation_steps

    @property
    def identity_payload(self) -> dict[str, Any]:
        return {
            "contractId": "HIM_P2_BATCH_SIZE_AUTHORITY_V2",
            "version": "2",
            "corpusReference": self.corpus_reference,
            "partitionReference": self.partition_reference,
            "leakageReference": self.leakage_reference,
            "sequenceReference": self.sequence_reference,
            "trainerContractReference": self.trainer_contract_reference,
            "runtimeCoreReference": self.runtime_core_reference,
            "trainingTotal": 32,
            "validationTotal": 8,
            "physicalBatchSize": self.physical_batch_size,
            "gradientAccumulationSteps": self.gradient_accumulation_steps,
            "effectiveBatchSize": self.effective_batch_size,
            "validationBatchSize": self.validation_batch_size,
            "epochs": self.epochs,
            "seed": self.seed,
            "dropLast": self.drop_last,
            "shuffle": self.shuffle,
            "maxSequenceLength": self.max_sequence_length,
            "trainerShapeCompatibility": self.trainer_shape_compatibility,
            "runtimeSourceAuthorityResolved": self.runtime_source_authority_resolved,
            "candidateBatches": list(self.candidate_batches),
            "memoryComponents": dict(self.memory_components),
            "batchComposition": list(self.batch_composition),
            "batchSizeAuthorized": self.batch_size_authorized,
            "status": self.status,
        }

    @property
    def logical_digest(self) -> str:
        return _digest(self.identity_payload)

    @property
    def reference(self) -> str:
        return f"him-p2-batch-authority:v2:{self.logical_digest}"

    @property
    def wire(self) -> dict[str, Any]:
        return {**self.identity_payload, "logicalDigest": self.logical_digest, "reference": self.reference}


def build_batch_size_authority_v2() -> BatchSizeAuthorityV2:
    """Build the sole conservative recommendation for the persisted V2 data."""

    return BatchSizeAuthorityV2(
        corpus_reference=CORPUS_REFERENCE,
        partition_reference=PARTITION_REFERENCE,
        leakage_reference=LEAKAGE_REFERENCE,
        sequence_reference=SEQUENCE_REFERENCE,
        trainer_contract_reference=TRAINER_CONTRACT_REFERENCE,
        runtime_core_reference=RUNTIME_CORE_REFERENCE,
        physical_batch_size=8,
        gradient_accumulation_steps=1,
        validation_batch_size=8,
        epochs=3,
        seed=7,
        drop_last=False,
        shuffle=False,
        max_sequence_length=256,
        trainer_shape_compatibility="BLOCKED_TRAINER_V2_INPUT_BINDING",
        runtime_source_authority_resolved=False,
        batch_size_authorized=False,
        status="BLOCKED_TRAINER_SHAPE",
        candidate_batches=tuple(_candidate(batch) for batch in (1, 2, 4, 8, 16)),
        memory_components={
            "parametersGiB": round(_PARAMETER_BYTES / _GIB, 6),
            "gradientsGiB": round(_PARAMETER_BYTES / _GIB, 6),
            "optimizerStatesGiB": round(2 * _PARAMETER_BYTES / _GIB, 6),
            "optimizerStateFormat": "TWO_FP32_ADAMW_MOMENTS",
            "activations": "B_AND_L_DEPENDENT_STATIC_BOUND",
            "attention": "B_AND_L_SQUARED_STATIC_BOUND",
            "logitsAndHeads": 0.01,
            "temporaryBuffers": 1.00,
            "cudaRuntimeOverhead": 2.00,
            "safetyMargin": 4.00,
            "sequenceLengthScale": 2.0,
            "attentionMatrixScale": 4.0,
            "measured": False,
        },
        batch_composition=(
            {"batch": 1, "size": 8, "primaryActive": 6, "secondaryActive": 7, "identity": 2, "variant": 4, "compatible": 5, "reject": 2},
            {"batch": 2, "size": 8, "primaryActive": 5, "secondaryActive": 8, "identity": 2, "variant": 3, "compatible": 6, "reject": 2},
            {"batch": 3, "size": 8, "primaryActive": 5, "secondaryActive": 7, "identity": 1, "variant": 4, "compatible": 4, "reject": 3},
            {"batch": 4, "size": 8, "primaryActive": 5, "secondaryActive": 8, "identity": 1, "variant": 4, "compatible": 5, "reject": 3},
        ),
    )


def validate_batch_size_authority_v2(authority: BatchSizeAuthorityV2) -> None:
    expected = build_batch_size_authority_v2()
    _require(authority.identity_payload == expected.identity_payload, "BATCH_AUTHORITY_FIELDS_MISMATCH")
    _require(authority.logical_digest == _digest(authority.identity_payload), "BATCH_AUTHORITY_DIGEST_MISMATCH")
    _require(authority.reference == f"him-p2-batch-authority:v2:{authority.logical_digest}", "BATCH_AUTHORITY_REFERENCE_MISMATCH")
    _require(authority.effective_batch_size == authority.physical_batch_size * authority.gradient_accumulation_steps, "EFFECTIVE_BATCH_SIZE_MISMATCH")
    _require(authority.physical_batch_size == 8 and authority.validation_batch_size == 8, "RECOMMENDED_BATCH_MISMATCH")
    _require(authority.drop_last is False and authority.shuffle is False, "DATA_CONSUMPTION_POLICY_MISMATCH")
    _require(authority.status == "BLOCKED_TRAINER_SHAPE" and authority.batch_size_authorized is False, "UNAUTHORIZED_BATCH_STATUS")


def decode_batch_size_authority_v2(value: object) -> BatchSizeAuthorityV2:
    _require(isinstance(value, Mapping), "BATCH_AUTHORITY_OBJECT_REQUIRED")
    required = set(build_batch_size_authority_v2().wire)
    _require(set(value) == required, "BATCH_AUTHORITY_FIELDS_INVALID")
    authority = build_batch_size_authority_v2()
    _require(dict(value) == authority.wire, "BATCH_AUTHORITY_VALUE_MISMATCH")
    validate_batch_size_authority_v2(authority)
    return authority


def persist_batch_size_authority_v2(path: str | Path, authority: BatchSizeAuthorityV2 | None = None) -> None:
    authority = authority or build_batch_size_authority_v2()
    validate_batch_size_authority_v2(authority)
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    fd, temporary = tempfile.mkstemp(prefix=f".{target.name}.", suffix=".tmp", dir=target.parent)
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as handle:
            json.dump(authority.wire, handle, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
            handle.write("\n")
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temporary, target)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


def reload_batch_size_authority_v2(path: str | Path) -> BatchSizeAuthorityV2:
    value = json.loads(Path(path).read_text(encoding="utf-8"))
    return decode_batch_size_authority_v2(value)
