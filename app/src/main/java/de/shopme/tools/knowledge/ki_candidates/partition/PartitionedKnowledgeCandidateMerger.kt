package de.shopme.tools.knowledge.ki_candidates.partition

import de.shopme.tools.knowledge.ki_candidates.CanonicalKnowledgeCandidate
import de.shopme.tools.knowledge.ki_candidates.KnowledgeCandidateMergeAccumulator

class PartitionedKnowledgeCandidateMerger(
    private val store:
    PartitionedKnowledgeCandidateStore
) {

    fun forEachMergedPartition(
        consumer:
            (
            partitionIndex: Int,
            candidates: List<CanonicalKnowledgeCandidate>,
            conflictCount: Int,
            blockedHighFanoutKeys: Map<String, Int>
        ) -> Unit
    ): PartitionedKnowledgeCandidateMergeResult {

        var mergedCandidateCount =
            0L

        var conflictCount =
            0L

        val blockedHighFanoutKeys =
            sortedMapOf<String, Int>()

        var maximumPartitionInputCount =
            0

        var maximumPartitionMergedCount =
            0

        val partitionResults =
            mutableListOf<
                    KnowledgeCandidatePartitionMergeResult
                    >()

        store.existingPartitions()
            .sorted()
            .forEach { partitionIndex ->

                val accumulator =
                    KnowledgeCandidateMergeAccumulator()

                var partitionInputCount =
                    0

                store.forEachCandidate(
                    partitionIndex =
                        partitionIndex
                ) { candidate ->

                    accumulator.add(
                        listOf(candidate)
                    )

                    partitionInputCount++
                }

                val mergedCandidates =
                    accumulator
                        .candidates()
                        .sortedBy { candidate ->
                            candidate.canonicalId
                        }
                        .toList()

                val partitionConflictCount =
                    accumulator.conflictCount()

                val partitionBlockedKeys =
                    accumulator
                        .blockedHighFanoutKeys()
                        .toSortedMap()

                maximumPartitionInputCount =
                    maxOf(
                        maximumPartitionInputCount,
                        partitionInputCount
                    )

                maximumPartitionMergedCount =
                    maxOf(
                        maximumPartitionMergedCount,
                        mergedCandidates.size
                    )

                mergedCandidateCount +=
                    mergedCandidates.size.toLong()

                conflictCount +=
                    partitionConflictCount.toLong()

                partitionBlockedKeys.forEach {
                        (key, count) ->

                    blockedHighFanoutKeys[key] =
                        blockedHighFanoutKeys
                            .getOrDefault(
                                key,
                                0
                            ) +
                                count
                }

                partitionResults +=
                    KnowledgeCandidatePartitionMergeResult(
                        partitionIndex =
                            partitionIndex,
                        inputCandidateCount =
                            partitionInputCount,
                        mergedCandidateCount =
                            mergedCandidates.size,
                        conflictCount =
                            partitionConflictCount,
                        blockedHighFanoutKeyCount =
                            partitionBlockedKeys.size
                    )

                consumer(
                    partitionIndex,
                    mergedCandidates,
                    partitionConflictCount,
                    partitionBlockedKeys
                )
            }

        return PartitionedKnowledgeCandidateMergeResult(
            inputCandidateCount =
                store.candidateCount(),
            mergedCandidateCount =
                mergedCandidateCount,
            conflictCount =
                conflictCount,
            blockedHighFanoutKeys =
                blockedHighFanoutKeys.toSortedMap(),
            processedPartitionCount =
                partitionResults.size,
            maximumPartitionInputCount =
                maximumPartitionInputCount,
            maximumPartitionMergedCount =
                maximumPartitionMergedCount,
            partitions =
                partitionResults
                    .sortedBy { result ->
                        result.partitionIndex
                    }
        )
    }
}

data class KnowledgeCandidatePartitionMergeResult(
    val partitionIndex: Int,
    val inputCandidateCount: Int,
    val mergedCandidateCount: Int,
    val conflictCount: Int,
    val blockedHighFanoutKeyCount: Int
) {

    init {
        require(partitionIndex >= 0) {
            "partitionIndex must not be negative."
        }

        require(inputCandidateCount >= 0) {
            "inputCandidateCount must not be negative."
        }

        require(mergedCandidateCount >= 0) {
            "mergedCandidateCount must not be negative."
        }

        require(conflictCount >= 0) {
            "conflictCount must not be negative."
        }

        require(blockedHighFanoutKeyCount >= 0) {
            "blockedHighFanoutKeyCount must not be negative."
        }

        require(
            mergedCandidateCount <=
                    inputCandidateCount
        ) {
            "mergedCandidateCount must not exceed inputCandidateCount."
        }
    }
}

data class PartitionedKnowledgeCandidateMergeResult(
    val inputCandidateCount: Long,
    val mergedCandidateCount: Long,
    val conflictCount: Long,
    val blockedHighFanoutKeys: Map<String, Int>,
    val processedPartitionCount: Int,
    val maximumPartitionInputCount: Int,
    val maximumPartitionMergedCount: Int,
    val partitions:
    List<KnowledgeCandidatePartitionMergeResult>
) {

    init {
        require(inputCandidateCount >= 0L) {
            "inputCandidateCount must not be negative."
        }

        require(mergedCandidateCount >= 0L) {
            "mergedCandidateCount must not be negative."
        }

        require(conflictCount >= 0L) {
            "conflictCount must not be negative."
        }

        require(processedPartitionCount >= 0) {
            "processedPartitionCount must not be negative."
        }

        require(maximumPartitionInputCount >= 0) {
            "maximumPartitionInputCount must not be negative."
        }

        require(maximumPartitionMergedCount >= 0) {
            "maximumPartitionMergedCount must not be negative."
        }

        require(
            mergedCandidateCount <=
                    inputCandidateCount
        ) {
            "mergedCandidateCount must not exceed inputCandidateCount."
        }

        require(
            processedPartitionCount ==
                    partitions.size
        ) {
            "processedPartitionCount must equal partitions.size."
        }

        require(
            inputCandidateCount ==
                    partitions.sumOf { partition ->
                        partition
                            .inputCandidateCount
                            .toLong()
                    }
        ) {
            "inputCandidateCount must equal the sum of partition inputs."
        }

        require(
            mergedCandidateCount ==
                    partitions.sumOf { partition ->
                        partition
                            .mergedCandidateCount
                            .toLong()
                    }
        ) {
            "mergedCandidateCount must equal the sum of partition outputs."
        }

        require(
            conflictCount ==
                    partitions.sumOf { partition ->
                        partition
                            .conflictCount
                            .toLong()
                    }
        ) {
            "conflictCount must equal the sum of partition conflicts."
        }

        require(
            blockedHighFanoutKeys.values.all { count ->
                count > 0
            }
        ) {
            "Blocked high-fanout counts must be greater than zero."
        }
    }
}