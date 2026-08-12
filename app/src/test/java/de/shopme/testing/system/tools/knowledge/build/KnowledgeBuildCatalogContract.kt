package de.shopme.testing.system.tools.knowledge.build

data class KnowledgeBuildCatalogContract(
    val catalogEntryCount: Int,
    val catalogSha256: String
) {

    companion object {

        fun from(
            context: KnowledgeBuildContext
        ) =
            KnowledgeBuildCatalogContract(
                catalogEntryCount =
                    context.catalogEntryCount,
                catalogSha256 =
                    context.catalogSha256
            )
    }
}