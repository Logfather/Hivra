"""Deterministic code-only comparison of two Open Food Facts snapshots.

This module deliberately does not redefine the internal OFF record identity.
The native ``source.code`` value is used only as a stable cross-snapshot key.
The optimized HIM artifact stores it as ``source.code``; the raw OFF export
stores the same source-native identifier as ``_id`` (with ``code`` as a
compatibility fallback). Rows without a non-empty string code are excluded.
"""

from __future__ import annotations

import gzip
import hashlib
import json
from collections import Counter
from pathlib import Path
from typing import Any, Iterable, Mapping


SCHEMA = "HIM_FRESH_OFF_CODE_DELTA_INVENTORY_V2"
CODE_DIGEST_ALGORITHM = "sha256(sorted unique source.code values joined by LF, trailing LF)"


def _canonical(value: Any) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def logical_digest(value: Any) -> str:
    return hashlib.sha256(_canonical(value)).hexdigest()


def _codes(path: str | Path) -> tuple[set[str], int, int, int, int]:
    counts: Counter[str] = Counter()
    raw_rows = valid_rows = excluded_rows = malformed_rows = 0
    with gzip.open(path, "rt", encoding="utf-8") as stream:
        for line in stream:
            raw_rows += 1
            try:
                record = json.loads(line)
            except json.JSONDecodeError:
                malformed_rows += 1
                continue
            code = None
            if isinstance(record, dict):
                source = record.get("source")
                if isinstance(source, dict):
                    code = source.get("code")
                if not isinstance(code, str) or not code:
                    code = record.get("_id")
                if not isinstance(code, str) or not code:
                    code = record.get("code")
            if not isinstance(code, str) or not code:
                excluded_rows += 1
                continue
            valid_rows += 1
            counts[code] += 1
    return set(counts), raw_rows, valid_rows, excluded_rows, malformed_rows


def _code_digest(codes: Iterable[str]) -> str:
    ordered = sorted(codes)
    return hashlib.sha256(("\n".join(ordered) + "\n").encode("utf-8")).hexdigest()


def build_delta(baseline_path: str | Path, fresh_path: str | Path) -> dict[str, Any]:
    baseline, baseline_rows, baseline_valid, baseline_excluded, baseline_malformed = _codes(baseline_path)
    fresh, fresh_rows, fresh_valid, fresh_excluded, fresh_malformed = _codes(fresh_path)
    new_codes = sorted(fresh - baseline)
    removed_codes = sorted(baseline - fresh)
    common_codes = sorted(baseline & fresh)
    payload = {
        "schema": SCHEMA,
        "version": 1,
        "comparison": "source.code only; valid non-empty string codes; duplicate rows collapsed to unique code universe",
        "baseline": {
            "path": str(baseline_path),
            "rawRowCount": baseline_rows,
            "validCodeRowCount": baseline_valid,
            "excludedRowCount": baseline_excluded,
            "malformedRowCount": baseline_malformed,
            "uniqueCodeCount": len(baseline),
            "codeUniverseDigest": _code_digest(baseline),
        },
        "fresh": {
            "path": str(fresh_path),
            "rawRowCount": fresh_rows,
            "validCodeRowCount": fresh_valid,
            "excludedRowCount": fresh_excluded,
            "malformedRowCount": fresh_malformed,
            "uniqueCodeCount": len(fresh),
            "codeUniverseDigest": _code_digest(fresh),
        },
        "delta": {
            "newCodeCount": len(new_codes),
            "removedCodeCount": len(removed_codes),
            "commonCodeCount": len(common_codes),
            "newCodeDigest": _code_digest(new_codes),
            "removedCodeDigest": _code_digest(removed_codes),
            "commonCodeDigest": _code_digest(common_codes),
            "newCodes": new_codes,
        },
        "authority": {
            "source": "OPEN_FOOD_FACTS",
            "comparisonKey": "source.code",
            "internalRecordIdentityPreserved": True,
            "excludedInvalidOrMissingCodes": True,
            "familyAndGroundTruthMaterialized": False,
            "codeDigestAlgorithm": CODE_DIGEST_ALGORITHM,
        },
    }
    digest = logical_digest(payload)
    return {
        "reference": f"him-fresh-off-code-delta:v2:{digest}",
        "logicalDigest": digest,
        "payload": payload,
    }


def validate_delta(value: Mapping[str, Any]) -> dict[str, Any]:
    payload = value.get("payload")
    if not isinstance(payload, Mapping) or logical_digest(payload) != value.get("logicalDigest"):
        raise ValueError("OFF_CODE_DELTA_DIGEST_INVALID")
    if value.get("reference") != f"him-fresh-off-code-delta:v2:{value['logicalDigest']}":
        raise ValueError("OFF_CODE_DELTA_REFERENCE_INVALID")
    if payload.get("schema") != SCHEMA:
        raise ValueError("OFF_CODE_DELTA_SCHEMA_INVALID")
    authority = payload.get("authority", {})
    if authority.get("internalRecordIdentityPreserved") is not True:
        raise ValueError("INTERNAL_RECORD_IDENTITY_MUST_REMAIN_PRESERVED")
    if authority.get("familyAndGroundTruthMaterialized") is not False:
        raise ValueError("FAMILY_AND_GROUND_TRUTH_MUST_REMAIN_EMPTY")
    return dict(value)


def persist_and_reload(baseline_path: str | Path, fresh_path: str | Path, destination: str | Path) -> dict[str, Any]:
    value = build_delta(baseline_path, fresh_path)
    path = Path(destination)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(_canonical(value) + b"\n")
    return validate_delta(json.loads(path.read_text(encoding="utf-8")))
