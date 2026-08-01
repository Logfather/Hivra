package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.aggregation

import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.BuildFinalOFFNutritionAggregateDataset
import de.shopme.tools.knowledge.off.nutrition.reference.persistence.OFFAcceptedNutritionReferenceManifestReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunBuildFinalOFFNutritionAggregateDatasetTest {

    @Test
    fun buildFinalOFFNutritionAggregateDataset() {

        val repositoryRoot =
            resolveRepositoryRoot()

        val acceptedReferenceFile =
            repositoryRoot.resolve(
                "data/generated/knowledge/references/off/" +
                        "off-nutrition-references.accepted.jsonl.gz"
            )

        val acceptedManifestFile =
            repositoryRoot.resolve(
                "data/generated/knowledge/references/off/" +
                        "off-nutrition-references.accepted.manifest.json"
            )

        val aggregateDatasetFile =
            repositoryRoot.resolve(
                "data/generated/knowledge/references/off/" +
                        "off-nutrition-reference-aggregates.json"
            )

        val aggregationReportFile =
            repositoryRoot.resolve(
                "data/generated/knowledge/reports/" +
                        "off-nutrition-reference-aggregation-final.json"
            )

        val acceptedManifest =
            OFFAcceptedNutritionReferenceManifestReader()
                .read(
                    acceptedManifestFile
                )

        val result =
            BuildFinalOFFNutritionAggregateDataset()
                .run(
                    acceptedReferenceFile =
                        acceptedReferenceFile,
                    aggregateDatasetOutputFile =
                        aggregateDatasetFile,
                    aggregationReportOutputFile =
                        aggregationReportFile
                )

        assertEquals(
            acceptedManifest.acceptedReferenceCount,
            result.inputReferenceCount
        )

        assertEquals(
            result.inputReferenceCount,
            result.deduplicatedReferenceCount +
                    result.removedDuplicateCount
        )

        assertTrue(
            result.aggregateCount > 0
        )

        assertTrue(
            result.aggregateCount <=
                    result.deduplicatedReferenceCount
        )

        assertEquals(
            result.aggregateCount,
            result.singleProfileAggregateCount +
                    result.multiProfileAggregateCount
        )

        assertTrue(
            result.maximumProfileCount > 0
        )

        assertTrue(
            result.aggregateDatasetFile.isFile
        )

        assertTrue(
            result.aggregateDatasetFile.length() > 0L
        )

        assertTrue(
            result.aggregationReportFile.isFile
        )

        println(
            "inputReferenceCount=" +
                    result.inputReferenceCount
        )

        println(
            "deduplicatedReferenceCount=" +
                    result.deduplicatedReferenceCount
        )

        println(
            "removedDuplicateCount=" +
                    result.removedDuplicateCount
        )

        println(
            "duplicateGroupCount=" +
                    result.duplicateGroupCount
        )

        println(
            "aggregateCount=" +
                    result.aggregateCount
        )

        println(
            "singleProfileAggregateCount=" +
                    result.singleProfileAggregateCount
        )

        println(
            "multiProfileAggregateCount=" +
                    result.multiProfileAggregateCount
        )

        println(
            "maximumProfileCount=" +
                    result.maximumProfileCount
        )

        println(
            "aggregateDatasetFile=" +
                    result.aggregateDatasetFile.absolutePath
        )

        println(
            "aggregateDatasetFileSizeBytes=" +
                    result.aggregateDatasetFileSizeBytes
        )

        println(
            "aggregationReportFile=" +
                    result.aggregationReportFile.absolutePath
        )
    }

    private fun resolveRepositoryRoot(): File {

        val workingDirectory =
            File(".").canonicalFile

        return if (workingDirectory.name == "app") {
            requireNotNull(
                workingDirectory.parentFile
            ).canonicalFile
        } else {
            workingDirectory
        }
    }
}