package de.shopme.tools.knowledge.off.nutrition.reference.freeze

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

class FreezeOFFNutritionSource(
    private val snapshotWriter:
    OFFNutritionSourceSnapshotWriter =
        OFFNutritionSourceSnapshotWriter()
) {

    fun freeze(
        request: FreezeOFFNutritionSourceRequest
    ): FreezeOFFNutritionSourceResult {

        val sourceAggregateFile =
            request.sourceAggregateFile
                .canonicalFile

        val validationReportFile =
            request.resultingNutritionValidationReportFile
                .canonicalFile

        val conflictPolicyFile =
            request.nutritionConflictPolicyFile
                .canonicalFile

        val frozenAggregateFile =
            request.frozenAggregateFile
                .canonicalFile

        val snapshotFile =
            request.snapshotFile
                .canonicalFile

        requireInputFile(
            file =
                sourceAggregateFile,
            description =
                "OFF Nutrition aggregate dataset"
        )

        requireInputFile(
            file =
                validationReportFile,
            description =
                "Resulting Nutrition validation report"
        )

        requireInputFile(
            file =
                conflictPolicyFile,
            description =
                "Nutrition conflict policy"
        )

        val nutritionValidationApproved =
            readNutritionValidationApproval(
                validationReportFile =
                    validationReportFile
            )

        require(nutritionValidationApproved) {
            "Resulting Nutrition validation report is not approved: " +
                    validationReportFile.absolutePath
        }

        val conflictPolicyEvidence =
            readConflictPolicyEvidence(
                conflictPolicyFile =
                    conflictPolicyFile
            )

        require(
            conflictPolicyEvidence.policyApproved
        ) {
            "Nutrition conflict policy did not approve the current " +
                    "Nutrition dataset: " +
                    conflictPolicyFile.absolutePath
        }

        require(
            conflictPolicyEvidence.violationCount ==
                    0L
        ) {
            "Nutrition conflict policy contains violations: " +
                    conflictPolicyEvidence.violationCount
        }

        val aggregateEntryCount =
            countAggregateEntries(
                aggregateFile =
                    sourceAggregateFile
            )

        val sourceFileSizeBytes =
            sourceAggregateFile.length()

        val sourceSha256 =
            sha256(
                file =
                    sourceAggregateFile
            )

        val frozenAggregateChanged =
            freezeAggregateFile(
                sourceFile =
                    sourceAggregateFile,
                targetFile =
                    frozenAggregateFile,
                sourceSha256 =
                    sourceSha256
            )

        val frozenFileSizeBytes =
            frozenAggregateFile.length()

        val frozenSha256 =
            sha256(
                file =
                    frozenAggregateFile
            )

        require(
            sourceFileSizeBytes ==
                    frozenFileSizeBytes
        ) {
            "Frozen OFF Nutrition aggregate size differs from source. " +
                    "source=$sourceFileSizeBytes, " +
                    "frozen=$frozenFileSizeBytes"
        }

        require(
            sourceSha256 ==
                    frozenSha256
        ) {
            "Frozen OFF Nutrition aggregate hash differs from source. " +
                    "source=$sourceSha256, " +
                    "frozen=$frozenSha256"
        }

        val commonPathRoot =
            resolveCommonPathRoot(
                files =
                    listOf(
                        sourceAggregateFile,
                        frozenAggregateFile
                    )
            )

        val snapshot =
            OFFNutritionSourceSnapshot(
                version =
                    OFFNutritionSourceSnapshot.CURRENT_VERSION,
                status =
                    OFFNutritionSourceSnapshotStatus.APPROVED,
                source =
                    OFFNutritionSourceSnapshot.SOURCE,
                sourceVersion =
                    OFFNutritionSourceSnapshot.SOURCE_VERSION,
                sourceAggregateFile =
                    relativePath(
                        root =
                            commonPathRoot,
                        file =
                            sourceAggregateFile
                    ),
                frozenAggregateFile =
                    relativePath(
                        root =
                            commonPathRoot,
                        file =
                            frozenAggregateFile
                    ),
                sourceFileSizeBytes =
                    sourceFileSizeBytes,
                frozenFileSizeBytes =
                    frozenFileSizeBytes,
                sourceSha256 =
                    sourceSha256,
                frozenSha256 =
                    frozenSha256,
                aggregateEntryCount =
                    aggregateEntryCount,
                nutritionValidationApproved =
                    nutritionValidationApproved,
                nutritionConflictPolicyVersion =
                    conflictPolicyEvidence.policyVersion,
                nutritionConflictPolicyApproved =
                    conflictPolicyEvidence.policyApproved
            )

        val snapshotChanged =
            snapshotWriter.write(
                snapshot =
                    snapshot,
                outputFile =
                    snapshotFile
            )

        return FreezeOFFNutritionSourceResult(
            sourceAggregateFile =
                sourceAggregateFile,
            frozenAggregateFile =
                frozenAggregateFile,
            snapshotFile =
                snapshotFile,
            sourceFileSizeBytes =
                sourceFileSizeBytes,
            frozenFileSizeBytes =
                frozenFileSizeBytes,
            sourceSha256 =
                sourceSha256,
            frozenSha256 =
                frozenSha256,
            aggregateEntryCount =
                aggregateEntryCount,
            snapshot =
                snapshot,
            frozenAggregateChanged =
                frozenAggregateChanged,
            snapshotChanged =
                snapshotChanged
        )
    }

    private fun freezeAggregateFile(
        sourceFile: File,
        targetFile: File,
        sourceSha256: String
    ): Boolean {

        targetFile.parentFile?.let { parent ->
            require(
                parent.mkdirs() ||
                        parent.isDirectory
            ) {
                "Could not create frozen OFF Nutrition directory: " +
                        parent.absolutePath
            }
        }

        if (targetFile.isFile) {
            val targetSha256 =
                sha256(
                    file =
                        targetFile
                )

            if (
                targetFile.length() ==
                sourceFile.length() &&
                targetSha256 ==
                sourceSha256
            ) {
                return false
            }
        }

        val temporaryFile =
            targetFile.resolveSibling(
                targetFile.name +
                        ".tmp"
            )

        sourceFile
            .inputStream()
            .buffered()
            .use { inputStream ->

                temporaryFile
                    .outputStream()
                    .buffered()
                    .use { outputStream ->
                        inputStream.copyTo(
                            outputStream
                        )
                    }
            }

        require(
            temporaryFile.length() ==
                    sourceFile.length()
        ) {
            "Temporary frozen OFF Nutrition aggregate has an " +
                    "unexpected size."
        }

        val temporarySha256 =
            sha256(
                file =
                    temporaryFile
            )

        require(
            temporarySha256 ==
                    sourceSha256
        ) {
            "Temporary frozen OFF Nutrition aggregate hash differs " +
                    "from source."
        }

        moveAtomically(
            sourceFile =
                temporaryFile,
            targetFile =
                targetFile
        )

        return true
    }

    private fun countAggregateEntries(
        aggregateFile: File
    ): Long {

        var count =
            0L

        jsonReader(
            file =
                aggregateFile
        ).use { reader ->

            reader.beginArray()

            var previousCanonicalId: String? =
                null

            while (reader.hasNext()) {
                reader.beginObject()

                var canonicalId: String? =
                    null

                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        CANONICAL_ID_PROPERTY_NAME -> {
                            require(canonicalId == null) {
                                "OFF Nutrition aggregate entry contains " +
                                        "multiple canonicalId properties."
                            }

                            canonicalId =
                                reader.nextString()
                        }

                        else ->
                            reader.skipValue()
                    }
                }

                reader.endObject()

                val requiredCanonicalId =
                    requireNotNull(
                        canonicalId
                    ) {
                        "OFF Nutrition aggregate entry has no canonicalId."
                    }

                require(
                    requiredCanonicalId.isNotBlank()
                ) {
                    "OFF Nutrition aggregate contains blank canonicalId."
                }

                if (previousCanonicalId != null) {
                    require(
                        previousCanonicalId <
                                requiredCanonicalId
                    ) {
                        "OFF Nutrition aggregate canonical IDs must be " +
                                "globally unique and strictly ordered. " +
                                "previous=$previousCanonicalId, " +
                                "current=$requiredCanonicalId"
                    }
                }

                previousCanonicalId =
                    requiredCanonicalId

                count++
            }

            reader.endArray()
        }

        require(count > 0L) {
            "OFF Nutrition aggregate dataset must not be empty."
        }

        return count
    }

    private fun readNutritionValidationApproval(
        validationReportFile: File
    ): Boolean {

        val root =
            parseJsonObject(
                file =
                    validationReportFile,
                description =
                    "Resulting Nutrition validation report"
            )

        val validElement =
            root.get(
                VALID_PROPERTY_NAME
            )
                ?: error(
                    "Resulting Nutrition validation report has no " +
                            "'valid' property."
                )

        require(
            validElement.isJsonPrimitive &&
                    validElement.asJsonPrimitive.isBoolean
        ) {
            "Resulting Nutrition validation report 'valid' property " +
                    "must be boolean."
        }

        return validElement.asBoolean
    }

    private fun readConflictPolicyEvidence(
        conflictPolicyFile: File
    ): ConflictPolicyEvidence {

        val root =
            parseJsonObject(
                file =
                    conflictPolicyFile,
                description =
                    "Nutrition conflict policy"
            )

        val policy =
            root.getAsJsonObject(
                POLICY_PROPERTY_NAME
            )
                ?: error(
                    "Nutrition conflict policy has no policy object."
                )

        val decision =
            root.getAsJsonObject(
                DECISION_PROPERTY_NAME
            )
                ?: error(
                    "Nutrition conflict policy has no decision object."
                )

        val policyVersion =
            policy
                .get(
                    VERSION_PROPERTY_NAME
                )
                ?.takeIf { element ->
                    element.isJsonPrimitive &&
                            element.asJsonPrimitive.isNumber
                }
                ?.asInt
                ?: error(
                    "Nutrition conflict policy has no numeric version."
                )

        val policyStatus =
            policy
                .get(
                    STATUS_PROPERTY_NAME
                )
                ?.takeIf { element ->
                    element.isJsonPrimitive &&
                            element.asJsonPrimitive.isString
                }
                ?.asString
                ?: error(
                    "Nutrition conflict policy has no status."
                )

        require(
            policyStatus ==
                    APPROVED_STATUS
        ) {
            "Nutrition conflict policy status must be APPROVED, " +
                    "but was $policyStatus."
        }

        val decisionApproved =
            decision
                .get(
                    APPROVED_PROPERTY_NAME
                )
                ?.takeIf { element ->
                    element.isJsonPrimitive &&
                            element.asJsonPrimitive.isBoolean
                }
                ?.asBoolean
                ?: error(
                    "Nutrition conflict policy decision has no boolean " +
                            "approved property."
                )

        val violations =
            decision.getAsJsonArray(
                VIOLATIONS_PROPERTY_NAME
            )
                ?: error(
                    "Nutrition conflict policy decision has no " +
                            "violations array."
                )

        return ConflictPolicyEvidence(
            policyVersion =
                policyVersion,
            policyApproved =
                decisionApproved,
            violationCount =
                violations.size().toLong()
        )
    }

    private fun parseJsonObject(
        file: File,
        description: String
    ): JsonObject {

        val element =
            file
                .reader(
                    StandardCharsets.UTF_8
                )
                .use { reader ->
                    JsonParser.parseReader(
                        reader
                    )
                }

        require(element.isJsonObject) {
            "$description must be a JSON object: ${file.absolutePath}"
        }

        return element.asJsonObject
    }

    private fun sha256(
        file: File
    ): String {

        require(file.isFile) {
            "Cannot calculate SHA-256 for missing file: " +
                    file.absolutePath
        }

        val digest =
            MessageDigest.getInstance(
                SHA_256_ALGORITHM
            )

        FileInputStream(
            file
        ).use { inputStream ->
            updateDigest(
                digest =
                    digest,
                inputStream =
                    inputStream
            )
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

    private fun updateDigest(
        digest: MessageDigest,
        inputStream: InputStream
    ) {
        val buffer =
            ByteArray(
                HASH_BUFFER_SIZE
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

    private fun requireInputFile(
        file: File,
        description: String
    ) {
        require(file.isFile) {
            "$description does not exist: ${file.absolutePath}"
        }

        require(file.length() > 0L) {
            "$description is empty: ${file.absolutePath}"
        }
    }

    private fun moveAtomically(
        sourceFile: File,
        targetFile: File
    ) {
        runCatching {
            Files.move(
                sourceFile.toPath(),
                targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        }
            .getOrElse {
                Files.move(
                    sourceFile.toPath(),
                    targetFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
    }

    private fun resolveCommonPathRoot(
        files: List<File>
    ): File {

        require(files.isNotEmpty())

        var candidate =
            requireNotNull(
                files
                    .first()
                    .canonicalFile
                    .parentFile
            )

        while (
            files.any { file ->
                !file
                    .canonicalFile
                    .toPath()
                    .startsWith(
                        candidate.toPath()
                    )
            }
        ) {
            candidate =
                candidate.parentFile
                    ?: return File(".").canonicalFile
        }

        return candidate
    }

    private fun relativePath(
        root: File,
        file: File
    ): String =
        root
            .canonicalFile
            .toPath()
            .relativize(
                file
                    .canonicalFile
                    .toPath()
            )
            .toString()
            .replace(
                File.separatorChar,
                '/'
            )

    private fun jsonReader(
        file: File
    ): JsonReader =
        JsonReader(
            file
                .inputStream()
                .buffered()
                .reader(
                    StandardCharsets.UTF_8
                )
        )

    private data class ConflictPolicyEvidence(
        val policyVersion: Int,
        val policyApproved: Boolean,
        val violationCount: Long
    )

    private companion object {

        const val CANONICAL_ID_PROPERTY_NAME =
            "canonicalId"

        const val VALID_PROPERTY_NAME =
            "valid"

        const val POLICY_PROPERTY_NAME =
            "policy"

        const val DECISION_PROPERTY_NAME =
            "decision"

        const val VERSION_PROPERTY_NAME =
            "version"

        const val STATUS_PROPERTY_NAME =
            "status"

        const val APPROVED_PROPERTY_NAME =
            "approved"

        const val VIOLATIONS_PROPERTY_NAME =
            "violations"

        const val APPROVED_STATUS =
            "APPROVED"

        const val SHA_256_ALGORITHM =
            "SHA-256"

        const val HASH_BUFFER_SIZE =
            1024 * 1024
    }
}