package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.application.CatalogCanonicalizationPlanApplier
import de.shopme.testing.system.tools.knowledge.catalog.category.CanonicalFoodCategoryRegistry
import de.shopme.testing.system.tools.knowledge.catalog.duplicate.resolution.diagnostics.CatalogRemainingTypoCandidateDiagnostics
import de.shopme.testing.system.tools.knowledge.catalog.model.CatalogFoodItem
import de.shopme.testing.system.tools.knowledge.catalog.removal.CatalogUnresolvedSemanticTypoRemover
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogMisclassifiedTypoReclassificationReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogRemainingTypoDiagnosticsReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogReviewBacklogAnalysisReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogReviewBacklogClassificationReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogReviewBacklogReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogSemanticTypoRemovalReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogUpdatedReviewAnalysisReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.review.CatalogReviewBacklogEvaluator
import de.shopme.testing.system.tools.knowledge.catalog.review.analysis.CatalogReviewBacklogAnalyzer
import de.shopme.testing.system.tools.knowledge.catalog.review.analysis.CatalogReviewResolutionRecommendation
import de.shopme.testing.system.tools.knowledge.catalog.review.analysis.updated.CatalogUpdatedReviewAnalyzer
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewBacklogClassifier
import de.shopme.testing.system.tools.knowledge.catalog.review.reclassification.CatalogMisclassifiedTypoReclassifier
import de.shopme.testing.system.tools.knowledge.catalog.review.reclassification.CatalogTypoReclassificationBacklogIntegrator
import de.shopme.testing.system.tools.knowledge.catalog.validation.NormalizedCatalogValidator
import de.shopme.testing.system.tools.knowledge.catalog.writer.NormalizedCatalogWriter
import java.io.File
import java.util.Locale

class CatalogNormalizationPipeline(
    private val auditPipeline:
    CatalogAuditPipeline,

    private val planApplier:
    CatalogCanonicalizationPlanApplier,

    private val validator:
    NormalizedCatalogValidator,

    private val categoryRegistry:
    CanonicalFoodCategoryRegistry,

    private val catalogWriter:
    NormalizedCatalogWriter,

    private val reviewBacklogEvaluator:
    CatalogReviewBacklogEvaluator,

    private val reviewBacklogReportWriter:
    CatalogReviewBacklogReportWriter,

    private val reviewBacklogClassifier:
    CatalogReviewBacklogClassifier,

    private val reviewBacklogClassificationReportWriter:
    CatalogReviewBacklogClassificationReportWriter,

    private val reviewBacklogAnalyzer:
    CatalogReviewBacklogAnalyzer,

    private val reviewBacklogAnalysisReportWriter:
    CatalogReviewBacklogAnalysisReportWriter,

    private val updatedReviewAnalyzer:
    CatalogUpdatedReviewAnalyzer,

    private val updatedReviewAnalysisReportWriter:
    CatalogUpdatedReviewAnalysisReportWriter,

    private val remainingTypoCandidateDiagnostics:
    CatalogRemainingTypoCandidateDiagnostics,

    private val remainingTypoDiagnosticsReportWriter:
    CatalogRemainingTypoDiagnosticsReportWriter,

    private val misclassifiedTypoReclassifier:
    CatalogMisclassifiedTypoReclassifier,

    private val misclassifiedTypoReclassificationReportWriter:
    CatalogMisclassifiedTypoReclassificationReportWriter,

    private val typoReclassificationBacklogIntegrator:
    CatalogTypoReclassificationBacklogIntegrator,

    private val unresolvedSemanticTypoRemover:
    CatalogUnresolvedSemanticTypoRemover,

    private val semanticTypoRemovalReportWriter:
    CatalogSemanticTypoRemovalReportWriter
) {

    fun run(
        catalogFile: File,
        auditOutputDirectory: File,
        normalizedOutputDirectory: File
    ): CatalogNormalizationPipelineResult {
        val auditResult =
            auditPipeline.run(
                catalogFile = catalogFile,
                outputDirectory = auditOutputDirectory
            )

        val applicationResult =
            planApplier.apply(
                entries = auditResult.entries,
                normalizations = auditResult.normalizations,
                plan = auditResult.canonicalizationPlan
            )

        /*
         * Die Review-Auswertung arbeitet weiterhin gegen das Ergebnis der
         * Canonicalization-Anwendung vor der abschließenden Entfernung der
         * zwölf bewusst verworfenen Legacy-Einträge.
         */
        val reviewBacklogResult =
            reviewBacklogEvaluator.evaluate(
                sourceEntries = auditResult.entries,
                normalizations = auditResult.normalizations,
                plan = auditResult.canonicalizationPlan,
                applicationResult = applicationResult,
                categoryRegistry = categoryRegistry
            )

        /*
         * Erste Klassifikation vor der fachlichen Typo-Reclassification.
         * Diese Version bleibt Eingabe der Typo-Diagnose.
         */
        val initialReviewBacklogClassificationResult =
            reviewBacklogClassifier.classify(
                backlogResult = reviewBacklogResult
            )

        val remainingTypoDiagnosticsResult =
            remainingTypoCandidateDiagnostics.diagnose(
                classificationResult =
                    initialReviewBacklogClassificationResult,
                sourceEntries = auditResult.entries,
                plan = auditResult.canonicalizationPlan
            )

        val misclassifiedTypoReclassificationResult =
            misclassifiedTypoReclassifier.reclassify(
                diagnosticsResult =
                    remainingTypoDiagnosticsResult
            )

        /*
         * Effektive Klassifikation:
         *
         * Die 23 TK-, Dosen- und Bio-Produktformen werden nicht mehr als
         * TYPO_VARIANT geführt, sondern als eigenständige Produktformen.
         */
        val reviewBacklogClassificationResult =
            typoReclassificationBacklogIntegrator.integrate(
                classificationResult =
                    initialReviewBacklogClassificationResult,
                reclassificationResult =
                    misclassifiedTypoReclassificationResult
            )

        /*
         * Ausschließlich die nach der Reclassification verbleibenden,
         * manuell bewerteten TYPO_VARIANT-Einträge werden entfernt.
         *
         * Wichtig:
         * Es wird die bestehende Source-Index-Zuordnung des Plan-Appliers
         * verwendet. Es werden keine künstlichen Indizes erzeugt.
         */
        val semanticTypoRemovalResult =
            unresolvedSemanticTypoRemover.remove(
                itemsBySourceIndex =
                    applicationResult.outputItemsBySourceIndex,
                effectiveClassificationResult =
                    reviewBacklogClassificationResult
            )

        /*
         * Dies ist der finale Katalogbestand.
         *
         * Erst nach der Removal-Phase dürfen Validierung und Katalog-Writer
         * ausgeführt werden.
         */
        val finalOutputItems =
            semanticTypoRemovalResult
                .outputItemsBySourceIndex
                .values
                .sortedWith(
                    FINAL_OUTPUT_ITEM_COMPARATOR
                )

        val validationResult =
            validator.validate(
                items = finalOutputItems,
                categoryRegistry = categoryRegistry
            )

        require(validationResult.valid) {
            buildString {
                appendLine(
                    "Final normalized catalog validation failed after " +
                            "semantic typo removal."
                )

                appendLine(
                    "applicationOutputEntryCount=" +
                            applicationResult.outputEntryCount
                )

                appendLine(
                    "semanticRemovalCandidateCount=" +
                            semanticTypoRemovalResult.candidateEntryCount
                )

                appendLine(
                    "semanticRemovedEntryCount=" +
                            semanticTypoRemovalResult.removedEntryCount
                )

                appendLine(
                    "semanticRemovalOutputEntryCount=" +
                            semanticTypoRemovalResult.outputEntryCount
                )

                appendLine(
                    "finalOutputItemsCount=" +
                            finalOutputItems.size
                )

                appendLine(
                    "removedSourceIndices=" +
                            semanticTypoRemovalResult
                                .removedSourceIndices
                                .joinToString(
                                    prefix = "[",
                                    postfix = "]"
                                )
                )

                append(
                    "validationResult="
                )

                append(validationResult)
            }
        }

        val reviewBacklogAnalysisResult =
            reviewBacklogAnalyzer.analyze(
                classificationResult =
                    reviewBacklogClassificationResult
            )

        val updatedReviewAnalysisResult =
            updatedReviewAnalyzer.analyze(
                previousAnalysis = null,
                currentAnalysis =
                    reviewBacklogAnalysisResult,
                implementedRecommendation =
                    CatalogReviewResolutionRecommendation
                        .IMPLEMENT_TYPO_VARIANT_RESOLVER
            )

        /*
         * Ausgabeverzeichnis vor allen Writer-Aufrufen sicherstellen.
         */
        if (!normalizedOutputDirectory.exists()) {
            require(normalizedOutputDirectory.mkdirs()) {
                "Could not create normalized catalog output directory: " +
                        normalizedOutputDirectory.absolutePath
            }
        }

        require(normalizedOutputDirectory.isDirectory) {
            "Normalized catalog output path is not a directory: " +
                    normalizedOutputDirectory.absolutePath
        }

        val normalizedCatalogFile =
            File(
                normalizedOutputDirectory,
                NORMALIZED_CATALOG_FILE_NAME
            )

        val reviewBacklogReportFile =
            File(
                normalizedOutputDirectory,
                REVIEW_BACKLOG_REPORT_FILE_NAME
            )

        val reviewBacklogClassificationReportFile =
            File(
                normalizedOutputDirectory,
                REVIEW_BACKLOG_CLASSIFICATION_REPORT_FILE_NAME
            )

        val reviewBacklogAnalysisReportFile =
            File(
                normalizedOutputDirectory,
                REVIEW_BACKLOG_ANALYSIS_REPORT_FILE_NAME
            )

        val updatedReviewAnalysisReportFile =
            File(
                normalizedOutputDirectory,
                UPDATED_REVIEW_ANALYSIS_REPORT_FILE_NAME
            )

        val remainingTypoDiagnosticsReportFile =
            File(
                normalizedOutputDirectory,
                REMAINING_TYPO_DIAGNOSTICS_REPORT_FILE_NAME
            )

        val misclassifiedTypoReclassificationReportFile =
            File(
                normalizedOutputDirectory,
                MISCLASSIFIED_TYPO_RECLASSIFICATION_REPORT_FILE_NAME
            )

        val semanticTypoRemovalReportFile =
            File(
                normalizedOutputDirectory,
                SEMANTIC_TYPO_REMOVAL_REPORT_FILE_NAME
            )

        /*
         * Finalen, bereits bereinigten Katalog schreiben.
         */
        catalogWriter.write(
            items = finalOutputItems,
            outputFile = normalizedCatalogFile
        )

        reviewBacklogReportWriter.write(
            result = reviewBacklogResult,
            outputFile = reviewBacklogReportFile
        )

        reviewBacklogClassificationReportWriter.write(
            result = reviewBacklogClassificationResult,
            outputFile =
                reviewBacklogClassificationReportFile
        )

        reviewBacklogAnalysisReportWriter.write(
            result = reviewBacklogAnalysisResult,
            outputFile = reviewBacklogAnalysisReportFile
        )

        updatedReviewAnalysisReportWriter.write(
            result = updatedReviewAnalysisResult,
            outputFile = updatedReviewAnalysisReportFile
        )

        remainingTypoDiagnosticsReportWriter.write(
            result = remainingTypoDiagnosticsResult,
            outputFile = remainingTypoDiagnosticsReportFile
        )

        misclassifiedTypoReclassificationReportWriter.write(
            result =
                misclassifiedTypoReclassificationResult,
            outputFile =
                misclassifiedTypoReclassificationReportFile
        )

        semanticTypoRemovalReportWriter.write(
            result = semanticTypoRemovalResult,
            outputFile = semanticTypoRemovalReportFile
        )

        val valid =
            auditResult.valid &&
                    applicationResult.valid &&
                    semanticTypoRemovalResult.valid &&
                    validationResult.valid &&
                    reviewBacklogResult.valid &&
                    initialReviewBacklogClassificationResult.valid &&
                    reviewBacklogClassificationResult.valid &&
                    reviewBacklogAnalysisResult.valid &&
                    updatedReviewAnalysisResult.valid &&
                    remainingTypoDiagnosticsResult.valid &&
                    misclassifiedTypoReclassificationResult.valid &&
                    isNonEmptyFile(normalizedCatalogFile) &&
                    isNonEmptyFile(reviewBacklogReportFile) &&
                    isNonEmptyFile(
                        reviewBacklogClassificationReportFile
                    ) &&
                    isNonEmptyFile(reviewBacklogAnalysisReportFile) &&
                    isNonEmptyFile(updatedReviewAnalysisReportFile) &&
                    isNonEmptyFile(remainingTypoDiagnosticsReportFile) &&
                    isNonEmptyFile(
                        misclassifiedTypoReclassificationReportFile
                    ) &&
                    isNonEmptyFile(semanticTypoRemovalReportFile)

        return CatalogNormalizationPipelineResult(
            auditResult = auditResult,
            applicationResult = applicationResult,
            validationResult = validationResult,

            reviewBacklogResult =
                reviewBacklogResult,

            reviewBacklogClassificationResult =
                reviewBacklogClassificationResult,

            reviewBacklogAnalysisResult =
                reviewBacklogAnalysisResult,

            updatedReviewAnalysisResult =
                updatedReviewAnalysisResult,

            remainingTypoDiagnosticsResult =
                remainingTypoDiagnosticsResult,

            misclassifiedTypoReclassificationResult =
                misclassifiedTypoReclassificationResult,

            semanticTypoRemovalResult =
                semanticTypoRemovalResult,

            normalizedCatalogFile =
                normalizedCatalogFile,

            reviewBacklogReportFile =
                reviewBacklogReportFile,

            reviewBacklogClassificationReportFile =
                reviewBacklogClassificationReportFile,

            reviewBacklogAnalysisReportFile =
                reviewBacklogAnalysisReportFile,

            updatedReviewAnalysisReportFile =
                updatedReviewAnalysisReportFile,

            remainingTypoDiagnosticsReportFile =
                remainingTypoDiagnosticsReportFile,

            misclassifiedTypoReclassificationReportFile =
                misclassifiedTypoReclassificationReportFile,

            semanticTypoRemovalReportFile =
                semanticTypoRemovalReportFile,

            valid = valid
        )
    }

    private fun isNonEmptyFile(
        file: File
    ): Boolean =
        file.isFile &&
                file.length() > 0L

    private companion object {

        val FINAL_OUTPUT_ITEM_COMPARATOR =
            compareBy<CatalogFoodItem>(
                {
                    it.category
                        ?.lowercase(Locale.ROOT)
                        ?: ""
                },
                {
                    it.itemname.lowercase(
                        Locale.GERMAN
                    )
                },
                {
                    it.normalized
                        ?.lowercase(Locale.ROOT)
                        ?: ""
                }
            )

        const val NORMALIZED_CATALOG_FILE_NAME =
            "catalog.normalized.json"

        const val REVIEW_BACKLOG_REPORT_FILE_NAME =
            "catalog-review-backlog-evaluation.json"

        const val REVIEW_BACKLOG_CLASSIFICATION_REPORT_FILE_NAME =
            "catalog-review-backlog-classification.json"

        const val REVIEW_BACKLOG_ANALYSIS_REPORT_FILE_NAME =
            "catalog-review-backlog-analysis.json"

        const val UPDATED_REVIEW_ANALYSIS_REPORT_FILE_NAME =
            "catalog-review-backlog-updated-analysis.json"

        const val REMAINING_TYPO_DIAGNOSTICS_REPORT_FILE_NAME =
            "catalog-remaining-typo-candidate-diagnostics.json"

        const val MISCLASSIFIED_TYPO_RECLASSIFICATION_REPORT_FILE_NAME =
            "catalog-misclassified-typo-reclassification.json"

        const val SEMANTIC_TYPO_REMOVAL_REPORT_FILE_NAME =
            "catalog-unresolved-semantic-typo-removals.json"
    }
}