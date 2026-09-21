"""Deterministic, offline materialization of frozen V3 model authorities."""
from __future__ import annotations
import hashlib
from pathlib import Path
from shutil import copyfile

MODEL_REVISION = "e73636d4f797dec63c3081bb6ed5c7b0bb3f2089"
MODEL_WEIGHTS_SHA256 = "6fd4797bc397c3b8b55d6bb5740366b57e6a3ce91c04c77f22aafc0c128e6feb"
TOKENIZER_SHA256 = "a898ea75433890f6610f4e470b8ebeb0c21dce5c8dd61f892eb09eb5919d2e2c"

def _digest(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""): h.update(chunk)
    return h.hexdigest()

def _verified_copy(source: Path, target: Path, expected: str) -> None:
    if not source.is_file() or _digest(source) != expected:
        raise ValueError(f"FROZEN_AUTHORITY_SOURCE_DIGEST_MISMATCH:{source}")
    target.parent.mkdir(parents=True, exist_ok=True)
    if target.exists() and _digest(target) != expected:
        raise ValueError(f"FROZEN_AUTHORITY_TARGET_DIGEST_MISMATCH:{target}")
    if not target.exists(): copyfile(source, target)
    if _digest(target) != expected:
        raise ValueError(f"FROZEN_AUTHORITY_TARGET_DIGEST_MISMATCH:{target}")

def _frozen_authority_root(workspace_root: Path) -> Path:
    """Frozen repository mirror is sibling to the productive /workspace/him tree."""
    if workspace_root.name == "him" and workspace_root.parent.name == "workspace":
        return workspace_root.parent / "training/him"
    if (workspace_root / "training/him/models").is_dir():
        return workspace_root / "training/him"
    return workspace_root

def materialize_model_tokenizer(root: str | Path = ".", target_root: str | Path | None = None) -> dict[str, str]:
    root = Path(root)
    target_root = Path(target_root) if target_root is not None else root / "retraining-v3-authority"
    source = _frozen_authority_root(root) / "models/xlm-roberta-base" / MODEL_REVISION
    model_target = target_root / "model"
    tokenizer_target = target_root / "tokenizer"
    _verified_copy(source / "model.safetensors", model_target / "model.safetensors", MODEL_WEIGHTS_SHA256)
    _verified_copy(source / "tokenizer.json", tokenizer_target / "tokenizer.json", TOKENIZER_SHA256)
    for name, destination in (("config.json", model_target), ("tokenizer_config.json", tokenizer_target), ("sentencepiece.bpe.model", tokenizer_target)):
        src = source / name
        if not src.is_file(): raise ValueError(f"FROZEN_AUTHORITY_FILE_MISSING:{src}")
        _verified_copy(src, destination / name, _digest(src))
    return {"modelRoot": str(model_target), "tokenizerRoot": str(tokenizer_target), "modelSha256": _digest(model_target / "model.safetensors"), "tokenizerSha256": _digest(tokenizer_target / "tokenizer.json")}

__all__ = ["materialize_model_tokenizer", "MODEL_REVISION", "MODEL_WEIGHTS_SHA256", "TOKENIZER_SHA256"]
