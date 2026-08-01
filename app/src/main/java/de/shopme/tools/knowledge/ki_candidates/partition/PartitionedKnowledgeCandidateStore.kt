package de.shopme.tools.knowledge.ki_candidates.partition

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.ki_candidates.CanonicalKnowledgeCandidate
import java.io.BufferedWriter
import java.io.Closeable
import java.io.File
import java.nio.charset.StandardCharsets

class PartitionedKnowledgeCandidateStore(
    private val directory: File,
    private val partitioner:
    KnowledgeCandidatePartitioner =
        KnowledgeCandidatePartitioner(),
    private val gson: Gson =
        GsonBuilder()
            .disableHtmlEscaping()
            .create()
) : Closeable {

    private val writers =
        arrayOfNulls<BufferedWriter>(
            partitioner.partitionCount
        )

    private var writtenCandidateCount =
        0L

    init {
        prepareDirectory()
    }

    fun append(
        candidate: CanonicalKnowledgeCandidate
    ) {
        val partitionIndex =
            partitioner.partitionIndex(
                candidate
            )

        val writer =
            writerFor(
                partitionIndex
            )

        writer.write(
            gson.toJson(candidate)
        )
        writer.newLine()

        writtenCandidateCount++
    }

    fun appendAll(
        candidates:
        Iterable<CanonicalKnowledgeCandidate>
    ) {
        candidates.forEach(::append)
    }

    fun candidateCount(): Long =
        writtenCandidateCount

    fun partitionCount(): Int =
        partitioner.partitionCount

    fun partitionFile(
        partitionIndex: Int
    ): File {
        require(
            partitionIndex in
                    0 until partitioner.partitionCount
        ) {
            "Invalid partition index: $partitionIndex."
        }

        return directory.resolve(
            partitionFileName(
                partitionIndex
            )
        )
    }

    fun existingPartitions(): List<Int> {
        closeWriters()

        return (0 until partitioner.partitionCount)
            .filter { partitionIndex ->
                val file =
                    partitionFile(
                        partitionIndex
                    )

                file.isFile &&
                        file.length() > 0L
            }
    }

    fun forEachCandidate(
        partitionIndex: Int,
        consumer:
            (CanonicalKnowledgeCandidate) -> Unit
    ) {
        closeWriters()

        val file =
            partitionFile(
                partitionIndex
            )

        if (!file.isFile || file.length() == 0L) {
            return
        }

        file.bufferedReader(
            StandardCharsets.UTF_8
        ).useLines { lines ->
            lines.forEachIndexed {
                    lineIndex,
                    line ->

                require(line.isNotBlank()) {
                    "Blank candidate line in partition " +
                            "$partitionIndex at line " +
                            "${lineIndex + 1}."
                }

                val candidate =
                    gson.fromJson(
                        line,
                        CanonicalKnowledgeCandidate::class.java
                    )

                requireNotNull(candidate) {
                    "Could not parse candidate in partition " +
                            "$partitionIndex at line " +
                            "${lineIndex + 1}."
                }

                val actualPartition =
                    partitioner.partitionIndex(
                        candidate
                    )

                require(
                    actualPartition ==
                            partitionIndex
                ) {
                    "Candidate ${candidate.canonicalId} " +
                            "was persisted in partition " +
                            "$partitionIndex but belongs to " +
                            "$actualPartition."
                }

                consumer(candidate)
            }
        }
    }

    override fun close() {
        closeWriters()
    }

    fun delete() {
        close()

        require(
            directory.deleteRecursively() ||
                    !directory.exists()
        ) {
            "Could not delete candidate partition directory: " +
                    directory.absolutePath
        }
    }

    private fun prepareDirectory() {
        if (directory.exists()) {
            require(
                directory.deleteRecursively()
            ) {
                "Could not clear candidate partition directory: " +
                        directory.absolutePath
            }
        }

        require(
            directory.mkdirs() ||
                    directory.isDirectory
        ) {
            "Could not create candidate partition directory: " +
                    directory.absolutePath
        }
    }

    private fun writerFor(
        partitionIndex: Int
    ): BufferedWriter {
        val existing =
            writers[partitionIndex]

        if (existing != null) {
            return existing
        }

        val writer =
            partitionFile(
                partitionIndex
            )
                .outputStream()
                .buffered()
                .writer(
                    StandardCharsets.UTF_8
                )
                .buffered()

        writers[partitionIndex] =
            writer

        return writer
    }

    private fun closeWriters() {
        writers.forEachIndexed {
                index,
                writer ->

            writer?.close()
            writers[index] = null
        }
    }

    private fun partitionFileName(
        partitionIndex: Int
    ): String =
        "candidates-part-" +
                partitionIndex
                    .toString()
                    .padStart(
                        length = 4,
                        padChar = '0'
                    ) +
                ".jsonl"
}