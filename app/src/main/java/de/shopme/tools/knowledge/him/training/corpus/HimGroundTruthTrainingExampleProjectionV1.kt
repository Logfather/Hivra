package de.shopme.tools.knowledge.him.training.corpus

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistry
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateRelation
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCanonicalFamilyMutationLedgerEntry
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthMutationType
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimMutationReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateGenerationInputRun
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateGenerationRun
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateHypothesis
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateOccurrence
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateEvidenceProvenance
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionProposalV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionTargetV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionType
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimDeterministicPromotionExecutionResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationRecord
import java.security.MessageDigest

sealed interface HimGroundTruthTrainingExampleProjectionResultV1 {
    data class Projected(
        val example: HimTrainingExampleV1,
    ) : HimGroundTruthTrainingExampleProjectionResultV1

    data class NotYetProjectable(
        val reason: String,
    ) : HimGroundTruthTrainingExampleProjectionResultV1 {
        init {
            require(reason.isNotBlank())
        }
    }
}

/**
 * Already-resolved historical lineage for exactly one candidate occurrence.
 * No filesystem lookup or current-release resolution is performed by the projector.
 */
data class HimGroundTruthTrainingExampleProjectionInputV1(
    val candidate: HimCandidateHypothesis,
    val occurrence: HimCandidateOccurrence,
    val generationRun: HimCandidateGenerationRun,
    val candidateDatasetDigest: HimSha256,
    val validation: HimCandidateValidationRecord,
    val promotion: HimCandidatePromotionProposalV1,
    val mutation: HimCanonicalFamilyMutationLedgerEntry?,
    val promotionExecution: HimDeterministicPromotionExecutionResultV1?,
    val resultingAuthority: HimCanonicalFamilyAuthority?,
    val resultingEntityIdRegistry: HimEntityIdRegistry?,
    val groundTruthReleaseReference: HimGroundTruthReleaseIdentityV1,
)

class HimGroundTruthTrainingExampleProjectionV1(
    private val familyPersistence: HimCanonicalFamilyPersistence = HimCanonicalFamilyPersistence(),
) {
    fun project(
        input: HimGroundTruthTrainingExampleProjectionInputV1,
    ): HimGroundTruthTrainingExampleProjectionResultV1 {
        validateCandidateLineage(input)

        if (input.promotion.target is HimCandidatePromotionTargetV1.CreateCanonical) {
            return HimGroundTruthTrainingExampleProjectionResultV1.NotYetProjectable(
                "CREATE_CANONICAL has no validated F3.7 child mutation lineage.",
            )
        }

        validatePromotedChildLineage(input)
        val inputRun = input.generationRun.inputRuns.single { it.inputRunReference == input.occurrence.inputRunReference }
        val evidence = input.occurrence.evidence.map(::projectEvidence)
        val target = projectTarget(input.promotion.target)
        val provenance = projectProvenance(input, inputRun, evidence)
        val example = HimTrainingExampleV1.create(
            taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
            input = HimTrainingInputV1(
                observedTerm = inputRun.input.rawInput,
                normalizedObservedTerm = inputRun.input.normalizedLookup,
                canonicalContext = inputRun.canonicalContext,
                evidence = evidence,
            ),
            target = target,
            provenance = provenance,
        )
        HimTrainingExampleValidatorV1.validate(example)
        return HimGroundTruthTrainingExampleProjectionResultV1.Projected(example)
    }

    fun projectOrThrow(input: HimGroundTruthTrainingExampleProjectionInputV1): HimTrainingExampleV1 =
        when (val result = project(input)) {
            is HimGroundTruthTrainingExampleProjectionResultV1.Projected -> result.example
            is HimGroundTruthTrainingExampleProjectionResultV1.NotYetProjectable ->
                error("Ground-Truth lineage is not yet projectable: ${result.reason}")
        }

    private fun validateCandidateLineage(input: HimGroundTruthTrainingExampleProjectionInputV1) {
        val candidateReference = input.candidate.candidateReference
        require(input.occurrence.candidate == input.candidate) {
            "Candidate occurrence does not bind the supplied candidate hypothesis."
        }
        require(input.generationRun.occurrences.any { it == input.occurrence }) {
            "Candidate occurrence is not part of the supplied generation run."
        }
        val inputRun = input.generationRun.inputRuns.singleOrNull {
            it.inputRunReference == input.occurrence.inputRunReference
        }
        require(inputRun != null) {
            "Candidate occurrence input-run is not part of the supplied generation run."
        }
        require(candidateReference in inputRun.persistedCandidateReferences) {
            "Candidate is not persisted by its input run."
        }
        require(input.validation.candidateReference == candidateReference) {
            "Validation does not bind the candidate reference."
        }
        require(input.validation.candidateDatasetDigest == input.candidateDatasetDigest) {
            "Validation candidate-dataset digest mismatch."
        }
        require(input.promotion.candidateReference == candidateReference) {
            "Promotion does not bind the candidate reference."
        }
        require(input.promotion.candidateDatasetDigest == input.candidateDatasetDigest) {
            "Promotion candidate-dataset digest mismatch."
        }
        require(input.promotion.validationReference == input.validation.validationReference) {
            "Promotion does not bind the validation reference."
        }
        require(input.groundTruthReleaseReference.value.isNotBlank())
    }

    private fun validatePromotedChildLineage(input: HimGroundTruthTrainingExampleProjectionInputV1) {
        val mutation = requireNotNull(input.mutation) {
            "Promoted child projection requires a mutation ledger entry."
        }
        val execution = requireNotNull(input.promotionExecution) {
            "Promoted child projection requires a promotion execution result."
        }
        val authority = requireNotNull(input.resultingAuthority) {
            "Promoted child projection requires the resulting Ground-Truth Authority."
        }
        val registry = requireNotNull(input.resultingEntityIdRegistry) {
            "Promoted child projection requires the resulting Entity-ID Registry."
        }

        require(execution.newReleaseReference == input.groundTruthReleaseReference) {
            "Ground-Truth release lineage mismatch."
        }
        require(execution.mutationReference.value == mutation.mutationReference.value) {
            "Promotion execution and mutation references do not match."
        }
        require(mutation.validationReference.value == input.validation.validationReference.value) {
            "Mutation does not bind the validation reference."
        }
        require(execution.newEntityId == mutation.newEntityId) {
            "Promotion execution and mutation Entity IDs do not match."
        }
        require(execution.promotionType == input.promotion.target.promotionType) {
            "Promotion execution type does not match the promotion target."
        }
        require(execution.targetType == expectedEntityType(input.promotion.target)) {
            "Promotion execution Entity type does not match the promotion target."
        }
        require(mutation.mutationType == expectedMutationType(input.promotion.target)) {
            "Mutation type does not match the promotion target."
        }
        require(mutation.entityType == expectedEntityType(input.promotion.target)) {
            "Mutation Entity type does not match the promotion target."
        }
        require(mutation.parent == expectedParent(input.promotion.target)) {
            "Mutation parent does not match the promotion scope."
        }
        require(input.promotion.target.targetName() == input.candidate.candidateTerm) {
            "Promotion target name does not match the candidate semantic term."
        }

        val authorityBytes = familyPersistence.serialize(authority)
        val registryBytes = familyPersistence.serialize(registry)
        require(sha256(authorityBytes) == mutation.canonicalFamilyAuthoritySha256After) {
            "Resulting Authority does not match the mutation ledger digest."
        }
        require(sha256(registryBytes) == mutation.entityIdRegistrySha256After) {
            "Resulting Entity-ID Registry does not match the mutation ledger digest."
        }
        require(registry.entries.any {
            it.entityId == execution.newEntityId &&
                it.entityType == expectedEntityType(input.promotion.target) &&
                it.sourceReference == input.promotion.promotionReference.value
        }) {
            "Resulting Entity-ID Registry does not contain the promoted child lineage."
        }
        require(authorityContainsTarget(authority, input.promotion.target, execution.newEntityId, input.candidate.candidateTerm)) {
            "Resulting Authority does not contain the promoted semantic child."
        }
    }

    private fun projectTarget(target: HimCandidatePromotionTargetV1): HimTrainingTargetV1 =
        when (target) {
            is HimCandidatePromotionTargetV1.AddIdentity ->
                HimTrainingTargetV1.Identity(target.parentCanonicalId)
            is HimCandidatePromotionTargetV1.AddVariant ->
                HimTrainingTargetV1.Variant(target.scope)
            is HimCandidatePromotionTargetV1.AddAlias ->
                HimTrainingTargetV1.Alias(target.equivalentEntity)
            is HimCandidatePromotionTargetV1.CreateCanonical ->
                error("CREATE_CANONICAL is handled as not-yet-projectable before target projection.")
        }

    private fun projectEvidence(value: HimCandidateEvidenceProvenance): HimTrainingEvidenceInputV1 =
        HimTrainingEvidenceInputV1(
            reference = value.reference,
            recordKind = value.recordKind,
            retrievalRank = value.retrievalRank,
        )

    private fun projectProvenance(
        input: HimGroundTruthTrainingExampleProjectionInputV1,
        inputRun: HimCandidateGenerationInputRun,
        evidence: List<HimTrainingEvidenceInputV1>,
    ): HimTrainingProvenanceV1 {
        val inference = requireNotNull(inputRun.inference) {
            "Completed candidate input run must retain inference provenance."
        }
        return HimTrainingProvenanceV1(
            candidateReference = input.candidate.candidateReference,
            generationRunReference = input.generationRun.runReference,
            inputRunReference = inputRun.inputRunReference,
            validationReference = input.validation.validationReference,
            promotionReference = input.promotion.promotionReference,
            mutationReference = requireNotNull(input.mutation).mutationReference,
            groundTruthReleaseReference = input.groundTruthReleaseReference,
            promotedEntityId = requireNotNull(input.promotionExecution).newEntityId,
            promotedEntityType = requireNotNull(input.promotionExecution).targetType,
            sourceEvidenceReferences = evidence.map { it.reference },
            sourceArtifactDigests = evidence.map { it.reference.sourceArtifactSha256 }.distinct().sortedBy { it.value },
            retrievalFoundationRelease = inference.retrievalFoundationRelease,
            retrievalFoundationReleaseSha256 = inference.retrievalFoundationReleaseSha256,
            retrievalFoundationDigest = inference.retrievalFoundationDigest,
            teacher = HimTrainingTeacherProvenanceV1(
                provider = inference.provider,
                model = inference.model,
                configurationFingerprint = inference.providerConfigurationFingerprint,
            ),
        )
    }

    private fun expectedEntityType(target: HimCandidatePromotionTargetV1): HimEntityType =
        when (target) {
            is HimCandidatePromotionTargetV1.AddIdentity -> HimEntityType.IDENTITY
            is HimCandidatePromotionTargetV1.AddVariant -> HimEntityType.VARIANT
            is HimCandidatePromotionTargetV1.AddAlias -> HimEntityType.ALIAS
            is HimCandidatePromotionTargetV1.CreateCanonical -> HimEntityType.CANONICAL
        }

    private fun expectedMutationType(target: HimCandidatePromotionTargetV1): HimGroundTruthMutationType =
        when (target) {
            is HimCandidatePromotionTargetV1.AddIdentity -> HimGroundTruthMutationType.ADD_IDENTITY
            is HimCandidatePromotionTargetV1.AddVariant -> HimGroundTruthMutationType.ADD_VARIANT
            is HimCandidatePromotionTargetV1.AddAlias -> HimGroundTruthMutationType.ADD_ALIAS
            is HimCandidatePromotionTargetV1.CreateCanonical -> error("CREATE_CANONICAL has no child mutation type.")
        }

    private fun expectedParent(target: HimCandidatePromotionTargetV1): HimFamilyEntityReference =
        when (target) {
            is HimCandidatePromotionTargetV1.AddIdentity -> HimFamilyEntityReference.Canonical(target.parentCanonicalId)
            is HimCandidatePromotionTargetV1.AddVariant -> target.scope
            is HimCandidatePromotionTargetV1.AddAlias -> target.equivalentEntity
            is HimCandidatePromotionTargetV1.CreateCanonical -> error("CREATE_CANONICAL has no child parent.")
        }

    private fun HimCandidatePromotionTargetV1.targetName(): String =
        when (this) {
            is HimCandidatePromotionTargetV1.AddIdentity -> identityName
            is HimCandidatePromotionTargetV1.AddVariant -> variantName
            is HimCandidatePromotionTargetV1.AddAlias -> aliasName
            is HimCandidatePromotionTargetV1.CreateCanonical -> canonicalName
        }

    private fun authorityContainsTarget(
        authority: HimCanonicalFamilyAuthority,
        target: HimCandidatePromotionTargetV1,
        entityId: HimEntityId,
        name: String,
    ): Boolean = when (target) {
        is HimCandidatePromotionTargetV1.AddIdentity ->
            authority.families.firstOrNull { it.canonicalId == target.parentCanonicalId }
                ?.identities?.any { it.identityId == entityId && it.identityName == name } == true
        is HimCandidatePromotionTargetV1.AddVariant ->
            when (val scope = target.scope) {
                is HimFamilyEntityReference.Canonical ->
                    authority.families.firstOrNull { it.canonicalId == scope.canonicalId }
                        ?.variants?.any { it.variantId == entityId && it.variantName == name } == true
                is HimFamilyEntityReference.Identity ->
                    authority.families.firstOrNull { it.canonicalId == scope.canonicalId }
                        ?.identities?.firstOrNull { it.identityId == scope.identityId }
                        ?.variants?.any { it.variantId == entityId && it.variantName == name } == true
            }
        is HimCandidatePromotionTargetV1.AddAlias ->
            when (val scope = target.equivalentEntity) {
                is HimFamilyEntityReference.Canonical ->
                    authority.families.firstOrNull { it.canonicalId == scope.canonicalId }
                        ?.aliases?.any { it.aliasId == entityId && it.aliasName == name } == true
                is HimFamilyEntityReference.Identity ->
                    authority.families.firstOrNull { it.canonicalId == scope.canonicalId }
                        ?.identities?.firstOrNull { it.identityId == scope.identityId }
                        ?.aliases?.any { it.aliasId == entityId && it.aliasName == name } == true
            }
        is HimCandidatePromotionTargetV1.CreateCanonical -> false
    }

    private fun sha256(bytes: ByteArray): HimSha256 =
        HimSha256(
            MessageDigest.getInstance("SHA-256")
                .digest(bytes)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) },
        )
}
