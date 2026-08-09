package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.expansion.approval
.CanonicalApprovedCatalogExpansionReportReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch
.CanonicalSemanticPolicyImplementationBatchReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact
.CanonicalSemanticPolicyBatchImpactAnalysisReader
import de.shopme.testing.system.tools.knowledge.catalog.finalization
.CanonicalFoodCatalogFinalizationReportReader
import de.shopme.testing.system.tools.knowledge.catalog.finalization
.CanonicalFoodCatalogFinalizationReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.finalization
.CanonicalFoodCatalogFinalizer
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCanonicalFoodCatalogFinalizationTest {

    @Test
    fun finalizeCanonicalFoodCatalog() {
        val projectDirectory =
            resolveProjectDirectory()

        val finalCatalogFile =
            File(
                projectDirectory,
                FINAL_CATALOG_PATH
            )

        val finalizationReportFile =
            File(
                projectDirectory,
                FINALIZATION_REPORT_PATH
            )

        val result =
            CanonicalFoodCatalogFinalizer()
                .finalize(
                    approvedExpansion =
                        CanonicalApprovedCatalogExpansionReportReader()
                            .read(
                                File(
                                    projectDirectory,
                                    APPROVED_EXPANSION_REPORT_PATH
                                )
                            ),

                    terminalPolicyBatches =
                        CanonicalSemanticPolicyImplementationBatchReader()
                            .read(
                                File(
                                    projectDirectory,
                                    POLICY_BATCH_PATH
                                )
                            ),

                    waveSixImpact =
                        CanonicalSemanticPolicyBatchImpactAnalysisReader()
                            .read(
                                File(
                                    projectDirectory,
                                    WAVE_SIX_IMPACT_PATH
                                )
                            ),

                    outputCatalogFile =
                        finalCatalogFile
                )

        assertTrue(
            result.valid,
            result.blockers.joinToString(
                separator =
                    System.lineSeparator()
            )
        )

        assertEquals(
            0,
            result.openImplementationBatchCount
        )

        assertEquals(
            0,
            result.openPolicyGapCount
        )

        assertEquals(
            result.finalCatalogEntryCount,
            result.uniqueCanonicalNameCount
        )

        assertEquals(
            result.finalCatalogEntryCount,
            result.uniqueNormalizedKeyCount
        )

        CanonicalFoodCatalogFinalizationReportWriter()
            .write(
                result =
                    result,

                outputFile =
                    finalizationReportFile
            )

        val persisted =
            CanonicalFoodCatalogFinalizationReportReader()
                .read(
                    finalizationReportFile
                )

        assertEquals(
            result,
            persisted
        )

        println(
            buildString {
                appendLine(
                    "Final canonical food catalog"
                )
                appendLine(
                    "----------------------------"
                )
                appendLine(
                    "Finalization: ${persisted.finalizationId}"
                )
                appendLine(
                    "Baseline entries: " +
                            persisted.baselineEntryCount
                )
                appendLine(
                    "Materialized entries: " +
                            persisted.materializedEntryCount
                )
                appendLine(
                    "Final catalog entries: " +
                            persisted.finalCatalogEntryCount
                )
                appendLine(
                    "Categories: " +
                            persisted.categoryCount
                )
                appendLine(
                    "Accepted candidates: " +
                            persisted.semanticAcceptedCandidateCount
                )
                appendLine(
                    "Rejected candidates: " +
                            persisted.semanticRejectedCandidateCount
                )
                appendLine(
                    "Review-required candidates: " +
                            persisted.semanticReviewRequiredCandidateCount
                )
                appendLine(
                    "Open implementation batches: " +
                            persisted.openImplementationBatchCount
                )
                appendLine(
                    "Open policy gaps: " +
                            persisted.openPolicyGapCount
                )
                appendLine(
                    "Unique canonical names: " +
                            persisted.uniqueCanonicalNames
                )
                appendLine(
                    "Unique normalized keys: " +
                            persisted.uniqueNormalizedKeys
                )
                appendLine(
                    "Deterministic order: " +
                            persisted.deterministicOrderValid
                )
                appendLine(
                    "Catalog SHA-256: " +
                            persisted.finalCatalogSha256
                )
                append(
                    "Finalization valid: " +
                            persisted.valid
                )
            }
        )
    }

    private fun resolveProjectDirectory(): File {
        val workingDirectory =
            File(
                requireNotNull(
                    System.getProperty(
                        "user.dir"
                    )
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

        const val APPROVED_EXPANSION_REPORT_PATH =
            "data/generated/knowledge/catalog/expansion/approved/" +
                    "canonical-food-catalog-expansion-report.json"

        const val POLICY_BATCH_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "policy-batches/" +
                    "canonical-semantic-policy-implementation-batches.json"

        const val WAVE_SIX_IMPACT_PATH =
            "data/generated/knowledge/catalog/expansion/impact/" +
                    "canonical-bounded-wave-006-impact-analysis.json"

        const val FINAL_CATALOG_PATH =
            "data/generated/knowledge/catalog/final/" +
                    "canonical-food-catalog.json"

        const val FINALIZATION_REPORT_PATH =
            "data/generated/knowledge/catalog/final/" +
                    "canonical-food-catalog-finalization.json"

        val REQUIRED_PATHS =
            listOf(
                APPROVED_EXPANSION_REPORT_PATH,
                POLICY_BATCH_PATH,
                WAVE_SIX_IMPACT_PATH
            )
    }
}