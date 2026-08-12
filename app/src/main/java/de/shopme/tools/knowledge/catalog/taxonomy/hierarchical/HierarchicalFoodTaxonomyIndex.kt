package de.shopme.tools.knowledge.catalog.taxonomy.hierarchical

import de.shopme.tools.knowledge.catalog.taxonomy.hierarchical.model.HierarchicalFoodTaxonomyNode
import de.shopme.tools.knowledge.catalog.taxonomy.hierarchical.model.HierarchicalPrimaryFoodTaxonomy

data class HierarchicalFoodTaxonomyPath(
    val ids: List<String>,
    val names: List<String>
)

class HierarchicalFoodTaxonomyIndex(
    taxonomy: HierarchicalPrimaryFoodTaxonomy
) {

    private val pathsByQualifiedId:
            Map<String, HierarchicalFoodTaxonomyPath>

    init {

        val collected =
            linkedMapOf<String, HierarchicalFoodTaxonomyPath>()

        taxonomy.departments
            .forEach { department ->

                val departmentIds =
                    listOf(
                        department.id
                    )

                val departmentNames =
                    listOf(
                        department.name
                    )

                department.children
                    .forEach { child ->

                        collect(
                            node =
                                child,

                            parentIds =
                                departmentIds,

                            parentNames =
                                departmentNames,

                            output =
                                collected
                        )
                    }
            }

        pathsByQualifiedId =
            collected.toMap()
    }

    fun containsPath(
        ids: List<String>
    ): Boolean {

        if (
            ids.size <
            2
        ) {
            return false
        }

        return pathsByQualifiedId[
            ids.joinToString(
                separator =
                    "/"
            )
        ] !=
                null
    }

    fun path(
        ids: List<String>
    ): HierarchicalFoodTaxonomyPath? =
        pathsByQualifiedId[
            ids.joinToString(
                separator =
                    "/"
            )
        ]

    fun allPaths():
            Collection<HierarchicalFoodTaxonomyPath> =
        pathsByQualifiedId.values

    private fun collect(
        node: HierarchicalFoodTaxonomyNode,
        parentIds: List<String>,
        parentNames: List<String>,
        output:
        MutableMap<String, HierarchicalFoodTaxonomyPath>
    ) {

        val ids =
            parentIds +
                    node.id

        val names =
            parentNames +
                    node.name

        val qualifiedId =
            ids.joinToString(
                separator =
                    "/"
            )

        check(
            qualifiedId !in
                    output
        ) {
            "Duplicate taxonomy path: $qualifiedId"
        }

        output[
            qualifiedId
        ] =
            HierarchicalFoodTaxonomyPath(
                ids =
                    ids,
                names =
                    names
            )

        node.children
            .forEach { child ->

                collect(
                    node =
                        child,
                    parentIds =
                        ids,
                    parentNames =
                        names,
                    output =
                        output
                )
            }
    }
}