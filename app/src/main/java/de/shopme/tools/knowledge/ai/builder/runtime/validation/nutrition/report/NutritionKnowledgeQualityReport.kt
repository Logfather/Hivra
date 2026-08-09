package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.report

import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.ResultingNutritionKnowledgeValidationReport
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.ResultingNutritionConflictReport
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.policy.ResultingNutritionConflictPolicyReport
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.coverage.ResultingNutritionCoverageGapClassificationReport
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.coverage.ResultingNutritionCoverageReport
import de.shopme.tools.knowledge.off.nutrition.reference.freeze.OFFNutritionSourceSnapshot

data class NutritionKnowledgeQualityReport(
    val validation:
    ResultingNutritionKnowledgeValidationReport,
    val coverage:
    ResultingNutritionCoverageReport,
    val coverageGapClassification:
    ResultingNutritionCoverageGapClassificationReport,
    val conflicts:
    ResultingNutritionConflictReport,
    val conflictPolicy:
    ResultingNutritionConflictPolicyReport,
    val offNutritionSourceSnapshot:
    OFFNutritionSourceSnapshot
) {

    val approved: Boolean
        get() =
            validation.valid &&
                    conflictPolicy.decision.approved &&
                    offNutritionSourceSnapshot
                        .nutritionValidationApproved &&
                    offNutritionSourceSnapshot
                        .nutritionConflictPolicyApproved

    val exactCoveragePercentage: Double
        get() =
            coverageGapClassification
                .exactCoveragePercentage

    val effectiveCoveragePercentage: Double
        get() =
            coverageGapClassification
                .effectiveCoveragePercentage

    val entryConflictPercentage: Double
        get() =
            conflicts.entryConflictPercentage

    val nutrientConflictPercentage: Double
        get() =
            conflicts.nutrientConflictPercentage
}