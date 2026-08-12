package de.shopme.testing.system.tools.knowledge.catalog.truecanonical.assignment

import de.shopme.tools.knowledge.catalog.taxonomy.PrimaryCanonicalGermanFoodTaxonomyRegistry
import de.shopme.tools.knowledge.catalog.truecanonical.TrueCanonicalGermanFoodIdentityRegistry
import de.shopme.tools.knowledge.catalog.truecanonical.assignment.CanonicalFoodTaxonomyAssignmentRegistry
import de.shopme.tools.knowledge.catalog.truecanonical.assignment.CanonicalFoodTaxonomyAssignmentValidator
import kotlin.test.Test
import kotlin.test.assertEquals

class CanonicalFoodTaxonomyAssignmentValidatorTest {

    @Test
    fun everyCanonicalIdentityHasExactlyOneValidTaxonomyAssignment() {

        val result =
            CanonicalFoodTaxonomyAssignmentValidator()
                .validate(
                    identities =
                        TrueCanonicalGermanFoodIdentityRegistry
                            .identities,

                    taxonomy =
                        PrimaryCanonicalGermanFoodTaxonomyRegistry
                            .taxonomy,

                    assignments =
                        CanonicalFoodTaxonomyAssignmentRegistry
                            .assignments
                )

        assertEquals(
            0,
            result.orphanAssignmentCount
        )

        assertEquals(
            0,
            result.invalidDepartmentCount
        )

        assertEquals(
            0,
            result.invalidGroupCount
        )
    }
}