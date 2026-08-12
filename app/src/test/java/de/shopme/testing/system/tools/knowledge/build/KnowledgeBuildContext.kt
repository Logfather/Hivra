package de.shopme.testing.system.tools.knowledge.build

import com.google.gson.JsonObject
import de.shopme.testing.system.tools.knowledge.catalog.build.CanonicalKnowledgeBuildCatalog

data class KnowledgeBuildContext(
    val canonicalCatalog: CanonicalKnowledgeBuildCatalog
) {

    val catalogEntries: List<JsonObject>
        get() =
            canonicalCatalog.entries

    val catalogEntryCount: Int
        get() =
            canonicalCatalog.entries.size

    val catalogSha256: String
        get() =
            canonicalCatalog.sha256
}