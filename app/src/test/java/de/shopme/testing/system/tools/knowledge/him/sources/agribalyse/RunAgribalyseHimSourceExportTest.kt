package de.shopme.testing.system.tools.knowledge.him.sources.agribalyse

import de.shopme.tools.knowledge.him.sources.agribalyse.AgribalyseHimSourceExporter
import org.junit.Test
import java.io.File

class RunAgribalyseHimSourceExportTest {

    @Test
    fun exportAgribalyseHimSource() {
        de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()

        val projectRoot =
            findProjectRoot()

        val sourceWorkbook =
            File(
                projectRoot,
                "data/sources/agribalyse/raw/" +
                        "AGRIBALYSE3.2_Tableur produits alimentaires_PublieAOUT25.xlsx"
            )

        val outputFile =
            File(
                projectRoot,
                "data/sources/agribalyse/optimized/" +
                        "agribalyse-him-final-source.jsonl.gz"
            )

        val result =
            AgribalyseHimSourceExporter()
                .export(
                    sourceWorkbook = sourceWorkbook,
                    outputFile = outputFile
                )

        println()
        println("AGRIBALYSE HIM SOURCE EXPORT")
        println("============================")
        println("Source   : ${result.sourceWorkbook.absolutePath}")
        println("Output   : ${result.outputFile.absolutePath}")
        println("Records  : ${result.exportedRecords}")
        println("Size     : ${result.outputFile.length()} bytes")
        println("Header   : POI row ${result.headerRowIndex}")
        println("Data     : POI row ${result.dataStartRowIndex}")
        println("============================")
        println()
    }

    private fun findProjectRoot(): File {

        var current =
            File(
                requireNotNull(
                    System.getProperty("user.dir")
                ) {
                    "System property 'user.dir' is not available."
                }
            ).canonicalFile

        while (true) {

            val settingsGradle =
                File(
                    current,
                    "settings.gradle"
                )

            val settingsGradleKts =
                File(
                    current,
                    "settings.gradle.kts"
                )

            if (
                settingsGradle.isFile ||
                settingsGradleKts.isFile
            ) {
                return current
            }

            current =
                current.parentFile
                    ?: error(
                        "Could not locate ShopMe project root."
                    )
        }
    }
}
