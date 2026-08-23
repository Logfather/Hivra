package de.shopme.tools.knowledge.him.canonical.family

import java.io.File

data class HimCanonicalFamilyPaths(
    val projectRoot: File,
) {

    val productOnlyMaster: File =
        projectRoot.resolve(PRODUCT_ONLY_MASTER_PATH)

    val masterDirectory: File =
        projectRoot.resolve(CANONICAL_FAMILY_MASTER_DIRECTORY)

    val entityIdRegistry: File =
        masterDirectory.resolve(ENTITY_ID_REGISTRY_FILE_NAME)

    val familyAuthority: File =
        masterDirectory.resolve(FAMILY_AUTHORITY_FILE_NAME)

    companion object {
        const val PRODUCT_ONLY_MASTER_PATH =
            "data/knowledge/catalog/master/product-only/" +
                    "canonical-food-catalog.product-only.master.json"

        const val CANONICAL_FAMILY_MASTER_DIRECTORY =
            "data/knowledge/him/canonical-family/master"

        const val ENTITY_ID_REGISTRY_FILE_NAME =
            "him-entity-id-registry.v1.json"

        const val FAMILY_AUTHORITY_FILE_NAME =
            "canonical-family-authority.v1.json"
    }
}
