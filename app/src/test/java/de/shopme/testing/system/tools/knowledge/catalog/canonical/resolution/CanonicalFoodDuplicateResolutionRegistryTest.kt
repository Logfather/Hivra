package de.shopme.testing.system.tools.knowledge.catalog.canonical.resolution

import de.shopme.tools.knowledge.catalog.canonical.resolution.CanonicalFoodDuplicateResolutionRegistry
import de.shopme.tools.knowledge.catalog.canonical.resolution.DuplicateResolutionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CanonicalFoodDuplicateResolutionRegistryTest {

    @Test
    fun germanCompoundWinsOverSeparatedForm() {

        val resolution =
            CanonicalFoodDuplicateResolutionRegistry
                .find(
                    listOf(
                        "Basmati Reis",
                        "Basmatireis"
                    )
                )

        assertNotNull(
            resolution
        )

        assertEquals(
            DuplicateResolutionType.MERGE,
            resolution.type
        )

        assertEquals(
            "Basmatireis",
            resolution.canonicalItemname
        )
    }

    @Test
    fun pluralWinsForCountableFoods() {

        val resolution =
            CanonicalFoodDuplicateResolutionRegistry
                .find(
                    listOf(
                        "Erdbeere",
                        "Erdbeeren"
                    )
                )

        assertNotNull(
            resolution
        )

        assertEquals(
            "Erdbeeren",
            resolution.canonicalItemname
        )
    }

    @Test
    fun invalidBeanMixGroupIsRejected() {

        val resolution =
            CanonicalFoodDuplicateResolutionRegistry
                .find(
                    listOf(
                        "Bohnen-Mix",
                        "Bohnenmix"
                    )
                )

        assertNotNull(
            resolution
        )

        assertEquals(
            DuplicateResolutionType.REJECT_GROUP,
            resolution.type
        )
    }

    @Test
    fun vegetableLasagnaIsReadyMeal() {

        val resolution =
            CanonicalFoodDuplicateResolutionRegistry
                .find(
                    listOf(
                        "Gemüse Lasagne",
                        "Gemüselasagne"
                    )
                )

        assertNotNull(
            resolution
        )

        assertEquals(
            "Gemüselasagne",
            resolution.canonicalItemname
        )

        assertEquals(
            "ready-meals",
            resolution.canonicalCategory
        )
    }
}