"""Self-contained evaluator for the frozen six-record relational V3 set."""
from __future__ import annotations
import hashlib, json
from pathlib import Path
from typing import Any, Mapping, Sequence
from .input_representation_v3 import serialize_contextual_input_v3
from .v3_prediction_adapter import predict_v3_v1

DEVELOPMENT_DIGEST = "fb3b2589fdb7276ee6b94f64b71b4b5bfbd984a508dce8d6566603fa08fcfa00"
DEVELOPMENT_RECORD_COUNT = 6

def _canonical(v: Any) -> bytes:
    return json.dumps(v, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode()

def _load_authority(path: str | Path) -> tuple[dict[str, Any], ...]:
    value = json.loads(Path(path).read_text(encoding="utf-8"))
    body = dict(value); logical = body.pop("logicalDigest", None)
    digest = hashlib.sha256(_canonical(body)).hexdigest()
    if logical != DEVELOPMENT_DIGEST or digest != DEVELOPMENT_DIGEST:
        raise ValueError("V3_DEVELOPMENT_AUTHORITY_DIGEST_MISMATCH")
    records = tuple(value.get("records", ()))
    if len(records) != DEVELOPMENT_RECORD_COUNT or any(r.get("partition") != "DEVELOPMENT" for r in records):
        raise ValueError("V3_DEVELOPMENT_RECORD_COUNT_MISMATCH")
    return records

def _decode(value: Any, allowed: Sequence[str], field: str) -> str:
    value = str(value).upper()
    if value not in allowed: raise ValueError(f"{field}_PREDICTION_INVALID")
    return value

def _metrics(rows: list[dict[str, Any]]) -> dict[str, Any]:
    applicable = [r for r in rows if r["expectedTargetKind"] != "NOT_APPLICABLE"]
    target_correct = sum(r["targetKindCorrect"] for r in applicable)
    compat_correct = sum(r["compatibilityCorrect"] for r in rows)
    def support(kind): return sum(r["expectedTargetKind"] == kind for r in applicable)
    def correct(kind): return sum(r["expectedTargetKind"] == kind and r["targetKindCorrect"] for r in applicable)
    groups = {}
    for r in rows: groups.setdefault(r["groupKey"], []).append(r)
    full = sum(all(x["targetKindCorrect"] and x["compatibilityCorrect"] for x in rs) for rs in groups.values())
    partial = sum(any(x["targetKindCorrect"] and x["compatibilityCorrect"] for x in rs) and not all(x["targetKindCorrect"] and x["compatibilityCorrect"] for x in rs) for rs in groups.values())
    return {"developmentTargetKindApplicableCount":len(applicable),"developmentTargetKindCorrectCount":target_correct,"developmentTargetKindAccuracy":target_correct/len(applicable) if applicable else None,"identitySupport":support("IDENTITY"),"identityCorrect":correct("IDENTITY"),"variantSupport":support("VARIANT"),"variantCorrect":correct("VARIANT"),"targetKindSingleClassCollapse":len({r["predictedTargetKind"] for r in applicable}) <= 1 if applicable else False,"developmentCompatibilityCount":len(rows),"developmentCompatibilityCorrectCount":compat_correct,"developmentCompatibilityAccuracy":compat_correct/len(rows),"compatibleSupport":sum(r["expectedCompatibility"]=="COMPATIBLE" for r in rows),"compatibleCorrect":sum(r["expectedCompatibility"]=="COMPATIBLE" and r["compatibilityCorrect"] for r in rows),"rejectSupport":sum(r["expectedCompatibility"]=="REJECT" for r in rows),"rejectCorrect":sum(r["expectedCompatibility"]=="REJECT" and r["compatibilityCorrect"] for r in rows),"compatibilitySingleClassCollapse":len({r["predictedCompatibility"] for r in rows}) <= 1,"controlledSwapGroupsFullyCorrect":full,"controlledSwapGroupsPartiallyCorrect":partial,"controlledSwapGroupsFailed":len(groups)-full-partial}

def evaluate_v3_development(*, authority_path: str | Path, model: Any, checkpoint: Any, output_path: str | Path | None = None, tokenizer: Any | None = None) -> dict[str, Any]:
    """Evaluate exactly six V3 records using the supplied loaded model state.

    The model is a loaded production model and predictions use the versioned
    runtime adapter.  No evaluator callback or model protocol is required.
    """
    records = _load_authority(authority_path)
    rows = []
    for record in records:
        context = record["inputRepresentation"]["context"]
        serialized = serialize_contextual_input_v3(context)
        if tokenizer is not None:
            prediction = predict_v3_v1(serialized_input=serialized, model=model, tokenizer=tokenizer, checkpoint=checkpoint).as_dict()
        else:
            # Compatibility for bounded evaluator fixtures only.  The
            # productive path always supplies a tokenizer and uses V3 adapter.
            legacy = getattr(model, "predict_v3", None)
            if legacy is None:
                raise ValueError("V3_TOKENIZER_REQUIRED")
            prediction = legacy(serialized, checkpoint)
        expected_t = str(record.get("targetKind", "NOT_APPLICABLE"))
        expected_c = str(record["candidateCompatibility"])
        predicted_t = _decode(prediction.get("targetKind", "NOT_APPLICABLE"), ("IDENTITY", "VARIANT", "NOT_APPLICABLE"), "TARGET_KIND")
        predicted_c = _decode(prediction.get("candidateCompatibility"), ("COMPATIBLE", "REJECT"), "CANDIDATE_COMPATIBILITY")
        rows.append({"recordId":record["recordId"],"observedTerm":record["observedTerm"],"candidateTerm":record["candidateTerm"],"groupKey":record.get("groupKey"),"serializedInput":serialized,"expectedTargetKind":expected_t,"predictedTargetKind":predicted_t,"targetKindCorrect":expected_t == predicted_t,"expectedCompatibility":expected_c,"predictedCompatibility":predicted_c,"compatibilityCorrect":expected_c == predicted_c})
    result = {"schema":"HIM_V2_V3_RELATIONAL_DEVELOPMENT_RESULT_V1","recordCount":len(rows),"records":rows,"metrics":_metrics(rows),"holdoutAccessCount":0}
    if output_path is not None:
        path=Path(output_path); path.parent.mkdir(parents=True, exist_ok=True); path.write_bytes(_canonical(result)+b"\n")
        reloaded=json.loads(path.read_text(encoding="utf-8"))
        if _canonical(reloaded) != _canonical(result): raise ValueError("V3_DEVELOPMENT_RESULT_RELOAD_MISMATCH")
        result["persistedPath"]=str(path)
    return result

__all__=["evaluate_v3_development","DEVELOPMENT_DIGEST","DEVELOPMENT_RECORD_COUNT"]
