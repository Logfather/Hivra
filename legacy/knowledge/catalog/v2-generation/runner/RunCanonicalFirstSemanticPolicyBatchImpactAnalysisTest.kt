package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.baseline.CanonicalFoodCatalogBaselineReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.approval.CanonicalApprovedCatalogExpansionReportReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.CanonicalCatalogExpansionSemanticValidationReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSemanticPolicyImplementationBatchReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact.CanonicalFirstSemanticPolicyBatchImpactAnalyzer
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact.CanonicalFirstSemanticPolicyBatchImpactBaselineFactory
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact.CanonicalFirstSemanticPolicyBatchImpactReference
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact.CanonicalSemanticPolicyBatchImpactAnalysis
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact.CanonicalSemanticPolicyBatchImpactAnalysisReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact.CanonicalSemanticPolicyBatchImpactAnalysisWriter
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact.CanonicalSemanticPolicyBatchImpactBaselineReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact.CanonicalSemanticPolicyBatchImpactBaselineWriter
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySetReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalInitialFamilyAxisSemanticPolicyFactory
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCanonicalFirstSemanticPolicyBatchImpactAnalysisTest {

    @Test
    fun analyzeFirstSemanticPolicyBatchImpact() {
        val projectDirectory =
            resolveProjectDirectory()

        val outputFile =
            File(
                projectDirectory,
                IMPACT_REPORT_PATH
            )

        if (
            outputFile.isFile &&
            outputFile.length() > 0L
        ) {
            val persistedAnalysis =
                CanonicalSemanticPolicyBatchImpactAnalysisReader()
                    .read(outputFile)

            CanonicalFirstSemanticPolicyBatchImpactReference
                .validate(
                    analysis =
                        persistedAnalysis
                )

            printAnalysis(
                analysis =
                    persistedAnalysis,
                mode =
                    "PRESERVED_FROZEN_RESULT"
            )

            return
        }

        val catalogBaseline =
            CanonicalFoodCatalogBaselineReader()
                .read(
                    File(
                        projectDirectory,
                        CATALOG_BASELINE_PATH
                    )
                )

        val expectedImpactBaseline =
            CanonicalFirstSemanticPolicyBatchImpactBaselineFactory()
                .create()

        val impactBaselineFile =
            File(
                projectDirectory,
                IMPACT_BASELINE_PATH
            )

        val impactBaseline =
            if (
                impactBaselineFile.isFile &&
                impactBaselineFile.length() > 0L
            ) {
                val persisted =
                    CanonicalSemanticPolicyBatchImpactBaselineReader()
                        .read(impactBaselineFile)

                require(
                    persisted ==
                            expectedImpactBaseline
                ) {
                    "Persisted first-batch impact baseline has changed."
                }

                persisted
            } else {
                CanonicalSemanticPolicyBatchImpactBaselineWriter()
                    .write(
                        baseline =
                            expectedImpactBaseline,
                        outputFile =
                            impactBaselineFile
                    )

                expectedImpactBaseline
            }

        val semanticValidation =
            CanonicalCatalogExpansionSemanticValidationReader()
                .read(
                    File(
                        projectDirectory,
                        SEMANTIC_VALIDATION_PATH
                    )
                )

        val policyBatches =
            CanonicalSemanticPolicyImplementationBatchReader()
                .read(
                    File(
                        projectDirectory,
                        POLICY_BATCH_PATH
                    )
                )

        val approvedExpansion =
            CanonicalApprovedCatalogExpansionReportReader()
                .read(
                    File(
                        projectDirectory,
                        APPROVED_EXPANSION_PATH
                    )
                )

        val basePolicySet =
            CanonicalInitialFamilyAxisSemanticPolicyFactory()
                .create(catalogBaseline)

        val currentPolicySet =
            CanonicalFamilyAxisSemanticPolicySetReader()
                .read(
                    File(
                        projectDirectory,
                        POLICY_SET_PATH
                    )
                )

        val analysis =
            CanonicalFirstSemanticPolicyBatchImpactAnalyzer()
                .analyze(
                    baseline =
                        impactBaseline,

                    currentSemanticValidation =
                        semanticValidation,

                    currentPolicyBatches =
                        policyBatches,

                    currentApprovedExpansion =
                        approvedExpansion,

                    basePolicySet =
                        basePolicySet,

                    currentPolicySet =
                        currentPolicySet
                )

        assertTrue(
            analysis.valid,
            analysis.blockers.joinToString()
        )

        CanonicalFirstSemanticPolicyBatchImpactReference
            .validate(
                analysis =
                    analysis
            )

        CanonicalSemanticPolicyBatchImpactAnalysisWriter()
            .write(
                analysis =
                    analysis,

                outputFile =
                    outputFile
            )

        assertTrue(outputFile.isFile)
        assertTrue(outputFile.length() > 0L)

        assertEquals(
            12,
            analysis.closedPolicyGapCount
        )

        printAnalysis(
            analysis =
                analysis,
            mode =
                "GENERATED_AND_FROZEN"
        )
    }

    private fun printAnalysis(
        analysis:
        CanonicalSemanticPolicyBatchImpactAnalysis,

        mode: String
    ) {
        println(
            buildString {
                appendLine(
                    "First semantic policy batch impact"
                )
                appendLine(
                    "----------------------------------"
                )
                appendLine(
                    "Analysis mode: $mode"
                )
                appendLine(
                    "Batch: ${analysis.batchKey}"
                )
                appendLine(
                    "Generated candidates: " +
                            analysis.generatedCandidateCount
                )
                appendLine(
                    "Accepted: " +
                            analysis.acceptedBefore +
                            " -> " +
                            analysis.acceptedAfter +
                            " (" +
                            signed(analysis.acceptedDelta) +
                            ")"
                )
                appendLine(
                    "Rejected: " +
                            analysis.rejectedBefore +
                            " -> " +
                            analysis.rejectedAfter +
                            " (" +
                            signed(analysis.rejectedDelta) +
                            ")"
                )
                appendLine(
                    "Review required: " +
                            analysis.reviewRequiredBefore +
                            " -> " +
                            analysis.reviewRequiredAfter +
                            " (" +
                            signed(
                                analysis.reviewRequiredDelta
                            ) +
                            ")"
                )
                appendLine(
                    "Resolved review candidates: " +
                            analysis.resolvedReviewCandidateCount
                )
                appendLine(
                    "Missing policy gaps: " +
                            analysis.missingPolicyGapsBefore +
                            " -> " +
                            analysis.missingPolicyGapsAfter +
                            " (" +
                            signed(
                                analysis.missingPolicyGapDelta
                            ) +
                            ")"
                )
                appendLine(
                    "Closed policy gaps: " +
                            analysis.closedPolicyGapCount
                )
                appendLine(
                    "Materialized entries: " +
                            analysis.materializedEntriesBefore +
                            " -> " +
                            analysis.materializedEntriesAfter +
                            " (" +
                            signed(
                                analysis.materializedEntryDelta
                            ) +
                            ")"
                )
                appendLine(
                    "Expanded catalog entries: " +
                            analysis.expandedCatalogEntriesBefore +
                            " -> " +
                            analysis.expandedCatalogEntriesAfter +
                            " (" +
                            signed(
                                analysis.expandedCatalogEntryDelta
                            ) +
                            ")"
                )
                appendLine(
                    "Policy count delta: " +
                            signed(
                                analysis.policyCountDelta
                            )
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

        return when {
            File(
                workingDirectory,
                SEMANTIC_VALIDATION_PATH
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
        const val CATALOG_BASELINE_PATH =
            "data/generated/knowledge/catalog/baseline/" +
                    "canonical-food-catalog-baseline.json"

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

        const val POLICY_SET_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-family-axis-semantic-policies.json"

        const val IMPACT_BASELINE_PATH =
            "data/generated/knowledge/catalog/expansion/impact/" +
                    "canonical-first-semantic-policy-batch-impact-baseline.json"

        const val IMPACT_REPORT_PATH =
            "data/generated/knowledge/catalog/expansion/impact/" +
                    "canonical-first-semantic-policy-batch-impact-analysis.json"
    }
}