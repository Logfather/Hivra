package de.shopme.testing.system.tools.knowledge.catalog.report

import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationAction
import de.shopme.testing.system.tools.knowledge.catalog.review.CatalogReviewBacklogStatus
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewAutomationAssessment
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewBacklogClassification
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewBacklogClassificationResult
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.ClassifiedCatalogReviewBacklogEntry
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CatalogReviewBacklogClassificationReportWriterTest {

    private val writer =
        CatalogReviewBacklogClassificationReportWriter()

    @Test
    fun writeClassificationReportDeterministically() {
        val outputDirectory =
            createTempDirectory(
                prefix = "catalog-review-classification-"
            ).toFile()

        try {
            val outputFile = File(
                outputDirectory,
                "catalog-review-backlog-classification.json"
            )

            val result = resultFixture()

            writer.write(
                result = result,
                outputFile = outputFile
            )

            val firstContent =
                outputFile.readText(Charsets.UTF_8)

            writer.write(
                result = result,
                outputFile = outputFile
            )

            val secondContent =
                outputFile.readText(Charsets.UTF_8)

            assertEquals(
                firstContent,
                secondContent
            )

            assertTrue(outputFile.isFile)
            assertTrue(outputFile.length() > 0L)

            assertTrue(
                firstContent.endsWith(
                    System.lineSeparator()
                )
            )

            assertFalse(
                firstContent.endsWith(
                    System.lineSeparator() +
                            System.lineSeparator()
                )
            )
        } finally {
            outputDirectory.deleteRecursively()
        }
    }

    private fun resultFixture():
            CatalogReviewBacklogClassificationResult {
        val entry =
            ClassifiedCatalogReviewBacklogEntry(
                sourceIndex = 10,
                itemName = "Curry Sauce",
                category = "sauces",
                normalizedKey = "curry-sauce",
                originalAction =
                    CatalogCanonicalizationAction.MERGE,
                backlogStatus =
                    CatalogReviewBacklogStatus
                        .STILL_REVIEW_REQUIRED,
                mergeTargetSourceIndex = 3,
                primaryClassification =
                    CatalogReviewBacklogClassification
                        .PUNCTUATION_VARIANT,
                detectedClassifications =
                    listOf(
                        CatalogReviewBacklogClassification
                            .PUNCTUATION_VARIANT
                    ),
                automationAssessment =
                    CatalogReviewAutomationAssessment
                        .POTENTIALLY_DETERMINISTIC,
                hasUniqueMergeTarget = true,
                hasMultipleConflictReasons = false,
                originalReasons =
                    listOf(
                        "Duplicate reasons: PUNCTUATION_VARIANT."
                    ),
                classificationReasons =
                    listOf(
                        "Automation assessment is " +
                                "POTENTIALLY_DETERMINISTIC.",
                        "Primary review classification is " +
                                "PUNCTUATION_VARIANT.",
                        "The review entry contains one explicit " +
                                "merge target."
                    ).sorted()
            )

        return CatalogReviewBacklogClassificationResult(
            version =
                CatalogReviewBacklogClassificationResult
                    .CURRENT_VERSION,
            sourceBacklogEntryCount = 1,
            classifiedEntryCount = 1,
            potentiallyDeterministicCount = 1,
            manualReviewRequiredCount = 0,
            conflictingEvidenceCount = 0,
            splitRequiredCount = 0,
            entriesWithUniqueMergeTargetCount = 1,
            entriesWithMultipleConflictReasonsCount = 0,
            countsByClassification =
                mapOf(
                    CatalogReviewBacklogClassification
                        .PUNCTUATION_VARIANT to 1
                ),
            countsByAutomationAssessment =
                mapOf(
                    CatalogReviewAutomationAssessment
                        .POTENTIALLY_DETERMINISTIC to 1
                ),
            countsByCategory =
                mapOf("sauces" to 1),
            entries = listOf(entry),
            valid = true
        )
    }
}