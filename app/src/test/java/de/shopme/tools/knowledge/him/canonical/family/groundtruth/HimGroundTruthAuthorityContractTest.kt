package de.shopme.tools.knowledge.him.canonical.family.groundtruth

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class HimGroundTruthAuthorityContractTest {

    @Test
    fun negativeGroundTruthAcceptsOnlyRejectedDecision() {
        assertFailsWith<IllegalArgumentException> {
            negativeProvenance(HimValidationDecision.UNKNOWN)
        }
        assertFailsWith<IllegalArgumentException> {
            negativeProvenance(HimValidationDecision.INSUFFICIENT_EVIDENCE)
        }
        assertEquals(
            HimValidationDecision.REJECTED,
            negativeProvenance(HimValidationDecision.REJECTED).decision,
        )
    }

    @Test
    fun retiredRegistryUsesHimEntityIdAndRejectsDuplicates() {
        val retiredId = HimEntityId("Old001")
        val entry =
            HimRetiredEntityIdRegistryEntry(
                entityId = retiredId,
                entityType = HimEntityType.IDENTITY,
                retirementMutationReference = HimMutationReference("mutation-retire-1"),
            )

        assertSame(retiredId, entry.entityId)
        assertFailsWith<IllegalArgumentException> {
            HimRetiredEntityIdRegistry("1", listOf(entry, entry))
        }
    }

    @Test
    fun mutationEntryRequiresValidationAndMatchingNonCanonicalEntityType() {
        val entry =
            mutationEntry(
                mutationType = HimGroundTruthMutationType.ADD_ALIAS,
                entityType = HimEntityType.ALIAS,
            )

        assertEquals(HimValidationReference("validation-1"), entry.validationReference)
        assertFailsWith<IllegalArgumentException> {
            mutationEntry(
                mutationType = HimGroundTruthMutationType.ADD_ALIAS,
                entityType = HimEntityType.CANONICAL,
            )
        }
    }

    @Test
    fun releaseReferencesFiveImmutableShaBoundSources() {
        val artifact =
            HimGroundTruthReleaseArtifactReference(
                path = "authority.json",
                sha256 = sha('a'),
                recordCount = 1,
            )
        val sources =
            HimGroundTruthReleaseSources(
                canonicalFamilyAuthority = artifact,
                activeEntityIdRegistry = artifact.copy(path = "active.json", sha256 = sha('b')),
                retiredEntityIdRegistry = artifact.copy(path = "retired.json", sha256 = sha('c')),
                mutationLedger = artifact.copy(path = "ledger.json", sha256 = sha('d')),
                entityFingerprintIndex = artifact.copy(path = "index.json", sha256 = sha('e')),
            )

        assertTrue(
            sources::class.java.declaredFields.map { it.name }.containsAll(
                setOf(
                    "canonicalFamilyAuthority",
                    "activeEntityIdRegistry",
                    "retiredEntityIdRegistry",
                    "mutationLedger",
                    "entityFingerprintIndex",
                )
            )
        )
        assertTrue(
            listOf(
                sources.canonicalFamilyAuthority,
                sources.activeEntityIdRegistry,
                sources.retiredEntityIdRegistry,
                sources.mutationLedger,
                sources.entityFingerprintIndex,
            ).all { it.sha256.value.length == 64 }
        )
    }

    @Test
    fun shaContractRejectsMutableOrMalformedReferences() {
        assertFailsWith<IllegalArgumentException> { HimSha256("abc") }
        assertFailsWith<IllegalArgumentException> { HimSha256("A".repeat(64)) }
        assertEquals("0".repeat(64), HimSha256("0".repeat(64)).value)
    }

    private fun negativeProvenance(decision: HimValidationDecision) =
        HimNegativeGroundTruthProvenance(
            candidateReference = HimCandidateReference("candidate-1"),
            validationReference = HimValidationReference("validation-1"),
            evidenceReferences = emptyList(),
            decision = decision,
            contractChecks = emptyList(),
            shortRationale = "Contract result.",
            validatedAgainst = snapshot(),
            contractVersion = "F3_V1",
            validatorVersion = "validator-1",
        )

    private fun mutationEntry(
        mutationType: HimGroundTruthMutationType,
        entityType: HimEntityType,
    ) =
        HimCanonicalFamilyMutationLedgerEntry(
            mutationReference = HimMutationReference("mutation-1"),
            validationReference = HimValidationReference("validation-1"),
            mutationType = mutationType,
            entityType = entityType,
            newEntityId = HimEntityId("New001"),
            parent = HimFamilyEntityReference.Canonical(HimEntityId("Food01")),
            canonicalFamilyAuthoritySha256Before = sha('1'),
            canonicalFamilyAuthoritySha256After = sha('2'),
            entityIdRegistrySha256Before = sha('3'),
            entityIdRegistrySha256After = sha('4'),
            contractVersion = "F3_V1",
        )

    private fun snapshot() =
        HimValidationAuthoritySnapshot(
            canonicalFamilyAuthoritySha256 = sha('a'),
            entityIdRegistrySha256 = sha('b'),
        )

    private fun sha(character: Char) = HimSha256(character.toString().repeat(64))
}
