package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingFamilyGroupReferenceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingFamilyGroupResolverV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingProvenanceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTaskTypeV1
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Point-10 materialization boundary for explicitly human-confirmed V2
 * semantics. AI proposals are retained only as lineage; they are never the
 * source of training authority. Claims, pairs, conditioning, payloads and
 * partition buckets are intentionally outside this boundary.
 */
object HimP1HumanAuthorizedTrainingFamilyMaterializationV2 {
    const val CONTRACT_ID = "HIM_P1_HUMAN_AUTHORIZED_TRAINING_FAMILY_MATERIALIZATION_V2"
    const val VERSION = "2"
    const val STATE = "HUMAN_AUTHORIZED_TRAINING_FAMILY_MATERIALIZED_BEFORE_PARTITION"
    const val BATCH_ID = "p1-human-validation-expansion-v2-batch-1-confirmation-v1"
    const val AUTHORITY_TYPE = "HUMAN_CONFIRMED_AI_ENRICHMENT"
    const val HUMAN_REVIEWER = "human-reviewer:christian-glatschke:v1"
    const val TRAINING_EXAMPLE_COUNT = 34
    const val FAMILY_GROUP_COUNT = 13

    private const val AUTHORITY_DIGEST_DOMAIN = "HIM_P1_HUMAN_AUTHORIZED_V2_AUTHORITY_DIGEST"
    private const val EXAMPLES_DIGEST_DOMAIN = "HIM_P1_HUMAN_AUTHORIZED_V2_TRAINING_EXAMPLES_DIGEST"
    private const val FAMILIES_DIGEST_DOMAIN = "HIM_P1_HUMAN_AUTHORIZED_V2_FAMILIES_DIGEST"
    private const val INVENTORY_DIGEST_DOMAIN = "HIM_P1_HUMAN_AUTHORIZED_V2_EXPANSION_INVENTORY_DIGEST"
    private val SHA256 = Regex("[0-9a-f]{64}")
    private val gson = GsonBuilder().disableHtmlEscaping().create()

    data class AuthorizedCandidate(
        val candidateId: HimEntityId,
        val candidateName: String,
        val stableEntryId: String,
        val reviewUnitId: String,
        val primaryDecisionIdentity: String,
        val confirmationReference: String,
        val humanReviewerReference: String,
        val humanRationale: String,
        val aiProposalLogicalDigest: HimSha256?,
        val records: List<AuthorizedRecord>,
    ) {
        init {
            require(candidateName.isNotBlank())
            require(stableEntryId.matches(SHA256))
            require(reviewUnitId.matches(SHA256))
            require(primaryDecisionIdentity.isNotBlank())
            require(confirmationReference.isNotBlank())
            require(humanReviewerReference == HUMAN_REVIEWER)
            require(humanRationale.isNotBlank())
            require(records.isNotEmpty())
            require(records.map { it.evidenceRecordId }.distinct().size == records.size)
        }
    }

    data class AuthorizedRecord(
        val evidenceRecordId: String,
        val evidenceReference: HimEvidenceReference,
        val evidenceReferenceId: String,
        val recordKind: String,
        val retrievalRank: Int,
        val observedTerm: String,
        val normalizedObservedTerm: String,
        val relationKind: SemanticRelationKindV2,
        val semanticLabel: String? = null,
        val variantLabel: String? = null,
        val processingForm: String? = null,
        val preparationState: String? = null,
        val productForm: String? = null,
    ) {
        init {
            require(evidenceRecordId.isNotBlank())
            require(evidenceReferenceId.matches(SHA256))
            require(recordKind.isNotBlank())
            require(retrievalRank in 1..10)
            require(observedTerm.isNotBlank() && normalizedObservedTerm.isNotBlank())
            require(relationKind != SemanticRelationKindV2.COMPONENT_OR_DERIVED_PRODUCT_OF)
            require(relationKind != SemanticRelationKindV2.UNRESOLVED_MIXED_RELATION)
            require(relationKind != SemanticRelationKindV2.ALIAS_OF)
            require(relationKind != SemanticRelationKindV2.NEW_CANONICAL)
        }
    }

    data class MaterializedExample(
        val candidateId: HimEntityId,
        val candidateName: String,
        val confirmationReference: String,
        val humanReviewerReference: String,
        val primaryDecisionIdentity: String,
        val relationKind: SemanticRelationKindV2,
        val evidenceRecordId: String,
        val evidenceReferenceId: String,
        val example: HimTrainingExampleV1,
        val familyGroupReference: HimTrainingFamilyGroupReferenceV1,
        val semanticLabel: String?,
        val variantLabel: String?,
        val processingForm: String?,
        val preparationState: String?,
        val productForm: String?,
    )

    data class FamilyGroup(
        val familyGroupReference: HimTrainingFamilyGroupReferenceV1,
        val candidateId: HimEntityId,
        val candidateName: String,
        val members: List<String>,
        val relationDistribution: Map<String, Int>,
        val humanAuthorityReferences: List<String>,
    )

    data class Materialization(
        val candidates: List<AuthorizedCandidate>,
        val examples: List<MaterializedExample>,
        val families: List<FamilyGroup>,
        val rawMaterializationCandidateCount: Int,
        val duplicateCandidateCount: Int,
        val excludedCandidateIds: List<String>,
    ) {
        val finalUniqueTrainingExampleCount: Int get() = examples.size
        val newPositiveTrainingExampleCount: Int get() = examples.size
        val newNegativeTrainingExampleCount: Int get() = 0
        val newTotalTrainingExampleCount: Int get() = examples.size
    }

    data class AuthorityRecordDto(
        val candidateId: String,
        val candidateName: String,
        val stableEntryId: String,
        val reviewUnitId: String,
        val primaryDecisionIdentity: String,
        val confirmationReference: String,
        val humanReviewerReference: String,
        val humanRationale: String,
        val aiProposalLogicalDigest: String?,
        val records: List<AuthorityRelationDto>,
    )

    data class AuthorityRelationDto(
        val evidenceRecordId: String,
        val evidenceReferenceId: String,
        val relation: String,
        val targetCanonicalId: String,
        val semanticLabel: String?,
        val variantLabel: String?,
        val processingForm: String?,
        val preparationState: String?,
        val productForm: String?,
    )

    data class AuthorityArtifact(
        val contractId: String,
        val version: String,
        val state: String,
        val batchId: String,
        val authorityType: String,
        val records: List<AuthorityRecordDto>,
        val humanAuthorizedCandidateCount: Int,
        val recordWiseRelationCount: Int,
        val logicalDigest: String,
    )

    data class TrainingExampleDto(
        val exampleReference: String,
        val candidateId: String,
        val candidateName: String,
        val observedTerm: String,
        val normalizedObservedTerm: String,
        val classification: String,
        val targetCanonicalId: String,
        val relation: String,
        val semanticLabel: String?,
        val variantLabel: String?,
        val processingForm: String?,
        val preparationState: String?,
        val productForm: String?,
        val evidenceRecordId: String,
        val evidenceReferenceId: String,
        val source: String,
        val sourceArtifactSha256: String,
        val humanAuthorityReference: String,
        val primaryDecisionIdentity: String,
        val familyGroupReference: String,
    )

    data class TrainingExamplesArtifact(
        val contractId: String,
        val version: String,
        val state: String,
        val batchId: String,
        val examples: List<TrainingExampleDto>,
        val positiveCount: Int,
        val negativeCount: Int,
        val unresolvedCount: Int,
        val aiOnlyCount: Int,
        val logicalDigest: String,
    )

    data class FamilyGroupDto(
        val familyGroupReference: String,
        val candidateId: String,
        val candidateName: String,
        val memberCount: Int,
        val trainingExampleReferences: List<String>,
        val relationDistribution: Map<String, Int>,
        val humanAuthorityReferences: List<String>,
    )

    data class FamilyArtifact(
        val contractId: String,
        val version: String,
        val state: String,
        val batchId: String,
        val familyGroups: List<FamilyGroupDto>,
        val newFamilyGroupCount: Int,
        val newFamilyMembershipCount: Int,
        val originalV1FamilyGroupCount: Int,
        val totalValidatedFamilyGroupCountAfterExpansion: Int,
        val logicalDigest: String,
    )

    data class ExpansionArtifact(
        val contractId: String,
        val version: String,
        val state: String,
        val batchId: String,
        val humanAuthorityBatchDigest: String,
        val trainingExampleReferences: List<String>,
        val familyGroupReferences: List<String>,
        val excludedCandidateIds: List<String>,
        val partitionBucketIncluded: Boolean,
        val partitionAssignmentIncluded: Boolean,
        val trainingStarted: Boolean,
        val inferenceStarted: Boolean,
        val logicalDigest: String,
    )

    fun materialize(
        candidates: List<AuthorizedCandidate>,
        excludedCandidateIds: List<String>,
    ): Materialization {
        require(candidates.isNotEmpty())
        require(candidates.map { it.candidateId.value }.distinct().size == candidates.size)
        require(candidates.none { it.candidateId.value in excludedCandidateIds })

        val examples = candidates.flatMap { candidate ->
            candidate.records.map { record ->
                val target = targetFor(record.relationKind, candidate.candidateId)
                val input = HimTrainingInputV1(
                    observedTerm = record.observedTerm,
                    normalizedObservedTerm = record.normalizedObservedTerm,
                    canonicalContext = listOf(
                        de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext(
                            rank = 1,
                            canonicalId = candidate.candidateId,
                            canonicalName = candidate.candidateName,
                            fullRecordCanonicalJson = null,
                        ),
                    ),
                    evidence = listOf(
                        de.shopme.tools.knowledge.him.training.corpus.HimTrainingEvidenceInputV1(
                            reference = record.evidenceReference,
                            recordKind = record.recordKind,
                            retrievalRank = record.retrievalRank,
                        ),
                    ),
                )
                val provenance = HimTrainingProvenanceV1(
                    candidateReference = HimCandidateReference("candidate-canonical-target:v1:${candidate.candidateId.value}"),
                    sourceEvidenceReferences = listOf(record.evidenceReference),
                )
                val example = HimTrainingExampleV1.create(
                    taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
                    input = input,
                    target = target,
                    provenance = provenance,
                )
                val family = when (val resolution = HimTrainingFamilyGroupResolverV1.resolve(example)) {
                    is de.shopme.tools.knowledge.him.training.corpus.HimTrainingFamilyGroupResolutionV1.Resolved ->
                        resolution.groupReference
                    is de.shopme.tools.knowledge.him.training.corpus.HimTrainingFamilyGroupResolutionV1.NotYetGroupable ->
                        error(resolution.reason)
                }
                MaterializedExample(
                    candidateId = candidate.candidateId,
                    candidateName = candidate.candidateName,
                    confirmationReference = candidate.confirmationReference,
                    humanReviewerReference = candidate.humanReviewerReference,
                    primaryDecisionIdentity = candidate.primaryDecisionIdentity,
                    relationKind = record.relationKind,
                    semanticLabel = record.semanticLabel,
                    variantLabel = record.variantLabel,
                    processingForm = record.processingForm,
                    preparationState = record.preparationState,
                    productForm = record.productForm,
                    evidenceRecordId = record.evidenceRecordId,
                    evidenceReferenceId = record.evidenceReferenceId,
                    example = example,
                    familyGroupReference = family,
                )
            }
        }.sortedWith(compareBy({ it.candidateId.value }, { it.evidenceRecordId }, { it.relationKind.name }))

        val byExample = examples.groupBy { it.example.exampleReference.value }
        val duplicates = byExample.values.filter { it.size > 1 }
        require(duplicates.isEmpty()) { "DUPLICATE_TRAINING_EXAMPLE_IDENTITY" }

        val families = examples.groupBy { it.familyGroupReference }.map { (reference, members) ->
            val candidateIds = members.map { it.candidateId }.distinct()
            require(candidateIds.size == 1)
            FamilyGroup(
                familyGroupReference = reference,
                candidateId = candidateIds.single(),
                candidateName = members.first().candidateName,
                members = members.map { it.example.exampleReference.value }.sorted(),
                relationDistribution = members.groupingBy { it.relationKind.name }.eachCount().toSortedMap(),
                humanAuthorityReferences = members.map { it.confirmationReference }.distinct().sorted(),
            )
        }.sortedBy { it.familyGroupReference.value }

        return Materialization(
            candidates = candidates.sortedBy { it.candidateId.value },
            examples = examples,
            families = families,
            rawMaterializationCandidateCount = candidates.sumOf { it.records.size },
            duplicateCandidateCount = duplicates.sumOf { it.size - 1 },
            excludedCandidateIds = excludedCandidateIds.sorted(),
        )
    }

    fun authorityArtifact(materialization: Materialization): AuthorityArtifact {
        val records = materialization.candidates.map { candidate ->
            AuthorityRecordDto(
                candidateId = candidate.candidateId.value,
                candidateName = candidate.candidateName,
                stableEntryId = candidate.stableEntryId,
                reviewUnitId = candidate.reviewUnitId,
                primaryDecisionIdentity = candidate.primaryDecisionIdentity,
                confirmationReference = candidate.confirmationReference,
                humanReviewerReference = candidate.humanReviewerReference,
                humanRationale = candidate.humanRationale,
                aiProposalLogicalDigest = candidate.aiProposalLogicalDigest?.value,
                records = candidate.records.map { record ->
                    AuthorityRelationDto(
                        evidenceRecordId = record.evidenceRecordId,
                        evidenceReferenceId = record.evidenceReferenceId,
                        relation = record.relationKind.name,
                        targetCanonicalId = candidate.candidateId.value,
                        semanticLabel = record.semanticLabel,
                        variantLabel = record.variantLabel,
                        processingForm = record.processingForm,
                        preparationState = record.preparationState,
                        productForm = record.productForm,
                    )
                }.sortedBy { it.evidenceRecordId },
            )
        }
        val unsigned = AuthorityArtifact(
            contractId = CONTRACT_ID,
            version = VERSION,
            state = STATE,
            batchId = BATCH_ID,
            authorityType = AUTHORITY_TYPE,
            records = records,
            humanAuthorizedCandidateCount = records.size,
            recordWiseRelationCount = records.sumOf { it.records.size },
            logicalDigest = "",
        )
        return unsigned.copy(logicalDigest = sha256(AUTHORITY_DIGEST_DOMAIN + gson.toJson(unsigned)))
    }

    fun trainingExamplesArtifact(materialization: Materialization): TrainingExamplesArtifact {
        val examples = materialization.examples.map { item ->
            TrainingExampleDto(
                exampleReference = item.example.exampleReference.value,
                candidateId = item.candidateId.value,
                candidateName = item.candidateName,
                observedTerm = item.example.input.observedTerm,
                normalizedObservedTerm = item.example.input.normalizedObservedTerm,
                classification = item.example.target.classification.name,
                targetCanonicalId = item.candidateId.value,
                relation = item.relationKind.name,
                semanticLabel = item.semanticLabel,
                variantLabel = item.variantLabel,
                processingForm = item.processingForm,
                preparationState = item.preparationState,
                productForm = item.productForm,
                evidenceRecordId = item.evidenceRecordId,
                evidenceReferenceId = item.evidenceReferenceId,
                source = item.example.input.evidence.single().reference.source,
                sourceArtifactSha256 = item.example.input.evidence.single().reference.sourceArtifactSha256.value,
                humanAuthorityReference = item.confirmationReference,
                primaryDecisionIdentity = item.primaryDecisionIdentity,
                familyGroupReference = item.familyGroupReference.value,
            )
        }
        val unsigned = TrainingExamplesArtifact(
            contractId = CONTRACT_ID,
            version = VERSION,
            state = STATE,
            batchId = BATCH_ID,
            examples = examples,
            positiveCount = examples.size,
            negativeCount = 0,
            unresolvedCount = 0,
            aiOnlyCount = 0,
            logicalDigest = "",
        )
        return unsigned.copy(logicalDigest = sha256(EXAMPLES_DIGEST_DOMAIN + gson.toJson(unsigned)))
    }

    fun familyArtifact(materialization: Materialization): FamilyArtifact {
        val groups = materialization.families.map { family ->
            FamilyGroupDto(
                familyGroupReference = family.familyGroupReference.value,
                candidateId = family.candidateId.value,
                candidateName = family.candidateName,
                memberCount = family.members.size,
                trainingExampleReferences = family.members,
                relationDistribution = family.relationDistribution,
                humanAuthorityReferences = family.humanAuthorityReferences,
            )
        }
        val unsigned = FamilyArtifact(
            contractId = CONTRACT_ID,
            version = VERSION,
            state = STATE,
            batchId = BATCH_ID,
            familyGroups = groups,
            newFamilyGroupCount = groups.size,
            newFamilyMembershipCount = groups.sumOf { it.memberCount },
            originalV1FamilyGroupCount = 2,
            totalValidatedFamilyGroupCountAfterExpansion = groups.size + 2,
            logicalDigest = "",
        )
        return unsigned.copy(logicalDigest = sha256(FAMILIES_DIGEST_DOMAIN + gson.toJson(unsigned)))
    }

    fun expansionArtifact(
        materialization: Materialization,
        authority: AuthorityArtifact,
    ): ExpansionArtifact {
        val unsigned = ExpansionArtifact(
            contractId = CONTRACT_ID,
            version = VERSION,
            state = STATE,
            batchId = BATCH_ID,
            humanAuthorityBatchDigest = authority.logicalDigest,
            trainingExampleReferences = materialization.examples.map { it.example.exampleReference.value }.sorted(),
            familyGroupReferences = materialization.families.map { it.familyGroupReference.value }.sorted(),
            excludedCandidateIds = materialization.excludedCandidateIds,
            partitionBucketIncluded = false,
            partitionAssignmentIncluded = false,
            trainingStarted = false,
            inferenceStarted = false,
            logicalDigest = "",
        )
        return unsigned.copy(logicalDigest = sha256(INVENTORY_DIGEST_DOMAIN + gson.toJson(unsigned)))
    }

    fun serialize(value: Any): ByteArray = (gson.toJson(value) + "\n").toByteArray(StandardCharsets.UTF_8)

    fun targetFor(relation: SemanticRelationKindV2, candidateId: HimEntityId): HimTrainingTargetV1 {
        require(HimP1HumanReviewSemanticContractV2.targetKindFor(relation) != null)
        return when (relation) {
            SemanticRelationKindV2.IDENTITY_OF -> HimTrainingTargetV1.Identity(candidateId)
            SemanticRelationKindV2.VARIANT_OF,
            SemanticRelationKindV2.PROCESSING_FORM_OF,
            SemanticRelationKindV2.PREPARATION_STATE_OF,
            SemanticRelationKindV2.PRODUCT_FORM_OF,
            -> HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(candidateId))
            else -> error("UNSUPPORTED_MODEL_TARGET_RELATION:${relation.name}")
        }
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
