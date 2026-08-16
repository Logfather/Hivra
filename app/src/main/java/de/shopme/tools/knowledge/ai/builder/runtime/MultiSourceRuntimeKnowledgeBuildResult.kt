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

    val multiDimensionCandidateCount: Int,

    val nutritionCandidateCount: Int,
    val nutritionArtifactEntryCount: Int,
    val nutritionArtifactFile: File,

    val environmentalImpactCandidateCount: Int,
    val environmentalImpactArtifactEntryCount: Int,
    val environmentalImpactArtifactFile: File,

    val allergensCandidateCount: Int,
    val allergenArtifactEntryCount: Int,
    val allergenArtifactFile: File,

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

    val pesticidesCandidateCount: Int,
    val pesticidesArtifactEntryCount: Int,
    val pesticidesArtifactFile: File,

    val foodMilesCandidateCount: Int,
    val foodMilesArtifactEntryCount: Int,
    val foodMilesArtifactFile: File,

    val nutriScoreCandidateCount: Int,
    val nutriScoreArtifactEntryCount: Int,
    val nutriScoreArtifactFile: File,

    val dietCandidateCount: Int,
    val dietArtifactEntryCount: Int,
    val dietArtifactFile: File,

    val animalWelfareCandidateCount: Int,
    val animalWelfareArtifactEntryCount: Int,
    val animalWelfareArtifactFile: File,

    val blockedHighFanoutKeys: Map<String, Int>
)