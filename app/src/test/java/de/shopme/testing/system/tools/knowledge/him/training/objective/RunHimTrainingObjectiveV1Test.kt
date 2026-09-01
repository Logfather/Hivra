package de.shopme.testing.system.tools.knowledge.him.training.objective

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeBoundaryTypeV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingClassificationV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingProvenanceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTaskTypeV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTeacherProvenanceV1
import de.shopme.tools.knowledge.him.training.objective.HimTrainingObjectiveV1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RunHimTrainingObjectiveV1Test {

    @Test
    fun validObjectiveContractCreationAndIdentity() {
        val objective = HimTrainingObjectiveV1.create()

        assertEquals(HimTrainingObjectiveV1.CONTRACT_ID, objective.contractId)
        assertEquals(HimTrainingObjectiveV1.VERSION, objective.version)
        assertEquals(HimTrainingObjectiveV1.STATE, objective.state)
        assertTrue(objective.logicalDigest.value.matches(Regex("[0-9a-f]{64}")))
        assertEquals(
            "training-objective:v1:${objective.logicalDigest.value}",
            objective.objectiveReference,
        )
    }

    @Test
    fun objectiveDigestAndReferenceAreDeterministic() {
        val first = HimTrainingObjectiveV1.create()
        val second = HimTrainingObjectiveV1.create()

        assertEquals(first.logicalDigest, second.logicalDigest)
        assertEquals(first.objectiveReference, second.objectiveReference)
    }

    @Test
    fun supportedSemanticTargetKindsAreCompleteAndOrdered() {
        assertEquals(
            listOf(
                HimTrainingClassificationV1.EXISTING_CANONICAL,
                HimTrainingClassificationV1.IDENTITY,
                HimTrainingClassificationV1.VARIANT,
                HimTrainingClassificationV1.ALIAS,
                HimTrainingClassificationV1.NEW_CANONICAL,
            ),
            HimTrainingObjectiveV1.SUPPORTED_TARGET_KINDS,
        )
    }

    @Test
    fun supportedNegativeBoundarySemanticsAreCompleteAndOrdered() {
        assertEquals(
            listOf(
                HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY,
                HimNegativeBoundaryTypeV1.IDENTITY_VS_VARIANT_BOUNDARY,
                HimNegativeBoundaryTypeV1.ALIAS_VS_SEMANTIC_CHILD_BOUNDARY,
                HimNegativeBoundaryTypeV1.WRONG_RELATION_LEVEL,
                HimNegativeBoundaryTypeV1.WRONG_SCOPE,
                HimNegativeBoundaryTypeV1.WRONG_CLASSIFICATION,
            ),
            HimTrainingObjectiveV1.SUPPORTED_NEGATIVE_BOUNDARY_TYPES,
        )
    }

    @Test
    fun semanticLossRolesAreExplicitAndUnweighted() {
        assertEquals(
            listOf(
                HimTrainingObjectiveV1.SemanticLossRole.TARGET_KIND,
                HimTrainingObjectiveV1.SemanticLossRole.TARGET_REFERENCE,
                HimTrainingObjectiveV1.SemanticLossRole.NEGATIVE_BOUNDARY_REJECTION,
            ),
            HimTrainingObjectiveV1.SUPPORTED_LOSS_ROLES,
        )
    }

    @Test
    fun changingSemanticTargetChangesProjectedTargetIdentity() {
        val objective = HimTrainingObjectiveV1.create()

        assertNotEquals(
            objective.projectPositive(positive(HimTrainingTargetV1.ExistingCanonical(CANONICAL_ID))).logicalDigest,
            objective.projectPositive(HimTrainingTargetV1.Identity(CANONICAL_ID).let(::positive)).logicalDigest,
        )
    }

    @Test
    fun existingCanonicalTargetProjectsDeterministically() {
        val target = HimTrainingTargetV1.ExistingCanonical(CANONICAL_ID)
        val result = HimTrainingObjectiveV1.create().projectPositive(positive(target))

        assertEquals(target, result.semanticTarget)
        assertEquals(HimTrainingClassificationV1.EXISTING_CANONICAL, result.semanticTarget.classification)
        assertTrue(result.targetReference.startsWith("objective-target:v1:"))
    }

    @Test
    fun identityTargetProjectsDeterministically() {
        val target = HimTrainingTargetV1.Identity(CANONICAL_ID)
        val result = HimTrainingObjectiveV1.create().projectPositive(positive(target))

        assertEquals(target, result.semanticTarget)
        assertEquals(CANONICAL_ID, (result.semanticTarget as HimTrainingTargetV1.Identity).parentCanonicalId)
    }

    @Test
    fun canonicalVariantTargetProjectsDeterministically() {
        val target = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(CANONICAL_ID))
        val result = HimTrainingObjectiveV1.create().projectPositive(positive(target))

        assertEquals(target, result.semanticTarget)
        assertEquals(
            HimFamilyEntityReference.Canonical(CANONICAL_ID),
            (result.semanticTarget as HimTrainingTargetV1.Variant).scope,
        )
    }

    @Test
    fun identityVariantTargetProjectsDeterministically() {
        val target = HimTrainingTargetV1.Variant(
            HimFamilyEntityReference.Identity(CANONICAL_ID, IDENTITY_ID),
        )
        val result = HimTrainingObjectiveV1.create().projectPositive(positive(target))

        assertEquals(target, result.semanticTarget)
        assertEquals(
            HimFamilyEntityReference.Identity(CANONICAL_ID, IDENTITY_ID),
            (result.semanticTarget as HimTrainingTargetV1.Variant).scope,
        )
    }

    @Test
    fun canonicalAliasTargetProjectsDeterministically() {
        val target = HimTrainingTargetV1.Alias(HimFamilyEntityReference.Canonical(CANONICAL_ID))
        val result = HimTrainingObjectiveV1.create().projectPositive(positive(target))

        assertEquals(target, result.semanticTarget)
        assertEquals(
            HimFamilyEntityReference.Canonical(CANONICAL_ID),
            (result.semanticTarget as HimTrainingTargetV1.Alias).equivalentEntity,
        )
    }

    @Test
    fun identityAliasTargetProjectsDeterministically() {
        val target = HimTrainingTargetV1.Alias(
            HimFamilyEntityReference.Identity(CANONICAL_ID, IDENTITY_ID),
        )
        val result = HimTrainingObjectiveV1.create().projectPositive(positive(target))

        assertEquals(target, result.semanticTarget)
        assertEquals(
            HimFamilyEntityReference.Identity(CANONICAL_ID, IDENTITY_ID),
            (result.semanticTarget as HimTrainingTargetV1.Alias).equivalentEntity,
        )
    }

    @Test
    fun namedNewCanonicalProjectsWithoutKnownAuthorityLabel() {
        val target = HimTrainingTargetV1.NewCanonical("Unbekanntes Essen")
        val result = HimTrainingObjectiveV1.create().projectPositive(positive(target))

        assertEquals(target, result.semanticTarget)
        assertEquals("Unbekanntes Essen", (result.semanticTarget as HimTrainingTargetV1.NewCanonical).proposedCanonicalName)
    }

    @Test
    fun unnamedNewCanonicalProjectsAsStructuralCategoryOnly() {
        val result = HimTrainingObjectiveV1.create().projectPositive(positive(HimTrainingTargetV1.NewCanonical()))

        assertTrue(result.semanticTarget is HimTrainingTargetV1.NewCanonical)
        assertEquals(null, (result.semanticTarget as HimTrainingTargetV1.NewCanonical).proposedCanonicalName)
    }

    @Test
    fun negativeSupervisionProjectsDeterministically() {
        val objective = HimTrainingObjectiveV1.create()
        val negative = canonicalChildNegative()
        val first = objective.projectNegative(negative)
        val second = objective.projectNegative(negative)

        assertEquals(first, second)
        assertEquals(negative.boundaryType, first.boundaryType)
        assertEquals(negative.rejectedTarget, first.rejectedTarget)
        assertTrue(first.targetReference.startsWith("objective-negative-target:v1:"))
    }

    @Test
    fun negativeProjectionPreservesAuthoritativeAndRejectedTargets() {
        val negative = canonicalChildNegative()
        val result = HimTrainingObjectiveV1.create().projectNegative(negative)

        assertEquals(negative.positiveExample.target, result.positiveTarget.semanticTarget)
        assertEquals(negative.rejectedTarget, result.rejectedTarget)
        assertEquals(HimTrainingClassificationV1.NEW_CANONICAL, result.rejectedTarget.classification)
    }

    @Test
    fun changingRejectedTargetChangesNegativeTargetIdentity() {
        val objective = HimTrainingObjectiveV1.create()
        val first = objective.projectNegative(canonicalChildNegative("Neue Nahrung"))
        val second = objective.projectNegative(canonicalChildNegative("Andere Nahrung"))

        assertNotEquals(first.logicalDigest, second.logicalDigest)
        assertNotEquals(first.targetReference, second.targetReference)
    }

    @Test
    fun changingNegativeBoundarySemanticsChangesNegativeTargetIdentity() {
        val objective = HimTrainingObjectiveV1.create()
        val canonicalBoundary = objective.projectNegative(canonicalChildNegative())
        val identityBoundary = objective.projectNegative(identityVariantNegative())

        assertNotEquals(canonicalBoundary.boundaryType, identityBoundary.boundaryType)
        assertNotEquals(canonicalBoundary.logicalDigest, identityBoundary.logicalDigest)
    }

    @Test
    fun sameSemanticTargetCanBeSharedByDifferentExamples() {
        val objective = HimTrainingObjectiveV1.create()
        val target = HimTrainingTargetV1.ExistingCanonical(CANONICAL_ID)
        val first = objective.projectPositive(positive(target, observedTerm = "Hering"))
        val second = objective.projectPositive(positive(target, observedTerm = "Fisch"))

        assertEquals(first.logicalDigest, second.logicalDigest)
        assertEquals(first.targetReference, second.targetReference)
    }

    @Test
    fun objectiveTargetIdentityExcludesIrrelevantProvenance() {
        val objective = HimTrainingObjectiveV1.create()
        val target = HimTrainingTargetV1.ExistingCanonical(CANONICAL_ID)
        val first = objective.projectPositive(positive(target))
        val second = objective.projectPositive(
            positive(
                target,
                provenance = HimTrainingProvenanceV1(
                    teacher = HimTrainingTeacherProvenanceV1("fixture", "fixture"),
                ),
            ),
        )

        assertEquals(first.logicalDigest, second.logicalDigest)
        assertEquals(first.targetReference, second.targetReference)
    }

    @Test
    fun objectiveDoesNotDefineDatasetSpecificNumericLabelIndex() {
        assertFalse(
            HimTrainingObjectiveV1::class.java.declaredFields.any { field ->
                field.name.contains("label", ignoreCase = true) || field.name.contains("index", ignoreCase = true)
            },
        )
    }

    @Test
    fun objectiveContainsNoTokenizerLogic() {
        assertTrue(
            HimTrainingObjectiveV1::class.java.declaredMethods.none { method ->
                method.name.contains("token", ignoreCase = true)
            },
        )
    }

    @Test
    fun objectiveContainsNoModelBindingDependency() {
        assertTrue(
            HimTrainingObjectiveV1::class.java.declaredFields.none { field ->
                field.type.name.contains("HimModelBinding", ignoreCase = true)
            },
        )
    }

    @Test
    fun objectiveContainsNoFrameworkPersistenceOrExecutionSurface() {
        val names = HimTrainingObjectiveV1::class.java.declaredMethods.map { it.name }

        assertTrue(names.none { it.contains("torch", ignoreCase = true) })
        assertTrue(names.none { it.contains("persist", ignoreCase = true) })
        assertTrue(names.none { it.contains("execute", ignoreCase = true) })
        assertTrue(names.none { it.contains("train", ignoreCase = true) })
        assertTrue(names.none { it.contains("network", ignoreCase = true) })
    }

    @Test
    fun abstentionIsReservedOnlyWithoutFabricatedTrainingLabels() {
        val objective = HimTrainingObjectiveV1.create()

        assertEquals(HimTrainingObjectiveV1.AbstentionSupport.RESERVED_ONLY, objective.abstentionSupport)
        assertFalse(
            objective.supportedTargetKinds.any { it.name.contains("ABSTAIN") || it.name.contains("UNKNOWN") },
        )
    }

    @Test
    fun objectivePreservesExistingEntityReferenceTypes() {
        val target = HimTrainingTargetV1.Alias(
            HimFamilyEntityReference.Identity(CANONICAL_ID, IDENTITY_ID),
        )
        val projected = HimTrainingObjectiveV1.create().projectPositive(positive(target))

        assertTrue(projected.semanticTarget is HimTrainingTargetV1.Alias)
        assertTrue(
            (projected.semanticTarget as HimTrainingTargetV1.Alias).equivalentEntity is
                HimFamilyEntityReference.Identity,
        )
    }

    @Test
    fun negativeProjectionPreservesTypedBoundaryAndReferenceData() {
        val negative = wrongScopeNegative()
        val projected = HimTrainingObjectiveV1.create().projectNegative(negative)

        assertEquals(HimNegativeBoundaryTypeV1.WRONG_SCOPE, projected.boundaryType)
        assertTrue(projected.rejectedTarget is HimTrainingTargetV1.Variant)
        assertTrue(
            (projected.rejectedTarget as HimTrainingTargetV1.Variant).scope is
                HimFamilyEntityReference.Canonical,
        )
        assertEquals(
            negative.rejectedTarget,
            projected.rejectedTarget,
        )
    }

    @Test
    fun allCurrentNegativeBoundaryTypesHaveObjectiveSemantics() {
        val objective = HimTrainingObjectiveV1.create()

        listOf(
            canonicalChildNegative(),
            identityVariantNegative(),
            aliasChildNegative(),
            wrongRelationLevelNegative(),
            wrongScopeNegative(),
            wrongClassificationNegative(),
        ).forEach { negative ->
            assertEquals(negative.boundaryType, objective.projectNegative(negative).boundaryType)
        }
    }

    @Test
    fun closedTaskDomainHasNoUnsupportedFallback() {
        assertEquals(
            listOf(HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION),
            HimTrainingTaskTypeV1.entries,
        )
        assertTrue(
            HimTrainingObjectiveV1.SUPPORTED_TARGET_KINDS.containsAll(
                HimTrainingClassificationV1.entries,
            ),
        )
    }

    private fun positive(
        target: HimTrainingTargetV1,
        observedTerm: String = "Hering",
        canonicalContext: List<de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext> = emptyList(),
        provenance: HimTrainingProvenanceV1 = HimTrainingProvenanceV1(),
    ): HimTrainingExampleV1 = HimTrainingExampleV1.create(
        taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
        input = HimTrainingInputV1(observedTerm, observedTerm.lowercase(), canonicalContext),
        target = target,
        provenance = provenance,
    )

    private fun canonicalChildNegative(proposedName: String = "Neue Nahrung") =
        HimNegativeTrainingExampleV1.create(
            positiveExample = positive(HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(CANONICAL_ID))),
            rejectedTarget = HimTrainingTargetV1.NewCanonical(proposedName),
            boundaryType = HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY,
        )

    private fun identityVariantNegative() = HimNegativeTrainingExampleV1.create(
        positiveExample = positive(HimTrainingTargetV1.Identity(CANONICAL_ID)),
        rejectedTarget = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(CANONICAL_ID)),
        boundaryType = HimNegativeBoundaryTypeV1.IDENTITY_VS_VARIANT_BOUNDARY,
    )

    private fun aliasChildNegative() = HimNegativeTrainingExampleV1.create(
        positiveExample = positive(HimTrainingTargetV1.Alias(HimFamilyEntityReference.Canonical(CANONICAL_ID))),
        rejectedTarget = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(CANONICAL_ID)),
        boundaryType = HimNegativeBoundaryTypeV1.ALIAS_VS_SEMANTIC_CHILD_BOUNDARY,
    )

    private fun wrongRelationLevelNegative() = HimNegativeTrainingExampleV1.create(
        positiveExample = positive(
            HimTrainingTargetV1.Variant(HimFamilyEntityReference.Identity(CANONICAL_ID, IDENTITY_ID)),
        ),
        rejectedTarget = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(CANONICAL_ID)),
        boundaryType = HimNegativeBoundaryTypeV1.WRONG_RELATION_LEVEL,
    )

    private fun wrongScopeNegative() = HimNegativeTrainingExampleV1.create(
        positiveExample = positive(
            target = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(CANONICAL_ID)),
            canonicalContext = listOf(
                de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext(
                    1,
                    CANONICAL_ID,
                    "Hering",
                    null,
                ),
                de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext(
                    2,
                    OTHER_CANONICAL_ID,
                    "Fisch",
                    null,
                ),
            ),
        ),
        rejectedTarget = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(OTHER_CANONICAL_ID)),
        boundaryType = HimNegativeBoundaryTypeV1.WRONG_SCOPE,
    )

    private fun wrongClassificationNegative() = HimNegativeTrainingExampleV1.create(
        positiveExample = positive(HimTrainingTargetV1.ExistingCanonical(CANONICAL_ID)),
        rejectedTarget = HimTrainingTargetV1.Identity(CANONICAL_ID),
        boundaryType = HimNegativeBoundaryTypeV1.WRONG_CLASSIFICATION,
    )

    private companion object {
        val CANONICAL_ID = HimEntityId("OzlByp")
        val IDENTITY_ID = HimEntityId("Iden01")
        val OTHER_CANONICAL_ID = HimEntityId("Abc123")
    }
}
