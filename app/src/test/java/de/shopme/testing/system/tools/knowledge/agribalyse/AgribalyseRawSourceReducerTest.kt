package de.shopme.testing.system.tools.knowledge.agribalyse

import de.shopme.tools.knowledge.agribalyse.parser.AgribalyseRawSourceReducer
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AgribalyseRawSourceReducerTest {

    @Test
    fun reduceAgribalyseRawSourceToSlimTsv() {

        val paths =
            KnowledgeBuildPaths.default()

        val input =
            paths.projectRoot.resolve(
                "data/sources/agribalyse/" +
                        "AGRIBALYSE3.2_Tableur produits alimentaires_" +
                        "PublieAOUT25.xlsx"
            )

        val output =
            paths.projectRoot.resolve(
                "build/knowledge/references/agribalyse/" +
                        "agribalyse-foods.slim.tsv"
            )

        AgribalyseRawSourceReducer()
            .reduce(
                input =
                    input,
                output =
                    output,
                sheetName =
                    "Synthese"
            )

        assertTrue(
            output.isFile
        )

        assertTrue(
            output.length() > 0L
        )

        val lines =
            output.readLines()

        assertTrue(
            lines.size > 1
        )

        val header =
            lines.first()
                .split("\t")

        assertEquals(
            expected =
                listOf(
                    "code_agb",
                    "code_ciqual",
                    "food_group",
                    "food_sub_group",
                    "name_fr",
                    "name_en",
                    "data_quality_score",
                    "environment_score_mpt_per_kg",
                    "climate_total_kg_co2_eq_per_kg",
                    "land_use_pt_per_kg",
                    "water_deprivation_m3_per_kg",
                    "climate_biogenic_kg_co2_eq_per_kg",
                    "climate_fossil_kg_co2_eq_per_kg",
                    "climate_land_use_change_kg_co2_eq_per_kg"
                ),
            actual =
                header
        )

        assertEquals(
            expected =
                14,
            actual =
                header.size
        )

        /*
         * Regression gegen den ersten bekannten AGRIBALYSE-3.2-Datensatz.
         *
         * Dadurch prüfen wir insbesondere die vier identisch benannten
         * CO2-Spalten.
         */
        val firstRow =
            lines[1]
                .split("\t")

        assertEquals(
            expected =
                14,
            actual =
                firstRow.size
        )

        assertEquals(
            expected =
                "11172",
            actual =
                firstRow[0]
        )

        assertEquals(
            expected =
                "7.58",
            actual =
                firstRow[8]
        )

        assertEquals(
            expected =
                "0.104",
            actual =
                firstRow[11]
        )

        assertEquals(
            expected =
                "7.46",
            actual =
                firstRow[12]
        )

        assertEquals(
            expected =
                "0.0212",
            actual =
                firstRow[13]
        )
    }
}