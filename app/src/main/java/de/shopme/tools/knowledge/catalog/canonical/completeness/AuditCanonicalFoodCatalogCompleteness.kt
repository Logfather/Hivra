package de.shopme.tools.knowledge.catalog.canonical.completeness

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.catalog.canonical.completeness.model.CanonicalFoodCatalogGap
import de.shopme.tools.knowledge.catalog.canonical.completeness.model.CanonicalFoodCompletenessAuditResult
import de.shopme.tools.knowledge.catalog.canonical.completeness.model.CanonicalFoodGapEvidence
import java.io.File

class AuditCanonicalFoodCatalogCompleteness {

    fun audit(
        paths: KnowledgeBuildPaths =
            KnowledgeBuildPaths.default()
    ): CanonicalFoodCompletenessAuditResult {

        val catalogFile =
            paths.projectRoot.resolve(
                "build/knowledge/catalog/base-resolved/" +
                        "canonical-food-catalog.base-resolved.json"
            )

        val sourceIdentityFile =
            paths.serverRoot.resolve(
                ".source-identities/source-identities.jsonl"
            )

        require(catalogFile.isFile)
        require(sourceIdentityFile.isFile)

        val catalog =
            readCatalog(
                catalogFile
            )

        val knownTerms =
            catalog
                .flatMap { entry ->

                    buildList {
                        add(
                            normalize(
                                entry.itemname
                            )
                        )

                        add(
                            normalize(
                                entry.normalized
                            )
                        )

                        entry.sourceVariants
                            .forEach {
                                add(
                                    normalize(it)
                                )
                            }
                    }
                }
                .filter(
                    String::isNotBlank
                )
                .toSet()

        val taxonomyMissing =
            GermanSupermarketCanonicalIdentityRegistry
                .identities
                .filter { required ->

                    normalize(
                        required.itemname
                    ) !in
                            knownTerms &&
                            normalize(
                                required.normalized
                            ) !in
                            knownTerms
                }

        val sourceEvidence =
            mutableMapOf<String, MutableSourceGap>()

        var sourceRecordCount =
            0

        sourceIdentityFile
            .bufferedReader()
            .useLines { lines ->

                lines.forEach { line ->

                    if (
                        line.isBlank()
                    ) {
                        return@forEach
                    }

                    sourceRecordCount++

                    val json =
                        JsonParser
                            .parseString(line)
                            .asJsonObject

                    val serverKey =
                        json[
                            "serverKey"
                        ]
                            ?.asString
                            ?.trim()
                            .orEmpty()

                    if (
                        !isUsableSourceIdentity(
                            serverKey
                        )
                    ) {
                        return@forEach
                    }

                    val normalized =
                        normalize(
                            serverKey
                        )

                    if (
                        normalized.isBlank() ||
                        coveredByCatalog(
                            source =
                                normalized,
                            knownTerms =
                                knownTerms
                        )
                    ) {
                        return@forEach
                    }

                    val genericKey =
                        normalizeGapIdentity(
                            serverKey
                        )

                    if (
                        genericKey.isBlank()
                    ) {
                        return@forEach
                    }

                    sourceEvidence
                        .getOrPut(
                            genericKey
                        ) {
                            MutableSourceGap(
                                count =
                                    0,
                                examples =
                                    linkedSetOf()
                            )
                        }
                        .apply {
                            count++

                            if (
                                examples.size <
                                MAXIMUM_SOURCE_EXAMPLES
                            ) {
                                examples +=
                                    serverKey
                            }
                        }
                }
            }

        val taxonomyByNormalized =
            taxonomyMissing
                .associateBy {
                    normalize(
                        it.itemname
                    )
                }

        val gaps =
            mutableListOf<CanonicalFoodCatalogGap>()

        taxonomyMissing
            .forEach { missing ->

                val normalized =
                    normalize(
                        missing.itemname
                    )

                val source =
                    sourceEvidence[
                        normalized
                    ]

                gaps +=
                    CanonicalFoodCatalogGap(
                        proposedItemname =
                            missing.itemname,

                        proposedNormalized =
                            missing.normalized,

                        proposedCategory =
                            missing.category,

                        evidence =
                            if (
                                source != null
                            ) {
                                CanonicalFoodGapEvidence
                                    .TAXONOMY_AND_SOURCE
                            } else {
                                CanonicalFoodGapEvidence
                                    .TAXONOMY
                            },

                        taxonomyRequired =
                            true,

                        sourceEvidenceCount =
                            source?.count
                                ?: 0,

                        sourceExamples =
                            source
                                ?.examples
                                ?.toList()
                                .orEmpty()
                    )
            }

        sourceEvidence
            .asSequence()
            .filter {
                it.value.count >=
                        MINIMUM_SOURCE_EVIDENCE_COUNT
            }
            .filter {
                it.key !in
                        taxonomyByNormalized
            }
            .sortedByDescending {
                it.value.count
            }
            .take(
                MAXIMUM_SOURCE_GAP_CANDIDATES
            )
            .forEach { (normalized, evidence) ->

                gaps +=
                    CanonicalFoodCatalogGap(
                        proposedItemname =
                            evidence.examples
                                .first(),

                        proposedNormalized =
                            normalizedKey(
                                evidence.examples
                                    .first()
                            ),

                        proposedCategory =
                            "UNRESOLVED",

                        evidence =
                            CanonicalFoodGapEvidence
                                .SOURCE,

                        taxonomyRequired =
                            false,

                        sourceEvidenceCount =
                            evidence.count,

                        sourceExamples =
                            evidence.examples
                                .toList()
                    )
            }

        val result =
            CanonicalFoodCompletenessAuditResult(
                version =
                    1,

                catalogEntryCount =
                    catalog.size,

                taxonomyRequiredIdentityCount =
                    GermanSupermarketCanonicalIdentityRegistry
                        .identities
                        .size,

                taxonomyMissingIdentityCount =
                    taxonomyMissing.size,

                sourceIdentityRecordCount =
                    sourceRecordCount,

                sourceGapCandidateCount =
                    gaps.count {
                        it.evidence ==
                                CanonicalFoodGapEvidence.SOURCE
                    },

                combinedGapCount =
                    gaps.size,

                gaps =
                    gaps.sortedWith(
                        compareByDescending<
                                CanonicalFoodCatalogGap
                                > {
                            when (it.evidence) {
                                CanonicalFoodGapEvidence
                                    .TAXONOMY_AND_SOURCE -> 3

                                CanonicalFoodGapEvidence
                                    .TAXONOMY -> 2

                                CanonicalFoodGapEvidence
                                    .SOURCE -> 1
                            }
                        }
                            .thenByDescending {
                                it.sourceEvidenceCount
                            }
                            .thenBy {
                                it.proposedItemname
                            }
                    )
            )

        val reportFile =
            paths.reportsRoot.resolve(
                "canonical-food-catalog-completeness-audit.json"
            )

        writeJson(
            result,
            reportFile
        )

        printReport(
            result,
            reportFile
        )

        return result
    }

    private fun coveredByCatalog(
        source: String,
        knownTerms: Set<String>
    ): Boolean {

        if (
            source in knownTerms
        ) {
            return true
        }

        return knownTerms.any { known ->

            known.length >=
                    MINIMUM_COVERAGE_TERM_LENGTH &&
                    (
                            source.contains(
                                known
                            ) ||
                                    known.contains(
                                        source
                                    )
                            )
        }
    }

    private fun isUsableSourceIdentity(
        value: String
    ): Boolean {

        if (
            value.length <
            MINIMUM_SOURCE_IDENTITY_LENGTH
        ) {
            return false
        }

        if (
            value.length >
            MAXIMUM_SOURCE_IDENTITY_LENGTH
        ) {
            return false
        }

        if (
            value.count {
                it.isLetter()
            } <
            MINIMUM_SOURCE_LETTER_COUNT
        ) {
            return false
        }

        if (
            BRAND_NOISE_REGEX
                .containsMatchIn(
                    value
                )
        ) {
            return false
        }

        return true
    }

    private fun normalizeGapIdentity(
        value: String
    ): String =
        normalize(
            value
        )

    private fun normalize(
        value: String
    ): String =
        value
            .lowercase()
            .replace("ä", "ae")
            .replace("ö", "oe")
            .replace("ü", "ue")
            .replace("ß", "ss")
            .replace(
                Regex(
                    """[^a-z0-9]+"""
                ),
                " "
            )
            .replace(
                Regex(
                    """\s+"""
                ),
                " "
            )
            .trim()

    private fun normalizedKey(
        value: String
    ): String =
        normalize(value)
            .replace(
                " ",
                "-"
            )

    private fun readCatalog(
        file: File
    ): List<CatalogEntry> {

        val root =
            JsonParser
                .parseString(
                    file.readText()
                )
                .asJsonArray

        return root.map { element ->

            val json =
                element.asJsonObject

            CatalogEntry(
                itemname =
                    json[
                        "itemname"
                    ].asString,

                normalized =
                    json[
                        "normalized"
                    ].asString,

                sourceVariants =
                    json[
                        "sourceVariants"
                    ]
                        .asJsonArray
                        .map {
                            it.asString
                        }
            )
        }
    }

    private fun writeJson(
        value: Any,
        file: File
    ) {

        require(
            file.parentFile.exists() ||
                    file.parentFile.mkdirs()
        )

        file.writeText(
            gson.toJson(
                value
            ) + "\n"
        )
    }

    private fun printReport(
        result: CanonicalFoodCompletenessAuditResult,
        reportFile: File
    ) {

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("CANONICAL FOOD CATALOG COMPLETENESS AUDIT")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("Catalog identities       : ${result.catalogEntryCount}")
        println(
            "Required taxonomy IDs    : " +
                    result.taxonomyRequiredIdentityCount
        )
        println(
            "Taxonomy gaps            : " +
                    result.taxonomyMissingIdentityCount
        )
        println(
            "Source identity records  : " +
                    result.sourceIdentityRecordCount
        )
        println(
            "Source gap candidates    : " +
                    result.sourceGapCandidateCount
        )
        println(
            "Total gap candidates     : " +
                    result.combinedGapCount
        )
        println()
        println("Report                   : ${reportFile.path}")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println()
    }

    private data class MutableSourceGap(
        var count: Int,
        val examples: LinkedHashSet<String>
    )

    private data class CatalogEntry(
        val itemname: String,
        val normalized: String,
        val sourceVariants: List<String>
    )

    companion object {

        private const val MINIMUM_SOURCE_EVIDENCE_COUNT =
            3

        private const val MAXIMUM_SOURCE_GAP_CANDIDATES =
            500

        private const val MAXIMUM_SOURCE_EXAMPLES =
            5

        private const val MINIMUM_SOURCE_IDENTITY_LENGTH =
            4

        private const val MAXIMUM_SOURCE_IDENTITY_LENGTH =
            80

        private const val MINIMUM_SOURCE_LETTER_COUNT =
            3

        private const val MINIMUM_COVERAGE_TERM_LENGTH =
            4

        private val BRAND_NOISE_REGEX =
            Regex(
                """(?i)\b(""" +
                        listOf(
                            "brand",
                            "company",
                            "ltd",
                            "inc",
                            "gmbh"
                        )
                            .joinToString("|") +
                        """)\b"""
            )

        private val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()
    }
}