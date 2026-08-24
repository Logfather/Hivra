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
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentClassificationV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimGlycemicIndexDeterministicPrimaryIdentityExtractorV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimGlycemicIndexEvidenceProjectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimGlycemicIndexPrimaryIdentityResolutionV1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RunHimGlycemicIndexDeterministicPrimaryIdentityExtractorV1Test {
    private val catalog = fixtureCatalog("Keks", "Apfel", "Vanille", "Alpha", "Beta")
    private val authority = fixtureAuthority()
    private val cookieFamily = authority.families.first { it.canonicalName == "Keks" }
    private val appleFamily = authority.families.first { it.canonicalName == "Apfel" }
    private val vanillaFamily = authority.families.first { it.canonicalName == "Vanille" }

    @Test
    fun realPrinceVanillaRecordUsesCookieCategoryAndKeepsVanillaAsModifier() {
        val extraction = extract(
            food = "Prince Petit Déjeuner Vanille (LU, France and Spain)",
            major = "COOKIES",
        )

        assertEquals("COOKIES", extraction.primaryIdentity)
        assertEquals(listOf("Vanille"), extraction.modifiers)
        assertEquals("CATEGORY_BACKED", extraction.extractionPath)
        val vanilla = HimEvidenceAlignmentContractV1.evaluate(extraction.toAlignmentInput(), vanillaFamily)
        val cookie = HimEvidenceAlignmentContractV1.evaluate(extraction.toAlignmentInput(), cookieFamily)
        assertEquals(HimEvidenceAlignmentClassificationV1.CANONICAL_ONLY_AS_MODIFIER, vanilla.classification)
        assertFalse(vanilla.directEvidenceSupported)
        assertEquals(listOf("Vanille"), vanilla.uncoveredModifiers)
        assertTrue(cookie.directEvidenceSupported)
    }

    @Test
    fun sameMeasurementIsIndependentOfLaterAlignmentTarget() {
        val record = record("Prince Petit Déjeuner Vanille (LU, France and Spain)", "COOKIES")
        val extraction = HimGlycemicIndexDeterministicPrimaryIdentityExtractorV1.extract(record, catalog, authority)
        val vanillaAlignment = HimEvidenceAlignmentContractV1.evaluate(extraction.toAlignmentInput(), vanillaFamily)
        val cookieAlignment = HimEvidenceAlignmentContractV1.evaluate(extraction.toAlignmentInput(), cookieFamily)

        assertEquals("COOKIES", extraction.primaryIdentity)
        assertEquals(HimEvidenceAlignmentClassificationV1.CANONICAL_ONLY_AS_MODIFIER, vanillaAlignment.classification)
        assertTrue(cookieAlignment.directEvidenceSupported)
    }

    @Test
    fun explicitAppleIdentityAndRawStateBeatUnboundCategory() {
        val extraction = extract(food = "Apple, raw", major = "FRUIT")

        assertEquals("Apple", extraction.primaryIdentity)
        assertEquals(listOf("raw"), extraction.modifiers)
        assertEquals("CATALOG_COMPOSITION", extraction.extractionPath)
        assertTrue(HimEvidenceAlignmentContractV1.evaluate(extraction.toAlignmentInput(), appleFamily).directEvidenceSupported)
    }

    @Test
    fun vanillaBiscuitUsesCategorySupportedBiscuitAsPrimary() {
        val extraction = extract(food = "Vanilla biscuit", major = "COOKIES")

        assertEquals("Biscuit", extraction.primaryIdentity)
        assertEquals(listOf("Vanilla"), extraction.modifiers)
        val vanillaAlignment = HimEvidenceAlignmentContractV1.evaluate(extraction.toAlignmentInput(), vanillaFamily)
        assertEquals(HimEvidenceAlignmentClassificationV1.OTHER_PRIMARY_IDENTITY, vanillaAlignment.classification)
        assertFalse(vanillaAlignment.directEvidenceSupported)
        assertTrue(HimEvidenceAlignmentContractV1.evaluate(extraction.toAlignmentInput(), cookieFamily).directEvidenceSupported)
    }

    @Test
    fun vanillaPodKeepsVanillaPrimaryAndPodAsFormModifier() {
        val extraction = extract(food = "Vanilla pod", major = "SPICES")

        assertEquals("Vanilla", extraction.primaryIdentity)
        assertEquals(listOf("pod"), extraction.modifiers)
        val alignment = HimEvidenceAlignmentContractV1.evaluate(extraction.toAlignmentInput(), vanillaFamily)
        assertTrue(alignment.directEvidenceSupported)
        assertEquals(listOf("pod"), alignment.uncoveredModifiers)
    }

    @Test
    fun unresolvedFoodWithoutCategoryCannotCreateIdentity() {
        val extraction = extract(food = "Unknown sample", major = "")

        assertNull(extraction.primaryIdentity)
        assertEquals(HimGlycemicIndexPrimaryIdentityResolutionV1.UNRESOLVED, extraction.resolution)
        assertFalse(HimEvidenceAlignmentContractV1.evaluate(extraction.toAlignmentInput(), vanillaFamily).directEvidenceSupported)
    }

    @Test
    fun categoryWithoutFoodItemRemainsUnresolved() {
        val extraction = extract(food = null, major = "COOKIES")

        assertNull(extraction.primaryIdentity)
        assertEquals(HimGlycemicIndexPrimaryIdentityResolutionV1.UNRESOLVED, extraction.resolution)
        assertEquals(emptyList(), extraction.modifiers)
    }

    @Test
    fun explicitFoodIdentityConflictingWithCategoryFailsClosed() {
        val extraction = extract(food = "Apple", major = "COOKIES")

        assertNull(extraction.primaryIdentity)
        assertEquals(HimGlycemicIndexPrimaryIdentityResolutionV1.UNRESOLVED, extraction.resolution)
        assertTrue(extraction.candidateIdentities.contains("Apple"))
        assertTrue(extraction.candidateIdentities.contains("COOKIES"))
    }

    @Test
    fun fullyBoundCompositeFoodIdentityWinsBeforeDecomposition() {
        val extraction = extract(food = "Breakfast cookie", major = "COOKIES")

        assertEquals("Breakfast cookie", extraction.primaryIdentity)
        assertEquals(emptyList(), extraction.modifiers)
        assertEquals("EXPLICIT_PRODUCT_IDENTITY", extraction.extractionPath)
    }

    @Test
    fun parentheticalCountryAndStudyContextCannotBecomeIdentityOrModifier() {
        val extraction = extract(food = "Apple (LU, France and Spain)", major = "FRUIT")

        assertEquals("Apple", extraction.primaryIdentity)
        assertEquals(emptyList(), extraction.modifiers)
    }

    @Test
    fun numericGiAndMeasurementFieldsAreNeverIdentitySignals() {
        val extraction = extract(food = "Unknown sample", major = "", gi = "999", subjects = "1000", method = "YSI")

        assertNull(extraction.primaryIdentity)
        assertEquals(emptyList(), extraction.modifiers)
        assertEquals(HimGlycemicIndexPrimaryIdentityResolutionV1.UNRESOLVED, extraction.resolution)
    }

    @Test
    fun nonMeasurementRecordKindsFailClosed() {
        val summary = HimGlycemicIndexEvidenceProjectionV1.fromProjectionJson(
            """
                {"recordKind":"MEAN_SUMMARY","arrayOrdinal":2,"pageNumber":1,"sourceContext":{"majorCategory":"COOKIES","subcategory":null,"deeperHeading":null},"lexicalText":{"lexicalValue":"Vanille","status":"PRESENT"}}
            """.trimIndent(),
            2,
        )

        assertFailsWith<IllegalArgumentException> {
            HimGlycemicIndexDeterministicPrimaryIdentityExtractorV1.extract(summary, catalog, authority)
        }
    }

    @Test
    fun repeatedIdenticalInputPreservesIdentityAndModifierOrder() {
        val first = extract("Vanilla biscuit", "COOKIES")
        val second = extract("Vanilla biscuit", "COOKIES")

        assertEquals(first, second)
        assertEquals(listOf("Vanilla"), first.modifiers)
        assertEquals(first.candidateIdentities, second.candidateIdentities)
    }

    @Test
    fun claimedDirectCannotOverrideForeignOrUnresolvedResult() {
        val foreign = extract("Apple", "FRUIT")
        val unresolved = extract("Unknown sample", "")
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
        food: String?,
        major: String,
        subcategory: String? = null,
        deeperHeading: String? = null,
        gi: String = "73",
        subjects: String = "10",
        method: String = "YSI",
    ) = HimGlycemicIndexDeterministicPrimaryIdentityExtractorV1.extract(
        record(food, major, subcategory, deeperHeading, gi, subjects, method),
        catalog,
        authority,
    )

    private fun record(
        food: String?,
        major: String,
        subcategory: String? = null,
        deeperHeading: String? = null,
        gi: String = "73",
        subjects: String = "10",
        method: String = "YSI",
    ) = HimGlycemicIndexEvidenceProjectionV1.fromProjectionJson(
        """
            {"recordKind":"MEASUREMENT","arrayOrdinal":1,"foodNumber":1,"pageNumber":1,"foodItem":{"lexicalValue":${jsonString(food)},"status":"PRESENT"},"country":{"lexicalValue":"France","status":"PRESENT"},"year":{"lexicalValue":"2010","status":"PRESENT"},"gi":{"lexicalValue":"${json(gi)}","status":"PRESENT"},"sem":{"lexicalValue":"6","status":"PRESENT"},"gl":{"lexicalValue":"11","status":"PRESENT"},"subjects":{"lexicalValue":"${json(subjects)}","status":"PRESENT"},"availableCarbohydrate":{"lexicalValue":"50","status":"PRESENT"},"testPortion":{"lexicalValue":"119.0","status":"PRESENT"},"referenceFoodTime":{"lexicalValue":"Bread, 2h","status":"PRESENT"},"timepoints":{"lexicalValue":"Standard","status":"PRESENT"},"sampleCollection":{"lexicalValue":"Capillary","status":"PRESENT"},"analysisMethod":{"lexicalValue":"${json(method)}","status":"PRESENT"},"referenceCode":{"lexicalValue":"UO7","status":"PRESENT"},"sourceContext":{"majorCategory":${jsonString(major)},"subcategory":${jsonString(subcategory)},"deeperHeading":${jsonString(deeperHeading)}}}
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
                    "Keks" -> listOf(alias("Cookies", "a00001"), alias("Biscuit", "a00002"), alias("Breakfast cookie", "a00003"))
                    "Apfel" -> listOf(alias("Apple", "a00004"))
                    "Vanille" -> listOf(alias("Vanilla", "a00005"))
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

    private fun jsonString(value: String?): String = value?.let { "\"${json(it)}\"" } ?: "null"
    private fun json(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun normalize(value: String): String = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFC)
        .trim().replace(Regex("\\s+"), " ").lowercase(java.util.Locale.ROOT)
}
