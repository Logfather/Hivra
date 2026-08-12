package de.shopme.tools.knowledge.catalog.truecanonical.assignment.runner

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.catalog.taxonomy.PrimaryCanonicalGermanFoodTaxonomyRegistry
import de.shopme.tools.knowledge.catalog.truecanonical.TrueCanonicalGermanFoodIdentityRegistry
import de.shopme.tools.knowledge.catalog.truecanonical.assignment.CanonicalFoodTaxonomyAssignmentRegistry
import de.shopme.tools.knowledge.catalog.truecanonical.assignment.CanonicalFoodTaxonomyAssignmentValidator

object RunAnalyzeCanonicalFoodTaxonomyAssignments {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {

        require(
            args.isEmpty()
        )

        val paths =
            KnowledgeBuildPaths.default()

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

        val outputFile =
            paths.reportsRoot.resolve(
                "canonical-food-taxonomy-assignment-validation.json"
            )

        val parent =
            requireNotNull(
                outputFile.parentFile
            )

        require(
            parent.exists() ||
                    parent.mkdirs()
        )

        outputFile.writeText(
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()
                .toJson(
                    result
                ) + "\n"
        )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("CANONICAL FOOD TAXONOMY ASSIGNMENT")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("Identities          : ${result.identityCount}")
        println("Assignments         : ${result.assignmentCount}")
        println("Assigned            : ${result.assignedIdentityCount}")
        println("Unassigned          : ${result.unassignedIdentityCount}")
        println("Orphan assignments  : ${result.orphanAssignmentCount}")
        println("Invalid departments : ${result.invalidDepartmentCount}")
        println("Invalid groups      : ${result.invalidGroupCount}")
        println()
        println("Report              : ${outputFile.path}")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println()
    }
}