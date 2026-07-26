package de.shopme.tools.knowledge.off.nutrition.reference

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Persistiert kanonische OFF-Nutrition-Referenzkandidaten als deterministisches
 * JSON-Dataset.
 *
 * Die Datei enthält direkt eine JSON-Liste aus
 * [CanonicalOFFNutritionReferenceCandidate].
 *
 * Der Writer:
 *
 * 1. sortiert Kandidaten deterministisch,
 * 2. validiert eindeutige OFF-Source-IDs,
 * 3. schreibt zunächst in eine temporäre Datei,
 * 4. ersetzt die Zieldatei nach Möglichkeit atomar,
 * 5. liefert ein validiertes Write-Result zurück.
 */
class OFFNutritionReferenceDatasetWriter {

    fun write(
        candidates: List<CanonicalOFFNutritionReferenceCandidate>,
        outputFile: File
    ): OFFNutritionReferenceDatasetWriteResult {

        require(outputFile.name.isNotBlank()) {
            "OFF nutrition reference dataset output filename must not be blank."
        }

        val normalizedCandidates =
            normalizeAndValidate(
                candidates =
                    candidates
            )

        val parentDirectory =
            requireNotNull(outputFile.parentFile) {
                "OFF nutrition reference dataset output file must have " +
                        "a parent directory: ${outputFile.path}"
            }

        ensureDirectoryExists(
            directory =
                parentDirectory
        )

        val temporaryFile =
            File(
                parentDirectory,
                "${outputFile.name}.tmp"
            )

        try {
            writeTemporaryFile(
                candidates =
                    normalizedCandidates,
                temporaryFile =
                    temporaryFile
            )

            replaceTargetFile(
                temporaryFile =
                    temporaryFile,
                outputFile =
                    outputFile
            )
        } finally {
            deleteTemporaryFileIfPresent(
                temporaryFile =
                    temporaryFile
            )
        }

        check(outputFile.isFile) {
            "OFF nutrition reference dataset was not written: " +
                    outputFile.absolutePath
        }

        check(outputFile.length() > 0L) {
            "Persisted OFF nutrition reference dataset is empty: " +
                    outputFile.absolutePath
        }

        return OFFNutritionReferenceDatasetWriteResult(
            outputFile =
                outputFile,
            candidateCount =
                normalizedCandidates.size,
            fileSizeBytes =
                outputFile.length()
        )
    }

    private fun normalizeAndValidate(
        candidates: List<CanonicalOFFNutritionReferenceCandidate>
    ): List<CanonicalOFFNutritionReferenceCandidate> {

        val sortedCandidates =
            candidates.sortedWith(
                CANDIDATE_COMPARATOR
            )

        validateCandidates(
            candidates =
                sortedCandidates
        )

        return sortedCandidates
    }

    private fun validateCandidates(
        candidates: List<CanonicalOFFNutritionReferenceCandidate>
    ) {
        candidates.forEachIndexed { index, candidate ->

            require(candidate.sourceId.isNotBlank()) {
                "OFF nutrition reference candidate at index $index " +
                        "has a blank sourceId."
            }

            require(candidate.canonicalId.isNotBlank()) {
                "OFF nutrition reference candidate at index $index " +
                        "has a blank canonicalId."
            }

            require(candidate.nutrition.isNotEmpty()) {
                "OFF nutrition reference candidate at index $index " +
                        "has an empty nutrition payload: " +
                        "sourceId=${candidate.sourceId}."
            }

            require(
                candidate.nutrition.values.all(Double::isFinite)
            ) {
                "OFF nutrition reference candidate at index $index " +
                        "contains a non-finite nutrition value: " +
                        "sourceId=${candidate.sourceId}."
            }

            require(candidate.source.isNotBlank()) {
                "OFF nutrition reference candidate at index $index " +
                        "has a blank source: " +
                        "sourceId=${candidate.sourceId}."
            }

            require(candidate.sourceVersion.isNotBlank()) {
                "OFF nutrition reference candidate at index $index " +
                        "has a blank sourceVersion: " +
                        "sourceId=${candidate.sourceId}."
            }

            require(candidate.sourceConfidence.isFinite()) {
                "OFF nutrition reference candidate at index $index " +
                        "has a non-finite sourceConfidence: " +
                        "sourceId=${candidate.sourceId}."
            }

            require(
                candidate.sourceConfidence in MIN_CONFIDENCE..MAX_CONFIDENCE
            ) {
                "OFF nutrition reference candidate at index $index " +
                        "has an invalid sourceConfidence: " +
                        "sourceId=${candidate.sourceId}, " +
                        "confidence=${candidate.sourceConfidence}."
            }
        }

        val duplicateSourceIds =
            candidates
                .groupingBy {
                    it.sourceId
                }
                .eachCount()
                .filterValues { occurrenceCount ->
                    occurrenceCount > 1
                }
                .keys
                .sorted()

        require(duplicateSourceIds.isEmpty()) {
            "OFF nutrition reference dataset contains duplicate sourceIds: " +
                    duplicateSourceIds
                        .take(MAX_REPORTED_DUPLICATE_SOURCE_IDS)
                        .joinToString()
        }
    }

    private fun ensureDirectoryExists(
        directory: File
    ) {
        check(
            directory.isDirectory ||
                    directory.mkdirs()
        ) {
            "Could not create OFF nutrition reference dataset directory: " +
                    directory.absolutePath
        }

        check(directory.isDirectory) {
            "OFF nutrition reference dataset parent path is not a directory: " +
                    directory.absolutePath
        }
    }

    private fun writeTemporaryFile(
        candidates: List<CanonicalOFFNutritionReferenceCandidate>,
        temporaryFile: File
    ) {
        if (temporaryFile.exists()) {
            check(temporaryFile.delete()) {
                "Could not remove existing temporary OFF nutrition " +
                        "reference dataset: ${temporaryFile.absolutePath}"
            }
        }

        temporaryFile
            .outputStream()
            .buffered()
            .writer(StandardCharsets.UTF_8)
            .use { writer ->
                GSON.toJson(
                    candidates,
                    writer
                )

                writer.write(
                    "\n"
                )
            }

        check(temporaryFile.isFile) {
            "Temporary OFF nutrition reference dataset was not written: " +
                    temporaryFile.absolutePath
        }

        check(temporaryFile.length() > 0L) {
            "Temporary OFF nutrition reference dataset is empty: " +
                    temporaryFile.absolutePath
        }
    }

    private fun replaceTargetFile(
        temporaryFile: File,
        outputFile: File
    ) {
        try {
            Files.move(
                temporaryFile.toPath(),
                outputFile.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (
            exception: AtomicMoveNotSupportedException
        ) {
            Files.move(
                temporaryFile.toPath(),
                outputFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    private fun deleteTemporaryFileIfPresent(
        temporaryFile: File
    ) {
        if (!temporaryFile.exists()) {
            return
        }

        check(temporaryFile.delete()) {
            "Could not remove temporary OFF nutrition reference dataset: " +
                    temporaryFile.absolutePath
        }
    }

    companion object {

        val CANDIDATE_COMPARATOR:
                Comparator<CanonicalOFFNutritionReferenceCandidate> =
            compareBy<CanonicalOFFNutritionReferenceCandidate>(
                {
                    it.sourceId
                },
                {
                    it.canonicalId
                }
            )

        private const val MIN_CONFIDENCE =
            0.0

        private const val MAX_CONFIDENCE =
            1.0

        private const val MAX_REPORTED_DUPLICATE_SOURCE_IDS =
            20

        private val GSON: Gson =
            GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create()
    }
}