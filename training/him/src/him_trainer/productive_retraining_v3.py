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
