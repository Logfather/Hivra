package de.shopme.testing.system.tools.knowledge.ciqual

import de.shopme.tools.knowledge.ciqual.CiqualNutritionSource
import de.shopme.tools.knowledge.ciqual.model.CiqualSourceFiles
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CiqualNutritionSourceTest {

    @Test
    fun readsAndJoinsCiqualNutritionData() {
        val directory =
            Files
                .createTempDirectory(
                    "ciqual-nutrition-source"
                )
                .toFile()

        try {
            val files =
                writeFixture(directory)

            val records =
                CiqualNutritionSource()
                    .read(files)

            assertEquals(
                1,
                records.size
            )

            val record =
                records.single()

            assertEquals(
                "20001",
                record.foodCode
            )
            assertEquals(
                "Cerfeuil, frais",
                record.frenchName
            )
            assertEquals(
                "Chervil, fresh",
                record.englishName
            )
            assertEquals(
                "02",
                record.groupCode
            )

            assertEquals(
                48.0,
                record.nutrition.energyKcalPer100g
            )
            assertEquals(
                0.6,
                record.nutrition.fatPer100g
            )
            assertEquals(
                0.1,
                record.nutrition.saturatedFatPer100g
            )
            assertEquals(
                3.1,
                record.nutrition.carbohydratesPer100g
            )
            assertEquals(
                0.0,
                record.nutrition.sugarsPer100g
            )
            assertEquals(
                5.2,
                record.nutrition.fiberPer100g
            )
            assertEquals(
                3.3,
                record.nutrition.proteinsPer100g
            )
            assertEquals(
                0.1,
                record.nutrition.saltPer100g
            )

            assertTrue(
                record.constituentValues.isNotEmpty()
            )
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun ignoresMissingAndInvalidCompositionValues() {
        val directory =
            Files
                .createTempDirectory(
                    "ciqual-invalid-values"
                )
                .toFile()

        try {
            val files =
                writeFixture(
                    directory = directory,
                    proteinValue = "traces",
                    sugarValue = "ND"
                )

            val record =
                CiqualNutritionSource()
                    .read(files)
                    .single()

            assertNull(
                record.nutrition.proteinsPer100g
            )
            assertNull(
                record.nutrition.sugarsPer100g
            )
            assertNotNull(
                record.nutrition.energyKcalPer100g
            )
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun writeFixture(
        directory: File,
        proteinValue: String = "3,3",
        sugarValue: String = "0"
    ): CiqualSourceFiles {
        val files =
            CiqualSourceFiles(
                foodsFile =
                    File(
                        directory,
                        "alim.xml"
                    ),
                foodGroupsFile =
                    File(
                        directory,
                        "alim_grp.xml"
                    ),
                compositionsFile =
                    File(
                        directory,
                        "compo.xml"
                    ),
                constituentsFile =
                    File(
                        directory,
                        "const.xml"
                    ),
                sourcesFile =
                    File(
                        directory,
                        "sources.xml"
                    )
            )

        files.foodsFile.writeText(
                    """
            <?xml version="1.0" encoding="UTF-8"?>
            <ALIMENTS>
                <ALIM>
                    <alim_code>20001</alim_code>
                    <alim_nom_fr>Cerfeuil, frais</alim_nom_fr>
                    <alim_nom_eng>Chervil, fresh</alim_nom_eng>
                    <alim_grp_code>02</alim_grp_code>
                </ALIM>
            </ALIMENTS>
            """.trimIndent().trimStart()
        )

        files.foodGroupsFile.writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <GROUPES>
                <ALIM_GRP>
                    <alim_grp_code>02</alim_grp_code>
                    <alim_grp_nom_fr>Légumes</alim_grp_nom_fr>
                </ALIM_GRP>
            </GROUPES>
            """.trimIndent().trimStart()
        )

        files.constituentsFile.writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <CONSTITUANTS>
                ${constituent("1001", "Energie, Règlement UE N° 1169/2011 (kcal/100 g)")}
                ${constituent("1002", "Lipides")}
                ${constituent("1003", "AG saturés")}
                ${constituent("1004", "Glucides")}
                ${constituent("1005", "Sucres")}
                ${constituent("1006", "Fibres alimentaires")}
                ${constituent("1007", "Protéines")}
                ${constituent("1008", "Sel chlorure de sodium")}
            </CONSTITUANTS>
            """.trimIndent().trimStart()
        )

        files.compositionsFile.writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <COMPOSITIONS>
                ${composition("1001", "48")}
                ${composition("1002", "0,6")}
                ${composition("1003", "0,1")}
                ${composition("1004", "3,1")}
                ${composition("1005", sugarValue)}
                ${composition("1006", "5,2")}
                ${composition("1007", proteinValue)}
                ${composition("1008", "0,1")}
            </COMPOSITIONS>
            """.trimIndent().trimStart()
        )

        files.sourcesFile.writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <SOURCES>
                <SOURCE>
                    <source_code>1</source_code>
                    <source_nom>CIQUAL fixture</source_nom>
                </SOURCE>
            </SOURCES>
            """.trimIndent().trimStart()
        )

        return files
    }

    private fun constituent(
        code: String,
        name: String
    ): String =
        """
        <CONST>
            <const_code>$code</const_code>
            <const_nom_fr>$name</const_nom_fr>
            <const_unite>g/100g</const_unite>
        </CONST>
        """.trimIndent()

    private fun composition(
        constituentCode: String,
        value: String
    ): String =
        """
        <COMPO>
            <alim_code>20001</alim_code>
            <const_code>$constituentCode</const_code>
            <teneur>$value</teneur>
        </COMPO>
        """.trimIndent()
}