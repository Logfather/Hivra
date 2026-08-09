package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.application.CatalogCanonicalizationApplicationStatus
import de.shopme.testing.system.tools.knowledge.catalog.category.CanonicalFoodCategoryRegistry
import de.shopme.testing.system.tools.knowledge.catalog.duplicate.resolution.diagnostics.CatalogTypoCandidateSubtype
import de.shopme.testing.system.tools.knowledge.catalog.review.CatalogReviewBacklogStatus
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewBacklogClassification
import de.shopme.testing.system.tools.knowledge.catalog.validation.NormalizedCatalogValidationIssueType
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RunCanonicalFoodCatalogNormalizationTest {

    @Test
    fun runCanonicalFoodCatalogNormalization() {
        val projectRoot = resolveProjectRoot()

        val catalogFile = resolveCatalogFile(
            projectRoot = projectRoot
        )

        val auditOutputDirectory = File(
            projectRoot,
            AUDIT_OUTPUT_DIRECTORY
        )

        val normalizedOutputDirectory = File(
            projectRoot,
            NORMALIZED_OUTPUT_DIRECTORY
        )

        prepareOutputDirectory(
            directory = auditOutputDirectory
        )

        prepareOutputDirectory(
            directory = normalizedOutputDirectory
        )

        val categoryRegistry =
            CanonicalFoodCategoryRegistry()

        val normalizationPipeline =
            CatalogNormalizationPipelineFactory.create(
                categoryRegistry =
                    categoryRegistry
            )

        val result = normalizationPipeline.run(
            catalogFile = catalogFile,
            auditOutputDirectory = auditOutputDirectory,
            normalizedOutputDirectory =
                normalizedOutputDirectory
        )

        validateResult(
            result = result,
            catalogFile = catalogFile,
            auditOutputDirectory =
                auditOutputDirectory,
            normalizedOutputDirectory =
                normalizedOutputDirectory
        )

        printResult(
            result = result,
            catalogFile = catalogFile
        )
    }

    private fun validateResult(
        result: CatalogNormalizationPipelineResult,
        catalogFile: File,
        auditOutputDirectory: File,
        normalizedOutputDirectory: File
    ) {
        val auditResult = result.auditResult
        val applicationResult =
            result.applicationResult
        val validationResult =
            result.validationResult


        assertTrue(
            result.remainingTypoDiagnosticsResult.valid,
            "Remaining typo diagnostics are invalid."
        )

        val remainingTypoClassificationCount =
            result.reviewBacklogClassificationResult
                .countsByClassification[
                CatalogReviewBacklogClassification
                    .TYPO_VARIANT
            ] ?: 0

        val effectiveTypoCandidateCount: Int =
            result.reviewBacklogClassificationResult
                .countsByClassification[
                CatalogReviewBacklogClassification
                    .TYPO_VARIANT
            ] ?: 0

        val reclassifiedTypoCandidateCount: Int =
            result.misclassifiedTypoReclassificationResult
                .reclassifiedEntryCount

        val initiallyDiagnosedTypoCandidateCount: Int =
            result.remainingTypoDiagnosticsResult
                .remainingTypoCandidateCount

        assertEquals(
            expected =
                initiallyDiagnosedTypoCandidateCount,
            actual =
                effectiveTypoCandidateCount +
                        reclassifiedTypoCandidateCount,
            message =
                "Initial typo diagnostics are inconsistent with the effective " +
                        "typo backlog and reclassified product variants."
        )

        val effectiveFrozenFormVariantCount: Int =
            result.reviewBacklogClassificationResult
                .countsByClassification[
                CatalogReviewBacklogClassification
                    .FROZEN_FORM_VARIANT
            ] ?: 0

        val effectiveCannedFormVariantCount: Int =
            result.reviewBacklogClassificationResult
                .countsByClassification[
                CatalogReviewBacklogClassification
                    .CANNED_FORM_VARIANT
            ] ?: 0

        val effectiveBioAttributeVariantCount: Int =
            result.reviewBacklogClassificationResult
                .countsByClassification[
                CatalogReviewBacklogClassification
                    .BIO_ATTRIBUTE_VARIANT
            ] ?: 0

        assertEquals(
            expected = 20,
            actual = effectiveFrozenFormVariantCount,
            message =
                "Unexpected effective frozen-form variant count."
        )

        assertEquals(
            expected = 2,
            actual = effectiveCannedFormVariantCount,
            message =
                "Unexpected effective canned-form variant count."
        )

        assertEquals(
            expected = 1,
            actual = effectiveBioAttributeVariantCount,
            message =
                "Unexpected effective bio-attribute variant count."
        )

        assertEquals(
            expected = 12,
            actual = effectiveTypoCandidateCount,
            message =
                "Unexpected remaining effective typo candidate count."
        )

        assertEquals(
            result.remainingTypoDiagnosticsResult
                .remainingTypoCandidateCount,
            result.remainingTypoDiagnosticsResult
                .diagnosedEntryCount
        )

        assertTrue(
            result.remainingTypoDiagnosticsReportFile.isFile,
            "Remaining typo diagnostics report was not generated: " +
                    result.remainingTypoDiagnosticsReportFile.absolutePath
        )

        assertTrue(
            result.remainingTypoDiagnosticsReportFile.length() > 0L,
            "Remaining typo diagnostics report must not be empty."
        )

        assertEquals(
            "catalog-remaining-typo-candidate-diagnostics.json",
            result.remainingTypoDiagnosticsReportFile.name
        )

        assertTrue(
            catalogFile.isFile,
            "Canonical catalog input file must still exist after " +
                    "normalization."
        )

        assertTrue(
            catalogFile.canRead(),
            "Canonical catalog input file must remain readable."
        )

        /*
         * Der Normalisierungslauf darf die historische Eingabedatei nicht
         * überschreiben. Der erzeugte Kandidatenkatalog muss immer in einem
         * separaten Generated-Verzeichnis liegen.
         */
        assertFalse(
            catalogFile.canonicalFile ==
                    result.normalizedCatalogFile.canonicalFile,
            "Normalized catalog must not overwrite the source catalog."
        )

        assertEquals(
            catalogFile.canonicalFile,
            auditResult.inputCatalogFile.canonicalFile,
            "Audit result references an unexpected input catalog."
        )

        assertEquals(
            auditOutputDirectory.canonicalFile,
            auditResult.outputDirectory.canonicalFile,
            "Audit reports were written to an unexpected directory."
        )

        assertEquals(
            normalizedOutputDirectory.canonicalFile,
            result.normalizedCatalogFile
                .parentFile
                .canonicalFile,
            "Normalized catalog was written to an unexpected directory."
        )

        assertEquals(
            NORMALIZED_CATALOG_FILE_NAME,
            result.normalizedCatalogFile.name,
            "Unexpected normalized catalog filename."
        )

        assertTrue(
            result.normalizedCatalogFile.isFile,
            "Normalized catalog was not created: " +
                    result.normalizedCatalogFile.absolutePath
        )

        assertTrue(
            result.normalizedCatalogFile.canRead(),
            "Normalized catalog is not readable: " +
                    result.normalizedCatalogFile.absolutePath
        )

        assertTrue(
            result.normalizedCatalogFile.length() > 0L,
            "Normalized catalog must not be empty."
        )

        val normalizedJson =
            result.normalizedCatalogFile.readText(
                StandardCharsets.UTF_8
            )

        assertTrue(
            normalizedJson.trimStart().startsWith("["),
            "Normalized catalog must contain a JSON array."
        )

        assertTrue(
            normalizedJson.trimEnd().endsWith("]"),
            "Normalized catalog JSON array is not closed."
        )

        assertTrue(
            normalizedJson.endsWith(
                System.lineSeparator()
            ),
            "Normalized catalog must end with one line separator."
        )

        /*
         * Jeder Input-Eintrag muss im Audit, im Normalisierungsergebnis und
         * im Application-Report eindeutig repräsentiert sein.
         */
        assertEquals(
            auditResult.inputEntryCount,
            auditResult.entries.size,
            "Audit result must retain every input entry."
        )

        assertEquals(
            auditResult.inputEntryCount,
            auditResult.normalizations.size,
            "Every catalog entry must have one normalization result."
        )

        assertEquals(
            auditResult.inputEntryCount,
            auditResult.canonicalizationPlan.planEntryCount,
            "Every catalog entry must have one canonicalization action."
        )

        assertEquals(
            auditResult.inputEntryCount,
            applicationResult.inputEntryCount,
            "Application result input count differs from the audit input."
        )

        assertEquals(
            applicationResult.inputEntryCount,
            applicationResult.entries.size,
            "Every input entry must have one application entry."
        )

        assertEquals(
            applicationResult.outputEntryCount,
            applicationResult.outputItems.size,
            "Application outputEntryCount is inconsistent."
        )

        assertEquals(
            expected =
                result.semanticTypoRemovalResult
                    .outputEntryCount,
            actual =
                result.validationResult
                    .inputEntryCount,
            message =
                "Validation must inspect every final output item after semantic " +
                        "typo removal."
        )

        assertEquals(
            expected =
                result.applicationResult
                    .outputEntryCount -
                        result.semanticTypoRemovalResult
                            .removedEntryCount,
            actual =
                result.validationResult
                    .inputEntryCount,
            message =
                "Validation input count is inconsistent with semantic typo removal."
        )

        assertEquals(
            applicationResult.inputEntryCount,
            applicationResult.appliedEntryCount +
                    applicationResult.skippedReviewEntryCount +
                    applicationResult.removedEntryCount +
                    applicationResult.mergedEntryCount +
                    applicationResult.failedEntryCount,
            "Application status counts must cover every input entry."
        )

        assertEquals(
            applicationResult.entries
                .map { it.sourceIndex }
                .distinct()
                .size,
            applicationResult.entries.size,
            "Application entries must have unique sourceIndex values."
        )

        assertEquals(
            applicationResult.entries
                .map { it.sourceIndex }
                .sorted(),
            applicationResult.entries
                .map { it.sourceIndex },
            "Application entries must be sorted by sourceIndex."
        )

        assertEquals(
            applicationResult.appliedEntryCount,
            applicationResult.entries.count {
                it.status ==
                        CatalogCanonicalizationApplicationStatus
                            .APPLIED
            }
        )

        assertEquals(
            applicationResult.skippedReviewEntryCount,
            applicationResult.entries.count {
                it.status ==
                        CatalogCanonicalizationApplicationStatus
                            .SKIPPED_REVIEW_REQUIRED
            }
        )

        assertEquals(
            applicationResult.removedEntryCount,
            applicationResult.entries.count {
                it.status ==
                        CatalogCanonicalizationApplicationStatus
                            .REMOVED
            }
        )

        assertEquals(
            applicationResult.mergedEntryCount,
            applicationResult.entries.count {
                it.status ==
                        CatalogCanonicalizationApplicationStatus
                            .MERGED_INTO_TARGET
            }
        )

        assertEquals(
            applicationResult.failedEntryCount,
            applicationResult.entries.count {
                it.status ==
                        CatalogCanonicalizationApplicationStatus
                            .FAILED
            }
        )

        assertTrue(
            applicationResult.valid,
            buildString {
                append(
                    "Canonicalization plan application failed."
                )

                applicationResult.entries
                    .filter {
                        it.status ==
                                CatalogCanonicalizationApplicationStatus
                                    .FAILED
                    }
                    .take(MAXIMUM_PRINTED_ISSUES)
                    .forEach { entry ->
                        appendLine()
                        append("- ")
                        append(entry.sourceIndex)
                        append(": ")
                        append(entry.originalItemName)
                        append(" -> ")
                        append(
                            entry.reasons.joinToString()
                        )
                    }
            }
        )

        assertEquals(
            0,
            applicationResult.failedEntryCount,
            "No canonicalization application may fail."
        )

        /*
         * Automatisch entfernte und zusammengeführte Einträge dürfen im
         * Output nicht mehr als eigene Katalogeinträge erscheinen.
         *
         * Review-Fälle bleiben hingegen bewusst erhalten.
         */
        assertEquals(
            applicationResult.inputEntryCount -
                    applicationResult.removedEntryCount -
                    applicationResult.mergedEntryCount,
            applicationResult.outputEntryCount,
            "Output count must equal input minus removals and merges."
        )

        assertTrue(
            applicationResult.outputItems.none {
                it.itemname.isBlank()
            },
            "Generated catalog contains blank item names."
        )

        assertTrue(
            applicationResult.outputItems.all {
                it.colloquial.none(String::isBlank) &&
                        it.phoneticTokens.none(String::isBlank) &&
                        it.autocompleteTokens.none(String::isBlank)
            },
            "Generated catalog contains blank list values."
        )

        assertEquals(
            validationResult.issueCount,
            validationResult.issues.size,
            "Validation issueCount is inconsistent."
        )

        assertEquals(
            validationResult.issueCount,
            validationResult.issueCountsByType
                .values
                .sum(),
            "Validation issueCountsByType is inconsistent."
        )

        assertEquals(
            validationResult.valid,
            validationResult.issues.isEmpty(),
            "Validation valid flag is inconsistent."
        )

        /*
         * Der erste Apply-Commit darf noch Review-Fälle und verbleibende
         * Zielverletzungen enthalten. Deshalb wird hier bewusst nicht
         * validationResult.valid verlangt.
         *
         * Nicht toleriert werden jedoch strukturelle Fehler oder fehlende
         * Ausgabedateien.
         */
        assertEquals(
            result.applicationResult.valid &&
                    result.validationResult.valid &&
                    result.reviewBacklogResult.valid &&
                    result.reviewBacklogClassificationResult.valid &&
                    result.normalizedCatalogFile.isFile &&
                    result.normalizedCatalogFile.length() > 0L &&
                    result.reviewBacklogReportFile.isFile &&
                    result.reviewBacklogReportFile.length() > 0L &&
                    result.reviewBacklogClassificationReportFile.isFile &&
                    result.reviewBacklogClassificationReportFile.length() > 0L &&
                    result.reviewBacklogAnalysisResult.valid &&
                    result.reviewBacklogAnalysisReportFile.isFile &&
                    result.reviewBacklogAnalysisReportFile.length() > 0L &&
                    result.remainingTypoDiagnosticsResult.valid &&
                    result.remainingTypoDiagnosticsReportFile.isFile &&
                    result.remainingTypoDiagnosticsReportFile.length() > 0L,
            result.valid,
            "Normalization pipeline valid flag is inconsistent."
        )

        assertTrue(
            auditResult.reportFiles.values.all {
                it.isFile && it.length() > 0L
            },
            "Not all audit reports were generated."
        )

        assertTrue(
            auditResult.reportFiles.keys.containsAll(
                EXPECTED_AUDIT_REPORT_KEYS
            ),
            "Audit result is missing expected report mappings."
        )

        assertTrue(
            result.reviewBacklogReportFile.isFile,
            "Review backlog report was not generated: " +
                    result.reviewBacklogReportFile.absolutePath
        )

        assertTrue(
            result.reviewBacklogReportFile.length() > 0L,
            "Review backlog report must not be empty."
        )

        assertTrue(
            result.reviewBacklogResult.valid,
            "Review backlog evaluation is structurally invalid."
        )

        assertEquals(
            result.reviewBacklogResult
                .evaluatedBacklogEntryCount,
            result.reviewBacklogResult
                .resolvedEntryCount +
                    result.reviewBacklogResult
                        .unresolvedEntryCount,
            "Review backlog result counts are inconsistent."
        )

        assertEquals(
            result.reviewBacklogResult
                .stillSplitRequiredCount,
            result.reviewBacklogClassificationResult
                .splitRequiredCount,
            "Split classification count must equal unresolved split backlog count."
        )

        val unresolvedBacklogSourceIndices =
            result.reviewBacklogResult.entries
                .filter { backlogEntry ->
                    backlogEntry.status ==
                            CatalogReviewBacklogStatus
                                .STILL_REVIEW_REQUIRED ||
                            backlogEntry.status ==
                            CatalogReviewBacklogStatus
                                .STILL_SPLIT_REQUIRED
                }
                .mapTo(sortedSetOf()) {
                    it.sourceIndex
                }

        assertEquals(
            unresolvedBacklogSourceIndices,
            result.reviewBacklogClassificationResult
                .entries
                .mapTo(sortedSetOf()) {
                    it.sourceIndex
                },
            "Classification must cover exactly the unresolved backlog indices."
        )

        validationResult.issues.forEach { issue ->
            if (
                issue.type ==
                NormalizedCatalogValidationIssueType
                    .NON_DETERMINISTIC_ORDER
            ) {
                throw AssertionError(
                    "Generated normalized catalog is not " +
                            "deterministically ordered."
                )
            }
        }

        assertTrue(
            result.reviewBacklogClassificationResult.valid,
            "Review backlog classification result is invalid."
        )

        assertEquals(
            result.reviewBacklogResult.unresolvedEntryCount,
            result.reviewBacklogClassificationResult
                .sourceBacklogEntryCount,
            "Classification source count must equal unresolved backlog count."
        )

        assertEquals(
            result.reviewBacklogResult.unresolvedEntryCount,
            result.reviewBacklogClassificationResult
                .classifiedEntryCount,
            "Every unresolved review entry must be classified."
        )

        assertEquals(
            result.reviewBacklogClassificationResult
                .classifiedEntryCount,
            result.reviewBacklogClassificationResult
                .potentiallyDeterministicCount +
                    result.reviewBacklogClassificationResult
                        .manualReviewRequiredCount +
                    result.reviewBacklogClassificationResult
                        .conflictingEvidenceCount +
                    result.reviewBacklogClassificationResult
                        .splitRequiredCount,
            "Classification automation assessment counts are inconsistent."
        )

        assertEquals(
            result.reviewBacklogClassificationResult
                .classifiedEntryCount,
            result.reviewBacklogClassificationResult
                .countsByClassification
                .values
                .sum(),
            "Classification counts do not cover all unresolved entries."
        )

        assertEquals(
            result.reviewBacklogClassificationResult
                .classifiedEntryCount,
            result.reviewBacklogClassificationResult
                .countsByAutomationAssessment
                .values
                .sum(),
            "Automation assessment counts do not cover all unresolved entries."
        )

        assertEquals(
            result.reviewBacklogClassificationResult
                .classifiedEntryCount,
            result.reviewBacklogClassificationResult
                .countsByCategory
                .values
                .sum(),
            "Category counts do not cover all unresolved entries."
        )

        assertTrue(
            result.reviewBacklogClassificationReportFile.isFile,
            "Review backlog classification report was not generated: " +
                    result.reviewBacklogClassificationReportFile.absolutePath
        )

        assertTrue(
            result.reviewBacklogClassificationReportFile.length() > 0L,
            "Review backlog classification report must not be empty."
        )

        assertTrue(
            result.reviewBacklogAnalysisResult.valid,
            "Review backlog analysis result is invalid."
        )

        assertEquals(
            result.reviewBacklogClassificationResult
                .classifiedEntryCount,
            result.reviewBacklogAnalysisResult
                .classifiedEntryCount,
            "Analysis must cover every classified backlog entry."
        )

        assertEquals(
            result.reviewBacklogAnalysisResult
                .classifiedEntryCount,
            result.reviewBacklogAnalysisResult
                .potentiallyDeterministicCount +
                    result.reviewBacklogAnalysisResult
                        .manualReviewRequiredCount +
                    result.reviewBacklogAnalysisResult
                        .conflictingEvidenceCount +
                    result.reviewBacklogAnalysisResult
                        .splitRequiredCount,
            "Analysis assessment counts are inconsistent."
        )

        assertTrue(
            result.reviewBacklogAnalysisReportFile.isFile,
            "Review backlog analysis report was not generated: " +
                    result.reviewBacklogAnalysisReportFile.absolutePath
        )

        assertTrue(
            result.reviewBacklogAnalysisReportFile.length() > 0L,
            "Review backlog analysis report must not be empty."
        )

        assertEquals(
            "catalog-review-backlog-analysis.json",
            result.reviewBacklogAnalysisReportFile.name
        )

        assertTrue(
            result.updatedReviewAnalysisResult.valid,
            "Updated review analysis is invalid."
        )

        assertEquals(
            result.reviewBacklogAnalysisResult
                .classifiedEntryCount,
            result.updatedReviewAnalysisResult
                .classifiedEntryCount,
            "Updated analysis does not cover the current analysis."
        )

        assertTrue(
            result.updatedReviewAnalysisReportFile.isFile,
            "Updated review analysis report was not generated: " +
                    result.updatedReviewAnalysisReportFile.absolutePath
        )

        assertTrue(
            result.updatedReviewAnalysisReportFile.length() > 0L,
            "Updated review analysis report must not be empty."
        )

        assertEquals(
            "catalog-review-backlog-updated-analysis.json",
            result.updatedReviewAnalysisReportFile.name
        )

        assertTrue(
            result.misclassifiedTypoReclassificationResult.valid,
            "Misclassified typo reclassification is invalid."
        )

        assertEquals(
            result.remainingTypoDiagnosticsResult
                .countsBySubtype[
                CatalogTypoCandidateSubtype
                    .PROBABLY_MISCLASSIFIED
            ] ?: 0,
            result.misclassifiedTypoReclassificationResult
                .inputCandidateCount,
            "Not every misclassified typo candidate was reclassified."
        )

        assertTrue(
            result.misclassifiedTypoReclassificationReportFile.isFile,
            "Misclassified typo reclassification report was not written."
        )

        assertTrue(
            result.misclassifiedTypoReclassificationReportFile.length() > 0L
        )

        assertEquals(
            "catalog-misclassified-typo-reclassification.json",
            result.misclassifiedTypoReclassificationReportFile.name
        )

        val expectedMisclassifiedTypoCount: Int =
            result.remainingTypoDiagnosticsResult
                .countsBySubtype[
                CatalogTypoCandidateSubtype
                    .PROBABLY_MISCLASSIFIED
            ] ?: 0

        val actualMisclassifiedTypoCount: Int =
            result.misclassifiedTypoReclassificationResult
                .inputCandidateCount

        assertEquals(
            expected = expectedMisclassifiedTypoCount,
            actual = actualMisclassifiedTypoCount,
            message =
                "Not every misclassified typo candidate was reclassified."
        )

        assertEquals(
            expected =
                expectedMisclassifiedTypoCount,
            actual =
                result.misclassifiedTypoReclassificationResult
                    .reclassifiedEntryCount,
            message =
                "Reclassified entry count is inconsistent."
        )

        assertTrue(
            result.misclassifiedTypoReclassificationResult.valid,
            "Misclassified typo reclassification is invalid."
        )

        assertTrue(
            result.misclassifiedTypoReclassificationReportFile.isFile,
            "Misclassified typo reclassification report was not written: " +
                    result.misclassifiedTypoReclassificationReportFile.absolutePath
        )

        assertTrue(
            result.misclassifiedTypoReclassificationReportFile.length() > 0L,
            "Misclassified typo reclassification report must not be empty."
        )

        assertEquals(
            expected =
                "catalog-misclassified-typo-reclassification.json",
            actual =
                result.misclassifiedTypoReclassificationReportFile.name,
            message =
                "Unexpected typo reclassification report filename."
        )

        val effectiveTypoCount =
            result.reviewBacklogClassificationResult
                .countsByClassification[
                CatalogReviewBacklogClassification
                    .TYPO_VARIANT
            ] ?: 0

        assertEquals(
            expected =
                result.remainingTypoDiagnosticsResult
                    .remainingTypoCandidateCount,
            actual =
                effectiveTypoCount +
                        result.misclassifiedTypoReclassificationResult
                            .reclassifiedEntryCount,
            message =
                "Effective typo backlog and reclassification do not cover the " +
                        "initial typo diagnostics."
        )

        assertEquals(
            expected = 20,
            actual =
                result.reviewBacklogClassificationResult
                    .countsByClassification[
                    CatalogReviewBacklogClassification
                        .FROZEN_FORM_VARIANT
                ] ?: 0,
            message =
                "Unexpected frozen-form variant count."
        )

        assertEquals(
            expected = 2,
            actual =
                result.reviewBacklogClassificationResult
                    .countsByClassification[
                    CatalogReviewBacklogClassification
                        .CANNED_FORM_VARIANT
                ] ?: 0,
            message =
                "Unexpected canned-form variant count."
        )

        assertEquals(
            expected = 1,
            actual =
                result.reviewBacklogClassificationResult
                    .countsByClassification[
                    CatalogReviewBacklogClassification
                        .BIO_ATTRIBUTE_VARIANT
                ] ?: 0,
            message =
                "Unexpected bio-attribute variant count."
        )

        assertTrue(
            result.semanticTypoRemovalResult.valid,
            "Semantic typo removal result is invalid."
        )

        assertEquals(
            expected = 12,
            actual =
                result.semanticTypoRemovalResult
                    .candidateEntryCount,
            message =
                "Unexpected unresolved semantic typo candidate count."
        )

        assertEquals(
            expected = 12,
            actual =
                result.semanticTypoRemovalResult
                    .removedEntryCount,
            message =
                "Not every unresolved semantic typo candidate was removed."
        )

        assertEquals(
            expected =
                result.semanticTypoRemovalResult
                    .inputEntryCount -
                        result.semanticTypoRemovalResult
                            .removedEntryCount,
            actual =
                result.semanticTypoRemovalResult
                    .outputEntryCount,
            message =
                "Semantic typo removal output count is inconsistent."
        )

        assertTrue(
            result.semanticTypoRemovalResult
                .removedSourceIndices
                .none {
                    it in
                            result.semanticTypoRemovalResult
                                .outputItemsBySourceIndex
                },
            "Removed semantic typo sourceIndex remains in output."
        )

        assertTrue(
            result.semanticTypoRemovalReportFile.isFile,
            "Semantic typo removal report was not written."
        )

        assertTrue(
            result.semanticTypoRemovalReportFile.length() > 0L,
            "Semantic typo removal report is empty."
        )

        assertEquals(
            expected =
                "catalog-unresolved-semantic-typo-removals.json",
            actual =
                result.semanticTypoRemovalReportFile.name,
            message =
                "Unexpected semantic typo removal report filename."
        )
    }

    private fun resolveCatalogFile(
        projectRoot: File
    ): File {
        val catalogFile =
            KnowledgeBuildPaths
                .default()
                .canonicalFoodCatalog

        require(catalogFile.exists()) {
            "Canonical food catalog does not exist: " +
                    catalogFile.absolutePath
        }

        require(catalogFile.isFile) {
            "Canonical food catalog path is not a file: " +
                    catalogFile.absolutePath
        }

        require(catalogFile.canRead()) {
            "Canonical food catalog is not readable: " +
                    catalogFile.absolutePath
        }

        require(catalogFile.length() > 0L) {
            "Canonical food catalog is empty: " +
                    catalogFile.absolutePath
        }

        return catalogFile
    }

    private fun resolveProjectRoot(): File {

        val userDir =
            requireNotNull(
                System.getProperty("user.dir")
            ) {
                "System property 'user.dir' is not available."
            }

        val startDirectory =
            File(userDir)
                .canonicalFile

        val candidates =
            generateSequence(
                startDirectory
            ) { current ->
                current.parentFile
            }
                .toList()

        return candidates
            .firstOrNull { candidate ->

                File(
                    candidate,
                    "data/catalog/canonical-food-catalog.json"
                ).isFile &&
                        File(
                            candidate,
                            "app"
                        ).isDirectory &&
                        File(
                            candidate,
                            "gradlew"
                        ).isFile
            }
            ?: throw IllegalArgumentException(
                buildString {
                    appendLine(
                        "Could not locate ShopMe project root."
                    )
                    appendLine("Start directory:")
                    appendLine(
                        "- ${startDirectory.absolutePath}"
                    )
                    appendLine("Checked candidates:")

                    candidates.forEach { candidate ->
                        appendLine(
                            "- ${candidate.absolutePath}"
                        )
                    }
                }.trimEnd()
            )
    }

    private fun prepareOutputDirectory(
        directory: File
    ) {
        require(
            !directory.exists() ||
                    directory.isDirectory
        ) {
            "Output path is not a directory: " +
                    directory.absolutePath
        }

        if (!directory.exists()) {
            require(directory.mkdirs()) {
                "Could not create output directory: " +
                        directory.absolutePath
            }
        }

        require(directory.canWrite()) {
            "Output directory is not writable: " +
                    directory.absolutePath
        }
    }

    private fun printResult(
        result: CatalogNormalizationPipelineResult,
        catalogFile: File
    ) {
        val application =
            result.applicationResult

        val validation =
            result.validationResult

        println()
        println("Canonical food catalog normalization")
        println("------------------------------------")
        println(
            "Input catalog: ${catalogFile.absolutePath}"
        )
        println(
            "Normalized catalog: " +
                    result.normalizedCatalogFile.absolutePath
        )
        println(
            "Input entries: ${application.inputEntryCount}"
        )
        println(
            "Output entries: ${application.outputEntryCount}"
        )
        println(
            "Applied entries: ${application.appliedEntryCount}"
        )
        println(
            "Skipped review entries: " +
                    application.skippedReviewEntryCount
        )
        println(
            "Removed non-food entries: " +
                    application.removedEntryCount
        )
        println(
            "Merged duplicate entries: " +
                    application.mergedEntryCount
        )
        println(
            "Failed applications: " +
                    application.failedEntryCount
        )
        println(
            "Remaining validation issues: " +
                    validation.issueCount
        )
        println(
            "Application valid: ${application.valid}"
        )
        println(
            "Normalized catalog valid: ${validation.valid}"
        )
        println(
            "Pipeline valid: ${result.valid}"
        )

        if (validation.issueCountsByType.isNotEmpty()) {
            println()
            println("Remaining issues by type:")

            validation.issueCountsByType
                .toList()
                .sortedBy { it.first.name }
                .forEach { (type, count) ->
                    println("- ${type.name}: $count")
                }
        }

        if (validation.issues.isNotEmpty()) {
            println()
            println(
                "First remaining validation issues:"
            )

            validation.issues
                .take(MAXIMUM_PRINTED_ISSUES)
                .forEach { issue ->
                    println(
                        buildString {
                            append("- ")
                            append(issue.type.name)

                            issue.sourceIndex?.let {
                                append(" [")
                                append(it)
                                append("]")
                            }

                            issue.itemName?.let {
                                append(" ")
                                append(it)
                            }

                            issue.value?.let {
                                append(" -> ")
                                append(it)
                            }

                            append(": ")
                            append(issue.message)
                        }
                    )
                }
        }

        println()
    }

    private companion object {

        const val AUDIT_OUTPUT_DIRECTORY =
            "data/generated/knowledge/catalog/audit"

        const val NORMALIZED_OUTPUT_DIRECTORY =
            "data/generated/knowledge/catalog/normalized"

        const val NORMALIZED_CATALOG_FILE_NAME =
            "catalog.normalized.json"

        const val MAXIMUM_PRINTED_ISSUES = 25

        val EXPECTED_AUDIT_REPORT_KEYS = setOf(
            "quality",
            "duplicates",
            "category",
            "language",
            "non-food",
            "canonicalization",
            "normalization",
            "taxonomy-gaps"
        )
    }
}