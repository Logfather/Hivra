package de.shopme.tools.knowledge.him.canonical.family.groundtruth

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class HimGroundTruthCandidateContractTest {

    @Test
    fun candidateReferencesExistingEntitiesWithoutAllocatingEntityId() {
        val candidate =
            HimGroundTruthCandidate(
                candidateReference = HimCandidateReference("candidate-1"),
                candidateTerm = "Braeburn",
                relation =
                    HimCandidateRelation.Identity(
                        parentCanonicalId = HimEntityId("Apple1"),
                    ),
                evidenceReferences = emptyList(),
                candidateConfidence = HimCandidateConfidence.HIGH,
            )

        assertEquals(HimCandidateType.IDENTITY, candidate.candidateType)
        assertFalse(
            HimGroundTruthCandidate::class.java.declaredFields.any {
                it.type == HimEntityId::class.java
            }
        )
    }

    @Test
    fun variantScopeExplicitlySupportsCanonicalOrIdentity() {
        val canonicalScope = HimFamilyEntityReference.Canonical(HimEntityId("Apple1"))
        val identityScope =
            HimFamilyEntityReference.Identity(
                canonicalId = HimEntityId("Apple1"),
                identityId = HimEntityId("Breed1"),
            )

        assertIs<HimFamilyEntityReference.Canonical>(
            HimCandidateRelation.Variant(canonicalScope).scope
        )
        assertIs<HimFamilyEntityReference.Identity>(
            HimCandidateRelation.Variant(identityScope).scope
        )
    }

    @Test
    fun aliasRelationHasNoFingerprintOrVariantCombinationContract() {
        val fields = HimCandidateRelation.Alias::class.java.declaredFields.map { it.name }

        assertFalse(fields.any { it.contains("fingerprint", ignoreCase = true) })
        assertFalse(fields.any { it.contains("variant", ignoreCase = true) })
    }

    @Test
    fun createNewCanonicalIsCandidateOnlyAndHasNoMutationType() {
        assertEquals(
            HimCandidateType.CREATE_NEW_CANONICAL,
            HimCandidateRelation.CreateNewCanonical.candidateType,
        )
        assertFalse(
            HimGroundTruthMutationType.entries.any { it.name == "ADD_CANONICAL" }
        )
    }

    @Test
    fun candidateAndApprovalConfidenceAreSeparateTypes() {
        assertEquals(
            HimCandidateConfidence.entries.map { it.name },
            HimApprovalConfidence.entries.map { it.name },
        )
        assertFalse(HimCandidateConfidence::class == HimApprovalConfidence::class)
    }
}
