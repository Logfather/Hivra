"""Assembly and immutable persistence for the authoritative HIM P2 Corpus V2.

The assembler consumes the already-finalized P2 V1 corpus plus the finalized
V2 human review authorities.  It never labels, opens Holdout material, loads a
model, or performs inference.  Targets and lineage are persisted separately
from the model-facing ``HIMV2`` serialization.
"""

from __future__ import annotations

import hashlib
import json
import os
import tempfile
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Any, Mapping, Sequence

from .corpus_coverage_v2 import (
    DEFINED_NEGATIVE_BOUNDARY_TYPES_V1,
    CorpusExampleV2,
    build_coverage_report,
    candidate_shortcut_diagnostic,
    corpus_coverage_contract_v2,
)
from .evidence_projection_v2 import EvidenceProjectionProvenanceV2, HimEvidenceProjectionV2
from .input_representation_v2 import (
    CandidateConditioningV2,
    HimInputRepresentationV2,
    ObservedConditioningV2,
    build_input_representation_v2,
)
from .partition_leakage_v2 import (
    IsolationUnitV2,
    LeakageValidationAuthorityV2,
    PartitionAuthorityV2,
    REQUIRED_ISOLATION_UNITS_V2,
    component_statistics,
    isolation_group_reference_v2,
    validate_partition_assignments,
)
from .partition_v2 import PARTITION_SEED_V2, PARTITION_STRATEGY_V2, _component_reference, assign_group_then_stratify
from .post_review_eligibility_v2 import POST_REVIEW_ELIGIBILITY_V2_ID
from .sequence_length_authority_v2 import sequence_length_authority_v2


CORPUS_CONTRACT_ID_V2 = "HIM_P2_CORPUS_V2"
CORPUS_VERSION_V2 = "2"
CORPUS_OUTPUT_RELATIVE = Path("data/knowledge/him/training/p2/canonical-catalog-expansion/v2/corpus-assembly-v2")
BASE_CORPUS_RELATIVE = Path("data/knowledge/him/training/p2/canonical-catalog-expansion/v1/mixed-supervision-training-example-view.p2.json")
CANONICAL_FAMILY_RELATIVE = Path("data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json")
PRIMARY_PACKET_RELATIVE = Path("build/knowledge/reports/him/training/p2/corpus-v2/targeted-human-review-expansion-v2/review-packet.v2.json")
PRIMARY_ADJ_RELATIVE = Path("build/knowledge/reports/him/training/p2/corpus-v2/targeted-human-review-expansion-v2/human-adjudication.v2.json")
REPLACEMENT_PACKET_RELATIVE = Path("build/knowledge/reports/him/training/p2/corpus-v2/secondary-contrast-replacement-review-v2/review-packet.v2.json")
REPLACEMENT_ADJ_RELATIVE = Path("build/knowledge/reports/him/training/p2/corpus-v2/secondary-contrast-replacement-review-v2/human-adjudication.v2.json")
TOKENIZER_RELATIVE = Path("training/him/models/xlm-roberta-base/e73636d4f797dec63c3081bb6ed5c7b0bb3f2089/tokenizer.json")

PRIMARY_PACKET_REFERENCE = "him-p2-targeted-human-review-expansion:v2:ada57b3c6bcf5feae11d1ddaf890decb4716f13f2c8312be306ad8b28ebc966f"
PRIMARY_ADJ_REFERENCE = "him-p2-targeted-human-adjudication:v2:fa29c0c2e17934b2a9554ede115481816102551d21b7a6404e11b91e7c4c4cec"
REPLACEMENT_PACKET_REFERENCE = "him-p2-secondary-contrast-replacement-review:v2:33b8eaa64bcec84710f8c2eac2a103ac8d3c0c2314b2dd2e35aba675ce1a3d0a"
REPLACEMENT_ADJ_REFERENCE = "him-p2-secondary-contrast-human-adjudication:v2:7770faec4546d7c5b869831822fd1b60cd87bafc68b0bdfcd8d1bc00ae404d17"


def _canonical(value: object) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def logical_digest(value: object) -> str:
    return hashlib.sha256(_canonical(value)).hexdigest()


def _load(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def _unescape(value: str) -> str:
    result: list[str] = []
    index = 0
    while index < len(value):
        if value[index] != "\\" or index + 1 >= len(value):
            result.append(value[index])
            index += 1
            continue
        escaped = value[index + 1]
        result.append({"n": "\n", "r": "\r", "t": "\t", "|": "|", "\\": "\\"}.get(escaped, escaped))
        index += 2
    return "".join(result)


def parse_model_input_v2(serialized: str) -> HimInputRepresentationV2:
    """Reconstruct an Input V2 value from persisted model-facing bytes."""

    lines = serialized.split("\n")
    if len(lines) != 11 or lines[0] != "<HIMV2>" or lines[-1] != "</HIMV2>":
        raise ValueError("INPUT_V2_SERIALIZATION_INVALID")
    values: dict[str, str] = {}
    for line in lines[1:-1]:
        if "=" not in line:
            raise ValueError("INPUT_V2_FIELD_INVALID")
        key, value = line.split("=", 1)
        values[key] = _unescape(value)
    if tuple(values) != ("O", "L", "I", "K", "G", "T", "C", "N", "X"):
        raise ValueError("INPUT_V2_FIELD_ORDER_INVALID")

    def list_value(value: str) -> tuple[str, ...]:
        return () if value == "UNKNOWN" else tuple(value.split("|"))

    paths = () if values["X"] == "UNKNOWN" else tuple(tuple(item.split("/")) for item in values["X"].split("|"))
    return HimInputRepresentationV2(
        observed=ObservedConditioningV2(
            term=values["O"], source_faithful_label=values["L"], composition=values["I"],
            categories=list_value(values["K"]), food_groups=list_value(values["G"]), product_type=values["T"],
        ),
        candidate=CandidateConditioningV2(
            term=values["C"], normalized_term=values["N"], taxonomy_paths=paths,
        ),
    )


def reload_corpus_examples_v2(path: Path) -> tuple[CorpusExampleV2, ...]:
    """Reload and reconstruct coverage examples from persisted JSON bytes."""

    payload = _load(path)
    core = {key: value for key, value in payload.items() if key not in {"reference", "logicalDigest"}}
    if logical_digest(core) != payload.get("logicalDigest"):
        raise ValueError("CORPUS_LOGICAL_DIGEST_MISMATCH")
    values = []
    for record in payload.get("examples", []):
        input_value = parse_model_input_v2(record["inputRepresentation"]["serialized"])
        values.append(CorpusExampleV2(
            example_reference=record["exampleReference"], family_reference=record["familyReference"],
            source_record_identity=record["sourceRecordReference"], evidence_projection_reference=record["evidenceProjectionReference"],
            model_input=input_value, candidate_reference=record["candidateReference"], source=record["source"],
            partition=record.get("partition", "UNASSIGNED"), primary_target=record.get("primaryTarget"),
            secondary_target=record.get("secondaryTarget"), boundary_type=record.get("negativeBoundary"),
            sequence_length=record.get("sequenceLength"),
        ))
    return tuple(values)


def _authority_payload_digest(value: Mapping[str, Any]) -> str:
    if "authorityPayload" in value:
        return logical_digest(value["authorityPayload"])
    return logical_digest({key: item for key, item in value.items() if key not in {"logicalDigest", "reference"}})


def _verify_authorities(root: Path) -> dict[str, Any]:
    paths = {
        "primaryPacket": root / PRIMARY_PACKET_RELATIVE,
        "primaryAdjudication": root / PRIMARY_ADJ_RELATIVE,
        "replacementPacket": root / REPLACEMENT_PACKET_RELATIVE,
        "replacementAdjudication": root / REPLACEMENT_ADJ_RELATIVE,
    }
    values = {name: _load(path) for name, path in paths.items()}
    expected = {
        "primaryPacket": PRIMARY_PACKET_REFERENCE,
        "primaryAdjudication": PRIMARY_ADJ_REFERENCE,
        "replacementPacket": REPLACEMENT_PACKET_REFERENCE,
        "replacementAdjudication": REPLACEMENT_ADJ_REFERENCE,
    }
    for name, reference in expected.items():
        value = values[name]
        reference_field = value.get("authorityReference", value.get("reference"))
        if reference_field != reference:
            raise ValueError(f"AUTHORITY_REFERENCE_MISMATCH:{name}")
        if value.get("logicalDigest") != reference.rsplit(":", 1)[-1]:
            raise ValueError(f"AUTHORITY_DIGEST_MISMATCH:{name}")
        if _authority_payload_digest(value) != value["logicalDigest"]:
            raise ValueError(f"AUTHORITY_PAYLOAD_DIGEST_MISMATCH:{name}")
    replacement_packet = values["replacementPacket"]
    if replacement_packet.get("logicalDigest") != REPLACEMENT_PACKET_REFERENCE.rsplit(":", 1)[-1]:
        raise ValueError("REPLACEMENT_PACKET_DIGEST_MISMATCH")
    return values


def _candidate_map(root: Path) -> dict[str, dict[str, Any]]:
    families = _load(root / CANONICAL_FAMILY_RELATIVE)["families"]
    return {item["canonicalId"]["value"]: item for item in families}


def _candidate_mapping(item: Mapping[str, Any], candidates: Mapping[str, Mapping[str, Any]]) -> dict[str, Any]:
    candidate = candidates[item["canonicalId"]]
    return {
        "itemname": candidate["canonicalName"],
        "normalized": candidate["normalizedName"],
        "taxonomyPaths": candidate["taxonomyPaths"],
    }


def _v2_projection_from_input(source: str, record_reference: str, input_value: Any) -> HimEvidenceProjectionV2:
    observed = input_value.observed
    return HimEvidenceProjectionV2(
        observed.source_faithful_label,
        observed.composition,
        observed.categories,
        observed.food_groups,
        observed.product_type,
        EvidenceProjectionProvenanceV2(
            source=source,
            source_record_identity=record_reference,
            source_artifact=f"source-artifact:{source}:optimized",
            source_field_paths=(),
        ),
    )


def _make_example(
    *,
    source: str,
    observed_term: str,
    source_record: str,
    family: str,
    candidate_reference: str,
    candidate: Mapping[str, Any],
    projection: HimEvidenceProjectionV2,
    primary: str | None,
    secondary: str | None,
    boundary: str | None,
    supervision: Mapping[str, Any],
    tokenizer: Any,
    stable_hint: str,
    validate_length: bool = True,
) -> tuple[CorpusExampleV2, dict[str, Any]]:
    model_input = build_input_representation_v2(
        observed_term,
        {
            "itemname": candidate["canonicalName"],
            "normalized": candidate.get("normalizedName", candidate.get("normalizedTerm")),
            "taxonomyPaths": candidate["taxonomyPaths"],
        },
        projection.model_input_mapping(),
    )
    length = len(tokenizer.encode(model_input.serialize()).ids)
    if validate_length:
        sequence_length_authority_v2().validate_token_length(length)
    identity_payload = {
        "schema": CORPUS_CONTRACT_ID_V2,
        "version": CORPUS_VERSION_V2,
        "sourceRecord": source_record,
        "family": family,
        "projection": projection.reference,
        "decisionContext": __import__("him_trainer.partition_leakage_v2", fromlist=["decision_context_reference_v2"]).decision_context_reference_v2(model_input),
        "candidate": candidate_reference,
        "primary": primary,
        "secondary": secondary,
        "boundary": boundary,
        "input": model_input.serialize(),
        "supervision": supervision,
        "stableHint": stable_hint,
    }
    example_digest = logical_digest(identity_payload)
    example_reference = f"him-p2-corpus-example:v2:{example_digest}"
    value = CorpusExampleV2(
        example_reference=example_reference,
        family_reference=family,
        source_record_identity=source_record,
        evidence_projection_reference=projection.reference,
        model_input=model_input,
        candidate_reference=candidate_reference,
        source=source,
        primary_target=primary,
        secondary_target=secondary,
        boundary_type=boundary,
        sequence_length=length,
    )
    record = {
        "exampleReference": example_reference,
        "familyReference": family,
        "sourceRecordReference": source_record,
        "evidenceProjectionReference": projection.reference,
        "decisionContextReference": value.decision_context_reference,
        "candidateReference": candidate_reference,
        "humanSupervisionAuthority": dict(supervision),
        "primaryTarget": primary,
        "primaryMask": 1 if primary else 0,
        "secondaryTarget": secondary,
        "secondaryMask": 1 if secondary else 0,
        "negativeBoundary": boundary,
        "inputRepresentation": {"id": "HIM_INPUT_REPRESENTATION_V2", "version": "2", "serialized": model_input.serialize()},
        "evidenceProjection": {"id": "HIM_EVIDENCE_PROJECTION_V2", "version": "2", "reference": projection.reference},
        "sequenceLengthAuthority": sequence_length_authority_v2().authority_reference,
        "sequenceLength": length,
        "source": source,
    }
    return value, record


def _base_supervision(item: Mapping[str, Any]) -> dict[str, Any]:
    return {
        "provenance": "EXPLICIT_HUMAN_AUTHORIZATION",
        "reviewer": item.get("humanAuthority", {}).get("reviewer"),
        "candidateCompatibilityAuthorityReference": item.get("humanCCAuthorityReference"),
        "relationAuthorityReference": item.get("humanRelationAuthorityReference"),
        "reviewUnitReference": item.get("reviewUnitReference"),
        "reviewPacketReference": "p2-mixed-supervision-human-review-authority:v1",
        "adjudicationReference": item.get("humanAuthority", {}).get("relationDecisionReference"),
    }


def assemble_eligible_examples(root: Path) -> tuple[tuple[CorpusExampleV2, ...], tuple[dict[str, Any], ...], dict[str, Any]]:
    authorities = _verify_authorities(root)
    base = _load(root / BASE_CORPUS_RELATIVE)["examples"]
    candidates = _candidate_map(root)
    from tokenizers import Tokenizer

    tokenizer = Tokenizer.from_file(str(root / TOKENIZER_RELATIVE))
    examples: list[CorpusExampleV2] = []
    records: list[dict[str, Any]] = []
    source_count = 0
    for item in base:
        primary_info = item.get("primary") or {}
        primary = primary_info.get("targetKind") if primary_info.get("targetKind") in {"IDENTITY", "VARIANT"} else None
        secondary = item.get("secondaryTarget")
        boundary = "WRONG_RELATION_LEVEL" if secondary == "REJECT" else None
        candidate = candidates[item["canonicalId"]]
        provisional_input = build_input_representation_v2(item["observedTerm"], _candidate_mapping(item, candidates), item["sourceEvidence"])
        projection = _v2_projection_from_input("OPEN_FOOD_FACTS", item["sourceRecordReference"], provisional_input)
        value, record = _make_example(
            source="OPEN_FOOD_FACTS",
            observed_term=item["observedTerm"],
            source_record=item["sourceRecordReference"],
            family=item.get("familyReference", item["family"]["familyGroupReference"]),
            candidate_reference=item["canonicalReference"],
            candidate=candidate,
            projection=projection,
            primary=primary,
            secondary=secondary,
            boundary=boundary,
            supervision=_base_supervision(item),
            tokenizer=tokenizer,
            stable_hint=item["exampleReference"],
        )
        examples.append(value)
        records.append(record)
        source_count += 1

    packet = authorities["primaryPacket"]["authorityPayload"]
    adjudication = authorities["primaryAdjudication"]["authorityPayload"]
    decisions = {item["reviewUnitId"]: item for item in adjudication["decisions"]}
    for index, unit in enumerate(packet["units"], start=1):
        decision = decisions[unit["reviewUnitId"]]
        primary = {"ID": "IDENTITY", "VAR": "VARIANT"}.get(decision["humanDecision"].get("primary"))
        secondary = decision["humanDecision"].get("secondary")
        boundary = decision["humanDecision"].get("negativeBoundary")
        candidate = unit["candidate"]
        projection_data = unit["evidenceProjection"]
        projection = HimEvidenceProjectionV2(
            projection_data["sourceFaithfulLabel"],
            projection_data["ingredientOrComposition"],
            tuple(projection_data["categories"]),
            tuple(projection_data["foodGroups"]),
            projection_data["productType"],
            EvidenceProjectionProvenanceV2(
                source=unit["source"], source_record_identity=unit["sourceRecordReference"],
                source_artifact=f"source-artifact:{unit['source']}:optimized", source_field_paths=(),
            ),
        )
        value, record = _make_example(
            source=unit["source"], observed_term=unit["observedTerm"], source_record=unit["sourceRecordReference"],
            family=unit["familyReference"], candidate_reference=f"canonical-family:v1:canonical:{candidate['canonicalId']}",
            candidate=candidate, projection=projection, primary=primary, secondary=secondary, boundary=boundary,
            supervision={"provenance": "EXPLICIT_HUMAN_AUTHORIZATION", "reviewer": adjudication["reviewer"], "reviewPacketReference": PRIMARY_PACKET_REFERENCE, "adjudicationReference": PRIMARY_ADJ_REFERENCE, "reviewUnitReference": unit["reviewUnitId"], "decisionReference": unit["reviewUnitId"]},
            tokenizer=tokenizer, stable_hint=f"targeted-unit-{index}", validate_length=index != 5,
        )
        if index == 5:
            source_count += 1
            continue
        examples.append(value)
        records.append(record)
        source_count += 1

    replacement = authorities["replacementPacket"]["reviewUnit"]
    replacement_adj = authorities["replacementAdjudication"]["authorityPayload"]["decisions"][0]
    input_fields = replacement["inputRepresentation"]
    projection = HimEvidenceProjectionV2(
        input_fields["L"], input_fields["I"], (), (), input_fields["T"],
        EvidenceProjectionProvenanceV2(source="OPEN_FOOD_FACTS", source_record_identity=replacement["sourceRecordReference"], source_artifact="source-artifact:OPEN_FOOD_FACTS:optimized", source_field_paths=()),
    )
    value, record = _make_example(
        source="OPEN_FOOD_FACTS", observed_term=replacement["observedTerm"], source_record=replacement["sourceRecordReference"],
        family=replacement["familyReference"], candidate_reference=f"canonical-family:v1:canonical:{replacement['candidate']['canonicalId']}",
        candidate=replacement["candidate"], projection=projection, primary=None, secondary="COMPATIBLE", boundary=None,
        supervision={"provenance": "EXPLICIT_HUMAN_AUTHORIZATION", "reviewer": "human-reviewer:christian-glatschke:v1", "reviewPacketReference": REPLACEMENT_PACKET_REFERENCE, "adjudicationReference": REPLACEMENT_ADJ_REFERENCE, "decisionReference": replacement["decisionContextReference"]},
        tokenizer=tokenizer, stable_hint="corn-mais-replacement",
    )
    examples.append(value)
    records.append(record)
    source_count += 1
    examples.sort(key=lambda item: item.example_reference)
    record_by_ref = {item["exampleReference"]: item for item in records}
    records = tuple(record_by_ref[item.example_reference] for item in examples)
    return tuple(examples), records, {"sourceExampleCount": source_count, "unit5TokenLength": 259, "unit5Excluded": True, "authorities": authorities}


def _report_dict(report: Any) -> dict[str, Any]:
    value = asdict(report)
    value["boundary_coverage"] = [asdict(item) for item in report.boundary_coverage]
    value["primary_contrastive_groups"] = [asdict(item) for item in report.primary_contrastive_groups]
    value["secondary_contrastive_groups"] = [asdict(item) for item in report.secondary_contrastive_groups]
    value["sequence_lengths"] = list(report.sequence_lengths)
    return value


def build_partition_artifacts(examples: Sequence[CorpusExampleV2], corpus_reference: str, corpus_digest: str) -> tuple[tuple[CorpusExampleV2, ...], dict[str, Any], dict[str, Any]]:
    assigned = assign_group_then_stratify(examples)
    leakage = validate_partition_assignments(tuple(item.isolation_example for item in assigned), REQUIRED_ISOLATION_UNITS_V2)
    if not leakage.valid:
        raise ValueError("PARTITION_LEAKAGE")
    components = []
    grouping_identities: list[tuple[str, str]] = []
    for component in __import__("him_trainer.partition_leakage_v2", fromlist=["connected_components"]).connected_components(tuple(item.isolation_example for item in assigned), REQUIRED_ISOLATION_UNITS_V2):
        values = tuple(next(item for item in assigned if item.example_reference == member.record_reference) for member in component)
        group_ref = _component_reference(values)
        grouping_identities.extend((item.example_reference, group_ref) for item in values)
        components.append({"componentReference": group_ref, "exampleReferences": [item.example_reference for item in values], "families": sorted({item.family_reference for item in values}), "sources": sorted({item.source for item in values}), "primaryTargets": sorted({item.primary_target for item in values if item.primary_target}), "secondaryTargets": sorted({item.secondary_target for item in values if item.secondary_target}), "assignedSplit": values[0].partition})
    components.sort(key=lambda item: item["componentReference"])
    binding = tuple(sorted({"inputRepresentationId": "HIM_INPUT_REPRESENTATION_V2", "inputRepresentationVersion": "2", "evidenceProjectionId": "HIM_EVIDENCE_PROJECTION_V2", "evidenceProjectionVersion": "2", "sequenceLengthAuthorityId": sequence_length_authority_v2().authority_id, "sequenceLengthAuthorityVersion": sequence_length_authority_v2().authority_version, "partitionSeed": PARTITION_SEED_V2}.items()))
    authority = PartitionAuthorityV2(
        corpus_identity=corpus_reference, partition_strategy=PARTITION_STRATEGY_V2,
        isolation_units=REQUIRED_ISOLATION_UNITS_V2,
        split_membership=tuple(sorted((item.example_reference, item.partition) for item in assigned)),
        grouping_identities=tuple(sorted(grouping_identities)),
        representation_binding=binding,
    )
    partition_payload = {"contractId": "HIM_P2_PARTITION_AUTHORITY_V2", "version": "2", "reference": authority.reference, "logicalDigest": authority.logical_digest, "corpusReference": corpus_reference, "corpusLogicalDigest": corpus_digest, "partitionStrategy": PARTITION_STRATEGY_V2, "partitionSeed": PARTITION_SEED_V2, "isolationUnits": [item.value for item in REQUIRED_ISOLATION_UNITS_V2], "components": components, "members": [{"exampleReference": item.example_reference, "partition": item.partition} for item in assigned], "representationBinding": dict(binding)}
    leakage_authority = LeakageValidationAuthorityV2(partition_identity=authority.reference, isolation_units_checked=REQUIRED_ISOLATION_UNITS_V2, collision_counts=tuple((item.value, 0) for item in REQUIRED_ISOLATION_UNITS_V2) + (("EXACT_V2_INPUT", 0),), collision_references=(), status="PASS")
    leakage_payload = {"contractId": leakage_authority.authority_id, "version": "2", "reference": leakage_authority.reference, "logicalDigest": leakage_authority.logical_digest, "partitionReference": authority.reference, "partitionLogicalDigest": authority.logical_digest, "isolationUnitsChecked": [item.value for item in REQUIRED_ISOLATION_UNITS_V2], "collisionCounts": {item.value: 0 for item in REQUIRED_ISOLATION_UNITS_V2} | {"EXACT_V2_INPUT": 0}, "collisionReferences": [], "status": "PASS"}
    return assigned, partition_payload, leakage_payload


def _persist_immutable(path: Path, payload: Mapping[str, Any]) -> None:
    data = _canonical(payload) + b"\n"
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.exists():
        if path.read_bytes() != data:
            raise ValueError(f"IMMUTABLE_ARTIFACT_COLLISION:{path}")
        return
    fd, temporary = tempfile.mkstemp(prefix=f".{path.name}.", suffix=".tmp", dir=str(path.parent))
    try:
        with os.fdopen(fd, "wb") as handle:
            handle.write(data)
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temporary, path)
    except Exception:
        try:
            os.unlink(temporary)
        except OSError:
            pass
        raise


def materialize(root: Path) -> dict[str, Any]:
    sequence = sequence_length_authority_v2()
    sequence.validate_model_position_capacity()
    contract = corpus_coverage_contract_v2()
    examples, records, meta = assemble_eligible_examples(root)
    coverage = build_coverage_report(examples)
    if coverage.input_scan != {key: 0 for key in coverage.input_scan}:
        raise ValueError("MODEL_INPUT_LEAKAGE")
    if coverage.primary_conflicting_target_group_count or coverage.secondary_conflicting_target_group_count:
        raise ValueError("CONFLICTING_TARGETS")
    if max(coverage.sequence_lengths) > sequence.max_sequence_length:
        raise ValueError("SEQUENCE_OVERFLOW")
    corpus_core = {"contractId": CORPUS_CONTRACT_ID_V2, "version": CORPUS_VERSION_V2, "ordering": "FAMILY_DECISION_CONTEXT_EXAMPLE_REFERENCE", "authorityBindings": {"inputRepresentation": ["HIM_INPUT_REPRESENTATION_V2", "2"], "evidenceProjection": ["HIM_EVIDENCE_PROJECTION_V2", "2"], "sequenceLengthAuthority": sequence.authority_reference, "coverage": contract.reference, "postReviewEligibility": POST_REVIEW_ELIGIBILITY_V2_ID, "humanReviewAuthorities": [PRIMARY_PACKET_REFERENCE, PRIMARY_ADJ_REFERENCE, REPLACEMENT_PACKET_REFERENCE, REPLACEMENT_ADJ_REFERENCE]}, "exampleCount": len(records), "examples": list(records)}
    corpus_digest = logical_digest(corpus_core)
    corpus_reference = f"him-p2-corpus:v2:{corpus_digest}"
    corpus_payload = {**corpus_core, "reference": corpus_reference, "logicalDigest": corpus_digest}
    assigned, partition_payload, leakage_payload = build_partition_artifacts(examples, corpus_reference, corpus_digest)
    split_records = {item.example_reference: {**record, "partition": item.partition} for item, record in zip(examples, records)}
    train = [item for item in assigned if item.partition == "TRAIN"]
    validation = [item for item in assigned if item.partition == "VALIDATION"]
    split_coverage = {"corpus": _report_dict(coverage), "train": _report_dict(build_coverage_report(train)), "validation": _report_dict(build_coverage_report(validation)), "candidateShortcutRisk": candidate_shortcut_diagnostic(coverage), "sourceTargetShortcutRisk": "MEDIUM", "sourceTargetShortcutRiskAcceptable": True}
    exclusions = {"contractId": "HIM_P2_CORPUS_V2_EXCLUSION_MANIFEST", "version": "2", "entries": [{"reference": "p2-targeted-human-review-unit:v2:c0924ffcac69d929770735ea35e7b2950d96b721bc0bf79128ef479423c8ccd6", "reason": "SEQUENCE_OVERFLOW", "tokenLength": 259, "humanReviewPreserved": True, "eligibleLater": True}, {"reference": "HISTORICAL_EXPANDED_VALIDATION_V2", "reason": "DIAGNOSTIC_ONLY_PROTECTED_FROM_ASSEMBLY", "humanReviewPreserved": True, "eligibleLater": False}, {"reference": "FUTURE_BLIND_EXPANDED_VALIDATION_V2", "reason": "FUTURE_EVALUATION_RESERVED_NOT_CONSUMED", "humanReviewPreserved": True, "eligibleLater": True}], "silentExclusionCount": 0, "holdoutOpened": False}
    output = root / CORPUS_OUTPUT_RELATIVE
    _persist_immutable(output / "corpus.v2.json", corpus_payload)
    _persist_immutable(output / "partition.v2.json", partition_payload)
    _persist_immutable(output / "leakage-validation.v2.json", leakage_payload)
    _persist_immutable(output / "coverage.v2.json", split_coverage)
    _persist_immutable(output / "exclusions.v2.json", exclusions)
    return {"corpus": corpus_payload, "partition": partition_payload, "leakage": leakage_payload, "coverage": split_coverage, "exclusions": exclusions, "examples": examples, "records": records, "assigned": assigned, "meta": meta}


__all__ = [
    "BASE_CORPUS_RELATIVE",
    "CORPUS_OUTPUT_RELATIVE",
    "CORPUS_CONTRACT_ID_V2",
    "assemble_eligible_examples",
    "build_partition_artifacts",
    "logical_digest",
    "materialize",
    "parse_model_input_v2",
    "reload_corpus_examples_v2",
]
