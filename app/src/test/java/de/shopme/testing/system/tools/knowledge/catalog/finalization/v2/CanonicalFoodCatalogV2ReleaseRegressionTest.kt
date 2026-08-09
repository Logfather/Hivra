package de.shopme.testing.system.tools.knowledge.catalog.finalization.v2

import com.google.gson.JsonParser
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalFoodCatalogV2ReleaseRegressionTest {

    @Test
    fun releasedCatalogArtifactsAreIdenticalAndValid() {

        val paths =
            KnowledgeBuildPaths.default()

        val reportFile =
            paths.catalogReleasesRoot.resolve(
                "canonical-food-catalog-finalization-v2.json"
            )

        require(reportFile.isFile) {
            "V2 finalization report not found: " +
                    reportFile.absolutePath
        }

        val report =
            JsonParser
                .parseString(
                    reportFile.readText()
                )
                .asJsonObject

        val finalizationId =
            report
                .get("finalizationId")
                .asString

        val expectedSha256 =
            report
                .get("sha256")
                .asString

        val immutableSnapshot =
            paths.catalogReleasesRoot.resolve(
                "$finalizationId.json"
            )

        val productiveCatalog =
            paths.canonicalFoodCatalog

        require(immutableSnapshot.isFile) {
            "Immutable V2 catalog snapshot not found: " +
                    immutableSnapshot.absolutePath
        }

        require(productiveCatalog.isFile) {
            "Productive canonical food catalog not found: " +
                    productiveCatalog.absolutePath
        }

        assertContentEquals(
            expected =
                immutableSnapshot.readBytes(),
            actual =
                productiveCatalog.readBytes()
        )

        val catalog =
            JsonParser
                .parseString(
                    productiveCatalog.readText()
                )
                .asJsonArray

        assertEquals(
            expected =
                KnowledgeBuildPaths.CANONICAL_CATALOG_ENTRY_COUNT,
            actual =
                catalog.size()
        )

        val actualSha256 =
            MessageDigest
                .getInstance("SHA-256")
                .digest(
                    productiveCatalog.readBytes()
                )
                .joinToString("") {
                    "%02x".format(
                        it.toInt() and 0xff
                    )
                }

        assertEquals(
            expected =
                KnowledgeBuildPaths.CANONICAL_CATALOG_SHA256,
            actual =
                actualSha256
        )

        assertEquals(
            expected =
                expectedSha256,
            actual =
                actualSha256
        )

        assertEquals(
            expected =
                finalizationId,
            actual =
                "canonical-food-catalog-final-v2-72d837193a6083df"
        )

        assertTrue(
            report
                .get("valid")
                .asBoolean
        )
    }
}