package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.aggregation

import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.OFFNutritionReferenceAggregateDatasetReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReadFinalOFFNutritionAggregateDatasetTest {

    @Test
    fun readFinalOFFNutritionAggregateDataset() {

        val repositoryRoot =
            resolveRepositoryRoot()

        val aggregateDatasetFile =
            repositoryRoot.resolve(
                "data/generated/knowledge/references/off/" +
                        "off-nutrition-reference-aggregates.json"
            )

        var firstCanonicalId: String? =
            null

        var lastCanonicalId: String? =
            null

        var totalProfileCount =
            0L

        var totalNutrientCount =
            0L

        val aggregateCount =
            OFFNutritionReferenceAggregateDatasetReader()
                .forEachAggregate(
                    inputFile =
                        aggregateDatasetFile
                ) { aggregate ->

                    if (firstCanonicalId == null) {
                        firstCanonicalId =
                            aggregate.canonicalId
                    }

                    lastCanonicalId =
                        aggregate.canonicalId

                    totalProfileCount +=
                        aggregate.profileCount.toLong()

                    totalNutrientCount +=
                        aggregate.nutrition.size.toLong()
                }

        assertTrue(
            aggregateCount > 0
        )

        assertTrue(
            firstCanonicalId
                .orEmpty()
                .isNotBlank()
        )

        assertTrue(
            lastCanonicalId
                .orEmpty()
                .isNotBlank()
        )

        assertTrue(
            totalProfileCount > 0L
        )

        assertTrue(
            totalNutrientCount > 0L
        )

        assertEquals(
            true,
            firstCanonicalId!! <=
                    lastCanonicalId!!
        )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("FINAL OFF NUTRITION AGGREGATE DATASET READER")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "aggregateCount=" +
                    aggregateCount
        )
        println(
            "totalProfileCount=" +
                    totalProfileCount
        )
        println(
            "totalNutrientCount=" +
                    totalNutrientCount
        )
        println(
            "firstCanonicalId=" +
                    firstCanonicalId
        )
        println(
            "lastCanonicalId=" +
                    lastCanonicalId
        )
        println(
            "aggregateDatasetFile=" +
                    aggregateDatasetFile.absolutePath
        )
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    private fun resolveRepositoryRoot(): File {

        val workingDirectory =
            File(".").canonicalFile

        return if (
            workingDirectory.name ==
            "app"
        ) {
            requireNotNull(
                workingDirectory.parentFile
            )
                .canonicalFile
        } else {
            workingDirectory
        }
    }
}