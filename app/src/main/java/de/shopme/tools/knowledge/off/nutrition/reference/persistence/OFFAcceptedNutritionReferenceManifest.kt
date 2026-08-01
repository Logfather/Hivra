package de.shopme.tools.knowledge.off.nutrition.reference.persistence

data class OFFAcceptedNutritionReferenceManifest(
    val version: Int,
    val format: String,
    val compression: String,
    val inputFile: String,
    val inputFileSizeBytes: Long,
    val policyFile: String,
    val policyVersion: Int,
    val outputFile: String,
    val processedCandidateCount: Long,
    val acceptedReferenceCount: Long,
    val rejectedCandidateCount: Long,
    val contentSha256: String,
    val uncompressedContentBytes: Long
) {

    init {
        require(version > 0)
        require(format.isNotBlank())
        require(compression.isNotBlank())
        require(inputFile.isNotBlank())
        require(inputFileSizeBytes > 0L)
        require(policyFile.isNotBlank())
        require(policyVersion > 0)
        require(outputFile.isNotBlank())

        require(processedCandidateCount >= 0L)
        require(acceptedReferenceCount >= 0L)
        require(rejectedCandidateCount >= 0L)
        require(uncompressedContentBytes >= 0L)

        require(
            processedCandidateCount ==
                    acceptedReferenceCount + rejectedCandidateCount
        ) {
            "Processed candidate count must equal accepted plus rejected."
        }

        require(
            contentSha256.matches(
                Regex("[0-9a-f]{64}")
            )
        ) {
            "contentSha256 must be a lowercase SHA-256 hash."
        }
    }

    companion object {

        const val CURRENT_VERSION =
            1

        const val JSONL_FORMAT =
            "CANONICAL_KNOWLEDGE_CANDIDATE_JSONL"

        const val GZIP_COMPRESSION =
            "GZIP"
    }
}