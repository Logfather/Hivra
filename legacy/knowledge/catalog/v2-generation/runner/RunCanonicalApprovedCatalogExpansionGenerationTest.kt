package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.baseline
.CanonicalFoodCatalogBaselineReader
import de.shopme.testing.system.tools.knowledge.catalog.category
.CanonicalFoodCategoryRegistry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.approval
.CanonicalApprovedCatalogExpansionGenerator
import de.shopme.testing.system.tools.knowledge.catalog.expansion.approval
.CanonicalApprovedCatalogExpansionReportReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.approval
.CanonicalApprovedCatalogExpansionReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.expansion.approval
.CanonicalNormalizedCatalogReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate
.CanonicalCatalogExpansionCandidateReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic
.CanonicalCatalogExpansionSemanticValidationReader
import de.shopme.testing.system.tools.knowledge.catalog.normalization
.CanonicalFoodKeyNormalizer
import de.shopme.testing.system.tools.knowledge.catalog.validation
.NormalizedCatalogValidator
import de.shopme.testing.system.tools.knowledge.catalog.writer
.NormalizedCatalogWriter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCanonicalApprovedCatalogExpansionGenerationTest {

    @Test
    fun generateFirstApprovedCanonicalCatalogExpansion() {
        val projectDirectory =
            resolveProjectDirectory()

        val baseline =
            CanonicalFoodCatalogBaselineReader()
                .read(
                    File(
                        projectDirectory,
                        BASELINE_METADATA_PATH
                    )
                )

        val baselineItems =
            CanonicalNormalizedCatalogReader()
                .read(
                    File(
                        projectDirectory,
                        NORMALIZED_BASELINE_PATH
                    )
                )

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

        val result =
            CanonicalApprovedCatalogExpansionGenerator()
                .generate(
                    baseline = baseline,
                    baselineItems = baselineItems,
                    candidates = candidates,
                    semanticValidation = semanticValidation
                )

        assertTrue(
            result.valid,
            result.blockers.joinToString()
        )

        assertEquals(
            semanticValidation.acceptedCandidateCount,
            result.materializedEntryCount
        )

        val expandedCatalogFile =
            File(
                projectDirectory,
                EXPANDED_CATALOG_PATH
            )

        val reportFile =
            File(
                projectDirectory,
                EXPANSION_REPORT_PATH
            )

        NormalizedCatalogWriter()
            .write(
                items =
                    result.expandedCatalogItems,

                outputFile =
                    expandedCatalogFile
            )

        CanonicalApprovedCatalogExpansionReportWriter()
            .write(
                result = result,
                outputFile = reportFile
            )

        val persistedReport =
            CanonicalApprovedCatalogExpansionReportReader()
                .read(reportFile)

        assertEquals(result, persistedReport)

        val persistedExpandedItems =
            CanonicalNormalizedCatalogReader()
                .read(expandedCatalogFile)

        assertEquals(
            result.expandedCatalogItems,
            persistedExpandedItems
        )

        val validationResult =
            NormalizedCatalogValidator(
                keyNormalizer =
                    CanonicalFoodKeyNormalizer()
            )
                .validate(
                    items =
                        persistedExpandedItems,

                    categoryRegistry =
                        CanonicalFoodCategoryRegistry()
                )

        val duplicateCanonicalNameDiagnostics =
            persistedExpandedItems
                .groupBy { item ->
                    item.itemname
                        .trim()
                        .lowercase()
                }
                .filterValues { items ->
                    items.size > 1
                }
                .toSortedMap()
                .entries
                .joinToString(
                    separator =
                        System.lineSeparator()
                ) { (normalizedName, items) ->
                    buildString {
                        append(
                            "Duplicate canonical name '$normalizedName':"
                        )

                        items
                            .sortedWith(
                                compareBy {
                                    requireNotNull(it.normalized)
                                }
                            )
                            .forEach { item ->
                                append(
                                    System.lineSeparator()
                                )
                                append(
                                    "  itemname='${item.itemname}', " +
                                            "normalized='${item.normalized}', " +
                                            "category='${item.category}'"
                                )
                            }

                        val matchingMaterializations =
                            result.entries
                                .filter { entry ->
                                    entry.resultingCanonicalName
                                        ?.trim()
                                        ?.lowercase() ==
                                            normalizedName
                                }
                                .sortedBy { entry ->
                                    entry.candidateIndex
                                }

                        matchingMaterializations.forEach { entry ->
                            append(
                                System.lineSeparator()
                            )
                            append(
                                "  candidateIndex=${entry.candidateIndex}, " +
                                        "candidateKey='${entry.candidateKey}', " +
                                        "familyKey='${entry.familyKey}', " +
                                        "proposedName='${entry.proposedCanonicalName}', " +
                                        "proposedKey='${entry.proposedNormalizedKey}', " +
                                        "resultingName='${entry.resultingCanonicalName}', " +
                                        "resultingKey='${entry.resultingNormalizedKey}', " +
                                        "status=${entry.status}"
                            )
                        }
                    }
                }

        assertTrue(
            validationResult.valid,
            buildString {
                appendLine(
                    "Expanded catalog validation failed."
                )

                appendLine(
                    validationResult.issues.joinToString(
                        separator =
                            System.lineSeparator()
                    )
                )

                if (
                    duplicateCanonicalNameDiagnostics.isNotBlank()
                ) {
                    appendLine()
                    append(
                        duplicateCanonicalNameDiagnostics
                    )
                }
            }
        )

        println(
            buildString {
                appendLine(
                    "First approved canonical catalog expansion"
                )
                appendLine(
                    "------------------------------------------"
                )
                appendLine(
                    "Baseline entries: " +
                            result.baselineEntryCount
                )
                appendLine(
                    "Generated candidates: " +
                            result.generatedCandidateCount
                )
                appendLine(
                    "Semantically accepted: " +
                            result.semanticallyAcceptedCandidateCount
                )
                appendLine(
                    "Semantically rejected: " +
                            result.semanticallyRejectedCandidateCount
                )
                appendLine(
                    "Review required: " +
                            result.reviewRequiredCandidateCount
                )
                appendLine(
                    "Materialized entries: " +
                            result.materializedEntryCount
                )
                appendLine(
                    "Baseline key collisions: " +
                            result.baselineKeyCollisionCount
                )
                appendLine(
                    "Expansion key collisions: " +
                            result.expansionKeyCollisionCount
                )
                appendLine(
                    "Expanded catalog entries: " +
                            result.expandedCatalogEntryCount
                )
                appendLine(
                    "Unique normalized keys: " +
                            result.uniqueNormalizedKeys
                )
                appendLine(
                    "Expanded catalog validation valid: " +
                            validationResult.valid
                )
                append(
                    "Expansion valid: " +
                            result.valid
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
                BASELINE_METADATA_PATH
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
        const val BASELINE_METADATA_PATH =
            "data/generated/knowledge/catalog/baseline/" +
                    "canonical-food-catalog-baseline.json"

        const val NORMALIZED_BASELINE_PATH =
            "data/generated/knowledge/catalog/normalized/" +
                    "catalog.normalized.json"

        const val CANDIDATE_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-catalog-expansion-candidates.json"

        const val SEMANTIC_VALIDATION_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-catalog-expansion-semantic-validation.json"

        const val EXPANDED_CATALOG_PATH =
            "data/generated/knowledge/catalog/expansion/approved/" +
                    "canonical-food-catalog-expanded.json"

        const val EXPANSION_REPORT_PATH =
            "data/generated/knowledge/catalog/expansion/approved/" +
                    "canonical-food-catalog-expansion-report.json"
    }
}