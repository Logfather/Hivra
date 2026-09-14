"""P2 V2 input binding and explicitly-authorized execution boundary.

The default path is model-free.  A real execution request must present two
immutable, mutually-bound authorities: the execution-enabled Runtime
Authority and its matching Training Readiness Authority.  This module never
discovers data or falls back to a historical packet; membership always comes
from Partition V2.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Callable, Mapping

from .training_input_authority_v2 import (
    BATCH_REFERENCE,
    MAX_SEQUENCE_LENGTH,
    TrainingInputV2Error,
    load_v2_input_bundle,
    persist_training_input_authority,
    logical_digest,
)


RUNNER_MODULE = "him_trainer.productive_training_p2_v2"
TRAIN_COUNT = 32
VALIDATION_COUNT = 8
PHYSICAL_BATCH_SIZE = 8
GRADIENT_ACCUMULATION_STEPS = 1
EPOCHS = 3
SHUFFLE = False
DROP_LAST = False
TOKENIZER_SHA256 = "a898ea75433890f6610f4e470b8ebeb0c21dce5c8dd61f892eb09eb5919d2e2c"
TOKENIZER_PAD_TOKEN_ID = 1
TOKENIZER_BOS_TOKEN_ID = 0
TOKENIZER_EOS_TOKEN_ID = 2
# Keep the execution-path padding symbol bound to the tokenizer authority.
# The model-free tensor builder and the real execute path use this name when
# validating and applying dynamic batch padding.
PAD_TOKEN_ID = TOKENIZER_PAD_TOKEN_ID
EXECUTION_AUTHORITY_ERROR = "REAL_P2_V2_TRAINING_EXECUTION_AUTHORITY_REQUIRED"
EXECUTION_AUTHORITY_CONTRACT = "HIM_P2_TRAINING_RUNTIME_AUTHORITY_V2"
READINESS_AUTHORITY_CONTRACT = "HIM_P2_TRAINING_READINESS_AUTHORITY_V2"


@dataclass(frozen=True)
class ExecutionAuthorityBinding:
    """The model-free result of the final pre-training authority gate."""

    runtime_authority: Mapping[str, Any]
    readiness: Mapping[str, Any]
    input_authority_reference: str
    runtime_image_digest: str


def _read_authority(path: str | Path, code: str) -> dict[str, Any]:
    target = Path(path)
    if target.is_symlink() or not target.is_file():
        raise TrainingInputV2Error(f"{code}_MISSING")
    try:
        value = json.loads(target.read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError) as error:
        raise TrainingInputV2Error(f"{code}_INVALID") from error
    if not isinstance(value, dict):
        raise TrainingInputV2Error(f"{code}_OBJECT_REQUIRED")
    return value


def _verify_logical_digest(value: Mapping[str, Any], code: str) -> None:
    digest = value.get("logicalDigest")
    if not isinstance(digest, str) or len(digest) != 64:
        raise TrainingInputV2Error(f"{code}_DIGEST_MISSING")
    core = {key: item for key, item in value.items() if key not in {"logicalDigest", "reference"}}
    if logical_digest(core) != digest:
        raise TrainingInputV2Error(f"{code}_DIGEST_MISMATCH")
    reference = value.get("reference")
    if not isinstance(reference, str) or not reference.endswith(f":{digest}"):
        raise TrainingInputV2Error(f"{code}_REFERENCE_MISMATCH")


def validate_execution_authority(
    root: str | Path,
    *,
    runtime_authority_path: str | Path,
    readiness_path: str | Path,
    expected_runtime_image_digest: str | None = None,
) -> ExecutionAuthorityBinding:
    """Validate every model-free gate needed before ``--execute``.

    This function intentionally performs no tokenizer/model import and does
    not open Holdout.  It is therefore also the remote pre-training probe.
    """

    bundle = load_v2_input_bundle(root)
    runtime = _read_authority(runtime_authority_path, "RUNTIME_AUTHORITY")
    readiness = _read_authority(readiness_path, "TRAINING_READINESS")
    _verify_logical_digest(runtime, "RUNTIME_AUTHORITY")
    _verify_logical_digest(readiness, "TRAINING_READINESS")
    if runtime.get("contractId") != EXECUTION_AUTHORITY_CONTRACT:
        raise TrainingInputV2Error("RUNTIME_AUTHORITY_CONTRACT_MISMATCH")
    if readiness.get("contractId") != READINESS_AUTHORITY_CONTRACT:
        raise TrainingInputV2Error("TRAINING_READINESS_CONTRACT_MISMATCH")
    if runtime.get("status") != "AUTHORIZED" or runtime.get("trainingRuntimeAuthorized") is not True:
        raise TrainingInputV2Error(EXECUTION_AUTHORITY_ERROR)
    if runtime.get("realTrainingExecutionAuthorized") is not True:
        raise TrainingInputV2Error(EXECUTION_AUTHORITY_ERROR)
    runtime_digest = runtime.get("runtimeImageDigest")
    if not isinstance(runtime_digest, str) or not runtime_digest.startswith("sha256:"):
        raise TrainingInputV2Error("RUNTIME_OCI_DIGEST_INVALID")
    if expected_runtime_image_digest is not None and runtime_digest != expected_runtime_image_digest:
        raise TrainingInputV2Error("RUNTIME_OCI_DIGEST_MISMATCH")
    if runtime.get("runtimeImageReference") != f"ghcr.io/logfather/him-a100-reference-runtime@{runtime_digest}":
        raise TrainingInputV2Error("RUNTIME_OCI_REFERENCE_MISMATCH")
    trainer = runtime.get("trainer")
    if runtime.get("runtimeSourceModuleCount") != 31 or not isinstance(trainer, Mapping) or trainer.get("runnerModule") != RUNNER_MODULE:
        raise TrainingInputV2Error("TRAINER_SOURCE_AUTHORITY_MISMATCH")
    if readiness.get("status") != "AUTHORIZED" or readiness.get("trainingReady") is not True:
        raise TrainingInputV2Error("TRAINING_READINESS_MISMATCH")
    if readiness.get("runtimeAuthorityReference") != runtime.get("reference"):
        raise TrainingInputV2Error("RUNTIME_READINESS_BINDING_MISMATCH")
    if readiness.get("runtimeImageDigest") != runtime_digest:
        raise TrainingInputV2Error("RUNTIME_READINESS_DIGEST_MISMATCH")
    if readiness.get("runtimeAuthorized") is not True or readiness.get("realTrainingExecutionAuthorized") is not True:
        raise TrainingInputV2Error("EXECUTION_AUTHORITY_GATE_FAILED")
    gates = readiness.get("gates")
    required_gates = ("corpus", "partition", "leakage", "coverage", "sequence", "batch", "trainingInput", "runtime", "trainerSource", "tokenizer", "baseModel", "holdoutClosed")
    if not isinstance(gates, Mapping) or any(gates.get(name) is not True for name in required_gates):
        raise TrainingInputV2Error("TRAINING_READINESS_GATES_FAILED")
    input_path = Path(root) / "data/knowledge/him/training/p2/canonical-catalog-expansion/v2/runtime-authority/training-input-authority.v2.json"
    input_authority = _read_authority(input_path, "TRAINING_INPUT_AUTHORITY")
    if input_authority.get("reference") != readiness.get("trainingInputAuthorityReference"):
        raise TrainingInputV2Error("TRAINING_INPUT_AUTHORITY_MISMATCH")
    if readiness.get("corpusReference") != bundle["corpus"].get("reference") or readiness.get("partitionReference") != bundle["partition"].get("reference") or readiness.get("leakageReference") != bundle["leakage"].get("reference"):
        raise TrainingInputV2Error("DATA_AUTHORITY_MISMATCH")
    if readiness.get("holdoutOpened") is not False or bundle["holdout"] != ():
        raise TrainingInputV2Error("HOLDOUT_EXECUTION_FORBIDDEN")
    if runtime.get("batchAuthorityReference") not in {bundle["batch"].get("reference"), readiness.get("batchAuthorityReference")}:
        raise TrainingInputV2Error("BATCH_AUTHORITY_MISMATCH")
    return ExecutionAuthorityBinding(runtime, readiness, input_authority["reference"], runtime_digest)


def execution_preflight(
    root: str | Path,
    *,
    runtime_authority_path: str | Path,
    readiness_path: str | Path,
    expected_runtime_image_digest: str | None = None,
) -> dict[str, Any]:
    """Reach the execution handoff without loading a model or tokenizer."""

    binding = validate_execution_authority(
        root,
        runtime_authority_path=runtime_authority_path,
        readiness_path=readiness_path,
        expected_runtime_image_digest=expected_runtime_image_digest,
    )
    return {
        "state": "P2_V2_EXECUTION_AUTHORITY_ACCEPTED",
        "executionAccepted": True,
        "runnerModule": RUNNER_MODULE,
        "runtimeAuthorityReference": binding.runtime_authority["reference"],
        "readinessReference": binding.readiness["reference"],
        "runtimeImageDigest": binding.runtime_image_digest,
        "train": TRAIN_COUNT,
        "validation": VALIDATION_COUNT,
        "holdout": 0,
        "modelDeserializationCount": 0,
        "forwardCount": 0,
        "backwardCount": 0,
        "optimizerCreatedCount": 0,
        "optimizerStepCount": 0,
        "holdoutExposureCount": 0,
    }


def build_partition_bound_batches(entries: tuple[Mapping[str, Any], ...], batch_size: int = PHYSICAL_BATCH_SIZE) -> tuple[tuple[Mapping[str, Any], ...], ...]:
    if batch_size != PHYSICAL_BATCH_SIZE:
        raise TrainingInputV2Error("UNAUTHORIZED_BATCH_CHANGE")
    if not entries or len(entries) % batch_size:
        raise TrainingInputV2Error("BATCH_MEMBERSHIP_INVALID")
    return tuple(tuple(entries[index:index + batch_size]) for index in range(0, len(entries), batch_size))


def build_tensor_shape_contract(entries: tuple[Mapping[str, Any], ...]) -> dict[str, tuple[int, ...]]:
    if not entries:
        raise TrainingInputV2Error("EMPTY_INPUT_SPLIT")
    width = max(int(item["sequenceLength"]) for item in entries)
    if width > MAX_SEQUENCE_LENGTH:
        raise TrainingInputV2Error("INPUT_SEQUENCE_TOO_LONG")
    batch = len(entries)
    return {
        "input_ids": (batch, width),
        "attention_mask": (batch, width),
        "primary_target": (batch,),
        "primary_active": (batch,),
        "secondary_target": (batch,),
        "secondary_active": (batch,),
    }


def build_batch_tensor_shape_contract(entries: tuple[Mapping[str, Any], ...]) -> tuple[dict[str, tuple[int, ...]], ...]:
    """Return dynamic-pad shapes for the physical batches in authority order."""

    return tuple(build_tensor_shape_contract(batch) for batch in build_partition_bound_batches(entries))


def preflight(root: str) -> dict[str, Any]:
    persisted = persist_training_input_authority(root)
    bundle = persisted["bundle"]
    train = tuple(bundle["train"])
    validation = tuple(bundle["validation"])
    train_batches = build_partition_bound_batches(train)
    validation_batches = build_partition_bound_batches(validation)
    return {
        "state": "P2_V2_INPUT_PREFLIGHT_PASS",
        "runnerModule": RUNNER_MODULE,
        "train": len(train),
        "validation": len(validation),
        "holdout": 0,
        "trainBatches": [len(item) for item in train_batches],
        "validationBatches": [len(item) for item in validation_batches],
        "trainShape": build_tensor_shape_contract(train),
        "validationShape": build_tensor_shape_contract(validation),
        "trainBatchShapes": build_batch_tensor_shape_contract(train),
        "validationBatchShapes": build_batch_tensor_shape_contract(validation),
        "sequenceLimit": MAX_SEQUENCE_LENGTH,
        "batchAuthority": BATCH_REFERENCE,
        "trainingInputAuthority": persisted["authority"]["reference"],
        "modelDeserializationCount": 0,
        "trainingCount": 0,
    }


@dataclass(frozen=True)
class ExecutionTensorView:
    input_ids: Any
    attention_mask: Any
    primary_target: Any
    secondary_target: Any
    primary_mask: Any
    secondary_mask: Any
    sequence_lengths: tuple[int, ...]


def _build_execution_tensor_view(
    entries: tuple[Mapping[str, Any], ...],
    corpus_examples: Mapping[str, Mapping[str, Any]],
    tokenizer: Any,
) -> ExecutionTensorView:
    import torch

    if not entries:
        raise TrainingInputV2Error("EMPTY_EXECUTION_SPLIT")
    encoded: list[list[int]] = []
    primary_targets: list[int] = []
    secondary_targets: list[int] = []
    primary_masks: list[float] = []
    secondary_masks: list[float] = []
    for entry in entries:
        example = corpus_examples.get(entry["exampleReference"])
        if not isinstance(example, Mapping):
            raise TrainingInputV2Error("EXECUTION_CORPUS_EXAMPLE_MISSING")
        representation = example.get("inputRepresentation")
        serialized = representation.get("serialized") if isinstance(representation, Mapping) else None
        if not isinstance(serialized, str):
            raise TrainingInputV2Error("EXECUTION_INPUT_SERIALIZATION_MISSING")
        try:
            ids = [int(value) for value in tokenizer.encode(serialized, add_special_tokens=True).ids]
        except Exception as error:
            raise TrainingInputV2Error("EXECUTION_TOKENIZATION_FAILED") from error
        if len(ids) != entry["sequenceLength"] or not ids or len(ids) > MAX_SEQUENCE_LENGTH or PAD_TOKEN_ID in ids:
            raise TrainingInputV2Error("EXECUTION_SEQUENCE_AUTHORITY_MISMATCH")
        encoded.append(ids)
        primary_targets.append({None: 0, "IDENTITY": 2, "VARIANT": 3}[entry["primaryTarget"]])
        secondary_targets.append({None: 0, "COMPATIBLE": 0, "REJECT": 1}[entry["secondaryTarget"]])
        primary_masks.append(float(entry["primaryActive"]))
        secondary_masks.append(float(entry["secondaryActive"]))
    width = max(len(ids) for ids in encoded)
    input_ids = [ids + [PAD_TOKEN_ID] * (width - len(ids)) for ids in encoded]
    attention = [[1] * len(ids) + [0] * (width - len(ids)) for ids in encoded]
    return ExecutionTensorView(
        torch.tensor(input_ids, dtype=torch.int64),
        torch.tensor(attention, dtype=torch.int64),
        torch.tensor(primary_targets, dtype=torch.int64),
        torch.tensor(secondary_targets, dtype=torch.int64),
        torch.tensor(primary_masks, dtype=torch.float32),
        torch.tensor(secondary_masks, dtype=torch.float32),
        tuple(len(ids) for ids in encoded),
    )


def execute_authorized_p2_v2(
    root: str | Path,
    *,
    runtime_authority_path: str | Path,
    readiness_path: str | Path,
    model_root: str | Path,
    tokenizer_path: str | Path,
    output_root: str | Path,
    expected_runtime_image_digest: str | None = None,
    model_loader: Callable[[], Any] | None = None,
) -> dict[str, Any]:
    """Run the frozen P2 V2 trajectory after the execution gate has passed.

    All numerical operations below reuse the existing Point-13 model, loss,
    trainability, optimizer, and checkpoint contracts.  The model loader is
    reached only after :func:`validate_execution_authority` succeeds.
    """

    binding = validate_execution_authority(
        root,
        runtime_authority_path=runtime_authority_path,
        readiness_path=readiness_path,
        expected_runtime_image_digest=expected_runtime_image_digest,
    )
    import torch
    from tokenizers import Tokenizer

    from .checkpoint_v2 import persist_checkpoint
    from .execution_device_v1 import resolve_him_execution_device_v1
    from .point13_loss_contract_v1 import build_him_masked_multi_objective_loss_contract_v1
    from .point13_loss_v1 import compute_him_masked_multi_objective_loss_v1
    from .point13_model_forward_v1 import (
        build_him_model_execution_binding_v1,
        load_pinned_him_multi_head_model_from_root_v1,
        load_pinned_model_config_from_root_v1,
    )
    from .point13_optimizer_construction_v1 import construct_him_adamw_v1
    from .point13_optimizer_execution_policy_v1 import build_him_optimizer_execution_policy_v1
    from .point13_trainability_policy_v1 import build_him_base_encoder_trainability_policy_v1
    from .point13_trainability_projection_v1 import project_him_trainability_policy_v1

    repository = Path(root)
    bundle = load_v2_input_bundle(repository)
    tokenizer_file = Path(tokenizer_path)
    if not tokenizer_file.is_file() or hashlib.sha256(tokenizer_file.read_bytes()).hexdigest() != TOKENIZER_SHA256:
        raise TrainingInputV2Error("TOKENIZER_AUTHORITY_MISMATCH")
    tokenizer = Tokenizer.from_file(str(tokenizer_path))
    if tokenizer.token_to_id("<pad>") != TOKENIZER_PAD_TOKEN_ID or tokenizer.token_to_id("<s>") != TOKENIZER_BOS_TOKEN_ID or tokenizer.token_to_id("</s>") != TOKENIZER_EOS_TOKEN_ID:
        raise TrainingInputV2Error("TOKENIZER_SPECIAL_TOKEN_AUTHORITY_MISMATCH")
    tokenizer.no_truncation()
    corpus_examples = {item["exampleReference"]: item for item in bundle["corpus"]["examples"]}
    train_view = _build_execution_tensor_view(tuple(bundle["train"]), corpus_examples, tokenizer)
    validation_view = _build_execution_tensor_view(tuple(bundle["validation"]), corpus_examples, tokenizer)
    if bundle["holdout"]:
        raise TrainingInputV2Error("HOLDOUT_EXECUTION_FORBIDDEN")
    device = resolve_him_execution_device_v1("CUDA", 0, torch_module=torch)
    if model_loader is None:
        model_path = Path(model_root)

        def model_loader() -> Any:
            config = load_pinned_model_config_from_root_v1(model_path)
            model_binding_digest = binding.runtime_authority.get("modelBindingDigest")
            if not isinstance(model_binding_digest, str):
                raise TrainingInputV2Error("MODEL_BINDING_AUTHORITY_MISSING")
            execution_binding = build_him_model_execution_binding_v1(config, 7, model_binding_digest)
            return load_pinned_him_multi_head_model_from_root_v1(execution_binding, model_binding_digest, 7, model_path, device)

    model = model_loader()
    model.train()
    policy = build_him_optimizer_execution_policy_v1(device_policy="CUDA", gradient_accumulation_steps=1)
    trainability = build_him_base_encoder_trainability_policy_v1()
    projection = project_him_trainability_policy_v1(model, trainability)
    optimizer, _ = construct_him_adamw_v1(model, policy, projection)
    loss_contract = build_him_masked_multi_objective_loss_contract_v1()
    optimizer_step = 0
    metrics: list[dict[str, Any]] = []

    def evaluate(epoch: int) -> None:
        model.eval()
        with torch.no_grad():
            output = model(validation_view.input_ids.to(device), validation_view.attention_mask.to(device))
            losses = compute_him_masked_multi_objective_loss_v1(
                primary_logits=output.primary_logits,
                secondary_logits=output.secondary_logits,
                primary_target=validation_view.primary_target.to(device),
                secondary_target=validation_view.secondary_target.to(device),
                primary_mask=validation_view.primary_mask.to(device),
                secondary_mask=validation_view.secondary_mask.to(device),
                loss_contract=loss_contract,
                selected_execution_device=device,
                allow_primary_target_absence=True,
            )
            metrics.append({"epoch": epoch, "validationLoss": float(losses.total_loss.detach().cpu().item()), "validationExampleCount": VALIDATION_COUNT})
        model.train()

    for epoch in range(EPOCHS):
        for start in range(0, TRAIN_COUNT, PHYSICAL_BATCH_SIZE):
            end = start + PHYSICAL_BATCH_SIZE
            optimizer.zero_grad(set_to_none=True)
            output = model(train_view.input_ids[start:end].to(device), train_view.attention_mask[start:end].to(device))
            losses = compute_him_masked_multi_objective_loss_v1(
                primary_logits=output.primary_logits,
                secondary_logits=output.secondary_logits,
                primary_target=train_view.primary_target[start:end].to(device),
                secondary_target=train_view.secondary_target[start:end].to(device),
                primary_mask=train_view.primary_mask[start:end].to(device),
                secondary_mask=train_view.secondary_mask[start:end].to(device),
                loss_contract=loss_contract,
                selected_execution_device=device,
                allow_primary_target_absence=True,
            )
            losses.total_loss.backward()
            optimizer.step()
            optimizer_step += 1
            metrics.append({"epoch": epoch + 1, "optimizerStep": optimizer_step, "loss": float(losses.total_loss.detach().cpu().item())})
        evaluate(epoch + 1)
    evaluate(EPOCHS)
    run_reference = f"productive-training-run:p2-v2:{binding.runtime_authority['logicalDigest']}:{binding.readiness['logicalDigest']}"
    runtime_identity = {"device": str(device), "runtimeImageDigest": binding.runtime_image_digest, "modelRevision": "e73636d4f797dec63c3081bb6ed5c7b0bb3f2089", "tokenizerSha256": "a898ea75433890f6610f4e470b8ebeb0c21dce5c8dd61f892eb09eb5919d2e2c"}
    optimizer_identity = {"optimizerId": "optimizer:adamw:v1", "learningRate": "0.0001", "weightDecay": "0.01", "gradientAccumulationSteps": 1, "seed": 7}
    authority_bindings = {
        "runtimeAuthorityReference": binding.runtime_authority["reference"],
        "runtimeAuthorityLogicalDigest": binding.runtime_authority["logicalDigest"],
        "readinessReference": binding.readiness["reference"],
        "readinessLogicalDigest": binding.readiness["logicalDigest"],
        "trainingInputAuthorityReference": binding.input_authority_reference,
        "corpusReference": bundle["corpus"]["reference"],
        "partitionReference": bundle["partition"]["reference"],
        "batchAuthorityReference": bundle["batch"]["reference"],
        "sequenceReference": "sequence-length-authority:v2:97130457decd4f283492da2d09a0faddb4bb99fc70f7d79732fbd390794cc509",
        "modelBindingDigest": binding.runtime_authority["modelBindingDigest"],
        "runtimeIdentity": dict(runtime_identity),
        "optimizerIdentity": dict(optimizer_identity),
    }
    checkpoint = persist_checkpoint(output_root, model=model, optimizer=optimizer, run_reference=run_reference, optimizer_step=optimizer_step, authority_bindings=authority_bindings, runtime_identity=runtime_identity, optimizer_identity=optimizer_identity, training_history=tuple(metrics))
    return {"state": "P2_V2_RUN_COMPLETED", "runReference": run_reference, "optimizerSteps": optimizer_step, "validationForwardCount": EPOCHS + 1, "holdoutForwardCount": 0, "checkpoint": checkpoint.evidence_fields(), "metrics": metrics}


def future_training_command() -> tuple[str, ...]:
    return (
        "python", "-m", RUNNER_MODULE, "--execute",
        "--corpus-v2", "<authoritative corpus.v2.json>",
        "--partition-v2", "<authoritative partition.v2.json>",
        "--batch-authority", "<updated authorized batch authority>",
        "--training-input-authority", "<training-input-authority.v2.json>",
        "--runtime-authority", "<updated training-runtime-authority.v2.json>",
        "--output-root", "<fresh immutable p2-v2 qualification output root>",
    )


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(prog=RUNNER_MODULE)
    parser.add_argument("--root", default=".")
    parser.add_argument("--preflight", action="store_true")
    parser.add_argument("--execute", action="store_true")
    parser.add_argument("--execution-preflight", action="store_true")
    parser.add_argument("--runtime-authority")
    parser.add_argument("--training-readiness")
    parser.add_argument("--runtime-image-digest")
    parser.add_argument("--model-root")
    parser.add_argument("--tokenizer-path")
    parser.add_argument("--output-root")
    args = parser.parse_args(argv)
    if args.execute:
        if not args.runtime_authority or not args.training_readiness:
            parser.error(EXECUTION_AUTHORITY_ERROR)
        try:
            result = execution_preflight(
                args.root,
                runtime_authority_path=args.runtime_authority,
                readiness_path=args.training_readiness,
                expected_runtime_image_digest=args.runtime_image_digest,
            )
        except (TrainingInputV2Error, OSError, UnicodeError, ValueError) as error:
            parser.error(str(error))
        if args.execution_preflight:
            print(json.dumps(result, ensure_ascii=False, sort_keys=True))
            return 0
        if not args.model_root or not args.tokenizer_path or not args.output_root:
            parser.error("REAL_P2_V2_TRAINING_EXECUTION_ARTIFACT_ROOTS_REQUIRED")
        try:
            evidence = execute_authorized_p2_v2(
                args.root,
                runtime_authority_path=args.runtime_authority,
                readiness_path=args.training_readiness,
                model_root=args.model_root,
                tokenizer_path=args.tokenizer_path,
                output_root=args.output_root,
                expected_runtime_image_digest=args.runtime_image_digest,
            )
        except (TrainingInputV2Error, OSError, UnicodeError, ValueError) as error:
            parser.error(str(error))
        print(json.dumps({"state": evidence["state"], "runReference": evidence["runReference"]}, ensure_ascii=False, sort_keys=True))
        return 0
    if args.execution_preflight:
        parser.error("--execution-preflight_requires_--execute")
    print(json.dumps(preflight(args.root), ensure_ascii=False, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())


__all__ = [
    "EXECUTION_AUTHORITY_ERROR", "ExecutionAuthorityBinding", "RUNNER_MODULE",
    "build_batch_tensor_shape_contract", "build_partition_bound_batches", "build_tensor_shape_contract",
    "execution_preflight", "execute_authorized_p2_v2", "future_training_command", "preflight",
    "validate_execution_authority",
]
