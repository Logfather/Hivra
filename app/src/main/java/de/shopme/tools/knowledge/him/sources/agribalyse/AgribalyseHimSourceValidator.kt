package de.shopme.tools.knowledge.him.sources.agribalyse

import com.google.gson.Gson
import java.io.File
import java.security.MessageDigest
import java.util.zip.GZIPInputStream

class AgribalyseHimSourceValidator {

    fun validate(
        sourceFile: File,
        expectedRecordCount: Int = EXPECTED_RECORD_COUNT
    ): AgribalyseHimSourceValidationResult {

        require(sourceFile.isFile) {
            "AGRIBALYSE HIM source artifact missing: " +
                    sourceFile.absolutePath
        }

        require(sourceFile.length() > 0L) {
            "AGRIBALYSE HIM source artifact is empty: " +
                    sourceFile.absolutePath
        }

        val gson =
            Gson()

        val agbCodeCounts =
            linkedMapOf<String, Int>()

        var recordCount =
            0

        var firstRecord: AgribalyseHimSourceRecord? =
            null

        var lastRecord: AgribalyseHimSourceRecord? =
            null

        GZIPInputStream(
            sourceFile
                .inputStream()
                .buffered()
        )
            .bufferedReader()
            .useLines { lines ->

                lines.forEachIndexed {
                        index,
                        rawLine ->

                    val lineNumber =
                        index + 1

                    require(
                        rawLine.isNotBlank()
                    ) {
                        "Blank JSONL record at line $lineNumber."
                    }

                    val record =
                        try {

                            gson.fromJson(
                                rawLine,
                                AgribalyseHimSourceRecord::class.java
                            )

                        } catch (
                            exception: Exception
                        ) {

                            throw IllegalStateException(
                                "Invalid AGRIBALYSE HIM source record " +
                                        "at JSONL line $lineNumber.",
                                exception
                            )
                        }

                    requireNotNull(
                        record
                    ) {
                        "Null AGRIBALYSE HIM source record " +
                                "at JSONL line $lineNumber."
                    }

                    val agbCode =
                        record
                            .agbCode
                            .trim()

                    require(
                        agbCode.isNotEmpty()
                    ) {
                        "Blank agbCode at JSONL line $lineNumber."
                    }

                    agbCodeCounts[agbCode] =
                        agbCodeCounts.getOrDefault(
                            agbCode,
                            0
                        ) + 1

                    if (
                        firstRecord == null
                    ) {
                        firstRecord =
                            record
                    }

                    lastRecord =
                        record

                    recordCount++
                }
            }

        require(
            recordCount ==
                    expectedRecordCount
        ) {
            "Unexpected AGRIBALYSE HIM source record count: " +
                    "$recordCount != $expectedRecordCount"
        }

        val resolvedFirstRecord =
            requireNotNull(
                firstRecord
            ) {
                "AGRIBALYSE HIM source contains no records."
            }

        val resolvedLastRecord =
            requireNotNull(
                lastRecord
            ) {
                "AGRIBALYSE HIM source contains no records."
            }

        val duplicateAgbCodes =
            agbCodeCounts
                .filterValues {
                    it > 1
                }

        return AgribalyseHimSourceValidationResult(
            sourceFile =
                sourceFile,
            recordCount =
                recordCount,
            uniqueAgbCodeCount =
                agbCodeCounts.size,
            duplicateAgbCodeCount =
                duplicateAgbCodes.size,
            duplicateAgbRecordCount =
                duplicateAgbCodes
                    .values
                    .sumOf {
                        it - 1
                    },
            duplicateAgbCodes =
                duplicateAgbCodes,
            firstRecord =
                resolvedFirstRecord,
            lastRecord =
                resolvedLastRecord,
            compressedSizeBytes =
                sourceFile.length(),
            compressedSha256 =
                sha256(
                    sourceFile
                ),
            contentSha256 =
                uncompressedSha256(
                    sourceFile
                )
        )
    }

    private fun sha256(
        file: File
    ): String {

        val digest =
            MessageDigest
                .getInstance(
                    "SHA-256"
                )

        file
            .inputStream()
            .buffered()
            .use { input ->

                val buffer =
                    ByteArray(
                        DEFAULT_BUFFER_SIZE
                    )

                while (true) {

                    val read =
                        input.read(
                            buffer
                        )

                    if (
                        read < 0
                    ) {
                        break
                    }

                    digest.update(
                        buffer,
                        0,
                        read
                    )
                }
            }

        return digest
            .digest()
            .joinToString(
                separator = ""
            ) { byte ->

                "%02x".format(
                    byte
                )
            }
    }

    private fun uncompressedSha256(
        file: File
    ): String {

        val digest =
            MessageDigest
                .getInstance(
                    "SHA-256"
                )

        GZIPInputStream(
            file
                .inputStream()
                .buffered()
        ).use { input ->

            val buffer =
                ByteArray(
                    DEFAULT_BUFFER_SIZE
                )

            while (true) {

                val read =
                    input.read(
                        buffer
                    )

                if (
                    read < 0
                ) {
                    break
                }

                digest.update(
                    buffer,
                    0,
                    read
                )
            }
        }

        return digest
            .digest()
            .joinToString(
                separator = ""
            ) { byte ->

                "%02x".format(
                    byte
                )
            }
    }

    private companion object {

        const val EXPECTED_RECORD_COUNT =
            2458
    }
}

data class AgribalyseHimSourceValidationResult(

    val sourceFile: File,

    val recordCount: Int,

    val uniqueAgbCodeCount: Int,

    val duplicateAgbCodeCount: Int,

    val duplicateAgbRecordCount: Int,

    val duplicateAgbCodes: Map<String, Int>,

    val firstRecord: AgribalyseHimSourceRecord,

    val lastRecord: AgribalyseHimSourceRecord,

    val compressedSizeBytes: Long,

    val contentSha256: String,

    val compressedSha256: String,
)