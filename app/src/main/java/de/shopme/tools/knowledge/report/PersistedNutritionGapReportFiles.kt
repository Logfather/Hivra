package de.shopme.tools.knowledge.report

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import java.io.File
import java.io.PrintStream
import java.util.Locale

/**
 * Vollständig aufgelöste Eingabedateien für den persistierten
 * Nutrition-Gap-Report.
 */
data class PersistedNutritionGapReportFiles(
    val catalogFile: File,
    val serverArtifactFile: File,
    val candidateRetrievalFile: File,
    val decisionFile: File,
    val validationFile: File?,
    val catalogServerMappingFile: File?,
    val diagnosticsFile: File?
)

/**
 * Ergebnis eines produktiven Report-Laufs.
 */
data class PersistedNutritionGapReportResult(
    val files: PersistedNutritionGapReportFiles,
    val report: RuntimeKnowledgeGapReport
)

/**
 * Baut und klassifiziert Nutrition-Gaps ausschließlich aus persistierten
 * Pipeline-Artefakten.
 *
 * Diese Komponente:
 *
 * - führt kein Candidate Retrieval aus,
 * - führt keinen AI-Aufruf aus,
 * - validiert keine Entscheidung erneut,
 * - verändert keine Mappings,
 * - verändert keine Runtime-Artefakte.
 */
class PersistedNutritionGapClassificationReporter(
    private val inputBuilder: RuntimeKnowledgeGapInputBuilder =
        RuntimeKnowledgeGapInputBuilder(),
    private val classifier: RuntimeKnowledgeGapClassifier =
        RuntimeKnowledgeGapClassifier()
) {

    fun run(
        files: PersistedNutritionGapReportFiles,
        output: PrintStream = System.out
    ): PersistedNutritionGapReportResult {

        val inputs =
            inputBuilder.build(
                catalogFile =
                    files.catalogFile,
                serverArtifactFile =
                    files.serverArtifactFile,
                candidateRetrievalFile =
                    files.candidateRetrievalFile,
                decisionFile =
                    files.decisionFile,
                validationFile =
                    files.validationFile,
                catalogServerMappingFile =
                    files.catalogServerMappingFile,
                diagnosticsFile =
                    files.diagnosticsFile
            )

        val report =
            classifier.classify(
                inputs = inputs
            )

        printReport(
            report = report,
            output = output
        )

        return PersistedNutritionGapReportResult(
            files = files,
            report = report
        )
    }

    private fun printReport(
        report: RuntimeKnowledgeGapReport,
        output: PrintStream
    ) {
        output.println()
        output.println(SEPARATOR)
        output.println("PERSISTED NUTRITION GAP CLASSIFICATION")
        output.println(SEPARATOR)
        output.println()

        output.printCount(
            label = "Missing catalog keys",
            count = report.totalCount
        )

        output.println()
        output.println(CLASSIFICATION_SEPARATOR)

        RuntimeKnowledgeGapType.entries
            .forEach { type ->
                output.printCount(
                    label = type.name,
                    count = report.count(type)
                )
            }

        output.println(CLASSIFICATION_SEPARATOR)

        output.printCount(
            label = "Classified",
            count = report.counts.values.sum()
        )

        output.println()

        val classificationComplete =
            report.totalCount ==
                    report.counts.values.sum()

        output.println(
            "Complete classification : " +
                    if (classificationComplete) {
                        "YES"
                    } else {
                        "NO"
                    }
        )

        output.println()
        output.println(SEPARATOR)

        printGapDetails(
            report = report,
            output = output
        )
    }

    private fun printGapDetails(
        report: RuntimeKnowledgeGapReport,
        output: PrintStream
    ) {
        if (report.gaps.isEmpty()) {
            output.println("No remaining nutrition gaps.")
            output.println(SEPARATOR)

            return
        }

        output.println()
        output.println("DETAILS")
        output.println(SEPARATOR)

        RuntimeKnowledgeGapType.entries
            .forEach { type ->

                val gaps =
                    report.gaps
                        .filter {
                            it.type == type
                        }

                if (gaps.isEmpty()) {
                    return@forEach
                }

                output.println()
                output.println(
                    "${type.name} (${gaps.size})"
                )

                gaps.forEach { gap ->
                    output.println(
                        formatGap(
                            gap = gap
                        )
                    )
                }
            }

        output.println()
        output.println(SEPARATOR)
    }

    private fun formatGap(
        gap: RuntimeKnowledgeGap
    ): String =
        buildString {
            append("- ")
            append(gap.catalogKey)

            append(" | candidates=")
            append(gap.candidateCount)

            gap.selectedServerKey
                ?.takeIf(String::isNotBlank)
                ?.let { selectedServerKey ->
                    append(" | selected=")
                    append(selectedServerKey)
                }

            gap.confidence
                ?.let { confidence ->
                    append(" | confidence=")
                    append(
                        String.format(
                            Locale.ROOT,
                            "%.4f",
                            confidence
                        )
                    )
                }

            gap.details
                ?.takeIf(String::isNotBlank)
                ?.let { details ->
                    append(" | ")
                    append(
                        details
                            .replace(
                                lineBreakRegex,
                                " "
                            )
                            .trim()
                    )
                }
        }

    private fun PrintStream.printCount(
        label: String,
        count: Int
    ) {
        printf(
            Locale.ROOT,
            "%-30s %8d%n",
            "$label:",
            count
        )
    }

    private companion object {

        const val SEPARATOR =
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

        const val CLASSIFICATION_SEPARATOR =
            "------------------------------------------------------------"

        val lineBreakRegex =
            Regex("[\\r\\n]+")
    }
}

/**
 * Manueller produktiver Einstiegspunkt.
 *
 * Unterstützte Argumente:
 *
 * --catalog=<path>
 * --nutrition=<path>
 * --candidates=<path>
 * --decisions=<path>
 * --validations=<path>
 * --mappings=<path>
 *
 * Validations und Mappings können mit dem Wert "none" deaktiviert werden.
 *
 * Ohne Argumente werden die bekannten Projektpfade verwendet.
 */

object RunPersistedNutritionGapClassificationReport {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {
        val arguments =
            parseArguments(
                args = args
            )

        val paths =
            KnowledgeBuildPaths.default()

        paths.ensureBuildDirectories()

        val files =
            PersistedNutritionGapReportFiles(
                catalogFile =
                    arguments.file(
                        name = "catalog",
                        defaultFile =
                            paths.canonicalFoodCatalog
                    ),
                serverArtifactFile =
                    arguments.file(
                        name = "nutrition",
                        defaultFile =
                            paths.serverArtifact(
                                "nutrition.json"
                            )
                    ),
                candidateRetrievalFile =
                    arguments.file(
                        name = "candidates",
                        defaultFile =
                            paths.nutritionMatchRequests
                    ),
                decisionFile =
                    arguments.file(
                        name = "decisions",
                        defaultFile =
                            paths.nutritionMatchDecisions
                    ),
                validationFile =
                    arguments.optionalFile(
                        name = "validations",
                        defaultFile =
                            paths.nutritionValidationReport
                    ),
                catalogServerMappingFile =
                    arguments.optionalFile(
                        name = "mappings",
                        defaultFile =
                            paths.catalogServerMappings
                    ),
                diagnosticsFile =
                    arguments.optionalFile(
                        name = "diagnostics",
                        defaultFile =
                            paths.nutritionMatchDiagnostics
                    )
            )

        printInputFiles(
            files = files,
            output = System.out
        )

        PersistedNutritionGapClassificationReporter()
            .run(
                files = files,
                output = System.out
            )
    }

    internal fun parseArguments(
        args: Array<String>
    ): Map<String, String> {

        val arguments =
            linkedMapOf<String, String>()

        args.forEach { argument ->

            require(
                argument.startsWith("--") &&
                        argument.contains("=")
            ) {
                "Invalid argument '$argument'. " +
                        "Expected --name=/path/to/file.json"
            }

            val name =
                argument
                    .substringAfter("--")
                    .substringBefore("=")
                    .trim()

            val value =
                argument
                    .substringAfter("=")
                    .trim()

            require(name.isNotBlank()) {
                "Argument name must not be blank: $argument"
            }

            require(value.isNotBlank()) {
                "Argument value must not be blank: $argument"
            }

            require(
                name in supportedArguments
            ) {
                "Unknown argument '--$name'. " +
                        "Supported arguments: " +
                        supportedArguments
                            .sorted()
                            .joinToString()
            }

            require(
                name !in arguments
            ) {
                "Duplicate argument '--$name'."
            }

            arguments[name] =
                value
        }

        return arguments
    }

    private fun printInputFiles(
        files: PersistedNutritionGapReportFiles,
        output: PrintStream
    ) {
        output.println()
        output.println(
            "Persisted nutrition gap report inputs:"
        )

        output.println(
            "Catalog     : ${files.catalogFile.path}"
        )

        output.println(
            "Nutrition   : ${files.serverArtifactFile.path}"
        )

        output.println(
            "Candidates  : ${files.candidateRetrievalFile.path}"
        )

        output.println(
            "Decisions   : ${files.decisionFile.path}"
        )

        output.println(
            "Validations : " +
                    (
                            files.validationFile
                                ?.path
                                ?: "disabled"
                            )
        )

        output.println(
            "Mappings    : " +
                    (
                            files.catalogServerMappingFile
                                ?.path
                                ?: "disabled"
                            )
        )

        output.println(
            "Diagnostics : " +
                    (
                            files.diagnosticsFile
                                ?.path
                                ?: "disabled"
                            )
        )
    }

    private fun Map<String, String>.file(
        name: String,
        defaultFile: File
    ): File =
        get(name)
            ?.let(::File)
            ?: defaultFile

    private fun Map<String, String>.optionalFile(
        name: String,
        defaultFile: File
    ): File? {

        val value =
            get(name)
                ?: return defaultFile

        if (
            value.equals(
                other = "none",
                ignoreCase = true
            )
        ) {
            return null
        }

        return File(value)
    }

    private val supportedArguments =
        setOf(
            "catalog",
            "nutrition",
            "candidates",
            "decisions",
            "validations",
            "mappings",
            "diagnostics"
        )
}