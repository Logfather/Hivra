package de.shopme.testing.system.tools.knowledge.him.sources.ciqual

import de.shopme.tools.knowledge.him.sources.ciqual.CiqualHimSourceProjector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RunCiqualHimSourceProjectionTest {

    @Test
    fun projectsCiqualRawRelationsSourceFaithfully() {
        de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()
        val projectRoot =
            resolveProjectRoot()

        val artifact =
            CiqualHimSourceProjector()
                .project(
                    projectRoot.resolve(
                        "data/sources/ciqual/raw"
                    )
                )

        assertEquals(
            3484,
            artifact.foods.size
        )
        assertEquals(
            138,
            artifact.taxonomy.size
        )
        assertEquals(
            74,
            artifact.constituents.size
        )
        assertEquals(
            1978,
            artifact.sources.size
        )
        assertEquals(
            257816,
            artifact.foods.sumOf {
                it.compositions.size
            }
        )

        assertEquals(
            artifact.foods.size,
            artifact.foods
                .map {
                    it.alimCode
                }
                .toSet()
                .size
        )
        assertEquals(
            artifact.constituents.size,
            artifact.constituents
                .map {
                    it.constCode
                }
                .toSet()
                .size
        )
        assertEquals(
            artifact.sources.size,
            artifact.sources
                .map {
                    it.sourceCode
                }
                .toSet()
                .size
        )

        val compositionKeys =
            artifact.foods.flatMap { food ->
                food.compositions.map { composition ->
                    food.alimCode to composition.constCode
                }
            }

        assertEquals(
            compositionKeys.size,
            compositionKeys.toSet().size
        )

        assertTrue(
            artifact.taxonomy.any {
                it.groupCode == "04" &&
                        it.subgroupCode == "0411" &&
                        it.subSubgroupCode == "000000"
            }
        )
        assertFalse(
            artifact.taxonomy.any {
                it.groupCode == "00" &&
                        it.subgroupCode == "0000" &&
                        it.subSubgroupCode == "000000"
            }
        )
        assertTrue(
            artifact.foods.any { food ->
                food.compositions.any {
                    it.sourceCode.missingAttributeValue != null
                }
            }
        )
        assertTrue(
            artifact.foods.any { food ->
                food.compositions.any {
                    it.teneurLexical == "traces" ||
                            it.teneurLexical == "< 2,2"
                }
            }
        )
    }

    private fun resolveProjectRoot(): File {
        var current =
            File(
                requireNotNull(
                    System.getProperty("user.dir")
                ) {
                    "System property 'user.dir' is not available."
                }
            ).absoluteFile

        while (true) {
            if (
                current.resolve("settings.gradle.kts").isFile &&
                current.resolve("gradlew").isFile
            ) {
                return current
            }

            current =
                requireNotNull(
                    current.parentFile
                ) {
                    "Could not resolve ShopMe project root from user.dir."
                }
        }
    }
}
