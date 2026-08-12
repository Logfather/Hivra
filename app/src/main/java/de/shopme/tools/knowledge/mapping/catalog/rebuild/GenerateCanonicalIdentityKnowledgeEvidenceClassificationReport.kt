package de.shopme.tools.knowledge.mapping.catalog.rebuild

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import java.io.File

class GenerateCanonicalIdentityKnowledgeEvidenceClassificationReport(
    private val classifier:
    CanonicalIdentityKnowledgeEvidenceClassifier =
        CanonicalIdentityKnowledgeEvidenceClassifier()
) {

    fun generate(
        paths: KnowledgeBuildPaths =
            KnowledgeBuildPaths.default(),

        inputFile: File =
            paths.projectRoot.resolve(
                "build/knowledge/catalog/variant-analysis/" +
                        "canonical-identity-knowledge-evidence.json"
            ),

        outputFile: File =
            paths.reportsRoot.resolve(
                "canonical-identity-knowledge-evidence-classification.json"
            )
    ): CanonicalIdentityKnowledgeEvidenceClassificationReport {

        require(
            inputFile.isFile
        ) {
            "Canonical identity Knowledge evidence file not found: " +
                    inputFile.absolutePath
        }

        val relationships =
            readRelationships(
                file =
                    inputFile
            )

        require(
            relationships.size ==
                    EXPECTED_REVIEW_RELATIONSHIP_COUNT
        ) {
            "Expected $EXPECTED_REVIEW_RELATIONSHIP_COUNT " +
                    "canonical identity review relationships, " +
                    "but found ${relationships.size}."
        }

        val classifications =
            classifier.classify(
                relationships =
                    relationships
            )

        val report =
            CanonicalIdentityKnowledgeEvidenceClassificationReport(
                version =
                    1,

                inputRelationshipCount =
                    relationships.size,

                keepIdentityCount =
                    classifications.count {
                        it.decision ==
                                CanonicalIdentityKnowledgeDecision
                                    .KEEP_IDENTITY
                    },

                reviewCount =
                    classifications.count {
                        it.decision ==
                                CanonicalIdentityKnowledgeDecision
                                    .REVIEW
                    },

                insufficientEvidenceCount =
                    classifications.count {
                        it.decision ==
                                CanonicalIdentityKnowledgeDecision
                                    .INSUFFICIENT_EVIDENCE
                    },

                classifications =
                    classifications
            )

        writeJson(
            value =
                report,
            file =
                outputFile
        )

        printReport(
            report =
                report,
            outputFile =
                outputFile
        )

        return report
    }

    private fun readRelationships(
        file: File
    ): List<CanonicalIdentityKnowledgeEvidenceRelationship> {

        val root =
            JsonParser
                .parseString(
                    file.readText()
                )
                .asJsonObject

        val relationships =
            root
                .getAsJsonArray(
                    "relationships"
                )

        return relationships.map { element ->

            val json =
                element.asJsonObject

            val dimensions =
                json
                    .getAsJsonObject(
                        "dimensions"
                    )
                    .entrySet()
                    .associate { (artifactName, dimensionElement) ->

                        val dimensionJson =
                            dimensionElement.asJsonObject

                        artifactName to
                                CanonicalIdentityKnowledgeEvidenceDimension(
                                    comparison =
                                        CanonicalIdentityKnowledgeComparison
                                            .valueOf(
                                                dimensionJson
                                                    .get(
                                                        "comparison"
                                                    )
                                                    .asString
                                            )
                                )
                    }

            CanonicalIdentityKnowledgeEvidenceRelationship(
                parent =
                    json
                        .get(
                            "parent"
                        )
                        .asString,

                candidate =
                    json
                        .get(
                            "candidate"
                        )
                        .asString,

                parentNormalized =
                    json
                        .get(
                            "parentNormalized"
                        )
                        .asString,

                candidateNormalized =
                    json
                        .get(
                            "candidateNormalized"
                        )
                        .asString,

                comparableDimensionCount =
                    json
                        .get(
                            "comparableDimensionCount"
                        )
                        .asInt,

                equalDimensionCount =
                    json
                        .get(
                            "equalDimensionCount"
                        )
                        .asInt,

                differentDimensionCount =
                    json
                        .get(
                            "differentDimensionCount"
                        )
                        .asInt,

                missingDimensionCount =
                    json
                        .get(
                            "missingDimensionCount"
                        )
                        .asInt,

                evidenceState =
                    json
                        .get(
                            "evidenceState"
                        )
                        .asString,

                dimensions =
                    dimensions
            )
        }
    }

    private fun writeJson(
        value: Any,
        file: File
    ) {

        val parent =
            requireNotNull(
                file.parentFile
            ) {
                "Output file has no parent directory: " +
                        file.absolutePath
            }

        require(
            parent.exists() ||
                    parent.mkdirs()
        ) {
            "Unable to create report directory: " +
                    parent.absolutePath
        }

        file.writeText(
            gson.toJson(
                value
            ) + "\n"
        )
    }

    private fun printReport(
        report:
        CanonicalIdentityKnowledgeEvidenceClassificationReport,

        outputFile: File
    ) {

        println()
        println(
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        )
        println(
            "CANONICAL IDENTITY KNOWLEDGE CLASSIFICATION"
        )
        println(
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        )
        println(
            "Relationships          : ${report.inputRelationshipCount}"
        )
        println(
            "KEEP_IDENTITY          : ${report.keepIdentityCount}"
        )
        println(
            "REVIEW                 : ${report.reviewCount}"
        )
        println(
            "INSUFFICIENT_EVIDENCE  : ${report.insufficientEvidenceCount}"
        )
        println()

        report.classifications
            .forEach { classification ->

                println(
                    classification.parent +
                            " -> " +
                            classification.candidate
                )

                println(
                    "  decision             : " +
                            classification.decision
                )

                println(
                    "  identity differences : " +
                            classification
                                .identitySupportingDifferences
                                .joinToString()
                                .ifBlank {
                                    "-"
                                }
                )

                println(
                    "  contextual differences: " +
                            classification
                                .contextualDifferences
                                .joinToString()
                                .ifBlank {
                                    "-"
                                }
                )
            }

        println()
        println(
            "Report                 : ${outputFile.path}"
        )
        println(
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        )
        println()
    }

    companion object {

        private const val EXPECTED_REVIEW_RELATIONSHIP_COUNT =
            25

        private val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()
    }
}