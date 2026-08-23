package de.shopme.tools.knowledge.him.training.corpus

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import java.security.MessageDigest

object HimNegativeTrainingExampleContractV1 {
    const val VERSION = "HIM_NEGATIVE_TRAINING_EXAMPLE_V1"
    const val POLICY_VERSION = "HIM_NEGATIVE_TRAINING_ADMISSIBILITY_POLICY_V1"
    const val EXAMPLE_REFERENCE_CONTRACT = "HIM_NEGATIVE_TRAINING_EXAMPLE_REFERENCE_V1"
    const val ALTERNATIVE_REFERENCE_CONTRACT = "HIM_NEGATIVE_TRAINING_ALTERNATIVE_REFERENCE_V1"
}

enum class HimNegativeBoundaryTypeV1 {
    WRONG_CLASSIFICATION,
    WRONG_SCOPE,
    WRONG_RELATION_LEVEL,
    CANONICAL_VS_CHILD_BOUNDARY,
    IDENTITY_VS_VARIANT_BOUNDARY,
    ALIAS_VS_SEMANTIC_CHILD_BOUNDARY,
}

data class HimNegativeTrainingExampleReferenceV1(
    val value: String,
) {
    init {
        require(value.matches(Regex("negative-example:v1:[0-9a-f]{64}")))
    }
}

data class HimNegativeAlternativeReferenceV1(
    val value: String,
) {
    init {
        require(value.matches(Regex("negative-alternative:v1:[0-9a-f]{64}")))
    }
}

data class HimNegativeTrainingExampleProvenanceV1(
    val positiveExampleReference: HimTrainingExampleReference,
    val derivationPolicyVersion: String = HimNegativeTrainingExampleContractV1.POLICY_VERSION,
    val rejectedAlternativeReference: HimNegativeAlternativeReferenceV1,
) {
    init {
        require(derivationPolicyVersion == HimNegativeTrainingExampleContractV1.POLICY_VERSION)
    }
}

data class HimNegativeTrainingExampleV1(
    val reference: HimNegativeTrainingExampleReferenceV1,
    val contractVersion: String = HimNegativeTrainingExampleContractV1.VERSION,
    val positiveExample: HimTrainingExampleV1,
    val rejectedTarget: HimTrainingTargetV1,
    val boundaryType: HimNegativeBoundaryTypeV1,
    val provenance: HimNegativeTrainingExampleProvenanceV1,
) {
    init {
        require(contractVersion == HimNegativeTrainingExampleContractV1.VERSION)
        HimNegativeTrainingExampleValidatorV1.validate(this)
    }

    /** The inference situation is exactly the positive example's model input. */
    fun modelInput(): HimTrainingInputV1 = positiveExample.modelInput()

    companion object {
        fun create(
            positiveExample: HimTrainingExampleV1,
            rejectedTarget: HimTrainingTargetV1,
            boundaryType: HimNegativeBoundaryTypeV1,
        ): HimNegativeTrainingExampleV1 {
            val alternativeReference = HimNegativeTrainingExampleIdentityV1.alternative(rejectedTarget)
            val provenance = HimNegativeTrainingExampleProvenanceV1(
                positiveExampleReference = positiveExample.exampleReference,
                rejectedAlternativeReference = alternativeReference,
            )
            return HimNegativeTrainingExampleV1(
                reference = HimNegativeTrainingExampleIdentityV1.example(
                    positiveExample = positiveExample,
                    rejectedTarget = rejectedTarget,
                    boundaryType = boundaryType,
                ),
                positiveExample = positiveExample,
                rejectedTarget = rejectedTarget,
                boundaryType = boundaryType,
                provenance = provenance,
            )
        }
    }
}

data class HimNegativeAdmissibilityResultV1(
    val admissible: Boolean,
    val reason: String,
) {
    init {
        require(reason.isNotBlank())
    }
}

object HimNegativeTrainingExamplePolicyV1 {
    fun evaluate(
        positiveExample: HimTrainingExampleV1,
        rejectedTarget: HimTrainingTargetV1,
        boundaryType: HimNegativeBoundaryTypeV1,
    ): HimNegativeAdmissibilityResultV1 {
        HimTrainingExampleValidatorV1.validate(positiveExample)
        if (positiveExample.target == rejectedTarget) {
            return inadmissible("Rejected target equals the authoritative positive target.")
        }

        val admissible = when (boundaryType) {
            HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY ->
                isCanonicalVsChildBoundary(positiveExample.target, rejectedTarget)
            HimNegativeBoundaryTypeV1.IDENTITY_VS_VARIANT_BOUNDARY ->
                isIdentityVsVariantBoundary(positiveExample.target, rejectedTarget)
            HimNegativeBoundaryTypeV1.ALIAS_VS_SEMANTIC_CHILD_BOUNDARY ->
                isAliasVsChildBoundary(positiveExample.target, rejectedTarget)
            HimNegativeBoundaryTypeV1.WRONG_SCOPE ->
                isWrongCanonicalScope(positiveExample, rejectedTarget)
            HimNegativeBoundaryTypeV1.WRONG_RELATION_LEVEL ->
                isWrongRelationLevel(positiveExample, rejectedTarget)
            HimNegativeBoundaryTypeV1.WRONG_CLASSIFICATION ->
                isExplicitCanonicalClassificationBoundary(positiveExample.target, rejectedTarget)
        }
        return if (admissible) {
            HimNegativeAdmissibilityResultV1(true, "The positive Ground-Truth relation proves this alternative incompatible.")
        } else {
            inadmissible("The current Ground-Truth contract cannot prove this alternative wrong.")
        }
    }

    fun requireAdmissible(
        positiveExample: HimTrainingExampleV1,
        rejectedTarget: HimTrainingTargetV1,
        boundaryType: HimNegativeBoundaryTypeV1,
    ) {
        val result = evaluate(positiveExample, rejectedTarget, boundaryType)
        require(result.admissible) { result.reason }
    }

    /** Small deterministic derivation; it never enumerates a catalog or creates arbitrary alternatives. */
    fun derive(positiveExample: HimTrainingExampleV1): List<HimNegativeTrainingExampleV1> {
        HimTrainingExampleValidatorV1.validate(positiveExample)
        val alternatives = buildList {
            childVsCanonical(positiveExample)?.let { add(it) }
            identityVsVariant(positiveExample)?.let { add(it) }
            aliasVsChild(positiveExample)?.let { add(it) }
            wrongRelationLevel(positiveExample)?.let { add(it) }
            wrongCanonicalScope(positiveExample)?.let { add(it) }
        }
        return alternatives
            .distinctBy { it.reference }
            .sortedWith(
                compareBy<HimNegativeTrainingExampleV1>(
                    { boundaryOrder(it.boundaryType) },
                    { HimNegativeTrainingExampleIdentityV1.targetKey(it.rejectedTarget) },
                    { it.reference.value },
                )
            )
    }

    private fun childVsCanonical(positive: HimTrainingExampleV1): HimNegativeTrainingExampleV1? {
        val positiveTarget = positive.target
        if (positiveTarget !is HimTrainingTargetV1.Identity &&
            positiveTarget !is HimTrainingTargetV1.Variant &&
            positiveTarget !is HimTrainingTargetV1.Alias
        ) {
            return null
        }
        val result = HimTrainingTargetV1.NewCanonical()
        return admissibleExample(positive, result, HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY)
    }

    private fun identityVsVariant(positive: HimTrainingExampleV1): HimNegativeTrainingExampleV1? {
        val rejected = when (val target = positive.target) {
            is HimTrainingTargetV1.Identity -> HimTrainingTargetV1.Variant(
                HimFamilyEntityReference.Canonical(target.parentCanonicalId),
            )
            is HimTrainingTargetV1.Variant -> HimTrainingTargetV1.Identity(target.scope.canonicalId)
            else -> return null
        }
        return admissibleExample(positive, rejected, HimNegativeBoundaryTypeV1.IDENTITY_VS_VARIANT_BOUNDARY)
    }

    private fun aliasVsChild(positive: HimTrainingExampleV1): HimNegativeTrainingExampleV1? {
        val rejected = when (val target = positive.target) {
            is HimTrainingTargetV1.Alias -> HimTrainingTargetV1.Variant(target.equivalentEntity)
            is HimTrainingTargetV1.Variant -> HimTrainingTargetV1.Alias(target.scope)
            is HimTrainingTargetV1.Identity -> HimTrainingTargetV1.Alias(
                HimFamilyEntityReference.Canonical(target.parentCanonicalId),
            )
            else -> return null
        }
        return admissibleExample(positive, rejected, HimNegativeBoundaryTypeV1.ALIAS_VS_SEMANTIC_CHILD_BOUNDARY)
    }

    private fun wrongRelationLevel(positive: HimTrainingExampleV1): HimNegativeTrainingExampleV1? {
        val target = positive.target as? HimTrainingTargetV1.Variant ?: return null
        if (target.scope !is HimFamilyEntityReference.Identity) return null
        val rejected = HimTrainingTargetV1.Variant(
            HimFamilyEntityReference.Canonical(target.scope.canonicalId),
        )
        return admissibleExample(positive, rejected, HimNegativeBoundaryTypeV1.WRONG_RELATION_LEVEL)
    }

    private fun wrongCanonicalScope(positive: HimTrainingExampleV1): HimNegativeTrainingExampleV1? {
        val positiveCanonicalId = canonicalId(positive.target) ?: return null
        val alternativeCanonicalId = positive.input.canonicalContext
            .asSequence()
            .filter { it.canonicalId != positiveCanonicalId }
            .sortedWith(compareBy({ it.rank }, { it.canonicalId.value }))
            .map { it.canonicalId }
            .firstOrNull() ?: return null
        val rejected = replaceCanonicalScope(positive.target, alternativeCanonicalId) ?: return null
        return admissibleExample(positive, rejected, HimNegativeBoundaryTypeV1.WRONG_SCOPE)
    }

    private fun admissibleExample(
        positive: HimTrainingExampleV1,
        target: HimTrainingTargetV1,
        boundaryType: HimNegativeBoundaryTypeV1,
    ): HimNegativeTrainingExampleV1? =
        if (evaluate(positive, target, boundaryType).admissible) {
            HimNegativeTrainingExampleV1.create(positive, target, boundaryType)
        } else {
            null
        }

    private fun isCanonicalVsChildBoundary(
        positive: HimTrainingTargetV1,
        rejected: HimTrainingTargetV1,
    ): Boolean =
        (positive is HimTrainingTargetV1.Identity || positive is HimTrainingTargetV1.Variant || positive is HimTrainingTargetV1.Alias) &&
            rejected is HimTrainingTargetV1.NewCanonical

    private fun isIdentityVsVariantBoundary(
        positive: HimTrainingTargetV1,
        rejected: HimTrainingTargetV1,
    ): Boolean {
        val identity = positive as? HimTrainingTargetV1.Identity ?: rejected as? HimTrainingTargetV1.Identity ?: return false
        val variant = positive as? HimTrainingTargetV1.Variant ?: rejected as? HimTrainingTargetV1.Variant ?: return false
        return identity.parentCanonicalId == variant.scope.canonicalId
    }

    private fun isAliasVsChildBoundary(
        positive: HimTrainingTargetV1,
        rejected: HimTrainingTargetV1,
    ): Boolean {
        val alias = positive as? HimTrainingTargetV1.Alias ?: rejected as? HimTrainingTargetV1.Alias ?: return false
        val child = when {
            positive is HimTrainingTargetV1.Identity -> positive
            positive is HimTrainingTargetV1.Variant -> positive
            rejected is HimTrainingTargetV1.Identity -> rejected
            rejected is HimTrainingTargetV1.Variant -> rejected
            else -> return false
        }
        return canonicalId(alias) == canonicalId(child)
    }

    private fun isWrongCanonicalScope(
        positive: HimTrainingExampleV1,
        rejected: HimTrainingTargetV1,
    ): Boolean {
        if (positive.target::class != rejected::class) return false
        val positiveCanonicalId = canonicalId(positive.target) ?: return false
        val rejectedCanonicalId = canonicalId(rejected) ?: return false
        if (positiveCanonicalId == rejectedCanonicalId || !scopeDiffers(positive.target, rejected)) return false
        return positive.input.canonicalContext.any { it.canonicalId == rejectedCanonicalId }
    }

    private fun isWrongRelationLevel(
        positive: HimTrainingExampleV1,
        rejected: HimTrainingTargetV1,
    ): Boolean {
        val positiveVariant = positive.target as? HimTrainingTargetV1.Variant ?: return false
        val rejectedVariant = rejected as? HimTrainingTargetV1.Variant ?: return false
        return positiveVariant.scope.canonicalId == rejectedVariant.scope.canonicalId &&
            positiveVariant.scope::class != rejectedVariant.scope::class
    }

    private fun isExplicitCanonicalClassificationBoundary(
        positive: HimTrainingTargetV1,
        rejected: HimTrainingTargetV1,
    ): Boolean {
        val canonical = positive as? HimTrainingTargetV1.ExistingCanonical ?: rejected as? HimTrainingTargetV1.ExistingCanonical ?: return false
        val child = when {
            positive is HimTrainingTargetV1.Identity -> positive
            positive is HimTrainingTargetV1.Variant -> positive
            positive is HimTrainingTargetV1.Alias -> positive
            rejected is HimTrainingTargetV1.Identity -> rejected
            rejected is HimTrainingTargetV1.Variant -> rejected
            rejected is HimTrainingTargetV1.Alias -> rejected
            else -> return false
        }
        return canonical.canonicalId == canonicalId(child)
    }

    private fun scopeDiffers(first: HimTrainingTargetV1, second: HimTrainingTargetV1): Boolean = when {
        first is HimTrainingTargetV1.Identity && second is HimTrainingTargetV1.Identity -> first.parentCanonicalId != second.parentCanonicalId
        first is HimTrainingTargetV1.Variant && second is HimTrainingTargetV1.Variant -> first.scope != second.scope
        first is HimTrainingTargetV1.Alias && second is HimTrainingTargetV1.Alias -> first.equivalentEntity != second.equivalentEntity
        first is HimTrainingTargetV1.ExistingCanonical && second is HimTrainingTargetV1.ExistingCanonical -> first.canonicalId != second.canonicalId
        else -> false
    }

    private fun replaceCanonicalScope(target: HimTrainingTargetV1, canonicalId: HimEntityId): HimTrainingTargetV1? = when (target) {
        is HimTrainingTargetV1.ExistingCanonical -> HimTrainingTargetV1.ExistingCanonical(canonicalId)
        is HimTrainingTargetV1.Identity -> HimTrainingTargetV1.Identity(canonicalId)
        is HimTrainingTargetV1.Variant -> HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(canonicalId))
        is HimTrainingTargetV1.Alias -> HimTrainingTargetV1.Alias(HimFamilyEntityReference.Canonical(canonicalId))
        is HimTrainingTargetV1.NewCanonical -> null
    }

    private fun canonicalId(target: HimTrainingTargetV1): HimEntityId? = when (target) {
        is HimTrainingTargetV1.ExistingCanonical -> target.canonicalId
        is HimTrainingTargetV1.Identity -> target.parentCanonicalId
        is HimTrainingTargetV1.Variant -> target.scope.canonicalId
        is HimTrainingTargetV1.Alias -> target.equivalentEntity.canonicalId
        is HimTrainingTargetV1.NewCanonical -> null
    }

    private fun inadmissible(reason: String) = HimNegativeAdmissibilityResultV1(false, reason)

    private fun boundaryOrder(type: HimNegativeBoundaryTypeV1): Int = when (type) {
        HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY -> 1
        HimNegativeBoundaryTypeV1.IDENTITY_VS_VARIANT_BOUNDARY -> 2
        HimNegativeBoundaryTypeV1.ALIAS_VS_SEMANTIC_CHILD_BOUNDARY -> 3
        HimNegativeBoundaryTypeV1.WRONG_RELATION_LEVEL -> 4
        HimNegativeBoundaryTypeV1.WRONG_SCOPE -> 5
        HimNegativeBoundaryTypeV1.WRONG_CLASSIFICATION -> 6
    }
}

object HimNegativeTrainingExampleValidatorV1 {
    fun validate(example: HimNegativeTrainingExampleV1) {
        require(example.contractVersion == HimNegativeTrainingExampleContractV1.VERSION)
        HimTrainingExampleValidatorV1.validate(example.positiveExample)
        require(example.provenance.positiveExampleReference == example.positiveExample.exampleReference) {
            "Negative provenance does not bind the positive training example."
        }
        val alternativeReference = HimNegativeTrainingExampleIdentityV1.alternative(example.rejectedTarget)
        require(example.provenance.rejectedAlternativeReference == alternativeReference) {
            "Negative provenance does not bind the rejected alternative."
        }
        require(example.rejectedTarget != example.positiveExample.target) {
            "A negative example cannot reject the positive target itself."
        }
        HimNegativeTrainingExamplePolicyV1.requireAdmissible(
            positiveExample = example.positiveExample,
            rejectedTarget = example.rejectedTarget,
            boundaryType = example.boundaryType,
        )
        require(example.modelInput() == example.positiveExample.modelInput()) {
            "Negative example changed the model input."
        }
        val expected = HimNegativeTrainingExampleIdentityV1.example(
            positiveExample = example.positiveExample,
            rejectedTarget = example.rejectedTarget,
            boundaryType = example.boundaryType,
        )
        require(example.reference == expected) {
            "Negative example reference is not deterministic for its contents."
        }
        HimTrainingExampleLeakageValidatorV1.validateModelInput(example.modelInput())
    }
}

object HimNegativeTrainingExampleIdentityV1 {
    fun alternative(target: HimTrainingTargetV1): HimNegativeAlternativeReferenceV1 =
        HimNegativeAlternativeReferenceV1("negative-alternative:v1:${sha256(targetKey(target))}")

    fun example(
        positiveExample: HimTrainingExampleV1,
        rejectedTarget: HimTrainingTargetV1,
        boundaryType: HimNegativeBoundaryTypeV1,
    ): HimNegativeTrainingExampleReferenceV1 {
        val canonical = buildString {
            line("contract", HimNegativeTrainingExampleContractV1.EXAMPLE_REFERENCE_CONTRACT)
            line("policy", HimNegativeTrainingExampleContractV1.POLICY_VERSION)
            line("positive-example", positiveExample.exampleReference.value)
            line("boundary", boundaryType.name)
            line("rejected-target", targetKey(rejectedTarget))
        }
        return HimNegativeTrainingExampleReferenceV1("negative-example:v1:${sha256(canonical)}")
    }

    internal fun targetKey(target: HimTrainingTargetV1): String = when (target) {
        is HimTrainingTargetV1.ExistingCanonical -> "EXISTING_CANONICAL|canonical=${target.canonicalId.value}"
        is HimTrainingTargetV1.Identity -> "IDENTITY|parent=${target.parentCanonicalId.value}"
        is HimTrainingTargetV1.Variant -> "VARIANT|${scopeKey(target.scope)}"
        is HimTrainingTargetV1.Alias -> "ALIAS|${scopeKey(target.equivalentEntity)}"
        is HimTrainingTargetV1.NewCanonical -> "NEW_CANONICAL|proposed=${target.proposedCanonicalName.orEmpty()}"
    }

    private fun scopeKey(scope: HimFamilyEntityReference): String = when (scope) {
        is HimFamilyEntityReference.Canonical -> "canonical=${scope.canonicalId.value}"
        is HimFamilyEntityReference.Identity -> "canonical=${scope.canonicalId.value}|identity=${scope.identityId.value}"
    }

    private fun StringBuilder.line(key: String, value: String) {
        append(key).append('=').append(value.length).append(':').append(value).append('\n')
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
