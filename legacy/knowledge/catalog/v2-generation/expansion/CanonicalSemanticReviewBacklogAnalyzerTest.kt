package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.analysis

import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate
.CanonicalCatalogExpansionCandidateGenerationResult
import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate
.CanonicalCatalogExpansionCandidateReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic
.CanonicalCatalogExpansionSemanticValidationReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic
.CanonicalCatalogExpansionSemanticValidationResult
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CanonicalSemanticReviewBacklogAnalyzerTest {

    @Test
    fun analyzeCompleteSemanticReviewBacklog() {
        val fixtures =
            readFixtures()

        val result =
            CanonicalSemanticReviewBacklogAnalyzer()
                .analyze(
                    candidates =
                        fixtures.candidates,

                    semanticValidation =
                        fixtures.semanticValidation
                )

        assertTrue(
            result.valid,
            result.implementationBlockers.joinToString(
                separator =
                    System.lineSeparator()
            )
        )

        assertEquals(
            9_304,
            result.generatedCandidateCount
        )

        assertEquals(
            fixtures.semanticValidation
                .reviewRequiredCandidateCount,
            result.reviewRequiredCandidateCount,
            "Analysis must reflect the current semantic validation backlog."
        )

        assertEquals(
            fixtures.semanticValidation
                .acceptedCandidateCount,
            result.acceptedCandidateCount
        )

        assertEquals(
            fixtures.semanticValidation
                .rejectedCandidateCount,
            result.rejectedCandidateCount
        )

        assertEquals(
            fixtures.semanticValidation
                .evaluatedCandidateCount,
            result.evaluatedCandidateCount
        )

        assertEquals(
            result.generatedCandidateCount,
            result.acceptedCandidateCount +
                    result.rejectedCandidateCount +
                    result.reviewRequiredCandidateCount,
            "Semantic decision counts must cover every generated candidate."
        )

        val hasReviewBacklog =
            result.reviewRequiredCandidateCount >
                    0

        if (hasReviewBacklog) {
            assertTrue(
                result.reviewedCategoryCount >
                        0,
                "A non-empty review backlog must cover at least one category."
            )

            assertTrue(
                result.reviewedFamilyCount >
                        0,
                "A non-empty review backlog must cover at least one family."
            )

            assertTrue(
                result.reviewedAxisCount >
                        0,
                "A non-empty review backlog must cover at least one axis."
            )

            assertTrue(
                result.reviewedDistinctValueCount >
                        0,
                "A non-empty review backlog must contain at least one value."
            )
        } else {
            assertEquals(
                0,
                result.reviewedCategoryCount
            )

            assertEquals(
                0,
                result.reviewedFamilyCount
            )

            assertEquals(
                0,
                result.reviewedAxisCount
            )

            assertEquals(
                0,
                result.reviewedDistinctValueCount
            )
        }

        assertTrue(
            result.reviewedCategoryCount <=
                    result.reviewedFamilyCount,
            "Every reviewed category must contain at least one reviewed family."
        )

        assertTrue(
            result.reviewedFamilyCount <=
                    result.reviewRequiredCandidateCount,
            "Reviewed family count cannot exceed the number of " +
                    "review-required candidates."
        )

        val hasOpenPolicyGaps =
            result.missingFamilyAxisPolicyGapCount >
                    0

        assertEquals(
            hasOpenPolicyGaps,
            result.implementationReady,
            "Implementation readiness must match the presence of open " +
                    "family-axis policy gaps."
        )

        if (hasOpenPolicyGaps) {
            assertTrue(
                result.candidatesWithMissingFamilyAxisPolicyCount >
                        0,
                "Open family-axis policy gaps must affect at least one candidate."
            )

            assertNotNull(
                result.highestPriorityImplementationKey,
                "Open policy gaps must expose a highest-priority " +
                        "implementation key."
            )

            assertNotNull(
                result.highestPriorityRecommendation,
                "Open policy gaps must expose a highest-priority recommendation."
            )

            assertTrue(
                result.highestPriorityAffectedCandidateCount >
                        0,
                "The highest-priority policy gap must affect at least " +
                        "one candidate."
            )
        } else {
            assertEquals(
                0,
                result.candidatesWithMissingFamilyAxisPolicyCount
            )

            assertNull(
                result.highestPriorityImplementationKey
            )

            assertNull(
                result.highestPriorityRecommendation
            )

            assertEquals(
                0,
                result.highestPriorityAffectedCandidateCount
            )
        }

        assertTrue(
            result.completeReviewCoverage,
            "Semantic review analysis must cover the complete current backlog."
        )
    }

    @Test
    fun analyzeDeterministically() {
        val fixtures =
            readFixtures()

        val analyzer =
            CanonicalSemanticReviewBacklogAnalyzer()

        val first =
            analyzer.analyze(
                candidates =
                    fixtures.candidates,

                semanticValidation =
                    fixtures.semanticValidation
            )

        val second =
            analyzer.analyze(
                candidates =
                    fixtures.candidates,

                semanticValidation =
                    fixtures.semanticValidation
            )

        assertEquals(
            first,
            second
        )
    }

    private fun readFixtures(): Fixtures {
        val projectDirectory =
            resolveProjectDirectory()

        return Fixtures(
            candidates =
                CanonicalCatalogExpansionCandidateReader()
                    .read(
                        File(
                            projectDirectory,
                            CANDIDATE_PATH
                        )
                    ),

            semanticValidation =
                CanonicalCatalogExpansionSemanticValidationReader()
                    .read(
                        File(
                            projectDirectory,
                            SEMANTIC_VALIDATION_PATH
                        )
                    )
        )
    }

    private fun resolveProjectDirectory(): File {
        val workingDirectory =
            File(
                requireNotNull(
                    System.getProperty("user.dir")
                )
            ).canonicalFile

        fun containsRequiredArtifacts(
            directory: File
        ): Boolean =
            REQUIRED_PATHS.all { relativePath ->
                File(
                    directory,
                    relativePath
                ).isFile
            }

        return when {
            containsRequiredArtifacts(
                workingDirectory
            ) ->
                workingDirectory

            workingDirectory.name ==
                    "app" &&
                    containsRequiredArtifacts(
                        requireNotNull(
                            workingDirectory.parentFile
                        )
                    ) ->
                requireNotNull(
                    workingDirectory.parentFile
                )

            else ->
                error(
                    "Could not resolve project directory from: " +
                            workingDirectory.absolutePath
                )
        }
    }

    private data class Fixtures(
        val candidates:
        CanonicalCatalogExpansionCandidateGenerationResult,

        val semanticValidation:
        CanonicalCatalogExpansionSemanticValidationResult
    )

    private companion object {

        const val CANDIDATE_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-catalog-expansion-candidates.json"

        const val SEMANTIC_VALIDATION_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-catalog-expansion-semantic-validation.json"

        val REQUIRED_PATHS =
            listOf(
                CANDIDATE_PATH,
                SEMANTIC_VALIDATION_PATH
            )
    }
}