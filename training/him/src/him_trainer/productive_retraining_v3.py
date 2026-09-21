"""Model-free control path for the frozen HIM V2 V3 retraining corpus.

This versioned path intentionally leaves historical V2 training/evaluation
entrypoints untouched. TRAIN and relational Development share one serializer.
"""
from __future__ import annotations
import hashlib, json
from pathlib import Path
from typing import Any, Mapping
from .input_representation_v3 import serialize_contextual_input_v3

CORPUS_PATH = Path("data/knowledge/him/training/v2/prospective/prospective-retraining-corpus-v1.json")
DEVELOPMENT_PATH = Path("data/knowledge/him/training/v2/prospective/prospective-relational-development-v1.json")
MANIFEST_PATH = Path("data/knowledge/him/training/v2/prospective/prospective-retraining-corpus-manifest-v1.json")
CORPUS_DIGEST = "1dd057734e9f560c87aac6ce13f24a2a513be4e7d1d1e26289a2b59ca9d2befc"
DEVELOPMENT_DIGEST = "fb3b2589fdb7276ee6b94f64b71b4b5bfbd984a508dce8d6566603fa08fcfa00"
MANIFEST_DIGEST = "6ccec1620703e13807144ed7be54901ceed5dc6c89f950fb6ac356b09a7d9c44"
PHYSICAL_BATCH_SIZE = 8
EPOCHS = 3
HISTORICAL_RUN_ID = "5af227d4-7ed1-4367-aec4-f42d2b83042d"

def _digest_without_logical(value: Mapping[str, Any]) -> str:
    body = dict(value); body.pop("logicalDigest", None)
    return hashlib.sha256(json.dumps(body, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode()).hexdigest()

def _load(path: Path, expected: str) -> dict[str, Any]:
    value = json.loads(path.read_text(encoding="utf-8"))
    if _digest_without_logical(value) != expected or value.get("logicalDigest") != expected:
        raise ValueError(f"AUTHORITY_DIGEST_MISMATCH:{path}")
    return value

def load_retraining_authorities(root: str | Path = ".") -> tuple[dict[str, Any], dict[str, Any], dict[str, Any]]:
    root = Path(root)
    corpus = _load(root / CORPUS_PATH, CORPUS_DIGEST)
    development = _load(root / DEVELOPMENT_PATH, DEVELOPMENT_DIGEST)
    manifest = _load(root / MANIFEST_PATH, MANIFEST_DIGEST)
    if len(corpus["records"]) != 328 or len(development["records"]) != 6:
        raise ValueError("RETRAINING_AUTHORITY_RECORD_COUNT_MISMATCH")
    if manifest["trainArtifactDigest"] != CORPUS_DIGEST or manifest["developmentArtifactDigest"] != DEVELOPMENT_DIGEST:
        raise ValueError("RETRAINING_MANIFEST_BINDING_MISMATCH")
    return corpus, development, manifest

def serialize_record(record: Mapping[str, Any]) -> str:
    """The sole TRAIN/Development model-input serializer."""
    context = record.get("inputRepresentation", {}).get("context", record)
    return serialize_contextual_input_v3(dict(context))

def prepare_partition(records: list[Mapping[str, Any]] | tuple[Mapping[str, Any], ...]) -> tuple[dict[str, Any], ...]:
    return tuple({"record": dict(r), "serializedInput": serialize_record(r)} for r in records)

def prepare_retraining_inputs(root: str | Path = ".") -> dict[str, Any]:
    corpus, development, manifest = load_retraining_authorities(root)
    return {"train": prepare_partition(corpus["records"]), "development": prepare_partition(development["records"]), "manifest": manifest}

def training_count_contract(record_count: int = 328) -> dict[str, int]:
    batches = (record_count + PHYSICAL_BATCH_SIZE - 1) // PHYSICAL_BATCH_SIZE
    return {"batchesPerEpoch": batches, "optimizerStepsPerEpoch": batches, "totalOptimizerSteps": batches * EPOCHS}

def new_run_namespace(corpus_digest: str = CORPUS_DIGEST, build_context_digest: str | None = None, root: str | Path = ".") -> dict[str, str]:
    """Derive a stable namespace from both frozen data and runtime generation."""
    if build_context_digest is None:
        identity_path = Path(root) / "training/him/runtime/a100/runtime-identity.json"
        identity = json.loads(identity_path.read_text(encoding="utf-8"))
        build_context_digest = str(identity["buildContextDigest"])
    if len(corpus_digest) != 64 or len(build_context_digest) != 64:
        raise ValueError("RUN_NAMESPACE_DIGEST_INVALID")
    run = "retraining-v3-" + build_context_digest[:12]
    return {"runId": run, "outputRoot": f"training-output/{run}", "checkpointRoot": f"training-output/{run}/checkpoint", "claimPath": f"training-output/{run}/exactly-once-claim.json", "state": "NOT_STARTED", "corpusDigest": corpus_digest, "buildContextDigest": build_context_digest}

# --- Productive orchestration contract (versioned; model execution is opt-in) ---
from dataclasses import dataclass
from typing import Callable
from .exactly_once_claim_v2 import create_claim_atomically, reload_claim, transition_claim

EXECUTION_ENTRYPOINT = "him_trainer.productive_retraining_v3.execute_productive_retraining_v3"
MODEL_ID = "FacebookAI/xlm-roberta-base"
MODEL_REVISION = "e73636d4f797dec63c3081bb6ed5c7b0bb3f2089"
MODEL_WEIGHTS_SHA256 = "6fd4797bc397c3b8b55d6bb5740366b57e6a3ce91c04c77f22aafc0c128e6feb"
TOKENIZER_SHA256 = "a898ea75433890f6610f4e470b8ebeb0c21dce5c8dd61f892eb09eb5919d2e2c"

def _load_deployed_runtime_identity() -> dict[str, Any]:
    """Load the immutable runtime identity authority used by the deployed image."""
    candidates = (
        Path("/opt/him/runtime/runtime-identity.json"),
        Path("training/him/runtime/a100/runtime-identity.json"),
    )
    for path in candidates:
        if path.is_file():
            value = json.loads(path.read_text(encoding="utf-8"))
            if not isinstance(value, dict) or not value:
                raise RuntimeError("RUNTIME_IDENTITY_AUTHORITY_INVALID")
            return dict(value)
    raise RuntimeError("RUNTIME_IDENTITY_AUTHORITY_MISSING")

@dataclass(frozen=True)
class ProductiveRetrainingPlan:
    run_id: str
    output_root: Path
    checkpoint_root: Path
    claim_path: Path
    train_count: int = 328
    development_count: int = 6
    epochs: int = 3
    batches_per_epoch: int = 41
    total_optimizer_steps: int = 123
    serializer_entrypoint: str = "him_trainer.input_representation_v3.serialize_contextual_input_v3"
    corpus_digest: str = CORPUS_DIGEST
    build_context_digest: str = ""

def build_productive_retraining_plan(root: str | Path = ".") -> ProductiveRetrainingPlan:
    root = Path(root); ns = new_run_namespace(root=root)
    return ProductiveRetrainingPlan(ns["runId"], root/ns["outputRoot"], root/ns["checkpointRoot"], root/ns["claimPath"], corpus_digest=ns["corpusDigest"], build_context_digest=ns["buildContextDigest"])

def preflight_productive_retraining_v3(root: str | Path = ".") -> dict[str, Any]:
    corpus, development, manifest = load_retraining_authorities(root)
    plan = build_productive_retraining_plan(root)
    if plan.corpus_digest != CORPUS_DIGEST or plan.build_context_digest != json.loads((Path(root) / "training/him/runtime/a100/runtime-identity.json").read_text(encoding="utf-8"))["buildContextDigest"]:
        raise RuntimeError("RUN_NAMESPACE_AUTHORITY_BINDING_MISMATCH")
    if plan.claim_path.exists():
        raise RuntimeError("RETRAINING_NAMESPACE_ALREADY_CLAIMED")
    return {"state":"PREFLIGHT_PASS", "plan":plan, "corpus":corpus, "development":development,
            "manifest":manifest, "trainInputs":prepare_partition(corpus["records"]),
            "developmentInputs":prepare_partition(development["records"]),
            "model":{"id":MODEL_ID,"revision":MODEL_REVISION,"weightsSha256":MODEL_WEIGHTS_SHA256},
            "tokenizerSha256":TOKENIZER_SHA256, "holdoutAccessCount":0}

@dataclass(frozen=True)
class ProductiveExecutionPrimitives:
    """Concrete primitives used by the authorized V3 execution path."""
    claim_create: Callable[..., Any]
    claim_reload: Callable[..., Any]
    claim_transition: Callable[..., Any]
    training: Callable[..., Mapping[str, Any]]
    development: Callable[..., Mapping[str, Any]]

def _training_primitive(inputs: tuple[dict[str, Any], ...], plan: ProductiveRetrainingPlan) -> Mapping[str, Any]:
    """Execute the frozen V3 loop with the repository's model primitives."""
    if len(inputs) != plan.train_count or training_count_contract(len(inputs)) != {"batchesPerEpoch": 41, "optimizerStepsPerEpoch": 41, "totalOptimizerSteps": 123}:
        raise RuntimeError("TRAINING_COUNT_OR_POLICY_DRIFT")
    import torch
    from tokenizers import Tokenizer
    from .point13_model_forward_v1 import build_him_model_execution_binding_v1, load_pinned_him_multi_head_model_from_root_v1, load_pinned_model_config_from_root_v1
    from .point13_loss_contract_v1 import build_him_masked_multi_objective_loss_contract_v1
    from .point13_loss_v1 import compute_him_masked_multi_objective_loss_v1
    from .point13_optimizer_construction_v1 import construct_him_adamw_v1
    from .point13_optimizer_execution_policy_v1 import build_him_optimizer_execution_policy_v1
    from .point13_trainability_policy_v1 import build_him_base_encoder_trainability_policy_v1
    from .point13_trainability_projection_v1 import project_him_trainability_policy_v1
    from .execution_device_v1 import resolve_him_execution_device_v1
    from .checkpoint_v2 import persist_checkpoint
    root = plan.output_root.parents[1]
    model_root = root / "training/him/models/xlm-roberta-base" / MODEL_REVISION
    tokenizer_path = model_root / "tokenizer.json"
    if not model_root.is_dir() or not tokenizer_path.is_file() or hashlib.sha256((model_root / "model.safetensors").read_bytes()).hexdigest() != MODEL_WEIGHTS_SHA256 or hashlib.sha256(tokenizer_path.read_bytes()).hexdigest() != TOKENIZER_SHA256:
        raise RuntimeError("V3_MODEL_TOKENIZER_AUTHORITY_MISMATCH")
    tokenizer = Tokenizer.from_file(str(tokenizer_path)); tokenizer.no_truncation()
    device = resolve_him_execution_device_v1("CUDA", 0, torch_module=torch)
    config = load_pinned_model_config_from_root_v1(model_root)
    binding = build_him_model_execution_binding_v1(config, 7, "V3_RETRAINING_MODEL_BINDING")
    model = load_pinned_him_multi_head_model_from_root_v1(binding, "V3_RETRAINING_MODEL_BINDING", 7, model_root, device)
    model.train()
    policy = build_him_optimizer_execution_policy_v1(device_policy="CUDA", gradient_accumulation_steps=1)
    projection = project_him_trainability_policy_v1(model, build_him_base_encoder_trainability_policy_v1())
    optimizer, _ = construct_him_adamw_v1(model, policy, projection)
    loss_contract = build_him_masked_multi_objective_loss_contract_v1()
    optimizer_steps, metrics = 0, []
    for epoch in range(EPOCHS):
        for start in range(0, len(inputs), PHYSICAL_BATCH_SIZE):
            batch = inputs[start:start + PHYSICAL_BATCH_SIZE]
            encoded = [tokenizer.encode(x["serializedInput"], add_special_tokens=True).ids for x in batch]
            if any(len(ids) > 256 or not ids for ids in encoded): raise RuntimeError("V3_SEQUENCE_LENGTH_DRIFT")
            width = max(map(len, encoded)); ids = torch.tensor([x + [1] * (width-len(x)) for x in encoded], dtype=torch.int64, device=device); mask = torch.tensor([[1]*len(x)+[0]*(width-len(x)) for x in encoded], dtype=torch.int64, device=device)
            primary = torch.tensor([{"IDENTITY":2,"VARIANT":3}.get(x["record"].get("targetKind"), 0) for x in batch], dtype=torch.int64, device=device)
            secondary = torch.tensor([{"COMPATIBLE":0,"REJECT":1}.get(x["record"].get("candidateCompatibility"), 0) for x in batch], dtype=torch.int64, device=device)
            primary_mask = torch.tensor([0.0 if x["record"].get("targetKind") in (None,"NOT_APPLICABLE") else 1.0 for x in batch], device=device); secondary_mask = torch.ones(len(batch), device=device)
            optimizer.zero_grad(set_to_none=True); output = model(ids, mask)
            losses = compute_him_masked_multi_objective_loss_v1(primary_logits=output.primary_logits, secondary_logits=output.secondary_logits, primary_target=primary, secondary_target=secondary, primary_mask=primary_mask, secondary_mask=secondary_mask, loss_contract=loss_contract, selected_execution_device=device, allow_primary_target_absence=True)
            losses.total_loss.backward(); optimizer.step(); optimizer_steps += 1; metrics.append({"epoch":epoch+1,"optimizerStep":optimizer_steps,"loss":float(losses.total_loss.detach().cpu())})
    if optimizer_steps != 123: raise RuntimeError("OPTIMIZER_STEP_COUNT_INVALID")
    runtime_identity = _load_deployed_runtime_identity()
    optimizer_identity = {"optimizerId": "optimizer:adamw:v1"}
    authority_bindings = {
        "serializer": "him_trainer.input_representation_v3.serialize_contextual_input_v3",
        "runtimeIdentity": runtime_identity,
        "optimizerIdentity": optimizer_identity,
    }
    checkpoint = persist_checkpoint(plan.checkpoint_root, model=model, optimizer=optimizer, run_reference=f"productive-retraining-v3:{plan.run_id}", optimizer_step=optimizer_steps, authority_bindings=authority_bindings, runtime_identity=runtime_identity, optimizer_identity=optimizer_identity, training_history=tuple(metrics))
    return {"state":"V3_TRAINING_COMPLETED","model":model,"optimizer":optimizer,"checkpoint":checkpoint.evidence_fields(),"optimizerSteps":optimizer_steps,"metrics":metrics}

def _development_primitive(inputs: tuple[dict[str, Any], ...], checkpoint: Any, plan: ProductiveRetrainingPlan) -> Mapping[str, Any]:
    if len(inputs) != plan.development_count:
        raise RuntimeError("DEVELOPMENT_COUNT_DRIFT")
    from .productive_development_v3 import evaluate_v3_development
    model = checkpoint.get("model") if isinstance(checkpoint, Mapping) else checkpoint
    if not hasattr(model, "predict_v3"):
        raise RuntimeError("V3_DEVELOPMENT_MODEL_PREDICTION_PROTOCOL_MISSING")
    return evaluate_v3_development(
        authority_path=DEVELOPMENT_PATH,
        model=model,
        checkpoint=checkpoint,
        output_path=plan.output_root / "development-result.json",
    )

def resolve_productive_execution_primitives() -> ProductiveExecutionPrimitives:
    return ProductiveExecutionPrimitives(create_claim_atomically, reload_claim, transition_claim, _training_primitive, _development_primitive)

def execute_productive_retraining_v3(*, root: str | Path = ".", execute: bool = False) -> dict[str, Any]:
    """Run the single authorized sequence using repository-bound primitives."""
    pre = preflight_productive_retraining_v3(root)
    if not execute:
        return {"state":"PRE_FIRST_FORWARD_READY", "plan":pre["plan"], "modelForwardCount":0,
                "backwardCount":0,"optimizerStepCount":0,"checkpointWriteCount":0}
    plan: ProductiveRetrainingPlan = pre["plan"]
    from .retraining_authority_materialization_v1 import materialize_model_tokenizer
    materialization = materialize_model_tokenizer(root)
    primitives = resolve_productive_execution_primitives()
    primitives.claim_create(plan.claim_path, {"executionFamily":"HIM_V2_RETRAINING_V3","executionMode":"TRAINING_ONLY","bundleReference":manifest_ref(pre["manifest"]),"runId":plan.run_id})
    try:
        training = dict(primitives.training(pre["trainInputs"], plan))
        checkpoint = training.get("checkpoint")
        if checkpoint is None: raise RuntimeError("CHECKPOINT_REQUIRED_BEFORE_EVALUATION")
        evaluation = dict(primitives.development(pre["developmentInputs"], checkpoint, plan))
        result = {"runId":plan.run_id,"materialization":materialization,"training":training,"development":evaluation,"claim":primitives.claim_transition(plan.claim_path,"COMPLETED")}
        plan.output_root.mkdir(parents=True, exist_ok=True); (plan.output_root/"execution-result.json").write_text(json.dumps(result,ensure_ascii=False,sort_keys=True,indent=2)+"\n")
        return result
    except Exception:
        primitives.claim_transition(plan.claim_path,"FAILED")
        raise

def manifest_ref(manifest: Mapping[str, Any]) -> str:
    return f"HIM_V2_RETRAINING_MANIFEST_V1:{manifest['logicalDigest']}"
