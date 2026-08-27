package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyValidator
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistry
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster

object HimZeroCandidateRecoveryHumanReviewRuntimeV1 {
    const val CONTRACT_ID = "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_RUNTIME_V1"
    const val VERSION = "1"
    const val SCOPE_DIGEST_DOMAIN = "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_RUNTIME_SCOPE_V1"

    fun scopeDigest(scope: HimZeroCandidateRecoveryHumanReviewScopeV1): String =
        HimZeroCandidateRecoveryReviewCorpusPersistenceV1.sha256(
            buildString {
                appendLine(SCOPE_DIGEST_DOMAIN)
                appendLine("runtimeContractId=$CONTRACT_ID")
                appendLine("runtimeVersion=$VERSION")
                appendLine("scopeId=${scope.scopeId}")
                appendLine("inputBindingDigest=${scope.inputBindingDigest}")
                appendLine("reviewRound=${scope.reviewRound}")
                scope.authorizedReviewUnitIds.sorted().forEach { appendLine("reviewUnitId=$it") }
                appendLine("maxNewDecisionRecords=${scope.maxNewDecisionRecords}")
            },
        )

    fun execute(
        request: HimZeroCandidateRecoveryHumanReviewRuntimeRequestV1,
    ): HimZeroCandidateRecoveryHumanReviewRuntimeResult =
        if (!request.enabled) {
            HimZeroCandidateRecoveryHumanReviewRuntimeResult.Disabled
        } else {
            try {
                validateRequestShape(request)
                val units = resolveUnits(request.corpus)
                validateFoundation(request.catalog, request.registry, request.authority)
                validateTargets(units, request.registry, request.authority)
                validateInputBinding(request.inputBinding, request.corpus, request.catalog)
                val scope = validateScope(request.reviewScope, request.inputBinding, units)
                val unitById = units.associateBy { it.reviewUnitId }
                val prior = validateRecords(
                    request.priorDecisionRecords,
                    unitById,
                    scope,
                    request.registry,
                    request.authority,
                    prior = true,
                )
                val submitted = validateRecords(
                    request.submittedDecisionRecords,
                    unitById,
                    scope,
                    request.registry,
                    request.authority,
                    prior = false,
                )
                if (submitted.isEmpty()) fail(FailureReason.EMPTY_SUBMITTED_DECISIONS, "submitted")
                if (submitted.size > scope.maxNewDecisionRecords) {
                    fail(FailureReason.SUBMISSION_LIMIT_EXCEEDED, "submitted")
                }
                validateRevisionSequences(prior, submitted, scope.reviewRound)
                val current = currentDecisions(prior + submitted, scope.reviewRound)
                val resolutions = current.keys
                    .mapNotNull { unitById[it] }
                    .sortedBy { it.reviewUnitId }
                    .map { unit ->
                        HimZeroCandidateRecoveryHumanReviewResolutionV1(
                            reviewUnitId = unit.reviewUnitId,
                            resolutionState = resolveState(current.getValue(unit.reviewUnitId)),
                            currentReviewerRefs = current.getValue(unit.reviewUnitId).map { it.reviewerRef }.distinct().sorted(),
                        )
                    }
                val counters = deriveCounters(
                    corpus = request.corpus,
                    authorizedUnits = scope.authorizedReviewUnitIds.size,
                    prior = prior,
                    submitted = submitted,
                    resolutions = resolutions,
                )
                val batch = buildBatch(request, submitted)
                val persistence = when (
                    val result = HimZeroCandidateRecoveryHumanReviewPersistenceV1.execute(
                        HimZeroCandidateRecoveryHumanReviewPersistenceRequestV1(
                            request.durableDecisionBatchRoot,
                            request.derivedReportRoot,
                            batch,
                        ),
                    )
                ) {
                    is HimZeroCandidateRecoveryHumanReviewPersistenceResultV1.Completed -> result
                    is HimZeroCandidateRecoveryHumanReviewPersistenceResultV1.Failed ->
                        fail(FailureReason.PERSISTENCE_FAILED, "persistence")
                }
                val persistedFile = request.durableDecisionBatchRoot
                    .resolve(batch.batchId)
                    .resolve("review-decisions.v1.json")
                val reloaded = try {
                    HimZeroCandidateRecoveryHumanReviewPersistenceV1.readBatch(persistedFile)
                } catch (_: Throwable) {
                    fail(FailureReason.PERSISTENCE_RELOAD_MISMATCH, "reload")
                }
                if (reloaded != persistence.batch) fail(FailureReason.PERSISTENCE_RELOAD_MISMATCH, "reload")
                HimZeroCandidateRecoveryHumanReviewRuntimeResult.Completed(
                    runtimeContractId = CONTRACT_ID,
                    runtimeVersion = VERSION,
                    scope = scope,
                    authorizedReviewUnits = scope.authorizedReviewUnitIds.map { unitById.getValue(it) },
                    submittedDecisionRecords = submitted,
                    resolutions = resolutions,
                    counters = counters.copy(persistedDecisionRecords = submitted.size),
                    decisionBatch = persistence.batch,
                    persistenceResult = persistence,
                )
            } catch (failure: RuntimeFailure) {
                HimZeroCandidateRecoveryHumanReviewRuntimeResult.Failed(failure.reason, failure.safeContext)
            } catch (_: Throwable) {
                HimZeroCandidateRecoveryHumanReviewRuntimeResult.Failed(
                    FailureReason.INTERNAL_INVARIANT_VIOLATION,
                    "runtime",
                )
            }
        }

    private fun validateRequestShape(request: HimZeroCandidateRecoveryHumanReviewRuntimeRequestV1) {
        if (request.batchId.isBlank()) fail(FailureReason.INVALID_REQUEST, "batchId")
        if (request.durableDecisionBatchRoot.path.isBlank() || request.derivedReportRoot.path.isBlank()) {
            fail(FailureReason.INVALID_REQUEST, "roots")
        }
    }

    private fun resolveUnits(
        corpus: HimZeroCandidateRecoveryReviewCorpusReportV1,
    ): List<HimZeroCandidateRecoveryHumanReviewReviewUnitV1> {
        try {
            corpus.validate()
        } catch (_: Throwable) {
            fail(FailureReason.INVALID_CORPUS, "corpus")
        }
        val units = corpus.entries.flatMap { entry ->
            entry.auditLinkedCanonicalTargets.map { target ->
                HimZeroCandidateRecoveryHumanReviewReviewUnitV1(entry.stableEntryId, target.canonicalEntityId)
            }
        }.sortedWith(compareBy({ it.stableEntryId }, { it.canonicalEntityId }, { it.reviewUnitId }))
        if (units.any { !it.validate().valid }) fail(FailureReason.INVALID_CORPUS, "units")
        if (units.map { it.reviewUnitId }.distinct().size != units.size) {
            fail(FailureReason.DUPLICATE_REVIEW_UNIT, "units")
        }
        return units
    }

    private fun validateFoundation(
        catalog: HimProductOnlyCanonicalMaster,
        registry: HimEntityIdRegistry,
        authority: HimCanonicalFamilyAuthority,
    ) {
        try {
            HimCanonicalFamilyValidator().validate(catalog, registry, authority)
        } catch (_: Throwable) {
            fail(FailureReason.INVALID_FOUNDATION, "foundation")
        }
    }

    private fun validateTargets(
        units: List<HimZeroCandidateRecoveryHumanReviewReviewUnitV1>,
        registry: HimEntityIdRegistry,
        authority: HimCanonicalFamilyAuthority,
    ) {
        val registryIds = registry.entries
            .filter { it.entityType == HimEntityType.CANONICAL }
            .map { it.entityId.value }
            .toSet()
        val authorityIds = authority.families.map { it.canonicalId.value }.toSet()
        if (units.any { it.canonicalEntityId !in registryIds || it.canonicalEntityId !in authorityIds }) {
            fail(FailureReason.INVALID_FOUNDATION, "targets")
        }
    }

    private fun validateInputBinding(
        binding: HimZeroCandidateRecoveryHumanReviewInputBindingV1,
        corpus: HimZeroCandidateRecoveryReviewCorpusReportV1,
        catalog: HimProductOnlyCanonicalMaster,
    ) {
        if (!binding.validate().valid) fail(FailureReason.INVALID_INPUT_BINDING, "inputBinding")
        try {
            binding.existingCorpusInputBinding.validate()
        } catch (_: Throwable) {
            fail(FailureReason.INVALID_INPUT_BINDING, "corpusBinding")
        }
        if (binding.bindingDigest != HimZeroCandidateRecoveryHumanReviewPersistenceV1.bindingDigest(binding)) {
            fail(FailureReason.INPUT_BINDING_MISMATCH, "inputBinding")
        }
        if (binding.corpusLogicalDigest != corpus.logicalDigest ||
            binding.corpusFileBinding.logicalDigest != corpus.logicalDigest ||
            binding.existingCorpusInputBinding != corpus.inputBinding ||
            binding.corpusFileBinding.sha256.isBlank() ||
            binding.corpusFileBinding.relativePath.isBlank()
        ) {
            fail(FailureReason.INPUT_BINDING_MISMATCH, "corpus")
        }
        if (binding.existingCorpusInputBinding.canonicalCatalog.relativePath != catalog.path ||
            binding.existingCorpusInputBinding.canonicalCatalog.sha256 != catalog.contentSha256
        ) {
            fail(FailureReason.INPUT_BINDING_MISMATCH, "catalog")
        }
    }

    private fun validateScope(
        scope: HimZeroCandidateRecoveryHumanReviewScopeV1,
        binding: HimZeroCandidateRecoveryHumanReviewInputBindingV1,
        units: List<HimZeroCandidateRecoveryHumanReviewReviewUnitV1>,
    ): HimZeroCandidateRecoveryHumanReviewScopeV1 {
        if (!scope.scopeId.matches(SCOPE_ID) || scope.scopeId.contains("..") || scope.scopeId.any { it.isWhitespace() }) {
            fail(FailureReason.INVALID_SCOPE, "scopeId")
        }
        if (scope.reviewRound < 1 || scope.maxNewDecisionRecords < 1) fail(FailureReason.INVALID_SCOPE, "scope")
        if (scope.authorizedReviewUnitIds.isEmpty() ||
            scope.authorizedReviewUnitIds != scope.authorizedReviewUnitIds.distinct().sorted()
        ) {
            fail(FailureReason.INVALID_SCOPE, "reviewUnits")
        }
        if (scope.inputBindingDigest != binding.bindingDigest) fail(FailureReason.INVALID_SCOPE, "binding")
        val known = units.map { it.reviewUnitId }.toSet()
        if (scope.authorizedReviewUnitIds.any { it !in known }) {
            fail(FailureReason.UNKNOWN_AUTHORIZED_REVIEW_UNIT, "reviewUnits")
        }
        if (scope.scopeDigest != scopeDigest(scope)) fail(FailureReason.SCOPE_DIGEST_MISMATCH, "scope")
        return scope
    }

    private fun validateRecords(
        records: List<HimZeroCandidateRecoveryHumanReviewDecisionRecordV1>,
        unitById: Map<String, HimZeroCandidateRecoveryHumanReviewReviewUnitV1>,
        scope: HimZeroCandidateRecoveryHumanReviewScopeV1,
        registry: HimEntityIdRegistry,
        authority: HimCanonicalFamilyAuthority,
        prior: Boolean,
    ): List<HimZeroCandidateRecoveryHumanReviewDecisionRecordV1> {
        val identities = mutableSetOf<String>()
        return records.map { record ->
            when (val validation = record.validate()) {
                HimZeroCandidateRecoveryHumanReviewValidationResultV1.Valid -> Unit
                is HimZeroCandidateRecoveryHumanReviewValidationResultV1.Invalid ->
                    fail(if (prior) FailureReason.INVALID_PRIOR_DECISION else FailureReason.INVALID_SUBMITTED_DECISION, "record")
            }
            val unit = unitById[record.reviewUnit.reviewUnitId]
                ?: fail(if (prior) FailureReason.INVALID_PRIOR_DECISION else FailureReason.REVIEW_UNIT_BINDING_MISMATCH, "unit")
            if (record.reviewUnit != unit) fail(FailureReason.REVIEW_UNIT_BINDING_MISMATCH, "unit")
            if (record.reviewRound != scope.reviewRound) fail(FailureReason.REVIEW_ROUND_MISMATCH, "round")
            if (!prior && record.reviewUnit.reviewUnitId !in scope.authorizedReviewUnitIds) {
                fail(FailureReason.UNAUTHORIZED_REVIEW_UNIT, "scope")
            }
            val identity = listOf(record.reviewUnit.reviewUnitId, record.reviewerRef, record.reviewRound, record.revision).joinToString("\u0000")
            if (!identities.add(identity)) {
                fail(FailureReason.DUPLICATE_REVIEWER_SUBMISSION, "reviewer")
            }
            record.alternativeCanonicalProposal?.let { proposal ->
                val exists = authority.families.count { it.canonicalId.value == proposal.canonicalEntityId } == 1 &&
                    registry.entries.count { it.entityId.value == proposal.canonicalEntityId && it.entityType == HimEntityType.CANONICAL } == 1
                if (!exists) fail(FailureReason.ALTERNATIVE_CANONICAL_NOT_FOUND, "alternative")
            }
            record
        }
    }

    private fun validateRevisionSequences(
        prior: List<HimZeroCandidateRecoveryHumanReviewDecisionRecordV1>,
        submitted: List<HimZeroCandidateRecoveryHumanReviewDecisionRecordV1>,
        reviewRound: Int,
    ) {
        val priorGroups = prior.groupBy { listOf(it.reviewUnit.reviewUnitId, it.reviewerRef, reviewRound) }
        priorGroups.values.forEach { records ->
            val revisions = records.map { it.revision }.sorted()
            if (revisions != (1..revisions.size).toList()) fail(FailureReason.INVALID_REVISION_SEQUENCE, "prior")
        }
        val submittedGroups = submitted.groupBy { listOf(it.reviewUnit.reviewUnitId, it.reviewerRef, reviewRound) }
        if (submittedGroups.values.any { it.size > 1 }) fail(FailureReason.DUPLICATE_REVIEWER_SUBMISSION, "reviewer")
        submitted.forEach { record ->
            val priorMax = priorGroups[listOf(record.reviewUnit.reviewUnitId, record.reviewerRef, reviewRound)]
                ?.maxOfOrNull { it.revision } ?: 0
            val expected = priorMax + 1
            if (record.revision <= priorMax) fail(FailureReason.STALE_REVISION, "revision")
            if (record.revision != expected) fail(FailureReason.INVALID_REVISION_SEQUENCE, "revision")
        }
    }

    private fun currentDecisions(
        records: List<HimZeroCandidateRecoveryHumanReviewDecisionRecordV1>,
        reviewRound: Int,
    ): Map<String, List<HimZeroCandidateRecoveryHumanReviewDecisionRecordV1>> = records
        .filter { it.reviewRound == reviewRound }
        .groupBy { it.reviewUnit.reviewUnitId }
        .mapValues { (_, unitRecords) ->
            unitRecords.groupBy { it.reviewerRef }
                .values
                .map { it.maxBy { record -> record.revision } }
                .sortedWith(compareBy({ it.reviewerRef }, { it.revision }))
        }

    private fun resolveState(
        records: List<HimZeroCandidateRecoveryHumanReviewDecisionRecordV1>,
    ): HimZeroCandidateRecoveryHumanReviewResolutionStateV1 = when {
        records.any { it.decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.ESCALATE_OUT_OF_SCOPE } ->
            HimZeroCandidateRecoveryHumanReviewResolutionStateV1.ESCALATED_OUT_OF_SCOPE
        records.any {
            it.decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_INSUFFICIENT_EVIDENCE ||
                it.decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_AMBIGUOUS ||
                it.decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_CONFLICTING_EVIDENCE
        } -> HimZeroCandidateRecoveryHumanReviewResolutionStateV1.ABSTENTION_PRESENT
        records.size == 1 -> HimZeroCandidateRecoveryHumanReviewResolutionStateV1.SINGLE_REVIEW_ONLY
        records.all { it.decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION } ->
            HimZeroCandidateRecoveryHumanReviewResolutionStateV1.INDEPENDENT_CONFIRM_AGREEMENT
        records.all { it.decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION } ->
            HimZeroCandidateRecoveryHumanReviewResolutionStateV1.INDEPENDENT_REJECT_AGREEMENT
        else -> HimZeroCandidateRecoveryHumanReviewResolutionStateV1.INDEPENDENT_DISAGREEMENT
    }

    private fun deriveCounters(
        corpus: HimZeroCandidateRecoveryReviewCorpusReportV1,
        authorizedUnits: Int,
        prior: List<HimZeroCandidateRecoveryHumanReviewDecisionRecordV1>,
        submitted: List<HimZeroCandidateRecoveryHumanReviewDecisionRecordV1>,
        resolutions: List<HimZeroCandidateRecoveryHumanReviewResolutionV1>,
    ): HimZeroCandidateRecoveryHumanReviewRuntimeCountersV1 =
        HimZeroCandidateRecoveryHumanReviewRuntimeCountersV1(
            corpusEntries = corpus.entries.size,
            resolvedReviewUnits = corpus.entries.flatMap { entry -> entry.auditLinkedCanonicalTargets }.size,
            authorizedReviewUnits = authorizedUnits,
            priorDecisionRecords = prior.size,
            submittedDecisionRecords = submitted.size,
            uniqueSubmittedReviewUnits = submitted.map { it.reviewUnit.reviewUnitId }.distinct().size,
            uniqueSubmittedReviewers = submitted.map { it.reviewerRef }.distinct().size,
            submittedDecisionBreakdown = HimZeroCandidateRecoveryHumanReviewDecisionV1.entries.map { decision ->
                HimZeroCandidateRecoveryHumanReviewRuntimeDecisionCounterV1(decision, submitted.count { it.decision == decision })
            },
            resolutionStateBreakdown = HimZeroCandidateRecoveryHumanReviewResolutionStateV1.entries.map { state ->
                HimZeroCandidateRecoveryHumanReviewResolutionCounterV1(state, resolutions.count { it.resolutionState == state })
            },
            alternativeCanonicalProposals = submitted.count { it.alternativeCanonicalProposal != null },
            persistedDecisionRecords = 0,
        )

    private fun buildBatch(
        request: HimZeroCandidateRecoveryHumanReviewRuntimeRequestV1,
        submitted: List<HimZeroCandidateRecoveryHumanReviewDecisionRecordV1>,
    ): HimZeroCandidateRecoveryHumanReviewDecisionBatchV1 {
        val records = submitted.sortedWith(compareBy({ it.reviewUnit.reviewUnitId }, { it.reviewerRef }, { it.reviewRound }, { it.revision }))
        val unsigned = HimZeroCandidateRecoveryHumanReviewDecisionBatchV1(
            persistenceContractId = HimZeroCandidateRecoveryHumanReviewPersistenceV1.CONTRACT_ID,
            persistenceVersion = HimZeroCandidateRecoveryHumanReviewPersistenceV1.VERSION,
            batchId = request.batchId,
            inputBinding = request.inputBinding,
            decisionRecords = records,
            counters = HimZeroCandidateRecoveryHumanReviewPersistenceV1.deriveCounters(records),
            bindingDigest = HimZeroCandidateRecoveryHumanReviewPersistenceV1.bindingDigest(request.inputBinding),
            batchLogicalDigest = "",
        )
        return unsigned.copy(
            batchLogicalDigest = HimZeroCandidateRecoveryHumanReviewPersistenceV1.batchLogicalDigest(unsigned),
        )
    }

    private fun fail(reason: FailureReason, safeContext: String): Nothing = throw RuntimeFailure(reason, safeContext)

    private class RuntimeFailure(
        val reason: FailureReason,
        val safeContext: String,
    ) : IllegalArgumentException()

    private val SCOPE_ID = Regex("[a-z0-9][a-z0-9-]{0,63}")
}

data class HimZeroCandidateRecoveryHumanReviewScopeV1(
    val scopeId: String,
    val reviewRound: Int,
    val authorizedReviewUnitIds: List<String>,
    val maxNewDecisionRecords: Int,
    val inputBindingDigest: String,
    val scopeDigest: String,
)

data class HimZeroCandidateRecoveryHumanReviewResolutionV1(
    val reviewUnitId: String,
    val resolutionState: HimZeroCandidateRecoveryHumanReviewResolutionStateV1,
    val currentReviewerRefs: List<String>,
)

enum class HimZeroCandidateRecoveryHumanReviewResolutionStateV1 {
    SINGLE_REVIEW_ONLY,
    INDEPENDENT_CONFIRM_AGREEMENT,
    INDEPENDENT_REJECT_AGREEMENT,
    INDEPENDENT_DISAGREEMENT,
    ABSTENTION_PRESENT,
    ESCALATED_OUT_OF_SCOPE,
}

data class HimZeroCandidateRecoveryHumanReviewRuntimeDecisionCounterV1(
    val decision: HimZeroCandidateRecoveryHumanReviewDecisionV1,
    val records: Int,
)

data class HimZeroCandidateRecoveryHumanReviewResolutionCounterV1(
    val resolutionState: HimZeroCandidateRecoveryHumanReviewResolutionStateV1,
    val units: Int,
)

data class HimZeroCandidateRecoveryHumanReviewRuntimeCountersV1(
    val corpusEntries: Int,
    val resolvedReviewUnits: Int,
    val authorizedReviewUnits: Int,
    val priorDecisionRecords: Int,
    val submittedDecisionRecords: Int,
    val uniqueSubmittedReviewUnits: Int,
    val uniqueSubmittedReviewers: Int,
    val submittedDecisionBreakdown: List<HimZeroCandidateRecoveryHumanReviewRuntimeDecisionCounterV1>,
    val resolutionStateBreakdown: List<HimZeroCandidateRecoveryHumanReviewResolutionCounterV1>,
    val alternativeCanonicalProposals: Int,
    val persistedDecisionRecords: Int,
)

enum class HimZeroCandidateRecoveryHumanReviewRuntimeFailureReasonV1 {
    INVALID_REQUEST,
    INVALID_INPUT_BINDING,
    INVALID_CORPUS,
    INVALID_FOUNDATION,
    INPUT_BINDING_MISMATCH,
    INVALID_SCOPE,
    SCOPE_DIGEST_MISMATCH,
    UNKNOWN_AUTHORIZED_REVIEW_UNIT,
    UNAUTHORIZED_REVIEW_UNIT,
    DUPLICATE_REVIEW_UNIT,
    EMPTY_SUBMITTED_DECISIONS,
    SUBMISSION_LIMIT_EXCEEDED,
    INVALID_PRIOR_DECISION,
    INVALID_SUBMITTED_DECISION,
    REVIEW_UNIT_BINDING_MISMATCH,
    REVIEW_ROUND_MISMATCH,
    INVALID_REVISION_SEQUENCE,
    STALE_REVISION,
    DUPLICATE_REVIEWER_SUBMISSION,
    ALTERNATIVE_CANONICAL_NOT_FOUND,
    PERSISTENCE_FAILED,
    PERSISTENCE_RELOAD_MISMATCH,
    INTERNAL_INVARIANT_VIOLATION,
}

private typealias FailureReason = HimZeroCandidateRecoveryHumanReviewRuntimeFailureReasonV1

data class HimZeroCandidateRecoveryHumanReviewRuntimeRequestV1(
    val enabled: Boolean,
    val batchId: String,
    val inputBinding: HimZeroCandidateRecoveryHumanReviewInputBindingV1,
    val corpus: HimZeroCandidateRecoveryReviewCorpusReportV1,
    val catalog: HimProductOnlyCanonicalMaster,
    val registry: HimEntityIdRegistry,
    val authority: HimCanonicalFamilyAuthority,
    val reviewScope: HimZeroCandidateRecoveryHumanReviewScopeV1,
    val priorDecisionRecords: List<HimZeroCandidateRecoveryHumanReviewDecisionRecordV1>,
    val submittedDecisionRecords: List<HimZeroCandidateRecoveryHumanReviewDecisionRecordV1>,
    val durableDecisionBatchRoot: java.io.File,
    val derivedReportRoot: java.io.File,
)

sealed interface HimZeroCandidateRecoveryHumanReviewRuntimeResult {
    data object Disabled : HimZeroCandidateRecoveryHumanReviewRuntimeResult

    data class Completed(
        val runtimeContractId: String,
        val runtimeVersion: String,
        val scope: HimZeroCandidateRecoveryHumanReviewScopeV1,
        val authorizedReviewUnits: List<HimZeroCandidateRecoveryHumanReviewReviewUnitV1>,
        val submittedDecisionRecords: List<HimZeroCandidateRecoveryHumanReviewDecisionRecordV1>,
        val resolutions: List<HimZeroCandidateRecoveryHumanReviewResolutionV1>,
        val counters: HimZeroCandidateRecoveryHumanReviewRuntimeCountersV1,
        val decisionBatch: HimZeroCandidateRecoveryHumanReviewDecisionBatchV1,
        val persistenceResult: HimZeroCandidateRecoveryHumanReviewPersistenceResultV1.Completed,
    ) : HimZeroCandidateRecoveryHumanReviewRuntimeResult

    data class Failed(
        val reason: HimZeroCandidateRecoveryHumanReviewRuntimeFailureReasonV1,
        val safeContext: String,
    ) : HimZeroCandidateRecoveryHumanReviewRuntimeResult
}
