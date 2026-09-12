"""Explicit real P2 productive runner.

This module is deliberately separate from :mod:`productive_training_p2`.
The latter is the model-free P2 authority and dry-run boundary.  This module
consumes the same frozen P2 packet, validates the complete execution
authority before model construction, and exposes two explicit paths:
``--preflight`` (no model load) and ``--execute`` (one pinned model load).

No corpus, partition, loss, or P1 authority is inferred or rewritten here.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import os
import platform
import sys
import tempfile
from dataclasses import dataclass
from pathlib import Path
from types import SimpleNamespace
from typing import Any, Callable, Mapping

from .checkpoint_v2 import persist_checkpoint, reload_checkpoint
from .productive_training_p2 import (
    P2ProductiveTrainingError,
    _require,
    validate_p2_inputs,
)


MODE_P2_REAL = "P2_PRODUCTIVE_TRAINING_V1"
MODEL_ID = "FacebookAI/xlm-roberta-base"
MODEL_REVISION = "e73636d4f797dec63c3081bb6ed5c7b0bb3f2089"
TOKENIZER_ID = "xlm-roberta-base-tokenizer"
TOKENIZER_SHA256 = "a898ea75433890f6610f4e470b8ebeb0c21dce5c8dd61f892eb09eb5919d2e2c"
MODEL_AUTHORITY_DIGEST = "7eb19b673b8176e7c54ee0ebaac565b61781af60daac696ccaf76ef9adacb6bb"
CONFIGURATION_DIGEST = "6bcadc3c20e61701bd430469d133008901b7bf572164f6e7c7d410d276b1f714"
CORPUS_DIGEST = "fd65bcdfcf5811dd497722a80e68bb3bf0b1819e1c771f0ef9e9a01eabbfc8ca"
PARTITION_DIGEST = "5f31d055ad9f05f9a978f25011a6dfc8abc292fad5b9612e328896a617713d9d"
LINEAGE_REFERENCE = "p2-lineage:v1:53a946c4aaefe1a27e5dfdc78dea7aad678e7fd212c2d66d40ec9a72bc9fa110"
EXPECTED_RUNNER_MODULE = "him_trainer.productive_training_p2_real"
EXPECTED_TRAIN = 26
EXPECTED_VALIDATION = 6
EXPECTED_HOLDOUT = 0
EXPECTED_COMPATIBLE = 17
EXPECTED_REJECT = 9
EXPECTED_SECONDARY_ONLY = 9
EXPECTED_EPOCHS = 3
EXPECTED_MICRO_BATCH = 8
EXPECTED_STEPS = 12
EXPECTED_VALIDATION_FORWARDS = 1
PRIMARY_ENCODING = {"IDENTITY": 2, "VARIANT": 3, "ALIAS": 4, "EXISTING_CANONICAL": 1, "NEW_CANONICAL": 5}
SECONDARY_ENCODING = {"COMPATIBLE": 0, "REJECT": 1}


class P2RealRunnerError(P2ProductiveTrainingError):
    """Fail-closed P2 real-runner authority or execution error."""


def _canonical(value: object) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"), allow_nan=False).encode("utf-8")


def _sha256(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def _read_json(path: Path) -> dict[str, Any]:
    _require(not path.is_symlink() and path.is_file(), f"PACKET_FILE_INVALID:{path}")
    try:
        value = json.loads(path.read_text(encoding="utf-8"), parse_constant=lambda _: (_ for _ in ()).throw(ValueError()))
    except (OSError, UnicodeError, json.JSONDecodeError, ValueError) as error:
        raise P2RealRunnerError(f"PACKET_JSON_INVALID:{path}") from error
    _require(isinstance(value, dict), f"PACKET_OBJECT_REQUIRED:{path}")
    return value


def _text(value: Any, code: str) -> str:
    _require(isinstance(value, str) and bool(value), code)
    return value


@dataclass(frozen=True)
class P2Packet:
    root: Path
    values: Mapping[str, dict[str, Any]]

    def __getitem__(self, key: str) -> dict[str, Any]:
        return self.values[key]


@dataclass(frozen=True)
class P2Example:
    reference: str
    family: str
    candidate_id: str
    candidate_name: str
    observed_term: str
    target_kind: str | None
    relation: str
    compatibility: str
    primary_target: int
    secondary_target: int
    primary_mask: float
    secondary_mask: float
    source_record_reference: str
    source_evidence_reference: str


@dataclass(frozen=True)
class P2TensorView:
    input_ids: Any
    attention_mask: Any
    primary_target: Any
    secondary_target: Any
    primary_mask: Any
    secondary_mask: Any
    examples: tuple[P2Example, ...]


@dataclass(frozen=True)
class PreparedP2:
    packet: P2Packet
    examples: tuple[P2Example, ...]
    train: tuple[P2Example, ...]
    validation: tuple[P2Example, ...]
    holdout: tuple[P2Example, ...]
    tensors: Mapping[str, P2TensorView]


def load_p2_packet(packet_root: str | Path) -> P2Packet:
    root = Path(packet_root)
    _require(root.is_dir() and not root.is_symlink(), "PACKET_ROOT_INVALID")
    paths = {
        "request": "request/productive-training-request.p2.json",
        "manifest": "manifest/productive-training-manifest.p2.json",
        "readiness": "readiness/productive-training-readiness.p2.json",
        "runner": "authority/p2-runner-authority.v1.json",
        "configuration": "authority/p2-training-configuration.v1.json",
        "runtime": "authority/p2-runtime-authority.v1.json",
        "model": "authority/p2-base-model-authority.v1.json",
        "tokenizer": "authority/p2-tokenizer-authority.v1.json",
        "corpus": "dataset/corpus.p2.json",
        "partition": "dataset/partition.p2.json",
        "plan": "plan/execution-plan.p2.json",
        "checkpoint": "contracts/checkpoint-contract.p2.json",
        "run_evidence": "contracts/run-evidence-contract.p2.json",
        "validation_evidence": "contracts/validation-evidence-contract.p2.json",
    }
    values = {key: _read_json(root / relative) for key, relative in paths.items()}
    return P2Packet(root, values)


def validate_p2_runtime_authority(packet: P2Packet, *, require_real_runner: bool = True, enforce_host: bool = False) -> None:
    request, runner, configuration = packet["request"], packet["runner"], packet["configuration"]
    runtime, model, tokenizer = packet["runtime"], packet["model"], packet["tokenizer"]
    manifest, readiness, plan = packet["manifest"], packet["readiness"], packet["plan"]
    corpus, partition = packet["corpus"], packet["partition"]
    _require(request.get("lineageReference") == LINEAGE_REFERENCE, "LINEAGE_AUTHORITY_MISMATCH")
    _require(manifest.get("requestLogicalDigest") == request.get("logicalDigest"), "MANIFEST_REQUEST_BINDING_MISMATCH")
    _require(readiness.get("requestLogicalDigest") == request.get("logicalDigest"), "READINESS_REQUEST_BINDING_MISMATCH")
    _require(plan.get("requestLogicalDigest") == request.get("logicalDigest"), "PLAN_REQUEST_BINDING_MISMATCH")
    _require(readiness.get("manifestLogicalDigest") == manifest.get("logicalDigest"), "READINESS_MANIFEST_BINDING_MISMATCH")
    _require(readiness.get("logicalDigest") == str(readiness.get("reference", "")).rsplit(":", 1)[-1], "READINESS_DIGEST_REFERENCE_MISMATCH")
    _require(request.get("runner", {}).get("logicalDigest") == runner.get("logicalDigest") and manifest.get("authorities", {}).get("runner") == runner.get("reference"), "RUNNER_AUTHORITY_BINDING_MISMATCH")
    _require(request.get("runtime", {}).get("logicalDigest") == runtime.get("logicalDigest") and manifest.get("authorities", {}).get("runtime") == runtime.get("reference"), "RUNTIME_AUTHORITY_BINDING_MISMATCH")
    _require(manifest.get("authorities", {}).get("configuration") == configuration.get("reference"), "CONFIGURATION_REFERENCE_BINDING_MISMATCH")
    _require(corpus.get("logicalDigest") == CORPUS_DIGEST == request.get("corpus", {}).get("logicalDigest"), "CORPUS_AUTHORITY_MISMATCH")
    _require(partition.get("logicalDigest") == PARTITION_DIGEST == request.get("partition", {}).get("logicalDigest"), "PARTITION_AUTHORITY_MISMATCH")
    _require(configuration.get("logicalDigest") == CONFIGURATION_DIGEST, "CONFIGURATION_AUTHORITY_MISMATCH")
    _require(request.get("configuration", {}).get("logicalDigest") == CONFIGURATION_DIGEST, "REQUEST_CONFIGURATION_MISMATCH")
    _require(request.get("model", {}).get("logicalDigest") == model.get("logicalDigest") == MODEL_AUTHORITY_DIGEST, "MODEL_AUTHORITY_MISMATCH")
    _require(request.get("tokenizer", {}).get("logicalDigest") == tokenizer.get("logicalDigest"), "TOKENIZER_AUTHORITY_MISMATCH")
    _require(runtime.get("interpreter") == "/opt/him/runtime/bin/python" and runtime.get("python") == "3.13.14", "RUNTIME_PYTHON_AUTHORITY_MISMATCH")
    _require(runtime.get("pytorch") == "2.14.0+cu130" and runtime.get("cudaBuild") == "13.0", "RUNTIME_TORCH_AUTHORITY_MISMATCH")
    _require(runtime.get("tokenizers") == "0.23.1" and runtime.get("sentencepiece") == "0.2.2" and runtime.get("transformers") == "ABSENT", "RUNTIME_LIBRARY_AUTHORITY_MISMATCH")
    _require(model.get("modelId") == MODEL_ID and model.get("revision") == MODEL_REVISION, "MODEL_IDENTITY_MISMATCH")
    _require(tokenizer.get("tokenizerId") == TOKENIZER_ID and tokenizer.get("tokenizerJsonSha256") == TOKENIZER_SHA256 and tokenizer.get("vocabularyMutation") == "NO", "TOKENIZER_IDENTITY_MISMATCH")
    _require(runner.get("runnerModule") == EXPECTED_RUNNER_MODULE if require_real_runner else runner.get("runnerModule"), "P2_RUNNER_MODULE_MISMATCH")
    _require(runner.get("supportsRealExecution") is True if require_real_runner else runner.get("supportsRealExecution") is False, "P2_RUNNER_REAL_SUPPORT_MISMATCH")
    _require(runner.get("supportsModelDeserialization") is True if require_real_runner else runner.get("supportsModelDeserialization") is False, "P2_RUNNER_MODEL_SUPPORT_MISMATCH")
    _require(manifest.get("checkpointContract") == "HIM_TRAINING_CHECKPOINT_CONTRACT_V1", "CHECKPOINT_CONTRACT_MISMATCH")
    _require(packet["checkpoint"].get("formatChangeRequired") == "NO", "CHECKPOINT_FORMAT_CHANGE_UNAUTHORIZED")
    _require(packet["run_evidence"].get("contractId") == "HIM_P2_PRODUCTIVE_TRAINING_RUN_EVIDENCE_V1", "RUN_EVIDENCE_CONTRACT_MISMATCH")
    _require(packet["validation_evidence"].get("contractId") == "HIM_P2_VALIDATION_EVIDENCE_V1", "VALIDATION_EVIDENCE_CONTRACT_MISMATCH")
    _require(plan.get("modelExecutionThisMission") in ("NO", None), "PLAN_MODEL_EXECUTION_STATE_INVALID")
    _require(request.get("numerical", {}).get("epochs") == EXPECTED_EPOCHS and request.get("numerical", {}).get("microBatchSize") == EXPECTED_MICRO_BATCH, "NUMERICAL_EPOCH_AUTHORITY_MISMATCH")
    _require(request.get("numerical", {}).get("gradientAccumulationSteps") == 1 and request.get("numerical", {}).get("learningRate") == "0.0001" and request.get("numerical", {}).get("weightDecay") == "0.01", "NUMERICAL_OPTIMIZER_AUTHORITY_MISMATCH")
    _require(request.get("numerical", {}).get("seed") == 7 and request.get("numerical", {}).get("precision") == "FP32", "NUMERICAL_SEED_PRECISION_MISMATCH")
    _require(request.get("holdoutInput", {}).get("state") == "EMPTY_BY_FROZEN_PARTITION_POLICY", "HOLDOUT_AUTHORITY_MISMATCH")
    _require(corpus.get("exampleCount") == 32 and corpus.get("familyCount") == 12 and corpus.get("secondaryOnlyCount") == 9, "CORPUS_COUNT_AUTHORITY_MISMATCH")
    _require(partition.get("trainExampleCount") == 26 and partition.get("validationExampleCount") == 6 and partition.get("holdoutExampleCount") == 0, "PARTITION_COUNT_AUTHORITY_MISMATCH")
    _require(partition.get("familyLeakageCount") == 0 and partition.get("exampleLeakageCount") == 0 and partition.get("rngUsed") is False, "PARTITION_LEAKAGE_AUTHORITY_MISMATCH")
    _require(corpus.get("candidateCompatibilityDistribution", {}).get("COMPATIBLE") == 23 and corpus.get("candidateCompatibilityDistribution", {}).get("REJECT") == 9 and corpus.get("candidateCompatibilityDistribution", {}).get("UNRESOLVED") == 0, "CORPUS_COMPATIBILITY_AUTHORITY_MISMATCH")
    _require(readiness.get("state") == "READY", "READINESS_NOT_READY")
    if enforce_host:
        _require(sys.executable == "/opt/him/runtime/bin/python", "AUTHORITATIVE_INTERPRETER_REQUIRED")
        _require(platform.python_version() == "3.13.14", "PYTHON_VERSION_MISMATCH")
        import torch
        import tokenizers
        import sentencepiece
        _require(torch.__version__.split("+")[0] == "2.14.0" and torch.version.cuda == "13.0", "TORCH_RUNTIME_MISMATCH")
        _require(tokenizers.__version__ == "0.23.1" and sentencepiece.__version__ == "0.2.2", "TOKEN_RUNTIME_MISMATCH")


def _project_examples(packet: P2Packet) -> tuple[P2Example, ...]:
    examples = packet["corpus"].get("examples")
    _require(isinstance(examples, list) and len(examples) == 32, "P2_CORPUS_EXAMPLE_COUNT_INVALID")
    projected: list[P2Example] = []
    for raw in examples:
        compatibility = _text(raw.get("candidateCompatibility"), "P2_COMPATIBILITY_MISSING")
        target_kind = raw.get("targetKind")
        role = raw.get("trainingRole")
        raw_primary_mask = raw.get("primaryMask")
        if raw_primary_mask is None:
            raw_primary_mask = raw.get("primary", {}).get("mask")
        if raw_primary_mask is None:
            raw_primary_mask = 0 if target_kind is None else 1
        primary_mask = float(raw_primary_mask)
        secondary_mask = float(raw.get("secondaryMask"))
        if target_kind is None:
            _require((role in (None, "SECONDARY_ONLY")) and raw.get("secondaryTarget") == "REJECT" and primary_mask == 0.0, "P2_SECONDARY_ONLY_PRIMARY_AUTHORITY_INVALID")
            primary_target = 0
        else:
            _require(target_kind in PRIMARY_ENCODING and primary_mask == 1.0, "P2_PRIMARY_TARGET_AUTHORITY_INVALID")
            primary_target = PRIMARY_ENCODING[target_kind]
        _require(compatibility in SECONDARY_ENCODING and secondary_mask == 1.0 and raw.get("secondaryTarget") == compatibility, "P2_SECONDARY_TARGET_AUTHORITY_INVALID")
        projected.append(P2Example(
            reference=_text(raw.get("exampleReference"), "P2_EXAMPLE_REFERENCE_INVALID"),
            family=_text(raw.get("family", {}).get("familyGroupReference"), "P2_FAMILY_REFERENCE_INVALID"),
            candidate_id=_text(raw.get("canonicalId"), "P2_CANDIDATE_ID_INVALID"),
            candidate_name=_text(raw.get("canonicalName"), "P2_CANDIDATE_NAME_INVALID"),
            observed_term=_text(raw.get("observedTerm"), "P2_OBSERVED_TERM_INVALID"),
            target_kind=target_kind,
            relation=_text(raw.get("relation"), "P2_RELATION_INVALID"),
            compatibility=compatibility,
            primary_target=primary_target,
            secondary_target=SECONDARY_ENCODING[compatibility],
            primary_mask=primary_mask,
            secondary_mask=secondary_mask,
            source_record_reference=_text(raw.get("sourceRecordReference"), "P2_SOURCE_RECORD_INVALID"),
            source_evidence_reference=_text(raw.get("sourceEvidenceReference"), "P2_SOURCE_EVIDENCE_INVALID"),
        ))
    _require(sum(item.compatibility == "COMPATIBLE" for item in projected) == 23 and sum(item.compatibility == "REJECT" for item in projected) == 9, "P2_CORPUS_DISTRIBUTION_INVALID")
    _require(sum(item.primary_mask == 1.0 for item in projected) == 23 and sum(item.primary_mask == 0.0 for item in projected) == 9, "P2_PRIMARY_MASK_DISTRIBUTION_INVALID")
    return tuple(projected)


def _apply_partition(packet: P2Packet, examples: tuple[P2Example, ...]) -> tuple[tuple[P2Example, ...], tuple[P2Example, ...], tuple[P2Example, ...]]:
    by_reference = {item.reference: item for item in examples}
    assignments = packet["partition"].get("exampleAssignments")
    _require(isinstance(assignments, list) and len(assignments) == 32, "P2_PARTITION_ASSIGNMENT_COUNT_INVALID")
    split: dict[str, list[P2Example]] = {"TRAIN": [], "VALIDATION": [], "HOLDOUT": []}
    seen: set[str] = set()
    for assignment in assignments:
        reference = _text(assignment.get("exampleReference"), "P2_PARTITION_EXAMPLE_REFERENCE_INVALID")
        bucket = _text(assignment.get("partition"), "P2_PARTITION_BUCKET_INVALID")
        _require(reference in by_reference and reference not in seen and bucket in split, "P2_PARTITION_ASSIGNMENT_INVALID")
        example = by_reference[reference]
        _require(assignment.get("familyGroupReference") == example.family and assignment.get("candidateId") == example.candidate_id, "P2_PARTITION_LINEAGE_INVALID")
        split[bucket].append(example)
        seen.add(reference)
    _require(len(seen) == 32 and len(split["TRAIN"]) == EXPECTED_TRAIN and len(split["VALIDATION"]) == EXPECTED_VALIDATION and not split["HOLDOUT"], "P2_PARTITION_COUNTS_INVALID")
    _require(len({x.family for x in split["TRAIN"]} & {x.family for x in split["VALIDATION"]}) == 0, "P2_FAMILY_LEAKAGE")
    train = split["TRAIN"]
    _require(sum(x.compatibility == "COMPATIBLE" for x in train) == EXPECTED_COMPATIBLE and sum(x.compatibility == "REJECT" for x in train) == EXPECTED_REJECT and sum(x.primary_mask == 0.0 for x in train) == EXPECTED_SECONDARY_ONLY, "P2_TRAIN_COUNTS_INVALID")
    _require(all(x.compatibility == "COMPATIBLE" and x.primary_mask == 1.0 for x in split["VALIDATION"]), "P2_VALIDATION_SEMANTICS_INVALID")
    return tuple(train), tuple(split["VALIDATION"]), tuple(split["HOLDOUT"])


def _load_tokenizer(path: Path, expected_sha: str):
    from tokenizers import Tokenizer
    _require(path.is_file() and not path.is_symlink(), "TOKENIZER_ARTIFACT_MISSING")
    _require(_sha256(path.read_bytes()) == expected_sha, "TOKENIZER_ARTIFACT_DIGEST_MISMATCH")
    tokenizer = Tokenizer.from_file(str(path))
    tokenizer.no_truncation()
    return tokenizer


def _tensor_view(items: tuple[P2Example, ...], tokenizer: Any) -> P2TensorView:
    import torch
    _require(bool(items), "P2_EMPTY_TENSOR_SPLIT")
    encoded: list[list[int]] = []
    for item in items:
        result = tokenizer.encode(item.observed_term, f"CANDIDATE {item.candidate_name}", add_special_tokens=True)
        ids = [int(value) for value in result.ids]
        _require(ids and len(ids) <= 128 and ids[0] == 0 and ids[-1] == 2, "P2_TOKEN_SEQUENCE_INVALID")
        encoded.append(ids)
    width = max(len(ids) for ids in encoded)
    input_ids = [ids + [1] * (width - len(ids)) for ids in encoded]
    attention = [[1] * len(ids) + [0] * (width - len(ids)) for ids in encoded]
    return P2TensorView(
        input_ids=torch.tensor(input_ids, dtype=torch.int64),
        attention_mask=torch.tensor(attention, dtype=torch.int64),
        primary_target=torch.tensor([x.primary_target for x in items], dtype=torch.int64),
        secondary_target=torch.tensor([x.secondary_target for x in items], dtype=torch.int64),
        primary_mask=torch.tensor([x.primary_mask for x in items], dtype=torch.float32),
        secondary_mask=torch.tensor([x.secondary_mask for x in items], dtype=torch.float32),
        examples=items,
    )


def prepare_loaded_p2(packet: P2Packet, *, tokenizer_path: str | Path | None = None, require_real_runner: bool = True, enforce_host: bool = False) -> PreparedP2:
    """Prepare one already-loaded packet; kept public for model-free contract tests."""
    validate_p2_runtime_authority(packet, require_real_runner=require_real_runner, enforce_host=enforce_host)
    validate_p2_inputs(packet["corpus"], packet["partition"])
    examples = _project_examples(packet)
    train, validation, holdout = _apply_partition(packet, examples)
    model_root = packet.root / packet["model"].get("modelFileRelativePath", "")
    model_root = model_root.parent
    tokenizer_path = Path(tokenizer_path) if tokenizer_path is not None else packet.root / packet["tokenizer"].get("tokenizerArtifactRelativePath", "")
    _require(model_root.is_dir() and not model_root.is_symlink(), "MODEL_ROOT_MUST_BE_DIRECTORY")
    tokenizer = _load_tokenizer(tokenizer_path, TOKENIZER_SHA256)
    tensors = {
        "TRAIN": _tensor_view(train, tokenizer),
        "VALIDATION": _tensor_view(validation, tokenizer),
    }
    return PreparedP2(packet, examples, train, validation, holdout, tensors)


def prepare_p2(packet_root: str | Path, *, require_real_runner: bool = True, enforce_host: bool = False) -> PreparedP2:
    return prepare_loaded_p2(load_p2_packet(packet_root), require_real_runner=require_real_runner, enforce_host=enforce_host)


def _finite(value: Any, code: str) -> None:
    import torch
    _require(bool(torch.isfinite(value).all().item()), code)


def _forward(model: Any, view: P2TensorView, device: Any) -> Any:
    return model(view.input_ids.to(device), view.attention_mask.to(device))


def _validation_evidence(prepared: PreparedP2, output: Any, losses: Any) -> list[dict[str, Any]]:
    import torch
    primary_predictions = output.primary_logits.argmax(dim=1) + 1
    secondary_predictions = output.secondary_logits.argmax(dim=1)
    records: list[dict[str, Any]] = []
    for index, example in enumerate(prepared.validation):
        records.append({
            "exampleReference": example.reference,
            "primaryTarget": example.primary_target,
            "primaryPrediction": int(primary_predictions[index].detach().cpu().item()),
            "primaryLogits": [float(value) for value in output.primary_logits[index].detach().cpu().tolist()],
            "primaryLoss": float(losses.primary_raw_loss[index].detach().cpu().item()),
            "primaryCorrect": bool(primary_predictions[index].item() == example.primary_target),
            "secondaryTarget": example.secondary_target,
            "secondaryPrediction": int(secondary_predictions[index].detach().cpu().item()),
            "secondaryLogits": [float(value) for value in output.secondary_logits[index].detach().cpu().tolist()],
            "secondaryLoss": float(losses.secondary_raw_loss[index].detach().cpu().item()),
            "secondaryCorrect": bool(secondary_predictions[index].item() == example.secondary_target),
            "primaryMask": example.primary_mask,
            "secondaryMask": example.secondary_mask,
            "familyReference": example.family,
            "provenance": {"sourceRecordReference": example.source_record_reference, "sourceEvidenceReference": example.source_evidence_reference},
        })
    _require(len(records) == EXPECTED_VALIDATION, "VALIDATION_EVIDENCE_COUNT_INVALID")
    return records


def _authority_bindings(prepared: PreparedP2, *, runtime_identity: Mapping[str, Any], optimizer_identity: Mapping[str, Any]) -> dict[str, Any]:
    request = prepared.packet["request"]
    return {
        "requestLogicalDigest": request["logicalDigest"],
        "requestReference": request["reference"],
        "manifestLogicalDigest": prepared.packet["manifest"]["logicalDigest"],
        "manifestReference": prepared.packet["manifest"]["reference"],
        "corpusLogicalDigest": prepared.packet["corpus"]["logicalDigest"],
        "partitionLogicalDigest": prepared.packet["partition"]["logicalDigest"],
        "lineageReference": LINEAGE_REFERENCE,
        "runnerAuthority": {"reference": prepared.packet["runner"]["reference"], "logicalDigest": prepared.packet["runner"]["logicalDigest"]},
        "runtimeIdentity": dict(runtime_identity),
        "optimizerIdentity": dict(optimizer_identity),
        "trainingConfiguration": {"epochs": EXPECTED_EPOCHS, "microBatchSize": EXPECTED_MICRO_BATCH, "gradientAccumulationSteps": 1, "learningRate": "0.0001", "weightDecay": "0.01", "seed": 7, "precision": "FP32"},
    }


def _run_evidence(prepared: PreparedP2, *, state: str, checkpoint: Mapping[str, Any] | None, train_losses: list[dict[str, Any]], validation_records: list[dict[str, Any]], failure: str | None = None) -> dict[str, Any]:
    payload: dict[str, Any] = {
        "contractId": "HIM_P2_PRODUCTIVE_TRAINING_RUN_EVIDENCE_V1",
        "version": 1,
        "state": state,
        "lineageReference": LINEAGE_REFERENCE,
        "requestLogicalDigest": prepared.packet["request"]["logicalDigest"],
        "manifestLogicalDigest": prepared.packet["manifest"]["logicalDigest"],
        "runnerAuthorityReference": prepared.packet["runner"]["reference"],
        "runtimeReference": prepared.packet["runtime"]["reference"],
        "counts": {"train": EXPECTED_TRAIN, "validation": EXPECTED_VALIDATION, "holdout": EXPECTED_HOLDOUT, "compatible": EXPECTED_COMPATIBLE, "reject": EXPECTED_REJECT, "secondaryOnly": EXPECTED_SECONDARY_ONLY},
        "trajectory": {"epochs": EXPECTED_EPOCHS, "batchesPerEpoch": 4, "batchSizes": [8, 8, 8, 2], "forwardTrain": EXPECTED_STEPS, "backward": EXPECTED_STEPS, "optimizerSteps": EXPECTED_STEPS, "forwardValidation": EXPECTED_VALIDATION_FORWARDS, "forwardHoldout": 0},
        "mixedSupervision": {"primaryAndSecondary": 17, "secondaryOnly": 9, "fakePrimaryTargetCount": 0},
        "trainingLosses": train_losses,
        "validation": {"records": validation_records, "recordCount": len(validation_records), "rejectQualityClaim": False},
        "holdout": {"state": "EMPTY_BY_FROZEN_PARTITION_POLICY", "forwardCount": 0, "predictionCount": 0, "metricCount": 0},
        "checkpoint": checkpoint,
        "reloadEquivalence": None if checkpoint is None else checkpoint.get("reload"),
        "numericalStability": {"finitenessGuards": True},
        "failure": failure,
    }
    payload["logicalDigest"] = _sha256(_canonical(payload))
    payload["reference"] = f"p2-training-run-evidence:v1:{payload['logicalDigest']}"
    return payload


def execute_p2(prepared: PreparedP2, output_root: str | Path, *, model_loader: Callable[[], Any], device: str = "cuda:0", synthetic: bool = False) -> dict[str, Any]:
    import torch
    from .point13_loss_contract_v1 import build_him_masked_multi_objective_loss_contract_v1
    from .point13_loss_v1 import compute_him_masked_multi_objective_loss_v1
    from .point13_optimizer_construction_v1 import construct_him_adamw_v1
    from .point13_optimizer_execution_policy_v1 import build_him_optimizer_execution_policy_v1
    from .point13_trainability_policy_v1 import build_him_base_encoder_trainability_policy_v1
    from .point13_trainability_projection_v1 import project_him_trainability_policy_v1

    if device == "cpu":
        _require(synthetic, "REAL_P2_REQUIRES_CUDA")
    else:
        _require(device == "cuda:0" and torch.cuda.is_available(), "CUDA_DEVICE_REQUIRED")
    torch.manual_seed(7)
    if torch.cuda.is_available():
        torch.cuda.manual_seed_all(7)
    model = model_loader()
    _require(model is not None, "MODEL_LOAD_FAILED")
    model.to(device)
    model.train()
    policy = build_him_optimizer_execution_policy_v1(device_policy="CPU" if device == "cpu" else "CUDA", gradient_accumulation_steps=1)
    projection = project_him_trainability_policy_v1(model, build_him_base_encoder_trainability_policy_v1())
    optimizer, _ = construct_him_adamw_v1(model, policy, projection)
    loss_contract = build_him_masked_multi_objective_loss_contract_v1()
    train_losses: list[dict[str, Any]] = []
    optimizer_step = 0
    try:
        for epoch in range(1, EXPECTED_EPOCHS + 1):
            for batch_index, start in enumerate(range(0, EXPECTED_TRAIN, EXPECTED_MICRO_BATCH), start=1):
                batch = prepared.tensors["TRAIN"]
                end = min(start + EXPECTED_MICRO_BATCH, EXPECTED_TRAIN)
                optimizer.zero_grad(set_to_none=True)
                output = _forward(model, P2TensorView(batch.input_ids[start:end], batch.attention_mask[start:end], batch.primary_target[start:end], batch.secondary_target[start:end], batch.primary_mask[start:end], batch.secondary_mask[start:end], batch.examples[start:end]), device)
                losses = compute_him_masked_multi_objective_loss_v1(primary_logits=output.primary_logits, secondary_logits=output.secondary_logits, primary_target=batch.primary_target[start:end].to(device), secondary_target=batch.secondary_target[start:end].to(device), primary_mask=batch.primary_mask[start:end].to(device), secondary_mask=batch.secondary_mask[start:end].to(device), loss_contract=loss_contract, selected_execution_device=torch.device(device), allow_primary_target_absence=True)
                _finite(losses.total_loss, "NONFINITE_TRAINING_LOSS")
                losses.total_loss.backward()
                optimizer.step()
                optimizer_step += 1
                _require(optimizer_step <= EXPECTED_STEPS, "OPTIMIZER_STEP_OVERFLOW")
                train_losses.append({"epoch": epoch, "batch": batch_index, "batchSize": end - start, "optimizerStep": optimizer_step, "loss": float(losses.total_loss.detach().cpu().item())})
        _require(optimizer_step == EXPECTED_STEPS and [x["batchSize"] for x in train_losses[:4]] == [8, 8, 8, 2], "OPTIMIZER_STEP_COUNT_INVALID")
        model.eval()
        with torch.no_grad():
            validation = prepared.tensors["VALIDATION"]
            validation_output = _forward(model, validation, device)
            validation_losses = compute_him_masked_multi_objective_loss_v1(primary_logits=validation_output.primary_logits, secondary_logits=validation_output.secondary_logits, primary_target=validation.primary_target.to(device), secondary_target=validation.secondary_target.to(device), primary_mask=validation.primary_mask.to(device), secondary_mask=validation.secondary_mask.to(device), loss_contract=loss_contract, selected_execution_device=torch.device(device), allow_primary_target_absence=True)
            _finite(validation_losses.total_loss, "NONFINITE_VALIDATION_LOSS")
            validation_records = _validation_evidence(prepared, validation_output, validation_losses)
            equivalence_logits = (validation_output.primary_logits.detach().cpu().clone(), validation_output.secondary_logits.detach().cpu().clone(), validation_losses.total_loss.detach().cpu().clone())
        request = prepared.packet["request"]
        runtime_identity = {"device": device, "runtimeImage": prepared.packet["runtime"].get("runtimeImage"), "python": prepared.packet["runtime"].get("python"), "pytorch": prepared.packet["runtime"].get("pytorch"), "cuda": prepared.packet["runtime"].get("cudaBuild")}
        optimizer_identity = {"optimizerId": "optimizer:adamw:v1", "learningRate": "0.0001", "weightDecay": "0.01", "gradientAccumulationSteps": 1, "seed": 7}
        bindings = _authority_bindings(prepared, runtime_identity=runtime_identity, optimizer_identity=optimizer_identity)
        output = Path(output_root)
        checkpoint_result = persist_checkpoint(output, model=model, optimizer=optimizer, run_reference=f"p2-training-run:v1:{prepared.packet['request']['logicalDigest']}", optimizer_step=optimizer_step, authority_bindings=bindings, runtime_identity=runtime_identity, optimizer_identity=optimizer_identity)
        checkpoint = checkpoint_result.evidence_fields()
        reload_result = reload_checkpoint(output / checkpoint["manifestPath"], model=model, optimizer=optimizer, expected_bindings=bindings, expected_optimizer_step=EXPECTED_STEPS)
        _require(reload_result.model_state_equivalent and reload_result.optimizer_state_equivalent and reload_result.step_equivalent, "CHECKPOINT_RELOAD_EQUIVALENCE_FAILED")
        model.eval()
        with torch.no_grad():
            reloaded = _forward(model, prepared.tensors["VALIDATION"], device)
            reloaded_losses = compute_him_masked_multi_objective_loss_v1(primary_logits=reloaded.primary_logits, secondary_logits=reloaded.secondary_logits, primary_target=prepared.tensors["VALIDATION"].primary_target.to(device), secondary_target=prepared.tensors["VALIDATION"].secondary_target.to(device), primary_mask=prepared.tensors["VALIDATION"].primary_mask.to(device), secondary_mask=prepared.tensors["VALIDATION"].secondary_mask.to(device), loss_contract=loss_contract, selected_execution_device=torch.device(device), allow_primary_target_absence=True)
        _require(torch.equal(equivalence_logits[0], reloaded.primary_logits.detach().cpu()) and torch.equal(equivalence_logits[1], reloaded.secondary_logits.detach().cpu()) and torch.equal(equivalence_logits[2], reloaded_losses.total_loss.detach().cpu()), "LOGIT_OR_LOSS_EQUIVALENCE_FAILED")
        checkpoint["reload"]["logitEquivalence"] = True
        checkpoint["reload"]["lossEquivalence"] = True
        evidence = _run_evidence(prepared, state="P2_RUN_COMPLETED", checkpoint=checkpoint, train_losses=train_losses, validation_records=validation_records)
        atomic_json_write(output / "run-evidence.p2.json", evidence)
        atomic_json_write(output / "validation-evidence.p2.json", {"contractId": "HIM_P2_VALIDATION_EVIDENCE_V1", "recordCount": len(validation_records), "records": validation_records, "logicalDigest": _sha256(_canonical(validation_records))})
        return evidence
    except Exception as error:
        evidence = _run_evidence(prepared, state="P2_RUN_FAILED", checkpoint=None, train_losses=train_losses, validation_records=[], failure=f"{type(error).__name__}:{error}")
        atomic_json_write(Path(output_root) / "run-evidence.failed.p2.json", evidence)
        raise P2RealRunnerError("P2_REAL_EXECUTION_FAILED") from error


def atomic_json_write(path: str | Path, value: object) -> None:
    destination = Path(path)
    destination.parent.mkdir(parents=True, exist_ok=True)
    payload = _canonical(value) + b"\n"
    descriptor, temporary_name = tempfile.mkstemp(prefix=f".{destination.name}.", suffix=".tmp", dir=str(destination.parent))
    temporary = Path(temporary_name)
    try:
        with os.fdopen(descriptor, "wb") as handle:
            handle.write(payload)
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temporary, destination)
    except Exception:
        try:
            temporary.unlink()
        except OSError:
            pass
        raise


def run_cli(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(prog="him_trainer P2_PRODUCTIVE_TRAINING_V1")
    parser.add_argument("--mode", required=True, choices=(MODE_P2_REAL,))
    parser.add_argument("--packet-root", required=True)
    parser.add_argument("--output-root", required=True)
    parser.add_argument("--tokenizer-path")
    parser.add_argument("--model-root")
    parser.add_argument("--preflight", action="store_true")
    parser.add_argument("--execute", action="store_true")
    args = parser.parse_args(argv)
    _require(args.preflight != args.execute, "P2_REAL_RUN_MODE_REQUIRED")
    try:
        prepared = prepare_p2(args.packet_root, require_real_runner=True, enforce_host=args.execute)
        if args.preflight:
            print(json.dumps({"state": "P2_REAL_RUNNER_PREFLIGHT_PASS", "modelDeserializationCount": 0, "train": len(prepared.train), "validation": len(prepared.validation), "holdout": len(prepared.holdout)}, sort_keys=True))
            return 0
        _require(args.model_root is not None, "MODEL_ROOT_REQUIRED")
        from .point13_model_forward_v1 import build_him_model_execution_binding_v1, load_pinned_him_multi_head_model_from_root_v1, load_pinned_model_config_from_root_v1
        config = load_pinned_model_config_from_root_v1(Path(args.model_root))
        binding = build_him_model_execution_binding_v1(config, 7, MODEL_AUTHORITY_DIGEST)
        evidence = execute_p2(prepared, args.output_root, model_loader=lambda: load_pinned_him_multi_head_model_from_root_v1(binding, MODEL_AUTHORITY_DIGEST, 7, Path(args.model_root), "cuda:0"), device="cuda:0")
        print(json.dumps({"state": evidence["state"], "reference": evidence["reference"]}, sort_keys=True))
        return 0
    except (P2RealRunnerError, OSError, UnicodeError, ValueError) as error:
        sys.stderr.write(f"{type(error).__name__}: {error}\n")
        return 1


if __name__ == "__main__":
    raise SystemExit(run_cli())
