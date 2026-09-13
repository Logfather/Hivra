#!/usr/bin/env bash
set -Eeuo pipefail

readonly DEFINITION_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd "$DEFINITION_ROOT/../../../.." && pwd)"
readonly PYTHON_VERSION=3.13.14
readonly UV_VERSION=0.12.2
readonly BUILD_CONTEXT_MANIFEST="$DEFINITION_ROOT/build-context.manifest.tsv"
readonly RUNTIME_SOURCE_ROLE="him-trainer-runtime-source"

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
    "$ROOTFS/opt/him/python" \
    "$ROOTFS/workspace"

install -m 0644 "$REPOSITORY_ROOT/training/him/pyproject.toml" \
    "$ROOTFS/opt/him/dependency-authority/pyproject.toml"
install -m 0644 "$REPOSITORY_ROOT/training/him/uv.lock" \
    "$ROOTFS/opt/him/dependency-authority/uv.lock"
install -m 0644 "$DEFINITION_ROOT/runtime-image-definition.json" \
    "$ROOTFS/opt/him/runtime-image-definition.json"
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
        uv python install --install-dir "$UV_PYTHON_INSTALL_DIR" "$PYTHON_VERSION"
        test -x /opt/him/python/cpython-3.13.14-linux-x86_64-gnu/bin/python3.13
        install -d -m 0755 /opt/him/python/bin
        if [ -e /opt/him/python/bin/python3.13 ] || [ -L /opt/him/python/bin/python3.13 ]; then
            test -L /opt/him/python/bin/python3.13
            test "$(readlink /opt/him/python/bin/python3.13)" = "../cpython-3.13.14-linux-x86_64-gnu/bin/python3.13"
        else
            ln -s ../cpython-3.13.14-linux-x86_64-gnu/bin/python3.13 /opt/him/python/bin/python3.13
        fi
        test -x /opt/him/python/bin/python3.13
        /opt/him/python/bin/python3.13 --version
        test ! -e /opt/him/runtime
        uv sync --frozen --no-dev \
          --project /opt/him/dependency-authority \
          --python /opt/him/python/bin/python3.13
        test -x /opt/him/runtime/bin/python
        test -f /opt/him/runtime/lib/python3.13/site-packages/torch/__init__.py
        test -x /usr/local/bin/uv
    '

install -m 0644 "$DEFINITION_ROOT/runtime-identity.json" \
    "$ROOTFS/opt/him/runtime/runtime-identity.json"

chroot "$ROOTFS" /opt/him/runtime/bin/python \
    /opt/him/validation/him_runtime_validation_v1.py \
    --mode build \
    --lineage-output /opt/him/validation/build-lineage.json

readonly TRAINER_RUNTIME_PACKAGE_ROOT="$ROOTFS/opt/him/runtime/lib/python3.13/site-packages/him_trainer"
install -d -m 0755 "$TRAINER_RUNTIME_PACKAGE_ROOT"
runtime_source_count=0
while IFS=$'\t' read -r source_path destination_path role copied_to_final_image build_only; do
    [[ "$source_path" == "source_path" ]] && continue
    [[ "$role" == "$RUNTIME_SOURCE_ROLE" ]] || continue
    [[ "$copied_to_final_image" == "YES" && "$build_only" == "NO" ]] \
        || fail "runtime source closure row is not final-image material"
    [[ "$destination_path" == trainer/him_trainer/*.py ]] \
        || fail "runtime source closure destination is not a trainer module: $destination_path"
    trainer_source="$REPOSITORY_ROOT/$source_path"
    [[ -f "$trainer_source" ]] || fail "runtime source closure source missing: $source_path"
    install -m 0644 "$trainer_source" "$TRAINER_RUNTIME_PACKAGE_ROOT/$(basename "$destination_path")"
    runtime_source_count=$((runtime_source_count + 1))
done < "$BUILD_CONTEXT_MANIFEST"
[[ "$runtime_source_count" -gt 0 ]] || fail "runtime source closure is empty"

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
