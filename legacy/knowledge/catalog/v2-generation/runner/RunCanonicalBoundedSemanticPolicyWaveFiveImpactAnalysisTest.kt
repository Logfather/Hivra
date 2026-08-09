package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.expansion.approval
.CanonicalApprovedCatalogExpansionReportReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic
.CanonicalCatalogExpansionSemanticValidationReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch
.CanonicalSemanticPolicyImplementationBatchReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure
.CanonicalBoundedSemanticPolicyClosurePlanReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation
.CanonicalCuratedSemanticPolicyBatchManifestReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact
.CanonicalBoundedSemanticPolicyWaveFiveImpactAnalyzer
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact
.CanonicalBoundedSemanticPolicyWaveFiveImpactBaselineFactory
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact
.CanonicalSemanticPolicyBatchImpactAnalysis
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact
.CanonicalSemanticPolicyBatchImpactAnalysisReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact
.CanonicalSemanticPolicyBatchImpactAnalysisWriter
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact
.CanonicalSemanticPolicyBatchImpactBaselineReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact
.CanonicalSemanticPolicyBatchImpactBaselineWriter
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy
.CanonicalFamilyAxisSemanticPolicySetReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCanonicalBoundedSemanticPolicyWaveFiveImpactAnalysisTest {

    @Test
    fun analyzeBoundedSemanticPolicyWaveFiveImpact() {
        val projectDirectory =
            resolveProjectDirectory()

        val reportFile =
            File(
                projectDirectory,
                IMPACT_REPORT_PATH
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

        val expectedBaseline =
            CanonicalBoundedSemanticPolicyWaveFiveImpactBaselineFactory()
                .create()

        val baselineFile =
            File(
                projectDirectory,
                IMPACT_BASELINE_PATH
            )

        val baseline =
            if (
                baselineFile.isFile &&
                baselineFile.length() > 0L
            ) {
                val persisted =
                    CanonicalSemanticPolicyBatchImpactBaselineReader()
                        .read(baselineFile)

                require(
                    persisted ==
                            expectedBaseline
                ) {
                    "Persisted Wave-5 impact baseline has changed."
                }

                persisted
            } else {
                CanonicalSemanticPolicyBatchImpactBaselineWriter()
                    .write(
                        baseline =
                            expectedBaseline,

                        outputFile =
                            baselineFile
                    )

                expectedBaseline
            }

        val analysis =
            CanonicalBoundedSemanticPolicyWaveFiveImpactAnalyzer()
                .analyze(
                    baseline =
                        baseline,

                    closurePlan =
                        CanonicalBoundedSemanticPolicyClosurePlanReader()
                            .read(
                                File(
                                    projectDirectory,
                                    CLOSURE_PLAN_PATH
                                )
                            ),

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
                    CanonicalBoundedSemanticPolicyWaveFiveImpactBaselineFactory
                        .WAVE_KEY
        )

        require(
            analysis.generatedCandidateCount ==
                    9_304
        )

        require(
            analysis.acceptedBefore ==
                    2_347
        )

        require(
            analysis.rejectedBefore ==
                    2_341
        )

        require(
            analysis.reviewRequiredBefore ==
                    4_616
        )

        require(
            analysis.missingPolicyGapsBefore ==
                    1_051
        )

        require(
            analysis.materializedEntriesBefore ==
                    2_347
        )

        require(
            analysis.expandedCatalogEntriesBefore ==
                    5_766
        )

        require(
            analysis.policyCountBefore ==
                    684
        )

        require(
            analysis.closedPolicyGapCount >=
                    EXPECTED_PERSISTED_WAVE_POLICY_COUNT
        )

        require(
            analysis.policyCountDelta >=
                    EXPECTED_PERSISTED_WAVE_POLICY_COUNT
        )

        val persistedWavePolicyCount =
            analysis.curatedPolicyCountDelta +
                    analysis.notApplicablePolicyCountDelta

        require(
            persistedWavePolicyCount ==
                    EXPECTED_PERSISTED_WAVE_POLICY_COUNT
        ) {
            "Unexpected persisted Wave-5 policy composition: " +
                    "expected=$EXPECTED_PERSISTED_WAVE_POLICY_COUNT, " +
                    "actual=$persistedWavePolicyCount, " +
                    "curated=${analysis.curatedPolicyCountDelta}, " +
                    "notApplicable=${analysis.notApplicablePolicyCountDelta}."
        }

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
                    "Bounded semantic policy Wave 5 impact"
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

        const val EXPECTED_PERSISTED_WAVE_POLICY_COUNT =
            505

        const val CLOSURE_PLAN_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "policy-closure/" +
                    "canonical-bounded-semantic-policy-closure-plan.json"

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

        const val IMPACT_BASELINE_PATH =
            "data/generated/knowledge/catalog/expansion/impact/" +
                    "canonical-bounded-wave-005-impact-baseline.json"

        const val IMPACT_REPORT_PATH =
            "data/generated/knowledge/catalog/expansion/impact/" +
                    "canonical-bounded-wave-005-impact-analysis.json"

        val REQUIRED_PATHS =
            listOf(
                CLOSURE_PLAN_PATH,
                CURATED_MANIFEST_PATH,
                POLICY_SET_PATH,
                SEMANTIC_VALIDATION_PATH,
                POLICY_BATCH_PATH,
                APPROVED_EXPANSION_PATH
            )
    }
}