package de.shopme.tools.knowledge.him.canonical.family

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class HimEntityFingerprintGeneratorTest {

    private val generator = HimEntityFingerprintGenerator()
    private val canonicalId = HimEntityId("A7x2Qp")

    @Test
    fun generatesCanonicalOnlyFingerprintFromExactCanonicalInput() {
        assertEquals(
            "canonicalId=A7x2Qp|identityId=|variantIds=",
            generator.canonicalInput(canonicalId, null, emptyList()),
        )
        assertTrue(
            generator.generate(canonicalId, null, emptyList())
                .matches(Regex("[0-9a-f]{64}"))
        )
    }

    @Test
    fun identityDifferenceChangesFingerprint() {
        assertNotEquals(
            generator.generate(canonicalId, HimEntityId("Ident1"), emptyList()),
            generator.generate(canonicalId, HimEntityId("Ident2"), emptyList()),
        )
    }

    @Test
    fun variantDifferenceChangesFingerprint() {
        assertNotEquals(
            generator.generate(canonicalId, null, emptyList()),
            generator.generate(canonicalId, null, listOf(HimEntityId("Var001"))),
        )
    }

    @Test
    fun variantOrderDoesNotChangeFingerprint() {
        val first = HimEntityId("Aaaaa1")
        val second = HimEntityId("Bbbbb2")

        assertEquals(
            generator.generate(canonicalId, null, listOf(first, second)),
            generator.generate(canonicalId, null, listOf(second, first)),
        )
    }
}
