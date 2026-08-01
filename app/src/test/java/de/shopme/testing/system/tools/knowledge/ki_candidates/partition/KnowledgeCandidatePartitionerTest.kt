package de.shopme.testing.system.tools.knowledge.ki_candidates.partition

import de.shopme.tools.knowledge.ki_candidates.partition.KnowledgeCandidatePartitioner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KnowledgeCandidatePartitionerTest {

    @Test
    fun partitionIndex_isDeterministic() {
        val partitioner =
            KnowledgeCandidatePartitioner(
                partitionCount =
                    256
            )

        val first =
            partitioner.partitionIndex(
                "apple"
            )

        val second =
            partitioner.partitionIndex(
                "apple"
            )

        assertEquals(
            first,
            second
        )

        assertTrue(
            first in 0 until 256
        )
    }

    @Test
    fun constructor_rejectsNonPowerOfTwo() {
        assertFailsWith<
                IllegalArgumentException
                > {
            KnowledgeCandidatePartitioner(
                partitionCount =
                    100
            )
        }
    }
}