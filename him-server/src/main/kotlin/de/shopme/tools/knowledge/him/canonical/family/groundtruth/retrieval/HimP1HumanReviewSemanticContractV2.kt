package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateType
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import java.security.MessageDigest

/**
 * Additive semantic transport for Batch-1 human review.
 *
 * V1 remains the aggregate, immutable authority. This contract carries the
 * explicitly recorded semantic distinction without asking downstream code to
 * parse human prose or to reinterpret an aggregate enum as a relation.
 */
object HimP1HumanReviewSemanticContractV2 {
    const val CONTRACT_ID = "HIM_P1_HUMAN_REVIEW_SEMANTIC_CONTRACT_V2"
    const val VERSION = "2"
    const val STATE = "SEMANTIC_TRANSPORT_ONLY"
    const val SECOND_PASS_VALIDATION_AUTHORITY = "SECOND_PASS_HUMAN_VALIDATION"
    const val REVIEW_UNIT_IDENTITY_CONTRACT = "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_V1"

    private const val DECISION_DIGEST_DOMAIN =
        "HIM_P1_HUMAN_REVIEW_SEMANTIC_DECISION_LOGICAL_V2"
    private const val PACKET_DIGEST_DOMAIN =
        "HIM_P1_HUMAN_REVIEW_SEMANTIC_BLIND_PACKET_LOGICAL_V2"

    val ALLOWED_RELATION_KINDS = SemanticRelationKindV2.entries

    fun targetKindFor(kind: SemanticRelationKindV2): HimCandidateType? = when (kind) {
        SemanticRelationKindV2.IDENTITY_OF -> HimCandidateType.IDENTITY
        SemanticRelationKindV2.VARIANT_OF,
        SemanticRelationKindV2.PROCESSING_FORM_OF,
        SemanticRelationKindV2.PREPARATION_STATE_OF,
        SemanticRelationKindV2.PRODUCT_FORM_OF,
        -> HimCandidateType.VARIANT
        SemanticRelationKindV2.ALIAS_OF -> HimCandidateType.ALIAS
        SemanticRelationKindV2.NEW_CANONICAL -> HimCandidateType.CREATE_NEW_CANONICAL
        SemanticRelationKindV2.COMPONENT_OR_DERIVED_PRODUCT_OF,
        SemanticRelationKindV2.UNRESOLVED_MIXED_RELATION,
        -> null
    }

    fun digest(value: String): HimSha256 = HimSha256(
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) },
    )

    fun decisionDigest(decision: SemanticDecisionV2): HimSha256 = digest(
        buildString {
            appendLine(DECISION_DIGEST_DOMAIN)
            appendLine("contractId=${decision.contractId}")
            appendLine("version=${decision.version}")
            appendLine("packetId=${decision.packetId}")
            appendLine("candidateId=${decision.candidateId}")
            appendLine("candidateCanonicalId=${decision.candidateCanonicalId}")
            appendLine("stableEntryId=${decision.stableEntryId}")
            appendLine("reviewUnitId=${decision.reviewUnitId}")
            appendLine("v1BatchId=${decision.v1BatchId}")
            appendLine("v1BatchLogicalDigest=${decision.v1BatchLogicalDigest.value}")
            appendLine("v1DecisionRecordIdentity=${decision.v1DecisionRecordIdentity}")
            appendLine("originalReviewerRef=${decision.originalReviewerRef}")
            appendLine("originalReviewRound=${decision.originalReviewRound}")
            appendLine("originalRevision=${decision.originalRevision}")
            appendLine("originalDecision=${decision.originalDecision.name}")
            appendRelation(this, "aggregate", decision.aggregateRelation)
            decision.recordRelations.forEachIndexed { index, relation ->
                appendLine("record[$index].evidenceRecordId=${relation.evidenceRecordId}")
                relation.evidenceReferenceIds.forEach { appendLine("record[$index].evidenceReferenceId=$it") }
                appendRelation(this, "record[$index].relation", relation.relation)
            }
            decision.evidenceReferenceIds.forEach { appendLine("evidenceReferenceId=$it") }
            appendLine("rationaleReviewerRef=${decision.rationaleEvidence.reviewerRef}")
            appendLine("rationaleSourceDecision=${decision.rationaleEvidence.sourceDecisionIdentity}")
            appendLine("rationale=${decision.rationaleEvidence.text}")
        },
    )

    private fun appendRelation(
        builder: StringBuilder,
        prefix: String,
        relation: SemanticRelationV2,
    ) {
        builder.appendLine("$prefix.kind=${relation.kind.name}")
        builder.appendLine("$prefix.resolution=${relation.resolution.name}")
        builder.appendLine("$prefix.targetKind=${relation.targetKind?.name.orEmpty()}")
        builder.appendLine("$prefix.targetReference=${relation.targetReference?.canonicalId?.value.orEmpty()}")
        if (relation.targetReference is HimFamilyEntityReference.Identity) {
            builder.appendLine("$prefix.targetIdentityReference=${relation.targetReference.identityId.value}")
        }
        builder.appendLine("$prefix.semanticLabel=${relation.semanticLabel.orEmpty()}")
        builder.appendLine("$prefix.variantLabel=${relation.variantLabel.orEmpty()}")
        builder.appendLine("$prefix.processingForm=${relation.processingForm.orEmpty()}")
        builder.appendLine("$prefix.preparationState=${relation.preparationState.orEmpty()}")
        builder.appendLine("$prefix.productForm=${relation.productForm.orEmpty()}")
    }

    fun projectV1(
        original: HimZeroCandidateRecoveryHumanReviewDecisionRecordV1,
        v1BatchId: String,
        v1BatchLogicalDigest: HimSha256,
        packetId: String,
        candidateId: String,
        aggregateRelation: SemanticRelationV2,
        recordRelations: List<RecordRelationV2>,
        rationale: HumanRationaleEvidenceV2,
    ): SemanticDecisionV2 = SemanticDecisionV2(
        contractId = CONTRACT_ID,
        version = VERSION,
        packetId = packetId,
        candidateId = candidateId,
        candidateCanonicalId = original.reviewUnit.canonicalEntityId,
        stableEntryId = original.reviewUnit.stableEntryId,
        reviewUnitId = original.reviewUnit.reviewUnitId,
        v1BatchId = v1BatchId,
        v1BatchLogicalDigest = v1BatchLogicalDigest,
        v1DecisionRecordIdentity = listOf(
            original.reviewUnit.reviewUnitId,
            original.reviewerRef,
            original.reviewRound,
            original.revision,
        ).joinToString("\u0000"),
        originalReviewerRef = original.reviewerRef,
        originalReviewRound = original.reviewRound,
        originalRevision = original.revision,
        originalDecision = original.decision,
        aggregateRelation = aggregateRelation,
        recordRelations = recordRelations,
        evidenceReferenceIds = original.evidenceReferences.map { it.evidenceReferenceId }.sorted(),
        rationaleEvidence = rationale,
    ).also { it.validate() }

    fun targetForTraining(relation: SemanticRelationV2): HimTrainingTargetV1? {
        val target = relation.targetReference ?: return null
        return when (relation.kind) {
            SemanticRelationKindV2.IDENTITY_OF -> HimTrainingTargetV1.Identity(target.canonicalId)
            SemanticRelationKindV2.VARIANT_OF,
            SemanticRelationKindV2.PROCESSING_FORM_OF,
            SemanticRelationKindV2.PREPARATION_STATE_OF,
            SemanticRelationKindV2.PRODUCT_FORM_OF,
            -> HimTrainingTargetV1.Variant(target)
            SemanticRelationKindV2.ALIAS_OF -> HimTrainingTargetV1.Alias(target)
            SemanticRelationKindV2.NEW_CANONICAL -> HimTrainingTargetV1.NewCanonical()
            SemanticRelationKindV2.COMPONENT_OR_DERIVED_PRODUCT_OF,
            SemanticRelationKindV2.UNRESOLVED_MIXED_RELATION,
            -> null
        }
    }

    fun blindPacketFromV1(
        original: HimZeroCandidateRecoveryHumanReviewDecisionRecordV1,
        packetId: String,
        candidateId: String,
        candidateLabel: String,
        decisionQuestion: String,
    ): BlindSecondPassPacketV2 {
        val sourceEvidence = original.evidenceReferences.map { evidence ->
            BlindSourceEvidenceV2(
                evidenceRecordId = evidence.evidenceReferenceId,
                kind = evidence.kind.name,
                artifactReference = evidence.artifactReference,
                recordReference = evidence.recordReference,
                artifactSha256 = evidence.artifactSha256,
                artifactLogicalDigest = evidence.artifactLogicalDigest,
                fieldReferences = evidence.fieldReferences,
            )
        }.sortedBy { it.evidenceRecordId }
        val logicalDigest = blindPacketDigest(
            contractId = CONTRACT_ID,
            version = VERSION,
            packetId = packetId,
            candidateId = candidateId,
            candidateLabel = candidateLabel,
            stableEntryId = original.reviewUnit.stableEntryId,
            canonicalEntityId = original.reviewUnit.canonicalEntityId,
            sourceEvidence = sourceEvidence,
            allowedRelationKinds = ALLOWED_RELATION_KINDS,
            decisionQuestion = decisionQuestion,
        )
        return BlindSecondPassPacketV2(
            contractId = CONTRACT_ID,
            version = VERSION,
            packetId = packetId,
            candidateId = candidateId,
            candidateLabel = candidateLabel,
            stableEntryId = original.reviewUnit.stableEntryId,
            canonicalEntityId = original.reviewUnit.canonicalEntityId,
            sourceEvidence = sourceEvidence,
            allowedRelationKinds = ALLOWED_RELATION_KINDS,
            decisionQuestion = decisionQuestion,
            logicalDigest = logicalDigest,
        )
    }

    fun blindPacketDigest(packet: BlindSecondPassPacketV2): HimSha256 = blindPacketDigest(
        contractId = packet.contractId,
        version = packet.version,
        packetId = packet.packetId,
        candidateId = packet.candidateId,
        candidateLabel = packet.candidateLabel,
        stableEntryId = packet.stableEntryId,
        canonicalEntityId = packet.canonicalEntityId,
        sourceEvidence = packet.sourceEvidence,
        allowedRelationKinds = packet.allowedRelationKinds,
        decisionQuestion = packet.decisionQuestion,
    )

    private fun blindPacketDigest(
        contractId: String,
        version: String,
        packetId: String,
        candidateId: String,
        candidateLabel: String,
        stableEntryId: String,
        canonicalEntityId: String,
        sourceEvidence: List<BlindSourceEvidenceV2>,
        allowedRelationKinds: List<SemanticRelationKindV2>,
        decisionQuestion: String,
    ): HimSha256 = digest(
        buildString {
            appendLine(PACKET_DIGEST_DOMAIN)
            appendLine("contractId=$contractId")
            appendLine("version=$version")
            appendLine("packetId=$packetId")
            appendLine("candidateId=$candidateId")
            appendLine("candidateLabel=$candidateLabel")
            appendLine("stableEntryId=$stableEntryId")
            appendLine("canonicalEntityId=$canonicalEntityId")
            sourceEvidence.forEach { evidence ->
                appendLine("evidenceRecordId=${evidence.evidenceRecordId}")
                appendLine("kind=${evidence.kind}")
                appendLine("artifactReference=${evidence.artifactReference}")
                appendLine("recordReference=${evidence.recordReference}")
                appendLine("artifactSha256=${evidence.artifactSha256}")
                appendLine("artifactLogicalDigest=${evidence.artifactLogicalDigest.orEmpty()}")
                evidence.fieldReferences.forEach { appendLine("fieldReference=$it") }
            }
            allowedRelationKinds.forEach { appendLine("allowedRelation=${it.name}") }
            appendLine("decisionQuestion=$decisionQuestion")
        },
    )
}

enum class SemanticRelationKindV2 {
    IDENTITY_OF,
    VARIANT_OF,
    PROCESSING_FORM_OF,
    PREPARATION_STATE_OF,
    PRODUCT_FORM_OF,
    COMPONENT_OR_DERIVED_PRODUCT_OF,
    ALIAS_OF,
    NEW_CANONICAL,
    UNRESOLVED_MIXED_RELATION,
}

enum class SemanticResolutionV2 {
    RESOLVED,
    AMBIGUOUS,
    UNRESOLVED,
}

data class SemanticRelationV2(
    val kind: SemanticRelationKindV2,
    val resolution: SemanticResolutionV2,
    val targetKind: HimCandidateType? = HimP1HumanReviewSemanticContractV2.targetKindFor(kind),
    val targetReference: HimFamilyEntityReference? = null,
    val semanticLabel: String? = null,
    val variantLabel: String? = null,
    val processingForm: String? = null,
    val preparationState: String? = null,
    val productForm: String? = null,
) {
    init {
        val expectedTargetKind = HimP1HumanReviewSemanticContractV2.targetKindFor(kind)
        require(targetKind == expectedTargetKind) { "Target kind does not match semantic relation." }
        if (resolution == SemanticResolutionV2.RESOLVED) {
            require(
                targetReference != null &&
                    !semanticLabel.isNullOrBlank() &&
                    (targetKind != null || kind == SemanticRelationKindV2.COMPONENT_OR_DERIVED_PRODUCT_OF),
            ) {
                "A resolved relation requires structured target metadata."
            }
        }
        if (kind == SemanticRelationKindV2.PROCESSING_FORM_OF) require(!processingForm.isNullOrBlank())
        if (kind == SemanticRelationKindV2.PREPARATION_STATE_OF) require(!preparationState.isNullOrBlank())
        if (kind == SemanticRelationKindV2.PRODUCT_FORM_OF) require(!productForm.isNullOrBlank())
    }
}

data class RecordRelationV2(
    val evidenceRecordId: String,
    val evidenceReferenceIds: List<String>,
    val relation: SemanticRelationV2,
) {
    init {
        require(evidenceRecordId.isNotBlank())
        require(evidenceReferenceIds.isNotEmpty() && evidenceReferenceIds.distinct().size == evidenceReferenceIds.size)
    }
}

data class HumanRationaleEvidenceV2(
    val reviewerRef: String,
    val sourceDecisionIdentity: String,
    val text: String,
) {
    init {
        require(reviewerRef.isNotBlank() && sourceDecisionIdentity.isNotBlank() && text.isNotBlank())
    }
}

data class SemanticDecisionV2(
    val contractId: String,
    val version: String,
    val packetId: String,
    val candidateId: String,
    val candidateCanonicalId: String,
    val stableEntryId: String,
    val reviewUnitId: String,
    val v1BatchId: String,
    val v1BatchLogicalDigest: HimSha256,
    val v1DecisionRecordIdentity: String,
    val originalReviewerRef: String,
    val originalReviewRound: Int,
    val originalRevision: Int,
    val originalDecision: HimZeroCandidateRecoveryHumanReviewDecisionV1,
    val aggregateRelation: SemanticRelationV2,
    val recordRelations: List<RecordRelationV2>,
    val evidenceReferenceIds: List<String>,
    val rationaleEvidence: HumanRationaleEvidenceV2,
) {
    fun validate() {
        require(contractId == HimP1HumanReviewSemanticContractV2.CONTRACT_ID)
        require(version == HimP1HumanReviewSemanticContractV2.VERSION)
        require(packetId.matches(Regex("human-review-expansion-v2:[A-Za-z0-9]+")))
        require(candidateId.matches(Regex("[A-Za-z0-9]+")))
        require(candidateCanonicalId.matches(Regex("[A-Za-z0-9]{6}")))
        require(stableEntryId.matches(Regex("[0-9a-f]{64}")))
        require(reviewUnitId.matches(Regex("[0-9a-f]{64}")))
        require(v1BatchId.isNotBlank() && v1BatchLogicalDigest.value.matches(Regex("[0-9a-f]{64}")))
        require(v1DecisionRecordIdentity.isNotBlank() && originalReviewerRef.isNotBlank())
        require(originalReviewRound >= 1 && originalRevision >= 1)
        require(evidenceReferenceIds.distinct().size == evidenceReferenceIds.size)
        require(recordRelations.map { it.evidenceRecordId }.distinct().size == recordRelations.size)
        require(recordRelations.all { relation -> relation.evidenceReferenceIds.all { it in evidenceReferenceIds } })
        if (recordRelations.size > 1) {
            require(aggregateRelation.kind == SemanticRelationKindV2.UNRESOLVED_MIXED_RELATION)
            require(aggregateRelation.resolution != SemanticResolutionV2.RESOLVED)
        }
        require(rationaleEvidence.reviewerRef == originalReviewerRef)
        require(HimP1HumanReviewSemanticContractV2.decisionDigest(this).value.matches(Regex("[0-9a-f]{64}")))
    }

    val logicalDigest: HimSha256
        get() = HimP1HumanReviewSemanticContractV2.decisionDigest(this)
}

data class BlindSourceEvidenceV2(
    val evidenceRecordId: String,
    val kind: String,
    val artifactReference: String,
    val recordReference: String,
    val artifactSha256: String,
    val artifactLogicalDigest: String?,
    val fieldReferences: List<String>,
) {
    init {
        require(evidenceRecordId.isNotBlank() && kind.isNotBlank() && artifactReference.isNotBlank())
        require(recordReference.isNotBlank() && artifactSha256.matches(Regex("[0-9a-f]{64}")))
        require(artifactLogicalDigest == null || artifactLogicalDigest.matches(Regex("[0-9a-f]{64}")))
        require(fieldReferences == fieldReferences.distinct().sorted())
    }
}

data class BlindSecondPassPacketV2(
    val contractId: String,
    val version: String,
    val packetId: String,
    val candidateId: String,
    val candidateLabel: String,
    val stableEntryId: String,
    val canonicalEntityId: String,
    val sourceEvidence: List<BlindSourceEvidenceV2>,
    val allowedRelationKinds: List<SemanticRelationKindV2>,
    val decisionQuestion: String,
    val logicalDigest: HimSha256,
) {
    init {
        require(contractId == HimP1HumanReviewSemanticContractV2.CONTRACT_ID)
        require(version == HimP1HumanReviewSemanticContractV2.VERSION)
        require(packetId.isNotBlank() && candidateId.isNotBlank() && candidateLabel.isNotBlank())
        require(stableEntryId.matches(Regex("[0-9a-f]{64}")))
        require(canonicalEntityId.matches(Regex("[A-Za-z0-9]{6}")))
        require(sourceEvidence.map { it.evidenceRecordId }.distinct().size == sourceEvidence.size)
        require(allowedRelationKinds == allowedRelationKinds.distinct())
        require(decisionQuestion.isNotBlank())
        require(logicalDigest.value.matches(Regex("[0-9a-f]{64}")))
        require(logicalDigest == HimP1HumanReviewSemanticContractV2.blindPacketDigest(this))
    }
}

enum class SecondPassComparisonOutcomeV2 {
    EXACT_AGREEMENT,
    SEMANTICALLY_COMPATIBLE_AGREEMENT,
    MEANINGFUL_DISAGREEMENT,
    UNRESOLVED,
    REQUIRES_ESCALATION,
}

data class SecondPassComparisonV2(
    val outcome: SecondPassComparisonOutcomeV2,
    val sameReviewer: Boolean,
    val personallyIndependent: Boolean,
    val independentValidatorCount: Int,
)

object HimP1SecondPassComparisonAuthorityV2 {
    fun recordWiseSemanticsFullyResolved(
        decision: SemanticDecisionV2,
        expectedEvidenceRecordIds: Set<String>? = null,
    ): Boolean {
        val actualEvidenceRecordIds = decision.recordRelations.map { it.evidenceRecordId }.toSet()
        if (decision.recordRelations.isEmpty() || actualEvidenceRecordIds.size != decision.recordRelations.size) return false
        if (expectedEvidenceRecordIds != null && actualEvidenceRecordIds != expectedEvidenceRecordIds) return false
        return decision.recordRelations.all { recordRelation ->
            val relation = recordRelation.relation
            relation.resolution == SemanticResolutionV2.RESOLVED &&
                relation.targetReference != null &&
                !relation.semanticLabel.isNullOrBlank() &&
                (relation.targetKind != null || relation.kind == SemanticRelationKindV2.COMPONENT_OR_DERIVED_PRODUCT_OF) &&
                when (relation.kind) {
                    SemanticRelationKindV2.PROCESSING_FORM_OF -> !relation.processingForm.isNullOrBlank()
                    SemanticRelationKindV2.PREPARATION_STATE_OF -> !relation.preparationState.isNullOrBlank()
                    SemanticRelationKindV2.PRODUCT_FORM_OF -> !relation.productForm.isNullOrBlank()
                    else -> true
                }
        }
    }

    fun compare(
        primary: SemanticDecisionV2,
        secondPass: SemanticDecisionV2,
        expectedEvidenceRecordIds: Set<String>? = null,
    ): SecondPassComparisonV2 {
        primary.validate()
        secondPass.validate()
        require(primary.reviewUnitId == secondPass.reviewUnitId)
        val sameReviewer = primary.originalReviewerRef == secondPass.originalReviewerRef
        val outcome = when {
            secondPass.originalDecision == HimZeroCandidateRecoveryHumanReviewDecisionV1.ESCALATE_OUT_OF_SCOPE ->
                SecondPassComparisonOutcomeV2.REQUIRES_ESCALATION
            recordWiseSemanticsFullyResolved(primary, expectedEvidenceRecordIds) &&
                recordWiseSemanticsFullyResolved(secondPass, expectedEvidenceRecordIds) ->
                compareCompleteRecordWise(primary, secondPass)
            primary.aggregateRelation.resolution == SemanticResolutionV2.RESOLVED &&
                secondPass.aggregateRelation.resolution == SemanticResolutionV2.RESOLVED ->
                compareStructuredAggregates(primary, secondPass)
            else -> SecondPassComparisonOutcomeV2.UNRESOLVED
        }
        return SecondPassComparisonV2(
            outcome = outcome,
            sameReviewer = sameReviewer,
            personallyIndependent = false,
            independentValidatorCount = 0,
        )
    }

    private fun compareCompleteRecordWise(
        primary: SemanticDecisionV2,
        secondPass: SemanticDecisionV2,
    ): SecondPassComparisonOutcomeV2 {
        val primaryByRecord = primary.recordRelations.associateBy { it.evidenceRecordId }
        val secondByRecord = secondPass.recordRelations.associateBy { it.evidenceRecordId }
        if (primaryByRecord.keys != secondByRecord.keys) return SecondPassComparisonOutcomeV2.MEANINGFUL_DISAGREEMENT
        if (primaryByRecord.any { (recordId, relation) -> relation.relation != secondByRecord.getValue(recordId).relation }) {
            return SecondPassComparisonOutcomeV2.MEANINGFUL_DISAGREEMENT
        }
        if (primary.originalDecision != secondPass.originalDecision) return SecondPassComparisonOutcomeV2.MEANINGFUL_DISAGREEMENT
        return if (aggregateContainersEquivalent(primary.aggregateRelation, secondPass.aggregateRelation)) {
            SecondPassComparisonOutcomeV2.EXACT_AGREEMENT
        } else {
            SecondPassComparisonOutcomeV2.SEMANTICALLY_COMPATIBLE_AGREEMENT
        }
    }

    private fun compareStructuredAggregates(
        primary: SemanticDecisionV2,
        secondPass: SemanticDecisionV2,
    ): SecondPassComparisonOutcomeV2 = when {
        primary.originalDecision != secondPass.originalDecision -> SecondPassComparisonOutcomeV2.MEANINGFUL_DISAGREEMENT
        primary.aggregateRelation == secondPass.aggregateRelation -> SecondPassComparisonOutcomeV2.EXACT_AGREEMENT
        relationKindsCompatible(primary.aggregateRelation, secondPass.aggregateRelation) ->
            SecondPassComparisonOutcomeV2.SEMANTICALLY_COMPATIBLE_AGREEMENT
        else -> SecondPassComparisonOutcomeV2.MEANINGFUL_DISAGREEMENT
    }

    private fun aggregateContainersEquivalent(
        first: SemanticRelationV2,
        second: SemanticRelationV2,
    ): Boolean = first == second || (
        first.kind == SemanticRelationKindV2.UNRESOLVED_MIXED_RELATION &&
            second.kind == SemanticRelationKindV2.UNRESOLVED_MIXED_RELATION
        )

    private fun relationKindsCompatible(first: SemanticRelationV2, second: SemanticRelationV2): Boolean =
        first.targetKind == HimCandidateType.VARIANT &&
            second.targetKind == HimCandidateType.VARIANT &&
            first.targetReference == second.targetReference
}
