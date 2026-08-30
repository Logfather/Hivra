package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateRelation
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateOccurrenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionTargetV1
import de.shopme.tools.knowledge.him.training.corpus.HimGroundTruthTrainingExampleProjectionInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimGroundTruthTrainingExampleProjectionResultV1
import de.shopme.tools.knowledge.him.training.corpus.HimGroundTruthTrainingExampleProjectionV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleIdentityV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleReference
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleValidatorV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Pure in-memory boundary from complete historical Ground-Truth lineage to a
 * positive training example. It does not discover, persist, partition, or
 * publish anything.
 */
object HimZeroCandidateRecoveryHumanReviewP1PilotPositiveTrainingExampleMaterializationContractV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_POSITIVE_TRAINING_EXAMPLE_MATERIALIZATION_CONTRACT_V1"
    const val VERSION = "1"
    const val STATE = "POSITIVE_TRAINING_EXAMPLE_MATERIALIZATION_CONTEXT_ONLY"

    private const val DECISION_ID_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_POSITIVE_TRAINING_EXAMPLE_MATERIALIZATION_DECISION_V1"
    private const val BATCH_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_POSITIVE_TRAINING_EXAMPLE_MATERIALIZATION_BATCH_V1"
    private const val PROJECTION_BINDING_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_POSITIVE_TRAINING_EXAMPLE_MATERIALIZATION_INPUT_V1"
    private val SHA256 = Regex("[0-9a-f]{64}")
    private val ZERO_DIGEST = "0".repeat(64)

    fun materialize(
        request: Request,
    ): MaterializationDecision {
        val reasons = preflight(request)
        if (reasons.isNotEmpty()) {
            return notYetProjectable(request, reasons)
        }

        val projectionResult = try {
            HimGroundTruthTrainingExampleProjectionV1().project(request.projectionInput)
        } catch (_: IllegalArgumentException) {
            return notYetProjectable(request, listOf(FailureReason.INVALID_PROJECTED_TRAINING_EXAMPLE))
        } catch (_: IllegalStateException) {
            return notYetProjectable(request, listOf(FailureReason.INVALID_PROJECTED_TRAINING_EXAMPLE))
        }

        return when (projectionResult) {
            is HimGroundTruthTrainingExampleProjectionResultV1.NotYetProjectable ->
                notYetProjectable(
                    request,
                    listOf(
                        if (request.projectionInput.promotion.target is
                            de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionTargetV1.CreateCanonical
                        ) {
                            FailureReason.UNSUPPORTED_CREATE_CANONICAL_PROJECTION
                        } else {
                            FailureReason.INVALID_PROJECTED_TRAINING_EXAMPLE
                        },
                    ),
                )

            is HimGroundTruthTrainingExampleProjectionResultV1.Projected ->
                projected(request, projectionResult.example)
        }
    }

    fun materializeBatch(
        requests: List<Request>,
    ): BatchResult {
        if (requests.isEmpty()) {
            return BatchResult.Failed(FailureReason.INVALID_MATERIALIZATION_CONTEXT, "empty-request")
        }
        val decisions = requests.map(::materialize)
        if (requests.map { it.preMaterializationIdentity }.distinct().size != requests.size) {
            return BatchResult.Failed(FailureReason.DUPLICATE_PRE_MATERIALIZATION_IDENTITY, "occurrence")
        }
        if (decisions.map { it.projectionInputBindingDigest }.distinct().size != decisions.size) {
            return BatchResult.Failed(FailureReason.DUPLICATE_PROJECTION_INPUT_BINDING, "projection-input")
        }
        val projectedReferences = decisions.mapNotNull { it.trainingExampleReference }
        if (projectedReferences.distinct().size != projectedReferences.size) {
            return BatchResult.Failed(FailureReason.DUPLICATE_MATERIALIZED_TRAINING_EXAMPLE, "example-reference")
        }

        val unsigned = Batch(
            contractId = CONTRACT_ID,
            version = VERSION,
            state = STATE,
            decisions = decisions,
            counters = Counters.from(decisions),
            logicalDigest = ZERO_DIGEST,
        )
        val batch = unsigned.copy(logicalDigest = batchLogicalDigest(unsigned))
        validate(batch)
        return BatchResult.Completed(batch)
    }

    fun validate(batch: Batch) {
        require(batch.contractId == CONTRACT_ID)
        require(batch.version == VERSION)
        require(batch.state == STATE)
        require(batch.decisions.map { it.decisionId }.distinct().size == batch.decisions.size)
        require(batch.decisions.map { it.preMaterializationIdentity }.distinct().size == batch.decisions.size)
        require(batch.decisions.map { it.projectionInputBindingDigest }.distinct().size == batch.decisions.size)
        val projectedReferences = batch.decisions.mapNotNull { it.trainingExampleReference }
        require(projectedReferences.distinct().size == projectedReferences.size)
        require(batch.counters == Counters.from(batch.decisions))
        require(SHA256.matches(batch.logicalDigest))
        require(batch.logicalDigest == batchLogicalDigest(batch.copy(logicalDigest = ZERO_DIGEST)))
    }

    fun batchLogicalDigest(batch: Batch): String = sha256(
        buildString {
            appendLine(BATCH_DIGEST_DOMAIN)
            appendLine("contractId=${batch.contractId}")
            appendLine("version=${batch.version}")
            appendLine("state=${batch.state}")
            batch.decisions.forEach { decision ->
                appendLine("decisionId=${decision.decisionId}")
                appendLine("preMaterializationIdentity=${decision.preMaterializationIdentity.value}")
                appendLine("occurrenceReference=${decision.occurrenceReference.value}")
                appendLine("projectionInputBindingDigest=${decision.projectionInputBindingDigest.value}")
                appendLine("state=${decision.state.name}")
                appendLine("trainingExampleReference=${decision.trainingExampleReference?.value.orEmpty()}")
                decision.reasons.forEach { appendLine("reason=${it.name}") }
            }
            appendLine("totalRecords=${batch.counters.totalRecords}")
            appendLine("projected=${batch.counters.projected}")
            appendLine("notYetProjectable=${batch.counters.notYetProjectable}")
        },
    )

    private fun preflight(request: Request): List<FailureReason> {
        val input = request.projectionInput
        val occurrence = input.occurrence
        val reasons = mutableListOf<FailureReason>()
        if (request.preMaterializationIdentity != occurrence.occurrenceReference) {
            reasons += FailureReason.MISSING_OCCURRENCE_BINDING
        }
        if (occurrence !in input.generationRun.occurrences) {
            reasons += FailureReason.INCONSISTENT_OCCURRENCE_LINEAGE
        }
        val inputRun = input.generationRun.inputRuns.singleOrNull {
            it.inputRunReference == occurrence.inputRunReference
        }
        if (inputRun == null) {
            reasons += FailureReason.MISSING_INPUT_RUN
        } else if (input.candidate.candidateReference !in inputRun.persistedCandidateReferences) {
            reasons += FailureReason.INCONSISTENT_CANDIDATE_LINEAGE
        }
        if (inputRun?.input?.rawInput.isNullOrBlank()) {
            reasons += FailureReason.MISSING_RAW_INPUT
        }
        if (inputRun?.input?.normalizedLookup.isNullOrBlank()) {
            reasons += FailureReason.MISSING_NORMALIZED_INPUT
        }
        if (inputRun?.canonicalContext.isNullOrEmpty()) {
            reasons += FailureReason.MISSING_CANONICAL_CONTEXT
        }
        if (occurrence.evidence.isEmpty()) {
            reasons += FailureReason.MISSING_TYPED_EVIDENCE
        }
        if (occurrence.evidence.map { it.reference }.distinct().size != occurrence.evidence.size) {
            reasons += FailureReason.INCONSISTENT_EVIDENCE_LINEAGE
        }
        if (input.validation.candidateReference != input.candidate.candidateReference ||
            input.validation.candidateDatasetDigest != input.candidateDatasetDigest
        ) {
            reasons += FailureReason.MISSING_VALIDATION_LINEAGE
        }
        if (input.promotion.candidateReference != input.candidate.candidateReference ||
            input.promotion.candidateDatasetDigest != input.candidateDatasetDigest ||
            input.promotion.validationReference != input.validation.validationReference
        ) {
            reasons += FailureReason.MISSING_PROMOTION_LINEAGE
        }
        if (input.promotion.target.targetName() == input.candidate.candidateTerm &&
            !targetLineageMatches(input.candidate.relation, input.promotion.target)
        ) {
            reasons += FailureReason.INCONSISTENT_TARGET_LINEAGE
        }

        if (input.promotion.target is
            de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionTargetV1.CreateCanonical
        ) {
            reasons += FailureReason.UNSUPPORTED_CREATE_CANONICAL_PROJECTION
        } else {
            if (input.mutation == null) reasons += FailureReason.MISSING_MUTATION_LINEAGE
            if (input.promotionExecution == null) reasons += FailureReason.MISSING_EXECUTION_LINEAGE
            if (input.resultingAuthority == null) reasons += FailureReason.MISSING_RESULTING_AUTHORITY_SNAPSHOT
            if (input.resultingEntityIdRegistry == null) reasons += FailureReason.MISSING_RESULTING_REGISTRY_SNAPSHOT
        }
        return reasons.distinct()
    }

    private fun targetLineageMatches(
        relation: HimCandidateRelation,
        target: HimCandidatePromotionTargetV1,
    ): Boolean = when (relation) {
        is HimCandidateRelation.Identity ->
            target is HimCandidatePromotionTargetV1.AddIdentity &&
                relation.parentCanonicalId == target.parentCanonicalId
        is HimCandidateRelation.Variant ->
            target is HimCandidatePromotionTargetV1.AddVariant &&
                relation.scope == target.scope
        is HimCandidateRelation.Alias ->
            target is HimCandidatePromotionTargetV1.AddAlias &&
                relation.equivalentEntity == target.equivalentEntity
        HimCandidateRelation.CreateNewCanonical ->
            target is HimCandidatePromotionTargetV1.CreateCanonical
    }

    private fun HimCandidatePromotionTargetV1.targetName(): String =
        when (this) {
            is HimCandidatePromotionTargetV1.AddIdentity -> identityName
            is HimCandidatePromotionTargetV1.AddVariant -> variantName
            is HimCandidatePromotionTargetV1.AddAlias -> aliasName
            is HimCandidatePromotionTargetV1.CreateCanonical -> canonicalName
        }

    private fun projected(
        request: Request,
        example: HimTrainingExampleV1,
    ): MaterializationDecision {
        try {
            HimTrainingExampleValidatorV1.validate(example)
            val expectedReference = HimTrainingExampleIdentityV1.example(
                taskType = example.taskType,
                input = example.input,
                target = example.target,
                provenance = example.provenance,
            )
            require(example.exampleReference == expectedReference)
        } catch (_: IllegalArgumentException) {
            return notYetProjectable(request, listOf(FailureReason.INVALID_PROJECTED_TRAINING_EXAMPLE))
        }
        val inputBindingDigest = projectionInputBindingDigest(request.projectionInput)
        val decisionId = decisionId(
            request = request,
            state = MaterializationState.PROJECTED,
            reasons = emptyList(),
            trainingExampleReference = example.exampleReference,
        )
        return MaterializationDecision(
            decisionId = decisionId,
            preMaterializationIdentity = request.preMaterializationIdentity,
            occurrenceReference = request.projectionInput.occurrence.occurrenceReference,
            projectionInputBindingDigest = inputBindingDigest,
            state = MaterializationState.PROJECTED,
            reasons = emptyList(),
            projectedTrainingExample = example,
            trainingExampleReference = example.exampleReference,
        )
    }

    private fun notYetProjectable(
        request: Request,
        reasons: List<FailureReason>,
    ): MaterializationDecision {
        val normalizedReasons = reasons.distinct().ifEmpty {
            listOf(FailureReason.INVALID_MATERIALIZATION_CONTEXT)
        }
        return MaterializationDecision(
            decisionId = decisionId(
                request = request,
                state = MaterializationState.NOT_YET_PROJECTABLE,
                reasons = normalizedReasons,
                trainingExampleReference = null,
            ),
            preMaterializationIdentity = request.preMaterializationIdentity,
            occurrenceReference = request.projectionInput.occurrence.occurrenceReference,
            projectionInputBindingDigest = projectionInputBindingDigest(request.projectionInput),
            state = MaterializationState.NOT_YET_PROJECTABLE,
            reasons = normalizedReasons,
            projectedTrainingExample = null,
            trainingExampleReference = null,
        )
    }

    private fun decisionId(
        request: Request,
        state: MaterializationState,
        reasons: List<FailureReason>,
        trainingExampleReference: HimTrainingExampleReference?,
    ): String = sha256(
        buildString {
            appendLine(DECISION_ID_DOMAIN)
            appendLine("preMaterializationIdentity=${request.preMaterializationIdentity.value}")
            appendLine("occurrenceReference=${request.projectionInput.occurrence.occurrenceReference.value}")
            appendLine("candidateReference=${request.projectionInput.candidate.candidateReference.value}")
            appendLine("generationRunReference=${request.projectionInput.generationRun.runReference.value}")
            appendLine("validationReference=${request.projectionInput.validation.validationReference.value}")
            appendLine("promotionReference=${request.projectionInput.promotion.promotionReference.value}")
            appendLine("releaseReference=${request.projectionInput.groundTruthReleaseReference.value}")
            appendLine("state=${state.name}")
            reasons.forEach { appendLine("reason=${it.name}") }
            appendLine("trainingExampleReference=${trainingExampleReference?.value.orEmpty()}")
        },
    )

    private fun projectionInputBindingDigest(
        input: HimGroundTruthTrainingExampleProjectionInputV1,
    ): HimSha256 = HimSha256(
        sha256(
            buildString {
                appendLine(PROJECTION_BINDING_DOMAIN)
                appendLine("occurrence=${input.occurrence.occurrenceReference.value}")
                appendLine("inputRun=${input.occurrence.inputRunReference.value}")
                appendLine("generationRun=${input.generationRun.runReference.value}")
                appendLine("candidate=${input.candidate.candidateReference.value}")
                appendLine("validation=${input.validation.validationReference.value}")
                appendLine("promotion=${input.promotion.promotionReference.value}")
                appendLine("release=${input.groundTruthReleaseReference.value}")
            },
        ),
    )

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    enum class MaterializationState {
        PROJECTED,
        NOT_YET_PROJECTABLE,
    }

    enum class FailureReason {
        INVALID_MATERIALIZATION_CONTEXT,
        MISSING_OCCURRENCE_BINDING,
        MISSING_INPUT_RUN,
        MISSING_RAW_INPUT,
        MISSING_NORMALIZED_INPUT,
        MISSING_CANONICAL_CONTEXT,
        MISSING_TYPED_EVIDENCE,
        MISSING_VALIDATION_LINEAGE,
        MISSING_PROMOTION_LINEAGE,
        MISSING_MUTATION_LINEAGE,
        MISSING_EXECUTION_LINEAGE,
        MISSING_RESULTING_AUTHORITY_SNAPSHOT,
        MISSING_RESULTING_REGISTRY_SNAPSHOT,
        INCONSISTENT_CANDIDATE_LINEAGE,
        INCONSISTENT_OCCURRENCE_LINEAGE,
        INCONSISTENT_TARGET_LINEAGE,
        INCONSISTENT_EVIDENCE_LINEAGE,
        INVALID_PROJECTED_TRAINING_EXAMPLE,
        UNSUPPORTED_CREATE_CANONICAL_PROJECTION,
        DUPLICATE_PRE_MATERIALIZATION_IDENTITY,
        DUPLICATE_PROJECTION_INPUT_BINDING,
        DUPLICATE_MATERIALIZED_TRAINING_EXAMPLE,
    }

    data class Request(
        val preMaterializationIdentity: HimCandidateOccurrenceReference,
        val projectionInput: HimGroundTruthTrainingExampleProjectionInputV1,
    )

    data class MaterializationDecision(
        val decisionId: String,
        val preMaterializationIdentity: HimCandidateOccurrenceReference,
        val occurrenceReference: HimCandidateOccurrenceReference,
        val projectionInputBindingDigest: HimSha256,
        val state: MaterializationState,
        val reasons: List<FailureReason>,
        val projectedTrainingExample: HimTrainingExampleV1?,
        val trainingExampleReference: HimTrainingExampleReference?,
    ) {
        init {
            require(SHA256.matches(decisionId))
            require(reasons.distinct().size == reasons.size)
            require(
                (state == MaterializationState.PROJECTED) ==
                    (projectedTrainingExample != null && trainingExampleReference != null && reasons.isEmpty()),
            )
            require(
                state == MaterializationState.PROJECTED ||
                    (projectedTrainingExample == null && trainingExampleReference == null && reasons.isNotEmpty()),
            )
            if (projectedTrainingExample != null) {
                require(projectedTrainingExample.exampleReference == trainingExampleReference)
            }
        }
    }

    data class Batch(
        val contractId: String,
        val version: String,
        val state: String,
        val decisions: List<MaterializationDecision>,
        val counters: Counters,
        val logicalDigest: String,
    )

    data class Counters(
        val totalRecords: Int,
        val projected: Int,
        val notYetProjectable: Int,
    ) {
        init {
            require(totalRecords >= 0)
            require(projected >= 0)
            require(notYetProjectable >= 0)
            require(projected + notYetProjectable == totalRecords)
        }

        companion object {
            fun from(decisions: List<MaterializationDecision>) = Counters(
                totalRecords = decisions.size,
                projected = decisions.count { it.state == MaterializationState.PROJECTED },
                notYetProjectable = decisions.count { it.state == MaterializationState.NOT_YET_PROJECTABLE },
            )
        }
    }

    sealed interface BatchResult {
        data class Completed(val batch: Batch) : BatchResult

        data class Failed(
            val reason: FailureReason,
            val safeContext: String,
        ) : BatchResult {
            init {
                require(safeContext.isNotBlank())
            }
        }
    }

}
