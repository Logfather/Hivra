package de.shopme.tools.knowledge.off.nutrition.reference.quality.pipeline.streaming

data class StreamingOFFNutritionReferenceQualityReport(
    val version: Int,
    val inputFile: String,
    val inputFileSizeBytes: Long,
    val batchSize: Int,
    val maxCandidates: Int?,
    val extractedCandidateCount: Long,
    val generatedCandidateCount: Long,
    val skippedWithoutNutritionCount: Long,
    val skippedInvalidIdentityCount: Long,
    val skippedInvalidNutritionPayloadCount: Long,
    val acceptedCandidateCount: Long,
    val rejectedCandidateCount: Long,
    val rejectionReasonOccurrenceCount: Long,
    val acceptanceRate: Double,
    val countsByReason: Map<String, Long>,
    val durationMillis: Long
) {

    init {
        require(version > 0)
        require(inputFile.isNotBlank())
        require(inputFileSizeBytes >= 0L)
        require(batchSize > 0)
        require(maxCandidates == null || maxCandidates > 0)
        require(extractedCandidateCount >= 0L)
        require(generatedCandidateCount >= 0L)
        require(acceptedCandidateCount >= 0L)
        require(rejectedCandidateCount >= 0L)
        require(rejectionReasonOccurrenceCount >= 0L)
        require(acceptanceRate in 0.0..1.0)
        require(durationMillis >= 0L)

        require(
            generatedCandidateCount ==
                    acceptedCandidateCount + rejectedCandidateCount
        )

        require(
            rejectionReasonOccurrenceCount ==
                    countsByReason.values.sum()
        )
    }

    companion object {

        const val CURRENT_VERSION =
            1

        fun create(
            request:
            StreamingOFFNutritionReferenceQualityPipelineRequest,
            statistics:
            StreamingOFFNutritionReferenceQualityStatistics,
            durationMillis: Long
        ): StreamingOFFNutritionReferenceQualityReport {

            return StreamingOFFNutritionReferenceQualityReport(
                version =
                    CURRENT_VERSION,
                inputFile =
                    request.inputFile.canonicalPath,
                inputFileSizeBytes =
                    request.inputFile.length(),
                batchSize =
                    request.batchSize,
                maxCandidates =
                    request.maxCandidates,
                extractedCandidateCount =
                    statistics.extractedCandidateCount,
                generatedCandidateCount =
                    statistics.generatedCandidateCount,
                skippedWithoutNutritionCount =
                    statistics.skippedWithoutNutritionCount,
                skippedInvalidIdentityCount =
                    statistics.skippedInvalidIdentityCount,
                skippedInvalidNutritionPayloadCount =
                    statistics.skippedInvalidNutritionPayloadCount,
                acceptedCandidateCount =
                    statistics.acceptedCandidateCount,
                rejectedCandidateCount =
                    statistics.rejectedCandidateCount,
                rejectionReasonOccurrenceCount =
                    statistics.rejectionReasonOccurrenceCount,
                acceptanceRate =
                    statistics.acceptanceRate,
                countsByReason =
                    statistics.countsByReason
                        .entries
                        .associate { (reason, count) ->
                            reason.name to count
                        }
                        .toSortedMap(),
                durationMillis =
                    durationMillis
            )
        }
    }
}