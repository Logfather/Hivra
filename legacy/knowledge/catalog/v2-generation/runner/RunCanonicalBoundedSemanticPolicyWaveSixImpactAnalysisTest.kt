package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.expansion.approval
.CanonicalApprovedCatalogExpansionReportReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic
.CanonicalCatalogExpansionSemanticValidationReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch
.CanonicalSemanticPolicyImplementationBatchReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation
.CanonicalCuratedSemanticPolicyBatchManifestReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact
.CanonicalBoundedSemanticPolicyWaveSixImpactAnalyzer
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact
.CanonicalSemanticPolicyBatchImpactAnalysis
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact
.CanonicalSemanticPolicyBatchImpactAnalysisReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact
.CanonicalSemanticPolicyBatchImpactAnalysisWriter
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy
.CanonicalFamilyAxisSemanticPolicySetReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCanonicalBoundedSemanticPolicyWaveSixImpactAnalysisTest {

    @Test
    fun analyzeBoundedSemanticPolicyWaveSixImpact() {
        val projectDirectory =
            resolveProjectDirectory()

        val reportFile =
            File(
                projectDirectory,
                WAVE_SIX_IMPACT_REPORT_PATH
            )

        if (
            reportFile.isFile &&
            reportFile.length() > 0L
        ) {
            val persisted =
                CanonicalSemanticPolicyBatchImpactAnalysisReader()
                    .read(reportFile)

            validatePersistedAnalysis(
                analysis =
                    persisted
            )

            printAnalysis(
                analysis =
                    persisted,

                mode =
                    "PRESERVED_FROZEN_RESULT"
            )

            return
        }

        val waveFiveImpact =
            CanonicalSemanticPolicyBatchImpactAnalysisReader()
                .read(
                    File(
                        projectDirectory,
                        WAVE_FIVE_IMPACT_REPORT_PATH
                    )
                )

        val analysis =
            CanonicalBoundedSemanticPolicyWaveSixImpactAnalyzer()
                .analyze(
                    waveFiveImpact =
                        waveFiveImpact,

                    curatedManifest =
                        CanonicalCuratedSemanticPolicyBatchManifestReader()
                            .read(
                                File(
                                    projectDirectory,
                                    CURATED_MANIFEST_PATH
                                )
                            ),

                    currentPolicySet =
                        CanonicalFamilyAxisSemanticPolicySetReader()
                            .read(
                                File(
                                    projectDirectory,
                                    POLICY_SET_PATH
                                )
                            ),

                    currentSemanticValidation =
                        CanonicalCatalogExpansionSemanticValidationReader()
                            .read(
                                File(
                                    projectDirectory,
                                    SEMANTIC_VALIDATION_PATH
                                )
                            ),

                    currentPolicyBatches =
                        CanonicalSemanticPolicyImplementationBatchReader()
                            .read(
                                File(
                                    projectDirectory,
                                    POLICY_BATCH_PATH
                                )
                            ),

                    currentApprovedExpansion =
                        CanonicalApprovedCatalogExpansionReportReader()
                            .read(
                                File(
                                    projectDirectory,
                                    APPROVED_EXPANSION_PATH
                                )
                            )
                )

        assertTrue(
            analysis.valid,
            analysis.blockers.joinToString(
                separator =
                    System.lineSeparator()
            )
        )

        validatePersistedAnalysis(
            analysis =
                analysis
        )

        CanonicalSemanticPolicyBatchImpactAnalysisWriter()
            .write(
                analysis =
                    analysis,

                outputFile =
                    reportFile
            )

        val persisted =
            CanonicalSemanticPolicyBatchImpactAnalysisReader()
                .read(reportFile)

        assertEquals(
            analysis,
            persisted
        )

        printAnalysis(
            analysis =
                persisted,

            mode =
                "GENERATED_AND_FROZEN"
        )
    }

    private fun validatePersistedAnalysis(
        analysis:
        CanonicalSemanticPolicyBatchImpactAnalysis
    ) {
        require(analysis.valid)

        require(
            analysis.batchKey ==
                    CanonicalBoundedSemanticPolicyWaveSixImpactAnalyzer
                        .WAVE_SIX_KEY
        )

        require(
            analysis.missingPolicyGapsBefore >
                    0
        )

        require(
            analysis.missingPolicyGapsAfter ==
                    0
        )

        require(
            analysis.closedPolicyGapCount ==
                    analysis.missingPolicyGapsBefore
        )

        require(
            analysis.policyCountDelta ==
                    analysis.closedPolicyGapCount
        )

        require(
            analysis.policyCountDelta >=
                    CanonicalBoundedSemanticPolicyWaveSixImpactAnalyzer
                        .EXPECTED_WAVE_SIX_POLICY_COUNT
        )

        require(
            analysis.curatedPolicyCountDelta +
                    analysis.notApplicablePolicyCountDelta ==
                    CanonicalBoundedSemanticPolicyWaveSixImpactAnalyzer
                        .EXPECTED_WAVE_SIX_POLICY_COUNT
        )

        require(
            analysis.missingPolicyGapsBefore ==
                    EXPECTED_REMAINING_POLICY_GAP_COUNT_BEFORE_WAVE_SIX
        )

        require(
            analysis.closedPolicyGapCount ==
                    EXPECTED_REMAINING_POLICY_GAP_COUNT_BEFORE_WAVE_SIX
        )

        require(
            analysis.policyCountDelta ==
                    EXPECTED_REMAINING_POLICY_GAP_COUNT_BEFORE_WAVE_SIX
        )

        require(analysis.semanticDecisionArithmeticValid)
        require(analysis.reviewBacklogReduced)
        require(analysis.expectedPolicyGapClosureReached)
        require(analysis.policyExpansionValid)
        require(analysis.catalogExpansionArithmeticValid)
    }

    private fun printAnalysis(
        analysis:
        CanonicalSemanticPolicyBatchImpactAnalysis,

        mode: String
    ) {
        println(
            buildString {
                appendLine(
                    "Bounded semantic policy Wave 6 impact"
                )
                appendLine(
                    "-------------------------------------"
                )
                appendLine(
                    "Analysis mode: $mode"
                )
                appendLine(
                    "Wave: ${analysis.batchKey}"
                )
                appendLine(
                    "Generated candidates: " +
                            analysis.generatedCandidateCount
                )
                appendLine(
                    "Accepted: ${analysis.acceptedBefore} -> " +
                            "${analysis.acceptedAfter} " +
                            "(${signed(analysis.acceptedDelta)})"
                )
                appendLine(
                    "Rejected: ${analysis.rejectedBefore} -> " +
                            "${analysis.rejectedAfter} " +
                            "(${signed(analysis.rejectedDelta)})"
                )
                appendLine(
                    "Review required: " +
                            "${analysis.reviewRequiredBefore} -> " +
                            "${analysis.reviewRequiredAfter} " +
                            "(${signed(analysis.reviewRequiredDelta)})"
                )
                appendLine(
                    "Resolved review candidates: " +
                            analysis.resolvedReviewCandidateCount
                )
                appendLine(
                    "Resolved review share: " +
                            analysis.resolvedReviewShare
                )
                appendLine(
                    "Missing policy gaps: " +
                            "${analysis.missingPolicyGapsBefore} -> " +
                            "${analysis.missingPolicyGapsAfter} " +
                            "(${signed(analysis.missingPolicyGapDelta)})"
                )
                appendLine(
                    "Closed policy gaps: " +
                            analysis.closedPolicyGapCount
                )
                appendLine(
                    "Materialized entries: " +
                            "${analysis.materializedEntriesBefore} -> " +
                            "${analysis.materializedEntriesAfter} " +
                            "(${signed(analysis.materializedEntryDelta)})"
                )
                appendLine(
                    "Expanded catalog entries: " +
                            "${analysis.expandedCatalogEntriesBefore} -> " +
                            "${analysis.expandedCatalogEntriesAfter} " +
                            "(${signed(analysis.expandedCatalogEntryDelta)})"
                )
                appendLine(
                    "Policy count: " +
                            "${analysis.policyCountBefore} -> " +
                            "${analysis.policyCountAfter} " +
                            "(${signed(analysis.policyCountDelta)})"
                )
                appendLine(
                    "Curated policy delta: " +
                            signed(
                                analysis.curatedPolicyCountDelta
                            )
                )
                appendLine(
                    "Not-applicable policy delta: " +
                            signed(
                                analysis.notApplicablePolicyCountDelta
                            )
                )
                appendLine(
                    "Terminal policy gaps: " +
                            analysis.missingPolicyGapsAfter
                )
                append(
                    "Impact analysis valid: " +
                            analysis.valid
                )
            }
        )
    }

    private fun signed(
        value: Int
    ): String =
        if (value >= 0) {
            "+$value"
        } else {
            value.toString()
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
                    "Could not resolve ShopMe project directory from: " +
                            workingDirectory.absolutePath
                )
        }
    }

    private companion object {

        const val EXPECTED_REMAINING_POLICY_GAP_COUNT_BEFORE_WAVE_SIX =
            40

        const val WAVE_FIVE_IMPACT_REPORT_PATH =
            "data/generated/knowledge/catalog/expansion/impact/" +
                    "canonical-bounded-wave-005-impact-analysis.json"

        const val WAVE_SIX_IMPACT_REPORT_PATH =
            "data/generated/knowledge/catalog/expansion/impact/" +
                    "canonical-bounded-wave-006-impact-analysis.json"

        const val CURATED_MANIFEST_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "policy-curation/" +
                    "canonical-curated-semantic-policy-batches.json"

        const val POLICY_SET_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-family-axis-semantic-policies.json"

        const val SEMANTIC_VALIDATION_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-catalog-expansion-semantic-validation.json"

        const val POLICY_BATCH_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "policy-batches/" +
                    "canonical-semantic-policy-implementation-batches.json"

        const val APPROVED_EXPANSION_PATH =
            "data/generated/knowledge/catalog/expansion/approved/" +
                    "canonical-food-catalog-expansion-report.json"

        val REQUIRED_PATHS =
            listOf(
                WAVE_FIVE_IMPACT_REPORT_PATH,
                CURATED_MANIFEST_PATH,
                POLICY_SET_PATH,
                SEMANTIC_VALIDATION_PATH,
                POLICY_BATCH_PATH,
                APPROVED_EXPANSION_PATH
            )
    }
}