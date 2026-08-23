package de.shopme.testing.system.tools.knowledge.him.training.corpus

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimMutationReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateInputRunReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateRunReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationDecisionReference
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingClassificationV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleIdentityV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleLeakageValidatorV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingEvidenceInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingProvenanceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTaskTypeV1
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RunHimTrainingCorpusContractV1Test {

    @Test
    fun `real Hering golden example is VARIANT scoped to Canonical OzlByp`() {
        val example = heringExample()

        assertEquals("Hering eingelegt", example.input.observedTerm)
        assertEquals(HimTrainingClassificationV1.VARIANT, example.target.classification)
        assertEquals(
            HimFamilyEntityReference.Canonical(HimEntityId("OzlByp")),
            (example.target as HimTrainingTargetV1.Variant).scope,
        )
        assertEquals(HimEntityId("bvoJLp"), example.provenance.promotedEntityId)
        HimTrainingExampleLeakageValidatorV1.validateModelInput(example.modelInput())
    }

    @Test
    fun `same semantic example constructed twice has identical reference`() {
        assertEquals(heringExample().exampleReference, heringExample().exampleReference)
    }

    @Test
    fun `changing observed term changes identity`() {
        val changed = exampleWith(
            input = heringExample().input.copy(observedTerm = "Hering geräuchert"),
        )
        assertNotEquals(heringExample().exampleReference, changed.exampleReference)
    }

    @Test
    fun `changing target classification changes identity`() {
        val changed = exampleWith(
            target = HimTrainingTargetV1.Identity(HimEntityId("OzlByp")),
            provenance = heringExample().provenance.copy(promotedEntityType = HimEntityType.IDENTITY),
        )
        assertNotEquals(heringExample().exampleReference, changed.exampleReference)
    }

    @Test
    fun `changing target scope changes identity`() {
        val changed = exampleWith(
            target = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(HimEntityId("Abc123"))),
        )
        assertNotEquals(heringExample().exampleReference, changed.exampleReference)
    }

    @Test
    fun `answer and infrastructure fields are absent from model input`() {
        val modelInput = heringExample().modelInput()
        val rendered = modelInput.toString()

        listOf(
            "VARIANT",
            "OzlByp",
            "bvoJLp",
            "validation:v1:",
            "promotion:v1:",
            "mutation:v1:",
            "release:v1:",
        ).forEach { forbidden -> assertFalse("$forbidden leaked into model input", rendered.contains(forbidden)) }
        assertTrue(HimTrainingInputV1::class.java.declaredFields.none { it.name == "target" })
        assertTrue(HimTrainingInputV1::class.java.declaredFields.none { it.name == "provenance" })
    }

    @Test
    fun `promoted Entity ID is provenance only`() {
        assertFalse(heringExample().modelInput().toString().contains("bvoJLp"))
        assertEquals("bvoJLp", heringExample().provenance.promotedEntityId?.value)
    }

    @Test
    fun `validation promotion and mutation references are provenance only`() {
        val modelInput = heringExample().modelInput().toString()
        assertFalse(modelInput.contains("validation:v1:"))
        assertFalse(modelInput.contains("promotion:v1:"))
        assertFalse(modelInput.contains("mutation:v1:"))
        assertTrue(heringExample().provenance.validationReference != null)
        assertTrue(heringExample().provenance.promotionReference != null)
        assertTrue(heringExample().provenance.mutationReference != null)
    }

    @Test
    fun `NEW_CANONICAL is representable without a fabricated production ID`() {
        val example = exampleWith(
            input = HimTrainingInputV1("Unbekanntes Essen", "unbekanntes essen"),
            target = HimTrainingTargetV1.NewCanonical("Unbekanntes Essen"),
            provenance = HimTrainingProvenanceV1(),
        )
        assertEquals(HimTrainingClassificationV1.NEW_CANONICAL, example.target.classification)
        assertEquals(null, example.provenance.promotedEntityId)
    }

    @Test
    @Suppress("CAST_NEVER_SUCCEEDS")
    fun `VARIANT without scope hard fails`() {
        assertFails { HimTrainingTargetV1.Variant(null as HimFamilyEntityReference) }
    }

    @Test
    fun `blank observed term hard fails`() {
        assertFails { HimTrainingInputV1(" ", "hering") }
        assertFails { HimTrainingInputV1("Hering", " ") }
    }

    @Test
    fun `duplicate evidence hard fails`() {
        val evidence = evidenceInput().first()
        assertFails { HimTrainingInputV1("Hering eingelegt", "hering eingelegt", evidence = listOf(evidence, evidence)) }
    }

    @Test
    fun `equivalent set-like evidence ordering has identical identity`() {
        val original = heringExample()
        val reversedInput = original.input.copy(evidence = original.input.evidence.reversed())
        val reversedProvenance = original.provenance.copy(
            sourceEvidenceReferences = original.provenance.sourceEvidenceReferences.reversed(),
        )
        val reversed = exampleWith(input = reversedInput, provenance = reversedProvenance)

        assertEquals(original.exampleReference, reversed.exampleReference)
    }

    @Test
    fun `ranked canonical context preserves semantic ordering`() {
        val first = canonicalContext(1, "OzlByp", "Hering")
        val second = canonicalContext(2, "Abc123", "Fisch")
        assertEquals(listOf(1, 2), HimTrainingInputV1("Hering", "hering", listOf(first, second)).canonicalContext.map { it.rank })
        assertFails { HimTrainingInputV1("Hering", "hering", listOf(second, first)) }
    }

    @Test
    fun `inconsistent promoted entity type hard fails`() {
        val original = heringExample()
        assertFails {
            exampleWith(
                provenance = original.provenance.copy(promotedEntityType = HimEntityType.IDENTITY),
            )
        }
    }

    @Test
    fun `structural leakage violation hard fails`() {
        data class LeakyInput(val observedTerm: String, val target: HimTrainingTargetV1)
        assertFails {
            HimTrainingExampleLeakageValidatorV1.validateModelInput(
                LeakyInput("Hering eingelegt", HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(HimEntityId("OzlByp")))),
            )
        }
    }

    @Test
    fun `example identity changes when target scope changes even with same input`() {
        val first = HimTrainingExampleIdentityV1.example(
            taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
            input = heringExample().input,
            target = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(HimEntityId("OzlByp"))),
            provenance = heringExample().provenance,
        )
        val second = HimTrainingExampleIdentityV1.example(
            taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
            input = heringExample().input,
            target = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Identity(HimEntityId("OzlByp"), HimEntityId("Iden01"))),
            provenance = heringExample().provenance,
        )
        assertNotEquals(first, second)
    }

    private fun heringExample(): HimTrainingExampleV1 {
        val evidence = evidenceInput()
        val provenance = HimTrainingProvenanceV1(
            candidateReference = HimCandidateReference("candidate:v1:8df2826510366d3cacc78d81f1ad9b3d81d50ef61d7ff3b29195eaf1ac1fd2ef"),
            generationRunReference = HimCandidateRunReference("run:v2:c1ab93c23dde76444a3585afd80b592038ecf9c14e39442846ddbaf7671a0823"),
            inputRunReference = HimCandidateInputRunReference("input-run:v1:${"1".repeat(64)}"),
            validationReference = HimCandidateValidationDecisionReference("validation:v1:f0b328d718b10878cdbb4cf41ea462f4e8d27ec8b9197ac4ca101792d458cbb4"),
            promotionReference = HimCandidatePromotionReference("promotion:v1:3578a9afcbd7f5a3c2d774301ebf51c07a3835b9c0a81ed657ea084afa6b07c1"),
            mutationReference = HimMutationReference("mutation:v1:${"2".repeat(64)}"),
            groundTruthReleaseReference = HimGroundTruthReleaseIdentityV1("release:v1:e877ccc673527e569520a9ee0932a79f6460ef3fabdb364a38200837e557c5f7"),
            promotedEntityId = HimEntityId("bvoJLp"),
            promotedEntityType = HimEntityType.VARIANT,
            sourceEvidenceReferences = evidence.map { it.reference },
            sourceArtifactDigests = listOf(HimSha256("1".repeat(64))),
        )
        return HimTrainingExampleV1.create(
            taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
            input = HimTrainingInputV1(
                observedTerm = "Hering eingelegt",
                normalizedObservedTerm = "hering eingelegt",
                evidence = evidence,
            ),
            target = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(HimEntityId("OzlByp"))),
            provenance = provenance,
        )
    }

    private fun evidenceInput(): List<HimTrainingEvidenceInputV1> = listOf(
        HimTrainingEvidenceInputV1(
            reference = HimEvidenceReference("OPEN_FOOD_FACTS", HimSha256("1".repeat(64)), "off:product:row:4530988:code:5701157480343"),
            recordKind = "real-pilot-evidence",
            retrievalRank = 1,
        ),
        HimTrainingEvidenceInputV1(
            reference = HimEvidenceReference("AGRIBALYSE", HimSha256("1".repeat(64)), "agribalyse:row:2171:agb:26010"),
            recordKind = "real-pilot-evidence",
            retrievalRank = 1,
        ),
        HimTrainingEvidenceInputV1(
            reference = HimEvidenceReference("CIQUAL", HimSha256("1".repeat(64)), "ciqual:food:26010"),
            recordKind = "real-pilot-evidence",
            retrievalRank = 1,
        ),
    )

    private fun exampleWith(
        input: HimTrainingInputV1 = heringExample().input,
        target: HimTrainingTargetV1 = heringExample().target,
        provenance: HimTrainingProvenanceV1 = heringExample().provenance,
    ) = HimTrainingExampleV1.create(
        taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
        input = input,
        target = target,
        provenance = provenance,
    )

    private fun canonicalContext(rank: Int, id: String, name: String) =
        HimCandidateCanonicalContext(rank, HimEntityId(id), name, null)

    private fun assertFails(block: () -> Unit) {
        try {
            block()
            fail("Expected the contract to reject the value")
        } catch (_: IllegalArgumentException) {
            // Expected hard failure.
        } catch (_: NullPointerException) {
            // Null scope is rejected before a usable target can exist.
        }
    }
}
