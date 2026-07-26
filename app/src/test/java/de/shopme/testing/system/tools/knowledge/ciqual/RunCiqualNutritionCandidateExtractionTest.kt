package de.shopme.testing.system.tools.knowledge.ciqual

import de.shopme.tools.knowledge.ciqual.extractor.CiqualNutritionCandidateExtractor
import de.shopme.tools.knowledge.ciqual.model.CiqualSourceFiles
import de.shopme.tools.knowledge.ki_candidates.KnowledgeDimensionCandidateType
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCiqualNutritionCandidateExtractionTest {

    @Test
    fun extractsProductiveCiqualNutritionCandidates() {
        val files =
            CiqualSourceFiles.fromDirectory(
                File(
                    "../data/raw/ciqual/Ciqual"
                )
            )

        val candidates =
            CiqualNutritionCandidateExtractor()
                .extract(files)

        val nutritionCandidates =
            candidates.filter { candidate ->
                candidate.dimensions.any { dimension ->
                    dimension.dimension ==
                            KnowledgeDimensionCandidateType.NUTRITION
                }
            }

        val chervilCandidates =
            nutritionCandidates.filter { candidate ->
                candidate.searchableNames()
                    .any { name ->
                        name.contains("chervil") ||
                                name.contains("cerfeuil")
                    }
            }

        val salsifyCandidates =
            nutritionCandidates.filter { candidate ->
                candidate.searchableNames()
                    .any { name ->
                        name.contains("salsify") ||
                                name.contains("salsifis") ||
                                name.contains("scorsonere")
                    }
            }

        val maceCandidates =
            nutritionCandidates.filter { candidate ->
                candidate.searchableNames()
                    .any { name ->
                        name == "mace" ||
                                name.contains(" mace ") ||
                                name.contains("macis")
                    }
            }

        println(
            "CIQUAL candidates=${candidates.size}"
        )
        println(
            "CIQUAL nutrition candidates=${nutritionCandidates.size}"
        )
        println(
            "CIQUAL chervil candidates=${chervilCandidates.size}"
        )
        println(
            "CIQUAL salsify candidates=${salsifyCandidates.size}"
        )
        println(
            "CIQUAL mace candidates=${maceCandidates.size}"
        )

        println(
            "CIQUAL chervil sample=" +
                    chervilCandidates
                        .take(10)
                        .map { candidate ->
                            candidate.canonicalId
                        }
        )

        println(
            "CIQUAL salsify sample=" +
                    salsifyCandidates
                        .take(10)
                        .map { candidate ->
                            candidate.canonicalId
                        }
        )

        assertTrue(
            candidates.isNotEmpty(),
            "CIQUAL must produce canonical candidates."
        )

        assertEquals(
            candidates.size,
            nutritionCandidates.size,
            "Every CIQUAL candidate must contain a Nutrition dimension."
        )

        assertTrue(
            chervilCandidates.isNotEmpty(),
            "CIQUAL must produce at least one chervil candidate."
        )

        assertTrue(
            salsifyCandidates.isNotEmpty(),
            "CIQUAL must produce at least one salsify candidate."
        )

        assertTrue(
            candidates.all { candidate ->
                candidate.metadata.source == "ciqual"
            },
            "Every extracted candidate must retain CIQUAL provenance."
        )

        assertTrue(
            candidates.all { candidate ->
                !candidate.metadata.sourceId.isNullOrBlank()
            },
            "Every extracted candidate must retain its CIQUAL food code."
        )
    }

    private fun de.shopme.tools.knowledge.ki_candidates
    .CanonicalKnowledgeCandidate.searchableNames(): Set<String> =
        buildSet {
            add(
                canonicalId.lowercase()
            )

            aliases
                .mapTo(this) {
                    it.lowercase()
                }

            matchAliases
                .mapTo(this) {
                    it.lowercase()
                }
        }
}