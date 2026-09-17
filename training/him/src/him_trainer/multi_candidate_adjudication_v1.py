"""Validator for the editable Mission 5G adjudication form."""
import json

ALLOWED_DECISIONS = {"COMPATIBLE", "REJECT", "UNRESOLVED"}
PACKET_ID = "HIM_V2_MULTI_CANDIDATE_DEVELOPMENT_HUMAN_REVIEW_PACKET_V1"

def validate_form(path, packet):
    expected = {u["reviewUnitId"]: u for u in packet["reviewUnits"]}
    seen = set(); rows = []
    with open(path, encoding="utf-8") as f:
        for line in f:
            if not line.strip(): continue
            row = json.loads(line); uid = row.get("reviewUnitId")
            if uid in seen or uid not in expected: raise ValueError("DUPLICATE_OR_UNKNOWN_REVIEW_UNIT")
            seen.add(uid)
            exp = expected[uid]
            for key in ("reviewPacketId","reviewPacketDigest","baseGroupId","candidateRelationId","baseId","candidateId"):
                if row.get(key) != (PACKET_ID if key == "reviewPacketId" else packet.get("logicalDigest") if key == "reviewPacketDigest" else exp[key]):
                    raise ValueError("PACKET_BINDING_INVALID")
            if row.get("humanDecision") not in ALLOWED_DECISIONS: raise ValueError("DECISION_INVALID")
            rows.append(row)
    if seen != set(expected): raise ValueError("REVIEW_UNIT_COUNT_INVALID")
    return rows
