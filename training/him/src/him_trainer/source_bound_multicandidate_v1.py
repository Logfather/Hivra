"""Source-bound bounded candidate retrieval and Development exclusion authority."""
from __future__ import annotations

import hashlib
import json
import sqlite3
from dataclasses import dataclass
from typing import Any, Iterable, Mapping


class AuthorityError(ValueError):
    pass


@dataclass(frozen=True)
class RetrievalConfig:
    max_query_results: int = 10
    max_distinct_candidates_per_base: int = 3

    def __post_init__(self) -> None:
        if not (1 <= self.max_query_results <= 100):
            raise AuthorityError("UNBOUNDED_QUERY_REQUEST")
        if not (1 <= self.max_distinct_candidates_per_base <= self.max_query_results):
            raise AuthorityError("INVALID_CANDIDATE_BOUND")


def _digest(value: Any) -> str:
    raw = json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode()
    return hashlib.sha256(raw).hexdigest()


def resolve_candidates_for_base(base: Mapping[str, Any], *, index_path: str,
                                source_authority: Mapping[str, Any],
                                index_authority: Mapping[str, Any],
                                config: RetrievalConfig = RetrievalConfig()) -> list[dict[str, Any]]:
    if source_authority.get("sourceSha256") != index_authority.get("sourceSha256"):
        raise AuthorityError("SOURCE_INDEX_BINDING_MISMATCH")
    if not base.get("baseId") or not base.get("term"):
        raise AuthorityError("BASE_IDENTITY_INVALID")
    query = str(base["term"]).strip().replace('"', '""')
    con = sqlite3.connect(index_path)
    con.row_factory = sqlite3.Row
    try:
        rows = con.execute("""
            SELECT r.source_record_reference, r.source_native_identifiers_json,
                   r.evidence_projection_json, s.rank
            FROM (SELECT rowid, rank FROM evidence_search WHERE evidence_search MATCH ?
                  ORDER BY rank, rowid LIMIT ?) s
            JOIN evidence_records r ON r.internal_record_key=s.rowid
            ORDER BY s.rank, r.source_record_reference
        """, (f'"{query}"', config.max_query_results)).fetchall()
    finally:
        con.close()
    result: list[dict[str, Any]] = []
    seen: set[str] = set()
    for row in rows:
        native = json.loads(row["source_native_identifiers_json"])
        candidate_id = str(native.get("code") or native.get("id") or row["source_record_reference"])
        if candidate_id in seen:
            continue
        seen.add(candidate_id)
        result.append({"baseId": base["baseId"], "baseTerm": base["term"],
                       "baseNormalizedTerm": base.get("normalizedTerm", str(base["term"]).casefold()),
                       "candidateId": candidate_id,
                       "candidateTerm": json.loads(row["evidence_projection_json"]).get("primaryName", ""),
                       "sourceRecordReference": row["source_record_reference"],
                       "sourceNativeIdentifier": native, "sourceAuthority": dict(source_authority),
                       "indexAuthority": dict(index_authority), "retrievalQuery": base["term"],
                       "retrievalRank": row["rank"], "groundTruthState": "UNRESOLVED"})
        if len(result) >= config.max_distinct_candidates_per_base:
            break
    return result


def materialize_with_exclusion(record: Mapping[str, Any], *, excluded: set[str],
                               materialization_id: str) -> tuple[dict[str, Any], dict[str, Any]]:
    identity = str(record.get("sourceRecordReference", ""))
    if not identity or identity in excluded:
        raise AuthorityError("EXCLUDED_IDENTITY_SELECTION")
    exclusion = {"schema": "HIM_DEVELOPMENT_FINAL_HOLDOUT_EXCLUSION_V1",
                 "recordIdentity": identity, "sourceAuthority": record["sourceAuthority"],
                 "sourceSha256": record["sourceAuthority"]["sourceSha256"],
                 "indexAuthority": record["indexAuthority"],
                 "reason": "DEVELOPMENT_MATERIALIZATION", "materializationAuthority": materialization_id,
                 "finalHoldoutEligible": False}
    exclusion["logicalDigest"] = _digest(exclusion)
    materialized = {**dict(record), "developmentOnly": True, "finalHoldoutEligible": False,
                    "materializationAuthority": materialization_id}
    return materialized, exclusion


def is_final_holdout_eligible(identity: str, exclusions: Iterable[Mapping[str, Any]]) -> bool:
    return not any(e.get("recordIdentity") == identity and e.get("finalHoldoutEligible") is False for e in exclusions)
