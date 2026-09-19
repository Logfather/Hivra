"""Versioned, non-inference binding for the productive HIM V2 checkpoint.

The historical P2 evaluator remains unchanged.  This module only adapts its
authority/input semantics to the completed productive checkpoint and exposes
fail-closed checks that run before the first model forward.
"""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
from typing import Any, Mapping

from him_trainer import expanded_validation_p2 as historical

AUTHORITY_VERSION = "HIM_P2_EXPANDED_VALIDATION_PRODUCTIVE_V2"
CHECKPOINT_REFERENCE = "him-training-checkpoint:v2:229444ee6b1c0fd29956ca9d2f68d879ecb1b9e10ce01b849aa37bfcc1906c88"
CHECKPOINT_DIGEST = "ba5e3b773ccf4b0a49066f8f738426ca2ea72d57f343636c274b8c5164befc8f"
RUNTIME_AUTHORITY_DIGEST = "eded7e5b895cce0c6512f526af8a9b5afec02c200dad2d3b7a54ec353b2e3374"
MODEL_REVISION = "e73636d4f797dec63c3081bb6ed5c7b0bb3f2089"
TOKENIZER_SHA256 = "a898ea75433890f6610f4e470b8ebeb0c21dce5c8dd61f892eb09eb5919d2e2c"
EXPECTED_RECORD_COUNT = 19
MANIFEST_SHA256 = "cece3f852d87c07c1d2339576002273b28e3a11b5e7719d12a17adf6874eba9b"


class ProductiveEvaluationAuthorityError(ValueError):
    pass


def _require(condition: bool, message: str) -> None:
    if not condition:
        raise ProductiveEvaluationAuthorityError(message)


def load_productive_authority(path: str | Path) -> dict[str, Any]:
    value = json.loads(Path(path).read_text(encoding="utf-8"))
    _require(value.get("authorityVersion") == AUTHORITY_VERSION, "AUTHORITY_VERSION_INVALID")
    _require(value.get("checkpointReference") == CHECKPOINT_REFERENCE, "CHECKPOINT_REFERENCE_INVALID")
    _require(value.get("checkpointDigest") == CHECKPOINT_DIGEST, "CHECKPOINT_DIGEST_INVALID")
    _require(value.get("runtimeAuthorityDigest") == RUNTIME_AUTHORITY_DIGEST, "RUNTIME_AUTHORITY_INVALID")
    _require(value.get("modelRevision") == MODEL_REVISION, "MODEL_REVISION_INVALID")
    _require(value.get("tokenizerSha256") == TOKENIZER_SHA256, "TOKENIZER_DIGEST_INVALID")
    _require(value.get("recordCount") == EXPECTED_RECORD_COUNT, "RECORD_COUNT_INVALID")
    _require(value.get("semantics") == {
        "targetKind": "PRIMARY_AND_SECONDARY",
        "candidateCompatibility": "COMPATIBLE_OR_REJECT",
        "candidateConditioning": "CANDIDATE_CANONICAL_NAME",
        "tokenization": "HISTORICAL_EVALUATOR_V1",
        "inputSerialization": "OBSERVED_TERM_PLUS_CANDIDATE",
        "classOrdering": "HISTORICAL_EVALUATOR_V1",
        "metricDefinitions": "HISTORICAL_EVALUATOR_V1",
        "decoding": "HISTORICAL_EVALUATOR_V1",
        "developmentMembershipOrder": "HISTORICAL_AUTHORITY_ORDER",
    }, "SEMANTICS_CHANGED")
    return value


def validate_productive_checkpoint(manifest_path: str | Path) -> dict[str, Any]:
    path = Path(manifest_path)
    _require(path.is_file() and not path.is_symlink(), "CHECKPOINT_MANIFEST_MISSING")
    _require(hashlib.sha256(path.read_bytes()).hexdigest() == MANIFEST_SHA256, "CHECKPOINT_MANIFEST_DIGEST_INVALID")
    manifest = json.loads(path.read_text(encoding="utf-8"))
    _require(manifest.get("checkpointReference") == CHECKPOINT_REFERENCE, "CHECKPOINT_REFERENCE_MISMATCH")
    _require(manifest.get("checkpointLogicalDigest") == CHECKPOINT_DIGEST, "CHECKPOINT_DIGEST_MISMATCH")
    bindings = manifest.get("authorityBindings", {})
    _require(bindings.get("runtimeAuthorityLogicalDigest") == RUNTIME_AUTHORITY_DIGEST, "RUNTIME_BINDING_MISMATCH")
    identity = bindings.get("runtimeIdentity", {})
    _require(identity.get("modelRevision") == MODEL_REVISION, "MODEL_BINDING_MISMATCH")
    _require(identity.get("tokenizerSha256") == TOKENIZER_SHA256, "TOKENIZER_BINDING_MISMATCH")
    return manifest


def prepare_productive_evaluation(*, authority_root: str | Path, authority_path: str | Path,
                                  checkpoint_manifest_path: str | Path, tokenizer: Any | None = None) -> dict[str, Any]:
    """Validate all pre-forward bindings and materialize the exact input set."""
    authority = load_productive_authority(authority_path)
    manifest = validate_productive_checkpoint(checkpoint_manifest_path)
    bundle = historical.load_authorities(authority_root)
    examples = historical.resolve_expanded_examples(bundle)
    _require(len(examples) == EXPECTED_RECORD_COUNT, "EXAMPLE_COUNT_INVALID")
    _require([x.ordering_index for x in examples] == list(range(EXPECTED_RECORD_COUNT)), "EXAMPLE_ORDER_INVALID")
    _require(not any(x.evaluation_bucket == "FROZEN_HOLDOUT" for x in examples), "HOLDOUT_EXECUTION_RECORD")
    model_input = historical.build_evaluation_model_input(examples, tokenizer) if tokenizer is not None else None
    return {"authority": authority, "manifest": manifest, "examples": examples,
            "modelInput": model_input, "modelForwardCount": 0, "holdoutAccessCount": 0}
