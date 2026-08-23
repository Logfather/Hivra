package de.shopme.tools.knowledge.him.sources.agribalyse

import com.google.gson.Gson
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.BufferedWriter
import java.io.File
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.zip.GZIPOutputStream

class AgribalyseHimSourceExporter(
    private val gson: Gson = Gson()
) {

    fun export(
        sourceWorkbook: File,
        outputFile: File
    ): AgribalyseHimSourceExportResult {

        require(sourceWorkbook.isFile) {
            "AGRIBALYSE workbook does not exist: ${sourceWorkbook.absolutePath}"
        }

        require(sourceWorkbook.canRead()) {
            "AGRIBALYSE workbook is not readable: ${sourceWorkbook.absolutePath}"
        }

        outputFile.parentFile?.mkdirs()

        val formatter =
            DataFormatter(
                Locale.ROOT
            )

        var exportedRecords = 0
        var detectedHeaderRow = -1
        var detectedDataStartRow = -1

        sourceWorkbook.inputStream().use { input ->

            WorkbookFactory.create(input).use { workbook ->

                val sheet =
                    requireNotNull(
                        workbook.getSheet(SHEET_NAME)
                    ) {
                        "AGRIBALYSE workbook does not contain sheet '$SHEET_NAME'."
                    }

                detectedHeaderRow =
                    findHeaderRow(
                        sheet = sheet,
                        formatter = formatter
                    ).rowNum

                val headerRow =
                    findHeaderRow(
                        sheet = sheet,
                        formatter = formatter
                    )

                BufferedWriter(
                    OutputStreamWriter(
                        GZIPOutputStream(
                            outputFile.outputStream()
                        ),
                        StandardCharsets.UTF_8
                    )
                ).use { writer ->

                    for (
                    rowIndex in
                    detectedDataStartRow..sheet.lastRowNum
                    ) {

                        val row =
                            sheet.getRow(rowIndex)
                                ?: continue

                        if (
                            isEmptyProductRow(
                                row = row,
                                formatter = formatter
                            )
                        ) {
                            continue
                        }

                        val record =
                            mapRecord(
                                row = row,
                                formatter = formatter
                            )

                        writer.write(
                            gson.toJson(record)
                        )

                        writer.newLine()

                        exportedRecords++
                    }
                }
            }
        }

        require(
            exportedRecords == EXPECTED_PRODUCT_ROWS
        ) {
            "Unexpected AGRIBALYSE Synthese product count. " +
                    "Expected $EXPECTED_PRODUCT_ROWS but exported $exportedRecords."
        }

        require(
            outputFile.isFile &&
                    outputFile.length() > 0L
        ) {
            "AGRIBALYSE HIM source artifact was not created correctly: " +
                    outputFile.absolutePath
        }

        return AgribalyseHimSourceExportResult(
            sourceWorkbook = sourceWorkbook,
            outputFile = outputFile,
            headerRowIndex = detectedHeaderRow,
            dataStartRowIndex = detectedDataStartRow,
            exportedRecords = exportedRecords
        )
    }

    private fun findHeaderRow(
        sheet: Sheet,
        formatter: DataFormatter
    ): Row {

        return requireNotNull(
            findRowContaining(
                sheet = sheet,
                formatter = formatter,
                expectedValue = "Code AGB"
            )
        ) {
            "Could not locate AGRIBALYSE Synthese header row " +
                    "containing 'Code AGB'."
        }
    }

    private fun findRowContaining(
        sheet: Sheet,
        formatter: DataFormatter,
        expectedValue: String
    ): Row? {

        val expected =
            normalizeStructuralText(
                expectedValue
            )

        return (
                sheet.firstRowNum..
                        sheet.lastRowNum
                )
            .asSequence()
            .mapNotNull {
                sheet.getRow(it)
            }
            .firstOrNull { row ->

                row.any { cell ->

                    normalizeStructuralText(
                        formatter
                            .formatCellValue(
                                cell
                            )
                    ) == expected
                }
            }
    }

    private fun normalizeStructuralText(
        value: String
    ): String {

        return value
            .replace(
                '\u00A0',
                ' '
            )
            .replace(
                '\u202F',
                ' '
            )
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()
            .lowercase()
    }

    private fun validateHeader(
        row: Row,
        formatter: DataFormatter
    ) {

        require(
            row.lastCellNum.toInt() >=
                    EXPECTED_COLUMN_COUNT
        ) {
            "AGRIBALYSE Synthese header has fewer than " +
                    "$EXPECTED_COLUMN_COUNT columns."
        }

        val firstHeader =
            stringValue(
                row = row,
                columnIndex = 0,
                formatter = formatter
            )

        require(
            firstHeader.contains(
                HEADER_MARKER,
                ignoreCase = true
            )
        ) {
            "Unexpected AGRIBALYSE Synthese first column: '$firstHeader'."
        }
    }

    private fun mapRecord(
        row: Row,
        formatter: DataFormatter
    ): AgribalyseHimSourceRecord =
        AgribalyseHimSourceRecord(
            agbCode =
                stringValue(row, 0, formatter),

            ciqualCode =
                stringValue(row, 1, formatter),

            foodGroup =
                stringValue(row, 2, formatter),

            foodSubgroup =
                stringValue(row, 3, formatter),

            productNameFr =
                stringValue(row, 4, formatter),

            lciName =
                stringValue(row, 5, formatter),

            seasonCode =
                stringValue(row, 6, formatter),

            airTransportCode =
                stringValue(row, 7, formatter),

            delivery =
                stringValue(row, 8, formatter),

            packagingApproach =
                stringValue(row, 9, formatter),

            preparation =
                stringValue(row, 10, formatter),

            dataQualityRating =
                numericValue(row, 11),

            efSingleScore =
                numericValue(row, 12),

            climateChange =
                numericValue(row, 13),

            ozoneDepletion =
                numericValue(row, 14),

            ionisingRadiation =
                numericValue(row, 15),

            photochemicalOzoneFormation =
                numericValue(row, 16),

            particulateMatter =
                numericValue(row, 17),

            humanToxicityNonCancer =
                numericValue(row, 18),

            humanToxicityCancer =
                numericValue(row, 19),

            terrestrialAndFreshwaterAcidification =
                numericValue(row, 20),

            freshwaterEutrophication =
                numericValue(row, 21),

            marineEutrophication =
                numericValue(row, 22),

            terrestrialEutrophication =
                numericValue(row, 23),

            freshwaterEcotoxicity =
                numericValue(row, 24),

            landUse =
                numericValue(row, 25),

            waterResourceDepletion =
                numericValue(row, 26),

            energyResourceDepletion =
                numericValue(row, 27),

            mineralResourceDepletion =
                numericValue(row, 28),

            climateChangeBiogenic =
                numericValue(row, 29),

            climateChangeFossil =
                numericValue(row, 30),

            climateChangeLandUseChange =
                numericValue(row, 31)
        )

    private fun stringValue(
        row: Row,
        columnIndex: Int,
        formatter: DataFormatter
    ): String =
        formatter
            .formatCellValue(
                row.getCell(
                    columnIndex,
                    Row.MissingCellPolicy.RETURN_BLANK_AS_NULL
                )
            )
            .trim()

    private fun numericValue(
        row: Row,
        columnIndex: Int
    ): Double? {

        val cell =
            row.getCell(
                columnIndex,
                Row.MissingCellPolicy.RETURN_BLANK_AS_NULL
            )
                ?: return null

        return when (
            cell.cellType
        ) {

            CellType.NUMERIC ->
                cell.numericCellValue

            CellType.FORMULA ->
                when (
                    cell.cachedFormulaResultType
                ) {
                    CellType.NUMERIC ->
                        cell.numericCellValue

                    else ->
                        null
                }

            CellType.BLANK ->
                null

            else ->
                null
        }
    }

    private fun isEmptyProductRow(
        row: Row,
        formatter: DataFormatter
    ): Boolean =
        stringValue(
            row = row,
            columnIndex = 0,
            formatter = formatter
        ).isBlank()

    companion object {

        private const val SHEET_NAME =
            "Synthese"

        private const val HEADER_MARKER =
            "Code AGB"

        private const val EXPECTED_COLUMN_COUNT =
            32

        private const val EXPECTED_PRODUCT_ROWS =
            2458
    }
}

data class AgribalyseHimSourceExportResult(

    val sourceWorkbook: File,

    val outputFile: File,

    /**
     * Zero-based Apache POI row index.
     */
    val headerRowIndex: Int,

    /**
     * Zero-based Apache POI row index.
     */
    val dataStartRowIndex: Int,

    val exportedRecords: Int
)