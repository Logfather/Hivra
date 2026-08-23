package de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimLifecycleStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HimCanonicalFamilyCandidateRetrievalTest {

    @Test
    fun enforcesTenResultsAndThreeFullRecordsWithVisibleRank() {
        val families = (1..12).map { family(it, "apple $it") }
        val results =
            HimCanonicalFamilyCandidateRetrieval(families).retrieve(
                HimCanonicalRetrievalQuery("apple", "apple")
            )

        assertEquals(10, results.size)
        assertEquals(3, results.count { it is HimCanonicalRetrievalResult.Full })
        assertEquals(7, results.count { it is HimCanonicalRetrievalResult.Compact })
        assertEquals((1..10).toList(), results.map { it.rank })
        assertIs<HimCanonicalRetrievalResult.Compact>(results[3])
    }

    @Test
    fun compactResultExposesOnlyRankAndCanonicalIdentityFields() {
        val fields =
            HimCanonicalRetrievalResult.Compact::class.java.declaredFields.map { it.name }

        assertTrue(fields.containsAll(setOf("rank", "canonicalId", "canonicalName")))
        assertFalse(fields.any { it.contains("score", ignoreCase = true) })
        assertFalse(fields.any { it.contains("family", ignoreCase = true) })
    }

    private fun family(index: Int, normalized: String) =
        HimCanonicalFamily(
            canonicalId = HimEntityId("A%05d".format(index)),
            canonicalName = "Apple $index",
            normalizedName = normalized,
            taxonomyPaths = emptyList(),
            lifecycleStatus = HimLifecycleStatus.ACTIVE,
            identities = emptyList(),
            variants = emptyList(),
            aliases = emptyList(),
        )
}
