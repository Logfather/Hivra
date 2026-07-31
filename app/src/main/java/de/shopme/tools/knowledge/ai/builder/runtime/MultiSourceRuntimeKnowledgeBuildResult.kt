package de.shopme.tools.knowledge.ai.builder.runtime

import java.io.File

data class MultiSourceRuntimeKnowledgeBuildResult(
    val offCandidateCount: Int,
    val offNutritionAggregateCount: Int,
    val agribalyseCandidateCount: Int,
    val ciqualCandidateCount: Int,
    val inputCandidateCount: Int,
    val normalizedCandidateCount: Int,
    val mergedCandidateCount: Int,
    val conflictCount: Int,

    val nutritionCandidateCount: Int,
    val environmentalImpactCandidateCount: Int,
    val multiDimensionCandidateCount: Int,

    val nutritionArtifactEntryCount: Int,
    val environmentalImpactArtifactEntryCount: Int,

    val nutritionArtifactFile: File,
    val environmentalImpactArtifactFile: File,

    val blockedHighFanoutKeys: Map<String, Int>,

    val ingredientsCandidateCount: Int,

    val ingredientsArtifactEntryCount: Int,
    val ingredientsArtifactFile: File,

    val allergensCandidateCount: Int,

    val allergenArtifactEntryCount: Int,
    val allergenArtifactFile: File,

    val packagingCandidateCount: Int,

    val packagingArtifactEntryCount: Int,
    val packagingArtifactFile: File,

    val taxonomyCandidateCount: Int,

    val taxonomyArtifactEntryCount: Int,
    val taxonomyArtifactFile: File,

    val processingCandidateCount: Int,

    val processingArtifactEntryCount: Int,
    val processingArtifactFile: File,

    val waterCandidateCount: Int,
    val waterArtifactEntryCount: Int,
    val waterArtifactFile: File,

    val waterStressCandidateCount: Int,
    val waterStressArtifactEntryCount: Int,
    val waterStressArtifactFile: File,

    val biodiversityCandidateCount: Int,
    val biodiversityArtifactEntryCount: Int,
    val biodiversityArtifactFile: File,

    val pollinatorCandidateCount: Int,
    val pollinatorArtifactEntryCount: Int,
    val pollinatorArtifactFile: File,

    val pesticidesCandidateCount: Int,
    val pesticidesArtifactEntryCount: Int,
    val pesticidesArtifactFile: File,

    val productionCandidateCount: Int,
    val productionArtifactEntryCount: Int,
    val productionArtifactFile: File,

    val foodMilesCandidateCount: Int,
    val foodMilesArtifactEntryCount: Int,
    val foodMilesArtifactFile: File,

    val localityCandidateCount: Int,
    val localityArtifactEntryCount: Int,
    val localityArtifactFile: File,

    val nutriScoreCandidateCount: Int,
    val nutriScoreArtifactEntryCount: Int,
    val nutriScoreArtifactFile: File,

    val seasonalityCandidateCount: Int,
    val seasonalityArtifactEntryCount: Int,
    val seasonalityArtifactFile: File,

    val dietCandidateCount: Int,
    val dietArtifactEntryCount: Int,
    val dietArtifactFile: File,

    val fairTradeCandidateCount: Int,
    val fairTradeArtifactEntryCount: Int,
    val fairTradeArtifactFile: File,

    val animalWelfareCandidateCount: Int,
    val animalWelfareArtifactEntryCount: Int,
    val animalWelfareArtifactFile: File,

    val recipeCandidateCount: Int,
    val recipeArtifactEntryCount: Int,
    val recipeArtifactFile: File,

    val ingredientGraphCandidateCount: Int,
    val ingredientGraphArtifactEntryCount: Int,
    val ingredientGraphArtifactFile: File,

    val recipeGraphCandidateCount: Int,
    val recipeGraphArtifactEntryCount: Int,
    val recipeGraphArtifactFile: File,
)