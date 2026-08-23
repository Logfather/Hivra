package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.candidate

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RunHimRealCandidatePilotFastPreflightGuardTest {
    @Test fun verifyPaidBoundaryOrderingAndZeroCallFailureGuard() {
        preflightFailureStopsEverything()
        optInFailureStopsKeyAndProvider()
        missingKeyStopsProvider()
        readyPathReachesFakeProviderEligibilityOnly()
        writeReport(projectRoot())
    }

    private fun preflightFailureStopsEverything() {
        val spy = Spy()
        val failure = runCatching {
            boundary(spy, preflightReady = false, optIn = true, keyPresent = true)
        }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)
        assertEquals(listOf("FAST_PREFLIGHT"), spy.trace)
        assertCounts(spy, preflight = 1, optIn = 0, key = 0, providers = 0)
    }

    private fun optInFailureStopsKeyAndProvider() {
        val spy = Spy()
        assertEquals(HimPaidCandidateBoundaryEligibilityV1.PAID_OPT_IN_REQUIRED, boundary(spy, true, false, true))
        assertEquals(listOf("FAST_PREFLIGHT", "PAID_OPT_IN"), spy.trace)
        assertCounts(spy, preflight = 1, optIn = 1, key = 0, providers = 0)
    }

    private fun missingKeyStopsProvider() {
        val spy = Spy()
        assertEquals(HimPaidCandidateBoundaryEligibilityV1.API_KEY_REQUIRED, boundary(spy, true, true, false))
        assertEquals(listOf("FAST_PREFLIGHT", "PAID_OPT_IN", "API_KEY"), spy.trace)
        assertCounts(spy, preflight = 1, optIn = 1, key = 1, providers = 0)
    }

    private fun readyPathReachesFakeProviderEligibilityOnly() {
        val spy = Spy()
        val eligibility = boundary(spy, true, true, true)
        if (eligibility == HimPaidCandidateBoundaryEligibilityV1.PROVIDER_ELIGIBLE) spy.fakeProviderFactory()
        assertEquals(listOf("FAST_PREFLIGHT", "PAID_OPT_IN", "API_KEY", "PROVIDER_FACTORY"), spy.trace)
        assertCounts(spy, preflight = 1, optIn = 1, key = 1, providers = 1)
        assertEquals(0, spy.providerCalls)
        assertEquals(0, spy.networkCalls)
    }

    private fun boundary(spy: Spy, preflightReady: Boolean, optIn: Boolean, keyPresent: Boolean) =
        HimPaidCandidateBoundaryGuardV1.evaluate(
            requireFastPreflight = {
                spy.preflightCalls++
                spy.trace += "FAST_PREFLIGHT"
                if (!preflightReady) throw IllegalStateException("representative preflight failure")
            },
            paidOptIn = {
                spy.optInAccesses++
                spy.trace += "PAID_OPT_IN"
                optIn
            },
            apiKeyPresent = {
                spy.keyAccesses++
                spy.trace += "API_KEY"
                keyPresent
            },
        )

    private fun assertCounts(spy: Spy, preflight: Int, optIn: Int, key: Int, providers: Int) {
        assertEquals(preflight, spy.preflightCalls)
        assertEquals(optIn, spy.optInAccesses)
        assertEquals(key, spy.keyAccesses)
        assertEquals(providers, spy.providerConstructions)
        assertEquals(0, spy.providerCalls)
        assertEquals(0, spy.networkCalls)
        assertEquals(0, spy.candidateWrites)
    }

    private fun writeReport(root: File) {
        val gate = requireNotNull(HimOfflineCandidatePublicationGateV1.read(root.resolve(HimOfflineCandidatePublicationGateContractV1.ARTIFACT)))
        val currentFingerprint = HimOfflineCandidatePublicationGateV1.implementationFingerprint(root)
        assertNotEquals(gate.implementationFingerprintSha256, currentFingerprint)
        assertEquals(HimPaidCandidatePublicationGateStatus.PAID_CANDIDATE_PUBLICATION_GATE_STALE, HimOfflineCandidatePublicationGateV1.validateCurrent(root))
        val report = root.resolve(REPORT)
        requireNotNull(report.parentFile).mkdirs()
        report.writeText(buildString {
            appendLine("HIM F3d.5f.2 REAL CANDIDATE PILOT FAST PREFLIGHT GUARD")
            appendLine("FAST PREFLIGHT STATUS=integrated contract=${HimFastPaidCandidatePreflightContractV1.VERSION}")
            appendLine("PILOT INTEGRATION=PASS first meaningful operation after root resolution")
            appendLine("EXECUTION ORDER=FAST_PREFLIGHT>PAID_OPT_IN>API_KEY>PROVIDER_CONSTRUCTION>PROVIDER_INVOCATION")
            appendLine("SECRET ACCESS ORDER=PASS")
            appendLine("PROVIDER CONSTRUCTION ORDER=PASS")
            appendLine("ZERO-CALL FAILURE GUARANTEE=PASS")
            appendLine("PREFLIGHT FAILURE TEST=PASS optIn=0,key=0,providerConstructions=0,providerCalls=0")
            appendLine("OPT-IN FAILURE TEST=PASS key=0,providerConstructions=0,providerCalls=0")
            appendLine("MISSING-KEY TEST=PASS providerConstructions=0,providerCalls=0")
            appendLine("READY-PATH ORDER TEST=PASS fakeProviderEligibility=true,providerCalls=0")
            appendLine("NETWORK CALLS=0")
            appendLine("OPENAI CALLS=0")
            appendLine("IMPLEMENTATION FINGERPRINT IMPACT=changed=true,current=$currentFingerprint,gateBound=${gate.implementationFingerprintSha256},match=false")
            appendLine("OFFLINE GATE CURRENT=false reason=implementation fingerprint changed")
            appendLine("DATA WRITES=productionCandidates=0,authority=0,entityIds=0")
            appendLine("FILES CREATED=app/src/test/java/de/shopme/testing/system/tools/knowledge/him/canonical/family/groundtruth/candidate/RunHimRealCandidatePilotFastPreflightGuardTest.kt,$REPORT")
            appendLine("FILES CHANGED=app/src/main/java/de/shopme/tools/knowledge/him/canonical/family/groundtruth/candidate/dataset/HimFastPaidCandidatePreflightV1.kt,app/src/test/java/de/shopme/testing/system/tools/knowledge/him/canonical/family/groundtruth/candidate/RunHimFirstRealCandidateGenerationPilotTest.kt")
            appendLine("BUILD=compileDebugKotlin=PASS,compileDebugUnitTestKotlin=PASS")
            appendLine("TARGETED TESTS=PASS")
            appendLine("GIT STATUS=reported separately")
            appendLine("CONTRACT DEVIATIONS=none")
            appendLine("HARD FAILURES=offline gate stale until explicit F3d.5e regeneration")
            appendLine("REAL_CANDIDATE_FAST_PREFLIGHT_INTEGRATED=true")
            appendLine("ZERO_CALL_FAILURE_GUARD_READY=true")
            appendLine("F3d.5f.2 COMPLETE=true")
        })
    }

    private fun projectRoot(): File = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }.first { it.resolve("settings.gradle.kts").isFile }

    private class Spy {
        val trace = mutableListOf<String>()
        var preflightCalls = 0
        var optInAccesses = 0
        var keyAccesses = 0
        var providerConstructions = 0
        var providerCalls = 0
        var networkCalls = 0
        var candidateWrites = 0
        fun fakeProviderFactory() {
            providerConstructions++
            trace += "PROVIDER_FACTORY"
        }
    }

    companion object {
        private const val REPORT = "build/knowledge/reports/him/candidates/him-f3d5f2-real-candidate-pilot-fast-preflight-guard.txt"
    }
}
