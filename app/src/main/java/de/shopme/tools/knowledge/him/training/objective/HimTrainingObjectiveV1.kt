package de.shopme.tools.knowledge.him.training.objective

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeBoundaryTypeV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExampleValidatorV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingClassificationV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTaskTypeV1
import java.security.MessageDigest

/**
 * The framework-independent semantic target boundary for Training V1.
 * It defines what a future numerical runtime must predict without defining
 * tensors, labels tied to a dataset, or a concrete ML framework.
 */
class HimTrainingObjectiveV1 private constructor(
    val contractId: String,
    val version: String,
    val state: String,
    val supportedTargetKinds: List<HimTrainingClassificationV1>,
    val supportedNegativeBoundaryTypes: List<HimNegativeBoundaryTypeV1>,
    val semanticLossRoles: List<SemanticLossRole>,
    val abstentionSupport: AbstentionSupport,
    val logicalDigest: HimSha256,
    val objectiveReference: String,
) {
    init {
        require(contractId == CONTRACT_ID)
        require(version == VERSION)
        require(state == STATE)
        require(supportedTargetKinds == SUPPORTED_TARGET_KINDS)
        require(supportedNegativeBoundaryTypes == SUPPORTED_NEGATIVE_BOUNDARY_TYPES)
        require(semanticLossRoles == SUPPORTED_LOSS_ROLES)
        require(abstentionSupport == AbstentionSupport.RESERVED_ONLY)
        require(objectiveReference == "$OBJECTIVE_REFERENCE_PREFIX${logicalDigest.value}")
    }

    fun projectPositive(example: HimTrainingExampleV1): Target.Positive {
        require(example.taskType == HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION) {
            "UNSUPPORTED_TRAINING_TASK"
        }
        val targetDigest = targetDigest(example.target)
        return Target.Positive(
            semanticTarget = example.target,
            logicalDigest = targetDigest,
            targetReference = "$TARGET_REFERENCE_PREFIX${targetDigest.value}",
        )
    }

    fun projectNegative(example: HimNegativeTrainingExampleV1): Target.Negative {
        HimNegativeTrainingExampleValidatorV1.validate(example)
        val positive = projectPositive(example.positiveExample)
        require(example.boundaryType in supportedNegativeBoundaryTypes) {
            "UNSUPPORTED_NEGATIVE_BOUNDARY"
        }
        val rejectedDigest = targetDigest(example.rejectedTarget)
        val negativeDigest = negativeTargetDigest(
            positiveTarget = positive.semanticTarget,
            rejectedTarget = example.rejectedTarget,
            boundaryType = example.boundaryType,
        )
        return Target.Negative(
            positiveTarget = positive,
            rejectedTarget = example.rejectedTarget,
            rejectedTargetLogicalDigest = rejectedDigest,
            rejectedTargetReference = "$TARGET_REFERENCE_PREFIX${rejectedDigest.value}",
            boundaryType = example.boundaryType,
            logicalDigest = negativeDigest,
            targetReference = "$NEGATIVE_TARGET_REFERENCE_PREFIX${negativeDigest.value}",
        )
    }

    sealed interface Target {
        val logicalDigest: HimSha256
        val targetReference: String

        data class Positive(
            val semanticTarget: HimTrainingTargetV1,
            override val logicalDigest: HimSha256,
            override val targetReference: String,
        ) : Target

        data class Negative(
            val positiveTarget: Positive,
            val rejectedTarget: HimTrainingTargetV1,
            val rejectedTargetLogicalDigest: HimSha256,
            val rejectedTargetReference: String,
            val boundaryType: HimNegativeBoundaryTypeV1,
            override val logicalDigest: HimSha256,
            override val targetReference: String,
        ) : Target
    }

    enum class SemanticLossRole {
        TARGET_KIND,
        TARGET_REFERENCE,
        NEGATIVE_BOUNDARY_REJECTION,
    }

    enum class AbstentionSupport {
        RESERVED_ONLY,
    }

    companion object {
        const val CONTRACT_ID = "HIM_TRAINING_OBJECTIVE_V1"
        const val VERSION = "1"
        const val STATE = "TRAINING_OBJECTIVE_DEFINED"

        private const val OBJECTIVE_REFERENCE_PREFIX = "training-objective:v1:"
        private const val TARGET_REFERENCE_PREFIX = "objective-target:v1:"
        private const val NEGATIVE_TARGET_REFERENCE_PREFIX = "objective-negative-target:v1:"

        val SUPPORTED_TARGET_KINDS = listOf(
            HimTrainingClassificationV1.EXISTING_CANONICAL,
            HimTrainingClassificationV1.IDENTITY,
            HimTrainingClassificationV1.VARIANT,
            HimTrainingClassificationV1.ALIAS,
            HimTrainingClassificationV1.NEW_CANONICAL,
        )

        val SUPPORTED_NEGATIVE_BOUNDARY_TYPES = listOf(
            HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY,
            HimNegativeBoundaryTypeV1.IDENTITY_VS_VARIANT_BOUNDARY,
            HimNegativeBoundaryTypeV1.ALIAS_VS_SEMANTIC_CHILD_BOUNDARY,
            HimNegativeBoundaryTypeV1.WRONG_RELATION_LEVEL,
            HimNegativeBoundaryTypeV1.WRONG_SCOPE,
            HimNegativeBoundaryTypeV1.WRONG_CLASSIFICATION,
        )

        val SUPPORTED_LOSS_ROLES = listOf(
            SemanticLossRole.TARGET_KIND,
            SemanticLossRole.TARGET_REFERENCE,
            SemanticLossRole.NEGATIVE_BOUNDARY_REJECTION,
        )

        fun create(): HimTrainingObjectiveV1 {
            val digest = objectiveDigest()
            return HimTrainingObjectiveV1(
                contractId = CONTRACT_ID,
                version = VERSION,
                state = STATE,
                supportedTargetKinds = SUPPORTED_TARGET_KINDS,
                supportedNegativeBoundaryTypes = SUPPORTED_NEGATIVE_BOUNDARY_TYPES,
                semanticLossRoles = SUPPORTED_LOSS_ROLES,
                abstentionSupport = AbstentionSupport.RESERVED_ONLY,
                logicalDigest = digest,
                objectiveReference = "$OBJECTIVE_REFERENCE_PREFIX${digest.value}",
            )
        }

        private fun objectiveDigest(): HimSha256 = sha256(buildString {
            field("contract", CONTRACT_ID)
            field("version", VERSION)
            field("state", STATE)
            SUPPORTED_TARGET_KINDS.forEachIndexed { index, kind -> field("target-kind-$index", kind.name) }
            SUPPORTED_NEGATIVE_BOUNDARY_TYPES.forEachIndexed { index, boundary ->
                field("negative-boundary-$index", boundary.name)
            }
            SUPPORTED_LOSS_ROLES.forEachIndexed { index, role -> field("loss-role-$index", role.name) }
            field("abstention", AbstentionSupport.RESERVED_ONLY.name)
        })

        private fun targetDigest(target: HimTrainingTargetV1): HimSha256 = sha256(buildString {
            field("objective", objectiveDigest().value)
            field("target", targetKey(target))
        })

        private fun negativeTargetDigest(
            positiveTarget: HimTrainingTargetV1,
            rejectedTarget: HimTrainingTargetV1,
            boundaryType: HimNegativeBoundaryTypeV1,
        ): HimSha256 = sha256(buildString {
            field("objective", objectiveDigest().value)
            field("positive-target", targetKey(positiveTarget))
            field("rejected-target", targetKey(rejectedTarget))
            field("boundary-type", boundaryType.name)
        })

        private fun targetKey(target: HimTrainingTargetV1): String = when (target) {
            is HimTrainingTargetV1.ExistingCanonical -> buildString {
                field("kind", target.classification.name)
                field("canonical-id", target.canonicalId.value)
            }
            is HimTrainingTargetV1.Identity -> buildString {
                field("kind", target.classification.name)
                field("parent-canonical-id", target.parentCanonicalId.value)
            }
            is HimTrainingTargetV1.Variant -> buildString {
                field("kind", target.classification.name)
                field("scope", familyReferenceKey(target.scope))
            }
            is HimTrainingTargetV1.Alias -> buildString {
                field("kind", target.classification.name)
                field("equivalent-entity", familyReferenceKey(target.equivalentEntity))
            }
            is HimTrainingTargetV1.NewCanonical -> buildString {
                field("kind", target.classification.name)
                field("proposed-canonical-name", target.proposedCanonicalName ?: "<null>")
            }
        }

        private fun familyReferenceKey(reference: HimFamilyEntityReference): String = when (reference) {
            is HimFamilyEntityReference.Canonical -> buildString {
                field("reference-kind", "CANONICAL")
                field("canonical-id", reference.canonicalId.value)
            }
            is HimFamilyEntityReference.Identity -> buildString {
                field("reference-kind", "IDENTITY")
                field("canonical-id", reference.canonicalId.value)
                field("identity-id", reference.identityId.value)
            }
        }

        private fun sha256(value: String): HimSha256 = HimSha256(
            MessageDigest.getInstance("SHA-256")
                .digest(value.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it.toInt() and 0xff) },
        )

        private fun StringBuilder.field(key: String, value: String) {
            append(key).append('=').append(value.length).append(':').append(value).append('\n')
        }
    }
}
