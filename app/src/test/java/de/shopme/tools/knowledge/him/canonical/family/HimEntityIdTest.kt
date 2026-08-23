package de.shopme.tools.knowledge.him.canonical.family

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HimEntityIdTest {

    @Test
    fun acceptsValidIds() {
        listOf(
            "A7x2Qp",
            "0abZ91",
            "q8M2Ls",
        ).forEach { value ->
            assertEquals(
                value,
                HimEntityId(value).value,
            )
        }
    }

    @Test
    fun rejectsInvalidIds() {
        listOf(
            "abc",
            "abcdefg",
            "abc-12",
            "abc_12",
            "äbc123",
            "ABC 12",
        ).forEach { value ->
            assertFailsWith<IllegalArgumentException> {
                HimEntityId(value)
            }
        }
    }
}
