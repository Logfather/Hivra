package de.shopme.testing.system.tools.knowledge.catalog.finalization

import com.google.gson.GsonBuilder
import de.shopme.testing.system.tools.knowledge.catalog.model.CatalogFoodItem
import java.io.File
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalFoodCatalogFinalizationRegressionTest {

    @Test
    fun preserveFinalCanonicalFoodCatalogDeterministically() {
        val projectDirectory =
            resolveProjectDirectory()

        val reportFile =
            File(
                projectDirectory,
                FINALIZATION_REPORT_PATH
            )

        val catalogFile =
            File(
                projectDirectory,
                FINAL_CATALOG_PATH
            )

        val reportReader =
            CanonicalFoodCatalogFinalizationReportReader()

        val firstReport =
            reportReader.read(
                reportFile
            )

        val secondReport =
            reportReader.read(
                reportFile
            )

        assertEquals(
            firstReport,
            secondReport
        )

        assertTrue(firstReport.valid)

        assertEquals(
            0,
            firstReport.openImplementationBatchCount
        )

        assertEquals(
            0,
            firstReport.openPolicyGapCount
        )

        assertEquals(
            firstReport.finalCatalogEntryCount,
            firstReport.uniqueCanonicalNameCount
        )

        assertEquals(
            firstReport.finalCatalogEntryCount,
            firstReport.uniqueNormalizedKeyCount
        )

        val items =
            GsonBuilder()
                .create()
                .fromJson(
                    catalogFile.readText(
                        Charsets.UTF_8
                    ),

                    Array<CatalogFoodItem>::class.java
                )
                .toList()

        assertEquals(
            firstReport.finalCatalogEntryCount,
            items.size
        )

        assertEquals(
            firstReport.finalCatalogSha256,
            sha256(
                catalogFile
            )
        )
    }

    private fun sha256(
        file: File
    ): String {
        val digest =
            MessageDigest.getInstance(
                "SHA-256"
            )

        digest.update(
            file.readBytes()
        )

        return digest
            .digest()
            .joinToString(
                separator =
                    ""
            ) { byte ->
                "%02x".format(
                    byte
                )
            }
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
            File(
                directory,
                FINAL_CATALOG_PATH
            ).isFile &&
                    File(
                        directory,
                        FINALIZATION_REPORT_PATH
                    ).isFile

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

        const val FINAL_CATALOG_PATH =
            "data/generated/knowledge/catalog/final/" +
                    "canonical-food-catalog.json"

        const val FINALIZATION_REPORT_PATH =
            "data/generated/knowledge/catalog/final/" +
                    "canonical-food-catalog-finalization.json"
    }
}