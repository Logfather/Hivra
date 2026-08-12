package de.shopme.tools.knowledge.catalog.canonical.completeness.runner

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.catalog.canonical.completeness.AuditCanonicalFoodCatalogCompleteness

object RunAuditCanonicalFoodCatalogCompleteness {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {

        require(args.isEmpty())

        AuditCanonicalFoodCatalogCompleteness()
            .audit(
                paths =
                    KnowledgeBuildPaths.default()
            )
    }
}