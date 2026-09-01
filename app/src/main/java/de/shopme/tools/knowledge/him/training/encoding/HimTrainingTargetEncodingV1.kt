package de.shopme.tools.knowledge.him.training.encoding

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeBoundaryTypeV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingClassificationV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import de.shopme.tools.knowledge.him.training.objective.HimTrainingObjectiveV1
import java.security.MessageDigest

/**
 * A technology-neutral projection of the frozen Training Objective.
 *
 * This type carries semantic codes and stable entity references only. It does
 * not assign dataset labels, tokenize input, describe a model, or execute
 * training.
 */
class HimTrainingTargetEncodingV1 private constructor(
    val contractId: String,
    val version: String,
    val state: String,
    val objectiveDigest: HimSha256,
    val objectiveReference: String,
    val positiveTargetKindCodes: Map<HimTrainingClassificationV1, Int>,
    val negativeBoundaryCodes: Map<HimNegativeBoundaryTypeV1, Int>,
    val lossRoleCodes: Map<HimTrainingObjectiveV1.SemanticLossRole, Int>,
    val abstentionSupport: HimTrainingObjectiveV1.AbstentionSupport,
    val activeAbstentionTargetCode: Int?,
    val logicalDigest: HimSha256,
    val encodingReference: String,
) {
    init {
        require(contractId == CONTRACT_ID)
        require(version == VERSION)
        require(state == STATE)
        require(objectiveReference == "training-objective:v1:${objectiveDigest.value}")
        require(positiveTargetKindCodes == POSITIVE_TARGET_KIND_CODES)
        require(negativeBoundaryCodes == NEGATIVE_BOUNDARY_CODES)
        require(lossRoleCodes == LOSS_ROLE_CODES)
        require(abstentionSupport == HimTrainingObjectiveV1.AbstentionSupport.RESERVED_ONLY)
        require(activeAbstentionTargetCode == null)
        require(logicalDigest == contractDigest(objectiveDigest))
        require(encodingReference == "$ENCODING_REFERENCE_PREFIX${logicalDigest.value}")
    }

    /** Encodes an already projected positive Objective target. */
    fun encodePositive(target: HimTrainingObjectiveV1.Target.Positive): Target.Positive {
        validatePositiveProjection(target)
        val semanticTarget = encodeSemanticTarget(target.semanticTarget)
        val kindCode = positiveTargetKindCodes.getValue(target.semanticTarget.classification)
        val encodedDigest = positiveEncodingDigest(kindCode, semanticTarget)
        return Target.Positive(
            encodingContractId = contractId,
            encodingVersion = version,
            objectiveDigest = objectiveDigest,
            objectiveReference = objectiveReference,
            semanticTarget = semanticTarget,
            targetKindCode = kindCode,
            lossRoleCodes = listOf(
                lossRoleCodes.getValue(HimTrainingObjectiveV1.SemanticLossRole.TARGET_KIND),
                lossRoleCodes.getValue(HimTrainingObjectiveV1.SemanticLossRole.TARGET_REFERENCE),
            ),
            logicalDigest = encodedDigest,
            targetReference = "$POSITIVE_REFERENCE_PREFIX${encodedDigest.value}",
        )
    }

    /** The Objective argument makes the binding explicit at call sites. */
    fun encodePositive(
        objective: HimTrainingObjectiveV1,
        target: HimTrainingObjectiveV1.Target.Positive,
    ): Target.Positive {
        validateObjective(objective)
        return encodePositive(target)
    }

    /** Encodes an already projected negative Objective target. */
    fun encodeNegative(target: HimTrainingObjectiveV1.Target.Negative): Target.Negative {
        validatePositiveProjection(target.positiveTarget)
        validateNegativeProjection(target)
        val positive = encodePositive(target.positiveTarget)
        val rejected = encodeSemanticTarget(target.rejectedTarget)
        val boundaryCode = negativeBoundaryCodes[target.boundaryType]
            ?: error("UNSUPPORTED_NEGATIVE_BOUNDARY")
        val encodedDigest = negativeEncodingDigest(positive, rejected, target.boundaryType, boundaryCode)
        return Target.Negative(
            encodingContractId = contractId,
            encodingVersion = version,
            objectiveDigest = objectiveDigest,
            objectiveReference = objectiveReference,
            positiveTarget = positive,
            rejectedTarget = rejected,
            boundaryType = target.boundaryType,
            boundaryTypeCode = boundaryCode,
            lossRoleCodes = listOf(
                lossRoleCodes.getValue(HimTrainingObjectiveV1.SemanticLossRole.TARGET_KIND),
                lossRoleCodes.getValue(HimTrainingObjectiveV1.SemanticLossRole.TARGET_REFERENCE),
                lossRoleCodes.getValue(HimTrainingObjectiveV1.SemanticLossRole.NEGATIVE_BOUNDARY_REJECTION),
            ),
            rejectedTargetLogicalDigest = target.rejectedTargetLogicalDigest,
            rejectedTargetReference = target.rejectedTargetReference,
            logicalDigest = encodedDigest,
            targetReference = "$NEGATIVE_REFERENCE_PREFIX${encodedDigest.value}",
        )
    }

    /** The Objective argument makes the binding explicit at call sites. */
    fun encodeNegative(
        objective: HimTrainingObjectiveV1,
        target: HimTrainingObjectiveV1.Target.Negative,
    ): Target.Negative {
        validateObjective(objective)
        return encodeNegative(target)
    }

    sealed interface Target {
        val encodingContractId: String
        val encodingVersion: String
        val objectiveDigest: HimSha256
        val objectiveReference: String
        val lossRoleCodes: List<Int>
        val logicalDigest: HimSha256
        val targetReference: String

        data class Positive internal constructor(
            override val encodingContractId: String,
            override val encodingVersion: String,
            override val objectiveDigest: HimSha256,
            override val objectiveReference: String,
            val semanticTarget: SemanticTarget,
            val targetKindCode: Int,
            override val lossRoleCodes: List<Int>,
            override val logicalDigest: HimSha256,
            override val targetReference: String,
        ) : Target

        data class Negative internal constructor(
            override val encodingContractId: String,
            override val encodingVersion: String,
            override val objectiveDigest: HimSha256,
            override val objectiveReference: String,
            val positiveTarget: Positive,
            val rejectedTarget: SemanticTarget,
            val boundaryType: HimNegativeBoundaryTypeV1,
            val boundaryTypeCode: Int,
            override val lossRoleCodes: List<Int>,
            val rejectedTargetLogicalDigest: HimSha256,
            val rejectedTargetReference: String,
            override val logicalDigest: HimSha256,
            override val targetReference: String,
        ) : Target
    }

    sealed interface SemanticTarget {
        val kind: HimTrainingClassificationV1
        val kindCode: Int

        data class ExistingCanonical internal constructor(
            val canonicalId: HimEntityId,
            override val kindCode: Int,
        ) : SemanticTarget {
            override val kind = HimTrainingClassificationV1.EXISTING_CANONICAL
        }

        data class Identity internal constructor(
            val parentCanonicalId: HimEntityId,
            override val kindCode: Int,
        ) : SemanticTarget {
            override val kind = HimTrainingClassificationV1.IDENTITY
        }

        data class Variant internal constructor(
            val scope: FamilyReference,
            override val kindCode: Int,
        ) : SemanticTarget {
            override val kind = HimTrainingClassificationV1.VARIANT
        }

        data class Alias internal constructor(
            val equivalentEntity: FamilyReference,
            override val kindCode: Int,
        ) : SemanticTarget {
            override val kind = HimTrainingClassificationV1.ALIAS
        }

        data class NewCanonical internal constructor(
            val proposedCanonicalName: String?,
            override val kindCode: Int,
        ) : SemanticTarget {
            override val kind = HimTrainingClassificationV1.NEW_CANONICAL
        }
    }

    sealed interface FamilyReference {
        val canonicalId: HimEntityId

        data class Canonical internal constructor(
            override val canonicalId: HimEntityId,
        ) : FamilyReference

        data class Identity internal constructor(
            override val canonicalId: HimEntityId,
            val identityId: HimEntityId,
        ) : FamilyReference
    }

    companion object {
        const val CONTRACT_ID = "HIM_TRAINING_TARGET_ENCODING_V1"
        const val VERSION = "1"
        const val STATE = "TRAINING_TARGET_ENCODING_DEFINED"

        const val GLOBAL_DATASET_ENTITY_LABEL_INDEX = "NO"
        const val ENTITY_INDEX_ASSIGNMENT = 0
        const val NEW_CANONICAL_ENTITY_INDEX = "NONE"
        const val NEW_CANONICAL_GENERATION_TARGET = "NO"
        const val ABSTENTION_NUMERICAL_SEMANTICS = "RESERVED_ONLY"
        const val TENSOR_REPRESENTATION = 0
        const val INPUT_TOKEN_ENCODING = 0
        const val MODEL_OUTPUT_SHAPE_BINDING = 0

        private const val ENCODING_REFERENCE_PREFIX = "training-target-encoding:v1:"
        private const val POSITIVE_REFERENCE_PREFIX = "training-encoded-positive-target:v1:"
        private const val NEGATIVE_REFERENCE_PREFIX = "training-encoded-negative-target:v1:"

        val POSITIVE_TARGET_KIND_CODES = linkedMapOf(
            HimTrainingClassificationV1.EXISTING_CANONICAL to 1,
            HimTrainingClassificationV1.IDENTITY to 2,
            HimTrainingClassificationV1.VARIANT to 3,
            HimTrainingClassificationV1.ALIAS to 4,
            HimTrainingClassificationV1.NEW_CANONICAL to 5,
        ).toMap()

        val NEGATIVE_BOUNDARY_CODES = linkedMapOf(
            HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY to 1,
            HimNegativeBoundaryTypeV1.IDENTITY_VS_VARIANT_BOUNDARY to 2,
            HimNegativeBoundaryTypeV1.ALIAS_VS_SEMANTIC_CHILD_BOUNDARY to 3,
            HimNegativeBoundaryTypeV1.WRONG_RELATION_LEVEL to 4,
            HimNegativeBoundaryTypeV1.WRONG_SCOPE to 5,
            HimNegativeBoundaryTypeV1.WRONG_CLASSIFICATION to 6,
        ).toMap()

        val LOSS_ROLE_CODES = linkedMapOf(
            HimTrainingObjectiveV1.SemanticLossRole.TARGET_KIND to 1,
            HimTrainingObjectiveV1.SemanticLossRole.TARGET_REFERENCE to 2,
            HimTrainingObjectiveV1.SemanticLossRole.NEGATIVE_BOUNDARY_REJECTION to 3,
        ).toMap()

        val ACTIVE_ABSTENTION_TARGET_CODE: Int? = null

        fun create(objective: HimTrainingObjectiveV1 = HimTrainingObjectiveV1.create()): HimTrainingTargetEncodingV1 {
            validateObjective(objective)
            val digest = contractDigest(objective.logicalDigest)
            return HimTrainingTargetEncodingV1(
                contractId = CONTRACT_ID,
                version = VERSION,
                state = STATE,
                objectiveDigest = objective.logicalDigest,
                objectiveReference = objective.objectiveReference,
                positiveTargetKindCodes = POSITIVE_TARGET_KIND_CODES,
                negativeBoundaryCodes = NEGATIVE_BOUNDARY_CODES,
                lossRoleCodes = LOSS_ROLE_CODES,
                abstentionSupport = objective.abstentionSupport,
                activeAbstentionTargetCode = ACTIVE_ABSTENTION_TARGET_CODE,
                logicalDigest = digest,
                encodingReference = "$ENCODING_REFERENCE_PREFIX${digest.value}",
            )
        }

        fun positiveTargetKindForCode(code: Int): HimTrainingClassificationV1? =
            POSITIVE_TARGET_KIND_CODES.entries.firstOrNull { it.value == code }?.key

        fun negativeBoundaryForCode(code: Int): HimNegativeBoundaryTypeV1? =
            NEGATIVE_BOUNDARY_CODES.entries.firstOrNull { it.value == code }?.key

        fun lossRoleForCode(code: Int): HimTrainingObjectiveV1.SemanticLossRole? =
            LOSS_ROLE_CODES.entries.firstOrNull { it.value == code }?.key

        private fun validateObjective(objective: HimTrainingObjectiveV1) {
            val canonical = HimTrainingObjectiveV1.create()
            require(objective.contractId == HimTrainingObjectiveV1.CONTRACT_ID)
            require(objective.version == HimTrainingObjectiveV1.VERSION)
            require(objective.state == HimTrainingObjectiveV1.STATE)
            require(objective.supportedTargetKinds == canonical.supportedTargetKinds)
            require(objective.supportedNegativeBoundaryTypes == canonical.supportedNegativeBoundaryTypes)
            require(objective.semanticLossRoles == canonical.semanticLossRoles)
            require(objective.abstentionSupport == HimTrainingObjectiveV1.AbstentionSupport.RESERVED_ONLY)
            require(objective.logicalDigest == canonical.logicalDigest) { "OBJECTIVE_DIGEST_MISMATCH" }
            require(objective.objectiveReference == canonical.objectiveReference) { "OBJECTIVE_REFERENCE_MISMATCH" }
        }

        private fun validatePositiveProjection(target: HimTrainingObjectiveV1.Target.Positive) {
            val expected = objectiveTargetDigest(target.semanticTarget)
            require(target.logicalDigest == expected) { "OBJECTIVE_TARGET_DIGEST_MISMATCH" }
            require(target.targetReference == "objective-target:v1:${expected.value}") {
                "OBJECTIVE_TARGET_REFERENCE_MISMATCH"
            }
        }

        private fun validateNegativeProjection(target: HimTrainingObjectiveV1.Target.Negative) {
            val rejectedDigest = objectiveTargetDigest(target.rejectedTarget)
            require(target.rejectedTargetLogicalDigest == rejectedDigest) {
                "OBJECTIVE_REJECTED_TARGET_DIGEST_MISMATCH"
            }
            require(target.rejectedTargetReference == "objective-target:v1:${rejectedDigest.value}") {
                "OBJECTIVE_REJECTED_TARGET_REFERENCE_MISMATCH"
            }
            val expected = objectiveNegativeTargetDigest(
                target.positiveTarget.semanticTarget,
                target.rejectedTarget,
                target.boundaryType,
            )
            require(target.logicalDigest == expected) { "OBJECTIVE_NEGATIVE_TARGET_DIGEST_MISMATCH" }
            require(target.targetReference == "objective-negative-target:v1:${expected.value}") {
                "OBJECTIVE_NEGATIVE_TARGET_REFERENCE_MISMATCH"
            }
        }

        private fun encodeSemanticTarget(target: HimTrainingTargetV1): SemanticTarget {
            val code = POSITIVE_TARGET_KIND_CODES[target.classification]
                ?: error("UNSUPPORTED_TARGET_KIND")
            return when (target) {
                is HimTrainingTargetV1.ExistingCanonical ->
                    SemanticTarget.ExistingCanonical(target.canonicalId, code)
                is HimTrainingTargetV1.Identity ->
                    SemanticTarget.Identity(target.parentCanonicalId, code)
                is HimTrainingTargetV1.Variant ->
                    SemanticTarget.Variant(encodeFamilyReference(target.scope), code)
                is HimTrainingTargetV1.Alias ->
                    SemanticTarget.Alias(encodeFamilyReference(target.equivalentEntity), code)
                is HimTrainingTargetV1.NewCanonical ->
                    SemanticTarget.NewCanonical(target.proposedCanonicalName, code)
            }
        }

        private fun encodeFamilyReference(reference: HimFamilyEntityReference): FamilyReference = when (reference) {
            is HimFamilyEntityReference.Canonical -> FamilyReference.Canonical(reference.canonicalId)
            is HimFamilyEntityReference.Identity ->
                FamilyReference.Identity(reference.canonicalId, reference.identityId)
        }

        private fun positiveEncodingDigest(kindCode: Int, target: SemanticTarget): HimSha256 = sha256(buildString {
            field("contract", CONTRACT_ID)
            field("version", VERSION)
            field("objective", HimTrainingObjectiveV1.create().logicalDigest.value)
            field("kind-code", kindCode.toString())
            field("target", semanticTargetKey(target))
            field("loss-role-code-0", LOSS_ROLE_CODES.getValue(HimTrainingObjectiveV1.SemanticLossRole.TARGET_KIND).toString())
            field("loss-role-code-1", LOSS_ROLE_CODES.getValue(HimTrainingObjectiveV1.SemanticLossRole.TARGET_REFERENCE).toString())
        })

        private fun negativeEncodingDigest(
            positive: Target.Positive,
            rejected: SemanticTarget,
            boundaryType: HimNegativeBoundaryTypeV1,
            boundaryCode: Int,
        ): HimSha256 = sha256(buildString {
            field("contract", CONTRACT_ID)
            field("version", VERSION)
            field("objective", positive.objectiveDigest.value)
            field("positive", positive.semanticTarget.let(::semanticTargetKey))
            field("rejected", semanticTargetKey(rejected))
            field("boundary", boundaryType.name)
            field("boundary-code", boundaryCode.toString())
            field("loss-role-code-0", LOSS_ROLE_CODES.getValue(HimTrainingObjectiveV1.SemanticLossRole.TARGET_KIND).toString())
            field("loss-role-code-1", LOSS_ROLE_CODES.getValue(HimTrainingObjectiveV1.SemanticLossRole.TARGET_REFERENCE).toString())
            field("loss-role-code-2", LOSS_ROLE_CODES.getValue(HimTrainingObjectiveV1.SemanticLossRole.NEGATIVE_BOUNDARY_REJECTION).toString())
        })

        private fun objectiveTargetDigest(target: HimTrainingTargetV1): HimSha256 = sha256(buildString {
            field("objective", HimTrainingObjectiveV1.create().logicalDigest.value)
            field("target", objectiveTargetKey(target))
        })

        private fun objectiveNegativeTargetDigest(
            positive: HimTrainingTargetV1,
            rejected: HimTrainingTargetV1,
            boundary: HimNegativeBoundaryTypeV1,
        ): HimSha256 = sha256(buildString {
            field("objective", HimTrainingObjectiveV1.create().logicalDigest.value)
            field("positive-target", objectiveTargetKey(positive))
            field("rejected-target", objectiveTargetKey(rejected))
            field("boundary-type", boundary.name)
        })

        private fun objectiveTargetKey(target: HimTrainingTargetV1): String = when (target) {
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

        private fun semanticTargetKey(target: SemanticTarget): String = when (target) {
            is SemanticTarget.ExistingCanonical -> buildString {
                field("kind", target.kind.name)
                field("canonical-id", target.canonicalId.value)
            }
            is SemanticTarget.Identity -> buildString {
                field("kind", target.kind.name)
                field("parent-canonical-id", target.parentCanonicalId.value)
            }
            is SemanticTarget.Variant -> buildString {
                field("kind", target.kind.name)
                field("scope", encodedFamilyReferenceKey(target.scope))
            }
            is SemanticTarget.Alias -> buildString {
                field("kind", target.kind.name)
                field("equivalent-entity", encodedFamilyReferenceKey(target.equivalentEntity))
            }
            is SemanticTarget.NewCanonical -> buildString {
                field("kind", target.kind.name)
                field("proposed-canonical-name", target.proposedCanonicalName ?: "<null>")
            }
        }

        private fun encodedFamilyReferenceKey(reference: FamilyReference): String = when (reference) {
            is FamilyReference.Canonical -> buildString {
                field("reference-kind", "CANONICAL")
                field("canonical-id", reference.canonicalId.value)
            }
            is FamilyReference.Identity -> buildString {
                field("reference-kind", "IDENTITY")
                field("canonical-id", reference.canonicalId.value)
                field("identity-id", reference.identityId.value)
            }
        }

        private fun contractDigest(objectiveDigest: HimSha256): HimSha256 = sha256(buildString {
            field("contract", CONTRACT_ID)
            field("version", VERSION)
            field("state", STATE)
            field("objective", objectiveDigest.value)
            field("objective-reference", "training-objective:v1:${objectiveDigest.value}")
            POSITIVE_TARGET_KIND_CODES.forEach { (kind, code) -> field("positive-kind-${kind.name}", code.toString()) }
            NEGATIVE_BOUNDARY_CODES.forEach { (boundary, code) -> field("negative-boundary-${boundary.name}", code.toString()) }
            LOSS_ROLE_CODES.forEach { (role, code) -> field("loss-role-${role.name}", code.toString()) }
            field("abstention", HimTrainingObjectiveV1.AbstentionSupport.RESERVED_ONLY.name)
            field("active-abstention-target-code", "<none>")
            field("null-proposed-canonical-name", "EXPLICIT_NULL")
            field("family-reference-encoding", "TYPED_CANONICAL_OR_IDENTITY")
            field("dataset-entity-label-index", GLOBAL_DATASET_ENTITY_LABEL_INDEX)
            field("tensor-representation", TENSOR_REPRESENTATION.toString())
            field("input-token-encoding", INPUT_TOKEN_ENCODING.toString())
            field("model-output-shape-binding", MODEL_OUTPUT_SHAPE_BINDING.toString())
        })

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
