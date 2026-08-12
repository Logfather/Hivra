package de.shopme.tools.knowledge.compiler.model

data class FoodDefinitionEntry(

    val reference: String,

    val displayName: String,

    val aliases: List<String>,

    val category: String?,

    val tags: List<String>,

    val production: List<String>,

    val dimensions: FoodDimensions?,

    val status: FoodKnowledgeStatus = FoodKnowledgeStatus.VERIFIED

)