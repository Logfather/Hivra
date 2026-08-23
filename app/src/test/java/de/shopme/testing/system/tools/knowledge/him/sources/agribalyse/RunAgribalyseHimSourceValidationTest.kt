package de.shopme.testing.system.tools.knowledge.him.sources.agribalyse

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.him.sources.agribalyse.AgribalyseHimSourceValidator
import org.junit.Test

class RunAgribalyseHimSourceValidationTest {

    @Test
    fun validateAgribalyseHimSource() {
        de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()

        val paths =
            KnowledgeBuildPaths.default()

        val sourceFile =
            paths
                .agribalyseSourceRoot
                .resolve("optimized")
                .resolve(
                    "agribalyse-him-final-source.jsonl.gz"
                )

        val result =
            AgribalyseHimSourceValidator()
                .validate(
                    sourceFile = sourceFile
                )

        println()
        println("AGRIBALYSE HIM SOURCE VALIDATION")
        println("================================")
        println(
            "Source          : " +
                    result.sourceFile.absolutePath
        )
        println(
            "Records         : " +
                    result.recordCount
        )
        println(
            "Unique AGB codes: " +
                    result.uniqueAgbCodeCount
        )

        println(
            "Duplicate codes  : " +
                    result.duplicateAgbCodeCount
        )

        println(
            "Duplicate records: " +
                    result.duplicateAgbRecordCount
        )

        if (
            result.duplicateAgbCodes.isNotEmpty()
        ) {

            println(
                "Duplicate AGBs  : " +
                        result.duplicateAgbCodes
                            .entries
                            .joinToString(
                                separator = ", "
                            ) {
                                "${it.key}=${it.value}"
                            }
            )
        }

        println(
            "Size            : " +
                    result.compressedSizeBytes +
                    " bytes"
        )
        println(
            "Compressed SHA-256: " +
                    result.compressedSha256
        )

        println(
            "Content SHA-256   : " +
                    result.contentSha256
        )
        println(
            "First AGB code  : " +
                    result.firstRecord.agbCode
        )
        println(
            "First product   : " +
                    result.firstRecord.productNameFr
        )
        println(
            "Last AGB code   : " +
                    result.lastRecord.agbCode
        )
        println(
            "Last product    : " +
                    result.lastRecord.productNameFr
        )
        println("================================")
    }
}
