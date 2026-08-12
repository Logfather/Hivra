package de.shopme.testing.system.tools.knowledge.build

import de.shopme.testing.system.tools.knowledge.catalog.build.CanonicalKnowledgeBuildCatalogSource
import java.io.File

class KnowledgeBuildContextFactory(
    private val projectRoot: File
) {

    fun create(): KnowledgeBuildContext =
        KnowledgeBuildContext(
            canonicalCatalog =
                CanonicalKnowledgeBuildCatalogSource(
                    projectRoot
                ).load()
        )
}