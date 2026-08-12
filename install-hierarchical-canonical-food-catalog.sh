#!/usr/bin/env bash

set -euo pipefail

PROJECT_ROOT="/Users/logfather/AndroidStudioProjects/ShopMe"
SOURCE_DIR="$HOME/Downloads/100826"

TAXONOMY_SOURCE="$SOURCE_DIR/primary-canonical-german-food-taxonomy.hierarchical.v2.json"
CATALOG_SOURCE="$SOURCE_DIR/canonical-food-catalog.hierarchical-taxonomy-assigned.json"
REPORT_SOURCE="$SOURCE_DIR/canonical-food-hierarchical-taxonomy-assignment-report.json"

TAXONOMY_TARGET_DIR="$PROJECT_ROOT/build/knowledge/catalog/taxonomy-hierarchical"
CATALOG_TARGET_DIR="$PROJECT_ROOT/build/knowledge/catalog/taxonomy-hierarchical-assigned"
REPORT_TARGET_DIR="$PROJECT_ROOT/build/knowledge/reports"

TAXONOMY_TARGET="$TAXONOMY_TARGET_DIR/primary-canonical-german-food-taxonomy.hierarchical.json"
CATALOG_TARGET="$CATALOG_TARGET_DIR/canonical-food-catalog.hierarchical-taxonomy-assigned.json"
REPORT_TARGET="$REPORT_TARGET_DIR/canonical-food-hierarchical-taxonomy-assignment-report.json"

echo
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "INSTALL HIERARCHICAL CANONICAL FOOD CATALOG"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo

#
# Verify source files
#

for file in \
    "$TAXONOMY_SOURCE" \
    "$CATALOG_SOURCE" \
    "$REPORT_SOURCE"
do
    if [[ ! -f "$file" ]]; then
        echo "ERROR: Source file not found:"
        echo "  $file"
        exit 1
    fi
done

#
# Verify ShopMe project
#

if [[ ! -d "$PROJECT_ROOT/app" ]]; then
    echo "ERROR: ShopMe project root not found:"
    echo "  $PROJECT_ROOT"
    exit 1
fi

#
# Validate JSON before copying
#

echo "Validating JSON..."

jq empty "$TAXONOMY_SOURCE"
jq empty "$CATALOG_SOURCE"
jq empty "$REPORT_SOURCE"

echo "JSON validation: OK"
echo

#
# Create target directories
#

mkdir -p \
    "$TAXONOMY_TARGET_DIR" \
    "$CATALOG_TARGET_DIR" \
    "$REPORT_TARGET_DIR"

#
# Backup existing files
#

TIMESTAMP="$(date '+%Y%m%d-%H%M%S')"
BACKUP_DIR="$PROJECT_ROOT/build/knowledge/backups/hierarchical-catalog-$TIMESTAMP"

mkdir -p "$BACKUP_DIR"

backup_if_exists() {
    local file="$1"

    if [[ -f "$file" ]]; then
        cp "$file" "$BACKUP_DIR/"
        echo "Backed up: $file"
    fi
}

backup_if_exists "$TAXONOMY_TARGET"
backup_if_exists "$CATALOG_TARGET"
backup_if_exists "$REPORT_TARGET"

echo

#
# Install new artifacts
#

cp \
    "$TAXONOMY_SOURCE" \
    "$TAXONOMY_TARGET"

cp \
    "$CATALOG_SOURCE" \
    "$CATALOG_TARGET"

cp \
    "$REPORT_SOURCE" \
    "$REPORT_TARGET"

echo "Files installed."
echo

#
# Validate installed artifacts
#

jq empty "$TAXONOMY_TARGET"
jq empty "$CATALOG_TARGET"
jq empty "$REPORT_TARGET"

IDENTITY_COUNT="$(
    jq 'length' \
        "$CATALOG_TARGET"
)"

VARIANT_COUNT="$(
    jq '[.[].variants[]] | length' \
        "$CATALOG_TARGET"
)"

SOURCE_VARIANT_COUNT="$(
    jq '[.[].sourceVariants[]] | length' \
        "$CATALOG_TARGET"
)"

EMPTY_PATH_COUNT="$(
    jq '
        [
            .[]
            | select(
                (.taxonomyPaths | length) == 0
            )
        ]
        | length
    ' \
        "$CATALOG_TARGET"
)"

DUPLICATE_NORMALIZED_COUNT="$(
    jq '
        [
            group_by(.normalized)[]
            | select(length > 1)
        ]
        | length
    ' \
        "$CATALOG_TARGET"
)"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "RESULT"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Canonical identities      : $IDENTITY_COUNT"
echo "Variants                  : $VARIANT_COUNT"
echo "Source variants           : $SOURCE_VARIANT_COUNT"
echo "Identities without path   : $EMPTY_PATH_COUNT"
echo "Duplicate normalized keys : $DUPLICATE_NORMALIZED_COUNT"
echo
echo "Taxonomy:"
echo "  $TAXONOMY_TARGET"
echo
echo "Catalog:"
echo "  $CATALOG_TARGET"
echo
echo "Report:"
echo "  $REPORT_TARGET"
echo
echo "Backup:"
echo "  $BACKUP_DIR"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"