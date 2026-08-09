package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.expansion.approval.CanonicalApprovedCatalogExpansionReportReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.CanonicalCatalogExpansionSemanticValidationReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSemanticPolicyImplementationBatchReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation.CanonicalCuratedSemanticPolicyBatchManifestReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact.CanonicalSemanticPolicyBatchImpactAnalysis
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact.CanonicalSemanticPolicyBatchImpactAnalysisReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact.CanonicalSemanticPolicyBatchImpactAnalysisWriter
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact.CanonicalSemanticPolicyBatchImpactBaselineReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact.CanonicalSemanticPolicyBatchImpactBaselineWriter
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact.CanonicalThirdGeneralizedSemanticPolicyWaveImpactAnalyzer
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact.CanonicalThirdGeneralizedSemanticPolicyWaveImpactBaselineFactory
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact.CanonicalThirdGeneralizedSemanticPolicyWaveImpactReference
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySetReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCanonicalThirdGeneralizedSemanticPolicyWaveImpactAnalysisTest {

    @Test
    fun analyzeThirdGeneralizedSemanticPolicyWaveImpact() {
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

            CanonicalThirdGeneralizedSemanticPolicyWaveImpactReference
                .validate(persisted)

            printAnalysis(
                analysis =
                    persisted,

                mode =
                    "PRESERVED_FROZEN_RESULT"
            )

            return
        }

        val expectedBaseline =
            CanonicalThirdGeneralizedSemanticPolicyWaveImpactBaselineFactory()
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
                    "Persisted Wave-3 impact baseline has changed."
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

        val currentPolicySet =
            CanonicalFamilyAxisSemanticPolicySetReader()
                .read(
                    File(
                        projectDirectory,
                        POLICY_SET_PATH
                    )
                )

        val curatedManifest =
            CanonicalCuratedSemanticPolicyBatchManifestReader()
                .read(
                    File(
                        projectDirectory,
                        CURATED_MANIFEST_PATH
                    )
                )

        val analysis =
            CanonicalThirdGeneralizedSemanticPolicyWaveImpactAnalyzer()
                .analyze(
                    baseline =
                        baseline,

                    currentSemanticValidation =
                        semanticValidation,

                    currentPolicyBatches =
                        policyBatches,

                    currentApprovedExpansion =
                        approvedExpansion,

                    currentPolicySet =
                        currentPolicySet,

                    curatedManifest =
                        curatedManifest
                )

        assertTrue(
            analysis.valid,
            analysis.blockers.joinToString(
                separator =
                    System.lineSeparator()
            )
        )

        CanonicalThirdGeneralizedSemanticPolicyWaveImpactReference
            .validate(analysis)

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

    private fun printAnalysis(
        analysis:
        CanonicalSemanticPolicyBatchImpactAnalysis,

        mode: String
    ) {
        println(
            buildString {
                appendLine(
                    "Third generalized semantic policy wave impact"
                )
                appendLine(
                    "---------------------------------------------"
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
                ) {
                    "System property 'user.dir' is not available."
                }
            ).canonicalFile

        return when {
            File(
                workingDirectory,
                SEMANTIC_VALIDATION_PATH
            ).isFile ->
                workingDirectory

            workingDirectory.name ==
                    "app" ->
                requireNotNull(
                    workingDirectory.parentFile
                ) {
                    "Could not resolve project directory from app directory."
                }

            else ->
                error(
                    "Could not resolve ShopMe project directory from: " +
                            workingDirectory.absolutePath
                )
        }
    }

    private companion object {
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

        const val CURATED_MANIFEST_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "policy-curation/" +
                    "canonical-curated-semantic-policy-batches.json"

        const val IMPACT_BASELINE_PATH =
            "data/generated/knowledge/catalog/expansion/impact/" +
                    "canonical-third-generalized-wave-impact-baseline.json"

        const val IMPACT_REPORT_PATH =
            "data/generated/knowledge/catalog/expansion/impact/" +
                    "canonical-third-generalized-wave-impact-analysis.json"
    }
}