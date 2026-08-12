package de.shopme.tools.knowledge.compiler.passes

import de.shopme.tools.knowledge.compiler.CompilerContext
import de.shopme.tools.knowledge.compiler.FoodKnowledgeCompilerPass
import de.shopme.tools.knowledge.foods.FoodLookup
import de.shopme.tools.knowledge.taxonomy.FoodTaxonomyResolver

class TaxonomyCompilerPass(

    private val resolver: FoodTaxonomyResolver,

    private val foodLookup: FoodLookup

) : FoodKnowledgeCompilerPass {

    override fun process(
        context: CompilerContext
    ) {

        /*
         * Canonical taxonomyPaths are authoritative and come directly
         * from the Product-Only master via CatalogItem.
         *
         * This pass must never overwrite them.
         */

        if (context.taxonomyPaths.isNotEmpty()) {
            return
        }

        /*
         * Compatibility fallback for non-master CatalogItems.
         *
         * This path exists only for legacy/test callers that do not yet
         * provide canonical taxonomyPaths.
         */

        val taxonomy =
            resolver.resolve(
                context.nutritionReference
                    ?: context.normalizedName
            )

        val fallbackPath =
            foodLookup.taxonomy(
                context.normalizedName
            )
                ?: taxonomy?.let { entry ->
                    listOf(
                        entry.parent,
                        context.normalizedName
                    )
                }
                ?: emptyList()

        if (fallbackPath.isEmpty()) {
            return
        }

        context.taxonomyPaths.add(
            fallbackPath.toList()
        )

        context.taxonomyPath.clear()
        context.taxonomyPath.addAll(
            fallbackPath
        )
    }
}