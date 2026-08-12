package de.shopme.testing.system.tools.knowledge.catalog.build

import com.google.gson.JsonParser
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import org.junit.Assert.assertTrue
import kotlin.test.Test
import kotlin.test.assertEquals

class CanonicalKnowledgeBuildCatalogSourceTest {

    @Test
    fun loadsOnlyReleasedProductiveCanonicalCatalog() {

        val paths =
            KnowledgeBuildPaths.default()

        val source =
            CanonicalKnowledgeBuildCatalogSource(
                paths.projectRoot
            )
                .load()

        assertEquals(
            expected =
                KnowledgeBuildPaths.CANONICAL_CATALOG_ENTRY_COUNT,
            actual =
                source.entries.size
        )

        assertEquals(
            expected =
                KnowledgeBuildPaths.CANONICAL_CATALOG_SHA256,
            actual =
                source.sha256
        )

        assertEquals(
            expected =
                CanonicalKnowledgeBuildCatalogSource
                    .EXPECTED_SHA256,
            actual =
                source.sha256
        )

        assertEquals(
            expected =
                paths.canonicalFoodCatalog.canonicalFile,
            actual =
                source.sourceFile.canonicalFile
        )
    }

    @Test
    fun productiveCatalogIsCanonicalProductOnlyMaster() {

        val paths =
            KnowledgeBuildPaths.default()

        val productiveCatalog =
            paths.canonicalFoodCatalog

        val expectedMaster =
            paths.projectRoot.resolve(
                "data/knowledge/catalog/master/product-only/" +
                        "canonical-food-catalog.product-only.master.json"
            )

        require(productiveCatalog.isFile) {
            "Productive canonical food catalog does not exist: " +
                    productiveCatalog.absolutePath
        }

        require(expectedMaster.isFile) {
            "Canonical Product-Only master does not exist: " +
                    expectedMaster.absolutePath
        }

        assertEquals(
            expected =
                expectedMaster.canonicalFile,
            actual =
                productiveCatalog.canonicalFile
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

        assertTrue(
            productiveCatalog.canonicalFile ==
                    paths.canonicalFoodCatalog.canonicalFile
        )
    }
}