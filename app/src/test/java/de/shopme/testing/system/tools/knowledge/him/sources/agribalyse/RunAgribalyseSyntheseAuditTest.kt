package de.shopme.testing.system.tools.knowledge.him.sources.agribalyse

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.junit.Test

class RunAgribalyseSyntheseAuditTest {

    private companion object {

        const val SOURCE_FILE_NAME =
            "AGRIBALYSE3.2_Tableur produits alimentaires_PublieAOUT25.xlsx"

        const val SHEET_NAME =
            "Synthese"

        const val EXPECTED_COLUMN_COUNT =
            32

        const val MAX_DISTINCT_VALUES_TO_PRINT =
            50

        const val MAX_SAMPLE_VALUES =
            5
    }

    @Test
    fun auditAgribalyseSynthese() {
        de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()

        val paths =
            KnowledgeBuildPaths.default()

        val sourceFile =
            paths
                .agribalyseSourceRoot
                .resolve("raw")
                .resolve(SOURCE_FILE_NAME)

        require(sourceFile.isFile) {
            "Agribalyse source file missing: ${sourceFile.absolutePath}"
        }

        val reportFile =
            paths.projectRoot
                .resolve(
                    "build/knowledge/reports/agribalyse/" +
                            "agribalyse-synthese-audit.txt"
                )

        val reportDirectory =
            requireNotNull(
                reportFile.parentFile
            ) {
                "Agribalyse audit report has no parent directory: " +
                        reportFile.absolutePath
            }

        require(
            reportDirectory.mkdirs() ||
                    reportDirectory.isDirectory
        ) {
            "Could not create report directory: " +
                    reportDirectory.absolutePath
        }

        val formatter =
            DataFormatter()

        val report =
            StringBuilder()

        fun line(
            value: String = ""
        ) {
            report
                .append(value)
                .append('\n')
        }

        WorkbookFactory
            .create(sourceFile)
            .use { workbook ->

                val sheet =
                    requireNotNull(
                        workbook.getSheet(SHEET_NAME)
                    ) {
                        "Agribalyse sheet missing: $SHEET_NAME"
                    }

                line("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                line("AGRIBALYSE SYNTHÈSE AUDIT")
                line("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                line("File   : ${sourceFile.absolutePath}")
                line("Sheet  : ${sheet.sheetName}")
                line("Rows   : ${sheet.physicalNumberOfRows}")
                line()

                val codeHeaderRow =
                    findRowContaining(
                        sheet = sheet,
                        formatter = formatter,
                        expectedValue = "Code AGB"
                    )

                line("FIRST STRUCTURAL ROWS")
                line("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                for (
                rowIndex in
                sheet.firstRowNum..minOf(
                    sheet.lastRowNum,
                    10
                )
                ) {

                    val row =
                        sheet.getRow(rowIndex)

                    if (row == null) {

                        line(
                            "Row $rowIndex: <NULL>"
                        )

                        continue
                    }

                    val values =
                        (
                                0 until minOf(
                                    row.lastCellNum
                                        .toInt()
                                        .coerceAtLeast(0),
                                    EXPECTED_COLUMN_COUNT
                                )
                                )
                            .map { columnIndex ->

                                formatter
                                    .formatCellValue(
                                        row.getCell(
                                            columnIndex,
                                            Row.MissingCellPolicy
                                                .RETURN_BLANK_AS_NULL
                                        )
                                    )
                                    .replace(
                                        '\u00A0',
                                        ' '
                                    )
                                    .replace(
                                        '\u202F',
                                        ' '
                                    )
                                    .replace(
                                        '\n',
                                        ' '
                                    )
                                    .replace(
                                        '\r',
                                        ' '
                                    )
                                    .trim()
                            }

                    line(
                        "Row $rowIndex: " +
                                values
                                    .mapIndexed {
                                            index,
                                            value ->

                                        "C${index + 1}=[$value]"
                                    }
                                    .joinToString(
                                        separator = " | "
                                    )
                    )
                }

                line()

                if (codeHeaderRow == null) {

                    reportFile.writeText(
                        report.toString()
                    )

                    error(
                        "Could not locate Synthese header row " +
                                "containing 'Code AGB'. " +
                                "Structural report written to: " +
                                reportFile.absolutePath
                    )
                }

                /*
                 * Synthese structure:
                 *
                 * Row before Code AGB:
                 * environmental indicator names
                 *
                 * Code AGB row:
                 * C1-C12 = field names
                 * C13-C32 = units
                 *
                 * Following row:
                 * first product record
                 */
                val headerRowIndex =
                    codeHeaderRow.rowNum

                val indicatorRowIndex =
                    headerRowIndex - 1

                val dataStartRowIndex =
                    headerRowIndex + 1

                line(
                    "Detected indicator row : $indicatorRowIndex"
                )

                line(
                    "Detected header row    : $headerRowIndex"
                )

                line(
                    "Detected data start    : $dataStartRowIndex"
                )

                line()

                val indicatorRow =
                    requireNotNull(
                        sheet.getRow(
                            indicatorRowIndex
                        )
                    ) {
                        "Indicator row missing at index " +
                                indicatorRowIndex
                    }

                val headerRow =
                    requireNotNull(
                        sheet.getRow(
                            headerRowIndex
                        )
                    ) {
                        "Header row missing at index " +
                                headerRowIndex
                    }

                val columnCount =
                    maxOf(
                        indicatorRow
                            .lastCellNum
                            .toInt(),

                        headerRow
                            .lastCellNum
                            .toInt()
                    )

                require(
                    columnCount ==
                            EXPECTED_COLUMN_COUNT
                ) {
                    "Unexpected Synthese column count: " +
                            "$columnCount != " +
                            EXPECTED_COLUMN_COUNT
                }

                val rows =
                    (
                            dataStartRowIndex..
                                    sheet.lastRowNum
                            )
                        .mapNotNull {
                            sheet.getRow(it)
                        }
                        .filter {
                            isProductRow(
                                row = it,
                                formatter = formatter
                            )
                        }

                line(
                    "Detected product rows: ${rows.size}"
                )

                line()

                val columns =
                    (0 until columnCount)
                        .map { columnIndex ->

                            analyzeColumn(
                                columnIndex = columnIndex,
                                indicatorRow = indicatorRow,
                                headerRow = headerRow,
                                rows = rows,
                                formatter = formatter
                            )
                        }

                line("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                line("COLUMN STRUCTURE")
                line("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                line()

                columns.forEach { column ->

                    line(
                        "Column ${
                            column.index
                                .toString()
                                .padStart(
                                    2,
                                    '0'
                                )
                        }"
                    )

                    line(
                        "  indicator : ${column.indicator}"
                    )

                    line(
                        "  header    : ${column.header}"
                    )

                    line(
                        "  filled    : ${column.filled}"
                    )

                    line(
                        "  blank     : ${column.blank}"
                    )

                    line(
                        "  distinct  : ${column.distinctCount}"
                    )

                    line(
                        "  types     : ${
                            column.types.joinToString()
                        }"
                    )

                    if (
                        column.sampleValues.isNotEmpty()
                    ) {

                        line(
                            "  samples   : ${
                                column.sampleValues
                                    .joinToString(
                                        separator = " | "
                                    )
                            }"
                        )
                    }

                    line()
                }

                line("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                line("IDENTITY DUPLICATES")
                line("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                line()

                auditDuplicates(
                    title = "Code AGB",
                    columnIndex = 0,
                    rows = rows,
                    formatter = formatter,
                    line = ::line
                )

                auditDuplicates(
                    title = "Code CIQUAL",
                    columnIndex = 1,
                    rows = rows,
                    formatter = formatter,
                    line = ::line
                )

                line("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                line("CONTROLLED / LOW-CARDINALITY VALUES")
                line("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                line()

                auditDistinctValues(
                    title = "code saison",
                    columnIndex = 6,
                    rows = rows,
                    formatter = formatter,
                    line = ::line
                )

                auditDistinctValues(
                    title = "code avion",
                    columnIndex = 7,
                    rows = rows,
                    formatter = formatter,
                    line = ::line
                )

                auditDistinctValues(
                    title = "Livraison",
                    columnIndex = 8,
                    rows = rows,
                    formatter = formatter,
                    line = ::line
                )

                auditDistinctValues(
                    title = "Approche emballage",
                    columnIndex = 9,
                    rows = rows,
                    formatter = formatter,
                    line = ::line
                )

                auditDistinctValues(
                    title = "Préparation",
                    columnIndex = 10,
                    rows = rows,
                    formatter = formatter,
                    line = ::line
                )

                line("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                line("AGRIBALYSE SYNTHÈSE AUDIT COMPLETE")
                line("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            }

        reportFile.writeText(
            report.toString()
        )

        println(
            "Agribalyse Synthese audit written to: " +
                    reportFile.absolutePath
        )
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

    private fun isProductRow(
        row: Row,
        formatter: DataFormatter
    ): Boolean {

        val agbCode =
            formatter
                .formatCellValue(
                    row.getCell(
                        0,
                        Row.MissingCellPolicy
                            .RETURN_BLANK_AS_NULL
                    )
                )
                .trim()

        val productName =
            formatter
                .formatCellValue(
                    row.getCell(
                        4,
                        Row.MissingCellPolicy
                            .RETURN_BLANK_AS_NULL
                    )
                )
                .trim()

        return agbCode.isNotBlank() &&
                productName.isNotBlank()
    }

    private fun analyzeColumn(
        columnIndex: Int,
        indicatorRow: Row,
        headerRow: Row,
        rows: List<Row>,
        formatter: DataFormatter
    ): ColumnAudit {

        val indicator =
            formatter
                .formatCellValue(
                    indicatorRow.getCell(
                        columnIndex,
                        Row.MissingCellPolicy
                            .RETURN_BLANK_AS_NULL
                    )
                )
                .trim()
                .ifBlank {
                    "<NONE>"
                }

        val header =
            formatter
                .formatCellValue(
                    headerRow.getCell(
                        columnIndex,
                        Row.MissingCellPolicy
                            .RETURN_BLANK_AS_NULL
                    )
                )
                .trim()
                .ifBlank {
                    "<EMPTY>"
                }

        var filled =
            0

        var blank =
            0

        val distinctValues =
            linkedSetOf<String>()

        val sampleValues =
            linkedSetOf<String>()

        val types =
            linkedSetOf<String>()

        rows.forEach { row ->

            val cell =
                row.getCell(
                    columnIndex,
                    Row.MissingCellPolicy
                        .RETURN_BLANK_AS_NULL
                )

            if (cell == null) {

                blank++

                return@forEach
            }

            val value =
                formatter
                    .formatCellValue(
                        cell
                    )
                    .trim()

            if (value.isBlank()) {

                blank++

                return@forEach
            }

            filled++

            distinctValues +=
                value

            types +=
                cell.cellType.name

            if (
                sampleValues.size <
                MAX_SAMPLE_VALUES
            ) {

                sampleValues +=
                    value
                        .replace(
                            '\n',
                            ' '
                        )
                        .replace(
                            '\r',
                            ' '
                        )
                        .take(
                            160
                        )
            }
        }

        return ColumnAudit(
            index = columnIndex + 1,
            indicator = indicator,
            header = header,
            filled = filled,
            blank = blank,
            distinctCount =
                distinctValues.size,
            types = types,
            sampleValues = sampleValues
        )
    }

    private fun auditDuplicates(
        title: String,
        columnIndex: Int,
        rows: List<Row>,
        formatter: DataFormatter,
        line: (String) -> Unit
    ) {

        val values =
            rows
                .mapNotNull { row ->

                    formatter
                        .formatCellValue(
                            row.getCell(
                                columnIndex,
                                Row.MissingCellPolicy
                                    .RETURN_BLANK_AS_NULL
                            )
                        )
                        .trim()
                        .takeIf {
                            it.isNotBlank()
                        }
                }

        val duplicates =
            values
                .groupingBy {
                    it
                }
                .eachCount()
                .filterValues {
                    it > 1
                }
                .toSortedMap()

        line("$title:")
        line("  values     : ${values.size}")
        line("  distinct   : ${values.toSet().size}")
        line("  duplicates : ${duplicates.size}")

        duplicates
            .entries
            .take(
                100
            )
            .forEach { (value, count) ->

                line(
                    "    $value -> $count"
                )
            }

        line("")
    }

    private fun auditDistinctValues(
        title: String,
        columnIndex: Int,
        rows: List<Row>,
        formatter: DataFormatter,
        line: (String) -> Unit
    ) {

        val counts =
            rows
                .mapNotNull { row ->

                    formatter
                        .formatCellValue(
                            row.getCell(
                                columnIndex,
                                Row.MissingCellPolicy
                                    .RETURN_BLANK_AS_NULL
                            )
                        )
                        .trim()
                        .takeIf {
                            it.isNotBlank()
                        }
                }
                .groupingBy {
                    it
                }
                .eachCount()
                .entries
                .sortedWith(
                    compareByDescending<
                            Map.Entry<String, Int>
                            > {
                        it.value
                    }.thenBy {
                        it.key
                    }
                )

        line("$title:")
        line("  distinct=${counts.size}")

        counts
            .take(
                MAX_DISTINCT_VALUES_TO_PRINT
            )
            .forEach { entry ->

                line(
                    "    ${entry.key} -> ${entry.value}"
                )
            }

        if (
            counts.size >
            MAX_DISTINCT_VALUES_TO_PRINT
        ) {

            line(
                "    ... ${
                    counts.size -
                            MAX_DISTINCT_VALUES_TO_PRINT
                } more"
            )
        }

        line("")
    }

    private data class ColumnAudit(

        val index: Int,

        val indicator: String,

        val header: String,

        val filled: Int,

        val blank: Int,

        val distinctCount: Int,

        val types: Set<String>,

        val sampleValues: Set<String>
    )
}
