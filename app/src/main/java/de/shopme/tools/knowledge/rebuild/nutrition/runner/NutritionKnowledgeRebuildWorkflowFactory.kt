package de.shopme.tools.knowledge.rebuild.nutrition.runner

import de.shopme.tools.knowledge.ai.AIProviderConfig
import de.shopme.tools.knowledge.ai.openai.OpenAIProvider
import de.shopme.tools.knowledge.ai.openai.OpenAIProviderConfig
import de.shopme.tools.knowledge.ai.openai.RealOpenAIHttpClient
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchRequestWriter
import de.shopme.tools.knowledge.mapping.catalog.DefaultCatalogKnowledgeMatchRequestGenerator
import de.shopme.tools.knowledge.mapping.catalog.OpenAICatalogKnowledgeMatcherFactory
import de.shopme.tools.knowledge.mapping.catalog.local.ConservativeLocalNutritionMatcher
import de.shopme.tools.knowledge.mapping.catalog.local.LocalFirstCatalogKnowledgeMatcher
import de.shopme.tools.knowledge.mapping.catalog.runner.RunOpenAINutritionKnowledgeMatcher
import de.shopme.tools.knowledge.mapping.catalog.runner.WriteValidatedNutritionCatalogServerMappings
import de.shopme.tools.knowledge.rebuild.nutrition.NutritionKnowledgeRebuildMode
import de.shopme.tools.knowledge.rebuild.nutrition.NutritionKnowledgeRebuildWorkflow
import de.shopme.tools.knowledge.rebuild.nutrition.adapter.DefaultNutritionKnowledgeMappingPersistenceStep
import de.shopme.tools.knowledge.rebuild.nutrition.adapter.DefaultNutritionKnowledgeRuntimeRebuildStep
import de.shopme.tools.knowledge.rebuild.nutrition.adapter.DefaultNutritionKnowledgeSnapshotReader
import de.shopme.tools.knowledge.rebuild.nutrition.adapter.ModeAwareNutritionKnowledgeMatchingStep
import de.shopme.tools.knowledge.rebuild.nutrition.adapter.NutritionExactMappingSynchronizer
import de.shopme.tools.knowledge.rebuild.nutrition.adapter.NutritionUnresolvedDecisionPreparer
import de.shopme.tools.knowledge.rebuild.nutrition.adapter.OfflineFallbackCatalogKnowledgeMatcher
import de.shopme.tools.knowledge.rebuild.nutrition.adapter.OfflineNutritionKnowledgeMatchingStep
import de.shopme.tools.knowledge.rebuild.nutrition.adapter.PersistedNutritionKnowledgeRequestRebuilder
import de.shopme.tools.knowledge.rebuild.nutrition.adapter.ProductiveLowConfidenceNutritionMatchValidator
import de.shopme.tools.knowledge.rebuild.nutrition.adapter.ProductiveNutritionKnowledgeMatchingStep
import de.shopme.tools.knowledge.runtime.CatalogRuntimeKnowledgeGenerator
import java.io.PrintStream

class NutritionKnowledgeRebuildWorkflowFactory(
    private val output: PrintStream =
        System.out
) {

    fun create(
        mode: NutritionKnowledgeRebuildMode,
        files: NutritionKnowledgeRebuildProjectFiles
    ): NutritionKnowledgeRebuildWorkflow {

        validateInputFiles(
            mode = mode,
            files = files
        )

        val conservativeLocalMatcher =
            ConservativeLocalNutritionMatcher
                .fromModelFile(
                    modelFile =
                        files.localModelFile,
                    autoAcceptThreshold =
                        ConservativeLocalNutritionMatcher
                            .DEFAULT_AUTO_ACCEPT_THRESHOLD
                )

        val unresolvedDecisionPreparer =
            NutritionUnresolvedDecisionPreparer(
                decisionFile =
                    files.decisionFile,
                validationReportFile =
                    files.validationReportFile
            )

        val offlineMatcher =
            LocalFirstCatalogKnowledgeMatcher(
                localMatcher =
                    conservativeLocalMatcher,
                fallbackMatcher =
                    OfflineFallbackCatalogKnowledgeMatcher()
            )

        val offlineMatchingStep =
            OfflineNutritionKnowledgeMatchingStep(
                matcher =
                    offlineMatcher,
                requestFile =
                    files.requestFile,
                decisionFile =
                    files.decisionFile,
                unresolvedDecisionPreparer =
                    unresolvedDecisionPreparer
            )

        val productiveMatchingStep =
            if (
                mode ==
                NutritionKnowledgeRebuildMode.PRODUCTIVE
            ) {

                createProductiveMatchingStep(
                    files =
                        files,
                    conservativeLocalMatcher =
                        conservativeLocalMatcher
                )

            } else {
                null
            }

        val matchingStep =
            ModeAwareNutritionKnowledgeMatchingStep(
                offline =
                    offlineMatchingStep,
                productive =
                    productiveMatchingStep
            )

        val mappingRunner =
            WriteValidatedNutritionCatalogServerMappings(
                requestFile =
                    files.requestFile,
                decisionFile =
                    files.decisionFile,
                diagnosticsFile =
                    files.diagnosticsFile,
                serverArtifactFile =
                    files.serverNutritionFile,
                exactMappingFile =
                    files.exactMappingFile,
                outputMappingFile =
                    files.outputMappingFile,
                validationReportFile =
                    files.validationReportFile,
                minimumConfidence =
                    MINIMUM_MAPPING_CONFIDENCE,
                printLine =
                    output::println
            )

        val runtimeGenerator =
            CatalogRuntimeKnowledgeGenerator(
                printLine =
                    output::println
            )

        val requestGenerator =
            DefaultCatalogKnowledgeMatchRequestGenerator(
                serverArtifactFile =
                    files.serverNutritionFile
            )

        val requestWriter =
            CatalogKnowledgeMatchRequestWriter()

        val exactMappingSynchronizer =
            NutritionExactMappingSynchronizer(
                exactMatchReportFile =
                    files.exactMatchReportFile,
                exactMappingFile =
                    files.exactMappingFile,
                serverNutritionFile =
                    files.serverNutritionFile
            )

        return NutritionKnowledgeRebuildWorkflow(
            snapshotReader =
                DefaultNutritionKnowledgeSnapshotReader(
                    catalogFile =
                        files.catalogFile,
                    exactMappingFile =
                        files.exactMappingFile,
                    runtimeNutritionFile =
                        files.runtimeNutritionFile,
                    mappingFile =
                        files.outputMappingFile
                ),
            requestRebuilder =
                PersistedNutritionKnowledgeRequestRebuilder(
                    requestFile =
                        files.requestFile,
                    rebuildRequests = {

                        val synchronizationResult =
                            exactMappingSynchronizer
                                .synchronize()

                        output.println(
                            "Exact nutrition mappings synchronized: " +
                                    "existing=" +
                                    synchronizationResult.existingMappingCount +
                                    ", added=" +
                                    synchronizationResult.addedMappingCount +
                                    ", final=" +
                                    synchronizationResult.finalMappingCount
                        )

                        val requests =
                            requestGenerator.generate(
                                matchReportFile =
                                    files.matchReportFile
                            )

                        requestWriter.write(
                            requests =
                                requests,
                            file =
                                files.requestFile
                        )

                        output.println(
                            "Nutrition match requests rebuilt: " +
                                    requests.requests.size
                        )
                    }
                ),
            matchingStep =
                matchingStep,
            mappingPersistenceStep =
                DefaultNutritionKnowledgeMappingPersistenceStep(
                    outputMappingFile =
                        files.outputMappingFile,
                    representativeValidationFile =
                        files.representativeValidationFile,
                    persistMappings = {
                        mappingRunner.run()
                    },
                    validateProductiveLowConfidenceMatches =
                        if (
                            mode ==
                            NutritionKnowledgeRebuildMode.PRODUCTIVE
                        ) {
                            {
                                ProductiveLowConfidenceNutritionMatchValidator(
                                    requestFile =
                                        files.requestFile,
                                    decisionFile =
                                        files.decisionFile,
                                    representativeValidationFile =
                                        files.representativeValidationFile,
                                    minimumConfidence =
                                        MINIMUM_MAPPING_CONFIDENCE
                                )
                                    .run()
                            }
                        } else {
                            null
                        }
                ),
            runtimeRebuildStep =
                DefaultNutritionKnowledgeRuntimeRebuildStep(
                    runtimeNutritionFile =
                        files.runtimeNutritionFile,
                    rebuildRuntime = {

                        runtimeGenerator.generate(
                            catalogFile =
                                files.catalogFile,
                            serverArtifactDirectory =
                                files.serverArtifactDirectory,
                            runtimeArtifactDirectory =
                                files.runtimeArtifactDirectory,
                            catalogServerMappingFile =
                                files.outputMappingFile
                        )
                    }
                ),
            files =
                files.toResultFiles(),
            resultFile =
                files.rebuildResultFile,
            output =
                output
        )
    }

    private fun createProductiveMatchingStep(
        files: NutritionKnowledgeRebuildProjectFiles,
        conservativeLocalMatcher:
        ConservativeLocalNutritionMatcher
    ): ProductiveNutritionKnowledgeMatchingStep {

        val openAIConfig =
            OpenAIProviderConfig
                .fromEnvironment()

        val providerConfig =
            AIProviderConfig(
                providerName =
                    "openai",
                model =
                    openAIConfig.model,
                apiKey =
                    openAIConfig.apiKey,
                endpoint =
                    openAIConfig.endpoint,
                temperature =
                    null
            )

        val provider =
            OpenAIProvider(
                config =
                    providerConfig,
                httpClient =
                    RealOpenAIHttpClient(
                        config =
                            providerConfig
                    )
            )

        val chatGptMatcher =
            OpenAICatalogKnowledgeMatcherFactory(
                openAIProvider =
                    provider
            )
                .create()

        val localFirstMatcher =
            LocalFirstCatalogKnowledgeMatcher(
                localMatcher =
                    conservativeLocalMatcher,
                fallbackMatcher =
                    chatGptMatcher
            )

        val runner =
            RunOpenAINutritionKnowledgeMatcher(
                matcher =
                    localFirstMatcher,
                requestFile =
                    files.requestFile,
                decisionFile =
                    files.decisionFile,
                errorFile =
                    files.errorFile,
                printLine =
                    output::println
            )

        return ProductiveNutritionKnowledgeMatchingStep(
            runner =
                runner,
            requestFile =
                files.requestFile,
            decisionFile =
                files.decisionFile
        )
    }

    private fun validateInputFiles(
        mode: NutritionKnowledgeRebuildMode,
        files: NutritionKnowledgeRebuildProjectFiles
    ) {
        require(files.catalogFile.isFile) {
            "Nutrition rebuild catalog file does not exist: " +
                    files.catalogFile.absolutePath
        }

        require(files.matchReportFile.isFile) {
            "Nutrition catalog-server match report does not exist: " +
                    files.matchReportFile.absolutePath
        }

        require(files.exactMatchReportFile.isFile) {
            "Nutrition exact-match report does not exist: " +
                    files.exactMatchReportFile.absolutePath
        }

        require(files.requestFile.isFile) {
            "Nutrition rebuild request file does not exist: " +
                    files.requestFile.absolutePath
        }

        require(files.serverArtifactDirectory.isDirectory) {
            "Server artifact directory does not exist: " +
                    files.serverArtifactDirectory.absolutePath
        }

        require(files.localModelFile.isFile) {
            "Local nutrition matcher model does not exist: " +
                    files.localModelFile.absolutePath
        }

        require(
            files.representativeValidationFile.isFile
        ) {
            "Representative nutrition validation file does not exist: " +
                    files.representativeValidationFile.absolutePath
        }

        if (
            mode ==
            NutritionKnowledgeRebuildMode.PRODUCTIVE
        ) {
            require(
                System.getenv("OPENAI_API_KEY")
                    ?.isNotBlank() ==
                        true
            ) {
                "PRODUCTIVE nutrition rebuild requires " +
                        "OPENAI_API_KEY."
            }
        }
    }

    private companion object {

        const val MINIMUM_MAPPING_CONFIDENCE =
            0.80
    }
}