package de.shopme.testing.system.tools.knowledge.him.training.authority

import de.shopme.tools.knowledge.him.training.authority.HimModelVisibleObservedTermAuthorityV2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RunHimModelVisibleObservedTermAuthorityV2Test {
    @Test
    fun exactTwoNegativePairsAreBoundWithoutChangingRejectAuthority() {
        val authority = HimModelVisibleObservedTermAuthorityV2.create()

        assertEquals(2, authority.bindings.size)
        assertTrue(authority.bindings.all { it.observedTerm == "Brie double crème" })
        assertTrue(authority.bindings.all { it.candidateReference == "uVfHe4" })
        assertTrue(authority.bindings.all { it.candidateCompatibility == "REJECT" })
        assertTrue(authority.bindings.all { it.secondaryTarget == 1 && it.secondaryMask == 1 })
        assertEquals(
            setOf(
                "negative-example:v1:113a5ce116991c864581175a486ff1f65ce89c5c9b892f5c0399c40ed9c84146",
                "negative-example:v1:bc4960dcca492cd8fe8f9181d4eb95561fdb254605598403aea70d7efc620107",
            ),
            authority.bindings.map { it.exampleReference }.toSet(),
        )
    }

    @Test
    fun exactEvidenceAndHumanAuthorityBindingsArePreserved() {
        val bindings = HimModelVisibleObservedTermAuthorityV2.create().bindings

        assertEquals(
            setOf(
                "280b91e4c4e10f95288308d50a0df31e3be773385c11afc104e8525127cc5b1e",
                "045ee67398a6f7355b0faef3760ceb0d24bf22515b9e67d84c7931e96d2c9fba",
            ),
            bindings.map { it.evidenceReference }.toSet(),
        )
        assertTrue(bindings.all {
            it.humanAuthorityReference == "user-response:training-input-authority-v2-batch-1" &&
                it.sourceAuthority == "ORIGINAL_V1_HUMAN_AUTHORITY"
        })
    }

    @Test
    fun authorityIsDeterministicAndDigestBound() {
        val first = HimModelVisibleObservedTermAuthorityV2.create()
        val second = HimModelVisibleObservedTermAuthorityV2.create()

        assertEquals(first, second)
        assertEquals(first.logicalDigest, second.logicalDigest)
        assertEquals(
            "model-visible-observed-term-authority:v2:${first.logicalDigest.value}",
            first.reference,
        )
        assertTrue(first.bindings.all {
            it.reference == "model-visible-observed-term-binding:v2:${it.logicalDigest.value}"
        })
    }

    @Test
    fun observedTermIsNotDerivedFromCandidateOrNegativePrefix() {
        val authority = HimModelVisibleObservedTermAuthorityV2.create()

        assertTrue(authority.bindings.all { it.observedTerm != it.candidateReference })
        assertTrue(authority.bindings.all { it.observedTerm != it.exampleReference })
        assertEquals("Brie double crème", HimModelVisibleObservedTermAuthorityV2.OBSERVED_TERM)
    }

    @Test
    fun tamperingWithTermOrCompatibilityFailsClosed() {
        val binding = HimModelVisibleObservedTermAuthorityV2.create().bindings.first()

        assertThrows(IllegalArgumentException::class.java) {
            binding.copy(observedTerm = "Brie")
        }
        assertThrows(IllegalArgumentException::class.java) {
            binding.copy(candidateCompatibility = "COMPATIBLE")
        }
        assertNotEquals("Brie", binding.observedTerm)
    }
}
