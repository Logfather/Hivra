package de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateConfidence
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateRelation
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimRetrievalTerminalState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimRetrievalRound
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticEvidenceOrigin
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticEvidenceRelation
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInformationGainJudgment
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticRetrievalDirective
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticUsage
import java.security.MessageDigest

object HimCandidateDatasetContractV2 {
    const val ROOT = "data/knowledge/him/candidates"
    const val MASTER_ROOT = "$ROOT/master"
    const val RUNS_ROOT = "$ROOT/runs"
    const val SCHEMA_VERSION = "HIM_CANDIDATE_DATASET_SCHEMA_V2"
    const val POLICY_VERSION = "HIM_CANDIDATE_DATASET_POLICY_V2"
    const val CANDIDATE_REFERENCE_CONTRACT = "HIM_CANDIDATE_REFERENCE_V1"
    const val RUN_CONTRACT = "HIM_CANDIDATE_GENERATION_RUN_V2"
    const val INPUT_RUN_CONTRACT = "HIM_CANDIDATE_GENERATION_INPUT_RUN_V1"
    const val LOGICAL_DIGEST_CONTRACT = "HIM_CANDIDATE_DATASET_LOGICAL_DIGEST_V2"
    const val CONTEXT_BUDGET_POLICY = "HIM_SEMANTIC_CONTEXT_BUDGET_POLICY_V1"
    const val PROVIDER = "OPENAI"
    const val MODEL = "gpt-5.6-sol"
    val PROVIDER_CONFIGURATION_FINGERPRINT =
        HimSha256("b01d790cb8ebd886024cef145d85786029fee684a032379776636d426d851579")
}

object HimCandidatePersistencePolicyV2 {
    fun persists(confidence: HimCandidateConfidence): Boolean =
        confidence == HimCandidateConfidence.HIGH || confidence == HimCandidateConfidence.MEDIUM

    fun novelCandidateAllowed(knownApprovedRelation: Boolean): Boolean = !knownApprovedRelation
}

data class HimCandidateRunReference(val value: String) { init { require(value.matches(Regex("run:v2:[0-9a-f]{64}"))) } }
data class HimCandidateInputRunReference(val value: String) { init { require(value.matches(Regex("input-run:v1:[0-9a-f]{64}"))) } }
data class HimCandidateOccurrenceReference(val value: String) { init { require(value.matches(Regex("occurrence:v1:[0-9a-f]{64}"))) } }

data class HimCandidateInputProvenance(
    val rawInput: String,
    val normalizedLookup: String,
) { init { require(rawInput.isNotBlank() && normalizedLookup.isNotBlank()) } }

data class HimCandidateCanonicalContext(
    val rank: Int,
    val canonicalId: HimEntityId,
    val canonicalName: String,
    val fullRecordCanonicalJson: String?,
) {
    init {
        require(rank in 1..10 && canonicalName.isNotBlank())
        require(fullRecordCanonicalJson == null || rank <= 3)
    }
}

data class HimCandidateEvidenceProvenance(
    val reference: HimEvidenceReference,
    val recordKind: String,
    val retrievalRank: Int,
    val includedInProviderContext: Boolean,
    val omittedDueToContextBudget: Boolean,
    val himAssignedRelation: HimSemanticEvidenceRelation?,
) {
    init {
        require(recordKind.isNotBlank() && retrievalRank in 1..10)
        require(!(includedInProviderContext && omittedDueToContextBudget))
    }
}

data class HimCandidateInferenceProvenance(
    val provider: String,
    val model: String,
    val providerConfigurationFingerprint: HimSha256,
    val inferenceSchema: String,
    val instructionPolicy: String,
    val contextBudgetPolicy: String,
    val retrievalFoundationRelease: String,
    val retrievalFoundationReleaseSha256: HimSha256,
    val retrievalFoundationDigest: HimSha256,
    val technicalAttemptCount: Int,
    val usage: HimSemanticUsage?,
) {
    init {
        require(provider.isNotBlank())
        require(model.isNotBlank())
        require(providerConfigurationFingerprint.value.isNotBlank())
        require(inferenceSchema.isNotBlank())
        require(instructionPolicy.isNotBlank())
        require(contextBudgetPolicy.isNotBlank())
        require(retrievalFoundationRelease.isNotBlank())
        require(technicalAttemptCount in 1..2)
    }
}

data class HimCandidateHypothesis(
    val candidateReference: HimCandidateReference,
    val candidateTerm: String,
    val normalizedCandidateTerm: String,
    val relation: HimCandidateRelation,
    val confidence: HimCandidateConfidence,
    val evidenceOrigin: HimSemanticEvidenceOrigin,
    val shortRationale: String,
) {
    init {
        require(candidateTerm.isNotBlank() && normalizedCandidateTerm.isNotBlank() && shortRationale.isNotBlank())
        require(confidence in setOf(HimCandidateConfidence.HIGH, HimCandidateConfidence.MEDIUM))
    }
}

data class HimCandidateRetrievalStepProvenance(
    val source: HimGroundTruthSource,
    val semanticQuery: String,
    val evidenceReferences: List<HimEvidenceReference>,
) {
    init {
        require(semanticQuery.isNotBlank())
        require(evidenceReferences.size <= 10)
        require(evidenceReferences.all { it.source == source.name })
    }
    val resultCount: Int get() = evidenceReferences.size
}

data class HimCandidateRetrievalRoundProvenance(
    val round: HimRetrievalRound,
    val directive: HimSemanticRetrievalDirective,
    val steps: List<HimCandidateRetrievalStepProvenance>,
    val includedEvidenceReferences: List<HimEvidenceReference>,
    val omittedDueToBudgetEvidenceReferences: List<HimEvidenceReference>,
) {
    init {
        require(round.value in 1..HimRetrievalRound.MAX_ROUNDS)
        val directivePairs = directive.sourceQueries.flatMap { item -> item.queries.map { item.source to it } }.toSet()
        require(steps.map { it.source to it.semanticQuery }.toSet() == directivePairs)
        val retrieved = steps.flatMap { it.evidenceReferences }.toSet()
        require(includedEvidenceReferences.all { it in retrieved })
        require(omittedDueToBudgetEvidenceReferences.all { it in retrieved })
        require(includedEvidenceReferences.intersect(omittedDueToBudgetEvidenceReferences.toSet()).isEmpty())
    }
}

enum class HimCandidateInputCompletionState { COMPLETED, FAILED_TECHNICAL }
enum class HimCandidateGenerationRunState { COMPLETE, FAILED }
enum class HimCandidateFinalInferenceOutcome {
    SUCCESS_WITH_PERSISTED_CANDIDATE,
    SUCCESS_WITH_NO_PERSISTED_CANDIDATE,
    TECHNICAL_FAILURE,
}

data class HimCandidateConfidenceDiagnostics(
    val high: Int,
    val medium: Int,
    val low: Int,
    val noConfidence: Int,
) { init { require(listOf(high, medium, low, noConfidence).all { it >= 0 }) } }

data class HimCandidateKnownRelationDiagnostic(
    val relationDescription: String,
    val evidenceReferences: List<HimEvidenceReference>,
) { init { require(relationDescription.isNotBlank()) } }

data class HimCandidateAuthorityConflictProvenance(
    val authorityEntityReference: String,
    val shortRationale: String,
    val evidenceReferences: List<HimEvidenceReference>,
) { init { require(authorityEntityReference.isNotBlank() && shortRationale.isNotBlank()) } }

data class HimCandidateTechnicalFailureProvenance(
    val failureKind: String,
    val safeMessage: String,
    val technicalAttemptCount: Int,
) {
    init {
        require(failureKind.isNotBlank() && safeMessage.isNotBlank())
        require(technicalAttemptCount in 1..2)
    }
}

data class HimCandidateGenerationInputRun(
    val inputRunReference: HimCandidateInputRunReference,
    val input: HimCandidateInputProvenance,
    val canonicalContext: List<HimCandidateCanonicalContext>,
    val retrievalHistory: List<HimCandidateRetrievalRoundProvenance>,
    val finalInformationGainJudgment: HimSemanticInformationGainJudgment?,
    val terminalState: HimRetrievalTerminalState?,
    val completionState: HimCandidateInputCompletionState,
    val finalOutcome: HimCandidateFinalInferenceOutcome,
    val confidenceDiagnostics: HimCandidateConfidenceDiagnostics,
    val knownRelations: List<HimCandidateKnownRelationDiagnostic>,
    val authorityConflicts: List<HimCandidateAuthorityConflictProvenance>,
    val persistedCandidateReferences: List<HimCandidateReference>,
    val inference: HimCandidateInferenceProvenance?,
    val technicalFailure: HimCandidateTechnicalFailureProvenance?,
) {
    init {
        require(canonicalContext.size <= 10)
        require(canonicalContext.map { it.rank } == (1..canonicalContext.size).toList())
        require(retrievalHistory.map { it.round.value } == (1..retrievalHistory.size).toList())
        require(persistedCandidateReferences.distinct().size == persistedCandidateReferences.size)
        when (completionState) {
            HimCandidateInputCompletionState.COMPLETED -> {
                require(inference != null && technicalFailure == null)
                require(finalOutcome != HimCandidateFinalInferenceOutcome.TECHNICAL_FAILURE)
            }
            HimCandidateInputCompletionState.FAILED_TECHNICAL -> {
                require(inference == null && technicalFailure != null)
                require(finalOutcome == HimCandidateFinalInferenceOutcome.TECHNICAL_FAILURE)
                require(persistedCandidateReferences.isEmpty())
            }
        }
        require((finalOutcome == HimCandidateFinalInferenceOutcome.SUCCESS_WITH_PERSISTED_CANDIDATE) == persistedCandidateReferences.isNotEmpty())
    }
}

data class HimCandidateOccurrence(
    val occurrenceReference: HimCandidateOccurrenceReference,
    val inputRunReference: HimCandidateInputRunReference,
    val candidate: HimCandidateHypothesis,
    val evidence: List<HimCandidateEvidenceProvenance>,
) {
    init {
        require(evidenceOriginValid(candidate.evidenceOrigin, evidence))
    }

    private fun evidenceOriginValid(origin: HimSemanticEvidenceOrigin, evidence: List<HimCandidateEvidenceProvenance>) = when (origin) {
        HimSemanticEvidenceOrigin.SOURCE_SUPPORTED -> evidence.any { it.includedInProviderContext }
        HimSemanticEvidenceOrigin.MODEL_DERIVED -> evidence.none { it.includedInProviderContext }
        HimSemanticEvidenceOrigin.MIXED -> evidence.any { it.includedInProviderContext }
    }
}

data class HimCandidateGenerationRun(
    val runReference: HimCandidateRunReference,
    val generationMissionReference: String,
    val runInputSetIdentity: HimSha256,
    val state: HimCandidateGenerationRunState,
    val inputRuns: List<HimCandidateGenerationInputRun>,
    val occurrences: List<HimCandidateOccurrence>,
) {
    init {
        require(generationMissionReference.isNotBlank())
        require(inputRuns.isNotEmpty())
        require(inputRuns.map { it.input.rawInput }.distinct().size == inputRuns.size)
        require(inputRuns.map { it.inputRunReference }.distinct().size == inputRuns.size)
        require((state == HimCandidateGenerationRunState.COMPLETE) == inputRuns.all { it.completionState == HimCandidateInputCompletionState.COMPLETED })
        val inputs = inputRuns.associateBy { it.inputRunReference }
        require(occurrences.all { occurrence ->
            val input = inputs[occurrence.inputRunReference]
            input != null && occurrence.candidate.candidateReference in input.persistedCandidateReferences
        })
    }
}

data class HimCandidateMasterRecord(
    val candidate: HimCandidateHypothesis,
    val occurrences: List<HimCandidateOccurrence>,
)

data class HimCandidateDataset(
    val schemaVersion: String = HimCandidateDatasetContractV2.SCHEMA_VERSION,
    val policyVersion: String = HimCandidateDatasetContractV2.POLICY_VERSION,
    val runs: List<HimCandidateGenerationRun> = emptyList(),
    val candidates: List<HimCandidateMasterRecord> = emptyList(),
) {
    init {
        require(schemaVersion == HimCandidateDatasetContractV2.SCHEMA_VERSION)
        require(policyVersion == HimCandidateDatasetContractV2.POLICY_VERSION)
    }
}

object HimCandidateIdentityV1 {
    fun candidate(normalizedCandidateTerm: String, relation: HimCandidateRelation): HimCandidateReference {
        require(normalizedCandidateTerm.isNotBlank())
        val canonical = buildString {
            appendLine("contract=${HimCandidateDatasetContractV2.CANDIDATE_REFERENCE_CONTRACT}")
            appendLine("candidate-type=${relation.candidateType.name}")
            appendLine("normalized-candidate-term=$normalizedCandidateTerm")
            when (relation) {
                is HimCandidateRelation.Identity -> appendLine("parent-canonical-id=${relation.parentCanonicalId.value}")
                is HimCandidateRelation.Variant -> appendEntity("scope", relation.scope)
                is HimCandidateRelation.Alias -> appendEntity("equivalent", relation.equivalentEntity)
                HimCandidateRelation.CreateNewCanonical -> appendLine("proposed-canonical=$normalizedCandidateTerm")
            }
        }
        return HimCandidateReference("candidate:v1:${sha256(canonical)}")
    }

    fun run(generationMissionReference: String, inputSetIdentity: HimSha256, inference: HimCandidateInferenceProvenance): HimCandidateRunReference {
        require(generationMissionReference.isNotBlank())
        val canonical = listOf(
            "contract=${HimCandidateDatasetContractV2.RUN_CONTRACT}",
            "generation-mission-reference=$generationMissionReference",
            "input-set=${inputSetIdentity.value}",
            "retrieval-release=${inference.retrievalFoundationRelease}",
            "retrieval-release-sha256=${inference.retrievalFoundationReleaseSha256.value}",
            "retrieval-digest=${inference.retrievalFoundationDigest.value}",
            "provider=${inference.provider}", "model=${inference.model}",
            "provider-configuration=${inference.providerConfigurationFingerprint.value}",
            "inference-schema=${inference.inferenceSchema}", "instruction-policy=${inference.instructionPolicy}",
            "context-budget-policy=${inference.contextBudgetPolicy}",
            "dataset-schema=${HimCandidateDatasetContractV2.SCHEMA_VERSION}",
            "dataset-policy=${HimCandidateDatasetContractV2.POLICY_VERSION}",
        ).joinToString("\n", postfix = "\n")
        return HimCandidateRunReference("run:v2:${sha256(canonical)}")
    }

    fun inputRun(run: HimCandidateRunReference, input: HimCandidateInputProvenance): HimCandidateInputRunReference {
        val canonical = listOf(
            "contract=${HimCandidateDatasetContractV2.INPUT_RUN_CONTRACT}",
            "run=${run.value}",
            "raw-input=${input.rawInput}",
            "normalized-lookup=${input.normalizedLookup}",
        ).joinToString("\n", postfix = "\n")
        return HimCandidateInputRunReference("input-run:v1:${sha256(canonical)}")
    }

    fun occurrence(candidate: HimCandidateReference, run: HimCandidateRunReference, inputRun: HimCandidateInputRunReference, evidence: List<HimCandidateEvidenceProvenance>): HimCandidateOccurrenceReference {
        val canonical = buildString {
            appendLine("candidate=${candidate.value}"); appendLine("run=${run.value}")
            appendLine("input-run=${inputRun.value}")
            evidence.sortedBy { evidenceKey(it) }.forEach { appendLine("evidence=${evidenceKey(it)}") }
        }
        return HimCandidateOccurrenceReference("occurrence:v1:${sha256(canonical)}")
    }

    fun datasetDigest(dataset: HimCandidateDataset): HimSha256 {
        val semantic = dataset.copy(runs = dataset.runs.map { run ->
            run.copy(inputRuns = run.inputRuns.map { input -> input.copy(inference = input.inference?.copy(usage = null)) })
        })
        return HimSha256(sha256(HimCandidateDatasetPersistenceV2.serialize(semantic).toString(Charsets.UTF_8)))
    }

    private fun StringBuilder.appendEntity(prefix: String, reference: HimFamilyEntityReference) {
        appendLine("$prefix-canonical-id=${reference.canonicalId.value}")
        appendLine("$prefix-identity-id=${(reference as? HimFamilyEntityReference.Identity)?.identityId?.value.orEmpty()}")
    }

    internal fun evidenceKey(value: HimCandidateEvidenceProvenance) = listOf(
        value.reference.source, value.reference.sourceArtifactSha256.value, value.reference.sourceRecordIdentity,
        value.recordKind, value.retrievalRank.toString(), value.includedInProviderContext.toString(),
        value.omittedDueToContextBudget.toString(), value.himAssignedRelation?.name.orEmpty(),
    ).joinToString("|")

    internal fun sha256(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
