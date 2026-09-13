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
import subprocess
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
runtime_source_role = "him-trainer-runtime-source"


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


def source_git_head() -> str:
    try:
        return subprocess.check_output(
            ["git", "-C", str(repository_root), "rev-parse", "HEAD"],
            text=True,
            stderr=subprocess.DEVNULL,
        ).strip()
    except (OSError, subprocess.CalledProcessError) as error:
        raise SystemExit("runtime identity: source git HEAD unavailable") from error


def runtime_source_file_digests() -> list[dict[str, object]]:
    entries: list[dict[str, object]] = []
    for source_path, destination_path, role, _copied, _build_only in manifest_rows():
        if role != runtime_source_role:
            continue
        source = repository_root / source_path
        content = source.read_bytes()
        entries.append(
            {
                "sourcePath": source_path,
                "runtimePath": f"/opt/him/runtime/lib/python3.13/site-packages/him_trainer/{Path(destination_path).name}",
                "sizeBytes": len(content),
                "sha256": sha256_bytes(content),
            }
        )
    if not entries:
        raise SystemExit("runtime identity: runtime source closure is empty")
    return entries


def runtime_source_logical_digest(entries: list[dict[str, object]]) -> str:
    payload = "".join(
        f"{entry['sourcePath']}\t{entry['runtimePath']}\t{entry['sizeBytes']}\t{entry['sha256']}\n"
        for entry in entries
    ).encode()
    return sha256_bytes(payload)


def dependency_lock_digest() -> str:
    return sha256_bytes((repository_root / "training/him/uv.lock").read_bytes())


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
    source_entries = runtime_source_file_digests()
    source_digest = runtime_source_logical_digest(source_entries)
    identity = {
        "identitySchema": "HIM_A100_RUNTIME_IDENTITY_V2",
        "runtimeImageId": "HIM_A100_REFERENCE_TRAINING_RUNTIME_IMAGE_V1",
        "referenceEnvironmentContractDigest": "e5bfa4442a50e154c7f23735def545cfc545798f8b326ff38051f2eea57111d9",
        "buildContextDigest": build_context_digest(),
        "runtimeImageDefinitionDigest": definition_digest,
        "buildDefinitionDigest": definition_digest,
        "dependencyLockDigest": dependency_lock_digest(),
        "sourceGitHead": source_git_head(),
        "runtimeSourceLogicalDigest": source_digest,
        "runtimeSourceClosure": {
            "manifest": "training/him/runtime/a100/build-context.manifest.tsv",
            "fileCount": len(source_entries),
        },
        "sourceTreeFileDigests": source_entries,
        "ociImageDigest": None,
        "runtimeIdentityRuntimePath": "/opt/him/runtime/runtime-identity.json",
        "runtimeImageDefinitionDigestExcludedFields": list(excluded_fields),
        "contentTagSchemeVersion": "V1",
        "contentTagPrefixLength": 12,
        "contentDerivedDeploymentTag": f"def-{definition_digest[:12]}",
        "previousContentDerivedDeploymentTag": "def-68f69540cf6c",
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
