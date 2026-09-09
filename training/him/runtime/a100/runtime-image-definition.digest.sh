#!/usr/bin/env bash
set -Eeuo pipefail

readonly DEFINITION_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

[[ -x "$DEFINITION_ROOT/runtime-identity.sh" ]] || {
    printf 'runtime-image-definition: runtime identity generator is not executable\n' >&2
    exit 1
}

exec "$DEFINITION_ROOT/runtime-identity.sh" --definition-digest
