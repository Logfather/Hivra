"""Build-only proof that the packaged protocol accepts the CUDA request shape."""

from __future__ import annotations

import json

import torch

from him_trainer.point13_forward_rng_contract_v1 import (
    build_him_train_forward_rng_contract_v1,
    forward_rng_contract_wire_v1,
)
from him_trainer.point13_loss_contract_v1 import build_him_masked_multi_objective_loss_contract_v1
from him_trainer.point13_optimizer_execution_policy_v1 import (
    build_him_optimizer_execution_policy_v1,
    optimizer_execution_policy_wire_v1,
)
from him_trainer.point13_trainability_policy_v1 import (
    build_him_base_encoder_trainability_policy_v1,
    trainability_policy_wire_v1,
)
from him_trainer.protocol_v1 import (
    decode_external_trainer_request_v1,
    head_initialization_binding_logical_digest_v1,
    model_execution_binding_logical_digest_v1,
)


def digest(letter: str) -> str:
    return letter * 64


def reference(prefix: str, value: str) -> str:
    return f"{prefix}{value}"


def authority_wire(contract: object) -> dict[str, object]:
    return {
        **contract.identity_payload(),
        "logicalDigest": contract.logical_digest,
        "reference": contract.reference,
    }


def build_request() -> dict[str, object]:
    model_digest = digest("a")
    head_digest = digest("b")
    full_state_digest = digest("c")
    head_reference = reference("him-head-architecture:v1:", head_digest)
    initialization_digest = head_initialization_binding_logical_digest_v1(
        "NORMAL_ZERO_BIAS", "0", "0.02", "0", 7,
        ("PRIMARY", "SECONDARY"), "ISOLATED_TORCH_GENERATOR_CPU_V1",
    )
    initialization_reference = reference("him-head-initialization:v1:", initialization_digest)
    full_state_reference = reference("him-full-initial-state:v1:", full_state_digest)
    execution_digest = model_execution_binding_logical_digest_v1(
        model_digest, head_digest, head_reference, initialization_digest,
        initialization_reference, full_state_digest, full_state_reference,
    )
    loss = build_him_masked_multi_objective_loss_contract_v1()
    forward_rng = build_him_train_forward_rng_contract_v1(7, device="cuda:0")
    trainability = build_him_base_encoder_trainability_policy_v1()
    optimizer = build_him_optimizer_execution_policy_v1(device_policy="CUDA")
    objective_digest = digest("d")
    record = {
        "assignmentIndex": 0,
        "recordReference": "record:v1:cuda-build-gate",
        "positiveExampleReference": reference("example:v1:", digest("e")),
        "groupReference": "group:v1:cuda-build-gate",
        "partition": "TRAIN_ONLY",
        "polarity": "POSITIVE",
        "input": {
            "observedTerm": "CUDA build gate",
            "normalizedObservedTerm": "cuda build gate",
            "canonicalContext": [{
                "rank": 1, "canonicalId": "AbCd12", "canonicalName": "Build gate",
                "fullRecordCanonicalJson": None,
            }],
            "evidence": [{
                "source": "CIQUAL", "sourceArtifactSha256": digest("f"),
                "sourceRecordIdentity": "ciqual:build-gate", "recordKind": "FOOD", "retrievalRank": 1,
            }],
        },
        "encodedTarget": {
            "kind": "POSITIVE",
            "encodingContractId": "HIM_TRAINING_TARGET_ENCODING_V1",
            "encodingVersion": "1",
            "objectiveDigest": objective_digest,
            "objectiveReference": reference("training-objective:v1:", objective_digest),
            "semanticTarget": {"kind": "EXISTING_CANONICAL", "kindCode": 1, "canonicalId": "AbCd12"},
            "targetKindCode": 1, "lossRoleCodes": [1, 2], "logicalDigest": digest("1"),
            "targetReference": reference("training-encoded-positive-target:v1:", digest("1")),
        },
        "candidateConditioning": {
            "contractId": "HIM_EXTERNAL_TRAINER_CANDIDATE_CONDITIONING_PROTOCOL_V1",
            "version": "1", "state": "CANDIDATE_CONDITIONING_TRANSPORTED", "applicability": "NOT_APPLICABLE",
        },
        "logicalDigest": digest("2"),
    }
    return {
        "contractId": "HIM_EXTERNAL_TRAINER_PROCESS_REQUEST_SERIALIZATION_V1",
        "version": "1", "state": "EXTERNAL_TRAINER_PROCESS_REQUEST_SERIALIZED",
        "formatId": "him-external-trainer-process-request:v1",
        "processBindingReference": reference("external-trainer-process-binding:v1:", digest("3")),
        "processBindingLogicalDigest": digest("3"),
        "runtimeBindingReference": reference("external-trainer-runtime-binding:v1:", digest("4")),
        "runtimeBindingLogicalDigest": digest("4"),
        "executionRequestReference": reference("external-trainer-execution-request:v1:", digest("5")),
        "executionRequestLogicalDigest": digest("5"),
        "trainer": {
            "module": "him_trainer", "implementationFingerprint": digest("6"),
            "implementationReference": reference("trainer-implementation:v1:", digest("6")),
            "entrypointMode": "PYTHON_MODULE", "processAdapterImplementationFingerprint": digest("7"),
        },
        "device": "CUDA", "deviceIndex": 0, "launcher": "UV",
        "workingDirectory": "training/him", "requestTransport": "IMMUTABLE_TEMP_FILE",
        "policies": {
            "launcher": "UV", "requestTransport": "IMMUTABLE_TEMP_FILE", "networkAccess": "FORBIDDEN",
            "environmentPolicy": "ALLOWLIST_ONLY", "stdinInteractive": "NO",
            "stdoutPolicy": "MACHINE_RESULT_PROTOCOL_ONLY", "stderrPolicy": "BOUNDED_DIAGNOSTICS",
            "outputBoundsRequired": "YES", "timeoutPolicyRequired": "YES", "cancellationPolicyRequired": "YES",
            "processExecutionPolicyFingerprint": digest("8"),
            "requestSerializationContractId": "HIM_EXTERNAL_TRAINER_PROCESS_REQUEST_SERIALIZATION_V1",
            "requestSerializationVersion": "1", "resultProtocolContractId": "HIM_EXTERNAL_TRAINER_PROCESS_RESULT_V1",
            "resultProtocolVersion": "1", "artifactReverification": "YES", "trainerReverification": "YES",
        },
        "runtimeEnvironment": {
            "reference": reference("python-pytorch-runtime-environment:v1:", digest("9")),
            "logicalDigest": digest("9"), "runtimeChannel": "STABLE", "pythonVersion": "3.13.14",
            "pythonImplementation": "CPython", "pytorchVersion": "2.14.0",
        },
        "artifacts": [
            {"role": "BASE_MODEL", "expectedSha256": digest("a"), "verifiedActualSha256": digest("a"), "operationalPath": "models/base", "relativePath": "models/base", "byteSize": 1},
            {"role": "TOKENIZER", "expectedSha256": digest("b"), "verifiedActualSha256": digest("b"), "operationalPath": "models/tokenizer", "relativePath": "models/tokenizer", "byteSize": 1},
            {"role": "MODEL_CONFIGURATION", "expectedSha256": digest("c"), "verifiedActualSha256": digest("c"), "operationalPath": "models/config", "relativePath": "models/config", "byteSize": 1},
        ],
        "trainingRequest": {
            "protocolReference": reference("trainer-protocol:v1:", digest("d")), "protocolLogicalDigest": digest("d"),
            "trainerPortRequestReference": reference("trainer-request:v1:", digest("e")), "trainerPortRequestLogicalDigest": digest("e"),
            "trainingMissionReference": "training-mission:v1:cuda-build-gate", "trainingMissionLogicalDigest": digest("f"),
            "objectiveReference": reference("training-objective:v1:", objective_digest), "objectiveLogicalDigest": objective_digest,
            "targetEncodingReference": reference("training-target-encoding:v1:", digest("1")), "targetEncodingLogicalDigest": digest("1"),
            "implementationFingerprint": digest("2"), "partition": "TRAIN_ONLY",
            "configuration": {
                "seed": 7, "epochs": 1, "microBatchSize": 1, "gradientAccumulationSteps": 1,
                "learningRate": "0.0001", "optimizerId": "optimizer:adamw:v1", "logicalDigest": digest("3"),
                "configurationReference": reference("training-configuration:v1:", digest("3")),
            },
            "modelBinding": {
                "modelFamilyId": "xlm-roberta", "baseModelId": "xlm-roberta-base",
                "baseModelArtifactDigest": digest("a"), "tokenizerId": "xlm-roberta-tokenizer",
                "tokenizerArtifactDigest": digest("b"), "modelConfigurationArtifactDigest": digest("c"),
                "logicalDigest": model_digest, "modelBindingReference": reference("model-binding:v1:", model_digest),
            },
            "modelExecutionBinding": {
                "contractId": "HIM_MODEL_EXECUTION_BINDING_V1", "version": "1",
                "baseModelBindingLogicalDigest": model_digest, "headContractLogicalDigest": head_digest,
                "headContractReference": head_reference,
                "headInitializationBinding": {
                    "contractId": "HIM_HEAD_INITIALIZATION_BINDING_V1", "version": "1",
                    "initializationScheme": "NORMAL_ZERO_BIAS", "mean": "0", "std": "0.02", "bias": "0",
                    "seed": 7, "initializationOrder": ["PRIMARY", "SECONDARY"],
                    "rng": "ISOLATED_TORCH_GENERATOR_CPU_V1", "logicalDigest": initialization_digest,
                    "reference": initialization_reference,
                },
                "fullInitialStateLogicalDigest": full_state_digest, "fullInitialStateReference": full_state_reference,
                "logicalDigest": execution_digest,
                "reference": reference("him-model-execution-binding:v1:", execution_digest),
            },
            "forwardRngAuthority": forward_rng_contract_wire_v1(forward_rng),
            "lossAuthority": authority_wire(loss),
            "trainabilityPolicy": trainability_policy_wire_v1(trainability),
            "optimizerExecutionPolicy": optimizer_execution_policy_wire_v1(optimizer),
            "adamW": {
                "optimizerId": optimizer.optimizer_id, "algorithm": optimizer.optimizer_type,
                "learningRate": optimizer.learning_rate, "beta1": optimizer.beta1, "beta2": optimizer.beta2,
                "epsilon": optimizer.epsilon, "weightDecay": optimizer.weight_decay,
                "decoupledWeightDecay": optimizer.decoupled_weight_decay, "biasCorrection": optimizer.bias_correction,
            },
            "records": [record],
        },
        "contentSha256": digest("4"),
    }


request = decode_external_trainer_request_v1(
    json.dumps(build_request(), ensure_ascii=False, separators=(",", ":")),
)
assert request.device == "CUDA"
assert request.device_index == 0
assert torch.device("cuda", request.device_index).type == "cuda"
assert torch.device("cuda", request.device_index).index == 0
print("HIM_CUDA_PROTOCOL_PARSE_GATE=PASS")
