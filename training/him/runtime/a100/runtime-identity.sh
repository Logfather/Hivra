#!/usr/bin/env bash
set -Eeuo pipefail

readonly DEFINITION_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd "$DEFINITION_ROOT/../../../.." && pwd)"
readonly MANIFEST="$DEFINITION_ROOT/build-context.manifest.tsv"
readonly IDENTITY_FILE="$DEFINITION_ROOT/runtime-identity.json"

python3 - "$DEFINITION_ROOT" "$REPOSITORY_ROOT" "$MANIFEST" "$IDENTITY_FILE" "${1:---generate}" <<'PY'
from __future__ import annotations

import hashlib
import json
import sys
from pathlib import Path


definition_root = Path(sys.argv[1])
repository_root = Path(sys.argv[2])
manifest_path = Path(sys.argv[3])
identity_path = Path(sys.argv[4])
operation = sys.argv[5]

excluded_fields = (
    "buildContextDigest",
    "runtimeImageDefinitionDigest",
    "ociImageDigest",
)
runtime_definition_source = "training/him/runtime/a100/runtime-image-definition.json"
runtime_identity_source = "training/him/runtime/a100/runtime-identity.json"


def sha256_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def canonical_runtime_definition_bytes() -> bytes:
    source = json.loads((definition_root / "runtime-image-definition.json").read_text())
    for field in excluded_fields:
        source.pop(field, None)
    return (
        json.dumps(source, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
        + "\n"
    ).encode()


def runtime_definition_digest() -> str:
    inputs = (
        ("Dockerfile", "image-definition", (definition_root / "Dockerfile").read_bytes()),
        ("start.sh", "startup-bootstrap", (definition_root / "start.sh").read_bytes()),
        (
            "runtime-image-definition.json",
            "runtime-metadata-canonical",
            canonical_runtime_definition_bytes(),
        ),
    )
    payload = "".join(
        f"{relative_path}\t{role}\t{sha256_bytes(content)}\n"
        for relative_path, role, content in inputs
    ).encode()
    return sha256_bytes(payload)


def manifest_rows() -> list[tuple[str, str, str, str, str]]:
    rows: list[tuple[str, str, str, str, str]] = []
    for line in manifest_path.read_text().splitlines()[1:]:
        if line:
            rows.append(tuple(line.split("\t")))
    return rows


def build_context_digest() -> str:
    digest_rows: list[str] = []
    for source_path, destination_path, role, copied, build_only in manifest_rows():
        if source_path == runtime_identity_source:
            digest_rows.append(
                f"{destination_path}\t{role}\t{copied}\t{build_only}\t"
                "DERIVED_RUNTIME_IDENTITY_EXCLUDED\n"
            )
            continue

        source = repository_root / source_path
        if source_path == runtime_definition_source:
            content = canonical_runtime_definition_bytes()
        else:
            content = source.read_bytes()
        digest_rows.append(
            f"{destination_path}\t{role}\t{copied}\t{build_only}\t"
            f"{len(content)}\t{sha256_bytes(content)}\n"
        )
    return sha256_bytes("".join(digest_rows).encode())


if operation == "--definition-digest":
    print(runtime_definition_digest())
elif operation == "--build-context-digest":
    print(build_context_digest())
elif operation == "--generate":
    definition_digest = runtime_definition_digest()
    identity = {
        "identitySchema": "HIM_A100_RUNTIME_IDENTITY_V1",
        "runtimeImageId": "HIM_A100_REFERENCE_TRAINING_RUNTIME_IMAGE_V1",
        "referenceEnvironmentContractDigest": "e5bfa4442a50e154c7f23735def545cfc545798f8b326ff38051f2eea57111d9",
        "buildContextDigest": build_context_digest(),
        "runtimeImageDefinitionDigest": definition_digest,
        "ociImageDigest": None,
        "runtimeIdentityRuntimePath": "/opt/him/runtime/runtime-identity.json",
        "runtimeImageDefinitionDigestExcludedFields": list(excluded_fields),
        "contentTagSchemeVersion": "V1",
        "contentTagPrefixLength": 12,
        "contentDerivedDeploymentTag": f"def-{definition_digest[:12]}",
        "previousContentDerivedDeploymentTag": "def-e5562d215017",
        "buildContextDigestRule": (
            "Canonical runtime-image-definition.json bytes exclude derived identity outputs; "
            "runtime-identity.json is derived metadata and excluded from the authority digest."
        ),
    }
    encoded = (json.dumps(identity, ensure_ascii=False, indent=2) + "\n").encode()
    identity_path.write_bytes(encoded)
    print(identity_path)
else:
    raise SystemExit(f"unsupported operation: {operation}")
PY
