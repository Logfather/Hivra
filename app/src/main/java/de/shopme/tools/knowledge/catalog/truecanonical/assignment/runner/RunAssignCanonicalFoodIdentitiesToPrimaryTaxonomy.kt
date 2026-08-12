package de.shopme.tools.knowledge.catalog.truecanonical.assignment.runner

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.catalog.truecanonical.assignment.AssignCanonicalFoodIdentitiesToPrimaryTaxonomy

object RunAssignCanonicalFoodIdentitiesToPrimaryTaxonomy {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {

        require(
            args.isEmpty()
        )

        AssignCanonicalFoodIdentitiesToPrimaryTaxonomy()
            .assign(
                paths =
                    KnowledgeBuildPaths.default()
            )
    }
}