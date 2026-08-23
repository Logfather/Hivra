package de.shopme.testing.system.tools.knowledge.him.sources.agribalyse

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.junit.Test

class RunAgribalyseWorkbookAuditTest {

    private companion object {

        const val SOURCE_FILE_NAME =
            "AGRIBALYSE3.2_Tableur produits alimentaires_PublieAOUT25.xlsx"

        const val MAX_SAMPLE_VALUES =
            5
    }

    @Test
    fun auditAgribalyseWorkbook() {
        de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()

        val report =
            StringBuilder()

        fun line(
            value: String = ""
        ) {
            report
                .append(value)
                .append('\n')
        }

        val sourceFile =
            KnowledgeBuildPaths
                .default()
                .agribalyseSourceRoot
                .resolve("raw")
                .resolve(SOURCE_FILE_NAME)

        require(sourceFile.isFile) {
            "Agribalyse source file missing: ${sourceFile.absolutePath}"
        }

        val formatter =
            DataFormatter()

        WorkbookFactory
            .create(sourceFile)
            .use { workbook ->

                line()
                line("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                line("AGRIBALYSE WORKBOOK AUDIT")
                line("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                line("File      : ${sourceFile.path}")
                line("Size      : ${sourceFile.length()} bytes")
                line("Sheets    : ${workbook.numberOfSheets}")
                line()

                for (
                sheetIndex in 0 until workbook.numberOfSheets
                ) {

                    val sheet =
                        workbook.getSheetAt(
                            sheetIndex
                        )

                    val physicalRows =
                        sheet.physicalNumberOfRows

                    val firstRowIndex =
                        sheet.firstRowNum

                    val lastRowIndex =
                        sheet.lastRowNum

                    val headerRow =
                        findFirstNonEmptyRow(
                            sheetRows =
                                (firstRowIndex..lastRowIndex)
                                    .mapNotNull {
                                        sheet.getRow(it)
                                    },
                            formatter =
                                formatter
                        )

                    val maxColumnCount =
                        calculateMaxColumnCount(
                            rows =
                                (firstRowIndex..lastRowIndex)
                                    .mapNotNull {
                                        sheet.getRow(it)
                                    }
                        )

                    line("──────────────────────────────────────────")
                    line(
                        "SHEET ${sheetIndex + 1}/${workbook.numberOfSheets}"
                    )
                    line("Name          : ${sheet.sheetName}")
                    line("Physical rows : $physicalRows")
                    line("First row     : $firstRowIndex")
                    line("Last row      : $lastRowIndex")
                    line("Max columns   : $maxColumnCount")

                    if (headerRow == null) {

                        line("Header        : <not detected>")
                        line()

                        continue
                    }

                    line(
                        "Header row    : ${headerRow.rowNum}"
                    )

                    line()

                    for (
                    columnIndex in 0 until maxColumnCount
                    ) {

                        val header =
                            formatter
                                .formatCellValue(
                                    headerRow.getCell(
                                        columnIndex,
                                        Row.MissingCellPolicy.RETURN_BLANK_AS_NULL
                                    )
                                )
                                .trim()
                                .ifBlank {
                                    "<EMPTY_HEADER>"
                                }

                        val stats =
                            analyzeColumn(
                                rows =
                                    (headerRow.rowNum + 1..lastRowIndex)
                                        .mapNotNull {
                                            sheet.getRow(it)
                                        },
                                columnIndex =
                                    columnIndex,
                                formatter =
                                    formatter
                            )

                        line(
                            buildString {

                                append("Column ")
                                append(
                                    (columnIndex + 1)
                                        .toString()
                                        .padStart(
                                            3,
                                            '0'
                                        )
                                )

                                append(" | ")

                                append(header)

                                appendLine()

                                append(
                                    "    filled="
                                )
                                append(stats.filled)

                                append(
                                    ", blank="
                                )
                                append(stats.blank)

                                append(
                                    ", types="
                                )
                                append(
                                    stats.cellTypes
                                        .sorted()
                                        .joinToString()
                                )

                                if (stats.samples.isNotEmpty()) {

                                    appendLine()

                                    append(
                                        "    samples="
                                    )

                                    append(
                                        stats.samples
                                            .joinToString(
                                                separator = " | "
                                            )
                                    )
                                }
                            }
                        )
                    }

                    line()
                }

                line("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                line("AGRIBALYSE WORKBOOK AUDIT COMPLETE")
                line("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            }

        val reportFile =
            KnowledgeBuildPaths
                .default()
                .projectRoot
                .resolve("build/knowledge/reports/agribalyse")
                .resolve("agribalyse-workbook-audit.txt")

        require(
            reportFile.parentFile.mkdirs() ||
                    reportFile.parentFile.isDirectory
        ) {
            "Could not create Agribalyse audit report directory: " +
                    reportFile.parentFile.absolutePath
        }

        reportFile.writeText(
            report.toString()
        )

        println(
            "Agribalyse workbook audit written to: " +
                    reportFile.absolutePath
        )
    }

    private fun findFirstNonEmptyRow(
        sheetRows: List<Row>,
        formatter: DataFormatter
    ): Row? {

        return sheetRows
            .firstOrNull { row ->

                row.any { cell ->

                    formatter
                        .formatCellValue(
                            cell
                        )
                        .isNotBlank()
                }
            }
    }

    private fun calculateMaxColumnCount(
        rows: List<Row>
    ): Int {

        return rows
            .maxOfOrNull {
                it.lastCellNum
                    .toInt()
                    .coerceAtLeast(
                        0
                    )
            }
            ?: 0
    }

    private fun analyzeColumn(
        rows: List<Row>,
        columnIndex: Int,
        formatter: DataFormatter
    ): ColumnStats {

        var filled =
            0

        var blank =
            0

        val types =
            linkedSetOf<String>()

        val samples =
            linkedSetOf<String>()

        rows.forEach { row ->

            val cell =
                row.getCell(
                    columnIndex,
                    Row.MissingCellPolicy.RETURN_BLANK_AS_NULL
                )

            if (cell == null) {

                blank++

                return@forEach
            }

            val formattedValue =
                formatter
                    .formatCellValue(
                        cell
                    )
                    .trim()

            if (formattedValue.isBlank()) {

                blank++

                return@forEach
            }

            filled++

            types +=
                effectiveCellType(
                    cell
                )

            if (
                samples.size <
                MAX_SAMPLE_VALUES
            ) {

                samples +=
                    sanitizeSample(
                        formattedValue
                    )
            }
        }

        return ColumnStats(
            filled =
                filled,
            blank =
                blank,
            cellTypes =
                types,
            samples =
                samples
        )
    }

    private fun effectiveCellType(
        cell: Cell
    ): String {

        return when (
            cell.cellType
        ) {

            CellType.FORMULA ->
                "FORMULA:${cell.cachedFormulaResultType.name}"

            else ->
                cell.cellType.name
        }
    }

    private fun sanitizeSample(
        value: String
    ): String {

        return value
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

    private data class ColumnStats(

        val filled: Int,

        val blank: Int,

        val cellTypes: Set<String>,

        val samples: Set<String>
    )
}
