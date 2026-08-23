package de.shopme.testing.system.tools.knowledge.him.training.corpus

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimMutationReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationDecisionReference
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeBoundaryTypeV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExampleContractV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExampleIdentityV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExamplePolicyV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingClassificationV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingProvenanceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTaskTypeV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExampleValidatorV1
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RunHimNegativeTrainingExampleContractV1Test {

    @Test
    fun `Hering VARIANT rejects NEW_CANONICAL`() {
        val negative = HimNegativeTrainingExampleV1.create(
            positiveExample = heringPositive(),
            rejectedTarget = HimTrainingTargetV1.NewCanonical(),
            boundaryType = HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY,
        )

        assertEquals(HimTrainingClassificationV1.VARIANT, negative.positiveExample.target.classification)
        assertEquals(HimTrainingClassificationV1.NEW_CANONICAL, negative.rejectedTarget.classification)
        assertEquals(HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY, negative.boundaryType)
        HimNegativeTrainingExampleValidatorV1.validate(negative)
    }

    @Test
    fun `Hering VARIANT rejects IDENTITY at the same Canonical scope`() {
        val negative = HimNegativeTrainingExampleV1.create(
            positiveExample = heringPositive(),
            rejectedTarget = HimTrainingTargetV1.Identity(HERRING_ID),
            boundaryType = HimNegativeBoundaryTypeV1.IDENTITY_VS_VARIANT_BOUNDARY,
        )
        assertEquals(HimTrainingClassificationV1.IDENTITY, negative.rejectedTarget.classification)
        assertEquals(HERRING_ID, (negative.rejectedTarget as HimTrainingTargetV1.Identity).parentCanonicalId)
    }

    @Test
    fun `Hering VARIANT rejects a different Canonical present in context`() {
        val negative = HimNegativeTrainingExampleV1.create(
            positiveExample = heringPositive(),
            rejectedTarget = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(OTHER_CANONICAL_ID)),
            boundaryType = HimNegativeBoundaryTypeV1.WRONG_SCOPE,
        )
        assertEquals(HimFamilyEntityReference.Canonical(OTHER_CANONICAL_ID), (negative.rejectedTarget as HimTrainingTargetV1.Variant).scope)
    }

    @Test
    fun `same derivation produces equal negative content and reference`() {
        val first = HimNegativeTrainingExampleV1.create(
            heringPositive(),
            HimTrainingTargetV1.NewCanonical(),
            HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY,
        )
        val second = HimNegativeTrainingExampleV1.create(
            heringPositive(),
            HimTrainingTargetV1.NewCanonical(),
            HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY,
        )
        assertEquals(first, second)
        assertEquals(first.reference, second.reference)
    }

    @Test
    fun `changing rejected target changes negative reference`() {
        val positive = heringPositive()
        val newCanonical = HimNegativeTrainingExampleV1.create(
            positive,
            HimTrainingTargetV1.NewCanonical(),
            HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY,
        )
        val identity = HimNegativeTrainingExampleV1.create(
            positive,
            HimTrainingTargetV1.Identity(HERRING_ID),
            HimNegativeBoundaryTypeV1.IDENTITY_VS_VARIANT_BOUNDARY,
        )
        assertNotEquals(newCanonical.reference, identity.reference)
    }

    @Test
    fun `changing positive reference changes negative reference`() {
        val first = HimNegativeTrainingExampleV1.create(
            heringPositive(),
            HimTrainingTargetV1.NewCanonical(),
            HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY,
        )
        val second = HimNegativeTrainingExampleV1.create(
            heringPositive(observedTerm = "Hering geräuchert", normalizedTerm = "hering geräuchert"),
            HimTrainingTargetV1.NewCanonical(),
            HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY,
        )
        assertNotEquals(first.positiveExample.exampleReference, second.positiveExample.exampleReference)
        assertNotEquals(first.reference, second.reference)
    }

    @Test
    fun `rejecting the same target hard fails`() {
        assertFails {
            HimNegativeTrainingExampleV1.create(
                heringPositive(),
                heringPositive().target,
                HimNegativeBoundaryTypeV1.WRONG_CLASSIFICATION,
            )
        }
    }

    @Test
    fun `wrong scope with the same scope hard fails`() {
        assertFails {
            HimNegativeTrainingExampleV1.create(
                heringPositive(),
                HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(HERRING_ID)),
                HimNegativeBoundaryTypeV1.WRONG_SCOPE,
            )
        }
    }

    @Test
    fun `invalid positive example hard fails`() {
        val positive = heringPositive()
        assertFails {
            HimTrainingExampleV1(
                exampleReference = positive.exampleReference,
                taskType = positive.taskType,
                input = positive.input.copy(observedTerm = "tampered"),
                target = positive.target,
                provenance = positive.provenance,
            )
        }
    }

    @Test
    fun `non-admissible alternative hard fails`() {
        assertFails {
            HimNegativeTrainingExampleV1.create(
                heringPositive(),
                HimTrainingTargetV1.Alias(HimFamilyEntityReference.Canonical(HERRING_ID)),
                HimNegativeBoundaryTypeV1.WRONG_CLASSIFICATION,
            )
        }
    }

    @Test
    fun `negative derivation keeps model input byte-for-byte equivalent`() {
        val positive = heringPositive()
        val negative = HimNegativeTrainingExampleV1.create(
            positive,
            HimTrainingTargetV1.NewCanonical(),
            HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY,
        )
        assertEquals(positive.modelInput(), negative.modelInput())
        assertEquals(positive.input.observedTerm, negative.modelInput().observedTerm)
        assertEquals(positive.input.canonicalContext, negative.modelInput().canonicalContext)
        assertEquals(positive.input.evidence, negative.modelInput().evidence)
    }

    @Test
    fun `negative metadata and rejected target stay outside model input`() {
        val negative = HimNegativeTrainingExampleV1.create(
            heringPositive(),
            HimTrainingTargetV1.NewCanonical(),
            HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY,
        )
        val renderedInput = negative.modelInput().toString()
        assertFalse(renderedInput.contains("NEW_CANONICAL"))
        assertFalse(renderedInput.contains("CANONICAL_VS_CHILD_BOUNDARY"))
        assertFalse(renderedInput.contains("bvoJLp"))
        assertFalse(renderedInput.contains("validation:v1:"))
        assertFalse(renderedInput.contains("promotion:v1:"))
        assertFalse(renderedInput.contains("mutation:v1:"))
    }

    @Test
    fun `identity-scoped variant rejects canonical-scoped variant at wrong relation level`() {
        val positive = heringPositive(
            target = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Identity(HERRING_ID, IDENTITY_ID)),
        )
        val negative = HimNegativeTrainingExampleV1.create(
            positive,
            HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(HERRING_ID)),
            HimNegativeBoundaryTypeV1.WRONG_RELATION_LEVEL,
        )
        assertEquals(HimNegativeBoundaryTypeV1.WRONG_RELATION_LEVEL, negative.boundaryType)
    }

    @Test
    fun `alias positive supports semantic-child boundary`() {
        val positive = HimTrainingExampleV1.create(
            taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
            input = HimTrainingInputV1(
                observedTerm = "Heringsfisch",
                normalizedObservedTerm = "heringsfisch",
                canonicalContext = listOf(canonicalContext(1, HERRING_ID, "Hering")),
            ),
            target = HimTrainingTargetV1.Alias(HimFamilyEntityReference.Canonical(HERRING_ID)),
        )
        val negative = HimNegativeTrainingExampleV1.create(
            positive,
            HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(HERRING_ID)),
            HimNegativeBoundaryTypeV1.ALIAS_VS_SEMANTIC_CHILD_BOUNDARY,
        )
        assertEquals(HimTrainingClassificationV1.ALIAS, positive.target.classification)
        assertEquals(HimTrainingClassificationV1.VARIANT, negative.rejectedTarget.classification)
    }

    @Test
    fun `derived boundaries use explicit deterministic ordering`() {
        val derived = HimNegativeTrainingExamplePolicyV1.derive(heringPositive())
        assertEquals(
            listOf(
                HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY,
                HimNegativeBoundaryTypeV1.IDENTITY_VS_VARIANT_BOUNDARY,
                HimNegativeBoundaryTypeV1.ALIAS_VS_SEMANTIC_CHILD_BOUNDARY,
                HimNegativeBoundaryTypeV1.WRONG_SCOPE,
            ),
            derived.map { it.boundaryType },
        )
        assertEquals(derived, HimNegativeTrainingExamplePolicyV1.derive(heringPositive()))
    }

    @Test
    fun `equivalent construction ordering preserves negative identity`() {
        val first = HimNegativeTrainingExampleV1.create(
            heringPositive(),
            HimTrainingTargetV1.NewCanonical(),
            HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY,
        )
        val second = HimNegativeTrainingExampleV1.create(
            heringPositive(reversedEvidence = true),
            HimTrainingTargetV1.NewCanonical(),
            HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY,
        )
        assertEquals(first.reference, second.reference)
        assertEquals(first.provenance.rejectedAlternativeReference, second.provenance.rejectedAlternativeReference)
    }

    @Test
    fun `negative identity binds contract policy boundary positive and rejected target`() {
        val positive = heringPositive()
        val negative = HimNegativeTrainingExampleV1.create(
            positive,
            HimTrainingTargetV1.NewCanonical(),
            HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY,
        )
        assertTrue(negative.reference.value.startsWith("negative-example:v1:"))
        assertEquals(HimNegativeTrainingExampleContractV1.POLICY_VERSION, negative.provenance.derivationPolicyVersion)
        assertEquals(positive.exampleReference, negative.provenance.positiveExampleReference)
        assertEquals(
            HimNegativeTrainingExampleIdentityV1.alternative(negative.rejectedTarget),
            negative.provenance.rejectedAlternativeReference,
        )
    }

    private fun heringPositive(
        observedTerm: String = "Hering eingelegt",
        normalizedTerm: String = "hering eingelegt",
        target: HimTrainingTargetV1 = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(HERRING_ID)),
        reversedEvidence: Boolean = false,
    ): HimTrainingExampleV1 {
        val evidence = listOf(
            HimEvidenceReference("OPEN_FOOD_FACTS", ARTIFACT_SHA, "off:product:row:4530988:code:5701157480343") to 1,
            HimEvidenceReference("AGRIBALYSE", ARTIFACT_SHA, "agribalyse:row:2171:agb:26010") to 2,
            HimEvidenceReference("CIQUAL", ARTIFACT_SHA, "ciqual:food:26010") to 3,
        ).let { if (reversedEvidence) it.reversed() else it }
        return HimTrainingExampleV1.create(
            taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
            input = HimTrainingInputV1(
                observedTerm = observedTerm,
                normalizedObservedTerm = normalizedTerm,
                canonicalContext = listOf(
                    canonicalContext(1, HERRING_ID, "Hering"),
                    canonicalContext(2, OTHER_CANONICAL_ID, "Makrele"),
                ),
                evidence = evidence.map { (reference, retrievalRank) ->
                    de.shopme.tools.knowledge.him.training.corpus.HimTrainingEvidenceInputV1(
                        reference = reference,
                        recordKind = "real-pilot-evidence",
                        retrievalRank = retrievalRank,
                    )
                },
            ),
            target = target,
            provenance = HimTrainingProvenanceV1(
                candidateReference = HimCandidateReference("candidate:v1:8df2826510366d3cacc78d81f1ad9b3d81d50ef61d7ff3b29195eaf1ac1fd2ef"),
                generationRunReference = de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateRunReference("run:v2:c1ab93c23dde76444a3585afd80b592038ecf9c14e39442846ddbaf7671a0823"),
                inputRunReference = de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateInputRunReference("input-run:v1:${"1".repeat(64)}"),
                validationReference = HimCandidateValidationDecisionReference("validation:v1:f0b328d718b10878cdbb4cf41ea462f4e8d27ec8b9197ac4ca101792d458cbb4"),
                promotionReference = HimCandidatePromotionReference("promotion:v1:3578a9afcbd7f5a3c2d774301ebf51c07a3835b9c0a81ed657ea084afa6b07c1"),
                mutationReference = HimMutationReference("mutation:v1:935c9e0a864b08cbb885f4abb9000090cd68011e30527cfb6335d2030f9eb576"),
                groundTruthReleaseReference = HimGroundTruthReleaseIdentityV1("release:v1:e877ccc673527e569520a9ee0932a79f6460ef3fabdb364a38200837e557c5f7"),
                promotedEntityId = HimEntityId("bvoJLp"),
                promotedEntityType = HimEntityType.VARIANT,
                sourceEvidenceReferences = evidence.map { it.first },
                sourceArtifactDigests = listOf(ARTIFACT_SHA),
            ),
        )
    }

    private fun canonicalContext(rank: Int, id: HimEntityId, name: String) =
        de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext(rank, id, name, null)

    private fun assertFails(block: () -> Unit) {
        try {
            block()
            fail("Expected hard failure")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    private companion object {
        val HERRING_ID = HimEntityId("OzlByp")
        val OTHER_CANONICAL_ID = HimEntityId("Abc123")
        val IDENTITY_ID = HimEntityId("Iden01")
        val ARTIFACT_SHA = HimSha256("1".repeat(64))
    }
}
