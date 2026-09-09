"""Strict, framework-neutral decoder for the HIM trainer request wire format."""

from __future__ import annotations

import json
import hashlib
import re
from dataclasses import dataclass
from enum import Enum
from typing import Any, Mapping

from .point13_loss_contract_v1 import (
    HimMaskedMultiObjectiveLossContractV1,
    decode_him_masked_multi_objective_loss_contract_v1,
)
from .point13_forward_rng_contract_v1 import (
    HimTrainForwardRngContractV1,
    decode_him_train_forward_rng_contract_v1,
)
from .point13_trainability_policy_v1 import (
    HimBaseEncoderTrainabilityPolicyV1,
    decode_him_base_encoder_trainability_policy_v1,
)
from .point13_optimizer_execution_policy_v1 import (
    HimOptimizerExecutionPolicyV1,
    decode_him_optimizer_execution_policy_v1,
)


_DIGEST = re.compile(r"[0-9a-f]{64}\Z")
_ENTITY_ID = re.compile(r"[0-9A-Za-z]{6}\Z")
MODEL_EXECUTION_BINDING_CONTRACT_ID_V1 = "HIM_MODEL_EXECUTION_BINDING_V1"
MODEL_EXECUTION_BINDING_VERSION_V1 = "1"
HEAD_INITIALIZATION_BINDING_CONTRACT_ID_V1 = "HIM_HEAD_INITIALIZATION_BINDING_V1"
HEAD_INITIALIZATION_BINDING_VERSION_V1 = "1"
HEAD_INITIALIZATION_SCHEME_V1 = "NORMAL_ZERO_BIAS"
HEAD_INITIALIZATION_RNG_V1 = "ISOLATED_TORCH_GENERATOR_CPU_V1"
HEAD_INITIALIZATION_ORDER_V1 = ("PRIMARY", "SECONDARY")
HEAD_INITIALIZATION_MEAN_V1 = "0"
HEAD_INITIALIZATION_STD_V1 = "0.02"
HEAD_INITIALIZATION_BIAS_V1 = "0"


class HimTrainerProtocolV1Error(ValueError):
    """Raised when a request is not an exact HIM protocol V1 value."""


def _canonical_digest(value: Mapping[str, Any]) -> str:
    encoded = json.dumps(value, ensure_ascii=False, separators=(",", ":"), sort_keys=True)
    return hashlib.sha256(encoded.encode("utf-8")).hexdigest()


def head_initialization_binding_logical_digest_v1(
    initialization_scheme: str,
    mean: str,
    std: str,
    bias: str,
    seed: int,
    initialization_order: tuple[str, ...],
    rng: str,
) -> str:
    return _canonical_digest({
        "contractId": HEAD_INITIALIZATION_BINDING_CONTRACT_ID_V1,
        "version": HEAD_INITIALIZATION_BINDING_VERSION_V1,
        "initializationScheme": initialization_scheme,
        "mean": mean,
        "std": std,
        "bias": bias,
        "seed": seed,
        "initializationOrder": list(initialization_order),
        "rng": rng,
    })


def model_execution_binding_logical_digest_v1(
    base_model_binding_logical_digest: str,
    head_contract_logical_digest: str,
    head_contract_reference: str,
    head_initialization_binding_logical_digest: str,
    head_initialization_binding_reference: str,
    full_initial_state_logical_digest: str,
    full_initial_state_reference: str,
) -> str:
    return _canonical_digest({
        "contractId": MODEL_EXECUTION_BINDING_CONTRACT_ID_V1,
        "version": MODEL_EXECUTION_BINDING_VERSION_V1,
        "baseModelBindingLogicalDigest": base_model_binding_logical_digest,
        "headContractLogicalDigest": head_contract_logical_digest,
        "headContractReference": head_contract_reference,
        "headInitializationBindingLogicalDigest": head_initialization_binding_logical_digest,
        "headInitializationBindingReference": head_initialization_binding_reference,
        "fullInitialStateLogicalDigest": full_initial_state_logical_digest,
        "fullInitialStateReference": full_initial_state_reference,
    })


class TargetKind(str, Enum):
    EXISTING_CANONICAL = "EXISTING_CANONICAL"
    IDENTITY = "IDENTITY"
    VARIANT = "VARIANT"
    ALIAS = "ALIAS"
    NEW_CANONICAL = "NEW_CANONICAL"


class CandidateCompatibility(str, Enum):
    COMPATIBLE = "COMPATIBLE"
    REJECT = "REJECT"


class ConditioningApplicability(str, Enum):
    APPLICABLE = "APPLICABLE"
    NOT_APPLICABLE = "NOT_APPLICABLE"


class RelationKind(str, Enum):
    IDENTITY = "IDENTITY"
    VARIANT = "VARIANT"
    ALIAS = "ALIAS"
    CREATE_NEW_CANONICAL = "CREATE_NEW_CANONICAL"


class CompatibilitySubjectKind(str, Enum):
    CANDIDATE = "CANDIDATE"
    EXISTING_CANONICAL_TARGET = "EXISTING_CANONICAL_TARGET"


class ScopeKind(str, Enum):
    CANONICAL = "CANONICAL"
    IDENTITY = "IDENTITY"


class Polarity(str, Enum):
    POSITIVE = "POSITIVE"
    NEGATIVE = "NEGATIVE"


class NegativeBoundaryType(str, Enum):
    WRONG_CLASSIFICATION = "WRONG_CLASSIFICATION"
    WRONG_SCOPE = "WRONG_SCOPE"
    WRONG_RELATION_LEVEL = "WRONG_RELATION_LEVEL"
    CANONICAL_VS_CHILD_BOUNDARY = "CANONICAL_VS_CHILD_BOUNDARY"
    IDENTITY_VS_VARIANT_BOUNDARY = "IDENTITY_VS_VARIANT_BOUNDARY"
    ALIAS_VS_SEMANTIC_CHILD_BOUNDARY = "ALIAS_VS_SEMANTIC_CHILD_BOUNDARY"
    CROSS_CANONICAL_CLASSIFICATION_BOUNDARY = "CROSS_CANONICAL_CLASSIFICATION_BOUNDARY"


class ObjectiveActivity(str, Enum):
    ACTIVE = "ACTIVE"
    NOT_APPLICABLE = "NOT_APPLICABLE"


@dataclass(frozen=True)
class FamilyReference:
    kind: ScopeKind
    canonical_id: str
    identity_id: str | None = None


@dataclass(frozen=True)
class Relation:
    kind: RelationKind
    parent_canonical_id: str | None = None
    scope: FamilyReference | None = None
    equivalent_entity: FamilyReference | None = None


@dataclass(frozen=True)
class PrimaryInput:
    observed_term: str
    normalized_observed_term: str


@dataclass(frozen=True)
class CandidateConditioningInput:
    contract_id: str
    version: str
    state: str
    primary_input: PrimaryInput
    proposed_target_kind: TargetKind
    candidate_term: str
    normalized_candidate_term: str
    relation: Relation
    conditioning_id: str
    conditioning_digest: str


@dataclass(frozen=True)
class CandidateClaim:
    conditioning: CandidateConditioningInput
    compatibility: CandidateCompatibility


@dataclass(frozen=True)
class CandidateConditioning:
    applicability: ConditioningApplicability
    claims: tuple[CandidateClaim, ...] = ()

    @property
    def candidate_compatibility_objective(self) -> ObjectiveActivity:
        return (
            ObjectiveActivity.ACTIVE
            if self.applicability is ConditioningApplicability.APPLICABLE
            else ObjectiveActivity.NOT_APPLICABLE
        )


@dataclass(frozen=True)
class CompatibilityConditioningInput:
    contract_id: str
    version: str
    state: str
    primary_input: PrimaryInput
    proposed_target_kind: TargetKind
    subject_kind: CompatibilitySubjectKind
    term: str
    normalized_term: str
    relation: Relation | None
    conditioning_id: str
    conditioning_digest: str


@dataclass(frozen=True)
class CompatibilityClaim:
    conditioning: CompatibilityConditioningInput
    compatibility: CandidateCompatibility


@dataclass(frozen=True)
class CompatibilityConditioning:
    applicability: ConditioningApplicability
    claims: tuple[CompatibilityClaim, ...] = ()

    @property
    def candidate_compatibility_objective(self) -> ObjectiveActivity:
        return (
            ObjectiveActivity.ACTIVE
            if self.applicability is ConditioningApplicability.APPLICABLE
            else ObjectiveActivity.NOT_APPLICABLE
        )


@dataclass(frozen=True)
class CanonicalContext:
    rank: int
    canonical_id: str
    canonical_name: str
    full_record_canonical_json: str | None


@dataclass(frozen=True)
class Evidence:
    source: str
    source_artifact_sha256: str
    source_record_identity: str
    record_kind: str
    retrieval_rank: int


@dataclass(frozen=True)
class TrainerInput:
    observed_term: str
    normalized_observed_term: str
    canonical_context: tuple[CanonicalContext, ...]
    evidence: tuple[Evidence, ...]


@dataclass(frozen=True)
class SemanticTarget:
    kind: TargetKind
    kind_code: int
    canonical_id: str | None = None
    parent_canonical_id: str | None = None
    scope: FamilyReference | None = None
    equivalent_entity: FamilyReference | None = None
    proposed_canonical_name: str | None = None


@dataclass(frozen=True)
class EncodedTarget:
    kind: Polarity
    encoding_contract_id: str
    encoding_version: str
    objective_digest: str
    objective_reference: str
    semantic_target: SemanticTarget
    target_kind_code: int
    loss_role_codes: tuple[int, ...]
    logical_digest: str
    target_reference: str
    positive_target: EncodedTarget | None = None
    rejected_target: SemanticTarget | None = None
    boundary_type: NegativeBoundaryType | None = None
    boundary_type_code: int | None = None
    rejected_target_logical_digest: str | None = None
    rejected_target_reference: str | None = None

    @property
    def target_kind_supervision(self) -> TargetKind:
        return self.semantic_target.kind


@dataclass(frozen=True)
class TrainingRecord:
    assignment_index: int
    record_reference: str
    positive_example_reference: str
    group_reference: str
    partition: str
    polarity: Polarity
    input: TrainerInput
    encoded_target: EncodedTarget
    candidate_conditioning: CandidateConditioning
    logical_digest: str
    compatibility_conditioning: CompatibilityConditioning | None = None


@dataclass(frozen=True)
class Configuration:
    seed: int
    epochs: int
    micro_batch_size: int
    gradient_accumulation_steps: int
    learning_rate: str
    optimizer_id: str
    logical_digest: str
    configuration_reference: str


@dataclass(frozen=True)
class ModelBinding:
    model_family_id: str
    base_model_id: str
    base_model_artifact_digest: str
    tokenizer_id: str
    tokenizer_artifact_digest: str
    model_configuration_artifact_digest: str
    logical_digest: str
    model_binding_reference: str


@dataclass(frozen=True)
class HeadInitializationBinding:
    contract_id: str
    version: str
    initialization_scheme: str
    mean: str
    std: str
    bias: str
    seed: int
    initialization_order: tuple[str, ...]
    rng: str
    logical_digest: str
    reference: str


@dataclass(frozen=True)
class ModelExecutionBinding:
    contract_id: str
    version: str
    base_model_binding_logical_digest: str
    head_contract_logical_digest: str
    head_contract_reference: str
    head_initialization_binding: HeadInitializationBinding
    full_initial_state_logical_digest: str
    full_initial_state_reference: str
    logical_digest: str
    reference: str


@dataclass(frozen=True)
class AdamW:
    optimizer_id: str
    algorithm: str
    learning_rate: str
    beta1: str
    beta2: str
    epsilon: str
    weight_decay: str
    decoupled_weight_decay: bool
    bias_correction: bool


@dataclass(frozen=True)
class TrainingRequest:
    protocol_reference: str
    protocol_logical_digest: str
    trainer_port_request_reference: str
    trainer_port_request_logical_digest: str
    training_mission_reference: str
    training_mission_logical_digest: str
    objective_reference: str
    objective_logical_digest: str
    target_encoding_reference: str
    target_encoding_logical_digest: str
    implementation_fingerprint: str
    partition: str
    configuration: Configuration
    model_binding: ModelBinding
    model_execution_binding: ModelExecutionBinding
    forward_rng_authority: HimTrainForwardRngContractV1
    loss_authority: HimMaskedMultiObjectiveLossContractV1
    trainability_policy: HimBaseEncoderTrainabilityPolicyV1
    optimizer_execution_policy: HimOptimizerExecutionPolicyV1
    adamw: AdamW
    records: tuple[TrainingRecord, ...]

    @property
    def identity_digest(self) -> str:
        """Digest of all semantic request authorities, including execution state."""

        return _canonical_digest({
            "protocolLogicalDigest": self.protocol_logical_digest,
            "trainerPortRequestLogicalDigest": self.trainer_port_request_logical_digest,
            "trainingMissionLogicalDigest": self.training_mission_logical_digest,
            "objectiveLogicalDigest": self.objective_logical_digest,
            "targetEncodingLogicalDigest": self.target_encoding_logical_digest,
            "implementationFingerprint": self.implementation_fingerprint,
            "partition": self.partition,
            "configuration": {
                "seed": self.configuration.seed,
                "epochs": self.configuration.epochs,
                "microBatchSize": self.configuration.micro_batch_size,
                "gradientAccumulationSteps": self.configuration.gradient_accumulation_steps,
                "learningRate": self.configuration.learning_rate,
                "optimizerId": self.configuration.optimizer_id,
                "logicalDigest": self.configuration.logical_digest,
            },
            "modelBinding": {
                "modelFamilyId": self.model_binding.model_family_id,
                "baseModelId": self.model_binding.base_model_id,
                "baseModelArtifactDigest": self.model_binding.base_model_artifact_digest,
                "tokenizerId": self.model_binding.tokenizer_id,
                "tokenizerArtifactDigest": self.model_binding.tokenizer_artifact_digest,
                "modelConfigurationArtifactDigest": self.model_binding.model_configuration_artifact_digest,
                "logicalDigest": self.model_binding.logical_digest,
            },
            "modelExecutionBindingLogicalDigest": self.model_execution_binding.logical_digest,
            "forwardRngAuthority": {
                "contractId": self.forward_rng_authority.contract_id,
                "version": self.forward_rng_authority.version,
                "state": self.forward_rng_authority.state,
                "rootSeedSource": self.forward_rng_authority.root_seed_source,
                "rootSeed": self.forward_rng_authority.root_seed,
                "domainSeparator": self.forward_rng_authority.domain_separator,
                "derivedForwardSeed": self.forward_rng_authority.derived_forward_seed,
                "device": self.forward_rng_authority.device,
                "initialization": self.forward_rng_authority.initialization,
                "resetPolicy": self.forward_rng_authority.reset_policy,
                "streamSemantics": self.forward_rng_authority.stream_semantics,
                "globalStatePolicy": self.forward_rng_authority.global_state_policy,
                "logicalDigest": self.forward_rng_authority.logical_digest,
                "reference": self.forward_rng_authority.reference,
            },
            "lossAuthority": {
                "contractId": self.loss_authority.contract_id,
                "version": self.loss_authority.version,
                "state": self.loss_authority.state,
                "logicalDigest": self.loss_authority.logical_digest,
                "reference": self.loss_authority.reference,
            },
            "trainabilityPolicy": self.trainability_policy.identity_payload() | {
                "logicalDigest": self.trainability_policy.logical_digest,
                "reference": self.trainability_policy.reference,
            },
            "optimizerExecutionPolicy": self.optimizer_execution_policy.identity_payload() | {
                "logicalDigest": self.optimizer_execution_policy.logical_digest,
                "reference": self.optimizer_execution_policy.reference,
            },
            "adamW": {
                "optimizerId": self.adamw.optimizer_id,
                "algorithm": self.adamw.algorithm,
                "learningRate": self.adamw.learning_rate,
                "beta1": self.adamw.beta1,
                "beta2": self.adamw.beta2,
                "epsilon": self.adamw.epsilon,
                "weightDecay": self.adamw.weight_decay,
                "decoupledWeightDecay": self.adamw.decoupled_weight_decay,
                "biasCorrection": self.adamw.bias_correction,
            },
            "recordLogicalDigests": [record.logical_digest for record in self.records],
        })


@dataclass(frozen=True)
class TrainerIdentity:
    module: str
    implementation_fingerprint: str
    implementation_reference: str
    entrypoint_mode: str
    process_adapter_implementation_fingerprint: str


@dataclass(frozen=True)
class ProcessPolicies:
    launcher: str
    request_transport: str
    network_access: str
    environment_policy: str
    stdin_interactive: str
    stdout_policy: str
    stderr_policy: str
    output_bounds_required: str
    timeout_policy_required: str
    cancellation_policy_required: str
    process_execution_policy_fingerprint: str
    request_serialization_contract_id: str
    request_serialization_version: str
    result_protocol_contract_id: str
    result_protocol_version: str
    artifact_reverification: str
    trainer_reverification: str


@dataclass(frozen=True)
class RuntimeEnvironment:
    reference: str
    logical_digest: str
    runtime_channel: str
    python_version: str
    python_implementation: str
    pytorch_version: str


@dataclass(frozen=True)
class VerifiedArtifact:
    role: str
    expected_sha256: str
    verified_actual_sha256: str
    operational_path: str
    relative_path: str
    byte_size: int


@dataclass(frozen=True)
class ExternalTrainerRequest:
    contract_id: str
    version: str
    state: str
    format_id: str
    process_binding_reference: str
    process_binding_logical_digest: str
    runtime_binding_reference: str
    runtime_binding_logical_digest: str
    execution_request_reference: str
    execution_request_logical_digest: str
    trainer: TrainerIdentity
    device: str
    launcher: str
    working_directory: str
    request_transport: str
    policies: ProcessPolicies
    runtime_environment: RuntimeEnvironment
    artifacts: tuple[VerifiedArtifact, ...]
    training_request: TrainingRequest
    content_sha256: str

    @property
    def sample_count(self) -> int:
        return len(self.training_request.records)


def decode_external_trainer_request_v1(payload: str | bytes) -> ExternalTrainerRequest:
    """Decode one complete canonical external-trainer request, fail-closed."""

    root = _parse_json(payload)
    _keys(root, {
        "contractId", "version", "state", "formatId", "processBindingReference",
        "processBindingLogicalDigest", "runtimeBindingReference", "runtimeBindingLogicalDigest",
        "executionRequestReference", "executionRequestLogicalDigest", "trainer", "device", "launcher",
        "workingDirectory", "requestTransport", "policies", "runtimeEnvironment", "artifacts",
        "trainingRequest", "contentSha256",
    }, "REQUEST")
    _equals(_string(root, "contractId"), "HIM_EXTERNAL_TRAINER_PROCESS_REQUEST_SERIALIZATION_V1", "REQUEST_CONTRACT_MISMATCH")
    _equals(_string(root, "version"), "1", "REQUEST_VERSION_MISMATCH")
    _equals(_string(root, "state"), "EXTERNAL_TRAINER_PROCESS_REQUEST_SERIALIZED", "REQUEST_STATE_MISMATCH")
    _equals(_string(root, "formatId"), "him-external-trainer-process-request:v1", "REQUEST_FORMAT_MISMATCH")
    process_digest = _sha(root, "processBindingLogicalDigest")
    runtime_digest = _sha(root, "runtimeBindingLogicalDigest")
    execution_digest = _sha(root, "executionRequestLogicalDigest")
    _bound_ref(root, "processBindingReference", "external-trainer-process-binding:v1:", process_digest)
    _bound_ref(root, "runtimeBindingReference", "external-trainer-runtime-binding:v1:", runtime_digest)
    _bound_ref(root, "executionRequestReference", "external-trainer-execution-request:v1:", execution_digest)
    trainer = _decode_trainer(_object(root, "trainer"))
    device = _enum_string(root, "device", {"CPU", "MPS"}, "UNKNOWN_DEVICE")
    launcher = _enum_string(root, "launcher", {"UV"}, "UNKNOWN_LAUNCHER")
    working_directory = _string(root, "workingDirectory")
    _require(working_directory == "training/him", "WORKING_DIRECTORY_MISMATCH")
    request_transport = _enum_string(root, "requestTransport", {"IMMUTABLE_TEMP_FILE"}, "UNKNOWN_REQUEST_TRANSPORT")
    policies = _decode_policies(_object(root, "policies"))
    environment = _decode_environment(_object(root, "runtimeEnvironment"))
    artifacts = tuple(_decode_artifact(value) for value in _list(root, "artifacts"))
    _require(len(artifacts) == 3, "ARTIFACT_COUNT_INVALID")
    _require(tuple(a.role for a in artifacts) == ("BASE_MODEL", "TOKENIZER", "MODEL_CONFIGURATION"), "ARTIFACT_ORDER_INVALID")
    training_request = _decode_training_request(_object(root, "trainingRequest"))
    content_sha256 = _sha(root, "contentSha256")
    return ExternalTrainerRequest(
        contract_id=_string(root, "contractId"), version=_string(root, "version"), state=_string(root, "state"),
        format_id=_string(root, "formatId"), process_binding_reference=_string(root, "processBindingReference"),
        process_binding_logical_digest=process_digest, runtime_binding_reference=_string(root, "runtimeBindingReference"),
        runtime_binding_logical_digest=runtime_digest, execution_request_reference=_string(root, "executionRequestReference"),
        execution_request_logical_digest=execution_digest, trainer=trainer, device=device, launcher=launcher,
        working_directory=working_directory, request_transport=request_transport, policies=policies,
        runtime_environment=environment, artifacts=artifacts, training_request=training_request,
        content_sha256=content_sha256,
    )


def _decode_trainer(root: dict[str, Any]) -> TrainerIdentity:
    _keys(root, {"module", "implementationFingerprint", "implementationReference", "entrypointMode", "processAdapterImplementationFingerprint"}, "TRAINER")
    fingerprint = _sha(root, "implementationFingerprint")
    adapter_fingerprint = _sha(root, "processAdapterImplementationFingerprint")
    _bound_ref(root, "implementationReference", "trainer-implementation:v1:", fingerprint)
    _equals(_string(root, "module"), "him_trainer", "TRAINER_MODULE_MISMATCH")
    _equals(_string(root, "entrypointMode"), "PYTHON_MODULE", "TRAINER_ENTRYPOINT_MODE_MISMATCH")
    return TrainerIdentity(_string(root, "module"), fingerprint, _string(root, "implementationReference"), _string(root, "entrypointMode"), adapter_fingerprint)


def _decode_policies(root: dict[str, Any]) -> ProcessPolicies:
    expected = {"launcher", "requestTransport", "networkAccess", "environmentPolicy", "stdinInteractive", "stdoutPolicy", "stderrPolicy", "outputBoundsRequired", "timeoutPolicyRequired", "cancellationPolicyRequired", "processExecutionPolicyFingerprint", "requestSerializationContractId", "requestSerializationVersion", "resultProtocolContractId", "resultProtocolVersion", "artifactReverification", "trainerReverification"}
    _keys(root, expected, "POLICIES")
    fingerprint = _sha(root, "processExecutionPolicyFingerprint")
    _equals(_string(root, "launcher"), "UV", "POLICY_LAUNCHER_MISMATCH")
    _equals(_string(root, "requestTransport"), "IMMUTABLE_TEMP_FILE", "POLICY_TRANSPORT_MISMATCH")
    _equals(_string(root, "networkAccess"), "FORBIDDEN", "POLICY_NETWORK_MISMATCH")
    _equals(_string(root, "environmentPolicy"), "ALLOWLIST_ONLY", "POLICY_ENVIRONMENT_MISMATCH")
    _equals(_string(root, "stdinInteractive"), "NO", "POLICY_STDIN_MISMATCH")
    return ProcessPolicies(
        _string(root, "launcher"), _string(root, "requestTransport"), _string(root, "networkAccess"), _string(root, "environmentPolicy"),
        _string(root, "stdinInteractive"), _string(root, "stdoutPolicy"), _string(root, "stderrPolicy"), _string(root, "outputBoundsRequired"),
        _string(root, "timeoutPolicyRequired"), _string(root, "cancellationPolicyRequired"), fingerprint, _string(root, "requestSerializationContractId"),
        _string(root, "requestSerializationVersion"), _string(root, "resultProtocolContractId"), _string(root, "resultProtocolVersion"),
        _string(root, "artifactReverification"), _string(root, "trainerReverification"),
    )


def _decode_environment(root: dict[str, Any]) -> RuntimeEnvironment:
    _keys(root, {"reference", "logicalDigest", "runtimeChannel", "pythonVersion", "pythonImplementation", "pytorchVersion"}, "ENVIRONMENT")
    digest = _sha(root, "logicalDigest")
    _bound_ref(root, "reference", "python-pytorch-runtime-environment:v1:", digest)
    return RuntimeEnvironment(_string(root, "reference"), digest, _string(root, "runtimeChannel"), _string(root, "pythonVersion"), _string(root, "pythonImplementation"), _string(root, "pytorchVersion"))


def _decode_artifact(value: Any) -> VerifiedArtifact:
    root = _as_object(value, "ARTIFACT")
    _keys(root, {"role", "expectedSha256", "verifiedActualSha256", "operationalPath", "relativePath", "byteSize"}, "ARTIFACT")
    expected = _sha(root, "expectedSha256")
    actual = _sha(root, "verifiedActualSha256")
    _require(expected == actual, "ARTIFACT_DIGEST_MISMATCH")
    size = _integer(root, "byteSize")
    _require(size >= 0, "ARTIFACT_SIZE_INVALID")
    return VerifiedArtifact(_string(root, "role"), expected, actual, _string(root, "operationalPath"), _string(root, "relativePath"), size)


def _decode_training_request(root: dict[str, Any]) -> TrainingRequest:
    _keys(root, {"protocolReference", "protocolLogicalDigest", "trainerPortRequestReference", "trainerPortRequestLogicalDigest", "trainingMissionReference", "trainingMissionLogicalDigest", "objectiveReference", "objectiveLogicalDigest", "targetEncodingReference", "targetEncodingLogicalDigest", "implementationFingerprint", "partition", "configuration", "modelBinding", "modelExecutionBinding", "forwardRngAuthority", "lossAuthority", "trainabilityPolicy", "optimizerExecutionPolicy", "adamW", "records"}, "TRAINING_REQUEST")
    protocol_digest = _sha(root, "protocolLogicalDigest")
    trainer_port_digest = _sha(root, "trainerPortRequestLogicalDigest")
    mission_digest = _sha(root, "trainingMissionLogicalDigest")
    objective_digest = _sha(root, "objectiveLogicalDigest")
    target_encoding_digest = _sha(root, "targetEncodingLogicalDigest")
    implementation_fingerprint = _sha(root, "implementationFingerprint")
    _reference_format(root, "protocolReference", "trainer-protocol:v1:")
    _bound_ref(root, "trainerPortRequestReference", "trainer-request:v1:", trainer_port_digest)
    _bound_ref(root, "objectiveReference", "training-objective:v1:", objective_digest)
    _bound_ref(root, "targetEncodingReference", "training-target-encoding:v1:", target_encoding_digest)
    _equals(_string(root, "partition"), "TRAIN_ONLY", "TRAINING_PARTITION_MISMATCH")
    records = tuple(_decode_record(value) for value in _list(root, "records"))
    _require(records, "TRAINING_RECORDS_EMPTY")
    _require(tuple(record.assignment_index for record in records) == tuple(range(len(records))), "TRAINING_RECORD_ORDER_INVALID")
    configuration = _decode_configuration(_object(root, "configuration"))
    model_binding = _decode_model_binding(_object(root, "modelBinding"))
    model_execution_binding = _decode_model_execution_binding(
        _object(root, "modelExecutionBinding"),
        expected_base_model_binding_logical_digest=model_binding.logical_digest,
        expected_seed=configuration.seed,
    )
    forward_rng_authority = decode_him_train_forward_rng_contract_v1(root["forwardRngAuthority"])
    if forward_rng_authority.root_seed != configuration.seed:
        raise HimTrainerProtocolV1Error("FORWARD_RNG_ROOT_SEED_MISMATCH")
    loss_authority = decode_him_masked_multi_objective_loss_contract_v1(root["lossAuthority"])
    trainability_policy = decode_him_base_encoder_trainability_policy_v1(root["trainabilityPolicy"])
    optimizer_execution_policy = decode_him_optimizer_execution_policy_v1(root["optimizerExecutionPolicy"])
    _equals(optimizer_execution_policy.learning_rate, configuration.learning_rate, "OPTIMIZER_CONFIGURATION_LEARNING_RATE_MISMATCH")
    if optimizer_execution_policy.gradient_accumulation_steps != configuration.gradient_accumulation_steps:
        raise HimTrainerProtocolV1Error("OPTIMIZER_CONFIGURATION_ACCUMULATION_MISMATCH")
    adamw = _decode_adamw(_object(root, "adamW"))
    if (
        adamw.optimizer_id != optimizer_execution_policy.optimizer_id
        or adamw.algorithm != optimizer_execution_policy.optimizer_type
        or adamw.learning_rate != optimizer_execution_policy.learning_rate
        or adamw.beta1 != optimizer_execution_policy.beta1
        or adamw.beta2 != optimizer_execution_policy.beta2
        or adamw.epsilon != optimizer_execution_policy.epsilon
        or adamw.weight_decay != optimizer_execution_policy.weight_decay
        or adamw.decoupled_weight_decay != optimizer_execution_policy.decoupled_weight_decay
        or adamw.bias_correction != optimizer_execution_policy.bias_correction
    ):
        raise HimTrainerProtocolV1Error("ADAMW_EXECUTION_AUTHORITY_MISMATCH")
    return TrainingRequest(
        _string(root, "protocolReference"), protocol_digest, _string(root, "trainerPortRequestReference"), trainer_port_digest,
        _string(root, "trainingMissionReference"), mission_digest, _string(root, "objectiveReference"), objective_digest,
        _string(root, "targetEncodingReference"), target_encoding_digest, implementation_fingerprint, _string(root, "partition"),
        configuration, model_binding, model_execution_binding, forward_rng_authority, loss_authority, trainability_policy,
        optimizer_execution_policy,
        adamw, records,
    )


def _decode_configuration(root: dict[str, Any]) -> Configuration:
    _keys(root, {"seed", "epochs", "microBatchSize", "gradientAccumulationSteps", "learningRate", "optimizerId", "logicalDigest", "configurationReference"}, "CONFIGURATION")
    digest = _sha(root, "logicalDigest")
    _bound_ref(root, "configurationReference", "training-configuration:v1:", digest)
    return Configuration(_integer(root, "seed"), _integer(root, "epochs"), _integer(root, "microBatchSize"), _integer(root, "gradientAccumulationSteps"), _string(root, "learningRate"), _string(root, "optimizerId"), digest, _string(root, "configurationReference"))


def _decode_model_binding(root: dict[str, Any]) -> ModelBinding:
    _keys(root, {"modelFamilyId", "baseModelId", "baseModelArtifactDigest", "tokenizerId", "tokenizerArtifactDigest", "modelConfigurationArtifactDigest", "logicalDigest", "modelBindingReference"}, "MODEL_BINDING")
    digest = _sha(root, "logicalDigest")
    _bound_ref(root, "modelBindingReference", "model-binding:v1:", digest)
    return ModelBinding(_string(root, "modelFamilyId"), _string(root, "baseModelId"), _sha(root, "baseModelArtifactDigest"), _string(root, "tokenizerId"), _sha(root, "tokenizerArtifactDigest"), _sha(root, "modelConfigurationArtifactDigest"), digest, _string(root, "modelBindingReference"))


def _decode_head_initialization_binding(root: dict[str, Any], expected_seed: int) -> HeadInitializationBinding:
    _keys(root, {"contractId", "version", "initializationScheme", "mean", "std", "bias", "seed", "initializationOrder", "rng", "logicalDigest", "reference"}, "HEAD_INITIALIZATION_BINDING")
    contract_id = _string(root, "contractId")
    version = _string(root, "version")
    initialization_scheme = _string(root, "initializationScheme")
    mean = _string(root, "mean")
    std = _string(root, "std")
    bias = _string(root, "bias")
    seed = _integer(root, "seed")
    order_values = _list(root, "initializationOrder")
    if any(not isinstance(value, str) for value in order_values):
        raise HimTrainerProtocolV1Error("HEAD_INITIALIZATION_ORDER_INVALID")
    initialization_order = tuple(order_values)
    rng = _string(root, "rng")
    logical_digest = _sha(root, "logicalDigest")
    reference = _string(root, "reference")
    _equals(contract_id, HEAD_INITIALIZATION_BINDING_CONTRACT_ID_V1, "HEAD_INITIALIZATION_CONTRACT_MISMATCH")
    _equals(version, HEAD_INITIALIZATION_BINDING_VERSION_V1, "HEAD_INITIALIZATION_VERSION_MISMATCH")
    _equals(initialization_scheme, HEAD_INITIALIZATION_SCHEME_V1, "HEAD_INITIALIZATION_SCHEME_MISMATCH")
    _equals(mean, HEAD_INITIALIZATION_MEAN_V1, "HEAD_INITIALIZATION_MEAN_MISMATCH")
    _equals(std, HEAD_INITIALIZATION_STD_V1, "HEAD_INITIALIZATION_STD_MISMATCH")
    _equals(bias, HEAD_INITIALIZATION_BIAS_V1, "HEAD_INITIALIZATION_BIAS_MISMATCH")
    _equals(seed, expected_seed, "HEAD_INITIALIZATION_SEED_MISMATCH")
    _equals(initialization_order, HEAD_INITIALIZATION_ORDER_V1, "HEAD_INITIALIZATION_ORDER_MISMATCH")
    _equals(rng, HEAD_INITIALIZATION_RNG_V1, "HEAD_INITIALIZATION_RNG_MISMATCH")
    expected_digest = head_initialization_binding_logical_digest_v1(
        initialization_scheme, mean, std, bias, seed, initialization_order, rng,
    )
    _equals(logical_digest, expected_digest, "HEAD_INITIALIZATION_DIGEST_MISMATCH")
    _equals(reference, f"him-head-initialization:v1:{logical_digest}", "HEAD_INITIALIZATION_REFERENCE_MISMATCH")
    return HeadInitializationBinding(contract_id, version, initialization_scheme, mean, std, bias, seed, initialization_order, rng, logical_digest, reference)


def _decode_model_execution_binding(
    root: dict[str, Any],
    *,
    expected_base_model_binding_logical_digest: str,
    expected_seed: int,
) -> ModelExecutionBinding:
    _keys(root, {"contractId", "version", "baseModelBindingLogicalDigest", "headContractLogicalDigest", "headContractReference", "headInitializationBinding", "fullInitialStateLogicalDigest", "fullInitialStateReference", "logicalDigest", "reference"}, "MODEL_EXECUTION_BINDING")
    contract_id = _string(root, "contractId")
    version = _string(root, "version")
    base_digest = _sha(root, "baseModelBindingLogicalDigest")
    head_digest = _sha(root, "headContractLogicalDigest")
    head_reference = _string(root, "headContractReference")
    initialization = _decode_head_initialization_binding(_object(root, "headInitializationBinding"), expected_seed)
    full_digest = _sha(root, "fullInitialStateLogicalDigest")
    full_reference = _string(root, "fullInitialStateReference")
    logical_digest = _sha(root, "logicalDigest")
    reference = _string(root, "reference")
    _equals(contract_id, MODEL_EXECUTION_BINDING_CONTRACT_ID_V1, "MODEL_EXECUTION_CONTRACT_MISMATCH")
    _equals(version, MODEL_EXECUTION_BINDING_VERSION_V1, "MODEL_EXECUTION_VERSION_MISMATCH")
    _equals(base_digest, expected_base_model_binding_logical_digest, "MODEL_EXECUTION_BASE_BINDING_MISMATCH")
    if not head_reference.startswith("him-head-architecture:v1:"):
        raise HimTrainerProtocolV1Error("MODEL_EXECUTION_HEAD_REFERENCE_INVALID")
    _equals(head_reference, f"him-head-architecture:v1:{head_digest}", "MODEL_EXECUTION_HEAD_REFERENCE_MISMATCH")
    _equals(full_reference, f"him-full-initial-state:v1:{full_digest}", "MODEL_EXECUTION_FULL_STATE_REFERENCE_MISMATCH")
    expected_logical_digest = model_execution_binding_logical_digest_v1(
        base_digest, head_digest, head_reference, initialization.logical_digest, initialization.reference, full_digest, full_reference,
    )
    _equals(logical_digest, expected_logical_digest, "MODEL_EXECUTION_DIGEST_MISMATCH")
    _equals(reference, f"him-model-execution-binding:v1:{logical_digest}", "MODEL_EXECUTION_REFERENCE_MISMATCH")
    return ModelExecutionBinding(contract_id, version, base_digest, head_digest, head_reference, initialization, full_digest, full_reference, logical_digest, reference)


def _decode_adamw(root: dict[str, Any]) -> AdamW:
    _keys(root, {"optimizerId", "algorithm", "learningRate", "beta1", "beta2", "epsilon", "weightDecay", "decoupledWeightDecay", "biasCorrection"}, "ADAMW")
    return AdamW(_string(root, "optimizerId"), _string(root, "algorithm"), _string(root, "learningRate"), _string(root, "beta1"), _string(root, "beta2"), _string(root, "epsilon"), _string(root, "weightDecay"), _boolean(root, "decoupledWeightDecay"), _boolean(root, "biasCorrection"))


def _decode_record(value: Any) -> TrainingRecord:
    root = _as_object(value, "RECORD")
    base_keys = {"assignmentIndex", "recordReference", "positiveExampleReference", "groupReference", "partition", "polarity", "input", "encodedTarget", "logicalDigest"}
    conditioning_keys = set(root) - base_keys
    _require(conditioning_keys in ({"candidateConditioning"}, {"compatibilityConditioning"}), "RECORD_CONDITIONING_FIELDS_INVALID")
    polarity = Polarity(_enum_string(root, "polarity", {"POSITIVE", "NEGATIVE"}, "UNKNOWN_POLARITY"))
    target = _decode_target(_object(root, "encodedTarget"))
    candidate_conditioning = _decode_conditioning(_object(root, "candidateConditioning")) if "candidateConditioning" in root else CandidateConditioning(ConditioningApplicability.NOT_APPLICABLE)
    compatibility_conditioning = _decode_compatibility_conditioning(_object(root, "compatibilityConditioning")) if "compatibilityConditioning" in root else None
    _require(target.kind is polarity, "TARGET_POLARITY_MISMATCH")
    if target.target_kind_supervision is TargetKind.NEW_CANONICAL:
        _require(candidate_conditioning.applicability is ConditioningApplicability.NOT_APPLICABLE, "NEW_CANONICAL_CONDITIONING_FORBIDDEN")
        _require(compatibility_conditioning is None or compatibility_conditioning.applicability is ConditioningApplicability.NOT_APPLICABLE, "NEW_CANONICAL_CONDITIONING_FORBIDDEN")
    return TrainingRecord(_integer(root, "assignmentIndex"), _string(root, "recordReference"), _string(root, "positiveExampleReference"), _string(root, "groupReference"), _string(root, "partition"), polarity, _decode_input(_object(root, "input")), target, candidate_conditioning, _sha(root, "logicalDigest"), compatibility_conditioning)


def _decode_input(root: dict[str, Any]) -> TrainerInput:
    _keys(root, {"observedTerm", "normalizedObservedTerm", "canonicalContext", "evidence"}, "INPUT")
    contexts = tuple(_decode_context(value) for value in _list(root, "canonicalContext"))
    _require(tuple(c.rank for c in contexts) == tuple(range(1, len(contexts) + 1)), "CANONICAL_CONTEXT_ORDER_INVALID")
    _require(len({c.canonical_id for c in contexts}) == len(contexts), "DUPLICATE_CANONICAL_CONTEXT")
    evidence = tuple(_decode_evidence(value) for value in _list(root, "evidence"))
    keys = tuple((e.source, e.source_artifact_sha256, e.source_record_identity, e.record_kind, e.retrieval_rank) for e in evidence)
    _require(len(set(keys)) == len(keys), "DUPLICATE_EVIDENCE")
    _require(keys == tuple(sorted(keys)), "EVIDENCE_ORDER_INVALID")
    return TrainerInput(_text(root, "observedTerm"), _text(root, "normalizedObservedTerm"), contexts, evidence)


def _decode_context(value: Any) -> CanonicalContext:
    root = _as_object(value, "CANONICAL_CONTEXT")
    _keys(root, {"rank", "canonicalId", "canonicalName", "fullRecordCanonicalJson"}, "CANONICAL_CONTEXT")
    rank = _integer(root, "rank")
    _require(1 <= rank <= 10, "CANONICAL_CONTEXT_RANK_INVALID")
    canonical_id = _entity(root, "canonicalId")
    name = _text(root, "canonicalName")
    full = root["fullRecordCanonicalJson"]
    _require(full is None or isinstance(full, str), "CANONICAL_RECORD_TYPE_INVALID")
    _require(full is None or full.strip(), "CANONICAL_RECORD_EMPTY")
    _require(full is None or rank <= 3, "CANONICAL_RECORD_RANK_INVALID")
    return CanonicalContext(rank, canonical_id, name, full)


def _decode_evidence(value: Any) -> Evidence:
    root = _as_object(value, "EVIDENCE")
    _keys(root, {"source", "sourceArtifactSha256", "sourceRecordIdentity", "recordKind", "retrievalRank"}, "EVIDENCE")
    source = _string(root, "source")
    _require(source in {"OPEN_FOOD_FACTS", "AGRIBALYSE", "CIQUAL", "GLYCEMIC_INDEX"}, "UNKNOWN_EVIDENCE_SOURCE")
    rank = _integer(root, "retrievalRank")
    _require(1 <= rank <= 10, "EVIDENCE_RANK_INVALID")
    return Evidence(source, _sha(root, "sourceArtifactSha256"), _text(root, "sourceRecordIdentity"), _text(root, "recordKind"), rank)


def _decode_target(root: dict[str, Any]) -> EncodedTarget:
    _keys(root, {"kind", "encodingContractId", "encodingVersion", "objectiveDigest", "objectiveReference", "semanticTarget", "targetKindCode", "lossRoleCodes", "logicalDigest", "targetReference", "positiveTarget", "rejectedTarget", "boundaryType", "boundaryTypeCode", "rejectedTargetLogicalDigest", "rejectedTargetReference"} & set(root), "TARGET")
    kind = _enum_string(root, "kind", {"POSITIVE", "NEGATIVE"}, "UNKNOWN_TARGET_POLARITY")
    digest = _sha(root, "logicalDigest")
    _bound_ref(root, "objectiveReference", "training-objective:v1:", _sha(root, "objectiveDigest"))
    _equals(_string(root, "encodingContractId"), "HIM_TRAINING_TARGET_ENCODING_V1", "TARGET_ENCODING_CONTRACT_MISMATCH")
    _equals(_string(root, "encodingVersion"), "1", "TARGET_ENCODING_VERSION_MISMATCH")
    semantic = _decode_semantic_target(_object(root, "semanticTarget")) if kind == "POSITIVE" else None
    if kind == "POSITIVE":
        _keys(root, {"kind", "encodingContractId", "encodingVersion", "objectiveDigest", "objectiveReference", "semanticTarget", "targetKindCode", "lossRoleCodes", "logicalDigest", "targetReference"}, "POSITIVE_TARGET")
        _require(_integer(root, "targetKindCode") == semantic.kind_code, "TARGET_KIND_CODE_MISMATCH")
        _bound_ref(root, "targetReference", "training-encoded-positive-target:v1:", digest)
        return EncodedTarget(Polarity.POSITIVE, _string(root, "encodingContractId"), _string(root, "encodingVersion"), _sha(root, "objectiveDigest"), _string(root, "objectiveReference"), semantic, _integer(root, "targetKindCode"), _ints(root, "lossRoleCodes"), digest, _string(root, "targetReference"))
    _keys(root, {"kind", "encodingContractId", "encodingVersion", "objectiveDigest", "objectiveReference", "positiveTarget", "rejectedTarget", "boundaryType", "boundaryTypeCode", "lossRoleCodes", "rejectedTargetLogicalDigest", "rejectedTargetReference", "logicalDigest", "targetReference"}, "NEGATIVE_TARGET")
    positive = _decode_target(_object(root, "positiveTarget"))
    rejected = _decode_semantic_target(_object(root, "rejectedTarget"))
    _require(positive.kind is Polarity.POSITIVE, "NEGATIVE_POSITIVE_TARGET_INVALID")
    _require(positive.semantic_target != rejected, "NEGATIVE_TARGET_NOT_DISTINCT")
    boundary_type = NegativeBoundaryType(_enum_string(root, "boundaryType", {value.value for value in NegativeBoundaryType}, "UNKNOWN_NEGATIVE_BOUNDARY"))
    boundary_code = _integer(root, "boundaryTypeCode")
    _require(boundary_code == {
        NegativeBoundaryType.CANONICAL_VS_CHILD_BOUNDARY: 1,
        NegativeBoundaryType.IDENTITY_VS_VARIANT_BOUNDARY: 2,
        NegativeBoundaryType.ALIAS_VS_SEMANTIC_CHILD_BOUNDARY: 3,
        NegativeBoundaryType.WRONG_RELATION_LEVEL: 4,
        NegativeBoundaryType.WRONG_SCOPE: 5,
        NegativeBoundaryType.WRONG_CLASSIFICATION: 6,
        NegativeBoundaryType.CROSS_CANONICAL_CLASSIFICATION_BOUNDARY: 7,
    }[boundary_type], "NEGATIVE_BOUNDARY_CODE_MISMATCH")
    rejected_digest = _sha(root, "rejectedTargetLogicalDigest")
    _bound_ref(root, "rejectedTargetReference", "objective-target:v1:", rejected_digest)
    _bound_ref(root, "targetReference", "training-encoded-negative-target:v1:", digest)
    return EncodedTarget(Polarity.NEGATIVE, _string(root, "encodingContractId"), _string(root, "encodingVersion"), _sha(root, "objectiveDigest"), _string(root, "objectiveReference"), positive.semantic_target, positive.target_kind_code, _ints(root, "lossRoleCodes"), digest, _string(root, "targetReference"), positive, rejected, boundary_type, boundary_code, rejected_digest, _string(root, "rejectedTargetReference"))


def _decode_semantic_target(root: dict[str, Any]) -> SemanticTarget:
    kind = _enum_string(root, "kind", {kind.value for kind in TargetKind}, "UNKNOWN_TARGET_KIND")
    code = _integer(root, "kindCode")
    _require(code == {TargetKind.EXISTING_CANONICAL: 1, TargetKind.IDENTITY: 2, TargetKind.VARIANT: 3, TargetKind.ALIAS: 4, TargetKind.NEW_CANONICAL: 5}[TargetKind(kind)], "TARGET_KIND_CODE_MISMATCH")
    base = {"kind", "kindCode"}
    if kind == "EXISTING_CANONICAL":
        _keys(root, base | {"canonicalId"}, "EXISTING_CANONICAL_TARGET")
        return SemanticTarget(TargetKind(kind), code, canonical_id=_entity(root, "canonicalId"))
    if kind == "IDENTITY":
        _keys(root, base | {"parentCanonicalId"}, "IDENTITY_TARGET")
        return SemanticTarget(TargetKind(kind), code, parent_canonical_id=_entity(root, "parentCanonicalId"))
    if kind == "VARIANT":
        _keys(root, base | {"scope"}, "VARIANT_TARGET")
        return SemanticTarget(TargetKind(kind), code, scope=_decode_family_reference(_object(root, "scope")))
    if kind == "ALIAS":
        _keys(root, base | {"equivalentEntity"}, "ALIAS_TARGET")
        return SemanticTarget(TargetKind(kind), code, equivalent_entity=_decode_family_reference(_object(root, "equivalentEntity")))
    _keys(root, base | {"proposedCanonicalName"}, "NEW_CANONICAL_TARGET")
    name = root["proposedCanonicalName"]
    _require(name is None or (isinstance(name, str) and name.strip()), "NEW_CANONICAL_NAME_INVALID")
    return SemanticTarget(TargetKind(kind), code, proposed_canonical_name=name)


def _decode_family_reference(root: dict[str, Any]) -> FamilyReference:
    kind = _enum_string(root, "referenceType", {"CANONICAL", "IDENTITY"}, "UNKNOWN_SCOPE")
    if kind == "CANONICAL":
        _keys(root, {"referenceType", "canonicalId"}, "CANONICAL_SCOPE")
        return FamilyReference(ScopeKind.CANONICAL, _entity(root, "canonicalId"))
    _keys(root, {"referenceType", "canonicalId", "identityId"}, "IDENTITY_SCOPE")
    return FamilyReference(ScopeKind.IDENTITY, _entity(root, "canonicalId"), _entity(root, "identityId"))


def _decode_conditioning(root: dict[str, Any]) -> CandidateConditioning:
    base = {"contractId", "version", "state", "applicability"}
    _require(set(root) >= base, "CONDITIONING_BASE_FIELDS_MISSING")
    _equals(_string(root, "contractId"), "HIM_EXTERNAL_TRAINER_CANDIDATE_CONDITIONING_PROTOCOL_V1", "CONDITIONING_CONTRACT_MISMATCH")
    _equals(_string(root, "version"), "1", "CONDITIONING_VERSION_MISMATCH")
    _equals(_string(root, "state"), "CANDIDATE_CONDITIONING_TRANSPORTED", "CONDITIONING_STATE_MISMATCH")
    applicability = _enum_string(root, "applicability", {"APPLICABLE", "NOT_APPLICABLE"}, "UNKNOWN_APPLICABILITY")
    if applicability == "NOT_APPLICABLE":
        _keys(root, base, "NOT_APPLICABLE_CONDITIONING")
        return CandidateConditioning(ConditioningApplicability.NOT_APPLICABLE)
    _keys(root, base | {"claims"}, "APPLICABLE_CONDITIONING")
    claims = tuple(_decode_claim(value) for value in _list(root, "claims"))
    _require(1 <= len(claims) <= 2, "CONDITIONING_CLAIM_COUNT_INVALID")
    _require(claims[0].compatibility is CandidateCompatibility.COMPATIBLE, "COMPATIBLE_CLAIM_REQUIRED_FIRST")
    _require(all(claim.compatibility is CandidateCompatibility.REJECT for claim in claims[1:]), "REJECT_CLAIMS_ORDER_INVALID")
    _require(len({claim.conditioning.conditioning_id for claim in claims}) == len(claims), "DUPLICATE_CONDITIONING_ID")
    _require(len({claim.conditioning.primary_input for claim in claims}) == 1, "CONDITIONING_PRIMARY_INPUT_MISMATCH")
    return CandidateConditioning(ConditioningApplicability.APPLICABLE, claims)


def _decode_claim(value: Any) -> CandidateClaim:
    root = _as_object(value, "CLAIM")
    _keys(root, {"conditioning", "compatibility"}, "CLAIM")
    compatibility = CandidateCompatibility(_enum_string(root, "compatibility", {"COMPATIBLE", "REJECT"}, "UNKNOWN_COMPATIBILITY"))
    return CandidateClaim(_decode_conditioning_input(_object(root, "conditioning")), compatibility)


def _decode_conditioning_input(root: dict[str, Any]) -> CandidateConditioningInput:
    _keys(root, {"contractId", "version", "state", "primaryInput", "proposedTargetKind", "candidateTerm", "normalizedCandidateTerm", "relation", "conditioningId", "conditioningDigest"}, "CONDITIONING_INPUT")
    _equals(_string(root, "contractId"), "HIM_CANDIDATE_CONDITIONING_INPUT_AUTHORITY_V1", "CONDITIONING_INPUT_CONTRACT_MISMATCH")
    _equals(_string(root, "version"), "1", "CONDITIONING_INPUT_VERSION_MISMATCH")
    _equals(_string(root, "state"), "CANDIDATE_CONDITIONING_INPUT_CONTEXT_ONLY", "CONDITIONING_INPUT_STATE_MISMATCH")
    target_kind = TargetKind(_enum_string(root, "proposedTargetKind", {kind.value for kind in TargetKind if kind is not TargetKind.NEW_CANONICAL}, "UNKNOWN_TARGET_KIND"))
    digest = _sha(root, "conditioningDigest")
    _bound_ref(root, "conditioningId", "candidate-conditioning:v1:", digest)
    return CandidateConditioningInput(_string(root, "contractId"), _string(root, "version"), _string(root, "state"), _decode_primary(_object(root, "primaryInput")), target_kind, _text(root, "candidateTerm"), _text(root, "normalizedCandidateTerm"), _decode_relation(_object(root, "relation")), _string(root, "conditioningId"), digest)


def _decode_compatibility_conditioning(root: dict[str, Any]) -> CompatibilityConditioning:
    base = {"contractId", "version", "state", "applicability"}
    _require(set(root) >= base, "COMPATIBILITY_CONDITIONING_BASE_FIELDS_MISSING")
    _equals(_string(root, "contractId"), "HIM_EXTERNAL_TRAINER_COMPATIBILITY_CONDITIONING_PROTOCOL_V1", "COMPATIBILITY_CONDITIONING_CONTRACT_MISMATCH")
    _equals(_string(root, "version"), "1", "COMPATIBILITY_CONDITIONING_VERSION_MISMATCH")
    _equals(_string(root, "state"), "COMPATIBILITY_CONDITIONING_TRANSPORTED", "COMPATIBILITY_CONDITIONING_STATE_MISMATCH")
    applicability = _enum_string(root, "applicability", {"APPLICABLE", "NOT_APPLICABLE"}, "UNKNOWN_APPLICABILITY")
    if applicability == "NOT_APPLICABLE":
        _keys(root, base, "NOT_APPLICABLE_COMPATIBILITY_CONDITIONING")
        return CompatibilityConditioning(ConditioningApplicability.NOT_APPLICABLE)
    _keys(root, base | {"claims"}, "APPLICABLE_COMPATIBILITY_CONDITIONING")
    claims = tuple(_decode_compatibility_claim(value) for value in _list(root, "claims"))
    _require(1 <= len(claims) <= 2, "COMPATIBILITY_CONDITIONING_CLAIM_COUNT_INVALID")
    _require(claims[0].compatibility is CandidateCompatibility.COMPATIBLE, "COMPATIBLE_CLAIM_REQUIRED_FIRST")
    _require(all(claim.compatibility is CandidateCompatibility.REJECT for claim in claims[1:]), "REJECT_CLAIMS_ORDER_INVALID")
    _require(len({claim.conditioning.conditioning_id for claim in claims}) == len(claims), "DUPLICATE_CONDITIONING_ID")
    _require(len({claim.conditioning.primary_input for claim in claims}) == 1, "CONDITIONING_PRIMARY_INPUT_MISMATCH")
    return CompatibilityConditioning(ConditioningApplicability.APPLICABLE, claims)


def _decode_compatibility_claim(value: Any) -> CompatibilityClaim:
    root = _as_object(value, "COMPATIBILITY_CLAIM")
    _keys(root, {"conditioning", "compatibility"}, "COMPATIBILITY_CLAIM")
    return CompatibilityClaim(
        _decode_compatibility_conditioning_input(_object(root, "conditioning")),
        CandidateCompatibility(_enum_string(root, "compatibility", {"COMPATIBLE", "REJECT"}, "UNKNOWN_COMPATIBILITY")),
    )


def _decode_compatibility_conditioning_input(root: dict[str, Any]) -> CompatibilityConditioningInput:
    common = {"contractId", "version", "state", "primaryInput", "proposedTargetKind", "subjectKind", "term", "normalizedTerm", "conditioningId", "conditioningDigest"}
    _require(set(root) >= common, "COMPATIBILITY_CONDITIONING_INPUT_FIELDS_MISSING")
    _equals(_string(root, "contractId"), "HIM_EXTERNAL_TRAINER_COMPATIBILITY_CONDITIONING_PROTOCOL_V1", "COMPATIBILITY_INPUT_CONTRACT_MISMATCH")
    _equals(_string(root, "version"), "1", "COMPATIBILITY_INPUT_VERSION_MISMATCH")
    _equals(_string(root, "state"), "COMPATIBILITY_CONDITIONING_INPUT_CONTEXT_ONLY", "COMPATIBILITY_INPUT_STATE_MISMATCH")
    subject_kind = CompatibilitySubjectKind(_enum_string(root, "subjectKind", {kind.value for kind in CompatibilitySubjectKind}, "UNKNOWN_SUBJECT_KIND"))
    if subject_kind is CompatibilitySubjectKind.CANDIDATE:
        _keys(root, common | {"relation"}, "CANDIDATE_COMPATIBILITY_INPUT")
        relation = _decode_relation(_object(root, "relation"))
    else:
        _keys(root, common, "EXISTING_CANONICAL_COMPATIBILITY_INPUT")
        relation = None
        _equals(_string(root, "proposedTargetKind"), TargetKind.EXISTING_CANONICAL.value, "EXISTING_CANONICAL_TARGET_KIND_MISMATCH")
    digest = _sha(root, "conditioningDigest")
    _bound_ref(root, "conditioningId", "compatibility-conditioning:v1:", digest)
    return CompatibilityConditioningInput(
        _string(root, "contractId"), _string(root, "version"), _string(root, "state"),
        _decode_primary(_object(root, "primaryInput")),
        TargetKind(_enum_string(root, "proposedTargetKind", {kind.value for kind in TargetKind if kind is not TargetKind.NEW_CANONICAL}, "UNKNOWN_TARGET_KIND")),
        subject_kind, _text(root, "term"), _text(root, "normalizedTerm"), relation,
        _string(root, "conditioningId"), digest,
    )


def _decode_primary(root: dict[str, Any]) -> PrimaryInput:
    _keys(root, {"observedTerm", "normalizedObservedTerm"}, "PRIMARY_INPUT")
    return PrimaryInput(_text(root, "observedTerm"), _text(root, "normalizedObservedTerm"))


def _decode_relation(root: dict[str, Any]) -> Relation:
    kind = _enum_string(root, "kind", {kind.value for kind in RelationKind}, "UNKNOWN_RELATION")
    if kind == "IDENTITY":
        _keys(root, {"kind", "parentCanonicalId"}, "IDENTITY_RELATION")
        return Relation(RelationKind.IDENTITY, parent_canonical_id=_entity(root, "parentCanonicalId"))
    if kind == "VARIANT":
        _keys(root, {"kind", "scope"}, "VARIANT_RELATION")
        return Relation(RelationKind.VARIANT, scope=_decode_wire_family_reference(_object(root, "scope")))
    if kind == "ALIAS":
        _keys(root, {"kind", "equivalentEntity"}, "ALIAS_RELATION")
        return Relation(RelationKind.ALIAS, equivalent_entity=_decode_wire_family_reference(_object(root, "equivalentEntity")))
    _keys(root, {"kind"}, "CREATE_NEW_CANONICAL_RELATION")
    return Relation(RelationKind.CREATE_NEW_CANONICAL)


def _decode_wire_family_reference(root: dict[str, Any]) -> FamilyReference:
    kind = _enum_string(root, "kind", {"CANONICAL", "IDENTITY"}, "UNKNOWN_SCOPE")
    if kind == "CANONICAL":
        _keys(root, {"kind", "canonicalId"}, "CANONICAL_SCOPE")
        return FamilyReference(ScopeKind.CANONICAL, _entity(root, "canonicalId"))
    _keys(root, {"kind", "canonicalId", "identityId"}, "IDENTITY_SCOPE")
    return FamilyReference(ScopeKind.IDENTITY, _entity(root, "canonicalId"), _entity(root, "identityId"))


def _parse_json(payload: str | bytes) -> dict[str, Any]:
    if not isinstance(payload, (str, bytes)):
        raise HimTrainerProtocolV1Error("REQUEST_JSON_TYPE_INVALID")
    try:
        value = json.loads(payload, object_pairs_hook=_unique_object, parse_constant=_reject_constant)
    except HimTrainerProtocolV1Error:
        raise
    except (UnicodeDecodeError, json.JSONDecodeError) as error:
        raise HimTrainerProtocolV1Error("REQUEST_JSON_INVALID") from error
    return _as_object(value, "REQUEST")


def _unique_object(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for key, value in pairs:
        if key in result:
            raise HimTrainerProtocolV1Error("DUPLICATE_JSON_FIELD")
        result[key] = value
    return result


def _reject_constant(value: str) -> None:
    raise HimTrainerProtocolV1Error("NON_FINITE_JSON")


def _as_object(value: Any, name: str) -> dict[str, Any]:
    if not isinstance(value, dict):
        raise HimTrainerProtocolV1Error(f"{name}_OBJECT_REQUIRED")
    return value


def _object(root: Mapping[str, Any], name: str) -> dict[str, Any]:
    return _as_object(_required(root, name), name)


def _list(root: Mapping[str, Any], name: str) -> list[Any]:
    value = _required(root, name)
    if not isinstance(value, list):
        raise HimTrainerProtocolV1Error(f"{name}_ARRAY_REQUIRED")
    return value


def _string(root: Mapping[str, Any], name: str) -> str:
    value = _required(root, name)
    if not isinstance(value, str):
        raise HimTrainerProtocolV1Error(f"{name}_STRING_REQUIRED")
    return value


def _text(root: Mapping[str, Any], name: str) -> str:
    value = _string(root, name)
    if not value.strip() or any(ord(char) < 32 for char in value):
        raise HimTrainerProtocolV1Error(f"{name}_TEXT_INVALID")
    return value


def _integer(root: Mapping[str, Any], name: str) -> int:
    value = _required(root, name)
    if isinstance(value, bool) or not isinstance(value, int):
        raise HimTrainerProtocolV1Error(f"{name}_INTEGER_REQUIRED")
    return value


def _boolean(root: Mapping[str, Any], name: str) -> bool:
    value = _required(root, name)
    if not isinstance(value, bool):
        raise HimTrainerProtocolV1Error(f"{name}_BOOLEAN_REQUIRED")
    return value


def _sha(root: Mapping[str, Any], name: str) -> str:
    value = _string(root, name)
    if not _DIGEST.fullmatch(value):
        raise HimTrainerProtocolV1Error(f"{name}_DIGEST_INVALID")
    return value


def _entity(root: Mapping[str, Any], name: str) -> str:
    value = _string(root, name)
    if not _ENTITY_ID.fullmatch(value):
        raise HimTrainerProtocolV1Error(f"{name}_ENTITY_INVALID")
    return value


def _ints(root: Mapping[str, Any], name: str) -> tuple[int, ...]:
    values = _list(root, name)
    if any(isinstance(value, bool) or not isinstance(value, int) for value in values):
        raise HimTrainerProtocolV1Error(f"{name}_INTEGER_ARRAY_INVALID")
    return tuple(values)


def _enum_string(root: Mapping[str, Any], name: str, values: set[str], error: str) -> str:
    value = _string(root, name)
    if value not in values:
        raise HimTrainerProtocolV1Error(error)
    return value


def _bound_ref(root: Mapping[str, Any], name: str, prefix: str, digest: str) -> None:
    if _string(root, name) != f"{prefix}{digest}":
        raise HimTrainerProtocolV1Error(f"{name}_REFERENCE_MISMATCH")


def _reference_format(root: Mapping[str, Any], name: str, prefix: str) -> None:
    value = _string(root, name)
    if not value.startswith(prefix) or not _DIGEST.fullmatch(value[len(prefix):]):
        raise HimTrainerProtocolV1Error(f"{name}_REFERENCE_MISMATCH")


def _required(root: Mapping[str, Any], name: str) -> Any:
    if name not in root:
        raise HimTrainerProtocolV1Error(f"{name}_MISSING")
    return root[name]


def _keys(root: Mapping[str, Any], expected: set[str], name: str) -> None:
    if set(root) != expected:
        raise HimTrainerProtocolV1Error(f"{name}_FIELDS_INVALID")


def _equals(actual: Any, expected: Any, error: str) -> None:
    if actual != expected:
        raise HimTrainerProtocolV1Error(error)


def _require(condition: Any, error: str) -> None:
    if not condition:
        raise HimTrainerProtocolV1Error(error)
