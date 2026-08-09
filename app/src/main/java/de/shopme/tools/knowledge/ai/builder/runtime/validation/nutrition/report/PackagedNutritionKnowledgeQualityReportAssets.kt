package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.report

object PackagedNutritionKnowledgeQualityReportAssets {

    const val DIRECTORY =
        "knowledge/reports/nutrition"

    const val VALIDATION_REPORT =
        "$DIRECTORY/resulting-nutrition-knowledge-validation.json"

    const val COVERAGE_REPORT =
        "$DIRECTORY/resulting-nutrition-coverage.json"

    const val COVERAGE_GAP_CLASSIFICATION_REPORT =
        "$DIRECTORY/resulting-nutrition-coverage-gap-classification.json"

    const val CONFLICT_REPORT =
        "$DIRECTORY/resulting-nutrition-conflict-rate.json"

    const val CONFLICT_POLICY_REPORT =
        "$DIRECTORY/resulting-nutrition-conflict-policy.json"

    const val OFF_SOURCE_SNAPSHOT =
        "$DIRECTORY/off-nutrition-source-snapshot.json"

    val all: List<String> =
        listOf(
            VALIDATION_REPORT,
            COVERAGE_REPORT,
            COVERAGE_GAP_CLASSIFICATION_REPORT,
            CONFLICT_REPORT,
            CONFLICT_POLICY_REPORT,
            OFF_SOURCE_SNAPSHOT
        )

    init {
        require(
            all.size ==
                    all.distinct().size
        ) {
            "Packaged Nutrition quality report asset paths must be unique."
        }
    }
}