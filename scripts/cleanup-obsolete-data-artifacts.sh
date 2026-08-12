#!/usr/bin/env bash

set -euo pipefail

PROJECT_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || true)"

if [[ -z "${PROJECT_ROOT}" ]]; then
    echo "ERROR: Not inside a Git repository."
    exit 1
fi

cd "${PROJECT_ROOT}"

if [[ ! -d "data" || ! -d "app" ]]; then
    echo "ERROR: Expected ShopMe repository structure not found."
    echo "Project root: ${PROJECT_ROOT}"
    exit 1
fi

echo "ShopMe repository:"
echo "  ${PROJECT_ROOT}"
echo

echo "Removing obsolete generated catalog artifacts..."
rm -rf \
    data/generated

echo "Removing obsolete pre-master canonical catalog..."
rm -f \
    data/catalog/canonical-food-catalog.json

echo "Removing Local Matcher experiment artifacts..."
rm -rf \
    data/models/comparison \
    data/models/nutrition-feature-optimization

echo
echo "Cleanup completed."
echo
echo "Remaining data structure:"
tree data