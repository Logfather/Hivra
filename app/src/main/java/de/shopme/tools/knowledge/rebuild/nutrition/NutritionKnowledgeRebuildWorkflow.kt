package de.shopme.tools.knowledge.rebuild.nutrition

import com.google.gson.GsonBuilder
import java.io.File
import java.io.PrintStream
import java.util.Locale

class NutritionKnowledgeRebuildWorkflow(
    private val snapshotReader:
    NutritionKnowledgeSnapshotReader,
    private val requestRebuilder:
    NutritionKnowledgeRequestRebuilder,
    private val matchingStep:
    NutritionKnowledgeMatchingStep,
    private val mappingPersistenceStep:
    NutritionKnowledgeMappingPersistenceStep,
    private val runtimeRebuildStep:
    NutritionKnowledgeRuntimeRebuildStep,
    private val files:
    NutritionKnowledgeRebuildFiles,
    private val resultFile: File,
    private val output: PrintStream = System.out
) {

    fun run(
        mode: NutritionKnowledgeRebuildMode
    ): NutritionKnowledgeRebuildResult {

        /*
         * Der vorhandene Runtime-Stand kann älter sein als die aktuellen
         * Catalog-, Server- oder Mapping-Artefakte.
         *
         * Vor dem Ausgangs-Snapshot wird die Runtime deshalb zunächst
         * deterministisch aus dem aktuellen Stand neu materialisiert.
         */
        runtimeRebuildStep.run()

        val before =
            snapshotReader.read()

        val requestResult =
            requestRebuilder.rebuild()

        val matching =
            matchingStep.run(
                mode = mode
            )

        require(
            matching.requestCount ==
                    requestResult.requestCount
        ) {
            "Matching request count differs from rebuilt " +
                    "request count: " +
                    "matching=${matching.requestCount}, " +
                    "rebuilt=${requestResult.requestCount}."
        }

        val persistence =
            mappingPersistenceStep.run()

        /*
         * Nach der Mapping-Persistenz muss die Runtime erneut aus dem
         * aktuellen Mapping-Bestand materialisiert werden.
         *
         * Dabei dürfen veraltete Mappings und die davon abgeleiteten
         * Runtime-Einträge deterministisch entfallen.
         */
        runtimeRebuildStep.run()

        val after =
            snapshotReader.read()

        val result =
            NutritionKnowledgeRebuildResult(
                mode =
                    mode,
                before =
                    before,
                matching =
                    matching,
                persistence =
                    persistence,
                after =
                    after,
                delta =
                    NutritionKnowledgeRebuildDelta(
                        mappingCount =
                            after.mappingCount -
                                    before.mappingCount,
                        coveredCatalogItemCount =
                            after.coveredCatalogItemCount -
                                    before.coveredCatalogItemCount,
                        missingCatalogItemCount =
                            after.missingCatalogItemCount -
                                    before.missingCatalogItemCount,
                        coverage =
                            after.coverage -
                                    before.coverage
                    ),
                files =
                    files
            )

        validateResult(
            result = result
        )

        writeResult(
            result = result
        )

        printResult(
            result = result
        )

        return result
    }

    private fun validateResult(
        result: NutritionKnowledgeRebuildResult
    ) {
        require(
            result.persistence.finalMappingCount ==
                    result.persistence.existingMappingCount +
                    result.persistence.addedMappingCount -
                    result.persistence.removedMappingCount
        ) {
            "Nutrition rebuild mapping reconciliation is invalid: " +
                    "existing=${result.persistence.existingMappingCount}, " +
                    "added=${result.persistence.addedMappingCount}, " +
                    "removed=${result.persistence.removedMappingCount}, " +
                    "final=${result.persistence.finalMappingCount}."
        }

        val expectedMappingDelta =
            result.persistence.addedMappingCount -
                    result.persistence.removedMappingCount

        require(
            result.delta.mappingCount ==
                    expectedMappingDelta
        ) {
            "Mapping delta differs from persisted mapping " +
                    "reconciliation: " +
                    "delta=${result.delta.mappingCount}, " +
                    "added=${result.persistence.addedMappingCount}, " +
                    "removed=${result.persistence.removedMappingCount}, " +
                    "expectedDelta=$expectedMappingDelta."
        }

        require(
            result.before.coveredCatalogItemCount +
                    result.before.missingCatalogItemCount ==
                    result.before.catalogItemCount
        ) {
            "Nutrition before snapshot does not partition the " +
                    "catalog completely: " +
                    "catalog=${result.before.catalogItemCount}, " +
                    "covered=${result.before.coveredCatalogItemCount}, " +
                    "missing=${result.before.missingCatalogItemCount}."
        }

        require(
            result.after.coveredCatalogItemCount +
                    result.after.missingCatalogItemCount ==
                    result.after.catalogItemCount
        ) {
            "Nutrition after snapshot does not partition the " +
                    "catalog completely: " +
                    "catalog=${result.after.catalogItemCount}, " +
                    "covered=${result.after.coveredCatalogItemCount}, " +
                    "missing=${result.after.missingCatalogItemCount}."
        }

        require(
            result.after.catalogItemCount ==
                    result.before.catalogItemCount
        ) {
            "Nutrition rebuild changed the catalog item count: " +
                    "before=${result.before.catalogItemCount}, " +
                    "after=${result.after.catalogItemCount}."
        }

        require(
            result.delta.coveredCatalogItemCount +
                    result.delta.missingCatalogItemCount ==
                    0
        ) {
            "Nutrition rebuild coverage deltas are inconsistent: " +
                    "coveredDelta=" +
                    "${result.delta.coveredCatalogItemCount}, " +
                    "missingDelta=" +
                    "${result.delta.missingCatalogItemCount}."
        }
    }

    private fun writeResult(
        result: NutritionKnowledgeRebuildResult
    ) {
        resultFile.parentFile
            ?.let { directory ->

                if (!directory.exists()) {
                    check(
                        directory.mkdirs()
                    ) {
                        "Could not create nutrition rebuild " +
                                "report directory: " +
                                directory.absolutePath
                    }
                }
            }

        val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()

        resultFile.writeText(
            gson.toJson(result) + "\n"
        )
    }

    private fun printResult(
        result: NutritionKnowledgeRebuildResult
    ) {
        output.println(
            "Catalog items            : " +
                    result.after.catalogItemCount
        )
        output.println()

        output.println(
            "Exact before             : " +
                    result.before.exactMatchCount
        )
        output.println(
            "Exact after              : " +
                    result.after.exactMatchCount
        )
        output.println()

        output.println(
            "Mapped before            : " +
                    result.before.mappedMatchCount
        )
        output.println(
            "Mapped after             : " +
                    result.after.mappedMatchCount
        )
        output.println(
            "Mappings added           : " +
                    result.persistence.addedMappingCount
        )
        output.println(
            "Mappings removed         : " +
                    result.persistence.removedMappingCount
        )
        output.println(
            "Mapping delta            : " +
                    formatSignedCount(
                        result.delta.mappingCount
                    )
        )
        output.println()

        output.println(
            "Runtime entries before   : " +
                    result.before.runtimeEntryCount
        )
        output.println(
            "Runtime entries after    : " +
                    result.after.runtimeEntryCount
        )
        output.println()

        output.println(
            "Covered before           : " +
                    result.before.coveredCatalogItemCount
        )
        output.println(
            "Covered after            : " +
                    result.after.coveredCatalogItemCount
        )
        output.println(
            "Covered delta            : " +
                    formatSignedCount(
                        result.delta.coveredCatalogItemCount
                    )
        )
        output.println()

        output.println(
            "Coverage before          : " +
                    formatCoverage(
                        result.before.coverage
                    )
        )
        output.println(
            "Coverage after           : " +
                    formatCoverage(
                        result.after.coverage
                    )
        )
        output.println(
            "Coverage delta           : " +
                    formatPercentagePointDelta(
                        result.delta.coverage
                    )
        )
        output.println()

        output.println(
            "Missing before           : " +
                    result.before.missingCatalogItemCount
        )
        output.println(
            "Missing after            : " +
                    result.after.missingCatalogItemCount
        )
        output.println(
            "Missing delta            : " +
                    formatSignedCount(
                        result.delta.missingCatalogItemCount
                    )
        )
    }

    private fun formatCoverage(
        value: Double
    ): String {

        return String.format(
            Locale.ROOT,
            "%.2f%%",
            value * 100.0
        )
    }

    private fun formatPercentagePointDelta(
        value: Double
    ): String {

        return String.format(
            Locale.ROOT,
            "%+.2f percentage points",
            value * 100.0
        )
    }

    private fun formatSignedCount(
        value: Int
    ): String {

        return String.format(
            Locale.ROOT,
            "%+d",
            value
        )
    }
}