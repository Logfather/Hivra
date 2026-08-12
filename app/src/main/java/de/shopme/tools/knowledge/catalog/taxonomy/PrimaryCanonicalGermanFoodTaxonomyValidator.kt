package de.shopme.tools.knowledge.catalog.taxonomy

import de.shopme.tools.knowledge.catalog.taxonomy.model.PrimaryFoodTaxonomy

class PrimaryCanonicalGermanFoodTaxonomyValidator {

    fun validate(
        taxonomy: PrimaryFoodTaxonomy
    ) {

        validateDomain(
            taxonomy
        )

        validateDepartmentsExist(
            taxonomy
        )

        validateUniqueDepartmentIds(
            taxonomy
        )

        validateUniqueDepartmentNames(
            taxonomy
        )

        validateGroupsExist(
            taxonomy
        )

        validateUniqueGroupIds(
            taxonomy
        )

        validateUniqueGroupNamesWithinDepartment(
            taxonomy
        )

        validateNoCrossCuttingConcepts(
            taxonomy
        )
    }

    private fun validateDomain(
        taxonomy: PrimaryFoodTaxonomy
    ) {

        require(
            taxonomy.domain.id ==
                    "food"
        )

        require(
            taxonomy.domain.name ==
                    "Food"
        )
    }

    private fun validateDepartmentsExist(
        taxonomy: PrimaryFoodTaxonomy
    ) {

        require(
            taxonomy.departments.isNotEmpty()
        ) {
            "Primary food taxonomy contains no departments."
        }
    }

    private fun validateUniqueDepartmentIds(
        taxonomy: PrimaryFoodTaxonomy
    ) {

        val duplicates =
            taxonomy.departments
                .groupBy {
                    it.id
                }
                .filterValues {
                    it.size >
                            1
                }

        require(
            duplicates.isEmpty()
        ) {
            "Duplicate department IDs: ${duplicates.keys}"
        }
    }

    private fun validateUniqueDepartmentNames(
        taxonomy: PrimaryFoodTaxonomy
    ) {

        val duplicates =
            taxonomy.departments
                .groupBy {
                    normalizeName(
                        it.name
                    )
                }
                .filterValues {
                    it.size >
                            1
                }

        require(
            duplicates.isEmpty()
        ) {
            "Duplicate department names: " +
                    duplicates.values.map { entries ->
                        entries.map {
                            it.name
                        }
                    }
        }
    }

    private fun validateGroupsExist(
        taxonomy: PrimaryFoodTaxonomy
    ) {

        val invalid =
            taxonomy.departments
                .filter {
                    it.groups.isEmpty()
                }

        require(
            invalid.isEmpty()
        ) {
            "Departments without groups: " +
                    invalid.map {
                        it.name
                    }
        }
    }

    private fun validateUniqueGroupIds(
        taxonomy: PrimaryFoodTaxonomy
    ) {

        val allGroups =
            taxonomy.departments
                .flatMap { department ->

                    department.groups.map { group ->

                        department.id to
                                group
                    }
                }

        val duplicates =
            allGroups
                .groupBy {
                    it.second.id
                }
                .filterValues {
                    it.size >
                            1
                }

        require(
            duplicates.isEmpty()
        ) {
            "Group IDs must be globally unique: " +
                    duplicates.keys
        }
    }

    private fun validateUniqueGroupNamesWithinDepartment(
        taxonomy: PrimaryFoodTaxonomy
    ) {

        taxonomy.departments
            .forEach { department ->

                val duplicates =
                    department.groups
                        .groupBy {
                            normalizeName(
                                it.name
                            )
                        }
                        .filterValues {
                            it.size >
                                    1
                        }

                require(
                    duplicates.isEmpty()
                ) {
                    "Duplicate group names in department " +
                            "'${department.name}': " +
                            duplicates.keys
                }
            }
    }

    private fun validateNoCrossCuttingConcepts(
        taxonomy: PrimaryFoodTaxonomy
    ) {

        val concepts =
            buildList {

                taxonomy.departments
                    .forEach { department ->

                        add(
                            TaxonomyConcept(
                                path =
                                    department.name,
                                value =
                                    department.name
                            )
                        )

                        department.groups
                            .forEach { group ->

                                add(
                                    TaxonomyConcept(
                                        path =
                                            "${department.name} > ${group.name}",
                                        value =
                                            group.name
                                    )
                                )
                            }
                    }
            }

        val invalid =
            concepts
                .filter { concept ->

                    FORBIDDEN_CROSS_CUTTING_PATTERNS
                        .any { pattern ->

                            pattern.containsMatchIn(
                                concept.value
                            )
                        }
                }

        require(
            invalid.isEmpty()
        ) {
            "Cross-cutting concepts found in primary taxonomy: " +
                    invalid.map {
                        it.path
                    }
        }
    }

    private fun normalizeName(
        value: String
    ): String =
        value
            .lowercase()
            .replace(
                Regex(
                    """[^a-zäöüß0-9]+"""
                ),
                " "
            )
            .trim()

    private data class TaxonomyConcept(
        val path: String,
        val value: String
    )

    companion object {

        /*
         * Diese Konzepte gehören in spätere
         * cross-cutting knowledge dimensions,
         * niemals in die Primary Food Taxonomy.
         */
        private val FORBIDDEN_CROSS_CUTTING_PATTERNS =
            listOf(

                Regex(
                    """(?i)(^|\s)(bio|organic)(\s|$)"""
                ),

                Regex(
                    """(?i)(^|\s)(vegan|vegetarisch)(\s|$)"""
                ),

                Regex(
                    """(?i)(^|\s)(glutenfrei|laktosefrei|zuckerfrei)(\s|$)"""
                ),

                Regex(
                    """(?i)(^|\s)(tiefkühl|tiefgekühlt|gefroren|tk)(\s|$)"""
                ),

                Regex(
                    """(?i)(^|\s)(konserve|konserven|dose|dosen|glas)(\s|$)"""
                ),

                Regex(
                    """(?i)^(international|asiatisch|italienisch|mexikanisch|orientalisch|mediterran)$"""
                )
            )
    }
}