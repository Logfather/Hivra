package de.shopme.tools.knowledge.mapping.catalog

data class CatalogServerKnowledgeMappings(
    val version: Int,
    val mappings: List<CatalogServerKnowledgeMapping>
) {

    init {
        require(version > 0) {
            "version must be greater than zero"
        }

        requireNoDuplicateMappings()
        requireMappingsAreDeterministicallyOrdered()
    }


    private fun requireNoDuplicateMappings() {

        val duplicateMappings =
            mappings
                .groupingBy {
                    MappingIdentity(
                        catalogKey =
                            it.catalogKey,
                        sourceArtifact =
                            it.sourceArtifact
                    )
                }
                .eachCount()
                .filterValues {
                    it > 1
                }
                .keys

        require(
            duplicateMappings.isEmpty()
        ) {
            "Duplicate catalog server knowledge mappings: " +
                    duplicateMappings
                        .sortedWith(
                            compareBy<MappingIdentity> {
                                it.catalogKey
                            }
                                .thenBy {
                                    it.sourceArtifact
                                }
                        )
                        .joinToString()
        }

    }

    private data class MappingIdentity(
        val catalogKey: String,
        val sourceArtifact: String
    )

    private fun requireMappingsAreDeterministicallyOrdered() {

        require(
            mappings ==
                    mappings.sortedWith(MAPPING_ORDER)
        ) {
            "mappings must be ordered by catalogKey and serverKey"
        }
    }


    companion object {

        const val CURRENT_VERSION =
            1

        val MAPPING_ORDER:
                Comparator<CatalogServerKnowledgeMapping> =
            compareBy<CatalogServerKnowledgeMapping> {
                it.catalogKey
            }
                .thenBy {
                    it.sourceArtifact
                }
                .thenBy {
                    it.serverKey
                }
    }
}