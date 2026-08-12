package de.shopme.tools.knowledge.compiler

import de.shopme.tools.knowledge.compiler.model.FoodDefinitionEntry

class DomainKnowledgeCompiler(

    private val foods: List<FoodDefinitionEntry>,

    private val passes: List<DomainCompilerPass>

) {

    fun compile() {

        passes.forEach {

            it.compile(

                foods

            )

        }

    }

}