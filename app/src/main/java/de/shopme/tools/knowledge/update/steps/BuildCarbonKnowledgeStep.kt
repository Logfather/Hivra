package de.shopme.tools.knowledge.update.steps

import de.shopme.tools.knowledge.agribalyse.mapper.AgribalyseReferenceMapper
import de.shopme.tools.knowledge.agribalyse.report.AgribalyseMappingReport
import de.shopme.tools.knowledge.agribalyse.report.AgribalyseMappingReportPrinter
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.carbon.builder.CarbonKnowledgeBuilder
import de.shopme.tools.knowledge.carbon.importer.AgribalyseCarbonImporter
import de.shopme.tools.knowledge.carbon.importer.CarbonBaselineImporter
import de.shopme.tools.knowledge.carbon.loader.CarbonReferenceLoader
import de.shopme.tools.knowledge.carbon.mapper.CarbonCandidateMapper
import de.shopme.tools.knowledge.carbon.merge.CarbonCandidateMerger
import de.shopme.tools.knowledge.carbon.merge.CarbonMergeReportPrinter
import de.shopme.tools.knowledge.carbon.report.CatalogCarbonGapReportBuilder
import de.shopme.tools.knowledge.carbon.report.CatalogCarbonGapReportPrinter
import de.shopme.tools.knowledge.catalog.CatalogReferenceLoader
import de.shopme.tools.knowledge.compiler.writer.CarbonKnowledgeWriter
import java.io.File

class BuildCarbonKnowledgeStep :

    KnowledgeUpdateStep {

    private val referenceMapper =
        AgribalyseReferenceMapper()



    override fun execute() {

        val agribalyseImporter =

            AgribalyseCarbonImporter(

                file =
                    File(
                        "data/generated/agribalyse-synthese.csv"
                    ),

                productColumn =
                    "Nom du Produit en Français",

                carbonColumn =
                    "Changement climatique"
            )

        val builder =

            CarbonKnowledgeBuilder(

                importers =
                    listOf(

                        CarbonBaselineImporter(
                            file =
                                File(
                                    "src/main/assets/knowledge/runtime/carbon_footprint_master.json"
                                )
                        ),

                        agribalyseImporter
                    ),

                merger =
                    CarbonCandidateMerger(),

                mapper =
                    CarbonCandidateMapper(),

                reportPrinter =
                    CarbonMergeReportPrinter()
            )

        CarbonKnowledgeWriter(
            builder = builder,
            outputFile =
                File(
                    "data/generated/carbon_footprint.json"
                )
        ).finish()

        val catalogReferences =

            CatalogReferenceLoader()
                .load(
                    KnowledgeBuildPaths
                        .default()
                        .canonicalFoodCatalog
                )

        val generatedCarbonFile =

            File(
                "data/generated/carbon_footprint.json"
            )

        val carbonReferences =

            CarbonReferenceLoader()
                .load(
                    generatedCarbonFile
                )

        val agribalyseReferences =

            agribalyseImporter.statistics
                .unmappedReferences
                .keys +
                    agribalyseImporter.statistics
                        .mappedReferences
                        .keys

        val gapReport =

            CatalogCarbonGapReportBuilder()
                .build(
                    catalogReferences = catalogReferences,
                    carbonReferences = carbonReferences,
                    agribalyseReferences = agribalyseReferences
                )

        CatalogCarbonGapReportPrinter()
            .print(
                gapReport
            )

        val statistics =

            agribalyseImporter.statistics

        AgribalyseMappingReportPrinter()
            .print(

                AgribalyseMappingReport(
                    totalRows = statistics.totalRows,
                    mappedRows = statistics.mappedRows,
                    unmappedRows = statistics.validRows - statistics.mappedRows,
                    mappedReferences = statistics.mappedReferences,
                    unmappedReferences = statistics.unmappedReferences
                )
            )
    }
}