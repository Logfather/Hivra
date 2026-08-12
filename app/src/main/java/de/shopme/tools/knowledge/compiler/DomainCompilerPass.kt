package de.shopme.tools.knowledge.compiler

import de.shopme.tools.knowledge.compiler.model.FoodDefinitionEntry


interface DomainCompilerPass {

    fun compile(

        foods: List<FoodDefinitionEntry>

    )

}