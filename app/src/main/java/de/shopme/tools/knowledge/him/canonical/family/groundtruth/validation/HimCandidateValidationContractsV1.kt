package de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import java.security.MessageDigest

object HimCandidateValidationContractV1 {
    const val VERSION = "HIM_CANDIDATE_VALIDATION_V1"
    const val DECISION_REFERENCE_CONTRACT =
        "HIM_CANDIDATE_VALIDATION_DECISION_REFERENCE_V1"
    const val LEDGER_VERSION =
        "HIM_CANDIDATE_VALIDATION_LEDGER_V1"
}

enum class HimCandidateValidationDecision {
    APPROVE,
    REJECT,
    DEFER,
}

enum class HimCandidateValidationReason {
    SEMANTICALLY_CORRECT,
    SEMANTICALLY_INCORRECT,
    SUPERSEDED_BY_BETTER_CANDIDATE,
    INSUFFICIENT_EVIDENCE,
    AUTHORITY_CONFLICT,
    REQUIRES_FURTHER_REVIEW,
}

data class HimCandidateValidationDecisionReference(
    val value: String,
) {
    init {
        require(
            value.matches(
                Regex("validation:v1:[0-9a-f]{64}")
            )
        )
    }
}

data class HimCandidateValidationRecord(
    val contractVersion: String =
        HimCandidateValidationContractV1.VERSION,
    val validationReference:
    HimCandidateValidationDecisionReference,
    val candidateReference:
    HimCandidateReference,
    val candidateDatasetDigest:
    HimSha256,
    val decision:
    HimCandidateValidationDecision,
    val reason:
    HimCandidateValidationReason,
    val supersededByCandidateReference:
    HimCandidateReference? = null,
    val rationale:
    String,
) {
    init {
        require(
            contractVersion ==
                    HimCandidateValidationContractV1.VERSION
        )

        require(rationale.isNotBlank())

        when (decision) {
            HimCandidateValidationDecision.APPROVE -> {
                require(
                    reason ==
                            HimCandidateValidationReason.SEMANTICALLY_CORRECT
                )
                require(
                    supersededByCandidateReference == null
                )
            }

            HimCandidateValidationDecision.REJECT -> {
                require(
                    reason in setOf(
                        HimCandidateValidationReason.SEMANTICALLY_INCORRECT,
                        HimCandidateValidationReason.SUPERSEDED_BY_BETTER_CANDIDATE,
                        HimCandidateValidationReason.AUTHORITY_CONFLICT,
                    )
                )

                if (
                    reason ==
                    HimCandidateValidationReason.SUPERSEDED_BY_BETTER_CANDIDATE
                ) {
                    requireNotNull(
                        supersededByCandidateReference
                    )
                    require(
                        supersededByCandidateReference !=
                                candidateReference
                    )
                } else {
                    require(
                        supersededByCandidateReference == null
                    )
                }
            }

            HimCandidateValidationDecision.DEFER -> {
                require(
                    reason in setOf(
                        HimCandidateValidationReason.INSUFFICIENT_EVIDENCE,
                        HimCandidateValidationReason.REQUIRES_FURTHER_REVIEW,
                    )
                )
                require(
                    supersededByCandidateReference == null
                )
            }
        }
    }
}

data class HimCandidateValidationLedgerV1(
    val contractVersion: String =
        HimCandidateValidationContractV1.LEDGER_VERSION,
    val validations:
    List<HimCandidateValidationRecord> =
        emptyList(),
) {
    init {
        require(
            contractVersion ==
                    HimCandidateValidationContractV1.LEDGER_VERSION
        )

        require(
            validations
                .map { it.validationReference }
                .distinct()
                .size ==
                    validations.size
        )

        require(
            validations
                .map { it.candidateReference }
                .distinct()
                .size ==
                    validations.size
        )

        require(
            validations.map {
                it.validationReference.value
            } ==
                    validations.map {
                        it.validationReference.value
                    }.sorted()
        )
    }
}

object HimCandidateValidationIdentityV1 {

    fun validation(
        candidateReference: HimCandidateReference,
        candidateDatasetDigest: HimSha256,
        decision: HimCandidateValidationDecision,
        reason: HimCandidateValidationReason,
        supersededByCandidateReference:
        HimCandidateReference?,
    ): HimCandidateValidationDecisionReference {
        val canonical = buildString {
            appendLine(
                "contract=" +
                        HimCandidateValidationContractV1
                            .DECISION_REFERENCE_CONTRACT
            )
            appendLine(
                "candidate=${candidateReference.value}"
            )
            appendLine(
                "candidate-dataset-digest=" +
                        candidateDatasetDigest.value
            )
            appendLine(
                "decision=${decision.name}"
            )
            appendLine(
                "reason=${reason.name}"
            )
            appendLine(
                "superseded-by=" +
                        (
                                supersededByCandidateReference
                                    ?.value
                                    .orEmpty()
                                )
            )
        }

        return HimCandidateValidationDecisionReference(
            "validation:v1:${sha256(canonical)}"
        )
    }

    private fun sha256(
        value: String,
    ): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(
                value.toByteArray(
                    Charsets.UTF_8
                )
            )
            .joinToString("") {
                "%02x".format(
                    it.toInt() and 0xff
                )
            }
}