package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogCanonicalizationPlanReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogCategoryReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogDuplicateGroupsReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogLanguageIssuesReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogNonFoodCandidatesReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogNormalizationReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogQualityReportWriter
import de.shopme.testing.system.tools.knowledge.catalog.report.CatalogTaxonomyGapReportWriter

data class CatalogAuditReportWriters(
    val qualityReportWriter: CatalogQualityReportWriter,
    val duplicateGroupsReportWriter: CatalogDuplicateGroupsReportWriter,
    val categoryReportWriter: CatalogCategoryReportWriter,
    val languageIssuesReportWriter: CatalogLanguageIssuesReportWriter,
    val nonFoodCandidatesReportWriter: CatalogNonFoodCandidatesReportWriter,
    val canonicalizationPlanReportWriter:
    CatalogCanonicalizationPlanReportWriter,
    val normalizationReportWriter: CatalogNormalizationReportWriter,
    val taxonomyGapReportWriter: CatalogTaxonomyGapReportWriter
)