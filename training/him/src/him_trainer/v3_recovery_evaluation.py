"""Explicit checkpoint-only V3 Development recovery path."""
from __future__ import annotations
import hashlib, json
from pathlib import Path
from typing import Any, Mapping
from .checkpoint_v2 import reload_checkpoint
from .productive_development_v3 import evaluate_v3_development

RECOVERY_SCHEMA = "HIM_V3_RECOVERY_DEVELOPMENT_RESULT_V1"

def recover_v3_development(*, checkpoint_manifest: str | Path, model: Any,
                            optimizer: Any, expected_bindings: Mapping[str, Any],
                            expected_optimizer_step: int, authority_path: str | Path,
                            tokenizer: Any, output_path: str | Path,
                            original_run_id: str, runtime_identity: Mapping[str, Any]) -> dict[str, Any]:
    """Reload a preserved checkpoint and publish one separate evaluation result."""
    out = Path(output_path)
    if out.exists():
        raise FileExistsError("RECOVERY_EVALUATION_ALREADY_PUBLISHED")
    validation = reload_checkpoint(checkpoint_manifest, model=model, optimizer=optimizer,
                                   expected_bindings=expected_bindings,
                                   expected_optimizer_step=expected_optimizer_step)
    result = evaluate_v3_development(authority_path=authority_path, model=model,
                                      checkpoint={"model": model, "optimizer": optimizer},
                                      tokenizer=tokenizer)
    body = {"schema": RECOVERY_SCHEMA, "originalRunId": original_run_id,
            "checkpointManifest": str(checkpoint_manifest),
            "checkpointManifestSha256": hashlib.sha256(Path(checkpoint_manifest).read_bytes()).hexdigest(),
            "runtimeIdentity": dict(runtime_identity), "developmentAuthority": str(authority_path),
            "serializer": "him_trainer.input_representation_v3.serialize_contextual_input_v3",
            "predictionAdapter": "HIM_V3_PREDICTION_ADAPTER_V1", "checkpointReload": {
                "modelStateEquivalent": validation.model_state_equivalent,
                "optimizerStateEquivalent": validation.optimizer_state_equivalent,
                "stepEquivalent": validation.step_equivalent,
                "authorityBindingsEquivalent": validation.authority_bindings_equivalent,
            },
            "result": result}
    canonical = json.dumps(body, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode()
    body["resultDigest"] = hashlib.sha256(canonical).hexdigest()
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(json.dumps(body, ensure_ascii=False, sort_keys=True, indent=2).encode() + b"\n")
    return body

__all__ = ["recover_v3_development", "RECOVERY_SCHEMA"]
