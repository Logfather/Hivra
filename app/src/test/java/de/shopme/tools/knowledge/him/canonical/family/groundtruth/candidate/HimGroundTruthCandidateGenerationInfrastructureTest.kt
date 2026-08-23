package de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalAlias
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilySourceCatalog
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalIdentity
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalVariant
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimLifecycleStatus
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateConfidence
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateRelation
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthCandidate
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HimGroundTruthCandidateGenerationInfrastructureTest {

    @Test
    fun filterRetainsOnlyHighAndMediumCandidates() {
        val candidates = HimCandidateConfidence.entries.mapIndexed { index, confidence ->
            candidate("candidate-$index", "term-$index", confidence)
        }
        val result = HimGroundTruthCandidateFilter().filter(candidates)

        assertEquals(
            listOf(HimCandidateConfidence.HIGH, HimCandidateConfidence.MEDIUM),
            result.retained.map { it.candidateConfidence },
        )
        assertEquals(1, result.lowDropped)
        assertEquals(1, result.noConfidenceDropped)
    }

    @Test
    fun explicitCandidateReferenceAllowsCrossSourceEvidenceConsolidation() {
        val first = candidate("candidate-1", "Braeburn", evidence = listOf(evidence("OFF", 'a')))
        val second = candidate("candidate-1", "Braeburn", evidence = listOf(evidence("CIQUAL", 'b')))
        val consolidated = HimGroundTruthCandidateConsolidator().consolidate(listOf(first, second))

        assertEquals(1, consolidated.size)
        assertEquals(setOf("OFF", "CIQUAL"), consolidated.single().evidenceReferences.map { it.source }.toSet())
    }

    @Test
    fun equalStringsWithDifferentCandidateReferencesAreNotMerged() {
        val first = candidate("candidate-1", "same")
        val second = candidate("candidate-2", "same")

        assertEquals(2, HimGroundTruthCandidateConsolidator().consolidate(listOf(first, second)).size)
    }

    @Test
    fun knownApprovedRelationIsRemovedFromNovelCandidateOutput() {
        val authority = authorityWithApprovedRelations()
        val knownIdentity =
            candidate(
                reference = "candidate-known",
                term = "Braeburn",
                relation = HimCandidateRelation.Identity(CANONICAL_ID),
            )
        val (novel, known) = HimKnownRelationDetector(authority).detect(listOf(knownIdentity))

        assertTrue(novel.isEmpty())
        assertEquals(1, known.size)
    }

    @Test
    fun generationResultSupportsMultipleIndependentCandidateTypesAndScopes() {
        val identity = candidate("identity", "Braeburn", relation = HimCandidateRelation.Identity(CANONICAL_ID))
        val canonicalVariant = candidate(
            "variant-canonical",
            "Bio",
            relation = HimCandidateRelation.Variant(HimFamilyEntityReference.Canonical(CANONICAL_ID)),
        )
        val identityVariant = candidate(
            "variant-identity",
            "sauer",
            relation =
                HimCandidateRelation.Variant(
                    HimFamilyEntityReference.Identity(CANONICAL_ID, IDENTITY_ID)
                ),
        )
        val alias = candidate(
            "alias",
            "apple",
            relation = HimCandidateRelation.Alias(HimFamilyEntityReference.Canonical(CANONICAL_ID)),
        )
        val result = listOf(identity, canonicalVariant, identityVariant, alias)

        assertEquals(4, result.size)
        assertIs<HimFamilyEntityReference.Canonical>(
            (canonicalVariant.relation as HimCandidateRelation.Variant).scope
        )
        assertIs<HimFamilyEntityReference.Identity>(
            (identityVariant.relation as HimCandidateRelation.Variant).scope
        )
        assertIs<HimCandidateRelation.Alias>(alias.relation)
    }

    @Test
    fun productionInfrastructureHasNoEntityIdGeneratorOrPreHimDependency() {
        val directory = File("src/main/java/de/shopme/tools/knowledge/him/canonical/family/groundtruth/candidate")
        val content = directory.walkTopDown().filter { it.extension == "kt" }.joinToString("\n") { it.readText() }

        listOf(
            "HimEntityIdGenerator",
            "CanonicalFoodIdentity",
            "TrueCanonicalFoodIdentity",
            "sourceVariants",
            "KnowledgeCandidate",
        ).forEach { forbidden -> assertFalse(content.contains(forbidden), forbidden) }
    }

    private fun candidate(
        reference: String,
        term: String,
        confidence: HimCandidateConfidence = HimCandidateConfidence.HIGH,
        relation: HimCandidateRelation = HimCandidateRelation.Identity(CANONICAL_ID),
        evidence: List<HimEvidenceReference> = emptyList(),
    ) =
        HimGroundTruthCandidate(
            candidateReference = HimCandidateReference(reference),
            candidateTerm = term,
            relation = relation,
            evidenceReferences = evidence,
            candidateConfidence = confidence,
        )

    private fun evidence(source: String, shaCharacter: Char) =
        HimEvidenceReference(
            source = source,
            sourceArtifactSha256 = HimSha256(shaCharacter.toString().repeat(64)),
            sourceRecordIdentity = "$source-record",
        )

    private fun authorityWithApprovedRelations(): HimCanonicalFamilyAuthority =
        HimCanonicalFamilyAuthority(
            schemaVersion = "1",
            sourceCatalog = HimCanonicalFamilySourceCatalog("fixture", "a".repeat(64), 1),
            families =
                listOf(
                    HimCanonicalFamily(
                        canonicalId = CANONICAL_ID,
                        canonicalName = "Apple",
                        normalizedName = "apple",
                        taxonomyPaths = emptyList(),
                        lifecycleStatus = HimLifecycleStatus.ACTIVE,
                        identities =
                            listOf(
                                HimCanonicalIdentity(
                                    identityId = IDENTITY_ID,
                                    identityName = "Braeburn",
                                    normalizedName = "braeburn",
                                    lifecycleStatus = HimLifecycleStatus.ACTIVE,
                                    variants =
                                        listOf(
                                            HimCanonicalVariant(
                                                HimEntityId("Sour01"),
                                                "sour",
                                                "sour",
                                                HimLifecycleStatus.ACTIVE,
                                            )
                                        ),
                                    aliases =
                                        listOf(
                                            HimCanonicalAlias(
                                                HimEntityId("Alias1"),
                                                "Braeburn apple",
                                                "braeburn apple",
                                                HimLifecycleStatus.ACTIVE,
                                            )
                                        ),
                                )
                            ),
                        variants = emptyList(),
                        aliases = emptyList(),
                    )
                ),
        )

    companion object {
        val CANONICAL_ID = HimEntityId("Apple1")
        val IDENTITY_ID = HimEntityId("Breed1")
    }
}
