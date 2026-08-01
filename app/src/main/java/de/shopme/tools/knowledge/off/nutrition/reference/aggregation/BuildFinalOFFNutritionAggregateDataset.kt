package de.shopme.tools.knowledge.off.nutrition.reference.aggregation

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonWriter
import de.shopme.tools.knowledge.off.nutrition.reference.CanonicalOFFNutritionReferenceCandidate
import de.shopme.tools.knowledge.off.nutrition.reference.deduplication.OFFNutritionReferenceCandidateDeduplicator
import de.shopme.tools.knowledge.off.nutrition.reference.persistence.OFFAcceptedNutritionReferenceReader
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.PriorityQueue
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class BuildFinalOFFNutritionAggregateDataset(
    private val acceptedReferenceReader:
    OFFAcceptedNutritionReferenceReader =
        OFFAcceptedNutritionReferenceReader(),
    private val deduplicator:
    OFFNutritionReferenceCandidateDeduplicator =
        OFFNutritionReferenceCandidateDeduplicator(),
    private val aggregator:
    OFFNutritionReferenceAggregator =
        OFFNutritionReferenceAggregator(),
    private val gson: Gson =
        GsonBuilder()
            .disableHtmlEscaping()
            .create(),
    private val prettyGson: Gson =
        GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create()
) {

    fun run(
        acceptedReferenceFile: File,
        aggregateDatasetOutputFile: File,
        aggregationReportOutputFile: File,
        bucketCount: Int = DEFAULT_BUCKET_COUNT
    ): OFFNutritionAggregateDatasetBuildResult {

        require(acceptedReferenceFile.isFile) {
            "Accepted OFF nutrition reference file does not exist: " +
                    acceptedReferenceFile.absolutePath
        }

        require(bucketCount > 0) {
            "bucketCount must be greater than zero."
        }

        require(bucketCount <= MAXIMUM_BUCKET_COUNT) {
            "bucketCount must not exceed $MAXIMUM_BUCKET_COUNT."
        }

        require(
            aggregateDatasetOutputFile.canonicalFile !=
                    acceptedReferenceFile.canonicalFile
        ) {
            "Aggregate output must not overwrite accepted references."
        }

        val workingDirectory =
            createWorkingDirectory(
                outputFile =
                    aggregateDatasetOutputFile
            )

        return try {
            val candidateBucketFiles =
                createCandidateBucketFiles(
                    workingDirectory =
                        workingDirectory.resolve(
                            "candidate-buckets"
                        ),
                    bucketCount =
                        bucketCount
                )
            val inputReferenceCount =
                partitionAcceptedReferences(
                    acceptedReferenceFile =
                        acceptedReferenceFile,
                    bucketFiles =
                        candidateBucketFiles
                )

            val bucketProcessingResult =
                processCandidateBuckets(
                    candidateBucketFiles =
                        candidateBucketFiles,
                    aggregateBucketDirectory =
                        workingDirectory.resolve(
                            "aggregate-buckets"
                        )
                )

            require(
                inputReferenceCount ==
                        bucketProcessingResult.inputReferenceCount
            ) {
                "Partitioned and processed reference counts differ: " +
                        "partitioned=$inputReferenceCount, " +
                        "processed=" +
                        bucketProcessingResult.inputReferenceCount
            }

            val mergeResult =
                mergeAggregateBuckets(
                    aggregateBucketFiles =
                        bucketProcessingResult.aggregateBucketFiles,
                    aggregateDatasetOutputFile =
                        aggregateDatasetOutputFile
                )

            require(
                mergeResult.aggregateCount ==
                        bucketProcessingResult.aggregateCount
            ) {
                "Merged aggregate count differs from bucket count: " +
                        "merged=${mergeResult.aggregateCount}, " +
                        "bucketed=${bucketProcessingResult.aggregateCount}."
            }

            require(
                mergeResult.profileCount ==
                        bucketProcessingResult
                            .deduplicatedReferenceCount
            ) {
                "Merged aggregate profiles do not cover all " +
                        "deduplicated references: " +
                        "profiles=${mergeResult.profileCount}, " +
                        "deduplicated=" +
                        bucketProcessingResult
                            .deduplicatedReferenceCount
            }

            val report =
                OFFNutritionReferenceAggregationReport(
                    version =
                        REPORT_VERSION,
                    inputCandidateCount =
                        requireDeduplicatedReferenceCountAsInt(
                            bucketProcessingResult
                                .deduplicatedReferenceCount
                        ),
                    aggregateCount =
                        mergeResult.aggregateCount,
                    singleProfileAggregateCount =
                        mergeResult.singleProfileAggregateCount,
                    multiProfileAggregateCount =
                        mergeResult.multiProfileAggregateCount,
                    maximumProfileCount =
                        mergeResult.maximumProfileCount,
                    profileCountDistribution =
                        mergeResult.profileCountDistribution,
                    largestAggregates =
                        mergeResult.largestAggregates
                )

            writeReport(
                report =
                    report,
                outputFile =
                    aggregationReportOutputFile
            )

            OFFNutritionAggregateDatasetBuildResult(
                inputReferenceCount =
                    inputReferenceCount,
                deduplicatedReferenceCount =
                    bucketProcessingResult
                        .deduplicatedReferenceCount,
                removedDuplicateCount =
                    bucketProcessingResult
                        .removedDuplicateCount,
                duplicateGroupCount =
                    bucketProcessingResult
                        .duplicateGroupCount,
                aggregateCount =
                    mergeResult.aggregateCount,
                singleProfileAggregateCount =
                    mergeResult.singleProfileAggregateCount,
                multiProfileAggregateCount =
                    mergeResult.multiProfileAggregateCount,
                maximumProfileCount =
                    mergeResult.maximumProfileCount,
                aggregateDatasetFile =
                    aggregateDatasetOutputFile,
                aggregationReportFile =
                    aggregationReportOutputFile,
                aggregateDatasetFileSizeBytes =
                    aggregateDatasetOutputFile.length()
            )
        } finally {
            deleteWorkingDirectory(
                workingDirectory =
                    workingDirectory
            )
        }
    }

    private fun processCandidateBuckets(
        candidateBucketFiles: List<File>,
        aggregateBucketDirectory: File
    ): BucketProcessingResult {

        require(
            aggregateBucketDirectory.isDirectory ||
                    aggregateBucketDirectory.mkdirs()
        ) {
            "Could not create aggregate bucket directory: " +
                    aggregateBucketDirectory.absolutePath
        }

        var inputReferenceCount =
            0L

        var deduplicatedReferenceCount =
            0L

        var removedDuplicateCount =
            0L

        var duplicateGroupCount =
            0L

        var aggregateCount =
            0

        val aggregateBucketFiles =
            mutableListOf<File>()

        candidateBucketFiles
            .sortedBy(File::getName)
            .forEachIndexed { bucketIndex, candidateBucketFile ->

                val candidates =
                    readCandidateBucket(
                        bucketFile =
                            candidateBucketFile
                    )

                if (candidates.isEmpty()) {
                    return@forEachIndexed
                }

                val deduplicationResult =
                    deduplicator.deduplicate(
                        candidates =
                            candidates
                    )

                val aggregationResult =
                    aggregator.aggregate(
                        candidates =
                            deduplicationResult.candidates
                    )

                require(
                    aggregationResult.inputCandidateCount ==
                            deduplicationResult.outputCandidateCount
                ) {
                    "Aggregation did not consume all deduplicated " +
                            "candidates in ${candidateBucketFile.name}."
                }

                val aggregateBucketFile =
                    aggregateBucketDirectory.resolve(
                        "bucket-" +
                                bucketIndex
                                    .toString()
                                    .padStart(
                                        length =
                                            BUCKET_INDEX_WIDTH,
                                        padChar =
                                            '0'
                                    ) +
                                ".aggregate.jsonl"
                    )

                writeAggregateBucket(
                    aggregates =
                        aggregationResult.aggregates,
                    outputFile =
                        aggregateBucketFile
                )

                inputReferenceCount +=
                    deduplicationResult
                        .inputCandidateCount
                        .toLong()

                deduplicatedReferenceCount +=
                    deduplicationResult
                        .outputCandidateCount
                        .toLong()

                removedDuplicateCount +=
                    deduplicationResult
                        .removedDuplicateCount
                        .toLong()

                duplicateGroupCount +=
                    deduplicationResult
                        .duplicateGroupCount
                        .toLong()

                aggregateCount +=
                    aggregationResult.aggregateCount

                aggregateBucketFiles +=
                    aggregateBucketFile
            }

        require(
            inputReferenceCount ==
                    deduplicatedReferenceCount +
                    removedDuplicateCount
        ) {
            "Bucket processing counts do not cover all references."
        }

        return BucketProcessingResult(
            inputReferenceCount =
                inputReferenceCount,
            deduplicatedReferenceCount =
                deduplicatedReferenceCount,
            removedDuplicateCount =
                removedDuplicateCount,
            duplicateGroupCount =
                duplicateGroupCount,
            aggregateCount =
                aggregateCount,
            aggregateBucketFiles =
                aggregateBucketFiles.sortedBy(File::getName)
        )
    }

    private fun writeAggregateBucket(
        aggregates: List<CanonicalOFFNutritionReferenceAggregate>,
        outputFile: File
    ) {
        val sortedAggregates =
            aggregates.sortedBy { aggregate ->
                aggregate.canonicalId
            }

        outputFile
            .bufferedWriter(
                StandardCharsets.UTF_8
            )
            .use { writer ->

                sortedAggregates.forEach { aggregate ->
                    gson.toJson(
                        aggregate,
                        writer
                    )

                    writer.newLine()
                }
            }

        require(outputFile.isFile) {
            "Aggregate bucket was not written: " +
                    outputFile.absolutePath
        }
    }

    private fun mergeAggregateBuckets(
        aggregateBucketFiles: List<File>,
        aggregateDatasetOutputFile: File
    ): AggregateMergeResult {

        val parentDirectory =
            requireNotNull(
                aggregateDatasetOutputFile.parentFile
            ) {
                "Aggregate output file must have a parent directory."
            }

        require(
            parentDirectory.isDirectory ||
                    parentDirectory.mkdirs()
        ) {
            "Could not create aggregate output directory: " +
                    parentDirectory.absolutePath
        }

        val temporaryOutputFile =
            parentDirectory.resolve(
                "${aggregateDatasetOutputFile.name}.tmp"
            )

        if (temporaryOutputFile.exists()) {
            require(temporaryOutputFile.delete()) {
                "Could not remove temporary aggregate dataset: " +
                        temporaryOutputFile.absolutePath
            }
        }

        val cursors =
            aggregateBucketFiles
                .mapIndexedNotNull { index, bucketFile ->
                    createCursor(
                        index =
                            index,
                        bucketFile =
                            bucketFile
                    )
                }

        val queue =
            PriorityQueue(
                compareBy<AggregateCursor> {
                    it.current.canonicalId
                }.thenBy {
                    it.index
                }
            )

        queue.addAll(cursors)

        var aggregateCount =
            0

        var singleProfileAggregateCount =
            0

        var multiProfileAggregateCount =
            0

        var maximumProfileCount =
            0

        var profileCount =
            0L

        val profileCountDistribution =
            sortedMapOf<Int, Int>()

        val largestAggregates =
            PriorityQueue(
                compareBy<OFFNutritionReferenceAggregationReportEntry> {
                    it.profileCount
                }.thenByDescending {
                    it.canonicalId
                }
            )

        var previousCanonicalId: String? =
            null

        try {
            JsonWriter(
                OutputStreamWriter(
                    BufferedOutputStream(
                        temporaryOutputFile.outputStream()
                    ),
                    StandardCharsets.UTF_8
                )
            ).use { jsonWriter ->

                jsonWriter.setIndent(
                    JSON_INDENT
                )

                jsonWriter.beginArray()

                while (queue.isNotEmpty()) {
                    val cursor =
                        queue.remove()

                    val aggregate =
                        cursor.current

                    require(
                        previousCanonicalId
                            ?.let { previous ->
                                previous < aggregate.canonicalId
                            }
                            ?: true
                    ) {
                        "Merged aggregate IDs are not unique and strictly sorted: " +
                                "previous=$previousCanonicalId, " +
                                "current=${aggregate.canonicalId}"
                    }

                    gson.toJson(
                        aggregate,
                        CanonicalOFFNutritionReferenceAggregate::class.java,
                        jsonWriter
                    )

                    previousCanonicalId =
                        aggregate.canonicalId

                    aggregateCount++

                    profileCount +=
                        aggregate.profileCount.toLong()

                    if (aggregate.profileCount == 1) {
                        singleProfileAggregateCount++
                    } else {
                        multiProfileAggregateCount++
                    }

                    maximumProfileCount =
                        maxOf(
                            maximumProfileCount,
                            aggregate.profileCount
                        )

                    profileCountDistribution[
                        aggregate.profileCount
                    ] =
                        profileCountDistribution
                            .getOrDefault(
                                aggregate.profileCount,
                                0
                            ) + 1

                    addLargestAggregate(
                        queue =
                            largestAggregates,
                        aggregate =
                            aggregate
                    )

                    if (cursor.advance()) {
                        queue +=
                            cursor
                    } else {
                        cursor.close()
                    }
                }

                jsonWriter.endArray()
            }

            replaceTargetFile(
                temporaryFile =
                    temporaryOutputFile,
                outputFile =
                    aggregateDatasetOutputFile
            )
        } finally {
            cursors.forEach { cursor ->
                cursor.closeQuietly()
            }

            if (temporaryOutputFile.exists()) {
                temporaryOutputFile.delete()
            }
        }

        require(aggregateDatasetOutputFile.isFile) {
            "Final aggregate dataset was not written: " +
                    aggregateDatasetOutputFile.absolutePath
        }

        require(aggregateDatasetOutputFile.length() > 0L) {
            "Final aggregate dataset is empty."
        }

        return AggregateMergeResult(
            aggregateCount =
                aggregateCount,
            singleProfileAggregateCount =
                singleProfileAggregateCount,
            multiProfileAggregateCount =
                multiProfileAggregateCount,
            maximumProfileCount =
                maximumProfileCount,
            profileCount =
                profileCount,
            profileCountDistribution =
                profileCountDistribution,
            largestAggregates =
                largestAggregates
                    .toList()
                    .sortedWith(
                        compareByDescending<
                                OFFNutritionReferenceAggregationReportEntry
                                > {
                            it.profileCount
                        }.thenBy {
                            it.canonicalId
                        }
                    )
        )
    }

    private fun createCursor(
        index: Int,
        bucketFile: File
    ): AggregateCursor? {

        val reader =
            bucketFile.bufferedReader(
                StandardCharsets.UTF_8
            )

        val firstLine =
            reader.readLine()

        if (firstLine == null) {
            reader.close()
            return null
        }

        require(firstLine.isNotBlank()) {
            "Blank first record in aggregate bucket: " +
                    bucketFile.absolutePath
        }

        return AggregateCursor(
            index =
                index,
            bucketFile =
                bucketFile,
            reader =
                reader,
            current =
                parseAggregate(
                    line =
                        firstLine,
                    bucketFile =
                        bucketFile
                ),
            gson =
                gson
        )
    }

    private fun addLargestAggregate(
        queue:
        PriorityQueue<
                OFFNutritionReferenceAggregationReportEntry
                >,
        aggregate: CanonicalOFFNutritionReferenceAggregate
    ) {
        val entry =
            OFFNutritionReferenceAggregationReportEntry(
                canonicalId =
                    aggregate.canonicalId,
                profileCount =
                    aggregate.profileCount,
                representativeSourceId =
                    aggregate.representativeSourceId,
                nutrientCount =
                    aggregate.nutrition.size
            )

        queue +=
            entry

        if (queue.size > LARGEST_AGGREGATE_LIMIT) {
            queue.remove()
        }
    }

    private fun parseAggregate(
        line: String,
        bucketFile: File
    ): CanonicalOFFNutritionReferenceAggregate =
        requireNotNull(
            gson.fromJson(
                line,
                CanonicalOFFNutritionReferenceAggregate::class.java
            )
        ) {
            "Empty aggregate in bucket: " +
                    bucketFile.absolutePath
        }

    private fun writeReport(
        report: OFFNutritionReferenceAggregationReport,
        outputFile: File
    ) {
        val parentDirectory =
            requireNotNull(outputFile.parentFile) {
                "Aggregation report must have a parent directory."
            }

        require(
            parentDirectory.isDirectory ||
                    parentDirectory.mkdirs()
        ) {
            "Could not create aggregation report directory: " +
                    parentDirectory.absolutePath
        }

        val temporaryFile =
            parentDirectory.resolve(
                "${outputFile.name}.tmp"
            )

        temporaryFile.writeText(
            prettyGson.toJson(
                report
            ) + "\n",
            StandardCharsets.UTF_8
        )

        replaceTargetFile(
            temporaryFile =
                temporaryFile,
            outputFile =
                outputFile
        )
    }

    private fun partitionAcceptedReferences(
        acceptedReferenceFile: File,
        bucketFiles: List<File>
    ): Long {

        val bucketWriters =
            bucketFiles.map { bucketFile ->
                createBucketWriter(
                    bucketFile =
                        bucketFile
                )
            }

        return try {
            acceptedReferenceReader.forEachReference(
                inputFile =
                    acceptedReferenceFile
            ) { reference ->

                validateReference(
                    reference =
                        reference
                )

                val bucketIndex =
                    bucketIndex(
                        canonicalId =
                            reference.canonicalId,
                        bucketCount =
                            bucketFiles.size
                    )

                gson.toJson(
                    reference,
                    bucketWriters[bucketIndex]
                )

                bucketWriters[bucketIndex]
                    .newLine()
            }
        } finally {
            bucketWriters.forEach { writer ->
                writer.close()
            }
        }
    }

    private fun readCandidateBucket(
        bucketFile: File
    ): List<CanonicalOFFNutritionReferenceCandidate> {

        val candidates =
            mutableListOf<CanonicalOFFNutritionReferenceCandidate>()

        GZIPInputStream(
            BufferedInputStream(
                bucketFile.inputStream()
            )
        ).bufferedReader(
            StandardCharsets.UTF_8
        ).useLines { lines ->

            lines.forEachIndexed { index, line ->
                require(line.isNotBlank()) {
                    "Blank candidate record in ${bucketFile.name} " +
                            "at line ${index + 1}."
                }

                val candidate =
                    requireNotNull(
                        gson.fromJson(
                            line,
                            CanonicalOFFNutritionReferenceCandidate::class.java
                        )
                    ) {
                        "Empty candidate record in ${bucketFile.name} " +
                                "at line ${index + 1}."
                    }

                validateReference(
                    reference =
                        candidate
                )

                candidates +=
                    candidate
            }
        }

        return candidates
    }

    private fun createCandidateBucketFiles(
        workingDirectory: File,
        bucketCount: Int
    ): List<File> {

        require(
            workingDirectory.isDirectory ||
                    workingDirectory.mkdirs()
        ) {
            "Could not create bucket directory: " +
                    workingDirectory.absolutePath
        }

        return (0 until bucketCount).map { bucketIndex ->
            workingDirectory.resolve(
                "bucket-" +
                        bucketIndex
                            .toString()
                            .padStart(
                                length = BUCKET_INDEX_WIDTH,
                                padChar = '0'
                            ) +
                        CANDIDATE_BUCKET_SUFFIX
            )
        }
    }

    private fun createBucketWriter(
        bucketFile: File
    ): BufferedWriter =
        BufferedWriter(
            OutputStreamWriter(
                GZIPOutputStream(
                    BufferedOutputStream(
                        bucketFile.outputStream()
                    )
                ),
                StandardCharsets.UTF_8
            )
        )

    private fun createWorkingDirectory(
        outputFile: File
    ): File {

        val parentDirectory =
            requireNotNull(outputFile.parentFile) {
                "Aggregate output file must have a parent directory."
            }

        require(
            parentDirectory.isDirectory ||
                    parentDirectory.mkdirs()
        ) {
            "Could not create aggregate output directory."
        }

        val workingDirectory =
            parentDirectory.resolve(
                ".${outputFile.name}.working"
            )

        if (workingDirectory.exists()) {
            require(
                workingDirectory.deleteRecursively()
            ) {
                "Could not remove existing working directory: " +
                        workingDirectory.absolutePath
            }
        }

        require(workingDirectory.mkdirs()) {
            "Could not create working directory: " +
                    workingDirectory.absolutePath
        }

        return workingDirectory
    }

    private fun bucketIndex(
        canonicalId: String,
        bucketCount: Int
    ): Int {

        val normalizedCanonicalId =
            canonicalId.trim()

        require(normalizedCanonicalId.isNotBlank()) {
            "Cannot bucket blank canonicalId."
        }

        val digest =
            MessageDigest
                .getInstance(SHA_256)
                .digest(
                    normalizedCanonicalId.toByteArray(
                        StandardCharsets.UTF_8
                    )
                )

        val value =
            ((digest[0].toInt() and BYTE_MASK) shl 24) or
                    ((digest[1].toInt() and BYTE_MASK) shl 16) or
                    ((digest[2].toInt() and BYTE_MASK) shl 8) or
                    (digest[3].toInt() and BYTE_MASK)

        return (value and Int.MAX_VALUE) %
                bucketCount
    }

    private fun validateReference(
        reference: CanonicalOFFNutritionReferenceCandidate
    ) {
        require(reference.sourceId.isNotBlank())
        require(reference.canonicalId.isNotBlank())
        require(reference.nutrition.isNotEmpty())

        require(
            reference.nutrition.values.all(Double::isFinite)
        )
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
        }
        catch (
            _: AtomicMoveNotSupportedException
        ) {
            Files.move(
                temporaryFile.toPath(),
                outputFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    private fun deleteWorkingDirectory(
        workingDirectory: File
    ) {
        if (!workingDirectory.exists()) {
            return
        }

        check(
            workingDirectory.deleteRecursively()
        ) {
            "Could not remove working directory: " +
                    workingDirectory.absolutePath
        }
    }

    private fun requireDeduplicatedReferenceCountAsInt(
        value: Long
    ): Int {

        require(value <= Int.MAX_VALUE.toLong()) {
            "deduplicatedReferenceCount exceeds Int range: $value"
        }

        return value.toInt()
    }

    private data class BucketProcessingResult(
        val inputReferenceCount: Long,
        val deduplicatedReferenceCount: Long,
        val removedDuplicateCount: Long,
        val duplicateGroupCount: Long,
        val aggregateCount: Int,
        val aggregateBucketFiles: List<File>
    )

    private data class AggregateMergeResult(
        val aggregateCount: Int,
        val singleProfileAggregateCount: Int,
        val multiProfileAggregateCount: Int,
        val maximumProfileCount: Int,
        val profileCount: Long,
        val profileCountDistribution: Map<Int, Int>,
        val largestAggregates:
        List<OFFNutritionReferenceAggregationReportEntry>
    )

    private class AggregateCursor(
        val index: Int,
        private val bucketFile: File,
        private val reader: BufferedReader,
        var current: CanonicalOFFNutritionReferenceAggregate,
        private val gson: Gson
    ) {

        fun advance(): Boolean {
            val line =
                reader.readLine()
                    ?: return false

            require(line.isNotBlank()) {
                "Blank aggregate record in bucket: " +
                        bucketFile.absolutePath
            }

            current =
                requireNotNull(
                    gson.fromJson(
                        line,
                        CanonicalOFFNutritionReferenceAggregate::class.java
                    )
                ) {
                    "Empty aggregate record in bucket: " +
                            bucketFile.absolutePath
                }

            return true
        }

        fun close() {
            reader.close()
        }

        fun closeQuietly() {
            runCatching {
                reader.close()
            }
        }
    }

    companion object {

        private const val CANDIDATE_BUCKET_SUFFIX =
            ".candidate.jsonl.gz"

        const val DEFAULT_BUCKET_COUNT =
            256

        private const val MAXIMUM_BUCKET_COUNT =
            512

        private const val BUCKET_INDEX_WIDTH =
            3

        private const val SHA_256 =
            "SHA-256"

        private const val BYTE_MASK =
            0xff

        private const val REPORT_VERSION =
            1

        private const val LARGEST_AGGREGATE_LIMIT =
            100

        private const val JSON_INDENT =
            "  "
    }
}