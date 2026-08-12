package de.shopme.testing.system.tools.knowledge.catalog.taxonomy

import de.shopme.tools.knowledge.catalog.taxonomy.PrimaryCanonicalGermanFoodTaxonomyRegistry
import de.shopme.tools.knowledge.catalog.taxonomy.PrimaryCanonicalGermanFoodTaxonomyValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PrimaryCanonicalGermanFoodTaxonomyValidatorTest {

    @Test
    fun primaryTaxonomyIsStructurallyValid() {

        val taxonomy =
            PrimaryCanonicalGermanFoodTaxonomyRegistry
                .taxonomy

        PrimaryCanonicalGermanFoodTaxonomyValidator()
            .validate(
                taxonomy
            )

        assertEquals(
            "food",
            taxonomy.domain.id
        )

        assertTrue(
            taxonomy.departments.isNotEmpty()
        )
    }

    @Test
    fun taxonomyContainsExpectedPrimaryDepartments() {

        val departmentNames =
            PrimaryCanonicalGermanFoodTaxonomyRegistry
                .taxonomy
                .departments
                .map {
                    it.name
                }
                .toSet()

        assertTrue(
            "Obst" in
                    departmentNames
        )

        assertTrue(
            "Gemüse" in
                    departmentNames
        )

        assertTrue(
            "Getränke" in
                    departmentNames
        )

        assertTrue(
            "Brot & Backwaren" in
                    departmentNames
        )

        assertTrue(
            "Fleisch & Geflügel" in
                    departmentNames
        )
    }

    @Test
    fun taxonomyDoesNotContainCrossCuttingDepartments() {

        val departmentNames =
            PrimaryCanonicalGermanFoodTaxonomyRegistry
                .taxonomy
                .departments
                .map {
                    it.name.lowercase()
                }

        assertFalse(
            departmentNames.any {
                "tiefkühl" in
                        it
            }
        )

        assertFalse(
            departmentNames.any {
                "konserve" in
                        it
            }
        )

        assertFalse(
            departmentNames.any {
                it ==
                        "international"
            }
        )

        assertFalse(
            departmentNames.any {
                "bio" in
                        it
            }
        )
    }
}