package de.shopme.testing.system.tools.knowledge.catalog.duplicate

import de.shopme.testing.system.tools.knowledge.catalog.model.CatalogFoodItem
import de.shopme.testing.system.tools.knowledge.catalog.model.IndexedCatalogFoodItem
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CatalogNormalizationChange
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CatalogNormalizationChangeType
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CatalogNormalizationResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CatalogDuplicateDetectorTest {

    private val detector = CatalogDuplicateDetector()

    @Test
    fun detectExactNormalizedKeyDuplicates() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 0,
                itemName = "Paprika rot",
                normalized = "paprika-rot"
            ),
            indexedItem(
                sourceIndex = 1,
                itemName = "Rote Paprika",
                normalized = "paprika-rot"
            )
        )

        val normalizations = listOf(
            normalization(
                sourceIndex = 0,
                originalItemName = "Paprika rot",
                originalNormalized = "paprika-rot",
                canonicalName = "Paprika rot",
                normalizedKey = "paprika-rot"
            ),
            normalization(
                sourceIndex = 1,
                originalItemName = "Rote Paprika",
                originalNormalized = "paprika-rot",
                canonicalName = "Rote Paprika",
                normalizedKey = "paprika-rot"
            )
        )

        val groups = detector.detect(
            entries = entries,
            normalizations = normalizations
        )

        assertEquals(
            1,
            groups.size,
            "Entries with an identical canonical normalized key must form " +
                    "one duplicate group."
        )

        val group = groups.single()

        assertEquals(
            2,
            group.members.size
        )

        assertEquals(
            setOf(0, 1),
            group.members
                .map { it.sourceIndex }
                .toSet()
        )

        assertTrue(
            group.members.all {
                it.normalizedKey == "paprika-rot"
            }
        )

        assertTrue(
            group.confidence in 0.0..1.0
        )

        assertTrue(
            group.members.all {
                it.matchScore in 0.0..1.0
            }
        )
    }

    @Test
    fun detectCanonicalNameDuplicatesWithDifferentOriginalKeys() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 2,
                itemName = "Hähnchen Brustfilet",
                normalized = "haehnchen-brust"
            ),
            indexedItem(
                sourceIndex = 5,
                itemName = "Hähnchen-Brustfilet",
                normalized = "haehnchen-brustfilet"
            )
        )

        val normalizations = listOf(
            normalization(
                sourceIndex = 2,
                originalItemName = "Hähnchen Brustfilet",
                originalNormalized = "haehnchen-brust",
                canonicalName = "Hähnchen-Brustfilet",
                normalizedKey = "haehnchen-brustfilet"
            ),
            normalization(
                sourceIndex = 5,
                originalItemName = "Hähnchen-Brustfilet",
                originalNormalized = "haehnchen-brustfilet",
                canonicalName = "Hähnchen-Brustfilet",
                normalizedKey = "haehnchen-brustfilet"
            )
        )

        val groups = detector.detect(
            entries = entries,
            normalizations = normalizations
        )

        assertEquals(1, groups.size)

        val group = groups.single()

        assertEquals(
            setOf(2, 5),
            group.members
                .map { it.sourceIndex }
                .toSet()
        )

        assertEquals(
            "Hähnchen-Brustfilet",
            group.canonicalCandidateName
        )
    }

    @Test
    fun detectCaseAndWhitespaceEquivalentNames() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 0,
                itemName = "Naturjoghurt"
            ),
            indexedItem(
                sourceIndex = 1,
                itemName = " naturjoghurt "
            ),
            indexedItem(
                sourceIndex = 2,
                itemName = "NATURJOGHURT"
            )
        )

        val normalizations = listOf(
            normalization(
                sourceIndex = 0,
                originalItemName = "Naturjoghurt",
                canonicalName = "Naturjoghurt",
                normalizedKey = "naturjoghurt"
            ),
            normalization(
                sourceIndex = 1,
                originalItemName = " naturjoghurt ",
                canonicalName = "Naturjoghurt",
                normalizedKey = "naturjoghurt"
            ),
            normalization(
                sourceIndex = 2,
                originalItemName = "NATURJOGHURT",
                canonicalName = "Naturjoghurt",
                normalizedKey = "naturjoghurt"
            )
        )

        val groups = detector.detect(
            entries = entries,
            normalizations = normalizations
        )

        assertEquals(1, groups.size)
        assertEquals(3, groups.single().members.size)
    }

    @Test
    fun detectUmlautAndAsciiEquivalentNames() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 0,
                itemName = "Käse"
            ),
            indexedItem(
                sourceIndex = 1,
                itemName = "Kaese"
            )
        )

        val normalizations = listOf(
            normalization(
                sourceIndex = 0,
                originalItemName = "Käse",
                canonicalName = "Käse",
                normalizedKey = "kaese"
            ),
            normalization(
                sourceIndex = 1,
                originalItemName = "Kaese",
                canonicalName = "Kaese",
                normalizedKey = "kaese"
            )
        )

        val groups = detector.detect(
            entries = entries,
            normalizations = normalizations
        )

        assertEquals(1, groups.size)

        assertEquals(
            setOf("Käse", "Kaese"),
            groups.single()
                .members
                .map { it.itemName }
                .toSet()
        )
    }

    @Test
    fun doNotGroupDistinctCanonicalFoods() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 0,
                itemName = "Paprika rot"
            ),
            indexedItem(
                sourceIndex = 1,
                itemName = "Paprika gelb"
            ),
            indexedItem(
                sourceIndex = 2,
                itemName = "Paprika grün"
            )
        )

        val normalizations = listOf(
            normalization(
                sourceIndex = 0,
                originalItemName = "Paprika rot",
                canonicalName = "Paprika rot",
                normalizedKey = "paprika-rot"
            ),
            normalization(
                sourceIndex = 1,
                originalItemName = "Paprika gelb",
                canonicalName = "Paprika gelb",
                normalizedKey = "paprika-gelb"
            ),
            normalization(
                sourceIndex = 2,
                originalItemName = "Paprika grün",
                canonicalName = "Paprika grün",
                normalizedKey = "paprika-gruen"
            )
        )

        val groups = detector.detect(
            entries = entries,
            normalizations = normalizations
        )

        assertTrue(
            groups.isEmpty(),
            "Distinct canonical food variants must not be grouped merely " +
                    "because they share a base noun."
        )
    }

    @Test
    fun doNotGroupDifferentFatLevels() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 0,
                itemName = "Naturjoghurt 1,5 %"
            ),
            indexedItem(
                sourceIndex = 1,
                itemName = "Naturjoghurt 3,5 %"
            ),
            indexedItem(
                sourceIndex = 2,
                itemName = "Naturjoghurt 10 %"
            )
        )

        val normalizations = listOf(
            normalization(
                sourceIndex = 0,
                originalItemName = "Naturjoghurt 1,5 %",
                canonicalName = "Naturjoghurt 1,5 %",
                normalizedKey = "naturjoghurt-1-5-prozent"
            ),
            normalization(
                sourceIndex = 1,
                originalItemName = "Naturjoghurt 3,5 %",
                canonicalName = "Naturjoghurt 3,5 %",
                normalizedKey = "naturjoghurt-3-5-prozent"
            ),
            normalization(
                sourceIndex = 2,
                originalItemName = "Naturjoghurt 10 %",
                canonicalName = "Naturjoghurt 10 %",
                normalizedKey = "naturjoghurt-10-prozent"
            )
        )

        val groups = detector.detect(
            entries = entries,
            normalizations = normalizations
        )

        assertTrue(
            groups.isEmpty(),
            "Different fat levels are separate canonical foods."
        )
    }

    @Test
    fun preserveCutsAndPreparationFormsAsSeparateFoods() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 0,
                itemName = "Kartoffel roh"
            ),
            indexedItem(
                sourceIndex = 1,
                itemName = "Kartoffel gekocht"
            ),
            indexedItem(
                sourceIndex = 2,
                itemName = "Kartoffelpüree"
            )
        )

        val normalizations = listOf(
            normalization(
                sourceIndex = 0,
                originalItemName = "Kartoffel roh",
                canonicalName = "Kartoffel roh",
                normalizedKey = "kartoffel-roh"
            ),
            normalization(
                sourceIndex = 1,
                originalItemName = "Kartoffel gekocht",
                canonicalName = "Kartoffel gekocht",
                normalizedKey = "kartoffel-gekocht"
            ),
            normalization(
                sourceIndex = 2,
                originalItemName = "Kartoffelpüree",
                canonicalName = "Kartoffelpüree",
                normalizedKey = "kartoffelpueree"
            )
        )

        val groups = detector.detect(
            entries = entries,
            normalizations = normalizations
        )

        assertTrue(
            groups.isEmpty(),
            "Raw, cooked and processed preparation forms must remain separate."
        )
    }

    @Test
    fun useOneCanonicalCandidatePerGroup() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 0,
                itemName = " Apfel "
            ),
            indexedItem(
                sourceIndex = 1,
                itemName = "Apfel"
            ),
            indexedItem(
                sourceIndex = 2,
                itemName = "APFEL"
            )
        )

        val normalizations = listOf(
            normalization(
                sourceIndex = 0,
                originalItemName = " Apfel ",
                canonicalName = "Apfel",
                normalizedKey = "apfel",
                changes = listOf(
                    change(
                        type = CatalogNormalizationChangeType
                            .TRIMMED_WHITESPACE,
                        field = "itemname",
                        before = " Apfel ",
                        after = "Apfel"
                    )
                )
            ),
            normalization(
                sourceIndex = 1,
                originalItemName = "Apfel",
                originalNormalized = "apfel",
                canonicalName = "Apfel",
                normalizedKey = "apfel",
                changes = emptyList()
            ),
            normalization(
                sourceIndex = 2,
                originalItemName = "APFEL",
                canonicalName = "Apfel",
                normalizedKey = "apfel",
                changes = listOf(
                    change(
                        type = CatalogNormalizationChangeType.NORMALIZED_CASE,
                        field = "itemname",
                        before = "APFEL",
                        after = "Apfel"
                    )
                )
            )
        )

        val group = detector.detect(
            entries = entries,
            normalizations = normalizations
        ).single()

        assertEquals(
            1,
            group.canonicalCandidateSourceIndex,
            "The already canonical entry should be preferred as group target."
        )

        assertEquals(
            "Apfel",
            group.canonicalCandidateName
        )

        assertEquals(
            1,
            group.members.count {
                it.sourceIndex ==
                        group.canonicalCandidateSourceIndex
            }
        )
    }

    @Test
    fun canonicalCandidateMustBelongToGroup() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 10,
                itemName = "Brokkoli"
            ),
            indexedItem(
                sourceIndex = 11,
                itemName = "Broccoli"
            )
        )

        val normalizations = listOf(
            normalization(
                sourceIndex = 10,
                originalItemName = "Brokkoli",
                canonicalName = "Brokkoli",
                normalizedKey = "brokkoli"
            ),
            normalization(
                sourceIndex = 11,
                originalItemName = "Broccoli",
                canonicalName = "Brokkoli",
                normalizedKey = "brokkoli"
            )
        )

        val group = detector.detect(
            entries = entries,
            normalizations = normalizations
        ).single()

        assertTrue(
            group.members.any {
                it.sourceIndex ==
                        group.canonicalCandidateSourceIndex
            }
        )
    }

    @Test
    fun groupMembersHaveDuplicateReasons() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 0,
                itemName = "Crème fraîche"
            ),
            indexedItem(
                sourceIndex = 1,
                itemName = "Creme fraiche"
            )
        )

        val normalizations = listOf(
            normalization(
                sourceIndex = 0,
                originalItemName = "Crème fraîche",
                canonicalName = "Crème fraîche",
                normalizedKey = "creme-fraiche"
            ),
            normalization(
                sourceIndex = 1,
                originalItemName = "Creme fraiche",
                canonicalName = "Creme fraiche",
                normalizedKey = "creme-fraiche"
            )
        )

        val group = detector.detect(
            entries = entries,
            normalizations = normalizations
        ).single()

        group.members.forEach { member ->
            assertTrue(
                member.reasons.isNotEmpty(),
                "Every duplicate candidate must explain why it belongs to " +
                        "the group."
            )

            assertEquals(
                member.reasons
                    .distinct()
                    .size,
                member.reasons.size
            )
        }
    }

    @Test
    fun scoresAndConfidenceAreFiniteAndBounded() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 0,
                itemName = "Öl"
            ),
            indexedItem(
                sourceIndex = 1,
                itemName = "Oel"
            )
        )

        val normalizations = listOf(
            normalization(
                sourceIndex = 0,
                originalItemName = "Öl",
                canonicalName = "Öl",
                normalizedKey = "oel"
            ),
            normalization(
                sourceIndex = 1,
                originalItemName = "Oel",
                canonicalName = "Oel",
                normalizedKey = "oel"
            )
        )

        val group = detector.detect(
            entries = entries,
            normalizations = normalizations
        ).single()

        assertTrue(group.confidence.isFinite())
        assertTrue(group.confidence in 0.0..1.0)

        group.members.forEach { member ->
            assertTrue(member.matchScore.isFinite())
            assertTrue(member.matchScore in 0.0..1.0)
        }
    }

    @Test
    fun groupIdsAreUniqueAndStable() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 0,
                itemName = "Apfel"
            ),
            indexedItem(
                sourceIndex = 1,
                itemName = "APFEL"
            ),
            indexedItem(
                sourceIndex = 2,
                itemName = "Birne"
            ),
            indexedItem(
                sourceIndex = 3,
                itemName = "BIRNE"
            )
        )

        val normalizations = listOf(
            normalization(
                sourceIndex = 0,
                originalItemName = "Apfel",
                canonicalName = "Apfel",
                normalizedKey = "apfel"
            ),
            normalization(
                sourceIndex = 1,
                originalItemName = "APFEL",
                canonicalName = "Apfel",
                normalizedKey = "apfel"
            ),
            normalization(
                sourceIndex = 2,
                originalItemName = "Birne",
                canonicalName = "Birne",
                normalizedKey = "birne"
            ),
            normalization(
                sourceIndex = 3,
                originalItemName = "BIRNE",
                canonicalName = "Birne",
                normalizedKey = "birne"
            )
        )

        val first = detector.detect(entries, normalizations)
        val second = detector.detect(entries, normalizations)

        assertEquals(first, second)

        assertEquals(
            first.size,
            first.map { it.groupId }.distinct().size
        )

        assertTrue(
            first.all { it.groupId.isNotBlank() }
        )
    }

    @Test
    fun detectionIsIndependentOfInputOrder() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 8,
                itemName = "Apfel"
            ),
            indexedItem(
                sourceIndex = 2,
                itemName = "APFEL"
            ),
            indexedItem(
                sourceIndex = 7,
                itemName = "Birne"
            ),
            indexedItem(
                sourceIndex = 4,
                itemName = "BIRNE"
            )
        )

        val normalizations = listOf(
            normalization(
                sourceIndex = 8,
                originalItemName = "Apfel",
                canonicalName = "Apfel",
                normalizedKey = "apfel"
            ),
            normalization(
                sourceIndex = 2,
                originalItemName = "APFEL",
                canonicalName = "Apfel",
                normalizedKey = "apfel"
            ),
            normalization(
                sourceIndex = 7,
                originalItemName = "Birne",
                canonicalName = "Birne",
                normalizedKey = "birne"
            ),
            normalization(
                sourceIndex = 4,
                originalItemName = "BIRNE",
                canonicalName = "Birne",
                normalizedKey = "birne"
            )
        )

        val forward = detector.detect(
            entries = entries,
            normalizations = normalizations
        )

        val reversed = detector.detect(
            entries = entries.reversed(),
            normalizations = normalizations.reversed()
        )

        assertEquals(
            forward,
            reversed,
            "Duplicate detection must not depend on input ordering."
        )
    }

    @Test
    fun groupsAreDeterministicallySorted() {
        val entries = listOf(
            indexedItem(0, "Apfel"),
            indexedItem(1, "APFEL"),
            indexedItem(2, "Birne"),
            indexedItem(3, "BIRNE"),
            indexedItem(4, "Tomate"),
            indexedItem(5, "TOMATE")
        )

        val normalizations = listOf(
            normalization(0, "Apfel", canonicalName = "Apfel", normalizedKey = "apfel"),
            normalization(1, "APFEL", canonicalName = "Apfel", normalizedKey = "apfel"),
            normalization(2, "Birne", canonicalName = "Birne", normalizedKey = "birne"),
            normalization(3, "BIRNE", canonicalName = "Birne", normalizedKey = "birne"),
            normalization(4, "Tomate", canonicalName = "Tomate", normalizedKey = "tomate"),
            normalization(5, "TOMATE", canonicalName = "Tomate", normalizedKey = "tomate")
        )

        val groups = detector.detect(entries, normalizations)

        assertEquals(
            groups.sortedWith(groupComparator()),
            groups,
            "Duplicate groups must be returned in deterministic order."
        )
    }

    @Test
    fun membersAreDeterministicallySorted() {
        val entries = listOf(
            indexedItem(9, "APFEL"),
            indexedItem(2, " Apfel "),
            indexedItem(5, "Apfel")
        )

        val normalizations = listOf(
            normalization(
                sourceIndex = 9,
                originalItemName = "APFEL",
                canonicalName = "Apfel",
                normalizedKey = "apfel"
            ),
            normalization(
                sourceIndex = 2,
                originalItemName = " Apfel ",
                canonicalName = "Apfel",
                normalizedKey = "apfel"
            ),
            normalization(
                sourceIndex = 5,
                originalItemName = "Apfel",
                originalNormalized = "apfel",
                canonicalName = "Apfel",
                normalizedKey = "apfel"
            )
        )

        val members = detector.detect(
            entries = entries,
            normalizations = normalizations
        ).single().members

        assertEquals(
            members.sortedWith(memberComparator()),
            members,
            "Duplicate members must be returned in deterministic order."
        )
    }

    @Test
    fun eachSourceIndexBelongsToAtMostOneGroup() {
        val entries = listOf(
            indexedItem(0, "Apfel"),
            indexedItem(1, "APFEL"),
            indexedItem(2, "Birne"),
            indexedItem(3, "BIRNE")
        )

        val normalizations = listOf(
            normalization(0, "Apfel", canonicalName = "Apfel", normalizedKey = "apfel"),
            normalization(1, "APFEL", canonicalName = "Apfel", normalizedKey = "apfel"),
            normalization(2, "Birne", canonicalName = "Birne", normalizedKey = "birne"),
            normalization(3, "BIRNE", canonicalName = "Birne", normalizedKey = "birne")
        )

        val groups = detector.detect(entries, normalizations)

        val allMemberIndices = groups.flatMap { group ->
            group.members.map { it.sourceIndex }
        }

        assertEquals(
            allMemberIndices.distinct().size,
            allMemberIndices.size,
            "A sourceIndex must not occur in multiple duplicate groups."
        )
    }

    @Test
    fun ignoreSingletons() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 0,
                itemName = "Apfel"
            )
        )

        val normalizations = listOf(
            normalization(
                sourceIndex = 0,
                originalItemName = "Apfel",
                canonicalName = "Apfel",
                normalizedKey = "apfel"
            )
        )

        val groups = detector.detect(entries, normalizations)

        assertTrue(groups.isEmpty())
    }

    @Test
    fun emptyInputProducesEmptyResult() {
        assertTrue(
            detector.detect(
                entries = emptyList(),
                normalizations = emptyList()
            ).isEmpty()
        )
    }

    @Test
    fun rejectDuplicateEntrySourceIndices() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 4,
                itemName = "Apfel"
            ),
            indexedItem(
                sourceIndex = 4,
                itemName = "Birne"
            )
        )

        val normalizations = listOf(
            normalization(
                sourceIndex = 4,
                originalItemName = "Apfel",
                canonicalName = "Apfel",
                normalizedKey = "apfel"
            )
        )

        val exception =
            assertFailsWith<IllegalArgumentException> {
                detector.detect(
                    entries = entries,
                    normalizations = normalizations
                )
            }

        assertTrue(
            exception.message
                ?.contains("duplicate sourceIndex") == true
        )
    }

    @Test
    fun rejectDuplicateNormalizationSourceIndices() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 0,
                itemName = "Apfel"
            )
        )

        val normalizations = listOf(
            normalization(
                sourceIndex = 0,
                originalItemName = "Apfel",
                canonicalName = "Apfel",
                normalizedKey = "apfel"
            ),
            normalization(
                sourceIndex = 0,
                originalItemName = "Apfel",
                canonicalName = "Apfel",
                normalizedKey = "apfel"
            )
        )

        assertFailsWith<IllegalArgumentException> {
            detector.detect(entries, normalizations)
        }
    }

    @Test
    fun rejectMissingNormalization() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 0,
                itemName = "Apfel"
            ),
            indexedItem(
                sourceIndex = 1,
                itemName = "Birne"
            )
        )

        val normalizations = listOf(
            normalization(
                sourceIndex = 0,
                originalItemName = "Apfel",
                canonicalName = "Apfel",
                normalizedKey = "apfel"
            )
        )

        val exception =
            assertFailsWith<IllegalArgumentException> {
                detector.detect(entries, normalizations)
            }

        assertNotNull(exception.message)

        assertTrue(
            exception.message
                ?.contains("1") == true,
            "Error should identify the missing sourceIndex."
        )
    }

    @Test
    fun rejectNormalizationForUnknownEntry() {
        val entries = listOf(
            indexedItem(
                sourceIndex = 0,
                itemName = "Apfel"
            )
        )

        val normalizations = listOf(
            normalization(
                sourceIndex = 0,
                originalItemName = "Apfel",
                canonicalName = "Apfel",
                normalizedKey = "apfel"
            ),
            normalization(
                sourceIndex = 7,
                originalItemName = "Birne",
                canonicalName = "Birne",
                normalizedKey = "birne"
            )
        )

        val exception =
            assertFailsWith<IllegalArgumentException> {
                detector.detect(entries, normalizations)
            }

        assertTrue(
            exception.message
                ?.contains("7") == true
        )
    }

    @Test
    fun everyGroupContainsAtLeastTwoMembers() {
        val entries = listOf(
            indexedItem(0, "Apfel"),
            indexedItem(1, "APFEL"),
            indexedItem(2, "Birne")
        )

        val normalizations = listOf(
            normalization(0, "Apfel", canonicalName = "Apfel", normalizedKey = "apfel"),
            normalization(1, "APFEL", canonicalName = "Apfel", normalizedKey = "apfel"),
            normalization(2, "Birne", canonicalName = "Birne", normalizedKey = "birne")
        )

        val groups = detector.detect(entries, normalizations)

        assertTrue(
            groups.all { it.members.size >= 2 }
        )
    }

    @Test
    fun recommendationIsPresentForEveryGroup() {
        val entries = listOf(
            indexedItem(0, "Apfel"),
            indexedItem(1, "APFEL")
        )

        val normalizations = listOf(
            normalization(0, "Apfel", canonicalName = "Apfel", normalizedKey = "apfel"),
            normalization(1, "APFEL", canonicalName = "Apfel", normalizedKey = "apfel")
        )

        val group = detector.detect(entries, normalizations).single()

        assertTrue(
            group.recommendation in
                    CatalogDuplicateRecommendation.entries
        )
    }

    private fun indexedItem(
        sourceIndex: Int,
        itemName: String,
        normalized: String? = null,
        category: String = "fruit",
        plural: String? = null,
        colloquial: List<String> = emptyList(),
        phoneticTokens: List<String> = emptyList(),
        autocompleteTokens: List<String> = emptyList()
    ): IndexedCatalogFoodItem =
        IndexedCatalogFoodItem(
            sourceIndex = sourceIndex,
            item = CatalogFoodItem(
                itemname = itemName,
                category = category,
                production = null,
                normalized = normalized,
                plural = plural,
                colloquial = colloquial,
                phoneticTokens = phoneticTokens,
                autocompleteTokens = autocompleteTokens,
                normalizedEnglish = null
            )
        )

    private fun normalization(
        sourceIndex: Int,
        originalItemName: String,
        originalNormalized: String? = null,
        canonicalName: String,
        normalizedKey: String,
        originalPlural: String? = null,
        computedPlural: String? = null,
        originalColloquial: List<String> = emptyList(),
        originalPhoneticTokens: List<String> = emptyList(),
        originalAutocompleteTokens: List<String> = emptyList(),
        normalizedColloquial: List<String> = emptyList(),
        normalizedPhoneticTokens: List<String> = emptyList(),
        normalizedAutocompleteTokens: List<String> = emptyList(),
        changes: List<CatalogNormalizationChange> = emptyList()
    ): CatalogNormalizationResult =
        CatalogNormalizationResult(
            sourceIndex = sourceIndex,
            originalItemName = originalItemName,
            originalNormalized = originalNormalized,
            originalPlural = originalPlural,
            originalColloquial = originalColloquial,
            originalPhoneticTokens = originalPhoneticTokens,
            originalAutocompleteTokens =
                originalAutocompleteTokens,
            computedCanonicalName = canonicalName,
            computedNormalizedKey = normalizedKey,
            computedPlural = computedPlural,
            normalizedColloquial = normalizedColloquial,
            normalizedPhoneticTokens =
                normalizedPhoneticTokens,
            normalizedAutocompleteTokens =
                normalizedAutocompleteTokens,
            changes = changes
        )

    private fun change(
        type: CatalogNormalizationChangeType,
        field: String,
        before: String?,
        after: String?,
        reason: String = "Test normalization change."
    ): CatalogNormalizationChange =
        CatalogNormalizationChange(
            type = type,
            field = field,
            before = before,
            after = after,
            reason = reason
        )

    private fun groupComparator():
            Comparator<CatalogDuplicateGroup> =
        compareByDescending<CatalogDuplicateGroup> {
            it.confidence
        }
            .thenBy {
                it.canonicalCandidateName.lowercase()
            }
            .thenBy {
                it.canonicalCandidateSourceIndex
            }
            .thenBy {
                it.groupId
            }

    private fun memberComparator():
            Comparator<CatalogDuplicateCandidate> =
        compareByDescending<CatalogDuplicateCandidate> {
            it.matchScore
        }
            .thenBy {
                it.itemName.lowercase()
            }
            .thenBy {
                it.normalizedKey
            }
            .thenBy {
                it.sourceIndex
            }
}