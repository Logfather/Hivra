package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate
.CanonicalCatalogExpansionCandidateReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic
.CanonicalCatalogExpansionSemanticValidationReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic
.analysis
.CanonicalSemanticReviewBacklogAnalysisReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic
.analysis
.CanonicalSemanticReviewBacklogAnalysisWriter
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic
.analysis
.CanonicalSemanticReviewBacklogAnalyzer
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCanonicalSemanticReviewBacklogAnalysisTest {

    @Test
    fun analyzeCanonicalSemanticValidationReviewBacklog() {
        val projectDirectory =
            resolveProjectDirectory()

        val candidates =
            CanonicalCatalogExpansionCandidateReader()
                .read(
                    File(
                        projectDirectory,
                        CANDIDATE_PATH
                    )
                )

        val semanticValidation =
            CanonicalCatalogExpansionSemanticValidationReader()
                .read(
                    File(
                        projectDirectory,
                        SEMANTIC_VALIDATION_PATH
                    )
                )

        val analysis =
            CanonicalSemanticReviewBacklogAnalyzer()
                .analyze(
                    candidates =
                        candidates,

                    semanticValidation =
                        semanticValidation
                )

        assertTrue(
            analysis.valid,
            "Semantic review backlog analysis is invalid: " +
                    analysis.implementationBlockers.joinToString()
        )

        val outputFile =
            File(
                projectDirectory,
                OUTPUT_PATH
            )

        CanonicalSemanticReviewBacklogAnalysisWriter()
            .write(
                analysis =
                    analysis,

                outputFile =
                    outputFile
            )

        val persisted =
            CanonicalSemanticReviewBacklogAnalysisReader()
                .read(outputFile)

        assertEquals(analysis, persisted)

        assertEquals(
            9_304,
            persisted.generatedCandidateCount
        )

        assertEquals(
            semanticValidation.reviewRequiredCandidateCount,
            persisted.reviewRequiredCandidateCount,
            "Review backlog analysis must use the current semantic validation result."
        )

        assertEquals(
            semanticValidation.acceptedCandidateCount,
            persisted.acceptedCandidateCount,
            "Accepted candidate count must match semantic validation."
        )

        assertEquals(
            semanticValidation.rejectedCandidateCount,
            persisted.rejectedCandidateCount,
            "Rejected candidate count must match semantic validation."
        )

        assertEquals(
            semanticValidation.evaluatedCandidateCount,
            persisted.evaluatedCandidateCount,
            "Evaluated candidate count must match semantic validation."
        )

        assertTrue(
            persisted.reviewedCategoryCount > 0,
            "Persisted review backlog must cover at least one category."
        )

        assertTrue(
            persisted.reviewedCategoryCount <=
                    persisted.reviewedFamilyCount,
            "Every persisted reviewed category must contain at least one reviewed family."
        )

        assertTrue(
            persisted.reviewedFamilyCount <=
                    persisted.reviewRequiredCandidateCount,
            "Persisted reviewed family count cannot exceed the current review backlog."
        )

        val hasOpenPolicyGaps =
            persisted.missingFamilyAxisPolicyGapCount >
                    0

        assertEquals(
            hasOpenPolicyGaps,
            persisted.implementationReady,
            "Implementation readiness must indicate whether open " +
                    "family-axis policy gaps remain."
        )

        assertTrue(
            persisted.completeReviewCoverage,
            "Semantic review analysis must cover the complete current backlog."
        )

        println(
            buildString {
                appendLine(
                    "Canonical semantic review backlog analysis"
                )
                appendLine(
                    "------------------------------------------"
                )
                appendLine(
                    "Generated candidates: " +
                            persisted.generatedCandidateCount
                )
                appendLine(
                    "Accepted candidates: " +
                            persisted.acceptedCandidateCount
                )
                appendLine(
                    "Rejected candidates: " +
                            persisted.rejectedCandidateCount
                )
                appendLine(
                    "Review required: " +
                            persisted.reviewRequiredCandidateCount
                )
                appendLine(
                    "Review-required share: " +
                            persisted.reviewRequiredShare
                )
                appendLine(
                    "Reviewed categories: " +
                            persisted.reviewedCategoryCount
                )
                appendLine(
                    "Reviewed families: " +
                            persisted.reviewedFamilyCount
                )
                appendLine(
                    "Reviewed axes: " +
                            persisted.reviewedAxisCount
                )
                appendLine(
                    "Reviewed distinct values: " +
                            persisted.reviewedDistinctValueCount
                )
                appendLine(
                    "Candidates with missing family-axis policy: " +
                            persisted
                                .candidatesWithMissingFamilyAxisPolicyCount
                )
                appendLine(
                    "Missing family-axis policy gaps: " +
                            persisted.missingFamilyAxisPolicyGapCount
                )
                appendLine(
                    "Highest-priority implementation: " +
                            persisted.highestPriorityImplementationKey
                )
                appendLine(
                    "Highest-priority recommendation: " +
                            persisted.highestPriorityRecommendation
                )
                appendLine(
                    "Highest-priority affected candidates: " +
                            persisted
                                .highestPriorityAffectedCandidateCount
                )
                appendLine(
                    "Highest-priority affected review share: " +
                            persisted
                                .highestPriorityAffectedReviewShare
                )
                appendLine(
                    "Implementation ready: " +
                            persisted.implementationReady
                )
                appendLine(
                    "Complete review coverage: " +
                            persisted.completeReviewCoverage
                )
                append(
                    "Analysis valid: " +
                            persisted.valid
                )
            }
        )
    }

    private fun resolveProjectDirectory(): File {
        val workingDirectory =
            File(
                requireNotNull(
                    System.getProperty("user.dir")
                )
            ).canonicalFile

        return when {
            File(
                workingDirectory,
                CANDIDATE_PATH
            ).isFile ->
                workingDirectory

            workingDirectory.name == "app" ->
                requireNotNull(
                    workingDirectory.parentFile
                )

            else ->
                error(
                    "Could not resolve ShopMe project directory from: " +
                            workingDirectory.absolutePath
                )
        }
    }

    private companion object {
        const val CANDIDATE_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-catalog-expansion-candidates.json"

        const val SEMANTIC_VALIDATION_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-catalog-expansion-semantic-validation.json"

        const val OUTPUT_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-catalog-expansion-semantic-review-" +
                    "backlog-analysis.json"
    }
}