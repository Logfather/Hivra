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
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimAgribalyseEvidenceProjectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimCiqualEvidenceProjectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimDeterministicEvidenceAlignmentEvaluatorV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentClassificationV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordKind
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimGlycemicIndexEvidenceProjectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimOffEvidenceProjectionV1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RunHimCrossSourceEvidenceAlignmentRegressionV1Test {
    private val catalog = fixtureCatalog("Vanille", "Pudding", "Zucker", "Keks", "Apfel", "Alpha", "Beta")
    private val authority = fixtureAuthority()
    private val vanillaFamily = authority.families.first { it.canonicalName == "Vanille" }
    private val appleFamily = authority.families.first { it.canonicalName == "Apfel" }

    @Test
    fun fourSourceVanillaMatrixSupportsExactlyOneDirectRelation() {
        val evaluations = evaluateAll(vanillaFixtures(), claimed = HimSemanticEvidenceRelation.DIRECT)

        assertEquals(4, evaluations.size)
        assertEquals(4, evaluations.count { it.alignment.claimedEvidenceRelation == HimSemanticEvidenceRelation.DIRECT })
        assertEquals(1, evaluations.count { it.directEvidenceSupported })
        assertEquals(3, evaluations.count { !it.directEvidenceSupported })
        assertEquals(listOf(HimGroundTruthSourceName.CIQUAL), evaluations.filter { it.directEvidenceSupported }.map { it.source.name })

        val off = evaluations.single { it.recordKind == HimEvidenceRecordKind.OFF_PRODUCT }
        val agribalyse = evaluations.single { it.recordKind == HimEvidenceRecordKind.AGRIBALYSE_RECORD }
        val ciqual = evaluations.single { it.recordKind == HimEvidenceRecordKind.CIQUAL_FOOD }
        val gi = evaluations.single { it.recordKind == HimEvidenceRecordKind.GI_MEASUREMENT }

        assertEquals("Pudding", off.primaryIdentity)
        assertEquals(HimEvidenceAlignmentClassificationV1.CANONICAL_ONLY_AS_MODIFIER, off.alignment.classification)
        assertFalse(off.directEvidenceSupported)

        assertEquals("Sucre", agribalyse.primaryIdentity)
        assertEquals(listOf("vanillé", "vanilla"), agribalyse.modifiers)
        assertFalse(agribalyse.directEvidenceSupported)

        assertEquals("Vanille", ciqual.primaryIdentity)
        assertEquals(listOf("gousse", "pod"), ciqual.modifiers)
        assertTrue(ciqual.directEvidenceSupported)
        assertEquals(listOf("gousse", "pod"), ciqual.uncoveredModifiers)

        assertEquals("COOKIES", gi.primaryIdentity)
        assertEquals(listOf("Vanille"), gi.modifiers)
        assertFalse(gi.directEvidenceSupported)
    }

    @Test
    fun evaluatorCanonicalizesInputOrderAndRepeatsIdempotently() {
        val fixtures = vanillaFixtures()
        val first = evaluateAll(fixtures, claimed = HimSemanticEvidenceRelation.DIRECT)
        val reversed = evaluateAll(fixtures.asReversed(), claimed = HimSemanticEvidenceRelation.DIRECT)
        val repeated = evaluateAll(fixtures, claimed = HimSemanticEvidenceRelation.DIRECT)

        assertEquals(first, reversed)
        assertEquals(first, repeated)
        assertEquals(first.map { it.sourceRecordIdentity }.sorted(), first.map { it.sourceRecordIdentity })
        assertTrue(first.all { it.rationale.isNotBlank() && !it.rationale.contains("timestamp", ignoreCase = true) })
    }

    @Test
    fun extractionHappensWithoutTargetAndAlignmentCanBeReused() {
        val ciqual = vanillaFixtures().single { it.recordKind == HimEvidenceRecordKind.CIQUAL_FOOD }
        val extraction = HimDeterministicEvidenceAlignmentEvaluatorV1.extract(ciqual, catalog, authority)
        val vanillaAlignment = HimDeterministicEvidenceAlignmentEvaluatorV1.align(extraction, vanillaFamily)
        val appleAlignment = HimDeterministicEvidenceAlignmentEvaluatorV1.align(extraction, appleFamily)

        assertEquals("Vanille", extraction.primaryIdentity)
        assertEquals(HimEvidenceAlignmentClassificationV1.PRIMARY_CANONICAL_MATCH, vanillaAlignment.alignment.classification)
        assertEquals(HimEvidenceAlignmentClassificationV1.OTHER_PRIMARY_IDENTITY, appleAlignment.alignment.classification)
        assertEquals(extraction, HimDeterministicEvidenceAlignmentEvaluatorV1.extract(ciqual, catalog, authority))
    }

    @Test
    fun claimedDirectCannotOverrideNegativeAlignment() {
        val evaluations = evaluateAll(vanillaFixtures(), claimed = HimSemanticEvidenceRelation.DIRECT)

        evaluations.filterNot { it.recordKind == HimEvidenceRecordKind.CIQUAL_FOOD }.forEach { evaluation ->
            assertEquals(HimSemanticEvidenceRelation.DIRECT, evaluation.alignment.claimedEvidenceRelation)
            assertFalse(evaluation.directEvidenceSupported)
            assertNull(evaluation.effectiveEvidenceRelation)
        }
    }

    @Test
    fun unsupportedRecordKindFailsClosedWithoutDirectSupport() {
        val taxonomy = HimCiqualEvidenceProjectionV1.fromProjectionJson(
            """
                {"recordKind":"TAXONOMY","groupCode":"01","groupNameFr":"Vanille","groupNameEn":"Vanilla","subgroupCode":"01","subgroupNameFr":"Spices","subgroupNameEn":"Spices","subSubgroupCode":"01","subSubgroupNameFr":"Pod","subSubgroupNameEn":"Pod"}
            """.trimIndent(),
            9,
        )
        val result = HimDeterministicEvidenceAlignmentEvaluatorV1.evaluate(taxonomy, catalog, authority, vanillaFamily, HimSemanticEvidenceRelation.DIRECT)

        assertEquals("UNSUPPORTED_RECORD_KIND", result.failureReason)
        assertEquals(HimEvidenceAlignmentClassificationV1.UNRESOLVED_PRIMARY_IDENTITY, result.alignment.classification)
        assertFalse(result.directEvidenceSupported)
        assertNull(result.effectiveEvidenceRelation)
    }

    @Test
    fun invalidProjectionFailsClosedWithoutPartialInterpretation() {
        val invalid = de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRetrievalIndexRecord(
            internalRecordKey = 1,
            sourceRecordReference = de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordReference.offProduct(1, "invalid"),
            recordKind = HimEvidenceRecordKind.OFF_PRODUCT,
            sourceNativeIdentifiersJson = "{\"code\":\"invalid\"}",
            evidenceProjection = de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceProjection("{malformed"),
            searchText = de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceSearchText("", "", "", "", ""),
        )
        val result = HimDeterministicEvidenceAlignmentEvaluatorV1.evaluate(invalid, catalog, authority, vanillaFamily, HimSemanticEvidenceRelation.DIRECT)

        assertEquals("INVALID_OR_INCOMPLETE_PROJECTION", result.failureReason)
        assertNull(result.primaryIdentity)
        assertFalse(result.directEvidenceSupported)
        assertNull(result.effectiveEvidenceRelation)
    }

    @Test
    fun sourceAndRecordKindMismatchCannotReachAlignment() {
        assertFailsWith<IllegalArgumentException> {
            HimOffEvidenceProjectionV1.fromProjectionJson(
                """
                    {"rowOrdinal":1,"source":{"code":"fixture"},"identity":{"productName":"Vanille"},"quality":{}}
                """.trimIndent(),
            ).copy(recordKind = HimEvidenceRecordKind.GI_MEASUREMENT)
        }
    }

    private fun evaluateAll(
        records: List<de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRetrievalIndexRecord>,
        claimed: HimSemanticEvidenceRelation? = null,
    ) = HimDeterministicEvidenceAlignmentEvaluatorV1.evaluateAll(records, catalog, authority, vanillaFamily, claimed)

    private fun vanillaFixtures() = listOf(offFixture(), agribalyseFixture(), ciqualFixture(), giFixture())

    private fun offFixture() = HimOffEvidenceProjectionV1.fromProjectionJson(
        """
            {"rowOrdinal":1,"source":{"code":"4260694945322"},"identity":{"productName":"Vanille Pudding mit Bourbon-Vanille","productNameGerman":null,"productNameEnglish":null,"genericName":null,"genericNameGerman":null,"genericNameEnglish":null,"brands":[],"productType":null},"taxonomy":{"categories":["Desserts"]},"ingredients":{"text":"Vanille"},"quality":{}}
        """.trimIndent(),
    ).let { record -> record.copy(sourceRecordReference = de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordReference.offProduct(4474818, "4260694945322"), internalRecordKey = 4474818) }

    private fun agribalyseFixture() = HimAgribalyseEvidenceProjectionV1.fromProjectionJson(
        """
            {"rowOrdinal":1,"agbCode":"31044","ciqualCode":"fixture-ciqual","seasonCode":"fixture-season","airTransportCode":"no","productNameFr":"Sucre vanillé","lciName":"Sugar, vanilla flavoured","foodGroup":"Sugar and sweet products","foodSubgroup":"Sugar","preparation":"fixture","delivery":"fixture","packagingApproach":"fixture"}
        """.trimIndent(),
    ).let { record -> record.copy(sourceRecordReference = de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordReference.agribalyse(1804, "31044"), internalRecordKey = 1804) }

    private fun ciqualFixture() = HimCiqualEvidenceProjectionV1.fromProjectionJson(
        """
            {"recordKind":"FOOD","alimCode":"11057","nameFr":"Vanille, gousse","nameEn":"Vanilla, pod","scientificName":{"lexicalValue":"Vanilla planifolia","missingAttributeValue":null},"groupCode":"01","groupNameFr":"Spices","groupNameEn":"Spices","subgroupCode":"01","subgroupNameFr":"Spices","subgroupNameEn":"Spices","subSubgroupCode":"01","subSubgroupNameFr":"Spices","subSubgroupNameEn":"Spices"}
        """.trimIndent(),
        3,
    ).let { record -> record.copy(sourceRecordReference = de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordReference.ciqualFood("11057")) }

    private fun giFixture() = HimGlycemicIndexEvidenceProjectionV1.fromProjectionJson(
        """
            {"recordKind":"MEASUREMENT","arrayOrdinal":823,"foodNumber":823,"pageNumber":1,"foodItem":{"lexicalValue":"Prince Petit Déjeuner Vanille (LU, France and Spain)","status":"PRESENT"},"country":{"lexicalValue":"France","status":"PRESENT"},"year":{"lexicalValue":"2010","status":"PRESENT"},"gi":{"lexicalValue":"73","status":"PRESENT"},"sem":{"lexicalValue":"6","status":"PRESENT"},"gl":{"lexicalValue":"11","status":"PRESENT"},"subjects":{"lexicalValue":"10","status":"PRESENT"},"availableCarbohydrate":{"lexicalValue":"50","status":"PRESENT"},"testPortion":{"lexicalValue":"119","status":"PRESENT"},"referenceFoodTime":{"lexicalValue":"Bread, 2h","status":"PRESENT"},"timepoints":{"lexicalValue":"Standard","status":"PRESENT"},"sampleCollection":{"lexicalValue":"Capillary","status":"PRESENT"},"analysisMethod":{"lexicalValue":"YSI","status":"PRESENT"},"referenceCode":{"lexicalValue":"UO7","status":"PRESENT"},"sourceContext":{"majorCategory":"COOKIES","subcategory":null,"deeperHeading":null}}
        """.trimIndent(),
        4,
    ).let { record -> record.copy(sourceRecordReference = de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordReference.gi("measurement", 823), internalRecordKey = 823) }

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
                    "Keks" -> listOf(alias("Cookies", "a00004"), alias("Biscuit", "a00005"))
                    "Apfel" -> listOf(alias("Apple", "a00006"))
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

    private object HimGroundTruthSourceName {
        const val CIQUAL = "CIQUAL"
    }
}
