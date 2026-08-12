package de.shopme.tools.knowledge.catalog.taxonomy.hierarchical

import de.shopme.tools.knowledge.catalog.taxonomy.hierarchical.model.HierarchicalFoodTaxonomyNode
import de.shopme.tools.knowledge.catalog.taxonomy.hierarchical.model.HierarchicalPrimaryFoodTaxonomy

data class HierarchicalFoodTaxonomyValidationResult(
    val departmentCount: Int,
    val nodeCount: Int,
    val leafCount: Int,
    val maximumDepth: Int,
    val duplicateDepartmentIdCount: Int,
    val duplicateSiblingNodeIdCount: Int,
    val emptyDepartmentCount: Int,
    val emptyNodeNameCount: Int,
    val forbiddenDepartmentCount: Int,
    val valid: Boolean
)

class HierarchicalCanonicalGermanFoodTaxonomyValidator {

    fun validate(
        taxonomy: HierarchicalPrimaryFoodTaxonomy
    ): HierarchicalFoodTaxonomyValidationResult {

        require(
            taxonomy.domain.id ==
                    "food"
        )

        require(
            taxonomy.domain.name ==
                    "Food"
        )

        val duplicateDepartmentIds =
            taxonomy.departments
                .groupBy {
                    it.id
                }
                .filterValues {
                    it.size >
                            1
                }

        val emptyDepartments =
            taxonomy.departments
                .filter {
                    it.children.isEmpty()
                }

        val forbiddenDepartments =
            taxonomy.departments
                .filter {
                    it.id in
                            FORBIDDEN_GLOBAL_DEPARTMENT_IDS
                }

        var nodeCount =
            0

        var leafCount =
            0

        var maximumDepth =
            1

        var duplicateSiblingNodeIdCount =
            0

        var emptyNodeNameCount =
            0

        taxonomy.departments
            .forEach { department ->

                val result =
                    validateNodes(
                        nodes =
                            department.children,
                        depth =
                            2
                    )

                nodeCount +=
                    result.nodeCount

                leafCount +=
                    result.leafCount

                maximumDepth =
                    maxOf(
                        maximumDepth,
                        result.maximumDepth
                    )

                duplicateSiblingNodeIdCount +=
                    result.duplicateSiblingNodeIdCount

                emptyNodeNameCount +=
                    result.emptyNodeNameCount
            }

        val valid =
            duplicateDepartmentIds.isEmpty() &&
                    emptyDepartments.isEmpty() &&
                    forbiddenDepartments.isEmpty() &&
                    duplicateSiblingNodeIdCount ==
                    0 &&
                    emptyNodeNameCount ==
                    0

        return HierarchicalFoodTaxonomyValidationResult(
            departmentCount =
                taxonomy.departments.size,

            nodeCount =
                nodeCount,

            leafCount =
                leafCount,

            maximumDepth =
                maximumDepth,

            duplicateDepartmentIdCount =
                duplicateDepartmentIds.size,

            duplicateSiblingNodeIdCount =
                duplicateSiblingNodeIdCount,

            emptyDepartmentCount =
                emptyDepartments.size,

            emptyNodeNameCount =
                emptyNodeNameCount,

            forbiddenDepartmentCount =
                forbiddenDepartments.size,

            valid =
                valid
        )
    }

    private fun validateNodes(
        nodes: List<HierarchicalFoodTaxonomyNode>,
        depth: Int
    ): NodeValidationResult {

        val duplicates =
            nodes
                .groupBy {
                    it.id
                }
                .filterValues {
                    it.size >
                            1
                }

        var nodeCount =
            nodes.size

        var leafCount =
            nodes.count {
                it.children.isEmpty()
            }

        var maximumDepth =
            if (
                nodes.isEmpty()
            ) {
                depth -
                        1
            } else {
                depth
            }

        var duplicateSiblingNodeIdCount =
            duplicates.size

        var emptyNodeNameCount =
            nodes.count {
                it.name.isBlank()
            }

        nodes
            .forEach { node ->

                if (
                    node.children.isNotEmpty()
                ) {

                    val child =
                        validateNodes(
                            nodes =
                                node.children,
                            depth =
                                depth +
                                        1
                        )

                    nodeCount +=
                        child.nodeCount

                    leafCount +=
                        child.leafCount

                    maximumDepth =
                        maxOf(
                            maximumDepth,
                            child.maximumDepth
                        )

                    duplicateSiblingNodeIdCount +=
                        child.duplicateSiblingNodeIdCount

                    emptyNodeNameCount +=
                        child.emptyNodeNameCount
                }
            }

        return NodeValidationResult(
            nodeCount =
                nodeCount,

            leafCount =
                leafCount,

            maximumDepth =
                maximumDepth,

            duplicateSiblingNodeIdCount =
                duplicateSiblingNodeIdCount,

            emptyNodeNameCount =
                emptyNodeNameCount
        )
    }

    private data class NodeValidationResult(
        val nodeCount: Int,
        val leafCount: Int,
        val maximumDepth: Int,
        val duplicateSiblingNodeIdCount: Int,
        val emptyNodeNameCount: Int
    )

    companion object {

        private val FORBIDDEN_GLOBAL_DEPARTMENT_IDS =
            setOf(
                "plant-based-foods",
                "ready-meals"
            )
    }
}