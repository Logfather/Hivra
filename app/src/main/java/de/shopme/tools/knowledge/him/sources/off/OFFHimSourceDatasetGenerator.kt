package de.shopme.tools.knowledge.him.sources.off

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import java.io.BufferedWriter
import java.io.File
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object OFFHimSourceDatasetGenerator {

    fun generate(
        inputFile: File,
        outputFile: File,
        maxRecords: Int? = null
    ): OFFHimSourceDatasetGenerationResult {

        require(inputFile.isFile) {
            "OFF source dump not found: ${inputFile.absolutePath}"
        }

        require(
            maxRecords == null ||
                    maxRecords > 0
        ) {
            "maxRecords must be positive when specified."
        }

        val outputDirectory =
            requireNotNull(
                outputFile.parentFile
            ) {
                "Output file requires a parent directory: " +
                        outputFile.absolutePath
            }

        require(
            outputDirectory.exists() ||
                    outputDirectory.mkdirs()
        ) {
            "Could not create output directory: " +
                    outputDirectory.absolutePath
        }

        val temporaryFile =
            File(
                outputDirectory,
                "${outputFile.name}.tmp"
            )

        if (temporaryFile.exists()) {
            check(temporaryFile.delete()) {
                "Could not delete stale temporary file: " +
                        temporaryFile.absolutePath
            }
        }

        val gson =
            GsonBuilder()
                .disableHtmlEscaping()
                .create()

        var processed = 0L
        var validJson = 0L
        var invalidJson = 0L

        var projected = 0L
        var unprojectable = 0L

        var validationAccepted = 0L
        var validationRejected = 0L

        var written = 0L

        var inputUncompressedBytes = 0L
        var outputUncompressedBytes = 0L

        var forbiddenStructuralLeaks = 0L

        val validationFailureCounts =
            linkedMapOf<
                    OFFHimSourceRecordValidationReason,
                    Long
                    >()

        try {
            GZIPInputStream(
                inputFile
                    .inputStream()
                    .buffered()
            ).bufferedReader(
                StandardCharsets.UTF_8
            ).use { reader ->

                GZIPOutputStream(
                    temporaryFile
                        .outputStream()
                        .buffered()
                ).use { gzipOutput ->

                    BufferedWriter(
                        OutputStreamWriter(
                            gzipOutput,
                            StandardCharsets.UTF_8
                        )
                    ).use { writer ->

                        while (
                            maxRecords == null ||
                            processed < maxRecords.toLong()
                        ) {

                            val line =
                                reader.readLine()
                                    ?: break

                            if (line.isBlank()) {
                                continue
                            }

                            processed++

                            inputUncompressedBytes +=
                                utf8LineSize(line)

                            val raw =
                                runCatching {
                                    JsonParser
                                        .parseString(line)
                                }
                                    .getOrNull()
                                    ?.takeIf {
                                        it.isJsonObject
                                    }
                                    ?.asJsonObject

                            if (raw == null) {
                                invalidJson++
                                continue
                            }

                            validJson++

                            val record =
                                OFFHimProjectionPolicy
                                    .project(raw)

                            if (record == null) {
                                unprojectable++
                                continue
                            }

                            projected++

                            val validation =
                                OFFHimSourceRecordValidator
                                    .validate(record)

                            if (!validation.valid) {
                                validationRejected++

                                validation.failures
                                    .forEach { failure ->

                                        validationFailureCounts[
                                            failure.reason
                                        ] =
                                            (
                                                    validationFailureCounts[
                                                        failure.reason
                                                    ] ?: 0L
                                                    ) + 1L
                                    }

                                continue
                            }

                            validationAccepted++

                            val projectedJson =
                                gson.toJson(record)

                            val leakingKeys =
                                findForbiddenStructuralKeys(
                                    projectedJson
                                )

                            if (leakingKeys.isNotEmpty()) {

                                forbiddenStructuralLeaks +=
                                    leakingKeys.size.toLong()

                                error(
                                    "Forbidden OFF structures leaked into " +
                                            "projected HIM record " +
                                            "${record.source.code}: " +
                                            leakingKeys.joinToString()
                                )
                            }

                            writer.write(
                                projectedJson
                            )

                            writer.newLine()

                            outputUncompressedBytes +=
                                utf8LineSize(
                                    projectedJson
                                )

                            written++
                        }
                    }
                }
            }

            check(
                written ==
                        validationAccepted
            ) {
                "Written record count differs from validation accepted count: " +
                        "written=$written, " +
                        "accepted=$validationAccepted"
            }

            check(
                projected +
                        unprojectable ==
                        validJson
            ) {
                "Projection accounting mismatch: " +
                        "validJson=$validJson, " +
                        "projected=$projected, " +
                        "unprojectable=$unprojectable"
            }

            check(
                validationAccepted +
                        validationRejected ==
                        projected
            ) {
                "Validation accounting mismatch: " +
                        "projected=$projected, " +
                        "accepted=$validationAccepted, " +
                        "rejected=$validationRejected"
            }

            check(
                validJson +
                        invalidJson ==
                        processed
            ) {
                "Input accounting mismatch: " +
                        "processed=$processed, " +
                        "validJson=$validJson, " +
                        "invalidJson=$invalidJson"
            }

            check(
                forbiddenStructuralLeaks == 0L
            ) {
                "Forbidden structures leaked into generated dataset."
            }

            moveAtomically(
                source = temporaryFile,
                target = outputFile
            )

            val outputCompressedBytes =
                outputFile.length()

            return OFFHimSourceDatasetGenerationResult(
                inputFile =
                    inputFile.canonicalFile,

                outputFile =
                    outputFile.canonicalFile,

                maxRecords =
                    maxRecords,

                processed =
                    processed,

                validJson =
                    validJson,

                invalidJson =
                    invalidJson,

                projected =
                    projected,

                unprojectable =
                    unprojectable,

                validationAccepted =
                    validationAccepted,

                validationRejected =
                    validationRejected,

                written =
                    written,

                forbiddenStructuralLeaks =
                    forbiddenStructuralLeaks,

                validationFailureCounts =
                    validationFailureCounts.toMap(),

                inputUncompressedBytes =
                    inputUncompressedBytes,

                outputUncompressedBytes =
                    outputUncompressedBytes,

                outputCompressedBytes =
                    outputCompressedBytes,

                uncompressedReductionRatio =
                    reductionRatio(
                        before =
                            inputUncompressedBytes,
                        after =
                            outputUncompressedBytes
                    ),

                outputCompressionRatio =
                    ratio(
                        numerator =
                            outputCompressedBytes,
                        denominator =
                            outputUncompressedBytes
                    )
            )
        } finally {

            if (temporaryFile.exists()) {
                temporaryFile.delete()
            }
        }
    }

    private fun utf8LineSize(
        line: String
    ): Long =
        line
            .toByteArray(
                StandardCharsets.UTF_8
            )
            .size
            .toLong() + 1L

    private fun findForbiddenStructuralKeys(
        json: String
    ): List<String> =
        FORBIDDEN_STRUCTURAL_KEYS
            .filter { key ->
                json.contains(
                    "\"$key\":",
                    ignoreCase = true
                )
            }

    private fun moveAtomically(
        source: File,
        target: File
    ) {

        try {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (
            _: AtomicMoveNotSupportedException
        ) {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    private fun reductionRatio(
        before: Long,
        after: Long
    ): Double {

        if (before <= 0L) {
            return 0.0
        }

        return 1.0 -
                (
                        after.toDouble() /
                                before.toDouble()
                        )
    }

    private fun ratio(
        numerator: Long,
        denominator: Long
    ): Double {

        if (denominator <= 0L) {
            return 0.0
        }

        return numerator.toDouble() /
                denominator.toDouble()
    }

    private val FORBIDDEN_STRUCTURAL_KEYS =
        setOf(
            "ecoscore_extended_data",
            "previous_data",
            "agribalyse",
            "agribalyse_food_code",
            "agribalyse_proxy_food_code",
            "images",
            "editors_tags",
            "informers_tags",
            "photographers_tags",
            "packaging_old",
            "packaging_old_before_taxonomization",
            "packagings_materials",
            "owner_fields",
            "teams_tags",
            "states_tags",
            "debug"
        )
}

data class OFFHimSourceDatasetGenerationResult(
    val inputFile: File,
    val outputFile: File,
    val maxRecords: Int?,
    val processed: Long,
    val validJson: Long,
    val invalidJson: Long,
    val projected: Long,
    val unprojectable: Long,
    val validationAccepted: Long,
    val validationRejected: Long,
    val written: Long,
    val forbiddenStructuralLeaks: Long,
    val validationFailureCounts:
    Map<
            OFFHimSourceRecordValidationReason,
            Long
            >,
    val inputUncompressedBytes: Long,
    val outputUncompressedBytes: Long,
    val outputCompressedBytes: Long,
    val uncompressedReductionRatio: Double,
    val outputCompressionRatio: Double
) {

    val uncompressedReductionPercent: Double
        get() =
            uncompressedReductionRatio *
                    100.0

    val outputCompressionPercent: Double
        get() =
            outputCompressionRatio *
                    100.0

    fun summary(): String =
        buildString {
            appendLine(
                "=== OFF HIM SOURCE DATASET GENERATION ==="
            )

            appendLine(
                "input                     : " +
                        inputFile.absolutePath
            )

            appendLine(
                "output                    : " +
                        outputFile.absolutePath
            )

            appendLine(
                "max records               : " +
                        (
                                maxRecords
                                    ?.let {
                                        format(
                                            it.toLong()
                                        )
                                    }
                                    ?: "FULL"
                                )
            )

            appendLine(
                "processed                 : " +
                        format(processed)
            )

            appendLine(
                "valid JSON                : " +
                        format(validJson)
            )

            appendLine(
                "invalid JSON              : " +
                        format(invalidJson)
            )

            appendLine(
                "projected                 : " +
                        format(projected)
            )

            appendLine(
                "unprojectable             : " +
                        format(unprojectable)
            )

            appendLine(
                "validation accepted       : " +
                        format(validationAccepted)
            )

            appendLine(
                "validation rejected       : " +
                        format(validationRejected)
            )

            appendLine(
                "written                   : " +
                        format(written)
            )

            appendLine(
                "forbidden structural leaks: " +
                        format(
                            forbiddenStructuralLeaks
                        )
            )

            appendLine()
            appendLine(
                "=== SIZE / REDUCTION ==="
            )

            appendLine(
                "raw JSON bytes sampled    : " +
                        format(
                            inputUncompressedBytes
                        )
            )

            appendLine(
                "projected JSON bytes      : " +
                        format(
                            outputUncompressedBytes
                        )
            )

            appendLine(
                "gzip output bytes         : " +
                        format(
                            outputCompressedBytes
                        )
            )

            appendLine(
                "JSON reduction            : " +
                        formatPercent(
                            uncompressedReductionPercent
                        )
            )

            appendLine(
                "gzip/output ratio         : " +
                        formatPercent(
                            outputCompressionPercent
                        )
            )

            appendLine()
            appendLine(
                "=== VALIDATION FAILURE SUMMARY ==="
            )

            if (
                validationFailureCounts.isEmpty()
            ) {
                appendLine("none")
            } else {
                validationFailureCounts
                    .entries
                    .sortedByDescending {
                        it.value
                    }
                    .forEach { (reason, count) ->
                        appendLine(
                            "${reason.name.padEnd(36)} " +
                                    format(count)
                        )
                    }
            }

            append(
                "=== GENERATION COMPLETE ==="
            )
        }

    private fun format(
        value: Long
    ): String =
        "%,d".format(
            Locale.US,
            value
        )

    private fun formatPercent(
        value: Double
    ): String =
        "%.2f%%".format(
            Locale.US,
            value
        )
}