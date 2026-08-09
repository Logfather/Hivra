package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.analysis

import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate
.CanonicalCatalogExpansionCandidate
import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate
.CanonicalCatalogExpansionCandidateGenerationResult
import de.shopme.testing.system.tools.knowledge.catalog.expansion.family
.CanonicalProductFamilyVariantAxis
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic
.CanonicalCatalogExpansionSemanticValidationResult
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic
.CanonicalExpansionCandidateSemanticValidationEntry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic
.CanonicalExpansionSemanticDecision
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic
.CanonicalExpansionSemanticRuleType

class CanonicalSemanticReviewBacklogAnalyzer {

    fun analyze(
        candidates:
        CanonicalCatalogExpansionCandidateGenerationResult,

        semanticValidation:
        CanonicalCatalogExpansionSemanticValidationResult
    ): CanonicalSemanticReviewBacklogAnalysis {
        require(candidates.valid) {
            "Expansion candidate generation must be valid."
        }

        require(semanticValidation.valid) {
            "Semantic validation result must be valid."
        }

        require(
            candidates.sourceBaselineId ==
                    semanticValidation.sourceBaselineId
        )

        require(
            candidates.sourceBaselineCatalogSha256 ==
                    semanticValidation.sourceBaselineCatalogSha256
        )

        require(
            candidates.generatedCandidateCount ==
                    semanticValidation.generatedCandidateCount
        )

        val blockers =
            mutableListOf<String>()

        val candidatesByIndex =
            candidates.candidates
                .associateBy {
                    it.candidateIndex
                }

        require(
            candidatesByIndex.size ==
                    candidates.generatedCandidateCount
        )

        val reviewEntries =
            semanticValidation.entries
                .filter {
                    it.decision ==
                            CanonicalExpansionSemanticDecision
                                .REVIEW_REQUIRED
                }
                .sortedBy {
                    it.candidateIndex
                }

        val reviewRecords =
            reviewEntries.map { entry ->
                val candidate =
                    requireNotNull(
                        candidatesByIndex[
                            entry.candidateIndex
                        ]
                    ) {
                        "No generated candidate exists for semantic entry " +
                                "${entry.candidateIndex}."
                    }

                require(
                    candidate.candidateKey ==
                            entry.candidateKey
                ) {
                    "Candidate key mismatch for index " +
                            entry.candidateIndex
                }

                ReviewRecord(
                    candidate =
                        candidate,

                    validationEntry =
                        entry,

                    missingPolicyAxes =
                        missingPolicyAxes(entry)
                )
            }

        val reviewCandidateCount =
            reviewRecords.size

        if (
            reviewCandidateCount !=
            semanticValidation.reviewRequiredCandidateCount
        ) {
            blockers +=
                "Review backlog count differs from semantic validation."
        }

        val categoryAnalyses =
            createCategoryAnalyses(
                allCandidates =
                    candidates.candidates,

                reviewRecords =
                    reviewRecords,

                completeReviewCount =
                    reviewCandidateCount
            )

        val familyAnalyses =
            createFamilyAnalyses(
                allCandidates =
                    candidates.candidates,

                reviewRecords =
                    reviewRecords,

                completeReviewCount =
                    reviewCandidateCount
            )

        val axisAnalyses =
            createAxisAnalyses(
                reviewRecords =
                    reviewRecords,

                completeReviewCount =
                    reviewCandidateCount
            )

        val valueAnalyses =
            createValueAnalyses(
                reviewRecords =
                    reviewRecords,

                completeReviewCount =
                    reviewCandidateCount
            )

        val missingPolicyGaps =
            createMissingPolicyGaps(
                reviewRecords =
                    reviewRecords,

                completeReviewCount =
                    reviewCandidateCount
            )

        val candidatesWithMissingPolicyCount =
            reviewRecords.count {
                it.missingPolicyAxes.isNotEmpty()
            }

        val reviewFindingCountsByRuleType =
            reviewEntries
                .flatMap {
                    it.findings
                }
                .groupingBy {
                    it.ruleType
                }
                .eachCount()
                .toList()
                .sortedBy {
                    it.first.name
                }
                .associate {
                    it
                }

        val highestPriorityGap =
            missingPolicyGaps.firstOrNull()

        val sortedBlockers =
            blockers
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()
                .sorted()

        val observations =
            buildObservations(
                reviewCandidateCount =
                    reviewCandidateCount,

                totalCandidateCount =
                    candidates.generatedCandidateCount,

                candidatesWithMissingPolicyCount =
                    candidatesWithMissingPolicyCount,

                categoryAnalyses =
                    categoryAnalyses,

                familyAnalyses =
                    familyAnalyses,

                axisAnalyses =
                    axisAnalyses,

                missingPolicyGaps =
                    missingPolicyGaps
            )

        val completeReviewCoverage =
            categoryAnalyses.sumOf {
                it.reviewRequiredCandidateCount
            } ==
                    reviewCandidateCount

        return CanonicalSemanticReviewBacklogAnalysis(
            version =
                CanonicalSemanticReviewBacklogAnalysis
                    .CURRENT_VERSION,

            sourceBaselineId =
                candidates.sourceBaselineId,

            sourceBaselineCatalogSha256 =
                candidates.sourceBaselineCatalogSha256,

            generatedCandidateCount =
                candidates.generatedCandidateCount,

            evaluatedCandidateCount =
                semanticValidation.evaluatedCandidateCount,

            acceptedCandidateCount =
                semanticValidation.acceptedCandidateCount,

            rejectedCandidateCount =
                semanticValidation.rejectedCandidateCount,

            reviewRequiredCandidateCount =
                reviewCandidateCount,

            reviewRequiredShare =
                share(
                    numerator =
                        reviewCandidateCount,

                    denominator =
                        candidates.generatedCandidateCount
                ),

            reviewedCategoryCount =
                categoryAnalyses.size,

            reviewedFamilyCount =
                familyAnalyses.size,

            reviewedAxisCount =
                axisAnalyses.size,

            reviewedDistinctValueCount =
                valueAnalyses.size,

            candidatesWithMissingFamilyAxisPolicyCount =
                candidatesWithMissingPolicyCount,

            candidatesWithMissingFamilyAxisPolicyShare =
                share(
                    numerator =
                        candidatesWithMissingPolicyCount,

                    denominator =
                        reviewCandidateCount
                ),

            missingFamilyAxisPolicyGapCount =
                missingPolicyGaps.size,

            reviewFindingCountsByRuleType =
                reviewFindingCountsByRuleType,

            categories =
                categoryAnalyses,

            families =
                familyAnalyses,

            axes =
                axisAnalyses,

            values =
                valueAnalyses,

            missingFamilyAxisPolicyGaps =
                missingPolicyGaps,

            highestPriorityImplementationKey =
                highestPriorityGap
                    ?.implementationKey,

            highestPriorityRecommendation =
                highestPriorityGap
                    ?.recommendation,

            highestPriorityAffectedCandidateCount =
                highestPriorityGap
                    ?.affectedCandidateCount
                    ?: 0,

            highestPriorityAffectedReviewShare =
                highestPriorityGap
                    ?.affectedReviewShare
                    ?: 0.0,

            implementationReady =
                sortedBlockers.isEmpty() &&
                        missingPolicyGaps.isNotEmpty(),

            implementationBlockers =
                sortedBlockers,

            observations =
                observations,

            completeReviewCoverage =
                completeReviewCoverage,

            valid =
                sortedBlockers.isEmpty() &&
                        completeReviewCoverage
        )
    }

    private fun createCategoryAnalyses(
        allCandidates:
        List<CanonicalCatalogExpansionCandidate>,

        reviewRecords:
        List<ReviewRecord>,

        completeReviewCount: Int
    ): List<CanonicalSemanticReviewCategoryAnalysis> {
        val generatedCounts =
            allCandidates
                .groupingBy {
                    it.category
                }
                .eachCount()

        return reviewRecords
            .groupBy {
                it.candidate.category
            }
            .map { (category, records) ->
                val generatedCount =
                    requireNotNull(
                        generatedCounts[category]
                    )

                UnrankedCategoryAnalysis(
                    category =
                        category,

                    generatedCandidateCount =
                        generatedCount,

                    reviewRequiredCandidateCount =
                        records.size,

                    reviewShareWithinCategory =
                        share(
                            numerator =
                                records.size,

                            denominator =
                                generatedCount
                        ),

                    shareOfCompleteReviewBacklog =
                        share(
                            numerator =
                                records.size,

                            denominator =
                                completeReviewCount
                        ),

                    affectedFamilyCount =
                        records
                            .map {
                                it.candidate.familyKey
                            }
                            .distinct()
                            .size,

                    affectedAxisCount =
                        records
                            .flatMap {
                                it.candidate.variantValues
                            }
                            .map {
                                it.axis
                            }
                            .distinct()
                            .size,

                    missingFamilyAxisPolicyCount =
                        records
                            .flatMap { record ->
                                record.missingPolicyAxes
                                    .map { axis ->
                                        record.candidate.familyKey to
                                                axis
                                    }
                            }
                            .distinct()
                            .size
                )
            }
            .sortedWith(
                compareByDescending<
                        UnrankedCategoryAnalysis
                        > {
                    it.reviewRequiredCandidateCount
                }.thenBy {
                    it.category
                }
            )
            .mapIndexed { index, analysis ->
                CanonicalSemanticReviewCategoryAnalysis(
                    rank =
                        index + 1,

                    category =
                        analysis.category,

                    generatedCandidateCount =
                        analysis.generatedCandidateCount,

                    reviewRequiredCandidateCount =
                        analysis.reviewRequiredCandidateCount,

                    reviewShareWithinCategory =
                        analysis.reviewShareWithinCategory,

                    shareOfCompleteReviewBacklog =
                        analysis.shareOfCompleteReviewBacklog,

                    affectedFamilyCount =
                        analysis.affectedFamilyCount,

                    affectedAxisCount =
                        analysis.affectedAxisCount,

                    missingFamilyAxisPolicyCount =
                        analysis.missingFamilyAxisPolicyCount
                )
            }
    }

    private fun createFamilyAnalyses(
        allCandidates:
        List<CanonicalCatalogExpansionCandidate>,

        reviewRecords:
        List<ReviewRecord>,

        completeReviewCount: Int
    ): List<CanonicalSemanticReviewFamilyAnalysis> {
        val generatedCounts =
            allCandidates
                .groupingBy {
                    familyIdentity(
                        category =
                            it.category,

                        familyKey =
                            it.familyKey
                    )
                }
                .eachCount()

        return reviewRecords
            .groupBy {
                familyIdentity(
                    category =
                        it.candidate.category,

                    familyKey =
                        it.candidate.familyKey
                )
            }
            .map { (_, records) ->
                val first =
                    records.first().candidate

                val generatedCount =
                    requireNotNull(
                        generatedCounts[
                            familyIdentity(
                                category =
                                    first.category,

                                familyKey =
                                    first.familyKey
                            )
                        ]
                    )

                val missingAxisCount =
                    records
                        .flatMap {
                            it.missingPolicyAxes
                        }
                        .distinct()
                        .size

                UnrankedFamilyAnalysis(
                    category =
                        first.category,

                    familyKey =
                        first.familyKey,

                    familyDisplayName =
                        first.familyDisplayName,

                    generatedCandidateCount =
                        generatedCount,

                    reviewRequiredCandidateCount =
                        records.size,

                    reviewShareWithinFamily =
                        share(
                            numerator =
                                records.size,

                            denominator =
                                generatedCount
                        ),

                    shareOfCompleteReviewBacklog =
                        share(
                            numerator =
                                records.size,

                            denominator =
                                completeReviewCount
                        ),

                    affectedAxisCount =
                        records
                            .flatMap {
                                it.candidate.variantValues
                            }
                            .map {
                                it.axis
                            }
                            .distinct()
                            .size,

                    distinctAffectedValueCount =
                        records
                            .flatMap {
                                it.candidate.variantValues
                            }
                            .map {
                                it.axis to
                                        it.valueKey
                            }
                            .distinct()
                            .size,

                    missingAxisPolicyCount =
                        missingAxisCount
                )
            }
            .sortedWith(
                compareByDescending<
                        UnrankedFamilyAnalysis
                        > {
                    it.reviewRequiredCandidateCount
                }.thenBy {
                    it.category
                }.thenBy {
                    it.familyKey
                }
            )
            .mapIndexed { index, analysis ->
                CanonicalSemanticReviewFamilyAnalysis(
                    rank =
                        index + 1,

                    category =
                        analysis.category,

                    familyKey =
                        analysis.familyKey,

                    familyDisplayName =
                        analysis.familyDisplayName,

                    generatedCandidateCount =
                        analysis.generatedCandidateCount,

                    reviewRequiredCandidateCount =
                        analysis.reviewRequiredCandidateCount,

                    reviewShareWithinFamily =
                        analysis.reviewShareWithinFamily,

                    shareOfCompleteReviewBacklog =
                        analysis.shareOfCompleteReviewBacklog,

                    affectedAxisCount =
                        analysis.affectedAxisCount,

                    distinctAffectedValueCount =
                        analysis.distinctAffectedValueCount,

                    missingAxisPolicyCount =
                        analysis.missingAxisPolicyCount,

                    priority =
                        priorityFor(
                            affectedCandidateCount =
                                analysis.reviewRequiredCandidateCount,

                            completeReviewCount =
                                completeReviewCount
                        ),

                    recommendation =
                        if (
                            analysis.missingAxisPolicyCount > 0
                        ) {
                            CanonicalSemanticBacklogRecommendation
                                .IMPLEMENT_FAMILY_AXIS_VALUE_POLICY
                        } else {
                            CanonicalSemanticBacklogRecommendation
                                .IMPLEMENT_FAMILY_SPECIFIC_SEMANTIC_RULE
                        }
                )
            }
    }

    private fun createAxisAnalyses(
        reviewRecords:
        List<ReviewRecord>,

        completeReviewCount: Int
    ): List<CanonicalSemanticReviewAxisAnalysis> {
        val recordsByAxis =
            mutableMapOf<
                    CanonicalProductFamilyVariantAxis,
                    MutableList<ReviewRecord>
                    >()

        reviewRecords.forEach { record ->
            record.candidate.variantValues
                .map {
                    it.axis
                }
                .distinct()
                .forEach { axis ->
                    recordsByAxis
                        .getOrPut(axis) {
                            mutableListOf()
                        }
                        .add(record)
                }
        }

        return recordsByAxis
            .map { (axis, records) ->
                val uniqueRecords =
                    records.distinctBy {
                        it.candidate.candidateIndex
                    }

                val missingPolicyCount =
                    uniqueRecords.count {
                        axis in it.missingPolicyAxes
                    }

                UnrankedAxisAnalysis(
                    axis =
                        axis,

                    affectedCandidateCount =
                        uniqueRecords.size,

                    shareOfCompleteReviewBacklog =
                        share(
                            numerator =
                                uniqueRecords.size,

                            denominator =
                                completeReviewCount
                        ),

                    affectedCategoryCount =
                        uniqueRecords
                            .map {
                                it.candidate.category
                            }
                            .distinct()
                            .size,

                    affectedFamilyCount =
                        uniqueRecords
                            .map {
                                it.candidate.category to
                                        it.candidate.familyKey
                            }
                            .distinct()
                            .size,

                    distinctValueCount =
                        uniqueRecords
                            .flatMap {
                                it.candidate.variantValues
                            }
                            .filter {
                                it.axis == axis
                            }
                            .map {
                                it.valueKey
                            }
                            .distinct()
                            .size,

                    missingFamilyPolicyCandidateCount =
                        missingPolicyCount,

                    missingFamilyPolicyShare =
                        share(
                            numerator =
                                missingPolicyCount,

                            denominator =
                                uniqueRecords.size
                        )
                )
            }
            .sortedWith(
                compareByDescending<
                        UnrankedAxisAnalysis
                        > {
                    it.affectedCandidateCount
                }.thenBy {
                    it.axis.name
                }
            )
            .mapIndexed { index, analysis ->
                CanonicalSemanticReviewAxisAnalysis(
                    rank =
                        index + 1,

                    axis =
                        analysis.axis,

                    affectedCandidateCount =
                        analysis.affectedCandidateCount,

                    shareOfCompleteReviewBacklog =
                        analysis.shareOfCompleteReviewBacklog,

                    affectedCategoryCount =
                        analysis.affectedCategoryCount,

                    affectedFamilyCount =
                        analysis.affectedFamilyCount,

                    distinctValueCount =
                        analysis.distinctValueCount,

                    missingFamilyPolicyCandidateCount =
                        analysis
                            .missingFamilyPolicyCandidateCount,

                    missingFamilyPolicyShare =
                        analysis.missingFamilyPolicyShare
                )
            }
    }

    private fun createValueAnalyses(
        reviewRecords:
        List<ReviewRecord>,

        completeReviewCount: Int
    ): List<CanonicalSemanticReviewValueAnalysis> {
        val recordsByValue =
            mutableMapOf<
                    Pair<
                            CanonicalProductFamilyVariantAxis,
                            String
                            >,
                    MutableList<ValueRecord>
                    >()

        reviewRecords.forEach { record ->
            record.candidate.variantValues
                .forEach { value ->
                    recordsByValue
                        .getOrPut(
                            value.axis to
                                    value.valueKey
                        ) {
                            mutableListOf()
                        }
                        .add(
                            ValueRecord(
                                reviewRecord =
                                    record,

                                displayName =
                                    value.displayName
                            )
                        )
                }
        }

        return recordsByValue
            .map { (identity, records) ->
                val uniqueRecords =
                    records.distinctBy {
                        it.reviewRecord
                            .candidate
                            .candidateIndex
                    }

                val axis =
                    identity.first

                UnrankedValueAnalysis(
                    axis =
                        axis,

                    valueKey =
                        identity.second,

                    displayName =
                        uniqueRecords
                            .first()
                            .displayName,

                    affectedCandidateCount =
                        uniqueRecords.size,

                    shareOfCompleteReviewBacklog =
                        share(
                            numerator =
                                uniqueRecords.size,

                            denominator =
                                completeReviewCount
                        ),

                    affectedCategoryCount =
                        uniqueRecords
                            .map {
                                it.reviewRecord
                                    .candidate
                                    .category
                            }
                            .distinct()
                            .size,

                    affectedFamilyCount =
                        uniqueRecords
                            .map {
                                it.reviewRecord
                                    .candidate
                                    .category to
                                        it.reviewRecord
                                            .candidate
                                            .familyKey
                            }
                            .distinct()
                            .size,

                    missingFamilyPolicyCandidateCount =
                        uniqueRecords.count {
                            axis in
                                    it.reviewRecord
                                        .missingPolicyAxes
                        }
                )
            }
            .sortedWith(
                compareByDescending<
                        UnrankedValueAnalysis
                        > {
                    it.affectedCandidateCount
                }.thenBy {
                    it.axis.name
                }.thenBy {
                    it.valueKey
                }
            )
            .mapIndexed { index, analysis ->
                CanonicalSemanticReviewValueAnalysis(
                    rank =
                        index + 1,

                    axis =
                        analysis.axis,

                    valueKey =
                        analysis.valueKey,

                    displayName =
                        analysis.displayName,

                    affectedCandidateCount =
                        analysis.affectedCandidateCount,

                    shareOfCompleteReviewBacklog =
                        analysis.shareOfCompleteReviewBacklog,

                    affectedCategoryCount =
                        analysis.affectedCategoryCount,

                    affectedFamilyCount =
                        analysis.affectedFamilyCount,

                    missingFamilyPolicyCandidateCount =
                        analysis
                            .missingFamilyPolicyCandidateCount
                )
            }
    }

    private fun createMissingPolicyGaps(
        reviewRecords:
        List<ReviewRecord>,

        completeReviewCount: Int
    ): List<CanonicalMissingFamilyAxisPolicyGap> {
        val recordsByGap =
            mutableMapOf<
                    FamilyAxisIdentity,
                    MutableList<ReviewRecord>
                    >()

        reviewRecords.forEach { record ->
            record.missingPolicyAxes
                .forEach { axis ->
                    val identity =
                        FamilyAxisIdentity(
                            category =
                                record.candidate.category,

                            familyKey =
                                record.candidate.familyKey,

                            familyDisplayName =
                                record.candidate.familyDisplayName,

                            axis =
                                axis
                        )

                    recordsByGap
                        .getOrPut(identity) {
                            mutableListOf()
                        }
                        .add(record)
                }
        }

        return recordsByGap
            .map { (identity, records) ->
                val uniqueRecords =
                    records.distinctBy {
                        it.candidate.candidateIndex
                    }

                val valueCounts =
                    uniqueRecords
                        .flatMap {
                            it.candidate.variantValues
                        }
                        .filter {
                            it.axis ==
                                    identity.axis
                        }
                        .groupingBy {
                            it.valueKey
                        }
                        .eachCount()
                        .toSortedMap()

                UnrankedPolicyGap(
                    category =
                        identity.category,

                    familyKey =
                        identity.familyKey,

                    familyDisplayName =
                        identity.familyDisplayName,

                    axis =
                        identity.axis,

                    affectedCandidateCount =
                        uniqueRecords.size,

                    affectedReviewShare =
                        share(
                            numerator =
                                uniqueRecords.size,

                            denominator =
                                completeReviewCount
                        ),

                    distinctValueCount =
                        valueCounts.size,

                    affectedValueCounts =
                        valueCounts
                )
            }
            .sortedWith(
                compareByDescending<
                        UnrankedPolicyGap
                        > {
                    it.affectedCandidateCount
                }.thenBy {
                    it.category
                }.thenBy {
                    it.familyKey
                }.thenBy {
                    it.axis.name
                }
            )
            .mapIndexed { index, gap ->
                CanonicalMissingFamilyAxisPolicyGap(
                    rank =
                        index + 1,

                    category =
                        gap.category,

                    familyKey =
                        gap.familyKey,

                    familyDisplayName =
                        gap.familyDisplayName,

                    axis =
                        gap.axis,

                    affectedCandidateCount =
                        gap.affectedCandidateCount,

                    affectedReviewShare =
                        gap.affectedReviewShare,

                    distinctValueCount =
                        gap.distinctValueCount,

                    affectedValueCounts =
                        gap.affectedValueCounts,

                    priority =
                        priorityFor(
                            affectedCandidateCount =
                                gap.affectedCandidateCount,

                            completeReviewCount =
                                completeReviewCount
                        ),

                    recommendation =
                        CanonicalSemanticBacklogRecommendation
                            .IMPLEMENT_FAMILY_AXIS_VALUE_POLICY,

                    implementationKey =
                        buildImplementationKey(
                            familyKey =
                                gap.familyKey,

                            axis =
                                gap.axis
                        ),

                    rationale =
                        "Define explicit allowed semantic values for family " +
                                "'${gap.familyKey}' and axis '${gap.axis}'. " +
                                "This policy directly addresses " +
                                "${gap.affectedCandidateCount} review candidates."
                )
            }
    }

    private fun missingPolicyAxes(
        entry:
        CanonicalExpansionCandidateSemanticValidationEntry
    ): Set<CanonicalProductFamilyVariantAxis> {
        val missingPolicyFindingCount =
            entry.findings.count {
                it.ruleType ==
                        CanonicalExpansionSemanticRuleType
                            .FAMILY_SPECIFIC_REVIEW &&
                        it.ruleKey ==
                        MISSING_POLICY_RULE_KEY
            }

        if (missingPolicyFindingCount == 0) {
            return emptySet()
        }

        /*
         * Der aktuelle Finding enthält die Achse nur im Message-Text.
         *
         * Da pro unbekannter Kandidatenachse genau ein Finding erzeugt wird,
         * wird die Achse deterministisch über den Kandidatenkontext
         * rekonstruiert: Eine Achse besitzt keine explizite Policy, wenn der
         * Validator für sie ein missing-family-axis-policy-Finding erzeugt.
         *
         * Für eine exakte, positionsunabhängige Zuordnung wird der Achsenname
         * aus der Finding-Message gelesen.
         */
        return entry.findings
            .filter {
                it.ruleType ==
                        CanonicalExpansionSemanticRuleType
                            .FAMILY_SPECIFIC_REVIEW &&
                        it.ruleKey ==
                        MISSING_POLICY_RULE_KEY
            }
            .mapNotNull { finding ->
                CanonicalProductFamilyVariantAxis.entries
                    .firstOrNull { axis ->
                        finding.message.contains(
                            "'${axis.name}'"
                        )
                    }
            }
            .toSet()
    }

    private fun buildObservations(
        reviewCandidateCount: Int,
        totalCandidateCount: Int,
        candidatesWithMissingPolicyCount: Int,

        categoryAnalyses:
        List<CanonicalSemanticReviewCategoryAnalysis>,

        familyAnalyses:
        List<CanonicalSemanticReviewFamilyAnalysis>,

        axisAnalyses:
        List<CanonicalSemanticReviewAxisAnalysis>,

        missingPolicyGaps:
        List<CanonicalMissingFamilyAxisPolicyGap>
    ): List<String> =
        buildList {
            add(
                "$reviewCandidateCount of $totalCandidateCount candidates " +
                        "require semantic review."
            )

            add(
                "$candidatesWithMissingPolicyCount review candidates contain " +
                        "at least one missing family-axis policy."
            )

            categoryAnalyses.firstOrNull()
                ?.let { category ->
                    add(
                        "Category '${category.category}' has the largest " +
                                "review backlog with " +
                                "${category.reviewRequiredCandidateCount} " +
                                "candidates."
                    )
                }

            familyAnalyses.firstOrNull()
                ?.let { family ->
                    add(
                        "Family '${family.familyKey}' has the largest family " +
                                "review backlog with " +
                                "${family.reviewRequiredCandidateCount} " +
                                "candidates."
                    )
                }

            axisAnalyses.firstOrNull()
                ?.let { axis ->
                    add(
                        "Axis '${axis.axis}' affects the largest number of " +
                                "review candidates: " +
                                "${axis.affectedCandidateCount}."
                    )
                }

            missingPolicyGaps.firstOrNull()
                ?.let { gap ->
                    add(
                        "The highest-priority implementation is " +
                                "'${gap.implementationKey}', affecting " +
                                "${gap.affectedCandidateCount} review candidates."
                    )
                }

            add(
                "${missingPolicyGaps.size} distinct family-axis policy gaps " +
                        "were identified."
            )
        }
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()

    private fun priorityFor(
        affectedCandidateCount: Int,
        completeReviewCount: Int
    ): CanonicalSemanticPolicyPriority {
        val affectedShare =
            share(
                numerator =
                    affectedCandidateCount,

                denominator =
                    completeReviewCount
            )

        return when {
            affectedCandidateCount >= 100 ||
                    affectedShare >= 0.02 ->
                CanonicalSemanticPolicyPriority.CRITICAL

            affectedCandidateCount >= 50 ||
                    affectedShare >= 0.01 ->
                CanonicalSemanticPolicyPriority.HIGH

            affectedCandidateCount >= 20 ||
                    affectedShare >= 0.005 ->
                CanonicalSemanticPolicyPriority.MEDIUM

            else ->
                CanonicalSemanticPolicyPriority.LOW
        }
    }

    private fun buildImplementationKey(
        familyKey: String,
        axis: CanonicalProductFamilyVariantAxis
    ): String =
        "implement-$familyKey-" +
                axis.name
                    .lowercase()
                    .replace('_', '-') +
                "-policy"

    private fun familyIdentity(
        category: String,
        familyKey: String
    ): String =
        "$category::$familyKey"

    private fun share(
        numerator: Int,
        denominator: Int
    ): Double =
        if (denominator == 0) {
            0.0
        } else {
            numerator.toDouble() /
                    denominator.toDouble()
        }

    private data class ReviewRecord(
        val candidate:
        CanonicalCatalogExpansionCandidate,

        val validationEntry:
        CanonicalExpansionCandidateSemanticValidationEntry,

        val missingPolicyAxes:
        Set<CanonicalProductFamilyVariantAxis>
    )

    private data class ValueRecord(
        val reviewRecord: ReviewRecord,
        val displayName: String
    )

    private data class FamilyAxisIdentity(
        val category: String,
        val familyKey: String,
        val familyDisplayName: String,
        val axis: CanonicalProductFamilyVariantAxis
    )

    private data class UnrankedCategoryAnalysis(
        val category: String,
        val generatedCandidateCount: Int,
        val reviewRequiredCandidateCount: Int,
        val reviewShareWithinCategory: Double,
        val shareOfCompleteReviewBacklog: Double,
        val affectedFamilyCount: Int,
        val affectedAxisCount: Int,
        val missingFamilyAxisPolicyCount: Int
    )

    private data class UnrankedFamilyAnalysis(
        val category: String,
        val familyKey: String,
        val familyDisplayName: String,
        val generatedCandidateCount: Int,
        val reviewRequiredCandidateCount: Int,
        val reviewShareWithinFamily: Double,
        val shareOfCompleteReviewBacklog: Double,
        val affectedAxisCount: Int,
        val distinctAffectedValueCount: Int,
        val missingAxisPolicyCount: Int
    )

    private data class UnrankedAxisAnalysis(
        val axis: CanonicalProductFamilyVariantAxis,
        val affectedCandidateCount: Int,
        val shareOfCompleteReviewBacklog: Double,
        val affectedCategoryCount: Int,
        val affectedFamilyCount: Int,
        val distinctValueCount: Int,
        val missingFamilyPolicyCandidateCount: Int,
        val missingFamilyPolicyShare: Double
    )

    private data class UnrankedValueAnalysis(
        val axis: CanonicalProductFamilyVariantAxis,
        val valueKey: String,
        val displayName: String,
        val affectedCandidateCount: Int,
        val shareOfCompleteReviewBacklog: Double,
        val affectedCategoryCount: Int,
        val affectedFamilyCount: Int,
        val missingFamilyPolicyCandidateCount: Int
    )

    private data class UnrankedPolicyGap(
        val category: String,
        val familyKey: String,
        val familyDisplayName: String,
        val axis: CanonicalProductFamilyVariantAxis,
        val affectedCandidateCount: Int,
        val affectedReviewShare: Double,
        val distinctValueCount: Int,
        val affectedValueCounts: Map<String, Int>
    )

    private companion object {
        const val MISSING_POLICY_RULE_KEY =
            "missing-family-axis-policy"
    }
}