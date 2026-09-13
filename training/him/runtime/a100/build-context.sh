#!/usr/bin/env bash
set -Eeuo pipefail

readonly DEFINITION_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd "$DEFINITION_ROOT/../../../.." && pwd)"
readonly MANIFEST="$DEFINITION_ROOT/build-context.manifest.tsv"
readonly RUNTIME_SOURCE_ROLE="him-trainer-runtime-source"

sha256_file() {
    if command -v sha256sum >/dev/null 2>&1; then
        sha256sum "$1" | awk '{print $1}'
    else
        shasum -a 256 "$1" | awk '{print $1}'
    fi
}

fail() {
    printf 'build-context: %s\n' "$1" >&2
    exit 1
}

[[ -x "$DEFINITION_ROOT/runtime-identity.sh" ]] || fail "runtime identity generator is not executable"

[[ -f "$MANIFEST" ]] || fail "manifest missing"

"$DEFINITION_ROOT/runtime-identity.sh" --generate >/dev/null

if [[ $# -gt 1 ]]; then
    fail "usage: build-context.sh [output-directory]"
fi

if [[ $# -eq 1 ]]; then
    readonly OUTPUT_ROOT="$1"
    [[ ! -e "$OUTPUT_ROOT" ]] || fail "output directory already exists: $OUTPUT_ROOT"
    mkdir -p "$OUTPUT_ROOT"
else
    readonly OUTPUT_ROOT="$(mktemp -d "${TMPDIR:-/tmp}/him-a100-build-context.XXXXXX")"
fi

readonly EXPECTED_COUNT="$(awk -F '\t' 'NR > 1 && NF { count++ } END { print count + 0 }' "$MANIFEST")"
[[ "$EXPECTED_COUNT" -gt 0 ]] || fail "manifest has no entries"

declare -a DESTINATIONS=()
declare -a SOURCE_PATHS=()
declare -a DESTINATION_PATHS=()
declare -a ROLES=()
declare -a FINAL_FLAGS=()
declare -a BUILD_ONLY_FLAGS=()

while IFS=$'\t' read -r source_path destination_path role copied_to_final_image build_only; do
    [[ "$source_path" == "source_path" ]] && continue
    [[ -n "$source_path" ]] || continue
    [[ "$source_path" != /* && "$source_path" != *".."* ]] \
        || fail "source path is not repository-relative: $source_path"
    [[ "$destination_path" != /* && "$destination_path" != *".."* ]] \
        || fail "destination path is not bounded: $destination_path"
    if [[ "${#DESTINATIONS[@]}" -gt 0 ]]; then
        for existing_destination in "${DESTINATIONS[@]}"; do
            [[ "$existing_destination" != "$destination_path" ]] \
                || fail "duplicate destination path: $destination_path"
        done
    fi
    [[ "$copied_to_final_image" == YES || "$copied_to_final_image" == NO ]] \
        || fail "invalid final-image flag for $source_path"
    [[ "$build_only" == YES || "$build_only" == NO ]] \
        || fail "invalid build-only flag for $source_path"

    source_file="$REPOSITORY_ROOT/$source_path"
    [[ -f "$source_file" ]] || fail "required source missing: $source_path"
    DESTINATIONS+=("$destination_path")
    SOURCE_PATHS+=("$source_path")
    DESTINATION_PATHS+=("$destination_path")
    ROLES+=("$role")
    FINAL_FLAGS+=("$copied_to_final_image")
    BUILD_ONLY_FLAGS+=("$build_only")
    mkdir -p "$OUTPUT_ROOT/$(dirname "$destination_path")"
    cp "$source_file" "$OUTPUT_ROOT/$destination_path"
done < "$MANIFEST"

actual_count="$(find "$OUTPUT_ROOT" -type f -print | wc -l | tr -d ' ')"
[[ "$actual_count" == "$EXPECTED_COUNT" ]] \
    || fail "generated file count differs from manifest"

runtime_source_count="$(awk -F '\t' -v role="$RUNTIME_SOURCE_ROLE" 'NR > 1 && $3 == role { count++ } END { print count + 0 }' "$MANIFEST")"
[[ "$runtime_source_count" -gt 0 ]] || fail "runtime source closure is empty"

if find "$OUTPUT_ROOT" -type f \( \
    -iname '.env' -o -iname '.env.*' -o -iname '*credential*' -o \
    -iname '*secret*' -o -iname '*private*key*' -o -iname '*.pem' -o \
    -iname '*.p12' -o -iname '*.pfx' -o -iname '*.jks' \
\) -print -quit | grep -q .; then
    fail "secret-like file found in generated context"
fi

if grep -R -I -n -E \
    'RUNPOD_API_KEY|OPENAI_API_KEY|BEGIN (RSA|OPENSSH|EC|DSA) PRIVATE KEY|ghp_[A-Za-z0-9]|github_pat_[A-Za-z0-9]' \
    "$OUTPUT_ROOT" >/dev/null; then
    fail "credential pattern found in generated context"
fi

if find "$OUTPUT_ROOT" -type f -print \
    | grep -E -i '/(model|corpus|dataset|checkpoint|decision|review|knowledge|source|firestore|firebase)(/|\.|$)' \
    | grep -q .; then
    fail "training or knowledge data path found in generated context"
fi

printf 'RELATIVE_PATH\tROLE\tSIZE_BYTES\tSHA256\tCOPIED_TO_FINAL_IMAGE\tBUILD_ONLY\n'
total_bytes=0
for index in "${!SOURCE_PATHS[@]}"; do
    source_path="${SOURCE_PATHS[$index]}"
    destination_path="${DESTINATION_PATHS[$index]}"
    role="${ROLES[$index]}"
    copied_to_final_image="${FINAL_FLAGS[$index]}"
    build_only="${BUILD_ONLY_FLAGS[$index]}"
    output_file="$OUTPUT_ROOT/$destination_path"
    size_bytes="$(wc -c < "$output_file" | tr -d ' ')"
    file_sha256="$(sha256_file "$output_file")"
    printf '%s\t%s\t%s\t%s\t%s\t%s\n' \
        "$destination_path" "$role" "$size_bytes" "$file_sha256" \
        "$copied_to_final_image" "$build_only"
    total_bytes=$((total_bytes + size_bytes))
done

context_digest="$("$DEFINITION_ROOT/runtime-identity.sh" --build-context-digest)"
printf 'BUILD_CONTEXT_ROOT=%s\n' "$OUTPUT_ROOT"
printf 'BUILD_CONTEXT_FILE_COUNT=%s\n' "$EXPECTED_COUNT"
printf 'BUILD_CONTEXT_TOTAL_BYTES=%s\n' "$total_bytes"
printf 'BUILD_CONTEXT_DIGEST=%s\n' "$context_digest"
python3 - "$REPOSITORY_ROOT/training/him/runtime/a100/runtime-identity.json" <<'PY'
import json
import sys

identity = json.loads(open(sys.argv[1], encoding="utf-8").read())
print(f"SOURCE_GIT_HEAD={identity['sourceGitHead']}")
print(f"RUNTIME_SOURCE_LOGICAL_DIGEST={identity['runtimeSourceLogicalDigest']}")
print(f"DEPENDENCY_LOCK_DIGEST={identity['dependencyLockDigest']}")
print(f"BUILD_DEFINITION_DIGEST={identity['buildDefinitionDigest']}")
PY
