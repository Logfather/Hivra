package de.shopme.testing.system.tools.knowledge.ciqual

import de.shopme.tools.knowledge.ciqual.CiqualNutritionSource
import de.shopme.tools.knowledge.ciqual.model.CiqualSourceFiles
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class RunCiqualNutritionSourceTest {

    @Test
    fun readProductiveCiqualNutritionSource() {
        val directory =
            File(
                "../data/raw/ciqual/Ciqual"
            )

        val files =
            CiqualSourceFiles.fromDirectory(directory)

        var recordCount = 0
        var chervilCount = 0
        var maceCount = 0
        var salsifyCount = 0

        CiqualNutritionSource()
            .forEachRecord(files) { record ->
                recordCount++

                val searchableName =
                    listOfNotNull(
                        record.frenchName,
                        record.englishName
                    )
                        .joinToString(" ")
                        .lowercase()

                if (
                    searchableName.contains("chervil") ||
                    searchableName.contains("cerfeuil")
                ) {
                    chervilCount++
                }

                if (
                    searchableName.contains("mace") ||
                    searchableName.contains("macis")
                ) {
                    maceCount++
                }

                if (
                    searchableName.contains("salsify") ||
                    searchableName.contains("salsifis") ||
                    searchableName.contains("scorsonere")
                ) {
                    salsifyCount++
                }
            }

        println("CIQUAL nutrition records=$recordCount")
        println("CIQUAL chervil records=$chervilCount")
        println("CIQUAL mace records=$maceCount")
        println("CIQUAL salsify records=$salsifyCount")

        assertTrue(
            recordCount > 0,
            "CIQUAL must provide at least one normalized nutrition record."
        )
    }
}