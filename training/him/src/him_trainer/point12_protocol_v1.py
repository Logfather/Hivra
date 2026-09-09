"""Strict, framework-neutral decoder for the Point-12 numerical payload."""

from __future__ import annotations

import hashlib
import json
import re
from dataclasses import dataclass
from typing import Any, Mapping


CONTRACT_ID = "HIM_P1_POINT_12_NUMERICAL_TRAINER_PAYLOAD_V1"
VERSION = "1"
STATE = "NUMERICAL_TRAINER_PAYLOAD_DEFINED"
FORMAT_ID = "him-p1-point12-numerical-trainer-payload:v1"
_DIGEST = re.compile(r"[0-9a-f]{64}\Z")
_EXAMPLE = re.compile(r"(?:example|negative-example):v1:[0-9a-f]{64}\Z")


class Point12ProtocolError(ValueError):
    """Raised for every schema, identity, policy, or semantic mismatch."""


@dataclass(frozen=True)
class Point12Sequence:
    contract_id: str
    version: str
    state: str
    observed_term: str
    subject_kind: str
    subject_term: str
    sequence_reference: str
    logical_digest: str
    segments: tuple[tuple[str, str], ...]


@dataclass(frozen=True)
class Point12Member:
    membership_reference: str
    example_reference: str
    family_group_reference: str
    sequence: Point12Sequence
    primary_target: int
    secondary_target: int
    primary_mask: float
    secondary_mask: float


@dataclass(frozen=True)
class Point12Policy:
    sequence_contract_id: str
    sequence_contract_version: str
    length_contract_id: str
    length_contract_version: str
    tensor_contract_id: str
    tensor_contract_version: str
    batch_collation_contract_id: str
    batch_collation_contract_version: str
    objective_weighting_authority_contract_id: str
    objective_weighting_authority_contract_version: str
    objective_weighting_authority_digest: str
    max_sequence_length: int
    padding_strategy: str
    padding_side: str
    pad_token_id: int
    truncation_policy: str
    attention_mask_real_token_value: int
    attention_mask_pad_value: int
    token_type_ids_required: bool
    primary_target_encoding: tuple[tuple[str, int], ...]
    secondary_target_encoding: tuple[tuple[str, int], ...]
    primary_mask_active: float
    primary_mask_inactive: float
    secondary_mask_active: float
    secondary_mask_inactive: float
    input_ids_dtype: str
    attention_mask_dtype: str
    target_dtype: str
    objective_mask_dtype: str
    shape_policy: str


@dataclass(frozen=True)
class Point12ModelTokenizerBinding:
    model_family_id: str
    base_model_id: str
    base_model_artifact_digest: str
    model_revision: str
    tokenizer_id: str
    tokenizer_artifact_digest: str
    tokenizer_revision: str
    model_configuration_artifact_digest: str
    model_binding_digest: str
    model_binding_reference: str


@dataclass(frozen=True)
class Point12Batch:
    partition_id: str
    partition_digest: str
    split: str
    batch_plan_id: str
    batch_plan_digest: str
    member_count: int
    target_length: int
    policy: Point12Policy
    model_tokenizer_binding: Point12ModelTokenizerBinding
    members: tuple[Point12Member, ...]
    logical_digest: str
    payload_reference: str


def decode_point12_numerical_payload_v1(payload: bytes | str) -> Point12Batch:
    raw = payload.encode("utf-8") if isinstance(payload, str) else bytes(payload)
    try:
        value = json.loads(raw.decode("utf-8"), parse_constant=_reject_constant)
    except (UnicodeDecodeError, json.JSONDecodeError, Point12ProtocolError) as exc:
        raise Point12ProtocolError("INVALID_JSON") from exc
    batch = _decode_root(value)
    _validate_content_digest(value, batch)
    return batch


def _decode_root(value: Any) -> Point12Batch:
    root = _object(value, "ROOT")
    _keys(root, {"contractId", "version", "state", "formatId", "payloadReference", "payloadLogicalDigest", "policy", "modelTokenizerBinding", "partition", "members", "contentSha256"}, "ROOT")
    _equal(_string(root, "contractId"), CONTRACT_ID, "CONTRACT_MISMATCH")
    _equal(_string(root, "version"), VERSION, "VERSION_MISMATCH")
    _equal(_string(root, "state"), STATE, "STATE_MISMATCH")
    _equal(_string(root, "formatId"), FORMAT_ID, "FORMAT_MISMATCH")
    _digest(_string(root, "contentSha256"), "CONTENT_DIGEST")
    policy = _decode_policy(root["policy"])
    binding = _decode_binding(root["modelTokenizerBinding"])
    partition = _object(root["partition"], "PARTITION")
    _keys(partition, {"partitionId", "partitionDigest", "split", "batchPlanId", "batchPlanDigest", "memberCount", "targetLength"}, "PARTITION")
    partition_id = _string(partition, "partitionId")
    partition_digest = _digest(_string(partition, "partitionDigest"), "PARTITION_DIGEST")
    _require(partition_id.startswith("training-partition-point10:v1:"), "PARTITION_ID_INVALID")
    split = _string(partition, "split")
    _require(split in {"TRAIN", "HOLDOUT"}, "SPLIT_INVALID")
    batch_plan_digest = _digest(_string(partition, "batchPlanDigest"), "BATCH_PLAN_DIGEST")
    batch_plan_id = _string(partition, "batchPlanId")
    _equal(batch_plan_id, f"training-batch-plan:v1:{batch_plan_digest}", "BATCH_PLAN_REFERENCE_MISMATCH")
    target_length = _integer(partition, "targetLength")
    _require(1 <= target_length <= policy.max_sequence_length, "TARGET_LENGTH_INVALID")
    members = tuple(_decode_member(item) for item in _array(root["members"], "MEMBERS"))
    member_count = _integer(partition, "memberCount")
    _require(member_count == len(members) and member_count > 0, "MEMBER_COUNT_INVALID")
    membership_refs = [item.membership_reference for item in members]
    example_refs = [item.example_reference for item in members]
    _require(len(set(membership_refs)) == len(membership_refs), "DUPLICATE_MEMBERSHIP_REFERENCE")
    _require(len(set(example_refs)) == len(example_refs), "DUPLICATE_EXAMPLE_REFERENCE")
    _require(all(item.sequence.contract_id == policy.sequence_contract_id for item in members), "SEQUENCE_CONTRACT_MISMATCH")
    logical_digest = _digest(_string(root, "payloadLogicalDigest"), "PAYLOAD_DIGEST")
    payload_reference = _string(root, "payloadReference")
    _equal(payload_reference, f"point12-numerical-payload:v1:{logical_digest}", "PAYLOAD_REFERENCE_MISMATCH")
    expected_digest = _payload_digest(partition_id, partition_digest, split, batch_plan_id, batch_plan_digest, target_length, policy, binding, members)
    _equal(logical_digest, expected_digest, "PAYLOAD_DIGEST_MISMATCH")
    return Point12Batch(partition_id, partition_digest, split, batch_plan_id, batch_plan_digest, member_count, target_length, policy, binding, members, logical_digest, payload_reference)


def _decode_policy(value: Any) -> Point12Policy:
    root = _object(value, "POLICY")
    _keys(root, {"sequence", "length", "tensor", "batchCollation", "objectiveWeightingAuthority", "objectiveWeightingAuthorityDigest", "maxSequenceLength", "paddingStrategy", "paddingSide", "padTokenId", "truncationPolicy", "attentionMaskRealTokenValue", "attentionMaskPadValue", "tokenTypeIdsRequired", "primaryTargetEncoding", "secondaryTargetEncoding", "primaryMaskActive", "primaryMaskInactive", "secondaryMaskActive", "secondaryMaskInactive", "inputIdsDtype", "attentionMaskDtype", "targetDtype", "objectiveMaskDtype", "shapePolicy"}, "POLICY")
    sequence = _contract(root["sequence"]); length = _contract(root["length"]); tensor = _contract(root["tensor"]); batch = _contract(root["batchCollation"]); objective = _contract(root["objectiveWeightingAuthority"])
    _equal(sequence, ("HIM_TRAINING_SEQUENCE_CONSTRUCTION_CONTRACT_V1", "1"), "SEQUENCE_CONTRACT_MISMATCH")
    _equal(length, ("HIM_TRAINING_SEQUENCE_LENGTH_PADDING_TRUNCATION_CONTRACT_V1", "1"), "LENGTH_CONTRACT_MISMATCH")
    _equal(tensor, ("HIM_TRAINING_TENSOR_CONSTRUCTION_CONTRACT_V1", "1"), "TENSOR_CONTRACT_MISMATCH")
    _equal(batch, ("HIM_P1_POINT_12_BATCH_COLLATION_CONTRACT_V1", "1"), "BATCH_COLLATION_CONTRACT_MISMATCH")
    _equal(objective, ("HIM_MULTI_OBJECTIVE_WEIGHTING_AUTHORITY_V1", "1"), "OBJECTIVE_WEIGHTING_AUTHORITY_MISMATCH")
    objective_digest = _digest(_string(root, "objectiveWeightingAuthorityDigest"), "OBJECTIVE_WEIGHTING_AUTHORITY_DIGEST")
    _equal(objective_digest, _objective_weighting_authority_digest(), "OBJECTIVE_WEIGHTING_AUTHORITY_DIGEST_MISMATCH")
    primary = _encodings(root["primaryTargetEncoding"]); secondary = _encodings(root["secondaryTargetEncoding"])
    policy = Point12Policy(sequence[0], sequence[1], length[0], length[1], tensor[0], tensor[1], batch[0], batch[1], objective[0], objective[1], objective_digest, _integer(root, "maxSequenceLength"), _string(root, "paddingStrategy"), _string(root, "paddingSide"), _integer(root, "padTokenId"), _string(root, "truncationPolicy"), _integer(root, "attentionMaskRealTokenValue"), _integer(root, "attentionMaskPadValue"), _boolean(root, "tokenTypeIdsRequired"), primary, secondary, _number(root, "primaryMaskActive"), _number(root, "primaryMaskInactive"), _number(root, "secondaryMaskActive"), _number(root, "secondaryMaskInactive"), _string(root, "inputIdsDtype"), _string(root, "attentionMaskDtype"), _string(root, "targetDtype"), _string(root, "objectiveMaskDtype"), _string(root, "shapePolicy"))
    _equal(policy.max_sequence_length, 128, "MAX_LENGTH_POLICY_MISMATCH")
    _equal(policy.padding_strategy, "BATCH_LONGEST_WITH_HARD_UPPER_BOUND", "PADDING_POLICY_MISMATCH")
    _equal(policy.padding_side, "RIGHT", "PADDING_SIDE_MISMATCH"); _equal(policy.pad_token_id, 1, "PAD_TOKEN_MISMATCH")
    _equal(policy.truncation_policy, "NO_TRUNCATION_FAIL_IF_TOO_LONG", "TRUNCATION_POLICY_MISMATCH")
    _equal(policy.attention_mask_real_token_value, 1, "ATTENTION_MASK_MISMATCH"); _equal(policy.attention_mask_pad_value, 0, "ATTENTION_MASK_MISMATCH")
    _require(not policy.token_type_ids_required, "TOKEN_TYPE_IDS_FORBIDDEN")
    _equal(policy.primary_target_encoding, (("EXISTING_CANONICAL", 1), ("IDENTITY", 2), ("VARIANT", 3), ("ALIAS", 4), ("NEW_CANONICAL", 5)), "PRIMARY_ENCODING_MISMATCH")
    _equal(policy.secondary_target_encoding, (("COMPATIBLE", 0), ("REJECT", 1)), "SECONDARY_ENCODING_MISMATCH")
    _equal((policy.primary_mask_active, policy.primary_mask_inactive, policy.secondary_mask_active, policy.secondary_mask_inactive), (1.0, 0.0, 1.0, 0.0), "MASK_POLICY_MISMATCH")
    _equal((policy.input_ids_dtype, policy.attention_mask_dtype, policy.target_dtype, policy.objective_mask_dtype), ("INT64", "INT64", "INT64", "FLOAT32"), "DTYPE_POLICY_MISMATCH")
    _equal(policy.shape_policy, "[B,L]_INPUTS_AND_MASKS_[B]_TARGETS_AND_MASKS", "SHAPE_POLICY_MISMATCH")
    return policy


def _decode_binding(value: Any) -> Point12ModelTokenizerBinding:
    root = _object(value, "MODEL_BINDING")
    _keys(root, {"modelFamilyId", "baseModelId", "baseModelArtifactDigest", "modelRevision", "tokenizerId", "tokenizerArtifactDigest", "tokenizerRevision", "modelConfigurationArtifactDigest", "modelBindingDigest", "modelBindingReference"}, "MODEL_BINDING")
    fields = (_string(root, "modelFamilyId"), _string(root, "baseModelId"), _digest(_string(root, "baseModelArtifactDigest"), "BASE_MODEL_ARTIFACT"), _string(root, "modelRevision"), _string(root, "tokenizerId"), _digest(_string(root, "tokenizerArtifactDigest"), "TOKENIZER_ARTIFACT"), _string(root, "tokenizerRevision"), _digest(_string(root, "modelConfigurationArtifactDigest"), "MODEL_CONFIG_ARTIFACT"), _digest(_string(root, "modelBindingDigest"), "MODEL_BINDING_DIGEST"), _string(root, "modelBindingReference"))
    expected_binding = _sha256("".join((_field("contract", "HIM_MODEL_BINDING_V1"), _field("version", "1"), _field("state", "MODEL_BINDING_VALIDATED"), _field("model-family-id", fields[0]), _field("base-model-id", fields[1]), _field("base-model-artifact-digest", fields[2]), _field("tokenizer-id", fields[4]), _field("tokenizer-artifact-digest", fields[5]), _field("model-configuration-artifact-digest", fields[7]))).encode())
    _equal(fields[8], expected_binding, "MODEL_BINDING_DIGEST_MISMATCH")
    _equal(fields[9], f"model-binding:v1:{fields[8]}", "MODEL_BINDING_REFERENCE_MISMATCH")
    return Point12ModelTokenizerBinding(*fields)


def _decode_member(value: Any) -> Point12Member:
    root = _object(value, "MEMBER")
    _keys(root, {"membershipReference", "exampleReference", "familyGroupReference", "sequence", "primaryTarget", "secondaryTarget", "primaryMask", "secondaryMask"}, "MEMBER")
    membership = _text(_string(root, "membershipReference"), "MEMBERSHIP_REFERENCE")
    example = _string(root, "exampleReference"); _require(_EXAMPLE.fullmatch(example) is not None, "EXAMPLE_REFERENCE_INVALID")
    family = _string(root, "familyGroupReference"); _require(family.startswith("family:v1:"), "FAMILY_REFERENCE_INVALID")
    primary = _integer(root, "primaryTarget"); secondary = _integer(root, "secondaryTarget"); pm = _number(root, "primaryMask"); sm = _number(root, "secondaryMask")
    _require(1 <= primary <= 5, "PRIMARY_TARGET_INVALID"); _require(secondary in (0, 1), "SECONDARY_TARGET_INVALID")
    _require(pm in (0.0, 1.0) and sm in (0.0, 1.0) and (pm == 1.0 or sm == 1.0), "OBJECTIVE_MASK_INVALID")
    return Point12Member(membership, example, family, _decode_sequence(root["sequence"]), primary, secondary, pm, sm)


def _decode_sequence(value: Any) -> Point12Sequence:
    root = _object(value, "SEQUENCE")
    _keys(root, {"contractId", "version", "state", "sequenceReference", "logicalDigest", "input", "segments"}, "SEQUENCE")
    contract_id = _string(root, "contractId"); version = _string(root, "version"); state = _string(root, "state")
    _equal(contract_id, "HIM_TRAINING_SEQUENCE_CONSTRUCTION_CONTRACT_V1", "SEQUENCE_CONTRACT_MISMATCH"); _equal(version, "1", "SEQUENCE_VERSION_MISMATCH"); _equal(state, "TRAINING_SEQUENCE_CONSTRUCTION_DEFINED", "SEQUENCE_STATE_MISMATCH")
    input_root = _object(root["input"], "SEQUENCE_INPUT"); _keys(input_root, {"observedTerm", "subject"}, "SEQUENCE_INPUT")
    observed = _text(_string(input_root, "observedTerm"), "OBSERVED_TERM")
    subject = _object(input_root["subject"], "SUBJECT"); _keys(subject, {"kind", "term"}, "SUBJECT"); kind = _string(subject, "kind"); _require(kind in {"CANDIDATE", "EXISTING_CANONICAL_TARGET"}, "SUBJECT_KIND_INVALID"); term = _text(_string(subject, "term"), "SUBJECT_TERM")
    segments = tuple((_string(_object(item, "SEGMENT"), "role"), _string(_object(item, "SEGMENT"), "text")) for item in _array(root["segments"], "SEGMENTS"))
    _equal(segments, (("OBSERVED_TERM", observed), ("COMPATIBILITY_SUBJECT", f"{kind} {term}")), "SEQUENCE_SEGMENTS_INVALID")
    logical = _digest(_string(root, "logicalDigest"), "SEQUENCE_DIGEST"); reference = _string(root, "sequenceReference"); _equal(reference, f"training-sequence:v1:{logical}", "SEQUENCE_REFERENCE_MISMATCH")
    expected = _sequence_digest(kind, segments)
    _equal(logical, expected, "SEQUENCE_DIGEST_MISMATCH")
    return Point12Sequence(contract_id, version, state, observed, kind, term, reference, logical, segments)


def _sequence_digest(kind: str, segments: tuple[tuple[str, str], ...]) -> str:
    parts = [_sequence_field("contract", "HIM_TRAINING_SEQUENCE_CONSTRUCTION_CONTRACT_V1"), _sequence_field("version", "1"), _sequence_field("state", "TRAINING_SEQUENCE_CONSTRUCTION_DEFINED"), _sequence_field("subject-kind", kind)]
    parts.extend(_sequence_field(f"segment-{i}-role", role) + _sequence_field(f"segment-{i}-text", text) for i, (role, text) in enumerate(segments))
    return _sha256("".join(parts).encode())


def _objective_weighting_authority_digest() -> str:
    parts = [
        _field("contract", "HIM_MULTI_OBJECTIVE_WEIGHTING_AUTHORITY_V1"),
        _field("version", "1"),
        _field("state", "MULTI_OBJECTIVE_WEIGHTING_AUTHORITY_CLOSED"),
        _field("primary-objective", "FOOD_IDENTITY_TARGET_KIND_CLASSIFICATION"),
        _field("secondary-objective", "CANDIDATE_COMPATIBILITY"),
        _field("objective-0", "FOOD_IDENTITY_TARGET_KIND_CLASSIFICATION"),
        _field("objective-1", "CANDIDATE_COMPATIBILITY"),
        _field("target-kind-weight-units", "1"),
        _field("candidate-compatibility-weight-units", "1"),
        _field("normalization", "DIVIDE_BY_SUM_OF_ACTIVE_WEIGHT_UNITS"),
        _field("active-contribution-required", "true"),
        _field("not-applicable-no-contribution", "true"),
        _field("zero-active-accepted", "false"),
        _field("sample-role-weighting", "false"),
        _field("boundary-weighting", "false"),
        _field("class-imbalance-weighting", "false"),
        _field("target-kind-class-weighting", "false"),
        _field("candidate-class-weighting", "false"),
        _field("target-reference-weighting", "false"),
        _field("rejected-kind-suppression-weight", "NONE"),
        _field("candidate-state-weighting", "false"),
        _field("weighting-search", "false"),
        _field("new-canonical-active-count", "1"),
        _field("new-canonical-candidate-na", "true"),
    ]
    parts.extend(_field(f"base-language-{index}", value) for index, value in enumerate(("DE", "EN", "FR", "ES", "IT")))
    return _sha256("".join(parts).encode())


def _payload_digest(partition_id: str, partition_digest: str, split: str, batch_id: str, batch_digest: str, target_length: int, policy: Point12Policy, binding: Point12ModelTokenizerBinding, members: tuple[Point12Member, ...]) -> str:
    parts = [_field("contract", CONTRACT_ID), _field("version", VERSION), _field("state", STATE), _field("partition-id", partition_id), _field("partition-digest", partition_digest), _field("split", split), _field("batch-plan-id", batch_id), _field("batch-plan-digest", batch_digest), _field("target-length", str(target_length))]
    parts.extend((_field("sequence", f"{policy.sequence_contract_id}:{policy.sequence_contract_version}"), _field("length", f"{policy.length_contract_id}:{policy.length_contract_version}"), _field("max-length", str(policy.max_sequence_length)), _field("padding", policy.padding_strategy), _field("padding-side", policy.padding_side), _field("pad-token-id", str(policy.pad_token_id)), _field("truncation", policy.truncation_policy), _field("attention-real", str(policy.attention_mask_real_token_value)), _field("attention-pad", str(policy.attention_mask_pad_value)), _field("token-type-ids", str(policy.token_type_ids_required).lower()), _field("tensor", f"{policy.tensor_contract_id}:{policy.tensor_contract_version}"), _field("batch-collation", f"{policy.batch_collation_contract_id}:{policy.batch_collation_contract_version}"), _field("objective-weighting", f"{policy.objective_weighting_authority_contract_id}:{policy.objective_weighting_authority_contract_version}"), _field("objective-weighting-digest", policy.objective_weighting_authority_digest)))
    parts.extend(_field(f"primary-code-{name}", str(code)) for name, code in policy.primary_target_encoding); parts.extend(_field(f"secondary-code-{name}", str(code)) for name, code in policy.secondary_target_encoding)
    parts.extend((_field("masks", f"{_float_text(policy.primary_mask_active)},{_float_text(policy.primary_mask_inactive)},{_float_text(policy.secondary_mask_active)},{_float_text(policy.secondary_mask_inactive)}"), _field("dtypes", f"{policy.input_ids_dtype},{policy.attention_mask_dtype},{policy.target_dtype},{policy.objective_mask_dtype}"), _field("shape", policy.shape_policy)))
    parts.extend((_field("model-family", binding.model_family_id), _field("base-model", binding.base_model_id), _field("base-model-artifact", binding.base_model_artifact_digest), _field("model-revision", binding.model_revision), _field("tokenizer-id", binding.tokenizer_id), _field("tokenizer-artifact", binding.tokenizer_artifact_digest), _field("tokenizer-revision", binding.tokenizer_revision), _field("model-config-artifact", binding.model_configuration_artifact_digest), _field("model-binding-digest", binding.model_binding_digest), _field("model-binding-reference", binding.model_binding_reference)))
    for i, member in enumerate(members):
        parts.extend((_field(f"member-{i}-membership", member.membership_reference), _field(f"member-{i}-example", member.example_reference), _field(f"member-{i}-family", member.family_group_reference), _field(f"member-{i}-sequence", member.sequence.sequence_reference), _field(f"member-{i}-sequence-digest", member.sequence.logical_digest), _field(f"member-{i}-primary", str(member.primary_target)), _field(f"member-{i}-secondary", str(member.secondary_target)), _field(f"member-{i}-primary-mask", _float_text(member.primary_mask)), _field(f"member-{i}-secondary-mask", _float_text(member.secondary_mask))))
    return _sha256("".join(parts).encode())


def _validate_content_digest(root: Mapping[str, Any], batch: Point12Batch) -> None:
    unsigned = dict(root); unsigned["contentSha256"] = None
    expected = _sha256(json.dumps(unsigned, ensure_ascii=False, separators=(",", ":")).encode("utf-8"))
    actual = root["contentSha256"]
    _equal(actual, expected, "CONTENT_DIGEST_MISMATCH")


def _contract(value: Any) -> tuple[str, str]:
    root = _object(value, "CONTRACT"); _keys(root, {"contractId", "version"}, "CONTRACT"); return _string(root, "contractId"), _string(root, "version")


def _encodings(value: Any) -> tuple[tuple[str, int], ...]:
    result = []
    for item in _array(value, "ENCODINGS"):
        root = _object(item, "ENCODING"); _keys(root, {"name", "code"}, "ENCODING"); result.append((_string(root, "name"), _integer(root, "code")))
    return tuple(result)


def _object(value: Any, name: str) -> dict[str, Any]:
    _require(isinstance(value, dict), f"{name}_OBJECT_REQUIRED"); return value


def _array(value: Any, name: str) -> list[Any]:
    _require(isinstance(value, list), f"{name}_ARRAY_REQUIRED"); return value


def _keys(value: Mapping[str, Any], expected: set[str], name: str) -> None:
    _require(set(value) == expected, f"{name}_FIELDS_INVALID")


def _string(root: Mapping[str, Any], name: str) -> str:
    value = root.get(name); _require(isinstance(value, str), f"{name}_STRING_REQUIRED"); return value


def _text(value: str, name: str) -> str:
    _require(bool(value.strip()) and not any(ord(ch) < 32 for ch in value), f"{name}_INVALID"); return value


def _integer(root: Mapping[str, Any], name: str) -> int:
    value = root.get(name); _require(isinstance(value, int) and not isinstance(value, bool), f"{name}_INTEGER_REQUIRED"); return value


def _number(root: Mapping[str, Any], name: str) -> float:
    value = root.get(name); _require(isinstance(value, (int, float)) and not isinstance(value, bool), f"{name}_NUMBER_REQUIRED"); return float(value)


def _boolean(root: Mapping[str, Any], name: str) -> bool:
    value = root.get(name); _require(isinstance(value, bool), f"{name}_BOOLEAN_REQUIRED"); return value


def _digest(value: str, name: str) -> str:
    _require(_DIGEST.fullmatch(value) is not None, f"{name}_INVALID"); return value


def _field(key: str, value: str) -> str:
    return f"{key}={_kotlin_length(value)}:{value}\n"


def _sequence_field(key: str, value: str) -> str:
    return f"{_kotlin_length(key)}:{key}{_kotlin_length(value)}:{value}|"


def _kotlin_length(value: str) -> int:
    return len(value.encode("utf-16-le")) // 2


def _float_text(value: float) -> str:
    return f"{value:.1f}" if value.is_integer() else str(value)


def _sha256(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def _equal(actual: Any, expected: Any, message: str) -> None:
    _require(actual == expected, message)


def _require(condition: bool, message: str) -> None:
    if not condition:
        raise Point12ProtocolError(message)


def _reject_constant(value: str) -> None:
    raise Point12ProtocolError(f"NON_FINITE_NUMBER:{value}")
