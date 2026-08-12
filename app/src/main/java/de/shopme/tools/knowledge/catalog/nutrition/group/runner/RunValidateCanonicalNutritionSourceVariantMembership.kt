package de.shopme.tools.knowledge.mapping.catalog.nutrition.group.runner

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.mapping.catalog.nutrition.group.ValidateCanonicalNutritionSourceVariantMembership

object RunValidateCanonicalNutritionSourceVariantMembership {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {

        require(
            args.isEmpty()
        ) {
            "RunValidateCanonicalNutritionSourceVariantMembership " +
                    "does not accept arguments."
        }

        ValidateCanonicalNutritionSourceVariantMembership()
            .validate(
                paths =
                    KnowledgeBuildPaths.default()
            )
    }
}