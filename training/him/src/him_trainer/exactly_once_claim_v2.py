"""Crash durable, exclusive Exactly-once claim state for HIM V2."""
from __future__ import annotations
import hashlib, json, os, tempfile
from pathlib import Path
from typing import Any, Mapping

SCHEMA = "HIM_V2_EXACTLY_ONCE_CLAIM_V1"
STATES = {"UNCLAIMED", "CLAIMED", "COMPLETED", "FAILED", "INTERRUPTED"}

def _canon(v: Any) -> bytes:
    return json.dumps(v, ensure_ascii=False, sort_keys=True, separators=(",", ":"), allow_nan=False).encode()
def logical_digest(v: Any) -> str: return hashlib.sha256(_canon(v)).hexdigest()

def create_claim_atomically(path: str | Path, payload: Mapping[str, Any]) -> dict[str, Any]:
    target = Path(path); target.parent.mkdir(parents=True, exist_ok=True)
    core = {"schema": SCHEMA, "version": 1, "state": "CLAIMED", **dict(payload)}
    core["claimKey"] = logical_digest({k: core[k] for k in ("schema", "version", "executionFamily", "executionMode", "bundleReference", "runtimeImageDigest", "executionAuthorityReference", "executionContractReference") if k in core})
    core["logicalDigest"] = logical_digest(core)
    data = _canon(core) + b"\n"
    try:
        fd = os.open(target, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    except FileExistsError as e:
        raise RuntimeError("REJECTED_ALREADY_CLAIMED") from e
    try:
        with os.fdopen(fd, "wb") as f: f.write(data); f.flush(); os.fsync(f.fileno())
    except Exception:
        try: target.unlink()
        except OSError: pass
        raise
    return core

def reload_claim(path: str | Path) -> dict[str, Any]:
    d = json.loads(Path(path).read_text())
    if d.get("schema") != SCHEMA or d.get("state") not in STATES: raise ValueError("CLAIM_SCHEMA_OR_STATE_INVALID")
    digest = d.get("logicalDigest"); core = {k:v for k,v in d.items() if k != "logicalDigest"}
    if digest != logical_digest(core): raise ValueError("CLAIM_DIGEST_INVALID")
    return d

def transition_claim(path: str | Path, state: str) -> dict[str, Any]:
    if state not in {"COMPLETED", "FAILED", "INTERRUPTED"}: raise ValueError("CLAIM_TERMINAL_STATE_INVALID")
    d = reload_claim(path)
    if d["state"] != "CLAIMED": raise ValueError("CLAIM_NOT_CLAIMED")
    d["state"] = state; d["logicalDigest"] = logical_digest({k:v for k,v in d.items() if k != "logicalDigest"})
    fd,tmp=tempfile.mkstemp(dir=str(Path(path).parent));
    with os.fdopen(fd,"wb") as f:f.write(_canon(d)+b"\n");f.flush();os.fsync(f.fileno())
    os.replace(tmp,path); return d
