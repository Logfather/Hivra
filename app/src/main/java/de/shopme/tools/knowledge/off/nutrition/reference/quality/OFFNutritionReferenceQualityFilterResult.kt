package de.shopme.tools.knowledge.off.nutrition.reference.quality

import de.shopme.tools.knowledge.off.nutrition.reference.CanonicalOFFNutritionReferenceCandidate

data class OFFNutritionReferenceQualityFilterResult(
    val inputCandidateCount: Int,
    val acceptedCandidateCount: Int,
    val rejectedCandidateCount: Int,
    val countsByReason:
    Map<OFFNutritionReferenceQualityRejectionReason, Int>,
    val acceptedCandidates:
    List<CanonicalOFFNutritionReferenceCandidate>,
    val rejections:
    List<OFFNutritionReferenceQualityRejection>
) {

    init {
        require(inputCandidateCount >= 0) {
            "inputCandidateCount must not be negative."
        }

        require(acceptedCandidateCount >= 0) {
            "acceptedCandidateCount must not be negative."
        }

        require(rejectedCandidateCount >= 0) {
            "rejectedCandidateCount must not be negative."
        }

        require(
            inputCandidateCount ==
                    acceptedCandidateCount +
                    rejectedCandidateCount
        ) {
            "Quality filter counts do not cover all input candidates."
        }

        require(
            acceptedCandidateCount ==
                    acceptedCandidates.size
        ) {
            "acceptedCandidateCount must equal acceptedCandidates.size."
        }

        require(
            rejectedCandidateCount ==
                    rejections.size
        ) {
            "rejectedCandidateCount must equal rejections.size."
        }

        require(
            countsByReason.values.all { count ->
                count > 0
            }
        ) {
            "Quality rejection reason counts must be positive."
        }

        require(
            countsByReason.keys.toList() ==
                    countsByReason.keys.sortedBy { reason ->
                        reason.name
                    }
        ) {
            "Quality rejection reason counts must be sorted " +
                    "deterministically."
        }

        val expectedCountsByReason =
            rejections
                .flatMap { rejection ->
                    rejection.reasons
                }
                .groupingBy { reason ->
                    reason
                }
                .eachCount()
                .toSortedMap(
                    compareBy { reason ->
                        reason.name
                    }
                )

        require(countsByReason == expectedCountsByReason) {
            "Quality rejection reason counts do not match rejections: " +
                    "expected=$expectedCountsByReason, " +
                    "actual=$countsByReason."
        }

        require(
            acceptedCandidates ==
                    acceptedCandidates.sortedWith(
                        CANDIDATE_COMPARATOR
                    )
        ) {
            "Accepted OFF nutrition reference candidates must be " +
                    "sorted deterministically."
        }

        require(
            rejections ==
                    rejections.sortedWith(
                        REJECTION_COMPARATOR
                    )
        ) {
            "OFF nutrition reference quality rejections must be " +
                    "sorted deterministically."
        }

        val acceptedIdentities =
            acceptedCandidates.map { candidate ->
                CandidateIdentity(
                    sourceId =
                        candidate.sourceId,
                    canonicalId =
                        candidate.canonicalId
                )
            }

        val rejectedIdentities =
            rejections.map { rejection ->
                CandidateIdentity(
                    sourceId =
                        rejection.sourceId,
                    canonicalId =
                        rejection.canonicalId
                )
            }

        require(
            acceptedIdentities.distinct().size ==
                    acceptedIdentities.size
        ) {
            "Accepted OFF nutrition reference candidates must not " +
                    "contain duplicate sourceId/canonicalId identities."
        }

        require(
            rejectedIdentities.distinct().size ==
                    rejectedIdentities.size
        ) {
            "OFF nutrition reference quality rejections must not " +
                    "contain duplicate sourceId/canonicalId identities."
        }

        val identitiesPresentInBothResults =
            acceptedIdentities
                .toSet()
                .intersect(
                    rejectedIdentities.toSet()
                )

        require(identitiesPresentInBothResults.isEmpty()) {
            "OFF nutrition reference candidate identities must not " +
                    "be both accepted and rejected: " +
                    identitiesPresentInBothResults
                        .sortedWith(
                            CANDIDATE_IDENTITY_COMPARATOR
                        )
        }

        require(
            acceptedIdentities.toSet().size +
                    rejectedIdentities.toSet().size ==
                    inputCandidateCount
        ) {
            "Accepted and rejected candidate identities do not " +
                    "represent exactly all input candidates."
        }
    }

    private data class CandidateIdentity(
        val sourceId: String,
        val canonicalId: String
    )

    private companion object {

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

        val REJECTION_COMPARATOR:
                Comparator<OFFNutritionReferenceQualityRejection> =
            compareBy<OFFNutritionReferenceQualityRejection>(
                {
                    it.sourceId
                },
                {
                    it.canonicalId
                }
            )

        val CANDIDATE_IDENTITY_COMPARATOR:
                Comparator<CandidateIdentity> =
            compareBy<CandidateIdentity>(
                {
                    it.sourceId
                },
                {
                    it.canonicalId
                }
            )
    }
}