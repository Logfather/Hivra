package de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic

import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.OFFNutritionRetrievalTextNormalizer
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.coverage.OFFNutritionCoverageAliasEntry
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.coverage.OFFNutritionSourceCoverageFinding
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.coverage.OFFNutritionSourceCoverageStage

class MissingOFFNutritionReferenceCandidateDiagnoser {

    fun diagnose(
        requestCount: Int,
        sourceCoverageFindings:
        List<OFFNutritionSourceCoverageFinding>,
        traces:
        List<OFFNutritionReferenceCandidateTrace>,
        persistedCandidates:
        List<OFFNutritionCoverageAliasEntry>
    ): MissingOFFNutritionReferenceCandidateReport {

        val relevantCoverageFindings =
            sourceCoverageFindings
                .filter { finding ->
                    finding.firstMissingStage ==
                            OFFNutritionSourceCoverageStage.REFERENCE_CANDIDATE
                }
                .sortedWith(
                    compareBy<OFFNutritionSourceCoverageFinding>(
                        { it.catalogIndex },
                        { it.catalogKey }
                    )
                )

        val findings =
            relevantCoverageFindings.map { coverageFinding ->
                diagnoseFinding(
                    coverageFinding =
                        coverageFinding,
                    traces =
                        traces,
                    persistedCandidates =
                        persistedCandidates
                )
            }

        return MissingOFFNutritionReferenceCandidateReport(
            version =
                MissingOFFNutritionReferenceCandidateReport.CURRENT_VERSION,

            requestCount =
                requestCount,

            referenceCandidateGapCount =
                findings.size,

            traceCount =
                traces.size,

            countsByFirstMissingStage =
                enumCounts(
                    values =
                        findings.map { finding ->
                            finding.firstMissingStage
                        }
                ),

            countsByIdentityRejectionReason =
                reasonCounts(
                    findings.flatMap { finding ->
                        expandReasonCounts(
                            finding.countsByIdentityRejectionReason
                        )
                    }
                ),

            countsByNutritionRejectionReason =
                reasonCounts(
                    findings.flatMap { finding ->
                        expandReasonCounts(
                            finding.countsByNutritionRejectionReason
                        )
                    }
                ),

            countsByReferenceEligibilityRejectionReason =
                reasonCounts(
                    findings.flatMap { finding ->
                        expandReasonCounts(
                            finding
                                .countsByReferenceEligibilityRejectionReason
                        )
                    }
                ),

            findings =
                findings
        )
    }
    private fun diagnoseFinding(
        coverageFinding:
        OFFNutritionSourceCoverageFinding,
        traces:
        List<OFFNutritionReferenceCandidateTrace>,
        persistedCandidates:
        List<OFFNutritionCoverageAliasEntry>
    ): MissingOFFNutritionReferenceCandidateFinding {

        val retrievalTerms =
            coverageFinding
                .retrievalTerms
                .asSequence()
                .map(
                    OFFNutritionRetrievalTextNormalizer::normalize
                )
                .filter(String::isNotBlank)
                .toSortedSet()

        val matchingTraces =
            traces
                .filter { trace ->
                    trace.normalizedProductIdentities.any { identity ->
                        retrievalTerms.any { term ->
                            matches(
                                left = identity,
                                right = term
                            )
                        }
                    }
                }
                .sortedWith(
                    compareBy<OFFNutritionReferenceCandidateTrace>(
                        { it.sourceProductId },
                        { it.productName }
                    )
                )

        val duplicateSourceProductIds =
            matchingTraces
                .groupingBy(
                    OFFNutritionReferenceCandidateTrace::sourceProductId
                )
                .eachCount()
                .filterValues { it > 1 }
                .keys
                .sorted()

        require(
            duplicateSourceProductIds.isEmpty()
        ) {
            "Duplicate matching traces: " +
                    duplicateSourceProductIds.joinToString()
        }

        val identityAccepted =
            matchingTraces.filter { trace ->
                trace.identityAccepted
            }

        val nutritionAccepted =
            identityAccepted.filter { trace ->
                trace.nutritionAccepted
            }

        val referenceEligible =
            nutritionAccepted.filter { trace ->
                trace.referenceEligible
            }

        val candidateCreated =
            referenceEligible.filter { trace ->
                trace.candidateCreated
            }

        val createdCandidateIds =
            candidateCreated
                .mapNotNull { trace ->
                    trace.createdCandidateId
                }
                .distinct()
                .sorted()

        val createdCandidateIdSet =
            createdCandidateIds.toSet()

        val persistedCandidateIds =
            persistedCandidates
                .filter { candidate ->
                    createdCandidateIdSet.contains(
                        candidate.identity
                    )
                }
                .map { candidate ->
                    candidate.identity
                }
                .distinct()
                .sorted()

        val firstMissingStage =
            determineFirstMissingStage(
                matchingTraceCount =
                    matchingTraces.size,
                identityAcceptedCount =
                    identityAccepted.size,
                nutritionAcceptedCount =
                    nutritionAccepted.size,
                referenceEligibleCount =
                    referenceEligible.size,
                candidateCreatedCount =
                    candidateCreated.size,
                candidatePersistedCount =
                    persistedCandidateIds.size
            )

        if (
            firstMissingStage ==
            MissingOFFNutritionReferenceCandidateStage.NONE
        ) {
            require(
                persistedCandidateIds.isNotEmpty()
            ) {
                "Stage NONE requires at least one persisted candidate."
            }
        }

        return MissingOFFNutritionReferenceCandidateFinding(
            catalogIndex =
                coverageFinding.catalogIndex,

            catalogKey =
                coverageFinding.catalogKey,

            normalizedEnglish =
                coverageFinding.normalizedEnglish,

            retrievalTerms =
                retrievalTerms.toList(),

            matchingTraceCount =
                matchingTraces.size,

            identityAcceptedCount =
                identityAccepted.size,

            nutritionAcceptedCount =
                nutritionAccepted.size,

            referenceEligibleCount =
                referenceEligible.size,

            candidateCreatedCount =
                candidateCreated.size,

            candidatePersistedCount =
                persistedCandidateIds.size,

            firstMissingStage =
                firstMissingStage,

            countsByIdentityRejectionReason =
                reasonCounts(
                    matchingTraces.flatMap { trace ->
                        trace.identityRejectionReasons
                    }
                ),

            countsByNutritionRejectionReason =
                reasonCounts(
                    matchingTraces.flatMap { trace ->
                        trace.nutritionRejectionReasons
                    }
                ),

            countsByReferenceEligibilityRejectionReason =
                reasonCounts(
                    matchingTraces.flatMap { trace ->
                        trace.referenceEligibilityRejectionReasons
                    }
                ),

            matchedProductNames =
                matchingTraces
                    .map { trace ->
                        trace.productName
                    }
                    .distinct()
                    .sorted()
                    .take(MAXIMUM_SAMPLE_COUNT),

            createdCandidateIds =
                createdCandidateIds
                    .take(MAXIMUM_SAMPLE_COUNT),

            persistedCandidateIds =
                persistedCandidateIds
                    .take(MAXIMUM_SAMPLE_COUNT),

            reasons =
                listOf(
                    reasonFor(
                        stage = firstMissingStage
                    )
                )
        )
    }

    private fun determineFirstMissingStage(
        matchingTraceCount: Int,
        identityAcceptedCount: Int,
        nutritionAcceptedCount: Int,
        referenceEligibleCount: Int,
        candidateCreatedCount: Int,
        candidatePersistedCount: Int
    ): MissingOFFNutritionReferenceCandidateStage {

        return when {
            matchingTraceCount == 0 -> {
                MissingOFFNutritionReferenceCandidateStage.IDENTITY_REJECTED
            }

            identityAcceptedCount == 0 -> {
                MissingOFFNutritionReferenceCandidateStage.IDENTITY_REJECTED
            }

            nutritionAcceptedCount == 0 -> {
                MissingOFFNutritionReferenceCandidateStage.NUTRITION_REJECTED
            }

            referenceEligibleCount == 0 -> {
                MissingOFFNutritionReferenceCandidateStage
                    .REFERENCE_ELIGIBILITY_REJECTED
            }

            candidateCreatedCount == 0 -> {
                MissingOFFNutritionReferenceCandidateStage
                    .CANDIDATE_NOT_CREATED
            }

            candidatePersistedCount == 0 -> {
                MissingOFFNutritionReferenceCandidateStage
                    .CANDIDATE_NOT_PERSISTED
            }

            else -> {
                MissingOFFNutritionReferenceCandidateStage.NONE
            }
        }
    }

    private fun matches(
        left: String,
        right: String
    ): Boolean {

        if (left == right) {
            return true
        }

        return " $left ".contains(" $right ") ||
                " $right ".contains(" $left ")
    }

    private fun reasonCounts(
        reasons: List<String>
    ): Map<String, Int> {

        return reasons
            .filter(String::isNotBlank)
            .groupingBy { reason ->
                reason
            }
            .eachCount()
            .toSortedMap()
    }

    private fun <T : Enum<T>> enumCounts(
        values: List<T>
    ): Map<T, Int> {

        return values
            .groupingBy { value ->
                value
            }
            .eachCount()
            .toSortedMap(
                compareBy { value ->
                    value.name
                }
            )
    }

    private fun expandReasonCounts(
        counts: Map<String, Int>
    ): List<String> {

        return counts.flatMap { (reason, count) ->
            List(count) {
                reason
            }
        }
    }

    private fun reasonFor(
        stage:
        MissingOFFNutritionReferenceCandidateStage
    ): String {

        return when (stage) {
            MissingOFFNutritionReferenceCandidateStage.IDENTITY_REJECTED ->
                "No matching OFF product passed the productive reference identity validation."

            MissingOFFNutritionReferenceCandidateStage.NUTRITION_REJECTED ->
                "Matching product identities exist, but none passed the productive nutrition validation."

            MissingOFFNutritionReferenceCandidateStage
                .REFERENCE_ELIGIBILITY_REJECTED ->
                "Matching products passed identity and nutrition validation, but none passed reference eligibility."

            MissingOFFNutritionReferenceCandidateStage
                .CANDIDATE_NOT_CREATED ->
                "At least one product was reference-eligible, but no candidate was created."

            MissingOFFNutritionReferenceCandidateStage
                .CANDIDATE_NOT_PERSISTED ->
                "At least one candidate was created, but no matching candidate was persisted."

            MissingOFFNutritionReferenceCandidateStage.NONE ->
                "A matching reference candidate was created and persisted."
        }
    }

    companion object {

        private const val MAXIMUM_SAMPLE_COUNT =
            20
    }
}