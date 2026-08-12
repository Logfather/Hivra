package de.shopme.testing.system.tools.knowledge.catalog.taxonomy

import de.shopme.tools.knowledge.catalog.taxonomy.PrimaryCanonicalGermanFoodTaxonomyRegistry
import kotlin.test.Test
import kotlin.test.assertFalse

class PrimaryFoodTaxonomyCrossCuttingSeparationTest {

    @Test
    fun storageStateIsNotPrimaryTaxonomy() {

        val allNames =
            allConceptNames()

        assertFalse(
            allNames.any {
                Regex(
                    """(?i)\b(tiefkühl|tiefgekühlt|gefroren|tk)\b"""
                )
                    .containsMatchIn(
                        it
                    )
            }
        )
    }

    @Test
    fun packagingAndPreservationAreNotPrimaryTaxonomy() {

        val allNames =
            allConceptNames()

        assertFalse(
            allNames.any {
                Regex(
                    """(?i)\b(konserve|konserven|dose|dosen)\b"""
                )
                    .containsMatchIn(
                        it
                    )
            }
        )
    }

    @Test
    fun dietaryClaimsAreNotPrimaryTaxonomy() {

        val allNames =
            allConceptNames()

        assertFalse(
            allNames.any {
                Regex(
                    """(?i)\b(bio|vegan|vegetarisch|glutenfrei|zuckerfrei)\b"""
                )
                    .containsMatchIn(
                        it
                    )
            }
        )
    }

    private fun allConceptNames(): List<String> {

        val taxonomy =
            PrimaryCanonicalGermanFoodTaxonomyRegistry
                .taxonomy

        return buildList {

            taxonomy.departments
                .forEach { department ->

                    add(
                        department.name
                    )

                    department.groups
                        .forEach { group ->
                            add(
                                group.name
                            )
                        }
                }
        }
    }
}