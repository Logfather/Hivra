#!/usr/bin/env bash
set -Eeuo pipefail

readonly DEFINITION_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd "$DEFINITION_ROOT/../../../.." && pwd)"
readonly PYTHON_VERSION=3.13.14
readonly UV_VERSION=0.12.2

fail() {
    printf 'runtime-rootfs: %s\n' "$1" >&2
    exit 78
}

[[ "$(uname -s)" == "Linux" ]] || fail "Linux host required"
case "$(uname -m)" in
    x86_64|amd64) ;;
    *) fail "x86_64 host required" ;;
esac

[[ $# -eq 1 ]] || fail "usage: build-runtime-rootfs.sh <prepared-rootfs>"
readonly ROOTFS="$(cd "$1" 2>/dev/null && pwd)" || fail "rootfs does not exist"
[[ "$ROOTFS" != "/" ]] || fail "refusing to use filesystem root"
[[ -x "$ROOTFS/bin/bash" ]] || fail "prepared rootfs lacks /bin/bash"
[[ -x "$ROOTFS/usr/bin/curl" ]] || fail "prepared rootfs lacks /usr/bin/curl"
[[ -x "$ROOTFS/usr/bin/apt-get" ]] || fail "prepared rootfs lacks /usr/bin/apt-get"

install -d -m 0755 \
    "$ROOTFS/opt/him/bin" \
    "$ROOTFS/opt/him/dependency-authority" \
    "$ROOTFS/opt/him/validation" \
    "$ROOTFS/opt/him/runtime" \
    "$ROOTFS/opt/him/python" \
    "$ROOTFS/workspace"

install -m 0644 "$REPOSITORY_ROOT/training/him/pyproject.toml" \
    "$ROOTFS/opt/him/dependency-authority/pyproject.toml"
install -m 0644 "$REPOSITORY_ROOT/training/him/uv.lock" \
    "$ROOTFS/opt/him/dependency-authority/uv.lock"
install -m 0644 "$DEFINITION_ROOT/runtime-image-definition.json" \
    "$ROOTFS/opt/him/runtime-image-definition.json"
install -m 0644 "$DEFINITION_ROOT/runtime-identity.json" \
    "$ROOTFS/opt/him/runtime/runtime-identity.json"
install -m 0644 "$REPOSITORY_ROOT/training/him/src/him_trainer/a100_validation_v1.py" \
    "$ROOTFS/opt/him/validation/a100_validation_v1.py"
install -m 0644 "$DEFINITION_ROOT/him_runtime_validation_v1.py" \
    "$ROOTFS/opt/him/validation/him_runtime_validation_v1.py"
install -m 0755 "$DEFINITION_ROOT/start.sh" "$ROOTFS/opt/him/bin/start.sh"

env -i \
    PATH=/usr/local/bin:/usr/bin:/bin \
    HOME=/root \
    PYTHON_VERSION="$PYTHON_VERSION" \
    UV_VERSION="$UV_VERSION" \
    UV_PYTHON_INSTALL_DIR=/opt/him/python \
    UV_PROJECT_ENVIRONMENT=/opt/him/runtime \
    chroot "$ROOTFS" /bin/bash -o pipefail -c '
        set -Eeuo pipefail
        apt-get update
        apt-get install --yes --no-install-recommends ca-certificates curl openssh-server
        rm -rf /var/lib/apt/lists/*
        curl --fail --silent --show-error --location https://astral.sh/uv/install.sh \
          | env UV_VERSION="$UV_VERSION" UV_UNMANAGED_INSTALL=/usr/local/bin sh
        export PATH=/opt/him/runtime/bin:/opt/him/python/bin:/usr/local/bin:$PATH
        uv python install "$PYTHON_VERSION"
        test -x /opt/him/python/bin/python3.13
        uv sync --frozen --no-dev \
          --project /opt/him/dependency-authority \
          --python "$PYTHON_VERSION"
        test -x /opt/him/runtime/bin/python
        test -f /opt/him/runtime/lib/python3.13/site-packages/torch/__init__.py
        test -f /opt/him/runtime/runtime-identity.json
        test -x /usr/local/bin/uv
        /opt/him/runtime/bin/python \
          /opt/him/validation/him_runtime_validation_v1.py \
          --mode build \
          --lineage-output /opt/him/validation/build-lineage.json
    '

readonly TRAINER_RUNTIME_PACKAGE_ROOT="$ROOTFS/opt/him/runtime/lib/python3.13/site-packages/him_trainer"
install -d -m 0755 "$TRAINER_RUNTIME_PACKAGE_ROOT"
while IFS= read -r trainer_source; do
    install -m 0644 "$trainer_source" "$TRAINER_RUNTIME_PACKAGE_ROOT/$(basename "$trainer_source")"
done < <(find "$REPOSITORY_ROOT/training/him/src/him_trainer" -maxdepth 1 -type f -name '*.py' -print | sort)

chroot "$ROOTFS" /opt/him/runtime/bin/python \
    -c 'import him_trainer; import him_trainer.__main__ as entrypoint; assert callable(entrypoint.run)'

for required_path in \
    "$ROOTFS/opt/him/python" \
    "$ROOTFS/opt/him/runtime" \
    "$ROOTFS/opt/him/runtime/bin/python" \
    "$ROOTFS/opt/him/runtime/lib/python3.13/site-packages" \
    "$ROOTFS/opt/him/runtime/lib/python3.13/site-packages/him_trainer/protocol_v1.py" \
    "$ROOTFS/opt/him/runtime/runtime-identity.json" \
    "$ROOTFS/usr/local/bin/uv" \
    "$ROOTFS/opt/him/validation/build-lineage.json"; do
    [[ -e "$required_path" ]] || fail "required export path missing: $required_path"
done

printf 'RUNTIME_ROOTFS_REALIZATION=PASS\n'
printf 'RUNTIME_ROOTFS=%s\n' "$ROOTFS"
