package de.shopme.tools.knowledge.catalog.truecanonical.assignment

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.catalog.taxonomy.PrimaryCanonicalGermanFoodTaxonomyRegistry
import de.shopme.tools.knowledge.catalog.truecanonical.TrueCanonicalGermanFoodIdentityRegistry
import de.shopme.tools.knowledge.catalog.truecanonical.assignment.model.TaxonomyAssignedCanonicalFoodIdentity
import java.io.File

data class CanonicalFoodTaxonomyAssignmentReport(
    val version: Int,
    val identityCount: Int,
    val assignedIdentityCount: Int,
    val unassignedIdentityCount: Int,
    val departmentCount: Int,
    val usedGroupCount: Int,
    val totalTaxonomyGroupCount: Int,
    val variantCount: Int,
    val sourceVariantCount: Int,
    val outputFile: String,
    val validationFile: String
)

class AssignCanonicalFoodIdentitiesToPrimaryTaxonomy {

    fun assign(
        paths: KnowledgeBuildPaths =
            KnowledgeBuildPaths.default()
    ): CanonicalFoodTaxonomyAssignmentReport {

        val identities =
            TrueCanonicalGermanFoodIdentityRegistry
                .identities

        val taxonomy =
            PrimaryCanonicalGermanFoodTaxonomyRegistry
                .taxonomy

        val assignments =
            CanonicalFoodTaxonomyAssignmentRegistry
                .assignments

        val validation =
            CanonicalFoodTaxonomyAssignmentValidator()
                .validate(
                    identities =
                        identities,
                    taxonomy =
                        taxonomy,
                    assignments =
                        assignments
                )

        val outputDirectory =
            paths.projectRoot.resolve(
                "build/knowledge/catalog/taxonomy-assigned"
            )

        require(
            outputDirectory.exists() ||
                    outputDirectory.mkdirs()
        )

        val validationFile =
            paths.reportsRoot.resolve(
                "canonical-food-taxonomy-assignment-validation.json"
            )

        writeJson(
            value =
                validation,
            file =
                validationFile
        )

        /*
         * Der Commit darf nur ein produktives Catalog-
         * Artefakt erzeugen, wenn alle 354 Identities
         * wirklich sauber zugeordnet sind.
         */
        require(
            validation.valid
        ) {
            buildString {

                append(
                    "Canonical food taxonomy assignment is incomplete."
                )

                append(
                    " assigned="
                )
                append(
                    validation.assignedIdentityCount
                )

                append(
                    "/"
                )
                append(
                    validation.identityCount
                )

                if (
                    validation.unassignedNormalizedKeys
                        .isNotEmpty()
                ) {

                    append(
                        ", unassigned="
                    )

                    append(
                        validation.unassignedNormalizedKeys
                    )
                }
            }
        }

        val output =
            identities
                .map { identity ->

                    val assignment =
                        requireNotNull(
                            assignments[
                                identity.normalized
                            ]
                        )

                    TaxonomyAssignedCanonicalFoodIdentity(
                        itemname =
                            identity.itemname,

                        normalized =
                            identity.normalized,

                        departmentId =
                            assignment.departmentId,

                        groupId =
                            assignment.groupId,

                        variants =
                            emptyList(),

                        sourceVariants =
                            emptyList()
                    )
                }
                .sortedBy {
                    it.normalized
                }

        validateOutput(
            output
        )

        val outputFile =
            outputDirectory.resolve(
                "canonical-food-catalog.taxonomy-assigned.json"
            )

        writeJson(
            value =
                output,
            file =
                outputFile
        )

        val report =
            CanonicalFoodTaxonomyAssignmentReport(
                version =
                    1,

                identityCount =
                    identities.size,

                assignedIdentityCount =
                    output.size,

                unassignedIdentityCount =
                    0,

                departmentCount =
                    output
                        .map {
                            it.departmentId
                        }
                        .distinct()
                        .size,

                usedGroupCount =
                    output
                        .map {
                            it.groupId
                        }
                        .distinct()
                        .size,

                totalTaxonomyGroupCount =
                    taxonomy.departments
                        .sumOf {
                            it.groups.size
                        },

                variantCount =
                    output.sumOf {
                        it.variants.size
                    },

                sourceVariantCount =
                    output.sumOf {
                        it.sourceVariants.size
                    },

                outputFile =
                    outputFile.path,

                validationFile =
                    validationFile.path
            )

        writeJson(
            value =
                report,

            file =
                paths.reportsRoot.resolve(
                    "canonical-food-taxonomy-assignment.json"
                )
        )

        return report
    }

    private fun validateOutput(
        entries:
        List<TaxonomyAssignedCanonicalFoodIdentity>
    ) {

        require(
            entries.isNotEmpty()
        )

        require(
            entries
                .map {
                    it.normalized
                }
                .distinct()
                .size ==
                    entries.size
        )

        require(
            entries.none {
                it.departmentId.isBlank()
            }
        )

        require(
            entries.none {
                it.groupId.isBlank()
            }
        )

        require(
            entries.none {
                it.variants.isNotEmpty()
            }
        )

        require(
            entries.none {
                it.sourceVariants.isNotEmpty()
            }
        )
    }

    private fun writeJson(
        value: Any,
        file: File
    ) {

        val parent =
            requireNotNull(
                file.parentFile
            )

        require(
            parent.exists() ||
                    parent.mkdirs()
        )

        file.writeText(
            gson.toJson(
                value
            ) + "\n"
        )
    }

    companion object {

        private val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()
    }
}