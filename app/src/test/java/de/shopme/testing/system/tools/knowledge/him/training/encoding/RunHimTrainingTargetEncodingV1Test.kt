package de.shopme.testing.system.tools.knowledge.him.training.encoding

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeBoundaryTypeV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingClassificationV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTaskTypeV1
import de.shopme.tools.knowledge.him.training.encoding.HimTrainingTargetEncodingV1
import de.shopme.tools.knowledge.him.training.objective.HimTrainingObjectiveV1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RunHimTrainingTargetEncodingV1Test {

    @Test
    fun validEncodingContractIdentity() {
        val encoding = HimTrainingTargetEncodingV1.create()

        assertEquals(HimTrainingTargetEncodingV1.CONTRACT_ID, encoding.contractId)
        assertEquals(HimTrainingTargetEncodingV1.VERSION, encoding.version)
        assertEquals(HimTrainingTargetEncodingV1.STATE, encoding.state)
        assertEquals(HimTrainingObjectiveV1.create().logicalDigest, encoding.objectiveDigest)
        assertEquals(
            "training-target-encoding:v1:${encoding.logicalDigest.value}",
            encoding.encodingReference,
        )
    }

    @Test
    fun encodingContractDigestAndReferenceAreDeterministic() {
        val first = HimTrainingTargetEncodingV1.create()
        val second = HimTrainingTargetEncodingV1.create()

        assertEquals(first.logicalDigest, second.logicalDigest)
        assertEquals(first.encodingReference, second.encodingReference)
    }

    @Test
    fun positiveSemanticCodesAreExactlyFrozen() {
        assertEquals(
            mapOf(
                HimTrainingClassificationV1.EXISTING_CANONICAL to 1,
                HimTrainingClassificationV1.IDENTITY to 2,
                HimTrainingClassificationV1.VARIANT to 3,
                HimTrainingClassificationV1.ALIAS to 4,
                HimTrainingClassificationV1.NEW_CANONICAL to 5,
            ),
            HimTrainingTargetEncodingV1.POSITIVE_TARGET_KIND_CODES,
        )
    }

    @Test
    fun negativeBoundaryCodesAreExactlyFrozen() {
        assertEquals(
            mapOf(
                HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY to 1,
                HimNegativeBoundaryTypeV1.IDENTITY_VS_VARIANT_BOUNDARY to 2,
                HimNegativeBoundaryTypeV1.ALIAS_VS_SEMANTIC_CHILD_BOUNDARY to 3,
                HimNegativeBoundaryTypeV1.WRONG_RELATION_LEVEL to 4,
                HimNegativeBoundaryTypeV1.WRONG_SCOPE to 5,
                HimNegativeBoundaryTypeV1.WRONG_CLASSIFICATION to 6,
            ),
            HimTrainingTargetEncodingV1.NEGATIVE_BOUNDARY_CODES,
        )
    }

    @Test
    fun semanticCodesAreExplicitRatherThanEnumOrdinals() {
        assertEquals(setOf(1, 2, 3, 4, 5), HimTrainingTargetEncodingV1.POSITIVE_TARGET_KIND_CODES.values.toSet())
        assertEquals(setOf(1, 2, 3, 4, 5, 6), HimTrainingTargetEncodingV1.NEGATIVE_BOUNDARY_CODES.values.toSet())
        assertEquals(setOf(1, 2, 3), HimTrainingTargetEncodingV1.LOSS_ROLE_CODES.values.toSet())
    }

    @Test
    fun existingCanonicalTargetEncodesDeterministically() {
        val target = HimTrainingTargetV1.ExistingCanonical(CANONICAL_ID)
        val first = encoder.encodePositive(objective.projectPositive(positive(target)))
        val second = encoder.encodePositive(objective.projectPositive(positive(target)))

        assertEquals(first, second)
        assertTrue(first.semanticTarget is HimTrainingTargetEncodingV1.SemanticTarget.ExistingCanonical)
        assertEquals(1, first.targetKindCode)
        assertEquals(CANONICAL_ID, (first.semanticTarget as HimTrainingTargetEncodingV1.SemanticTarget.ExistingCanonical).canonicalId)
    }

    @Test
    fun identityTargetEncodesDeterministically() {
        val result = encoder.encodePositive(
            objective.projectPositive(positive(HimTrainingTargetV1.Identity(CANONICAL_ID))),
        )

        assertEquals(2, result.targetKindCode)
        assertEquals(
            CANONICAL_ID,
            (result.semanticTarget as HimTrainingTargetEncodingV1.SemanticTarget.Identity).parentCanonicalId,
        )
    }

    @Test
    fun variantTargetEncodesTypedCanonicalScope() {
        val result = encoder.encodePositive(
            objective.projectPositive(
                positive(HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(CANONICAL_ID))),
            ),
        )

        val target = result.semanticTarget as HimTrainingTargetEncodingV1.SemanticTarget.Variant
        assertEquals(3, result.targetKindCode)
        assertEquals(
            HimTrainingTargetEncodingV1.FamilyReference.Canonical(CANONICAL_ID),
            target.scope,
        )
    }

    @Test
    fun aliasTargetEncodesTypedIdentityScope() {
        val result = encoder.encodePositive(
            objective.projectPositive(
                positive(
                    HimTrainingTargetV1.Alias(
                        HimFamilyEntityReference.Identity(CANONICAL_ID, IDENTITY_ID),
                    ),
                ),
            ),
        )

        val target = result.semanticTarget as HimTrainingTargetEncodingV1.SemanticTarget.Alias
        assertEquals(4, result.targetKindCode)
        assertEquals(
            HimTrainingTargetEncodingV1.FamilyReference.Identity(CANONICAL_ID, IDENTITY_ID),
            target.equivalentEntity,
        )
    }

    @Test
    fun newCanonicalTargetPreservesNullableName() {
        val result = encoder.encodePositive(
            objective.projectPositive(positive(HimTrainingTargetV1.NewCanonical("Neue Nahrung"))),
        )

        assertEquals(5, result.targetKindCode)
        assertEquals(
            "Neue Nahrung",
            (result.semanticTarget as HimTrainingTargetEncodingV1.SemanticTarget.NewCanonical).proposedCanonicalName,
        )
    }

    @Test
    fun newCanonicalHasNoEntityIndex() {
        val target = encoder.encodePositive(
            objective.projectPositive(positive(HimTrainingTargetV1.NewCanonical())),
        ).semanticTarget

        assertTrue(target is HimTrainingTargetEncodingV1.SemanticTarget.NewCanonical)
        assertEquals(HimTrainingTargetEncodingV1.NEW_CANONICAL_ENTITY_INDEX, "NONE")
        assertFalse(target::class.java.declaredFields.any { it.name.contains("entityIndex", ignoreCase = true) })
    }

    @Test
    fun newCanonicalHasNoSequenceGenerationTarget() {
        val target = encoder.encodePositive(
            objective.projectPositive(positive(HimTrainingTargetV1.NewCanonical())),
        ).semanticTarget

        assertFalse(target::class.java.declaredFields.any { it.name.contains("sequence", ignoreCase = true) })
        assertEquals("NO", HimTrainingTargetEncodingV1.NEW_CANONICAL_GENERATION_TARGET)
    }

    @Test
    fun negativeTargetEncodesDeterministically() {
        val negative = canonicalChildNegative()
        val first = encoder.encodeNegative(objective.projectNegative(negative))
        val second = encoder.encodeNegative(objective.projectNegative(negative))

        assertEquals(first, second)
        assertEquals(1, first.boundaryTypeCode)
        assertEquals(HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY, first.boundaryType)
        assertTrue(first.rejectedTarget is HimTrainingTargetEncodingV1.SemanticTarget.NewCanonical)
    }

    @Test
    fun rejectedTargetChangesEncodedTargetDigest() {
        val first = encoder.encodeNegative(objective.projectNegative(canonicalChildNegative("Neue Nahrung")))
        val second = encoder.encodeNegative(objective.projectNegative(canonicalChildNegative("Andere Nahrung")))

        assertNotEquals(first.logicalDigest, second.logicalDigest)
        assertNotEquals(first.targetReference, second.targetReference)
    }

    @Test
    fun negativeBoundaryChangesEncodedTargetCodeAndDigest() {
        val first = encoder.encodeNegative(objective.projectNegative(canonicalChildNegative()))
        val second = encoder.encodeNegative(objective.projectNegative(identityVariantNegative()))

        assertNotEquals(first.boundaryTypeCode, second.boundaryTypeCode)
        assertNotEquals(first.logicalDigest, second.logicalDigest)
    }

    @Test
    fun objectiveDigestIsBoundIntoEveryEncodedTarget() {
        val positive = encoder.encodePositive(objective.projectPositive(positive(HimTrainingTargetV1.Identity(CANONICAL_ID))))
        val negative = encoder.encodeNegative(objective.projectNegative(canonicalChildNegative()))

        assertEquals(encoder.objectiveDigest, positive.objectiveDigest)
        assertEquals(encoder.objectiveDigest, negative.objectiveDigest)
        assertEquals(encoder.objectiveReference, positive.objectiveReference)
        assertEquals(encoder.objectiveReference, negative.objectiveReference)
    }

    @Test
    fun forgedObjectiveProjectionFailsClosed() {
        val projected = objective.projectPositive(positive(HimTrainingTargetV1.Identity(CANONICAL_ID)))
        val forged = projected.copy(logicalDigest = HimSha256("0".repeat(64)))

        assertFailsWith<IllegalArgumentException> { encoder.encodePositive(forged) }
    }

    @Test
    fun encoderAcceptsOnlyObjectiveProjectionTypes() {
        val methods = HimTrainingTargetEncodingV1::class.java.declaredMethods
            .filter { it.name == "encodePositive" || it.name == "encodeNegative" }

        assertTrue(methods.isNotEmpty())
        assertTrue(
            methods.flatMap { it.parameterTypes.toList() }.all { type ->
                type.name.contains("HimTrainingObjectiveV1")
            },
        )
    }

    @Test
    fun rawSemanticTargetIsNotAcceptedAsEncodingInput() {
        val parameters = HimTrainingTargetEncodingV1::class.java.declaredMethods
            .filter { it.name == "encodePositive" }
            .flatMap { it.parameterTypes.toList() }

        assertFalse(parameters.any { it.name.contains("HimTrainingTargetV1") })
    }

    @Test
    fun rawNegativeExampleIsNotAcceptedAsEncodingInput() {
        val parameters = HimTrainingTargetEncodingV1::class.java.declaredMethods
            .filter { it.name == "encodeNegative" }
            .flatMap { it.parameterTypes.toList() }

        assertFalse(parameters.any { it.name.contains("HimNegativeTrainingExampleV1") })
    }

    @Test
    fun noDatasetEntityLabelMapExists() {
        assertEquals(
            setOf(
                HimTrainingClassificationV1.EXISTING_CANONICAL,
                HimTrainingClassificationV1.IDENTITY,
                HimTrainingClassificationV1.VARIANT,
                HimTrainingClassificationV1.ALIAS,
                HimTrainingClassificationV1.NEW_CANONICAL,
            ),
            encoder.positiveTargetKindCodes.keys,
        )
        assertEquals("NO", HimTrainingTargetEncodingV1.GLOBAL_DATASET_ENTITY_LABEL_INDEX)
        assertEquals(0, HimTrainingTargetEncodingV1.ENTITY_INDEX_ASSIGNMENT)
    }

    @Test
    fun noTensorRepresentationExists() {
        assertEquals(0, HimTrainingTargetEncodingV1.TENSOR_REPRESENTATION)
        assertFalse(
            HimTrainingTargetEncodingV1::class.java.declaredFields.any { field ->
                field.type == FloatArray::class.java || field.type == DoubleArray::class.java
            },
        )
    }

    @Test
    fun noTokenizerOrInputEncodingExists() {
        assertEquals(0, HimTrainingTargetEncodingV1.INPUT_TOKEN_ENCODING)
        assertFalse(
            HimTrainingTargetEncodingV1::class.java.declaredMethods.any { method ->
                method.name.contains("token", ignoreCase = true) || method.name.contains("input", ignoreCase = true)
            },
        )
    }

    @Test
    fun noModelOutputShapeBindingExists() {
        assertEquals(0, HimTrainingTargetEncodingV1.MODEL_OUTPUT_SHAPE_BINDING)
        assertFalse(
            HimTrainingTargetEncodingV1::class.java.declaredFields.any { field ->
                field.name.contains("hidden", ignoreCase = true) || field.name.contains("outputShape", ignoreCase = true)
            },
        )
    }

    @Test
    fun abstentionRemainsReservedOnly() {
        assertEquals(
            HimTrainingObjectiveV1.AbstentionSupport.RESERVED_ONLY,
            encoder.abstentionSupport,
        )
        assertEquals("RESERVED_ONLY", HimTrainingTargetEncodingV1.ABSTENTION_NUMERICAL_SEMANTICS)
    }

    @Test
    fun noActiveAbstentionTargetCodeExists() {
        assertNull(HimTrainingTargetEncodingV1.ACTIVE_ABSTENTION_TARGET_CODE)
        assertNull(encoder.activeAbstentionTargetCode)
    }

    @Test
    fun semanticCodeReverseMappingsAreDeterministic() {
        assertEquals(
            HimTrainingClassificationV1.VARIANT,
            HimTrainingTargetEncodingV1.positiveTargetKindForCode(3),
        )
        assertEquals(
            HimNegativeBoundaryTypeV1.WRONG_SCOPE,
            HimTrainingTargetEncodingV1.negativeBoundaryForCode(5),
        )
        assertEquals(
            HimTrainingObjectiveV1.SemanticLossRole.TARGET_REFERENCE,
            HimTrainingTargetEncodingV1.lossRoleForCode(2),
        )
        assertNull(HimTrainingTargetEncodingV1.positiveTargetKindForCode(99))
    }

    @Test
    fun nullNewCanonicalNameIsDistinctFromNamedTarget() {
        val unnamed = encoder.encodePositive(
            objective.projectPositive(positive(HimTrainingTargetV1.NewCanonical())),
        )
        val named = encoder.encodePositive(
            objective.projectPositive(positive(HimTrainingTargetV1.NewCanonical("Neue Nahrung"))),
        )

        assertNull((unnamed.semanticTarget as HimTrainingTargetEncodingV1.SemanticTarget.NewCanonical).proposedCanonicalName)
        assertEquals(
            "Neue Nahrung",
            (named.semanticTarget as HimTrainingTargetEncodingV1.SemanticTarget.NewCanonical).proposedCanonicalName,
        )
        assertNotEquals(unnamed.logicalDigest, named.logicalDigest)
    }

    @Test
    fun targetContentIdentityExcludesExampleLineageAndProvenance() {
        val target = HimTrainingTargetV1.ExistingCanonical(CANONICAL_ID)
        val first = encoder.encodePositive(objective.projectPositive(positive(target, "Hering")))
        val second = encoder.encodePositive(objective.projectPositive(positive(target, "Fisch")))

        assertEquals(first.logicalDigest, second.logicalDigest)
        assertEquals(first.targetReference, second.targetReference)
    }

    @Test
    fun positiveTargetCarriesAllRelevantLossRoles() {
        val result = encoder.encodePositive(
            objective.projectPositive(positive(HimTrainingTargetV1.ExistingCanonical(CANONICAL_ID))),
        )

        assertEquals(listOf(1, 2), result.lossRoleCodes)
    }

    @Test
    fun negativeTargetCarriesBoundaryRejectionLossRole() {
        val result = encoder.encodeNegative(objective.projectNegative(canonicalChildNegative()))

        assertEquals(listOf(1, 2, 3), result.lossRoleCodes)
    }

    @Test
    fun positiveReferenceIsBoundToEncodedDigest() {
        val result = encoder.encodePositive(
            objective.projectPositive(positive(HimTrainingTargetV1.ExistingCanonical(CANONICAL_ID))),
        )

        assertEquals("training-encoded-positive-target:v1:${result.logicalDigest.value}", result.targetReference)
    }

    @Test
    fun negativeReferenceIsBoundToEncodedDigest() {
        val result = encoder.encodeNegative(objective.projectNegative(canonicalChildNegative()))

        assertEquals("training-encoded-negative-target:v1:${result.logicalDigest.value}", result.targetReference)
    }

    @Test
    fun semanticReferencesRemainTyped() {
        val variant = encoder.encodePositive(
            objective.projectPositive(
                positive(HimTrainingTargetV1.Variant(HimFamilyEntityReference.Identity(CANONICAL_ID, IDENTITY_ID))),
            ),
        )

        val scope = (variant.semanticTarget as HimTrainingTargetEncodingV1.SemanticTarget.Variant).scope
        assertTrue(scope is HimTrainingTargetEncodingV1.FamilyReference.Identity)
        assertEquals(IDENTITY_ID, scope.identityId)
    }

    @Test
    fun unsupportedReverseCodesFailAsNull() {
        assertNull(HimTrainingTargetEncodingV1.negativeBoundaryForCode(0))
        assertNull(HimTrainingTargetEncodingV1.negativeBoundaryForCode(7))
        assertNull(HimTrainingTargetEncodingV1.lossRoleForCode(-1))
    }

    @Test
    fun encodingHasNoPersistenceNetworkTrainingOrExecutionSurface() {
        val names = HimTrainingTargetEncodingV1::class.java.declaredMethods.map { it.name }

        assertTrue(names.none { name ->
            name.contains("persist", ignoreCase = true) ||
                name.contains("network", ignoreCase = true) ||
                name.contains("train", ignoreCase = true) ||
                name.contains("execute", ignoreCase = true)
        })
    }

    private fun positive(
        target: HimTrainingTargetV1,
        observedTerm: String = "Hering",
    ): HimTrainingExampleV1 = HimTrainingExampleV1.create(
        taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
        input = HimTrainingInputV1(observedTerm, observedTerm.lowercase()),
        target = target,
    )

    private fun canonicalChildNegative(name: String = "Neue Nahrung") = HimNegativeTrainingExampleV1.create(
        positiveExample = positive(
            HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(CANONICAL_ID)),
        ),
        rejectedTarget = HimTrainingTargetV1.NewCanonical(name),
        boundaryType = HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY,
    )

    private fun identityVariantNegative() = HimNegativeTrainingExampleV1.create(
        positiveExample = positive(HimTrainingTargetV1.Identity(CANONICAL_ID)),
        rejectedTarget = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(CANONICAL_ID)),
        boundaryType = HimNegativeBoundaryTypeV1.IDENTITY_VS_VARIANT_BOUNDARY,
    )

    private companion object {
        val CANONICAL_ID = HimEntityId("OzlByp")
        val IDENTITY_ID = HimEntityId("Iden01")
        val objective = HimTrainingObjectiveV1.create()
        val encoder = HimTrainingTargetEncodingV1.create(objective)
    }
}
