"""Model-free final HIM training runtime closure.

This module is deliberately limited to immutable authority/package checks.
It does not import torch, load a tokenizer, deserialize a model, open Holdout,
or execute training.  Its purpose is to make the final 83/19/33 training
authority available from a clean runtime checkout and to prove that the
historical qualification-count assumptions are not active on the final path.
"""

from __future__ import annotations

from pathlib import Path
from typing import Any, Mapping

from .productive_training_p2_v2 import (
    FINAL_TRAINING_ARTIFACT_DIRECTORY,
    FINAL_TRAINING_RUNTIME_AUTHORITY_FILE,
    RUNTIME_IMAGE_DIGEST_BINDING_STAGE_DEPLOYMENT,
    training_count_contract,
)
from .training_input_authority_v2 import TrainingInputV2Error, load_v2_input_bundle, logical_digest


PACKET_FILE_NAMES = (
    "final-training-corpus.v2.json",
    "final-training-partition.v2.json",
    "final-training-leakage-validation.v2.json",
    "final-training-coverage.v2.json",
    "final-training-batch-authority.v2.json",
    "final-training-input-inventory.v2.json",
    "final-training-input-authority.v2.json",
    "final-training-readiness.v2.json",
)

FINAL_CORPUS_REFERENCE = "him-final-training-corpus:v1:4e3621d8ff82e92f57af2262e819beae98dd438166e1b06f7bf7d511131b7b75"
FINAL_PARTITION_REFERENCE = "him-final-training-partition:v1:b8f48b6b253c633965ed1bb7afebd6325b25d62f4f125b3055150b881b45e27f"
FINAL_INPUT_AUTHORITY_REFERENCE = "him-final-training-input-authority:v1:9ceb2bb424978cdaf4ac5230eeba9c3fb3b64f319ec951d8f1e95f22fe045654"
FINAL_EPOCH_COUNT = 3
FINAL_PHYSICAL_BATCH_SIZE = 8


def _verify_self_describing(value: Mapping[str, Any], *, reference: str, code: str) -> None:
    if value.get("reference") != reference:
        raise TrainingInputV2Error(f"{code}_REFERENCE_MISMATCH")
    digest = value.get("logicalDigest")
    if not isinstance(digest, str) or not reference.endswith(f":{digest}"):
        raise TrainingInputV2Error(f"{code}_DIGEST_BINDING_MISMATCH")
    core = {key: item for key, item in value.items() if key not in {"reference", "logicalDigest"}}
    if logical_digest(core) != digest:
        raise TrainingInputV2Error(f"{code}_DIGEST_RECOMPUTATION_MISMATCH")


def final_training_packet_root(root: str | Path) -> Path:
    return Path(root) / FINAL_TRAINING_ARTIFACT_DIRECTORY


def final_training_runtime_authority_path(root: str | Path) -> Path:
    return Path(root) / FINAL_TRAINING_RUNTIME_AUTHORITY_FILE


def final_training_runtime_closure(root: str | Path = ".") -> dict[str, Any]:
    repository = Path(root)
    packet_root = final_training_packet_root(repository)
    runtime_authority_path = final_training_runtime_authority_path(repository)
    missing = [name for name in PACKET_FILE_NAMES if not (packet_root / name).is_file()]
    if missing:
        raise TrainingInputV2Error(f"FINAL_RUNTIME_PACKET_INCOMPLETE:{','.join(missing)}")
    if not runtime_authority_path.is_file() or runtime_authority_path.is_symlink():
        raise TrainingInputV2Error("FINAL_RUNTIME_AUTHORITY_MISSING")

    bundle = load_v2_input_bundle(repository, artifact_directory=packet_root)
    contract = training_count_contract(bundle)
    readiness = bundle["authority"]
    readiness_value = __import__("json").loads((packet_root / "final-training-readiness.v2.json").read_text(encoding="utf-8"))
    runtime_authority_value = __import__("json").loads(runtime_authority_path.read_text(encoding="utf-8"))
    _verify_self_describing(bundle["corpus"], reference=FINAL_CORPUS_REFERENCE, code="FINAL_CORPUS")
    _verify_self_describing(bundle["partition"], reference=FINAL_PARTITION_REFERENCE, code="FINAL_PARTITION")
    _verify_self_describing(bundle["authority"], reference=FINAL_INPUT_AUTHORITY_REFERENCE, code="FINAL_INPUT_AUTHORITY")
    _verify_self_describing(
        readiness_value,
        reference=readiness_value.get("reference", ""),
        code="FINAL_READINESS",
    )
    _verify_self_describing(
        runtime_authority_value,
        reference=runtime_authority_value.get("reference", ""),
        code="FINAL_RUNTIME_AUTHORITY",
    )

    if readiness_value.get("runtimeAuthorityReference") != runtime_authority_value.get("reference"):
        raise TrainingInputV2Error("FINAL_READINESS_RUNTIME_AUTHORITY_REFERENCE_MISMATCH")
    if (
        readiness_value.get("runtimeImageDigestBindingStage") != RUNTIME_IMAGE_DIGEST_BINDING_STAGE_DEPLOYMENT
        or runtime_authority_value.get("runtimeImageDigestBindingStage") != RUNTIME_IMAGE_DIGEST_BINDING_STAGE_DEPLOYMENT
        or readiness_value.get("runtimeImageDigest") is not None
        or runtime_authority_value.get("runtimeImageDigest") is not None
    ):
        raise TrainingInputV2Error("FINAL_RUNTIME_IMAGE_DIGEST_BINDING_MISMATCH")

    if bundle["batch"].get("physicalBatchSize") != FINAL_PHYSICAL_BATCH_SIZE:
        raise TrainingInputV2Error("FINAL_HYPERPARAMETER_AUTHORITY_MISMATCH")

    final_counts = readiness_value.get("finalCounts")
    if not isinstance(final_counts, Mapping):
        raise TrainingInputV2Error("FINAL_COUNTS_MISSING")
    if (
        final_counts.get("train") != contract.train_count
        or final_counts.get("validation") != contract.validation_count
        or final_counts.get("batchesPerEpoch") != contract.batches_per_epoch
        or final_counts.get("totalOptimizerSteps") != contract.total_optimizer_steps
    ):
        raise TrainingInputV2Error("FINAL_COUNT_AUTHORITY_MISMATCH")

    return {
        "contractId": "HIM_FINAL_TRAINING_RUNTIME_CLOSURE_V1",
        "packetRoot": str(FINAL_TRAINING_ARTIFACT_DIRECTORY),
        "packetFileCount": len(PACKET_FILE_NAMES),
        "finalCorpusReference": bundle["corpus"]["reference"],
        "finalPartitionReference": bundle["partition"]["reference"],
        "finalTrainingInputAuthorityReference": readiness["reference"],
        "finalReadinessReference": readiness_value["reference"],
        "finalRuntimeAuthorityReference": runtime_authority_value["reference"],
        "runtimeImageDigestBindingStage": runtime_authority_value["runtimeImageDigestBindingStage"],
        "runtimeImageDigestSelfReferenceRisk": False,
        "trainCount": contract.train_count,
        "validationCount": contract.validation_count,
        "holdoutCount": 0,
        "batchesPerEpoch": contract.batches_per_epoch,
        "optimizerStepsPerEpoch": contract.optimizer_steps_per_epoch,
        "epochCount": FINAL_EPOCH_COUNT,
        "totalOptimizerSteps": contract.total_optimizer_steps,
        "trainExamplePresentations": contract.train_count * FINAL_EPOCH_COUNT,
        "partialLastBatchAuthorized": True,
        "startsFromBaseModel": True,
        "checkpointInitializationAuthorized": False,
        "finalCountDependentRunnerGapCount": 0,
        "finalRuntimeBindingGapCount": 0,
        "finalCleanEnvOriginalMissingInputCount": readiness_value.get("cleanEnvironment", {}).get("missingInputCount"),
        "finalCleanEnvMissingInputCount": 0,
        "modelDeserializationCount": 0,
        "forwardCount": 0,
        "trainingCount": 0,
        "holdoutExposureCount": 0,
    }


__all__ = [
    "FINAL_CORPUS_REFERENCE",
    "FINAL_INPUT_AUTHORITY_REFERENCE",
    "FINAL_PARTITION_REFERENCE",
    "PACKET_FILE_NAMES",
    "final_training_runtime_authority_path",
    "final_training_packet_root",
    "final_training_runtime_closure",
]
