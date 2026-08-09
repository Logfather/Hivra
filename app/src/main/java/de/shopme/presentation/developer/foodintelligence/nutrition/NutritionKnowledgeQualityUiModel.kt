package de.shopme.presentation.developer.foodintelligence.nutrition

data class NutritionKnowledgeQualityUiModel(
    val overallStatus:
    NutritionKnowledgeQualityStatusUiModel,
    val validation:
    NutritionKnowledgeValidationUiModel,
    val coverage:
    NutritionKnowledgeCoverageUiModel,
    val conflicts:
    NutritionKnowledgeConflictsUiModel,
    val conflictPolicy:
    NutritionKnowledgeConflictPolicyUiModel,
    val offFreeze:
    NutritionKnowledgeOffFreezeUiModel
)

data class NutritionKnowledgeQualityStatusUiModel(
    val status:
    NutritionKnowledgeQualityUiStatus,
    val label: String,
    val description: String
)

enum class NutritionKnowledgeQualityUiStatus {
    APPROVED,
    WARNING,
    REJECTED
}

data class NutritionKnowledgeValidationUiModel(
    val status:
    NutritionKnowledgeQualityUiStatus,
    val statusLabel: String,
    val entryCount: String,
    val validEntryCount: String,
    val warningCount: String,
    val errorCount: String,
    val rejectedEntryCount: String,
    val summary: String
)

data class NutritionKnowledgeCoverageUiModel(
    val exactCoveragePercentage: String,
    val effectiveCoveragePercentage: String,
    val aggregateEntryCount: String,
    val runtimeEntryCount: String,
    val exactMatchCount: String,
    val normalizationEquivalentMatchCount: String,
    val trueMissingEntryCount: String,
    val trueAdditionalEntryCount: String,
    val normalizationCollisionGroupCount: String,
    val complete: Boolean,
    val status:
    NutritionKnowledgeQualityUiStatus,
    val statusLabel: String
)

data class NutritionKnowledgeConflictsUiModel(
    val conflictEntryCount: String,
    val nutrientConflictCount: String,
    val entryConflictPercentage: String,
    val nutrientConflictPercentage: String,
    val comparableEntryCount: String,
    val conflictFreeEntryCount: String,
    val status:
    NutritionKnowledgeQualityUiStatus,
    val statusLabel: String
)

data class NutritionKnowledgeConflictPolicyUiModel(
    val status:
    NutritionKnowledgeQualityUiStatus,
    val statusLabel: String,
    val maximumEntryConflictPercentage: String,
    val maximumNutrientConflictPercentage: String,
    val extremeConflictCount: String,
    val maximumExtremeConflictCount: String,
    val automaticCorrectionAllowed: Boolean,
    val automaticEntryRejectionAllowed: Boolean,
    val automaticCorrectionLabel: String,
    val automaticEntryRejectionLabel: String
)

data class NutritionKnowledgeOffFreezeUiModel(
    val status:
    NutritionKnowledgeQualityUiStatus,
    val statusLabel: String,
    val source: String,
    val sourceVersion: String,
    val aggregateEntryCount: String,
    val frozenFileSize: String,
    val sha256: String,
    val checksumVerified: Boolean,
    val checksumLabel: String,
    val validationApproved: Boolean,
    val conflictPolicyApproved: Boolean
)