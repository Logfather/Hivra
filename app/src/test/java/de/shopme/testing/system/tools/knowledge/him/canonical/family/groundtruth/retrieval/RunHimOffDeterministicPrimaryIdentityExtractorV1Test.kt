package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalAlias
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilySourceCatalog
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimLifecycleStatus
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonical
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticEvidenceRelation
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentClassificationV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceProjection
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordKind
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRetrievalIndexRecord
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceSearchText
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimOffDeterministicPrimaryIdentityExtractorV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimOffPrimaryIdentityResolutionV1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RunHimOffDeterministicPrimaryIdentityExtractorV1Test {
    private val catalog = fixtureCatalog(
        "Vanille", "Pudding", "Zucker", "Keks", "Apfelsaft", "Apfel", "Saft", "Alpha", "Beta",
    )
    private val authority = fixtureAuthority()
    private val vanillaFamily = authority.families.first { it.canonicalName == "Vanille" }
    private val puddingFamily = authority.families.first { it.canonicalName == "Pudding" }

    @Test
    fun productNameCanonicalInModifierDoesNotBecomePrimaryOrDirectEvidence() {
        val extraction = extract(
            productName = "Vanille Pudding mit Bourbon-Vanille",
            taxonomy = listOf("Vanille"),
            ingredientText = "Vanille",
        )

        assertEquals("Pudding", extraction.primaryIdentity)
        assertEquals(listOf("Vanille"), extraction.modifiers)
        val alignment = HimEvidenceAlignmentContractV1.evaluate(extraction.toAlignmentInput(), vanillaFamily)
        assertEquals(HimEvidenceAlignmentClassificationV1.CANONICAL_ONLY_AS_MODIFIER, alignment.classification)
        assertFalse(alignment.directEvidenceSupported)
        assertNull(alignment.effectiveEvidenceRelation)
    }

    @Test
    fun closedCompoundsUseCatalogBackedHeadInsteadOfContainedCanonicalToken() {
        assertExtraction("Vanillepudding", "Pudding", listOf("Vanille"))
        assertExtraction("Vanillezucker", "Zucker", listOf("Vanille"))
        assertExtraction("Vanillekeks", "Keks", listOf("Vanille"))
    }

    @Test
    fun exactBoundNaturalCompositeWinsBeforeDecomposition() {
        val extraction = extract("Apfelsaft")

        assertEquals("Apfelsaft", extraction.primaryIdentity)
        assertTrue(extraction.modifiers.isEmpty())
        assertEquals("EXPLICIT_PRODUCT_IDENTITY", extraction.extractionPath)
    }

    @Test
    fun exactCanonicalAndUniqueHyphenatedCompositionResolveDeterministically() {
        val vanilla = extract("Vanille")
        val bourbonVanilla = extract("Bourbon-Vanille")

        assertEquals("Vanille", vanilla.primaryIdentity)
        assertTrue(vanilla.modifiers.isEmpty())
        assertEquals("Vanille", bourbonVanilla.primaryIdentity)
        assertEquals(listOf("Bourbon"), bourbonVanilla.modifiers)
        assertTrue(
            HimEvidenceAlignmentContractV1.evaluate(vanilla.toAlignmentInput(), vanillaFamily).directEvidenceSupported,
        )
        assertTrue(
            HimEvidenceAlignmentContractV1.evaluate(
                bourbonVanilla.toAlignmentInput(),
                vanillaFamily,
            ).directEvidenceSupported,
        )
    }

    @Test
    fun genericNameIsUsedOnlyAfterExplicitIdentityFields() {
        val structured = extract(productName = null, productNameGerman = "Vanille", genericName = "Pudding")
        val generic = extract(productName = null, genericName = "Vanille")

        assertEquals("Vanille", structured.primaryIdentity)
        assertEquals("identity.productNameGerman", structured.identityFieldUsed)
        assertEquals("Vanille", generic.primaryIdentity)
        assertEquals("identity.genericName", generic.identityFieldUsed)
    }

    @Test
    fun ingredientsTaxonomyAndContextCannotCreatePrimaryIdentity() {
        val extraction = extract(
            productName = null,
            genericName = null,
            taxonomy = listOf("Vanille"),
            ingredientText = "Vanille",
            productType = "Dessert",
        )

        assertNull(extraction.primaryIdentity)
        assertEquals(HimOffPrimaryIdentityResolutionV1.MISSING, extraction.resolution)
        assertTrue(extraction.candidateIdentities.isEmpty())
    }

    @Test
    fun brandIsNotPrimaryIdentity() {
        val extraction = extract(productName = "Acme Pudding", brands = listOf("Acme"))

        assertEquals("Pudding", extraction.primaryIdentity)
        assertFalse(extraction.primaryIdentity == "Acme")
    }

    @Test
    fun equalAlternativeIdentityCandidatesAreUnresolvedWithStableOrder() {
        val first = extract("Alpha/Beta")
        val second = extract("Alpha/Beta")

        assertEquals(HimOffPrimaryIdentityResolutionV1.UNRESOLVED, first.resolution)
        assertNull(first.primaryIdentity)
        assertEquals(listOf("Alpha", "Beta"), first.candidateIdentities)
        assertEquals(first, second)
        assertFalse(
            HimEvidenceAlignmentContractV1.evaluate(first.toAlignmentInput(), vanillaFamily).directEvidenceSupported,
        )
    }

    @Test
    fun extractionIsIndependentOfDesiredAlignmentTarget() {
        val record = offRecord("Vanille Pudding mit Bourbon-Vanille")
        val extractionForVanille = HimEvidenceAlignmentContractV1.evaluate(
            HimOffDeterministicPrimaryIdentityExtractorV1.extract(record, catalog, authority).toAlignmentInput(),
            vanillaFamily,
        )
        val extractionForPudding = HimEvidenceAlignmentContractV1.evaluate(
            HimOffDeterministicPrimaryIdentityExtractorV1.extract(record, catalog, authority).toAlignmentInput(),
            puddingFamily,
        )
        val directExtraction = HimOffDeterministicPrimaryIdentityExtractorV1.extract(record, catalog, authority)

        assertEquals("Pudding", directExtraction.primaryIdentity)
        assertEquals(listOf("Vanille"), directExtraction.modifiers)
        assertEquals(HimEvidenceAlignmentClassificationV1.CANONICAL_ONLY_AS_MODIFIER, extractionForVanille.classification)
        assertEquals(HimEvidenceAlignmentClassificationV1.PRIMARY_CANONICAL_MATCH, extractionForPudding.classification)
        assertFalse(extractionForVanille.directEvidenceSupported)
        assertTrue(extractionForPudding.directEvidenceSupported)
    }

    @Test
    fun repeatedIdenticalInputPreservesSemanticResultAndModifierOrder() {
        val first = extract("Vanille Pudding mit Bourbon-Vanille")
        val second = extract("Vanille Pudding mit Bourbon-Vanille")

        assertEquals(first, second)
        assertEquals(listOf("Vanille"), first.modifiers)
    }

    private fun assertExtraction(productName: String, primary: String, modifiers: List<String>) {
        val extraction = extract(productName)
        assertEquals(primary, extraction.primaryIdentity)
        assertEquals(modifiers, extraction.modifiers)
        assertEquals(HimOffPrimaryIdentityResolutionV1.RESOLVED, extraction.resolution)
    }

    private fun extract(
        productName: String?,
        productNameGerman: String? = null,
        genericName: String? = null,
        brands: List<String> = emptyList(),
        taxonomy: List<String> = emptyList(),
        ingredientText: String? = null,
        productType: String? = null,
    ) = HimOffDeterministicPrimaryIdentityExtractorV1.extract(
        offRecord(productName, productNameGerman, genericName, brands, taxonomy, ingredientText, productType),
        catalog,
        authority,
    )

    private fun offRecord(
        productName: String?,
        productNameGerman: String? = null,
        genericName: String? = null,
        brands: List<String> = emptyList(),
        taxonomy: List<String> = emptyList(),
        ingredientText: String? = null,
        productType: String? = null,
    ): HimEvidenceRetrievalIndexRecord {
        val json = """
            {
              "rowOrdinal": 1,
              "source": {"code": "fixture"},
              "identity": {
                "productName": ${jsonString(productName)},
                "productNameGerman": ${jsonString(productNameGerman)},
                "productNameEnglish": null,
                "genericName": ${jsonString(genericName)},
                "genericNameGerman": null,
                "genericNameEnglish": null,
                "brands": [${brands.joinToString(",") { jsonString(it) }}],
                "productType": ${jsonString(productType)},
                "quantity": null,
                "servingSize": null
              },
              "taxonomy": {"categories": [${taxonomy.joinToString(",") { jsonString(it) }}]},
              "ingredients": {"text": ${jsonString(ingredientText)}},
              "quality": {}
            }
        """.trimIndent().replace("\n", "")
        return HimEvidenceRetrievalIndexRecord(
            internalRecordKey = 1,
            sourceRecordReference = HimEvidenceRecordReference.offProduct(1, "fixture"),
            recordKind = HimEvidenceRecordKind.OFF_PRODUCT,
            sourceNativeIdentifiersJson = "{\"code\":\"fixture\",\"rowOrdinal\":1}",
            evidenceProjection = HimEvidenceProjection(json),
            searchText = HimEvidenceSearchText("", "", "", "", ""),
        )
    }

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
                aliases = if (record.itemname == "Vanille") listOf(
                    HimCanonicalAlias(
                        aliasId = HimEntityId("a00001"),
                        aliasName = "Vanilleschote",
                        normalizedName = normalize("Vanilleschote"),
                        lifecycleStatus = HimLifecycleStatus.ACTIVE,
                    ),
                ) else emptyList(),
            )
        },
    )

    private fun jsonString(value: String?): String = value?.let { "\"${it.replace("\\", "\\\\").replace("\"", "\\\"")}\"" } ?: "null"

    private fun normalize(value: String): String =
        java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFC)
            .trim()
            .replace(Regex("\\s+"), " ")
            .lowercase(java.util.Locale.ROOT)
}
