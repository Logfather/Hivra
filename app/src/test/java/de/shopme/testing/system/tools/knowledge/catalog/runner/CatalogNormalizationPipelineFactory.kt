package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.application.CatalogCanonicalizationPlanApplier
import de.shopme.testing.system.tools.knowledge.catalog.runner.CatalogAuditReportWriters
import de.shopme.testing.system.tools.knowledge.catalog.category.CanonicalFoodCategoryRegistry
import de.shopme.testing.system.tools.knowledge.catalog.category.migration.CatalogLegacyCategoryMigrator
import de.shopme.testing.system.tools.knowledge.catalog.duplicate.CatalogDuplicateDetector
import de.shopme.testing.system.tools.knowledge.catalog.duplicate.resolution.CatalogDeterministicDuplicateResolver
import de.shopme.testing.system.tools.knowledge.catalog.duplicate.resolution.diagnostics.CatalogRemainingTypoCandidateDiagnostics
import de.shopme.testing.system.tools.knowledge.catalog.language.CatalogLanguageValidator
import de.shopme.testing.system.tools.knowledge.catalog.nonfood.CatalogNonFoodDetector
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CanonicalFoodKeyNormalizer
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CanonicalFoodNameNormalizer
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CatalogFoodItemNormalizer
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CatalogTokenNormalizer
import de.shopme.testing.system.tools.knowledge.catalog.normalization.GermanFoodPluralNormalizer
import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationPlanner
import de.shopme.testing.system.tools.knowledge.catalog.reader.CatalogFoodItemReader
import de.shopme.testing.system.tools.knowledge.catalog.removal.CatalogUnresolvedSemanticTypoRemover
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogCanonicalizationPlanReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogCategoryReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogDuplicateGroupsReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogLanguageIssuesReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogMisclassifiedTypoReclassificationReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogNonFoodCandidatesReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogNormalizationReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogQualityReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogRemainingTypoDiagnosticsReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogReviewBacklogAnalysisReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogReviewBacklogClassificationReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogReviewBacklogReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogSemanticTypoRemovalReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogTaxonomyGapReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogUpdatedReviewAnalysisReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.review.CatalogReviewBacklogEvaluator
import de.shopme.testing.system.tools.knowledge.catalog.review.analysis.CatalogReviewBacklogAnalyzer
import de.shopme.testing.system.tools.knowledge.catalog.review.analysis.updated.CatalogUpdatedReviewAnalyzer
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewBacklogClassifier
import de.shopme.testing.system.tools.knowledge.catalog.review.reclassification.CatalogMisclassifiedTypoReclassifier
import de.shopme.testing.system.tools.knowledge.catalog.review.reclassification.CatalogTypoReclassificationBacklogIntegrator
import de.shopme.testing.system.tools.knowledge.catalog.category.CatalogCategoryValidator
import de.shopme.testing.system.tools.knowledge.catalog.validation.CatalogQualityValidator
import de.shopme.testing.system.tools.knowledge.catalog.validation.NormalizedCatalogValidator
import de.shopme.testing.system.tools.knowledge.catalog.writer.NormalizedCatalogWriter

object CatalogNormalizationPipelineFactory {

    fun create(
        categoryRegistry:
        CanonicalFoodCategoryRegistry =
            CanonicalFoodCategoryRegistry()
    ): CatalogNormalizationPipeline {
        val keyNormalizer =
            CanonicalFoodKeyNormalizer()

        val auditPipeline =
            CatalogAuditPipeline(
                reader =
                    CatalogFoodItemReader(),

                normalizer =
                    CatalogFoodItemNormalizer(
                        foodNameNormalizer =
                            CanonicalFoodNameNormalizer(),

                        keyNormalizer =
                            keyNormalizer,

                        tokenNormalizer =
                            CatalogTokenNormalizer(),

                        pluralNormalizer =
                            GermanFoodPluralNormalizer()
                    ),

                qualityValidator =
                    CatalogQualityValidator(),

                duplicateDetector =
                    CatalogDuplicateDetector(),

                categoryValidator =
                    CatalogCategoryValidator(),

                languageValidator =
                    CatalogLanguageValidator(),

                nonFoodDetector =
                    CatalogNonFoodDetector(),

                canonicalizationPlanner =
                    CatalogCanonicalizationPlanner(),

                reportWriters =
                    CatalogAuditReportWriters(
                        qualityReportWriter =
                            CatalogQualityReportWriter(),

                        duplicateGroupsReportWriter =
                            CatalogDuplicateGroupsReportWriter(),

                        categoryReportWriter =
                            CatalogCategoryReportWriter(),

                        languageIssuesReportWriter =
                            CatalogLanguageIssuesReportWriter(),

                        nonFoodCandidatesReportWriter =
                            CatalogNonFoodCandidatesReportWriter(),

                        canonicalizationPlanReportWriter =
                            CatalogCanonicalizationPlanReportWriter(),

                        normalizationReportWriter =
                            CatalogNormalizationReportWriter(),

                        taxonomyGapReportWriter =
                            CatalogTaxonomyGapReportWriter()
                    ),

                categoryRegistry =
                    categoryRegistry
            )

        return CatalogNormalizationPipeline(
            auditPipeline =
                auditPipeline,

            planApplier =
                CatalogCanonicalizationPlanApplier(
                    categoryMigrator =
                        CatalogLegacyCategoryMigrator(),

                    categoryRegistry =
                        categoryRegistry,

                    duplicateResolver =
                        CatalogDeterministicDuplicateResolver()
                ),

            validator =
                NormalizedCatalogValidator(
                    keyNormalizer =
                        keyNormalizer
                ),

            categoryRegistry =
                categoryRegistry,

            catalogWriter =
                NormalizedCatalogWriter(),

            reviewBacklogEvaluator =
                CatalogReviewBacklogEvaluator(
                    keyNormalizer =
                        keyNormalizer
                ),

            reviewBacklogReportWriter =
                CatalogReviewBacklogReportWriter(),

            reviewBacklogClassifier =
                CatalogReviewBacklogClassifier(),

            reviewBacklogClassificationReportWriter =
                CatalogReviewBacklogClassificationReportWriter(),

            reviewBacklogAnalyzer =
                CatalogReviewBacklogAnalyzer(),

            reviewBacklogAnalysisReportWriter =
                CatalogReviewBacklogAnalysisReportWriter(),

            updatedReviewAnalyzer =
                CatalogUpdatedReviewAnalyzer(),

            updatedReviewAnalysisReportWriter =
                CatalogUpdatedReviewAnalysisReportWriter(),

            remainingTypoCandidateDiagnostics =
                CatalogRemainingTypoCandidateDiagnostics(),

            remainingTypoDiagnosticsReportWriter =
                CatalogRemainingTypoDiagnosticsReportWriter(),

            misclassifiedTypoReclassifier =
                CatalogMisclassifiedTypoReclassifier(),

            misclassifiedTypoReclassificationReportWriter =
                CatalogMisclassifiedTypoReclassificationReportWriter(),

            typoReclassificationBacklogIntegrator =
                CatalogTypoReclassificationBacklogIntegrator(),

            unresolvedSemanticTypoRemover =
                CatalogUnresolvedSemanticTypoRemover(),

            semanticTypoRemovalReportWriter =
                CatalogSemanticTypoRemovalReportWriter()
        )
    }
}