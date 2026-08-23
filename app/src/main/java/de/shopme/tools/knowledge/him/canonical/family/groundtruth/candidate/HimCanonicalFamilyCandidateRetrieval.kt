package de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId

data class HimCanonicalRetrievalQuery(
    val rawQuery: String,
    val normalizedQuery: String,
) {
    init {
        require(rawQuery.isNotBlank())
        require(normalizedQuery.isNotBlank())
    }
}

sealed interface HimCanonicalRetrievalResult {
    val rank: Int
    val canonicalId: HimEntityId
    val canonicalName: String

    data class Full(
        override val rank: Int,
        val family: HimCanonicalFamily,
    ) : HimCanonicalRetrievalResult {
        override val canonicalId = family.canonicalId
        override val canonicalName = family.canonicalName
    }

    data class Compact(
        override val rank: Int,
        override val canonicalId: HimEntityId,
        override val canonicalName: String,
    ) : HimCanonicalRetrievalResult
}

class HimCanonicalFamilyCandidateRetrieval(
    private val families: List<HimCanonicalFamily>,
) {

    fun retrieve(query: HimCanonicalRetrievalQuery): List<HimCanonicalRetrievalResult> =
        families
            .asSequence()
            .mapNotNull { family ->
                retrievalGroup(family.normalizedName, query.normalizedQuery)?.let { group ->
                    group to family
                }
            }
            .sortedWith(
                compareBy<Pair<Int, HimCanonicalFamily>>(
                    { it.first },
                    { it.second.normalizedName },
                    { it.second.canonicalId.value },
                )
            )
            .take(MAX_CANDIDATES)
            .mapIndexed { index, (_, family) ->
                val rank = index + 1
                if (rank <= MAX_FULL_RECORDS) {
                    HimCanonicalRetrievalResult.Full(rank, family)
                } else {
                    HimCanonicalRetrievalResult.Compact(
                        rank = rank,
                        canonicalId = family.canonicalId,
                        canonicalName = family.canonicalName,
                    )
                }
            }
            .toList()

    private fun retrievalGroup(normalizedName: String, normalizedQuery: String): Int? =
        when {
            normalizedName == normalizedQuery -> 0
            normalizedName.startsWith(normalizedQuery) -> 1
            normalizedName.contains(normalizedQuery) -> 2
            normalizedQuery.split(Regex("\\s+")).any { it in normalizedName } -> 3
            else -> null
        }

    companion object {
        const val MAX_CANDIDATES = 10
        const val MAX_FULL_RECORDS = 3
    }
}
