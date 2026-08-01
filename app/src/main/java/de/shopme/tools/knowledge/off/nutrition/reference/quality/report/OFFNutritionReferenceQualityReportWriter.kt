package de.shopme.tools.knowledge.off.nutrition.reference.quality.report

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class OFFNutritionReferenceQualityReportWriter {

    fun write(
        report: OFFNutritionReferenceQualityReport,
        outputFile: File
    ): OFFNutritionReferenceQualityReportWriteResult {

        val canonicalOutputFile =
            outputFile.canonicalFile

        val parentDirectory =
            requireNotNull(
                canonicalOutputFile.parentFile
            ) {
                "Output file must have a parent directory."
            }

        require(
            parentDirectory.exists() ||
                    parentDirectory.mkdirs()
        ) {
            "Could not create output directory: $parentDirectory"
        }

        require(parentDirectory.isDirectory) {
            "Output parent is not a directory: $parentDirectory"
        }

        val json =
            serialize(
                report =
                    report
            )

        val temporaryFile =
            File(
                parentDirectory,
                "${canonicalOutputFile.name}.tmp"
            )

        temporaryFile.writeText(
            text =
                json,
            charset =
                StandardCharsets.UTF_8
        )

        moveAtomicallyOrReplace(
            sourceFile =
                temporaryFile,
            targetFile =
                canonicalOutputFile
        )

        return OFFNutritionReferenceQualityReportWriteResult(
            outputFile =
                canonicalOutputFile,
            writtenByteCount =
                canonicalOutputFile.length()
        )
    }

    internal fun serialize(
        report: OFFNutritionReferenceQualityReport
    ): String {

        val countsJson =
            report.countsByReason
                .entries
                .joinToString(
                    separator =
                        ",\n",
                    prefix =
                        "{\n",
                    postfix =
                        "\n  }"
                ) { (reason, count) ->
                    "    \"${escapeJson(reason.name)}\": $count"
                }

        return buildString {
            appendLine("{")
            appendLine("  \"version\": ${report.version},")
            appendLine(
                "  \"inputCandidateCount\": " +
                        "${report.inputCandidateCount},"
            )
            appendLine(
                "  \"acceptedCandidateCount\": " +
                        "${report.acceptedCandidateCount},"
            )
            appendLine(
                "  \"rejectedCandidateCount\": " +
                        "${report.rejectedCandidateCount},"
            )
            appendLine(
                "  \"rejectionReasonOccurrenceCount\": " +
                        "${report.rejectionReasonOccurrenceCount},"
            )
            appendLine(
                "  \"acceptanceRate\": " +
                        "${report.acceptanceRate},"
            )
            append("  \"countsByReason\": ")
            appendLine(countsJson)
            appendLine("}")
        }
    }

    private fun moveAtomicallyOrReplace(
        sourceFile: File,
        targetFile: File
    ) {

        try {
            Files.move(
                sourceFile.toPath(),
                targetFile.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: UnsupportedOperationException) {
            Files.move(
                sourceFile.toPath(),
                targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    private fun escapeJson(
        value: String
    ): String {

        return buildString {
            value.forEach { character ->
                when (character) {
                    '\\' ->
                        append("\\\\")

                    '"' ->
                        append("\\\"")

                    '\b' ->
                        append("\\b")

                    '\u000C' ->
                        append("\\f")

                    '\n' ->
                        append("\\n")

                    '\r' ->
                        append("\\r")

                    '\t' ->
                        append("\\t")

                    else -> {
                        if (character.code < 0x20) {
                            append(
                                "\\u%04x".format(
                                    character.code
                                )
                            )
                        } else {
                            append(character)
                        }
                    }
                }
            }
        }
    }
}