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

def new_run_namespace(corpus_digest: str = CORPUS_DIGEST) -> dict[str, str]:
    run = "retraining-v3-" + corpus_digest[:16]
    return {"runId": run, "outputRoot": f"training-output/{run}", "checkpointRoot": f"training-output/{run}/checkpoint", "claimPath": f"training-output/{run}/exactly-once-claim.json", "state": "NOT_STARTED"}

# --- Productive orchestration contract (versioned; model execution is opt-in) ---
from dataclasses import dataclass
from typing import Callable
from .exactly_once_claim_v2 import create_claim_atomically, reload_claim, transition_claim

EXECUTION_ENTRYPOINT = "him_trainer.productive_retraining_v3.execute_productive_retraining_v3"
MODEL_ID = "FacebookAI/xlm-roberta-base"
MODEL_REVISION = "e73636d4f797dec63c3081bb6ed5c7b0bb3f2089"
MODEL_WEIGHTS_SHA256 = "6fd4797bc397c3b8b55d6bb5740366b57e6a3ce91c04c77f22aafc0c128e6feb"
TOKENIZER_SHA256 = "a898ea75433890f6610f4e470b8ebeb0c21dce5c8dd61f892eb09eb5919d2e2c"

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

def build_productive_retraining_plan(root: str | Path = ".") -> ProductiveRetrainingPlan:
    root = Path(root); ns = new_run_namespace()
    return ProductiveRetrainingPlan(ns["runId"], root/ns["outputRoot"], root/ns["checkpointRoot"], root/ns["claimPath"])

def preflight_productive_retraining_v3(root: str | Path = ".") -> dict[str, Any]:
    corpus, development, manifest = load_retraining_authorities(root)
    plan = build_productive_retraining_plan(root)
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
    """Bind the repository training authority and fail closed if unavailable."""
    from .productive_training_p2_v2 import PHYSICAL_BATCH_SIZE, GRADIENT_ACCUMULATION_STEPS, EPOCHS
    if len(inputs) != plan.train_count or (PHYSICAL_BATCH_SIZE, GRADIENT_ACCUMULATION_STEPS, EPOCHS) != (8, 1, 3):
        raise RuntimeError("TRAINING_COUNT_OR_POLICY_DRIFT")
    raise RuntimeError("PRODUCTIVE_TRAINING_AUTHORITY_BINDING_REQUIRES_DEPLOYED_EXECUTION_ARTIFACTS")

def _development_primitive(inputs: tuple[dict[str, Any], ...], checkpoint: Any, plan: ProductiveRetrainingPlan) -> Mapping[str, Any]:
    if len(inputs) != plan.development_count:
        raise RuntimeError("DEVELOPMENT_COUNT_DRIFT")
    raise RuntimeError("PRODUCTIVE_DEVELOPMENT_AUTHORITY_BINDING_REQUIRES_DEPLOYED_EVALUATION_ARTIFACTS")

def resolve_productive_execution_primitives() -> ProductiveExecutionPrimitives:
    return ProductiveExecutionPrimitives(create_claim_atomically, reload_claim, transition_claim, _training_primitive, _development_primitive)

def execute_productive_retraining_v3(*, root: str | Path = ".", execute: bool = False) -> dict[str, Any]:
    """Run the single authorized sequence using repository-bound primitives."""
    pre = preflight_productive_retraining_v3(root)
    if not execute:
        return {"state":"PRE_FIRST_FORWARD_READY", "plan":pre["plan"], "modelForwardCount":0,
                "backwardCount":0,"optimizerStepCount":0,"checkpointWriteCount":0}
    plan: ProductiveRetrainingPlan = pre["plan"]
    primitives = resolve_productive_execution_primitives()
    primitives.claim_create(plan.claim_path, {"executionFamily":"HIM_V2_RETRAINING_V3","executionMode":"TRAINING_ONLY","bundleReference":manifest_ref(pre["manifest"]),"runId":plan.run_id})
    try:
        training = dict(primitives.training(pre["trainInputs"], plan))
        checkpoint = training.get("checkpoint")
        if checkpoint is None: raise RuntimeError("CHECKPOINT_REQUIRED_BEFORE_EVALUATION")
        evaluation = dict(primitives.development(pre["developmentInputs"], checkpoint, plan))
        result = {"runId":plan.run_id,"training":training,"development":evaluation,"claim":primitives.claim_transition(plan.claim_path,"COMPLETED")}
        plan.output_root.mkdir(parents=True, exist_ok=True); (plan.output_root/"execution-result.json").write_text(json.dumps(result,ensure_ascii=False,sort_keys=True,indent=2)+"\n")
        return result
    except Exception:
        primitives.claim_transition(plan.claim_path,"FAILED")
        raise

def manifest_ref(manifest: Mapping[str, Any]) -> str:
    return f"HIM_V2_RETRAINING_MANIFEST_V1:{manifest['logicalDigest']}"
