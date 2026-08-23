package de.shopme.testing.system.tools.knowledge.him.sources.ciqual

import de.shopme.tools.knowledge.him.sources.ciqual.CiqualHimSourceExporter
import de.shopme.tools.knowledge.him.sources.ciqual.CiqualHimSourceValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RunCiqualHimSourceExportTest {

    @Test
    fun exportsValidatesAndReproducesFinalCiqualHimSourceArtifact() {
        de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()
        val projectRoot =
            resolveProjectRoot()

        val sourceDirectory =
            projectRoot.resolve(
                "data/sources/ciqual/raw"
            )

        val outputFile =
            projectRoot.resolve(
                "data/sources/ciqual/optimized/" +
                        "ciqual-him-final-source.json.gz"
            )

        val exporter =
            CiqualHimSourceExporter()

        val validator =
            CiqualHimSourceValidator()

        exporter.export(
            sourceDirectory = sourceDirectory,
            outputFile = outputFile
        )

        val firstValidation =
            validator.validate(outputFile)

        exporter.export(
            sourceDirectory = sourceDirectory,
            outputFile = outputFile
        )

        val secondValidation =
            validator.validate(outputFile)

        val deterministicContent =
            firstValidation.contentSha256 ==
                    secondValidation.contentSha256

        val deterministicCompressedArtifact =
            firstValidation.compressedSha256 ==
                    secondValidation.compressedSha256

        assertTrue(
            "CIQUAL decompressed JSON content is not deterministic.",
            deterministicContent
        )

        assertTrue(
            "CIQUAL compressed artifact is not deterministic.",
            deterministicCompressedArtifact
        )

        assertEquals(
            firstValidation.sizeBytes,
            secondValidation.sizeBytes
        )

        println(
            secondValidation.renderReport(
                deterministicContent = deterministicContent,
                deterministicCompressedArtifact =
                    deterministicCompressedArtifact
            )
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
