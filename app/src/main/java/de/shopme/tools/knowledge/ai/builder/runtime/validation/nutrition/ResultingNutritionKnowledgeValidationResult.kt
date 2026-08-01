package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition

import java.io.File

data class ResultingNutritionKnowledgeValidationResult(
    val inputFile: File,
    val inputFileSizeBytes: Long,
    val entryCount: Long,
    val validEntryCount: Long,
    val warningEntryCount: Long,
    val rejectedEntryCount: Long,
    val warningCount: Long,
    val errorCount: Long,
    val countsByReason:
    Map<ResultingNutritionKnowledgeValidationReason, Long>,
    val warningIssues:
    List<ResultingNutritionKnowledgeValidationIssue>,
    val errorIssues:
    List<ResultingNutritionKnowledgeValidationIssue>,
    val durationMillis: Long
) {

    val isValid: Boolean
        get() =
            errorCount == 0L &&
                    rejectedEntryCount == 0L

    val issueCount: Long
        get() =
            warningCount +
                    errorCount

    val reportedWarningCount: Int
        get() =
            warningIssues.size

    val reportedErrorCount: Int
        get() =
            errorIssues.size

    val omittedWarningCount: Long
        get() =
            warningCount -
                    warningIssues.size.toLong()

    val omittedErrorCount: Long
        get() =
            errorCount -
                    errorIssues.size.toLong()

    init {
        require(inputFileSizeBytes >= 0L)
        require(entryCount >= 0L)
        require(validEntryCount >= 0L)
        require(warningEntryCount >= 0L)
        require(rejectedEntryCount >= 0L)
        require(warningCount >= 0L)
        require(errorCount >= 0L)
        require(durationMillis >= 0L)

        require(
            entryCount ==
                    validEntryCount +
                    warningEntryCount +
                    rejectedEntryCount
        ) {
            "Entry counts are inconsistent."
        }

        require(
            warningIssues.size.toLong() <=
                    warningCount
        ) {
            "Reported warning issue count must not exceed " +
                    "total warning count."
        }

        require(
            errorIssues.size.toLong() <=
                    errorCount
        ) {
            "Reported error issue count must not exceed " +
                    "total error count."
        }

        require(
            issueCount ==
                    countsByReason.values.sum()
        ) {
            "Issue count must equal countsByReason sum."
        }

        require(omittedWarningCount >= 0L)
        require(omittedErrorCount >= 0L)
    }
}