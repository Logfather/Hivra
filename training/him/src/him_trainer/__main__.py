"""Protocol-only entrypoint for the bounded HIM external trainer process."""

from __future__ import annotations

import hashlib
import json
import sys
from pathlib import Path

from .protocol_v1 import (
    ExternalTrainerRequest,
    HimTrainerProtocolV1Error,
    decode_durable_training_request_manifest_v1,
)


_LIVENESS_SIGNAL_REFERENCE_PREFIX = "trainer-liveness-signal:v1:"
_REQUEST_SERIALIZATION_REFERENCE_PREFIX = "external-trainer-process-request-serialization:v1:"
_RESULT_REFERENCE_PREFIX = "external-trainer-process-result:v1:"
_RESULT_SERIALIZATION_REFERENCE_PREFIX = "external-trainer-process-result-serialization:v1:"
_DIAGNOSTICS_DIGEST = hashlib.sha256(
    b"him-trainer-protocol-only-no-training-result-v1",
).hexdigest()


def _sha256(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def _json(value: object) -> str:
    return json.dumps(value, ensure_ascii=False, separators=(",", ":"), allow_nan=False)


def _field(key: str, value: str) -> str:
    return f"{key}={len(value)}:{value}\n"


def _liveness_signal(request: ExternalTrainerRequest) -> str:
    process_reference = request.process_binding_reference
    process_digest = request.process_binding_logical_digest
    request_content_digest = request.content_sha256
    request_reference = f"{_REQUEST_SERIALIZATION_REFERENCE_PREFIX}{request_content_digest}"
    payload = {
        "contractId": "HIM_TRAINER_LIVENESS_SIGNAL_V1",
        "version": "1",
        "state": "TRAINER_LIVENESS_SIGNAL_DEFINED",
        "formatId": "him-trainer-liveness-signal:v1",
        "signalKind": "ALIVE",
        "processBindingReference": process_reference,
        "processBindingLogicalDigest": process_digest,
        "requestSerializationReference": request_reference,
        "requestSerializationContentSha256": request_content_digest,
        "sequence": 0,
    }
    content_digest = _sha256(_json(payload).encode("utf-8"))
    logical_input = "".join(
        (
            _field("contract", "HIM_TRAINER_LIVENESS_SIGNAL_V1"),
            _field("version", "1"),
            _field("state", "TRAINER_LIVENESS_SIGNAL_DEFINED"),
            _field("format", "him-trainer-liveness-signal:v1"),
            _field("signal-kind", "ALIVE"),
            _field("process-binding-reference", process_reference),
            _field("process-binding-digest", process_digest),
            _field("request-serialization-reference", request_reference),
            _field("request-serialization-content-sha256", request_content_digest),
            _field("sequence", "0"),
            _field("content-sha256", content_digest),
        ),
    )
    logical_digest = _sha256(logical_input.encode("utf-8"))
    return _json(
        {
            "contractId": "HIM_TRAINER_LIVENESS_SIGNAL_V1",
            "version": "1",
            "state": "TRAINER_LIVENESS_SIGNAL_DEFINED",
            "formatId": "him-trainer-liveness-signal:v1",
            "signalReference": f"{_LIVENESS_SIGNAL_REFERENCE_PREFIX}{logical_digest}",
            "signalLogicalDigest": logical_digest,
            "contentSha256": content_digest,
            "payload": payload,
        },
    )


def _result(request: ExternalTrainerRequest) -> str:
    runtime_reference = request.runtime_binding_reference
    runtime_digest = request.runtime_binding_logical_digest
    execution_reference = request.execution_request_reference
    execution_digest = request.execution_request_logical_digest
    training = request.training_request
    trainer = request.trainer
    runtime_environment = request.runtime_environment
    request_content_digest = request.content_sha256
    request_reference = f"{_REQUEST_SERIALIZATION_REFERENCE_PREFIX}{request_content_digest}"
    process_reference = request.process_binding_reference
    process_digest = request.process_binding_logical_digest
    process_status = "COMPLETED"
    process_exit_code = 0
    training_outcome = "NO_TRAINING_RESULT"

    logical_input = "".join(
        (
            _field("contract", "HIM_EXTERNAL_TRAINER_PROCESS_RESULT_V1"),
            _field("version", "1"),
            _field("state", "EXTERNAL_TRAINER_PROCESS_RESULT_REPORTED"),
            _field("process-binding-digest", process_digest),
            _field("process-binding-reference", process_reference),
            _field("request-serialization-digest", request_content_digest),
            _field("request-serialization-reference", request_reference),
            _field("runtime-binding-digest", runtime_digest),
            _field("runtime-binding-reference", runtime_reference),
            _field("runtime-environment-digest", runtime_environment.logical_digest),
            _field("runtime-environment-reference", runtime_environment.reference),
            _field("execution-request-digest", execution_digest),
            _field("execution-request-reference", execution_reference),
            _field("training-mission-digest", training.training_mission_logical_digest),
            _field("training-mission-reference", training.training_mission_reference),
            _field("training-configuration-digest", training.configuration.logical_digest),
            _field("training-configuration-reference", training.configuration.configuration_reference),
            _field("model-binding-digest", training.model_binding.logical_digest),
            _field("model-binding-reference", training.model_binding.model_binding_reference),
            _field("trainer-module", trainer.module),
            _field("trainer-fingerprint", trainer.implementation_fingerprint),
            _field("trainer-reference", trainer.implementation_reference),
            _field("adapter-fingerprint", trainer.process_adapter_implementation_fingerprint),
            _field("device", request.device),
            _field("process-status", process_status),
            _field("process-exit-code", str(process_exit_code)),
            _field("training-outcome", training_outcome),
            _field("failure-reason", "NONE"),
            _field("diagnostics-digest", _DIAGNOSTICS_DIGEST),
            _field("artifact-count", "0"),
        ),
    )
    result_digest = _sha256(logical_input.encode("utf-8"))
    result_reference = f"{_RESULT_REFERENCE_PREFIX}{result_digest}"
    payload = {
        "processResultReference": result_reference,
        "processResultLogicalDigest": result_digest,
        "processBindingReference": process_reference,
        "processBindingLogicalDigest": process_digest,
        "requestSerializationReference": request_reference,
        "requestSerializationLogicalDigest": request_content_digest,
        "runtimeBindingReference": runtime_reference,
        "runtimeBindingLogicalDigest": runtime_digest,
        "runtimeEnvironmentReference": runtime_environment.reference,
        "runtimeEnvironmentLogicalDigest": runtime_environment.logical_digest,
        "executionRequestReference": execution_reference,
        "executionRequestLogicalDigest": execution_digest,
        "trainingMissionReference": training.training_mission_reference,
        "trainingMissionLogicalDigest": training.training_mission_logical_digest,
        "trainingConfigurationReference": training.configuration.configuration_reference,
        "trainingConfigurationLogicalDigest": training.configuration.logical_digest,
        "modelBindingReference": training.model_binding.model_binding_reference,
        "modelBindingLogicalDigest": training.model_binding.logical_digest,
        "trainerModule": trainer.module,
        "trainerImplementationFingerprint": trainer.implementation_fingerprint,
        "trainerImplementationReference": trainer.implementation_reference,
        "processAdapterImplementationFingerprint": trainer.process_adapter_implementation_fingerprint,
        "device": request.device,
        "processStatus": process_status,
        "processExitCode": process_exit_code,
        "trainingOutcome": training_outcome,
        "failureReason": None,
        "diagnosticsDigest": _DIAGNOSTICS_DIGEST,
        "outputArtifacts": [],
    }
    payload_json = _json(payload)
    content_digest = _sha256(payload_json.encode("utf-8"))
    serialization_input = "".join(
        (
            _field("contract", "HIM_EXTERNAL_TRAINER_PROCESS_RESULT_SERIALIZATION_V1"),
            _field("version", "1"),
            _field("state", "EXTERNAL_TRAINER_PROCESS_RESULT_SERIALIZED"),
            _field("format", "him-external-trainer-process-result:v1"),
            _field("process-result-digest", result_digest),
            _field("process-result-reference", result_reference),
            _field("content-sha256", content_digest),
        ),
    )
    serialization_digest = _sha256(serialization_input.encode("utf-8"))
    return _json(
        {
            "contractId": "HIM_EXTERNAL_TRAINER_PROCESS_RESULT_SERIALIZATION_V1",
            "version": "1",
            "state": "EXTERNAL_TRAINER_PROCESS_RESULT_SERIALIZED",
            "formatId": "him-external-trainer-process-result:v1",
            "serializationReference": f"{_RESULT_SERIALIZATION_REFERENCE_PREFIX}{serialization_digest}",
            "serializationLogicalDigest": serialization_digest,
            "contentSha256": content_digest,
            "payload": payload,
        },
    )


def _request_manifest_argument(arguments: list[str]) -> Path:
    if len(arguments) != 2 or arguments[0] != "--request-manifest" or not arguments[1]:
        raise ValueError("REQUEST_MANIFEST_ARGUMENTS_INVALID")
    return Path(arguments[1])


def run(arguments: list[str] | None = None) -> int:
    raw_arguments = list(sys.argv[1:] if arguments is None else arguments)
    if raw_arguments and raw_arguments[0] == "--mode":
        mode = raw_arguments[1] if len(raw_arguments) > 1 else ""
        if mode == "P2_PRODUCTIVE_TRAINING_V1":
            from .productive_training_p2_real import run_cli
        else:
            from .productive_training_v2 import run_cli

        return run_cli(raw_arguments)
    try:
        manifest = _request_manifest_argument(raw_arguments)
        if manifest.is_symlink() or not manifest.is_file():
            raise ValueError("REQUEST_MANIFEST_INVALID")
        raw = manifest.read_bytes()
        manifest_request = decode_durable_training_request_manifest_v1(raw)
        request = manifest_request.payload
        sys.stdout.write(_liveness_signal(request))
        sys.stdout.write("\n")
        sys.stdout.write(_result(request))
        sys.stdout.write("\n")
        sys.stdout.flush()
        return 0
    except (HimTrainerProtocolV1Error, OSError, UnicodeError, ValueError, json.JSONDecodeError) as error:
        sys.stderr.write(f"{type(error).__name__}: {error}\n")
        sys.stderr.flush()
        return 1


if __name__ == "__main__":
    raise SystemExit(run())
