package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalAlias
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilySourceCatalog
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimLifecycleStatus
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonical
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticEvidenceRelation
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimCiqualDeterministicPrimaryIdentityExtractorV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimCiqualEvidenceProjectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimCiqualLogicalRecordKind
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimCiqualPrimaryIdentityResolutionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentClassificationV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentContractV1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RunHimCiqualDeterministicPrimaryIdentityExtractorV1Test {
    private val catalog = fixtureCatalog(
        "Vanille", "Dessert", "Biscuit", "Pomme", "Alpha", "Beta",
    )
    private val authority = fixtureAuthority()
    private val vanillaFamily = authority.families.first { it.canonicalName == "Vanille" }
    private val dessertFamily = authority.families.first { it.canonicalName == "Dessert" }
    private val pommeFamily = authority.families.first { it.canonicalName == "Pomme" }

    @Test
    fun realVanillaPodProjectionSeparatesPrimaryAndUncoveredModifiers() {
        val extraction = extract(french = "Vanille, gousse", english = "Vanilla, pod")

        assertEquals("Vanille", extraction.primaryIdentity)
        assertEquals(listOf("gousse", "pod"), extraction.modifiers)
        assertEquals(HimCiqualPrimaryIdentityResolutionV1.RESOLVED, extraction.resolution)

        val alignment = HimEvidenceAlignmentContractV1.evaluate(extraction.toAlignmentInput(), vanillaFamily)
        assertEquals(HimEvidenceAlignmentClassificationV1.PRIMARY_CANONICAL_MATCH, alignment.classification)
        assertTrue(alignment.directEvidenceSupported)
        assertEquals(listOf("gousse", "pod"), alignment.uncoveredModifiers)
        assertEquals(listOf(false, false), alignment.modifierCoverage.map { it.coveredByAuthority })
    }

    @Test
    fun sameExtractionCanBeComparedToForeignTargetWithoutLabelLeakage() {
        val record = record("Vanille, gousse", "Vanilla, pod")
        val extraction = HimCiqualDeterministicPrimaryIdentityExtractorV1.extract(record, catalog, authority)
        val vanillaAlignment = HimEvidenceAlignmentContractV1.evaluate(extraction.toAlignmentInput(), vanillaFamily)
        val dessertAlignment = HimEvidenceAlignmentContractV1.evaluate(extraction.toAlignmentInput(), dessertFamily)

        assertEquals("Vanille", extraction.primaryIdentity)
        assertEquals(HimEvidenceAlignmentClassificationV1.PRIMARY_CANONICAL_MATCH, vanillaAlignment.classification)
        assertEquals(HimEvidenceAlignmentClassificationV1.OTHER_PRIMARY_IDENTITY, dessertAlignment.classification)
        assertFalse(dessertAlignment.directEvidenceSupported)
    }

    @Test
    fun flavorContextDoesNotBecomePrimaryIdentity() {
        val dessert = extract(french = "Dessert à la vanille", english = "Dessert with vanilla")
        val biscuit = extract(french = "Biscuit à la vanille", english = "Biscuit with vanilla")

        assertEquals("Dessert", dessert.primaryIdentity)
        assertEquals(listOf("vanille", "vanilla"), dessert.modifiers)
        assertEquals("Biscuit", biscuit.primaryIdentity)
        assertEquals(listOf("vanille", "vanilla"), biscuit.modifiers)
        assertEquals(HimEvidenceAlignmentClassificationV1.CANONICAL_ONLY_AS_MODIFIER, HimEvidenceAlignmentContractV1.evaluate(dessert.toAlignmentInput(), vanillaFamily).classification)
        assertEquals(HimEvidenceAlignmentClassificationV1.PRIMARY_CANONICAL_MATCH, HimEvidenceAlignmentContractV1.evaluate(dessert.toAlignmentInput(), dessertFamily).classification)
    }

    @Test
    fun productStateAfterCommaRemainsModifier() {
        val extraction = extract(french = "Pomme, crue", english = "Apple, raw")

        assertEquals("Pomme", extraction.primaryIdentity)
        assertEquals(listOf("crue", "raw"), extraction.modifiers)
        assertTrue(HimEvidenceAlignmentContractV1.evaluate(extraction.toAlignmentInput(), pommeFamily).directEvidenceSupported)
    }

    @Test
    fun fullyBoundCompositeWinsBeforeGeneralDecomposition() {
        val extraction = extract(french = "Dessert vanille", english = "Dessert vanille")

        assertEquals("Dessert vanille", extraction.primaryIdentity)
        assertTrue(extraction.modifiers.isEmpty())
        assertEquals("EXPLICIT_PRODUCT_IDENTITY", extraction.extractionPath)
    }

    @Test
    fun boundScientificNameMaySupportIdentityButUnboundScientificNameCannot() {
        val bound = extract(french = "Unknown food", english = "Unknown food", scientific = "Vanilla planifolia")
        val unbound = extract(french = "Unknown food", english = "Unknown food", scientific = "Unbound plantus")

        assertEquals("Vanilla planifolia", bound.primaryIdentity)
        assertEquals(HimCiqualPrimaryIdentityResolutionV1.RESOLVED, bound.resolution)
        assertTrue(HimEvidenceAlignmentContractV1.evaluate(bound.toAlignmentInput(), vanillaFamily).directEvidenceSupported)
        assertNull(unbound.primaryIdentity)
        assertEquals(HimCiqualPrimaryIdentityResolutionV1.UNRESOLVED, unbound.resolution)
    }

    @Test
    fun groupsAloneCannotCreatePrimaryIdentity() {
        val extraction = extract(
            french = "Unclassified food",
            english = "Unclassified food",
            groupFr = "Vanille",
            subgroupFr = "Spices",
        )

        assertNull(extraction.primaryIdentity)
        assertEquals(HimCiqualPrimaryIdentityResolutionV1.UNRESOLVED, extraction.resolution)
        assertFalse(HimEvidenceAlignmentContractV1.evaluate(extraction.toAlignmentInput(), vanillaFamily).directEvidenceSupported)
    }

    @Test
    fun conflictingFrenchAndEnglishProductSignalsFailClosed() {
        val extraction = extract(french = "Vanille", english = "Pomme")

        assertNull(extraction.primaryIdentity)
        assertEquals(HimCiqualPrimaryIdentityResolutionV1.UNRESOLVED, extraction.resolution)
        assertEquals(listOf("Pomme", "Vanille"), extraction.candidateIdentities)
        assertFalse(HimEvidenceAlignmentContractV1.evaluate(extraction.toAlignmentInput(), vanillaFamily).directEvidenceSupported)
    }

    @Test
    fun nonFoodCiqualRecordKindFailsClosed() {
        val taxonomy = HimCiqualEvidenceProjectionV1.fromProjectionJson(
            """
                {"recordKind":"TAXONOMY","groupCode":"01","groupNameFr":"Vanille","groupNameEn":"Vanilla","subgroupCode":"01","subgroupNameFr":"Spices","subgroupNameEn":"Spices","subSubgroupCode":"01","subSubgroupNameFr":"Pod","subSubgroupNameEn":"Pod"}
            """.trimIndent(),
            2,
        )

        assertFailsWith<IllegalArgumentException> {
            HimCiqualDeterministicPrimaryIdentityExtractorV1.extract(taxonomy, catalog, authority)
        }
    }

    @Test
    fun repeatedInputIsByteIndependentAndModifierOrderIsStable() {
        val first = extract(french = "Vanille, gousse", english = "Vanilla, pod")
        val second = extract(french = "Vanille, gousse", english = "Vanilla, pod")

        assertEquals(first, second)
        assertEquals(listOf("gousse", "pod"), first.modifiers)
        assertEquals(first.candidateIdentities, second.candidateIdentities)
    }

    @Test
    fun claimedDirectCannotOverrideForeignOrUnresolvedAlignment() {
        val foreign = extract(french = "Pomme", english = "Apple")
        val unresolved = extract(french = "Unclassified food", english = "Unclassified food")

        val foreignResult = HimEvidenceAlignmentContractV1.evaluate(
            foreign.toAlignmentInput().copy(claimedEvidenceRelation = HimSemanticEvidenceRelation.DIRECT),
            vanillaFamily,
        )
        val unresolvedResult = HimEvidenceAlignmentContractV1.evaluate(
            unresolved.toAlignmentInput().copy(claimedEvidenceRelation = HimSemanticEvidenceRelation.DIRECT),
            vanillaFamily,
        )

        assertEquals(HimEvidenceAlignmentClassificationV1.OTHER_PRIMARY_IDENTITY, foreignResult.classification)
        assertEquals(HimEvidenceAlignmentClassificationV1.UNRESOLVED_PRIMARY_IDENTITY, unresolvedResult.classification)
        assertNull(foreignResult.effectiveEvidenceRelation)
        assertNull(unresolvedResult.effectiveEvidenceRelation)
    }

    private fun extract(
        french: String,
        english: String,
        scientific: String = "Unbound plantus",
        groupFr: String = "Food",
        subgroupFr: String = "Food",
    ) = HimCiqualDeterministicPrimaryIdentityExtractorV1.extract(
        record(french, english, scientific, groupFr, subgroupFr),
        catalog,
        authority,
    )

    private fun record(
        french: String,
        english: String,
        scientific: String = "Unbound plantus",
        groupFr: String = "Food",
        subgroupFr: String = "Food",
    ) = HimCiqualEvidenceProjectionV1.fromProjectionJson(
        """
            {"recordKind":"FOOD","alimCode":"fixture-1","nameFr":"${json(french)}","nameEn":"${json(english)}","scientificName":{"lexicalValue":"${json(scientific)}","missingAttributeValue":null},"groupCode":"01","groupNameFr":"${json(groupFr)}","groupNameEn":"Food","subgroupCode":"01","subgroupNameFr":"${json(subgroupFr)}","subgroupNameEn":"Food","subSubgroupCode":"01","subSubgroupNameFr":"Food","subSubgroupNameEn":"Food"}
        """.trimIndent(),
        1,
    )

    private fun fixtureCatalog(vararg names: String) = HimProductOnlyCanonicalMaster(
        path = "fixture/catalog.json",
        contentSha256 = "fixture",
        records = names.map { HimProductOnlyCanonical(it, normalize(it), emptyList()) },
    )

    private fun fixtureAuthority() = HimCanonicalFamilyAuthority(
        schemaVersion = "fixture",
        sourceCatalog = HimCanonicalFamilySourceCatalog("fixture/catalog.json", "fixture", catalog.records.size),
        families = catalog.records.mapIndexed { index, record ->
            HimCanonicalFamily(
                canonicalId = HimEntityId("c%05d".format(index)),
                canonicalName = record.itemname,
                normalizedName = record.normalized,
                taxonomyPaths = emptyList(),
                lifecycleStatus = HimLifecycleStatus.ACTIVE,
                identities = emptyList(),
                variants = emptyList(),
                aliases = when (record.itemname) {
                    "Vanille" -> listOf(alias("Vanilla", "a00001"), alias("Vanilla planifolia", "a00002"))
                    "Dessert" -> listOf(alias("Dessert vanille", "a00003"))
                    "Pomme" -> listOf(alias("Apple", "a00004"))
                    else -> emptyList()
                },
            )
        },
    )

    private fun alias(name: String, id: String) = HimCanonicalAlias(
        aliasId = HimEntityId(id),
        aliasName = name,
        normalizedName = normalize(name),
        lifecycleStatus = HimLifecycleStatus.ACTIVE,
    )

    private fun json(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun normalize(value: String): String = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFC)
        .trim().replace(Regex("\\s+"), " ").lowercase(java.util.Locale.ROOT)
}
