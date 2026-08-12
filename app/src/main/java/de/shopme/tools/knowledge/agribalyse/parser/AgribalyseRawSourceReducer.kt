package de.shopme.tools.knowledge.agribalyse.parser

import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable
import org.apache.poi.xssf.eventusermodel.XSSFReader
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler
import org.apache.poi.xssf.usermodel.XSSFComment
import org.xml.sax.InputSource
import org.xml.sax.helpers.XMLReaderFactory
import java.io.File
import java.util.Locale

class AgribalyseRawSourceReducer {

    fun reduce(
        input: File,
        output: File,
        sheetName: String = "Synthese",
        maxRows: Int? = null
    ) {

        require(input.isFile) {
            "Agribalyse input missing: " +
                    input.absolutePath
        }

        val outputDirectory =
            requireNotNull(
                output.parentFile
            ) {
                "Agribalyse output has no parent directory: " +
                        output.absolutePath
            }

        require(
            outputDirectory.exists() ||
                    outputDirectory.mkdirs()
        ) {
            "Could not create Agribalyse output directory: " +
                    outputDirectory.absolutePath
        }

        output
            .bufferedWriter()
            .use { writer ->

                OPCPackage
                    .open(input)
                    .use { pkg ->

                        val reader =
                            XSSFReader(pkg)

                        val sharedStrings =
                            ReadOnlySharedStringsTable(pkg)

                        val sheetIterator =
                            reader.sheetsData as
                                    XSSFReader.SheetIterator

                        var foundSheet =
                            false

                        while (
                            sheetIterator.hasNext()
                        ) {

                            val stream =
                                sheetIterator.next()

                            val currentSheetName =
                                sheetIterator.sheetName

                            if (
                                currentSheetName !=
                                sheetName
                            ) {

                                stream.close()
                                continue
                            }

                            foundSheet =
                                true

                            stream.use { sheetStream ->

                                val handler =
                                    AgribalyseSlimTsvHandler(
                                        maxRows =
                                            maxRows,
                                        onRow = { row ->

                                            writer.appendLine(
                                                row.joinToString(
                                                    separator = "\t"
                                                ) { value ->
                                                    value.cleanForTsv()
                                                }
                                            )
                                        }
                                    )

                                val xmlReader =
                                    XMLReaderFactory
                                        .createXMLReader()

                                xmlReader.contentHandler =
                                    XSSFSheetXMLHandler(
                                        reader.stylesTable,
                                        sharedStrings,
                                        handler,
                                        DataFormatter(
                                            Locale.ROOT
                                        ),
                                        false
                                    )

                                xmlReader.parse(
                                    InputSource(
                                        sheetStream
                                    )
                                )
                            }

                            break
                        }

                        require(foundSheet) {
                            "Agribalyse sheet '$sheetName' not found."
                        }
                    }
            }
    }

    private fun String.cleanForTsv(): String {

        val cleaned =
            replace(
                "\t",
                " "
            )
                .replace(
                    "\n",
                    " "
                )
                .replace(
                    "\r",
                    " "
                )
                .trim()

        val numericValue =
            cleaned
                .toDoubleOrNull()

        return if (numericValue != null) {

            java.math.BigDecimal
                .valueOf(
                    numericValue
                )
                .stripTrailingZeros()
                .toPlainString()

        } else {

            cleaned
        }
    }
}

private class AgribalyseSlimTsvHandler(
    private val maxRows: Int?,
    private val onRow: (List<String>) -> Unit
) : XSSFSheetXMLHandler.SheetContentsHandler {

    private val currentRow =
        mutableMapOf<Int, String>()

    private var headerFound =
        false

    private var selectedColumns =
        emptyList<Int>()

    private var emittedDataRows =
        0

    private val outputHeaders =
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
        )

    override fun startRow(
        rowNum: Int
    ) {
        currentRow.clear()
    }

    override fun endRow(
        rowNum: Int
    ) {

        val values =
            currentRowValues()

        if (!headerFound) {

            if (
                !isSynthesisHeader(
                    values = values
                )
            ) {
                return
            }

            selectedColumns =
                resolveSelectedColumns(
                    values = values
                )

            require(
                selectedColumns.size ==
                        outputHeaders.size
            ) {
                "Agribalyse selected column count mismatch. " +
                        "selected=${selectedColumns.size}, " +
                        "headers=${outputHeaders.size}"
            }

            onRow(
                outputHeaders
            )

            headerFound =
                true

            println(
                "Agribalyse selected columns=" +
                        selectedColumns.size
            )

            return
        }

        if (
            maxRows != null &&
            emittedDataRows >= maxRows
        ) {
            return
        }

        if (values.isEmpty()) {
            return
        }

        val reduced =
            selectedColumns.map { index ->
                values
                    .getOrNull(index)
                    .orEmpty()
            }

        /*
         * Keine vollständig leere Datenzeile persistieren.
         */
        if (
            reduced.all {
                it.isBlank()
            }
        ) {
            return
        }

        onRow(
            reduced
        )

        emittedDataRows++
    }

    override fun cell(
        cellReference: String,
        formattedValue: String?,
        comment: XSSFComment?
    ) {

        val columnIndex =
            cellReference
                .toColumnIndex()

        currentRow[columnIndex] =
            formattedValue.orEmpty()
    }

    override fun headerFooter(
        text: String?,
        isHeader: Boolean,
        tagName: String?
    ) = Unit

    private fun isSynthesisHeader(
        values: List<String>
    ): Boolean {

        val normalized =
            values.map {
                it.normalizedHeader()
            }

        return normalized
            .getOrNull(0) ==
                "code agb" &&
                normalized
                    .getOrNull(1) ==
                "code ciqual"
    }

    private fun resolveSelectedColumns(
        values: List<String>
    ): List<Int> {

        val normalized =
            values.map {
                it.normalizedHeader()
            }

        fun requiredIndex(
            header: String
        ): Int {

            val normalizedHeader =
                header.normalizedHeader()

            val index =
                normalized.indexOf(
                    normalizedHeader
                )

            require(
                index >= 0
            ) {
                "Required Agribalyse column not found: " +
                        "'$header'"
            }

            return index
        }

        fun requiredOccurrence(
            header: String,
            occurrence: Int
        ): Int {

            require(
                occurrence > 0
            ) {
                "Header occurrence must be > 0."
            }

            val normalizedHeader =
                header.normalizedHeader()

            val matches =
                normalized
                    .mapIndexedNotNull { index, value ->
                        index.takeIf {
                            value ==
                                    normalizedHeader
                        }
                    }

            require(
                matches.size >= occurrence
            ) {
                "Required Agribalyse column occurrence not found: " +
                        "'$header', occurrence=$occurrence, " +
                        "found=${matches.size}"
            }

            return matches[
                occurrence - 1
            ]
        }

        return listOf(
            requiredIndex(
                "Code AGB"
            ),
            requiredIndex(
                "Code CIQUAL"
            ),
            requiredIndex(
                "Groupe d'aliment"
            ),
            requiredIndex(
                "Sous-groupe d'aliment"
            ),
            requiredIndex(
                "Nom du Produit en Français"
            ),
            requiredIndex(
                "LCI Name"
            ),
            requiredIndex(
                "DQR - Note de qualité de la donnée " +
                        "(1 excellente ; 5 très faible)"
            ),
            requiredIndex(
                "mPt/kg de produit"
            ),

            /*
             * 1. Vorkommen:
             * Changement climatique – total.
             */
            requiredOccurrence(
                header =
                    "kg CO2 eq/kg de produit",
                occurrence =
                    1
            ),

            requiredIndex(
                "Pt/kg de produit"
            ),
            requiredIndex(
                "m3 depriv./kg de produit"
            ),

            /*
             * Die Synthese-Tabelle besitzt vier identisch benannte
             * CO2-Spalten. Ihre Reihenfolge ist in AGRIBALYSE 3.2:
             *
             * 1 = total
             * 2 = biogenic
             * 3 = fossil
             * 4 = land-use change
             */
            requiredOccurrence(
                header =
                    "kg CO2 eq/kg de produit",
                occurrence =
                    2
            ),
            requiredOccurrence(
                header =
                    "kg CO2 eq/kg de produit",
                occurrence =
                    3
            ),
            requiredOccurrence(
                header =
                    "kg CO2 eq/kg de produit",
                occurrence =
                    4
            )
        )
    }

    private fun currentRowValues():
            List<String> {

        val maxColumn =
            currentRow
                .keys
                .maxOrNull()
                ?: return emptyList()

        return (0..maxColumn)
            .map { index ->
                currentRow[
                    index
                ].orEmpty()
            }
    }
}

private fun String.normalizedHeader(): String =
    trim()
        .lowercase()
        .replace(
            Regex("\\s+"),
            " "
        )

private fun String.toColumnIndex(): Int {

    val letters =
        takeWhile { char ->
            char.isLetter()
        }

    var result =
        0

    letters.forEach { char ->

        result *=
            26

        result +=
            char.uppercaseChar() -
                    'A' +
                    1
    }

    return result - 1
}