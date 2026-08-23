package de.shopme.tools.knowledge.him.training.corpus

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimMutationReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateInputRunReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateRunReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationDecisionReference
import java.security.MessageDigest

/** Stable identifiers and policy names for the semantic training-example contract. */
object HimTrainingCorpusContractV1 {
    const val VERSION = "HIM_TRAINING_EXAMPLE_V1"
    const val EXAMPLE_REFERENCE_CONTRACT = "HIM_TRAINING_EXAMPLE_REFERENCE_V1"
    const val INPUT_CONTRACT = "HIM_TRAINING_INPUT_V1"
    const val TARGET_CONTRACT = "HIM_TRAINING_TARGET_V1"
    const val PROVENANCE_CONTRACT = "HIM_TRAINING_PROVENANCE_V1"
    const val LEAKAGE_AUDIT_CONTRACT = "HIM_TRAINING_INPUT_LEAKAGE_AUDIT_V1"
}

enum class HimTrainingTaskTypeV1 {
    FOOD_IDENTITY_CLASSIFICATION,
}

enum class HimTrainingClassificationV1 {
    EXISTING_CANONICAL,
    IDENTITY,
    VARIANT,
    ALIAS,
    NEW_CANONICAL,
}

data class HimTrainingExampleReference(
    val value: String,
) {
    init {
        require(value.matches(Regex("example:v1:[0-9a-f]{64}"))) {
            "Invalid HIM training example reference: $value"
        }
    }
}

/** Evidence available to the model. It deliberately carries no HIM-assigned relation. */
data class HimTrainingEvidenceInputV1(
    val reference: de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference,
    val recordKind: String,
    val retrievalRank: Int,
) {
    init {
        require(recordKind.isNotBlank())
        require('\n' !in recordKind && '\r' !in recordKind)
        require(retrievalRank in 1..10)
        require(HimGroundTruthSource.entries.any { it.name == reference.source }) {
            "Training evidence must bind to one of the four HIM sources."
        }
        require('\n' !in reference.sourceRecordIdentity && '\r' !in reference.sourceRecordIdentity)
    }

    internal fun canonicalKey(): String =
        listOf(
            reference.source,
            reference.sourceArtifactSha256.value,
            reference.sourceRecordIdentity,
            recordKind,
            retrievalRank.toString(),
        ).joinToString("\u0000")
}

/** Model-facing input only. No target, validation, promotion, mutation, or release fields exist here. */
data class HimTrainingInputV1(
    val observedTerm: String,
    val normalizedObservedTerm: String,
    val canonicalContext: List<HimCandidateCanonicalContext> = emptyList(),
    val evidence: List<HimTrainingEvidenceInputV1> = emptyList(),
) {
    init {
        require(observedTerm.isNotBlank())
        require(normalizedObservedTerm.isNotBlank())
        require('\n' !in observedTerm && '\r' !in observedTerm)
        require('\n' !in normalizedObservedTerm && '\r' !in normalizedObservedTerm)
        require(canonicalContext.size <= 10)
        require(canonicalContext.map { it.rank } == (1..canonicalContext.size).toList()) {
            "Canonical context must preserve its retrieval rank order."
        }
        require(canonicalContext.map { it.canonicalId }.distinct().size == canonicalContext.size)
        require(canonicalContext.all { it.fullRecordCanonicalJson == null || it.fullRecordCanonicalJson.isNotBlank() })
        require(evidence.map { it.reference }.distinct().size == evidence.size) {
            "Duplicate evidence references are not allowed in training input."
        }
        require(evidence.groupBy { it.reference.source }.values.all { it.size <= 10 })
    }
}

/** Semantic answer only; the sealed shape prevents operational IDs from being required by NEW_CANONICAL. */
sealed interface HimTrainingTargetV1 {
    val classification: HimTrainingClassificationV1

    data class ExistingCanonical(
        val canonicalId: HimEntityId,
    ) : HimTrainingTargetV1 {
        override val classification = HimTrainingClassificationV1.EXISTING_CANONICAL
    }

    data class Identity(
        val parentCanonicalId: HimEntityId,
    ) : HimTrainingTargetV1 {
        override val classification = HimTrainingClassificationV1.IDENTITY
    }

    data class Variant(
        val scope: HimFamilyEntityReference,
    ) : HimTrainingTargetV1 {
        override val classification = HimTrainingClassificationV1.VARIANT
    }

    data class Alias(
        val equivalentEntity: HimFamilyEntityReference,
    ) : HimTrainingTargetV1 {
        override val classification = HimTrainingClassificationV1.ALIAS
    }

    data class NewCanonical(
        val proposedCanonicalName: String? = null,
    ) : HimTrainingTargetV1 {
        override val classification = HimTrainingClassificationV1.NEW_CANONICAL

        init {
            require(proposedCanonicalName == null || proposedCanonicalName.isNotBlank())
        }
    }
}

data class HimTrainingTeacherProvenanceV1(
    val provider: String,
    val model: String,
    val configurationFingerprint: HimSha256? = null,
) {
    init {
        require(provider.isNotBlank() && model.isNotBlank())
    }
}

/** Audit-only lineage. This type is never reachable through [HimTrainingInputV1]. */
data class HimTrainingProvenanceV1(
    val candidateReference: HimCandidateReference? = null,
    val generationRunReference: HimCandidateRunReference? = null,
    val inputRunReference: HimCandidateInputRunReference? = null,
    val validationReference: HimCandidateValidationDecisionReference? = null,
    val promotionReference: HimCandidatePromotionReference? = null,
    val mutationReference: HimMutationReference? = null,
    val groundTruthReleaseReference: HimGroundTruthReleaseIdentityV1? = null,
    val promotedEntityId: HimEntityId? = null,
    val promotedEntityType: HimEntityType? = null,
    val sourceEvidenceReferences: List<de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference> = emptyList(),
    val sourceArtifactDigests: List<HimSha256> = emptyList(),
    val retrievalFoundationRelease: String? = null,
    val retrievalFoundationReleaseSha256: HimSha256? = null,
    val retrievalFoundationDigest: HimSha256? = null,
    val teacher: HimTrainingTeacherProvenanceV1? = null,
) {
    init {
        require(sourceEvidenceReferences.distinct().size == sourceEvidenceReferences.size)
        require(sourceArtifactDigests.distinct().size == sourceArtifactDigests.size)
        require(sourceEvidenceReferences.all { reference ->
            HimGroundTruthSource.entries.any { it.name == reference.source }
        })
        require(inputRunReference == null || generationRunReference != null)
        require(validationReference == null || candidateReference != null)
        require(promotionReference == null || (candidateReference != null && validationReference != null))
        require(mutationReference == null || promotionReference != null)
        require(promotedEntityId == null || (mutationReference != null && groundTruthReleaseReference != null))
        require(promotedEntityType == null || promotedEntityId != null)
        require(retrievalFoundationReleaseSha256 == null || retrievalFoundationRelease != null)
        require(retrievalFoundationDigest == null || retrievalFoundationRelease != null)
    }
}

data class HimTrainingExampleV1(
    val exampleReference: HimTrainingExampleReference,
    val contractVersion: String = HimTrainingCorpusContractV1.VERSION,
    val taskType: HimTrainingTaskTypeV1,
    val input: HimTrainingInputV1,
    val target: HimTrainingTargetV1,
    val provenance: HimTrainingProvenanceV1 = HimTrainingProvenanceV1(),
) {
    init {
        require(contractVersion == HimTrainingCorpusContractV1.VERSION)
        HimTrainingExampleValidatorV1.validate(this)
    }

    /** Explicit model boundary: callers can obtain input, but not target or provenance, through this view. */
    fun modelInput(): HimTrainingInputV1 = input

    companion object {
        fun create(
            taskType: HimTrainingTaskTypeV1,
            input: HimTrainingInputV1,
            target: HimTrainingTargetV1,
            provenance: HimTrainingProvenanceV1 = HimTrainingProvenanceV1(),
        ): HimTrainingExampleV1 {
            val reference = HimTrainingExampleIdentityV1.example(
                taskType = taskType,
                input = input,
                target = target,
                provenance = provenance,
            )
            return HimTrainingExampleV1(reference, HimTrainingCorpusContractV1.VERSION, taskType, input, target, provenance)
        }
    }
}

object HimTrainingExampleIdentityV1 {
    fun example(
        taskType: HimTrainingTaskTypeV1,
        input: HimTrainingInputV1,
        target: HimTrainingTargetV1,
        provenance: HimTrainingProvenanceV1,
    ): HimTrainingExampleReference {
        val canonical = buildString {
            line("contract", HimTrainingCorpusContractV1.EXAMPLE_REFERENCE_CONTRACT)
            line("task", taskType.name)
            line("observed", input.observedTerm)
            line("normalized", input.normalizedObservedTerm)
            input.canonicalContext.forEachIndexed { index, context ->
                line("canonical-context-$index-rank", context.rank.toString())
                line("canonical-context-$index-id", context.canonicalId.value)
                line("canonical-context-$index-name", context.canonicalName)
                line("canonical-context-$index-record", context.fullRecordCanonicalJson.orEmpty())
            }
            input.evidence.sortedBy { it.canonicalKey() }.forEachIndexed { index, evidence ->
                line("evidence-$index", evidence.canonicalKey())
            }
            appendTarget(this, target)
            appendProvenance(this, provenance)
        }
        return HimTrainingExampleReference("example:v1:${sha256(canonical)}")
    }

    private fun appendTarget(builder: StringBuilder, target: HimTrainingTargetV1) {
        builder.line("target-classification", target.classification.name)
        when (target) {
            is HimTrainingTargetV1.ExistingCanonical -> builder.line("target-canonical-id", target.canonicalId.value)
            is HimTrainingTargetV1.Identity -> builder.line("target-parent-canonical-id", target.parentCanonicalId.value)
            is HimTrainingTargetV1.Variant -> builder.appendEntity("target-scope", target.scope)
            is HimTrainingTargetV1.Alias -> builder.appendEntity("target-equivalent", target.equivalentEntity)
            is HimTrainingTargetV1.NewCanonical -> builder.line("target-proposed-canonical", target.proposedCanonicalName.orEmpty())
        }
    }

    private fun appendProvenance(builder: StringBuilder, provenance: HimTrainingProvenanceV1) {
        builder.line("candidate", provenance.candidateReference?.value.orEmpty())
        builder.line("generation-run", provenance.generationRunReference?.value.orEmpty())
        builder.line("input-run", provenance.inputRunReference?.value.orEmpty())
        builder.line("validation", provenance.validationReference?.value.orEmpty())
        builder.line("promotion", provenance.promotionReference?.value.orEmpty())
        builder.line("mutation", provenance.mutationReference?.value.orEmpty())
        builder.line("ground-truth-release", provenance.groundTruthReleaseReference?.value.orEmpty())
        builder.line("promoted-entity-id", provenance.promotedEntityId?.value.orEmpty())
        builder.line("promoted-entity-type", provenance.promotedEntityType?.name.orEmpty())
        provenance.sourceEvidenceReferences.sortedBy { evidenceKey(it) }.forEachIndexed { index, reference ->
            builder.line("provenance-evidence-$index", evidenceKey(reference))
        }
        provenance.sourceArtifactDigests.sortedBy { it.value }.forEachIndexed { index, digest ->
            builder.line("source-artifact-digest-$index", digest.value)
        }
        builder.line("retrieval-foundation-release", provenance.retrievalFoundationRelease.orEmpty())
        builder.line("retrieval-foundation-release-sha256", provenance.retrievalFoundationReleaseSha256?.value.orEmpty())
        builder.line("retrieval-foundation-digest", provenance.retrievalFoundationDigest?.value.orEmpty())
        builder.line("teacher-provider", provenance.teacher?.provider.orEmpty())
        builder.line("teacher-model", provenance.teacher?.model.orEmpty())
        builder.line("teacher-configuration", provenance.teacher?.configurationFingerprint?.value.orEmpty())
    }

    private fun StringBuilder.appendEntity(prefix: String, reference: HimFamilyEntityReference) {
        line("$prefix-canonical-id", reference.canonicalId.value)
        line("$prefix-identity-id", (reference as? HimFamilyEntityReference.Identity)?.identityId?.value.orEmpty())
    }

    private fun evidenceKey(reference: de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference) =
        listOf(reference.source, reference.sourceArtifactSha256.value, reference.sourceRecordIdentity).joinToString("\u0000")

    private fun StringBuilder.line(key: String, value: String) {
        append(key).append('=').append(value.length).append(':').append(value).append('\n')
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
