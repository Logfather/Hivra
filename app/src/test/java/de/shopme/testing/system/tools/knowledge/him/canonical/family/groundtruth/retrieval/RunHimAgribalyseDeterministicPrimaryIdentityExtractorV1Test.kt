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
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimAgribalyseDeterministicPrimaryIdentityExtractorV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimAgribalyseEvidenceProjectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimAgribalysePrimaryIdentityResolutionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentClassificationV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentContractV1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RunHimAgribalyseDeterministicPrimaryIdentityExtractorV1Test {
    private val catalog = fixtureCatalog(
        "Vanille", "Zucker", "Pudding", "Pomme", "Apfelsaft", "Alpha", "Beta",
    )
    private val authority = fixtureAuthority()
    private val vanillaFamily = authority.families.first { it.canonicalName == "Vanille" }
    private val sugarFamily = authority.families.first { it.canonicalName == "Zucker" }
    private val appleJuiceFamily = authority.families.first { it.canonicalName == "Apfelsaft" }

    @Test
    fun sugarVanillaSignalsKeepSugarPrimaryAndRejectVanillaDirectEvidence() {
        val extraction = extract(
            french = "Sucre vanillé",
            lci = "Sugar, vanilla flavoured",
            group = "Sucre et produits sucrés",
            subgroup = "Sugar and sweet products",
        )

        assertEquals("Sucre", extraction.primaryIdentity)
        assertEquals(HimAgribalysePrimaryIdentityResolutionV1.RESOLVED, extraction.resolution)
        assertEquals(listOf("vanillé", "vanilla"), extraction.modifiers)
        assertTrue(extraction.primaryIdentityAuthorityBound)

        val alignment = HimEvidenceAlignmentContractV1.evaluate(
            extraction.toAlignmentInput().copy(claimedEvidenceRelation = HimSemanticEvidenceRelation.DIRECT),
            vanillaFamily,
        )
        assertEquals(HimEvidenceAlignmentClassificationV1.OTHER_PRIMARY_IDENTITY, alignment.classification)
        assertFalse(alignment.directEvidenceSupported)
        assertNull(alignment.effectiveEvidenceRelation)
    }

    @Test
    fun sameRecordExtractsOnceAndOnlyAlignmentTargetChangesTheDownstreamResult() {
        val extraction = extract(
            french = "Sucre vanillé",
            lci = "Sugar, vanilla flavoured",
            group = "Sucre et produits sucrés",
            subgroup = "Sugar and sweet products",
        )
        val vanillaAlignment = HimEvidenceAlignmentContractV1.evaluate(extraction.toAlignmentInput(), vanillaFamily)
        val sugarAlignment = HimEvidenceAlignmentContractV1.evaluate(extraction.toAlignmentInput(), sugarFamily)

        assertEquals("Sucre", extraction.primaryIdentity)
        assertFalse(vanillaAlignment.directEvidenceSupported)
        assertTrue(sugarAlignment.directEvidenceSupported)
    }

    @Test
    fun simpleBoundMultilingualProductSupportsDirectEvidence() {
        val extraction = extract(french = "Vanille", lci = "Vanilla", group = "Spices", subgroup = "Vanilla")

        assertEquals("Vanille", extraction.primaryIdentity)
        assertTrue(extraction.modifiers.isEmpty())
        assertEquals(listOf("productNameFr", "lciName"), extraction.identityFieldsUsed)
        assertTrue(HimEvidenceAlignmentContractV1.evaluate(extraction.toAlignmentInput(), vanillaFamily).directEvidenceSupported)
    }

    @Test
    fun fullyBoundCompositeWinsBeforeDecomposition() {
        val extraction = extract(french = "Jus de pomme", lci = "Jus de pomme", group = "Fruit", subgroup = "Juices")

        assertEquals("Jus de pomme", extraction.primaryIdentity)
        assertTrue(extraction.modifiers.isEmpty())
        assertEquals("EXPLICIT_PRODUCT_IDENTITY", extraction.extractionPath)
        assertTrue(HimEvidenceAlignmentContractV1.evaluate(extraction.toAlignmentInput(), appleJuiceFamily).directEvidenceSupported)
    }

    @Test
    fun productFlavorAndPreparationRemainModifiers() {
        val flavor = extract(french = "Sucre vanillé", lci = "Sugar, vanilla", group = "Sweet products", subgroup = "Sugar")
        val preparation = extract(french = "Pomme cuite", lci = "Pomme cuite", group = "Fruit", subgroup = "Prepared fruit")

        assertEquals("Sucre", flavor.primaryIdentity)
        assertEquals(listOf("vanillé", "vanilla"), flavor.modifiers)
        assertEquals("Pomme", preparation.primaryIdentity)
        assertEquals(listOf("cuite"), preparation.modifiers)
    }

    @Test
    fun groupsWithoutBoundProductNameCannotCreatePrimaryIdentity() {
        val extraction = extract(french = "Process label", lci = "Process label", group = "Sucre", subgroup = "Sugar")

        assertNull(extraction.primaryIdentity)
        assertEquals(HimAgribalysePrimaryIdentityResolutionV1.UNRESOLVED, extraction.resolution)
        assertFalse(HimEvidenceAlignmentContractV1.evaluate(extraction.toAlignmentInput(), vanillaFamily).directEvidenceSupported)
    }

    @Test
    fun brandOrProcessLikeNameWithoutIdentityBindingRemainsUnresolved() {
        val extraction = extract(french = "Brand process", lci = "Brand process", group = "Desserts", subgroup = "Processed")

        assertNull(extraction.primaryIdentity)
        assertEquals(HimAgribalysePrimaryIdentityResolutionV1.UNRESOLVED, extraction.resolution)
    }

    @Test
    fun conflictingFrenchAndEnglishPrimarySignalsFailClosed() {
        val extraction = extract(french = "Vanille", lci = "Zucker", group = "Food", subgroup = "Food")

        assertNull(extraction.primaryIdentity)
        assertEquals(HimAgribalysePrimaryIdentityResolutionV1.UNRESOLVED, extraction.resolution)
        assertEquals(listOf("Vanille", "Zucker"), extraction.candidateIdentities)
        assertFalse(HimEvidenceAlignmentContractV1.evaluate(extraction.toAlignmentInput(), vanillaFamily).directEvidenceSupported)
    }

    @Test
    fun equalCandidatesAreStableAndUnresolved() {
        val first = extract(french = "Alpha/Beta", lci = "Alpha/Beta", group = "Food", subgroup = "Food")
        val second = extract(french = "Alpha/Beta", lci = "Alpha/Beta", group = "Food", subgroup = "Food")

        assertEquals(HimAgribalysePrimaryIdentityResolutionV1.UNRESOLVED, first.resolution)
        assertNull(first.primaryIdentity)
        assertEquals(listOf("Alpha", "Beta"), first.candidateIdentities)
        assertEquals(first, second)
    }

    @Test
    fun repeatedIdenticalExtractionPreservesModifierAndCandidateOrder() {
        val first = extract(french = "Sucre vanillé", lci = "Sugar, vanilla flavoured", group = "Sugar", subgroup = "Sweet")
        val second = extract(french = "Sucre vanillé", lci = "Sugar, vanilla flavoured", group = "Sugar", subgroup = "Sweet")

        assertEquals(first, second)
        assertEquals(listOf("vanillé", "vanilla"), first.modifiers)
        assertEquals(first.candidateIdentities, second.candidateIdentities)
    }

    private fun extract(
        french: String,
        lci: String,
        group: String,
        subgroup: String,
    ) = HimAgribalyseDeterministicPrimaryIdentityExtractorV1.extract(
        HimAgribalyseEvidenceProjectionV1.fromProjectionJson(
            """
                {
                  "rowOrdinal": 1,
                  "agbCode": "fixture",
                  "ciqualCode": "fixture-ciqual",
                  "seasonCode": "fixture-season",
                  "airTransportCode": "no",
                  "productNameFr": "${json(french)}",
                  "lciName": "${json(lci)}",
                  "foodGroup": "${json(group)}",
                  "foodSubgroup": "${json(subgroup)}",
                  "preparation": "fixture-preparation",
                  "delivery": "fixture-delivery",
                  "packagingApproach": "fixture-packaging"
                }
            """.trimIndent().replace("\n", ""),
        ),
        catalog,
        authority,
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
                    "Vanille" -> listOf(alias("Vanilla", "a00001"))
                    "Zucker" -> listOf(alias("Sucre", "a00002"), alias("Sugar", "a00003"))
                    "Apfelsaft" -> listOf(alias("Jus de pomme", "a00004"))
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

    private fun normalize(value: String): String =
        java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFC)
            .trim()
            .replace(Regex("\\s+"), " ")
            .lowercase(java.util.Locale.ROOT)
}
