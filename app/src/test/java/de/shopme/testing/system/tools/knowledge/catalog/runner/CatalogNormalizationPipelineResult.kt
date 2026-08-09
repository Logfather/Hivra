package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.application.CatalogCanonicalizationApplicationResult
import de.shopme.testing.system.tools.knowledge.catalog.duplicate.resolution.diagnostics.CatalogRemainingTypoDiagnosticsResult
import de.shopme.testing.system.tools.knowledge.catalog.removal.CatalogSemanticTypoRemovalResult
import de.shopme.testing.system.tools.knowledge.catalog.review.CatalogReviewBacklogEvaluationResult
import de.shopme.testing.system.tools.knowledge.catalog.review.analysis.CatalogReviewBacklogAnalysisResult
import de.shopme.testing.system.tools.knowledge.catalog.review.analysis.updated.CatalogUpdatedReviewAnalysisResult
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewBacklogClassification
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewBacklogClassificationResult
import de.shopme.testing.system.tools.knowledge.catalog.review.reclassification.CatalogMisclassifiedTypoClass
import de.shopme.testing.system.tools.knowledge.catalog.review.reclassification.CatalogMisclassifiedTypoReclassificationResult
import de.shopme.testing.system.tools.knowledge.catalog.validation.NormalizedCatalogValidationResult
import java.io.File

data class CatalogNormalizationPipelineResult(
    val auditResult: CatalogAuditPipelineResult,

    val applicationResult:
    CatalogCanonicalizationApplicationResult,

    val semanticTypoRemovalResult:
    CatalogSemanticTypoRemovalResult,

    val validationResult:
    NormalizedCatalogValidationResult,

    val reviewBacklogResult:
    CatalogReviewBacklogEvaluationResult,

    val reviewBacklogClassificationResult:
    CatalogReviewBacklogClassificationResult,

    val reviewBacklogAnalysisResult:
    CatalogReviewBacklogAnalysisResult,

    val updatedReviewAnalysisResult:
    CatalogUpdatedReviewAnalysisResult,

    val remainingTypoDiagnosticsResult:
    CatalogRemainingTypoDiagnosticsResult,

    val misclassifiedTypoReclassificationResult:
    CatalogMisclassifiedTypoReclassificationResult,

    val normalizedCatalogFile: File,

    val reviewBacklogReportFile: File,

    val reviewBacklogClassificationReportFile: File,

    val reviewBacklogAnalysisReportFile: File,

    val updatedReviewAnalysisReportFile: File,

    val remainingTypoDiagnosticsReportFile: File,

    val misclassifiedTypoReclassificationReportFile: File,

    val semanticTypoRemovalReportFile: File,

    val valid: Boolean
) {

    init {
        validateSemanticTypoRemoval()
        validateTypoReclassification()
        validateReviewBacklogCoverage()
        validateAnalysisCoverage()
        validateOutputFiles()
    }

    private fun validateSemanticTypoRemoval() {
        val effectiveTypoCandidateCount =
            reviewBacklogClassificationResult
                .countsByClassification[
                CatalogReviewBacklogClassification
                    .TYPO_VARIANT
            ] ?: 0

        require(
            semanticTypoRemovalResult.candidateEntryCount ==
                    effectiveTypoCandidateCount
        ) {
            "Semantic typo removal must cover every effective " +
                    "TYPO_VARIANT entry. " +
                    "effectiveTypo=$effectiveTypoCandidateCount, " +
                    "removalCandidates=" +
                    semanticTypoRemovalResult.candidateEntryCount +
                    "."
        }

        require(
            semanticTypoRemovalResult.removedEntryCount ==
                    semanticTypoRemovalResult.candidateEntryCount
        ) {
            "Every semantic typo removal candidate must be removed. " +
                    "candidates=" +
                    semanticTypoRemovalResult.candidateEntryCount +
                    ", removed=" +
                    semanticTypoRemovalResult.removedEntryCount +
                    "."
        }

        require(
            semanticTypoRemovalResult.outputEntryCount ==
                    semanticTypoRemovalResult.inputEntryCount -
                    semanticTypoRemovalResult.removedEntryCount
        ) {
            "Semantic typo removal output count is inconsistent."
        }

        require(
            semanticTypoRemovalResult.outputEntryCount ==
                    semanticTypoRemovalResult
                        .outputItemsBySourceIndex
                        .size
        ) {
            "Semantic typo removal output map size is inconsistent."
        }

        require(
            semanticTypoRemovalResult.removedSourceIndices.none {
                it in
                        semanticTypoRemovalResult
                            .outputItemsBySourceIndex
            }
        ) {
            "A removed semantic typo sourceIndex remains in the output."
        }

        require(
            semanticTypoRemovalResult.valid
        ) {
            "Semantic typo removal result must be valid."
        }
    }

    private fun validateTypoReclassification() {
        val effectiveTypoCandidateCount =
            reviewBacklogClassificationResult
                .countsByClassification[
                CatalogReviewBacklogClassification
                    .TYPO_VARIANT
            ] ?: 0

        val reclassifiedTypoCandidateCount =
            misclassifiedTypoReclassificationResult
                .reclassifiedEntryCount

        val initiallyDiagnosedTypoCandidateCount =
            remainingTypoDiagnosticsResult
                .remainingTypoCandidateCount

        require(
            initiallyDiagnosedTypoCandidateCount ==
                    effectiveTypoCandidateCount +
                    reclassifiedTypoCandidateCount
        ) {
            "Initial typo diagnostics are inconsistent with the " +
                    "effective review backlog. " +
                    "diagnosed=$initiallyDiagnosedTypoCandidateCount, " +
                    "effectiveTypo=$effectiveTypoCandidateCount, " +
                    "reclassified=$reclassifiedTypoCandidateCount."
        }

        requireReclassificationCount(
            backlogClassification =
                CatalogReviewBacklogClassification
                    .FROZEN_FORM_VARIANT,
            reclassificationClass =
                CatalogMisclassifiedTypoClass
                    .FROZEN_FORM_VARIANT,
            description =
                "Frozen-form"
        )

        requireReclassificationCount(
            backlogClassification =
                CatalogReviewBacklogClassification
                    .CANNED_FORM_VARIANT,
            reclassificationClass =
                CatalogMisclassifiedTypoClass
                    .CANNED_FORM_VARIANT,
            description =
                "Canned-form"
        )

        requireReclassificationCount(
            backlogClassification =
                CatalogReviewBacklogClassification
                    .BIO_ATTRIBUTE_VARIANT,
            reclassificationClass =
                CatalogMisclassifiedTypoClass
                    .BIO_ATTRIBUTE_VARIANT,
            description =
                "Bio-attribute"
        )

        require(
            misclassifiedTypoReclassificationResult.valid
        ) {
            "Misclassified typo reclassification result must be valid."
        }

        require(
            remainingTypoDiagnosticsResult.valid
        ) {
            "Remaining typo diagnostics result must be valid."
        }
    }

    private fun requireReclassificationCount(
        backlogClassification:
        CatalogReviewBacklogClassification,

        reclassificationClass:
        CatalogMisclassifiedTypoClass,

        description: String
    ) {
        val effectiveCount =
            reviewBacklogClassificationResult
                .countsByClassification[
                backlogClassification
            ] ?: 0

        val reclassifiedCount =
            misclassifiedTypoReclassificationResult
                .countsByClass[
                reclassificationClass
            ] ?: 0

        require(effectiveCount == reclassifiedCount) {
            "$description reclassification count is inconsistent. " +
                    "effective=$effectiveCount, " +
                    "reclassified=$reclassifiedCount."
        }
    }

    private fun validateReviewBacklogCoverage() {
        require(
            reviewBacklogClassificationResult
                .sourceBacklogEntryCount ==
                    reviewBacklogResult.unresolvedEntryCount
        ) {
            "Classification input count must equal the unresolved " +
                    "review backlog count."
        }

        require(
            reviewBacklogClassificationResult
                .classifiedEntryCount ==
                    reviewBacklogResult.unresolvedEntryCount
        ) {
            "Every unresolved review backlog entry must be classified."
        }

        require(
            reviewBacklogClassificationResult.valid
        ) {
            "Review backlog classification result must be valid."
        }

        require(
            reviewBacklogResult.valid
        ) {
            "Review backlog evaluation result must be valid."
        }
    }

    private fun validateAnalysisCoverage() {
        require(
            reviewBacklogAnalysisResult.classifiedEntryCount ==
                    reviewBacklogClassificationResult.classifiedEntryCount
        ) {
            "Review backlog analysis must cover every classified entry."
        }

        require(
            updatedReviewAnalysisResult.classifiedEntryCount ==
                    reviewBacklogAnalysisResult.classifiedEntryCount
        ) {
            "Updated review analysis must cover the current analysis."
        }

        require(
            reviewBacklogAnalysisResult.valid
        ) {
            "Review backlog analysis result must be valid."
        }

        require(
            updatedReviewAnalysisResult.valid
        ) {
            "Updated review analysis result must be valid."
        }
    }

    private fun validateOutputFiles() {
        requireFileName(
            file = normalizedCatalogFile,
            expectedName = null,
            description = "Normalized catalog"
        )

        requireFileName(
            file = reviewBacklogReportFile,
            expectedName = null,
            description = "Review backlog report"
        )

        requireFileName(
            file = reviewBacklogClassificationReportFile,
            expectedName = null,
            description = "Review backlog classification report"
        )

        requireFileName(
            file = reviewBacklogAnalysisReportFile,
            expectedName = null,
            description = "Review backlog analysis report"
        )

        requireFileName(
            file = updatedReviewAnalysisReportFile,
            expectedName = null,
            description = "Updated review analysis report"
        )

        requireFileName(
            file = remainingTypoDiagnosticsReportFile,
            expectedName = null,
            description = "Remaining typo diagnostics report"
        )

        requireFileName(
            file = misclassifiedTypoReclassificationReportFile,
            expectedName =
                MISCLASSIFIED_TYPO_RECLASSIFICATION_REPORT_FILE_NAME,
            description =
                "Misclassified typo reclassification report"
        )

        requireFileName(
            file = semanticTypoRemovalReportFile,
            expectedName =
                SEMANTIC_TYPO_REMOVAL_REPORT_FILE_NAME,
            description =
                "Semantic typo removal report"
        )
    }

    private fun requireFileName(
        file: File,
        expectedName: String?,
        description: String
    ) {
        require(file.name.isNotBlank()) {
            "$description file must have a filename."
        }

        if (expectedName != null) {
            require(file.name == expectedName) {
                "$description filename is '${file.name}', " +
                        "expected '$expectedName'."
            }
        }
    }

    private companion object {
        const val MISCLASSIFIED_TYPO_RECLASSIFICATION_REPORT_FILE_NAME =
            "catalog-misclassified-typo-reclassification.json"

        const val SEMANTIC_TYPO_REMOVAL_REPORT_FILE_NAME =
            "catalog-unresolved-semantic-typo-removals.json"
    }
}