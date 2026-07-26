package de.shopme.tools.knowledge.ciqual

import de.shopme.tools.knowledge.ciqual.model.CiqualNutritionRecord
import de.shopme.tools.knowledge.ciqual.model.CiqualSourceFiles
import de.shopme.tools.knowledge.ciqual.parser.CiqualNutrientNormalizer
import de.shopme.tools.knowledge.ciqual.parser.CiqualXmlRowReader
import java.util.Locale

class CiqualNutritionSource private constructor(
    private val rowReader: CiqualXmlRowReader,
    private val nutrientNormalizer: CiqualNutrientNormalizer
) {

    constructor() : this(
        rowReader = CiqualXmlRowReader(),
        nutrientNormalizer = CiqualNutrientNormalizer()
    )

    fun read(
        files: CiqualSourceFiles
    ): List<CiqualNutritionRecord> {
        val records =
            mutableListOf<CiqualNutritionRecord>()

        forEachRecord(files) { record ->
            records += record
        }

        return records
    }

    fun forEachRecord(
        files: CiqualSourceFiles,
        consumer: (CiqualNutritionRecord) -> Unit
    ) {
        files.validate()

        val foods =
            readFoods(files)

        val constituents =
            readConstituents(files)

        val compositionByFood =
            readComposition(
                files = files,
                constituents = constituents
            )

        foods
            .values
            .asSequence()
            .sortedBy { it.code }
            .forEach { food ->
                val constituentValues =
                    compositionByFood[food.code]
                        .orEmpty()
                        .toSortedMap()

                if (constituentValues.isEmpty()) {
                    return@forEach
                }

                val nutrition =
                    nutrientNormalizer.normalize(
                        constituentValues
                    )

                if (!nutrition.hasAnyValue()) {
                    return@forEach
                }

                consumer(
                    CiqualNutritionRecord(
                        foodCode = food.code,
                        frenchName = food.frenchName,
                        englishName = food.englishName,
                        groupCode = food.groupCode,
                        nutrition = nutrition,
                        constituentValues = constituentValues
                    )
                )
            }
    }

    private fun readFoods(
        files: CiqualSourceFiles
    ): Map<String, FoodRow> {
        val foods =
            linkedMapOf<String, FoodRow>()

        rowReader.readRows(
            file = files.foodsFile,
            acceptedRowNames =
                setOf(
                    "ALIM",
                    "aliment",
                    "food"
                )
        ) { row ->
            val code =
                row.firstValue(
                    "alim_code",
                    "code_alim",
                    "food_code",
                    "code"
                )
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: return@readRows

            val frenchName =
                row.firstValue(
                    "alim_nom_fr",
                    "nom_fr",
                    "alim_nom",
                    "food_name_fr",
                    "libelle"
                )
                    ?.cleanText()
                    ?.takeIf { it.isNotEmpty() }
                    ?: return@readRows

            foods[code] =
                FoodRow(
                    code = code,
                    frenchName = frenchName,
                    englishName =
                        row.firstValue(
                            "alim_nom_eng",
                            "alim_nom_en",
                            "nom_eng",
                            "nom_en",
                            "food_name_en"
                        )
                            ?.cleanText()
                            ?.takeIf { it.isNotEmpty() },
                    groupCode =
                        row.firstValue(
                            "alim_grp_code",
                            "grp_code",
                            "group_code"
                        )
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }
                )
        }

        check(foods.isNotEmpty()) {
            "CIQUAL foods file contained no readable food rows: " +
                    files.foodsFile.absolutePath
        }

        return foods
    }

    private fun readConstituents(
        files: CiqualSourceFiles
    ): Map<String, ConstituentRow> {
        val constituents =
            linkedMapOf<String, ConstituentRow>()

        rowReader.readRows(
            file = files.constituentsFile,
            acceptedRowNames =
                setOf(
                    "CONST",
                    "constituent",
                    "nutrient"
                )
        ) { row ->
            val code =
                row.firstValue(
                    "const_code",
                    "code_const",
                    "nutrient_code",
                    "code"
                )
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: return@readRows

            val name =
                row.firstValue(
                    "const_nom_fr",
                    "nom_fr",
                    "const_nom",
                    "nutrient_name",
                    "libelle"
                )
                    ?.cleanText()
                    ?.takeIf { it.isNotEmpty() }
                    ?: return@readRows

            constituents[code] =
                ConstituentRow(
                    code = code,
                    name = name,
                    unit =
                        row.firstValue(
                            "const_unite",
                            "unite",
                            "unit"
                        )
                            ?.cleanText()
                )
        }

        check(constituents.isNotEmpty()) {
            "CIQUAL constituents file contained no readable constituent rows: " +
                    files.constituentsFile.absolutePath
        }

        return constituents
    }

    private fun readComposition(
        files: CiqualSourceFiles,
        constituents: Map<String, ConstituentRow>
    ): Map<String, Map<String, Double>> {
        val compositionByFood =
            linkedMapOf<String, MutableMap<String, Double>>()

        rowReader.readRows(
            file = files.compositionsFile,
            acceptedRowNames =
                setOf(
                    "COMPO",
                    "composition"
                )
        ) { row ->
            val foodCode =
                row.firstValue(
                    "alim_code",
                    "code_alim",
                    "food_code"
                )
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: return@readRows

            val constituentCode =
                row.firstValue(
                    "const_code",
                    "code_const",
                    "nutrient_code"
                )
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: return@readRows

            val constituent =
                constituents[constituentCode]
                    ?: return@readRows

            val rawValue =
                row.firstValue(
                    "teneur",
                    "valeur",
                    "value",
                    "content"
                )
                    ?: return@readRows

            val value =
                parseCompositionValue(rawValue)
                    ?: return@readRows

            compositionByFood
                .getOrPut(foodCode) {
                    linkedMapOf()
                }[constituent.name] = value
        }

        return compositionByFood
    }

    private fun parseCompositionValue(
        rawValue: String
    ): Double? {
        val normalized =
            rawValue
                .trim()
                .lowercase(Locale.ROOT)
                .replace('\u00A0', ' ')
                .replace(",", ".")
                .replace(
                    Regex("\\s+"),
                    ""
                )

        if (
            normalized.isEmpty() ||
            normalized == "-" ||
            normalized == "nd" ||
            normalized == "n.d." ||
            normalized == "na" ||
            normalized == "traces" ||
            normalized == "trace"
        ) {
            return null
        }

        val numericPart =
            normalized
                .removePrefix("<")
                .removePrefix(">")
                .removePrefix("≤")
                .removePrefix("≥")
                .replace(
                    Regex("[^0-9.eE+-]"),
                    ""
                )

        val value =
            numericPart.toDoubleOrNull()
                ?: return null

        if (!value.isFinite() || value < 0.0) {
            return null
        }

        return value
    }

    private fun Map<String, String>.firstValue(
        vararg names: String
    ): String? {
        names.forEach { name ->
            this[name]?.let {
                return it
            }
        }

        return null
    }

    private fun String.cleanText(): String =
        replace(
            Regex("\\s+"),
            " "
        )
            .trim()

    private data class FoodRow(
        val code: String,
        val frenchName: String,
        val englishName: String?,
        val groupCode: String?
    )

    private data class ConstituentRow(
        val code: String,
        val name: String,
        val unit: String?
    )
}