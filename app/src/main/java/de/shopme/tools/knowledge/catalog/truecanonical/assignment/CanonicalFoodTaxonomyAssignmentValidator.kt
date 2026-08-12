package de.shopme.tools.knowledge.catalog.truecanonical.assignment

import de.shopme.tools.knowledge.catalog.taxonomy.model.PrimaryFoodTaxonomy
import de.shopme.tools.knowledge.catalog.truecanonical.model.TrueCanonicalFoodIdentity

data class CanonicalFoodTaxonomyAssignmentValidationResult(
    val identityCount: Int,
    val assignmentCount: Int,
    val assignedIdentityCount: Int,
    val unassignedIdentityCount: Int,
    val orphanAssignmentCount: Int,
    val invalidDepartmentCount: Int,
    val invalidGroupCount: Int,
    val duplicateAssignmentCount: Int,
    val unassignedNormalizedKeys: List<String>,
    val orphanNormalizedKeys: List<String>
) {

    val valid: Boolean
        get() =
            unassignedIdentityCount ==
                    0 &&
                    orphanAssignmentCount ==
                    0 &&
                    invalidDepartmentCount ==
                    0 &&
                    invalidGroupCount ==
                    0 &&
                    duplicateAssignmentCount ==
                    0
}

class CanonicalFoodTaxonomyAssignmentValidator {

    fun validate(
        identities: List<TrueCanonicalFoodIdentity>,
        taxonomy: PrimaryFoodTaxonomy,
        assignments:
        Map<String, de.shopme.tools.knowledge.catalog.truecanonical.assignment.model.CanonicalFoodTaxonomyAssignment>
    ): CanonicalFoodTaxonomyAssignmentValidationResult {

        val identityKeys =
            identities
                .map {
                    it.normalized
                }
                .toSet()

        val assignmentKeys =
            assignments.keys

        val departments =
            taxonomy.departments
                .associateBy {
                    it.id
                }

        val unassigned =
            identityKeys -
                    assignmentKeys

        val orphan =
            assignmentKeys -
                    identityKeys

        val invalidDepartments =
            assignments.values
                .filter {
                    it.departmentId !in
                            departments
                }

        val invalidGroups =
            assignments.values
                .filter { assignment ->

                    val department =
                        departments[
                            assignment.departmentId
                        ]
                            ?: return@filter false

                    department.groups
                        .none {
                            it.id ==
                                    assignment.groupId
                        }
                }

        /*
         * Map kann technisch keine identischen normalized
         * Keys mehrfach enthalten.
         *
         * Trotzdem behalten wir dieses Feld explizit im
         * Report, damit die Invariante sichtbar bleibt.
         */
        val duplicateAssignmentCount =
            assignments.values
                .groupBy {
                    it.normalized
                }
                .count {
                    it.value.size >
                            1
                }

        return CanonicalFoodTaxonomyAssignmentValidationResult(
            identityCount =
                identities.size,

            assignmentCount =
                assignments.size,

            assignedIdentityCount =
                identityKeys
                    .intersect(
                        assignmentKeys
                    )
                    .size,

            unassignedIdentityCount =
                unassigned.size,

            orphanAssignmentCount =
                orphan.size,

            invalidDepartmentCount =
                invalidDepartments.size,

            invalidGroupCount =
                invalidGroups.size,

            duplicateAssignmentCount =
                duplicateAssignmentCount,

            unassignedNormalizedKeys =
                unassigned.sorted(),

            orphanNormalizedKeys =
                orphan.sorted()
        )
    }
}