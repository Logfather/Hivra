package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.*
import org.junit.Assert.*
import org.junit.Test

class RunHimCandidateDatasetProvenanceContractTest {
    @Test fun `f3d5 candidate identity and retention remain frozen`() {
        assertTrue(HimCandidatePersistencePolicyV2.persists(HimCandidateConfidence.HIGH))
        assertTrue(HimCandidatePersistencePolicyV2.persists(HimCandidateConfidence.MEDIUM))
        assertFalse(HimCandidatePersistencePolicyV2.persists(HimCandidateConfidence.LOW))
        assertFalse(HimCandidatePersistencePolicyV2.persists(HimCandidateConfidence.NO_CONFIDENCE))
        assertFalse(HimCandidatePersistencePolicyV2.novelCandidateAllowed(true))
        val identity = HimCandidateRelation.Identity(HimEntityId("APPLE1"))
        val alias = HimCandidateRelation.Alias(HimFamilyEntityReference.Canonical(HimEntityId("APPLE1")))
        val reference = HimCandidateIdentityV1.candidate("braeburn", identity)
        assertEquals(reference, HimCandidateIdentityV1.candidate("braeburn", identity))
        assertNotEquals(reference, HimCandidateIdentityV1.candidate("braeburn", alias))
        assertTrue(reference.value.matches(Regex("candidate:v1:[0-9a-f]{64}")))
    }

    @Test fun `v2 identities are deterministic and separate`() {
        val inference = RunHimPerInputRunProvenanceAmendmentTest.inferenceFixture()
        val inputSet = HimSha256(HimCandidateIdentityV1.sha256("Kartoffelbrot\n"))
        val run = HimCandidateIdentityV1.run("F3D-FIXTURE", inputSet, inference)
        val input = HimCandidateInputProvenance("Kartoffelbrot", "kartoffelbrot")
        assertEquals(run, HimCandidateIdentityV1.run("F3D-FIXTURE", inputSet, inference))
        assertEquals(HimCandidateIdentityV1.inputRun(run, input), HimCandidateIdentityV1.inputRun(run, input))
        assertNotEquals(HimCandidateIdentityV1.inputRun(run, input), HimCandidateIdentityV1.inputRun(run, HimCandidateInputProvenance("Brot", "brot")))
    }
}
