package de.shopme.tools.knowledge.off.nutrition.reference.freeze

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

class OFFNutritionSourceSnapshotValidator(
    private val snapshotReader:
    OFFNutritionSourceSnapshotReader =
        OFFNutritionSourceSnapshotReader()
) {

    fun validate(
        snapshotFile: File,
        frozenAggregateFile: File,
        expectedSourceAggregateFile: File? =
            null
    ): OFFNutritionSourceSnapshotValidationResult {

        val canonicalSnapshotFile =
            snapshotFile.canonicalFile

        val canonicalFrozenAggregateFile =
            frozenAggregateFile.canonicalFile

        require(canonicalSnapshotFile.isFile) {
            "OFF Nutrition source snapshot does not exist: " +
                    canonicalSnapshotFile.absolutePath
        }

        require(canonicalFrozenAggregateFile.isFile) {
            "Frozen OFF Nutrition aggregate does not exist: " +
                    canonicalFrozenAggregateFile.absolutePath
        }

        val snapshot =
            snapshotReader.read(
                file =
                    canonicalSnapshotFile
            )

        val issues =
            mutableListOf<String>()

        val actualFrozenSize =
            canonicalFrozenAggregateFile.length()

        if (
            actualFrozenSize !=
            snapshot.frozenFileSizeBytes
        ) {
            issues +=
                "Frozen aggregate size differs from snapshot. " +
                        "expected=${snapshot.frozenFileSizeBytes}, " +
                        "actual=$actualFrozenSize"
        }

        val actualFrozenSha256 =
            sha256(
                file =
                    canonicalFrozenAggregateFile
            )

        if (
            actualFrozenSha256 !=
            snapshot.frozenSha256
        ) {
            issues +=
                "Frozen aggregate SHA-256 differs from snapshot. " +
                        "expected=${snapshot.frozenSha256}, " +
                        "actual=$actualFrozenSha256"
        }

        if (expectedSourceAggregateFile != null) {
            val canonicalSourceFile =
                expectedSourceAggregateFile.canonicalFile

            if (!canonicalSourceFile.isFile) {
                issues +=
                    "Expected OFF Nutrition source aggregate does not " +
                            "exist: ${canonicalSourceFile.absolutePath}"
            } else {
                val sourceSize =
                    canonicalSourceFile.length()

                if (
                    sourceSize !=
                    snapshot.sourceFileSizeBytes
                ) {
                    issues +=
                        "Current source aggregate size differs from frozen " +
                                "snapshot. expected=" +
                                "${snapshot.sourceFileSizeBytes}, " +
                                "actual=$sourceSize"
                }

                val sourceSha256 =
                    sha256(
                        file =
                            canonicalSourceFile
                    )

                if (
                    sourceSha256 !=
                    snapshot.sourceSha256
                ) {
                    issues +=
                        "Current source aggregate SHA-256 differs from " +
                                "frozen snapshot. expected=" +
                                "${snapshot.sourceSha256}, " +
                                "actual=$sourceSha256"
                }
            }
        }

        return OFFNutritionSourceSnapshotValidationResult(
            valid =
                issues.isEmpty(),
            snapshot =
                snapshot,
            snapshotFile =
                canonicalSnapshotFile,
            frozenAggregateFile =
                canonicalFrozenAggregateFile,
            actualFrozenFileSizeBytes =
                actualFrozenSize,
            actualFrozenSha256 =
                actualFrozenSha256,
            issues =
                issues.sorted()
        )
    }

    private fun sha256(
        file: File
    ): String {

        val digest =
            MessageDigest.getInstance(
                "SHA-256"
            )

        FileInputStream(
            file
        ).use { inputStream ->
            val buffer =
                ByteArray(
                    1024 * 1024
                )

            while (true) {
                val read =
                    inputStream.read(
                        buffer
                    )

                if (read < 0) {
                    break
                }

                if (read > 0) {
                    digest.update(
                        buffer,
                        0,
                        read
                    )
                }
            }
        }

        return digest
            .digest()
            .joinToString(
                separator =
                    ""
            ) { byte ->
                "%02x".format(
                    byte.toInt() and
                            0xff
                )
            }
    }
}

data class OFFNutritionSourceSnapshotValidationResult(
    val valid: Boolean,
    val snapshot: OFFNutritionSourceSnapshot,
    val snapshotFile: File,
    val frozenAggregateFile: File,
    val actualFrozenFileSizeBytes: Long,
    val actualFrozenSha256: String,
    val issues: List<String>
) {

    init {
        require(actualFrozenFileSizeBytes > 0L)

        require(
            issues ==
                    issues.distinct().sorted()
        ) {
            "OFF Nutrition source snapshot validation issues must be " +
                    "unique and sorted."
        }

        require(
            valid ==
                    issues.isEmpty()
        ) {
            "OFF Nutrition source snapshot validity must equal absence " +
                    "of issues."
        }
    }
}