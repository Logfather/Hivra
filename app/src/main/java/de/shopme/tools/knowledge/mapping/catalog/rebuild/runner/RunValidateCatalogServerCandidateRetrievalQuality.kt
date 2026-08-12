package de.shopme.tools.knowledge.mapping.catalog.rebuild.runner

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.mapping.catalog.rebuild.ValidateCatalogServerCandidateRetrievalQuality

object RunValidateCatalogServerCandidateRetrievalQuality {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {

        require(
            args.isEmpty()
        ) {
            "RunValidateCatalogServerCandidateRetrievalQuality " +
                    "does not accept arguments."
        }

        ValidateCatalogServerCandidateRetrievalQuality()
            .validate(
                paths =
                    KnowledgeBuildPaths.default()
            )
    }
}