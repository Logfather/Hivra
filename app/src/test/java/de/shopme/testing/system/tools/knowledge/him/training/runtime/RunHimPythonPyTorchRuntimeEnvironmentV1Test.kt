package de.shopme.testing.system.tools.knowledge.him.training.runtime

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.training.runtime.HimPythonPyTorchRuntimeEnvironmentV1
import java.lang.reflect.Modifier
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RunHimPythonPyTorchRuntimeEnvironmentV1Test {
    @Test
    fun validRcEnvironmentCanBeCreated() {
        assertNotNull(environment())
    }

    @Test
    fun contractIdIsCorrect() {
        assertEquals("HIM_PYTHON_PYTORCH_RUNTIME_ENVIRONMENT_V1", environment().contractId)
    }

    @Test
    fun versionIsOne() {
        assertEquals("1", environment().version)
    }

    @Test
    fun stateIsCorrect() {
        assertEquals("PYTHON_PYTORCH_RUNTIME_ENVIRONMENT_VALIDATED", environment().state)
    }

    @Test
    fun identicalInputsProduceIdenticalDigestAndReference() {
        val first = environment()
        val second = environment()
        assertEquals(first.logicalDigest, second.logicalDigest)
        assertEquals(first.environmentReference, second.environmentReference)
        assertEquals(first, second)
    }

    @Test
    fun changingRuntimeChannelChangesDigest() {
        assertNotEquals(
            environment().logicalDigest,
            environment(runtimeChannel = HimPythonPyTorchRuntimeEnvironmentV1.RuntimeChannel.RC_PILOT).logicalDigest,
        )
    }

    @Test
    fun changingPythonVersionChangesDigest() {
        assertNotEquals(environment().logicalDigest, environment(pythonVersion = "3.13.13").logicalDigest)
    }

    @Test
    fun changingPythonImplementationChangesDigest() {
        assertNotEquals(environment().logicalDigest, environment(pythonImplementation = "PyPy").logicalDigest)
    }

    @Test
    fun changingUvVersionChangesDigest() {
        assertNotEquals(environment().logicalDigest, environment(uvVersion = "0.12.3").logicalDigest)
    }

    @Test
    fun changingPlatformChangesDigest() {
        assertNotEquals(environment().logicalDigest, environment(targetOsFamily = "linux").logicalDigest)
    }

    @Test
    fun changingArchitectureChangesDigest() {
        assertNotEquals(environment().logicalDigest, environment(targetArchitecture = "x86_64").logicalDigest)
    }

    @Test
    fun changingPythonAbiChangesDigest() {
        assertNotEquals(environment().logicalDigest, environment(pythonAbi = "cpython-313-linux").logicalDigest)
    }

    @Test
    fun changingPyTorchVersionChangesDigest() {
        assertNotEquals(environment().logicalDigest, environment(pytorchVersion = "2.14.1").logicalDigest)
    }

    @Test
    fun changingPackageSourceChangesDigest() {
        assertNotEquals(
            environment().logicalDigest,
            environment(pytorchPackageSource = "https://download.pytorch.org/whl/other").logicalDigest,
        )
    }

    @Test
    fun changingWheelIdentityChangesDigest() {
        assertNotEquals(
            environment().logicalDigest,
            environment(pytorchWheelIdentity = "torch-2.13.0-cp313-cp313-macosx_14_0_arm64-alt.whl").logicalDigest,
        )
    }

    @Test
    fun changingPackageSha256ChangesDigest() {
        assertNotEquals(environment().logicalDigest, environment(pytorchPackageSha256 = digest("package")).logicalDigest)
    }

    @Test
    fun changingPyTorchGitVersionChangesDigest() {
        assertNotEquals(
            environment().logicalDigest,
            environment(pytorchGitVersion = "18187d9e0fba026dc8217405802ab5381dc88d90").logicalDigest,
        )
    }

    @Test
    fun changingPyprojectDigestChangesDigest() {
        assertNotEquals(environment().logicalDigest, environment(pyprojectSha256 = digest("pyproject")).logicalDigest)
    }

    @Test
    fun changingUvLockDigestChangesDigest() {
        assertNotEquals(environment().logicalDigest, environment(uvLockSha256 = digest("lock")).logicalDigest)
    }

    @Test
    fun changingPythonVersionFileDigestChangesDigest() {
        assertNotEquals(
            environment().logicalDigest,
            environment(pythonVersionFileSha256 = digest("version-file")).logicalDigest,
        )
    }

    @Test
    fun changingEnvironmentImplementationFingerprintChangesDigest() {
        assertNotEquals(
            environment().logicalDigest,
            environment(environmentImplementationFingerprint = digest("environment-implementation")).logicalDigest,
        )
    }

    @Test
    fun currentBootstrapValuesConstructSuccessfully() {
        val result = environment()
        assertEquals(HimPythonPyTorchRuntimeEnvironmentV1.RuntimeChannel.STABLE, result.runtimeChannel)
        assertEquals("3.13.14", result.pythonVersion)
        assertEquals("CPython", result.pythonImplementation)
        assertEquals("0.12.2", result.uvVersion)
        assertEquals("macOS", result.targetOsFamily)
        assertEquals("arm64", result.targetArchitecture)
        assertEquals("cpython-313-darwin", result.pythonAbi)
        assertEquals("2.14.0", result.pytorchVersion)
        assertEquals("https://download.pytorch.org/whl/cpu", result.pytorchPackageSource)
        assertEquals("torch-2.14.0-cp313-cp313-macosx_14_0_arm64.whl", result.pytorchWheelIdentity)
        assertEquals(PACKAGE_SHA256, result.pytorchPackageSha256)
        assertEquals("08187d9e0fba026dc8217405802ab5381dc88d90", result.pytorchGitVersion)
        assertEquals(PYPROJECT_SHA256, result.pyprojectSha256)
        assertEquals(UV_LOCK_SHA256, result.uvLockSha256)
        assertEquals(PYTHON_VERSION_FILE_SHA256, result.pythonVersionFileSha256)
    }

    @Test
    fun exactPythonVersionIsPreserved() {
        assertEquals("3.13.14", environment().pythonVersion)
    }

    @Test
    fun exactPythonImplementationIsPreserved() {
        assertEquals("CPython", environment().pythonImplementation)
    }

    @Test
    fun exactTorchVersionIsPreserved() {
        assertEquals("2.14.0", environment().pytorchVersion)
    }

    @Test
    fun exactStableWheelIdentityIsPreserved() {
        assertEquals("torch-2.14.0-cp313-cp313-macosx_14_0_arm64.whl", environment().pytorchWheelIdentity)
    }

    @Test
    fun exactPackageHashIsPreserved() {
        assertEquals(PACKAGE_SHA256, environment().pytorchPackageSha256)
    }

    @Test
    fun runtimeChannelIsStable() {
        assertEquals(HimPythonPyTorchRuntimeEnvironmentV1.RuntimeChannel.STABLE, environment().runtimeChannel)
    }

    @Test
    fun noDeviceSelectionFieldExists() {
        assertFalse(declaredFieldNames().any { it.contains("device") || it.contains("cuda") || it.contains("mps") })
        assertEquals("NO", HimPythonPyTorchRuntimeEnvironmentV1.DEVICE_SELECTION_BOUND)
    }

    @Test
    fun mpsCapabilityIsNotSemanticEnvironmentIdentity() {
        assertEquals("NO", HimPythonPyTorchRuntimeEnvironmentV1.MPS_CAPABILITY_IN_ENVIRONMENT_DIGEST)
        assertFalse(declaredFieldNames().any { it.contains("mps") })
    }

    @Test
    fun noAbsolutePathFieldExists() {
        assertFalse(declaredFieldNames().any { it.contains("path") || it.contains("directory") })
        assertEquals("NO", HimPythonPyTorchRuntimeEnvironmentV1.ABSOLUTE_PATH_IN_ENVIRONMENT_DIGEST)
    }

    @Test
    fun noVenvPathIdentityExists() {
        assertFalse(declaredFieldNames().any { it.contains("venv") })
    }

    @Test
    fun noPythonExecutablePathIdentityExists() {
        assertFalse(declaredFieldNames().any { it.contains("executable") })
    }

    @Test
    fun noTrainerImplementationFingerprintFieldExists() {
        assertFalse(declaredFieldNames().any { it.contains("trainer") })
        assertEquals("YES", HimPythonPyTorchRuntimeEnvironmentV1.ENVIRONMENT_IDENTITY_SEPARATE_FROM_TRAINER_IMPLEMENTATION)
    }

    @Test
    fun environmentImplementationFingerprintExists() {
        assertTrue(declaredFieldNames().contains("environmentImplementationFingerprint"))
        assertEquals(ENVIRONMENT_IMPLEMENTATION_FINGERPRINT, environment().environmentImplementationFingerprint)
    }

    @Test
    fun transitivePackageGraphIsBoundViaLockfile() {
        assertEquals("YES", HimPythonPyTorchRuntimeEnvironmentV1.TRANSITIVE_PACKAGE_GRAPH_BOUND_VIA_LOCKFILE)
        assertEquals(UV_LOCK_SHA256, environment().uvLockSha256)
    }

    @Test
    fun noRuntimeFilesystemProbeExists() {
        assertEquals(0, HimPythonPyTorchRuntimeEnvironmentV1.RUNTIME_ENVIRONMENT_PROBING_IN_CONTRACT)
        assertEquals(0, HimPythonPyTorchRuntimeEnvironmentV1.ENVIRONMENT_CONTRACT_FILESYSTEM_IO)
        assertFalse(
            declaredImplementationMethodNames().any {
                it.contains("read") || it.contains("probe") || it.contains("file")
            },
        )
    }

    @Test
    fun noProcessBuilderExists() {
        assertEquals(0, HimPythonPyTorchRuntimeEnvironmentV1.PROCESS_EXECUTION)
        assertFalse(declaredNames().any { it.contains("process") || it.contains("exec") })
    }

    @Test
    fun noUvInvocationExists() {
        assertFalse(declaredNames().any { it == "uv" || it.contains("invokeuv") || it.contains("uvrun") })
    }

    @Test
    fun noTorchInvocationOrImportBridgeExists() {
        assertFalse(declaredImplementationMethodNames().any { it.contains("torch") })
    }

    @Test
    fun noNetworkApiExists() {
        assertEquals(0, HimPythonPyTorchRuntimeEnvironmentV1.NETWORK_ACCESS)
        assertFalse(declaredNames().any { it.contains("network") || it.contains("http") || it.contains("socket") })
    }

    @Test
    fun noPackageInstallationExists() {
        assertEquals(0, HimPythonPyTorchRuntimeEnvironmentV1.PACKAGE_INSTALLATION)
        assertFalse(declaredNames().any { it.contains("install") || it.contains("sync") })
    }

    @Test
    fun noModelLoadingExists() {
        assertFalse(declaredNames().any { it.contains("model") || it.contains("load") })
    }

    @Test
    fun noNumericalTrainingExists() {
        assertEquals(0, HimPythonPyTorchRuntimeEnvironmentV1.NUMERICAL_TRAINING_EXECUTION)
        assertFalse(declaredNames().any { it.contains("train") || it.contains("tensor") || it.contains("optimizer") })
    }

    @Test
    fun noPersistenceExists() {
        assertEquals(0, HimPythonPyTorchRuntimeEnvironmentV1.ENVIRONMENT_PERSISTENCE)
        assertFalse(declaredNames().any { it.contains("persist") || it.contains("save") || it.contains("json") })
    }

    @Test
    fun rcToStableChangesRuntimeIdentity() {
        val stable = environment(runtimeChannel = HimPythonPyTorchRuntimeEnvironmentV1.RuntimeChannel.STABLE)
        val rc = environment(runtimeChannel = HimPythonPyTorchRuntimeEnvironmentV1.RuntimeChannel.RC_PILOT)
        assertNotEquals(stable.logicalDigest, rc.logicalDigest)
        assertNotEquals(stable.environmentReference, rc.environmentReference)
        assertEquals("NO", HimPythonPyTorchRuntimeEnvironmentV1.RC_TO_STABLE_SAME_RUNTIME_IDENTITY_POSSIBLE)
    }

    @Test
    fun invalidDigestInputFailsClosed() {
        assertFailsWith<IllegalArgumentException> {
            environment(pytorchPackageSha256 = HimSha256("not-a-sha256"))
        }
    }

    @Test
    fun invalidRequiredStringInputsFailClosed() {
        assertFailsWith<IllegalArgumentException> { environment(pythonVersion = "3.13") }
        assertFailsWith<IllegalArgumentException> { environment(pythonImplementation = " ") }
        assertFailsWith<IllegalArgumentException> { environment(pytorchGitVersion = "not-a-git-hash") }
        assertFailsWith<IllegalArgumentException> {
            environment(pytorchPackageSource = "http://download.pytorch.org/whl/cpu")
        }
        assertFailsWith<IllegalArgumentException> { environment(pytorchWheelIdentity = "/tmp/torch.whl") }
    }

    @Test
    fun environmentReferenceIsDigestBound() {
        val result = environment()
        assertEquals("python-pytorch-runtime-environment:v1:${result.logicalDigest.value}", result.environmentReference)
    }

    @Test
    fun logicalDigestIsCanonicalSha256() {
        assertTrue(environment().logicalDigest.value.matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun environmentReferenceHasStablePrefix() {
        assertTrue(environment().environmentReference.startsWith("python-pytorch-runtime-environment:v1:"))
    }

    @Test
    fun semanticPropertiesAreImmutable() {
        assertTrue(
            HimPythonPyTorchRuntimeEnvironmentV1::class.java.declaredFields
                .filterNot { Modifier.isStatic(it.modifiers) }
                .all { Modifier.isFinal(it.modifiers) },
        )
    }

    @Test
    fun noPythonInvocationMethodExists() {
        assertFalse(declaredImplementationMethodNames().any { it.contains("python") })
    }

    @Test
    fun stableSourceRejectsCredentialsAndQuery() {
        assertFailsWith<IllegalArgumentException> {
            environment(pytorchPackageSource = "https://user:secret@download.pytorch.org/whl/cpu")
        }
        assertFailsWith<IllegalArgumentException> {
            environment(pytorchPackageSource = "https://download.pytorch.org/whl/cpu?channel=stable")
        }
    }

    @Test
    fun futureRuntimeBindingHasStableIdentityInputs() {
        val environment = environment()
        assertEquals(environment.logicalDigest.value, environment.environmentReference.substringAfterLast(':'))
        assertEquals(64, environment.environmentImplementationFingerprint.value.length)
    }

    @Test
    fun stablePolicyIsFrozen() {
        assertEquals("STABLE", HimPythonPyTorchRuntimeEnvironmentV1.PYTORCH_RUNTIME_CHANNEL)
        assertEquals("YES", HimPythonPyTorchRuntimeEnvironmentV1.PYTORCH_STABLE_SOURCE_BOUND)
        assertEquals(0, HimPythonPyTorchRuntimeEnvironmentV1.FLOATING_RUNTIME_DEPENDENCIES)
        assertEquals(0, HimPythonPyTorchRuntimeEnvironmentV1.RUNTIME_PACKAGE_INSTALLATION)
        assertEquals(0, HimPythonPyTorchRuntimeEnvironmentV1.RUNTIME_DEPENDENCY_RESOLUTION)
        assertEquals("YES", HimPythonPyTorchRuntimeEnvironmentV1.RC_TO_STABLE_IS_NEW_RUNTIME_IDENTITY)
    }

    @Test
    fun hostOsPointVersionIsExcludedFromIdentity() {
        assertEquals("NO", HimPythonPyTorchRuntimeEnvironmentV1.HOST_OS_VERSION_IN_LOGICAL_DIGEST)
        assertFalse(declaredFieldNames().any { it.contains("osversion") || it.contains("macosversion") })
    }

    @Test
    fun packageIdentityIsNotVersionOnly() {
        assertEquals("NO", HimPythonPyTorchRuntimeEnvironmentV1.PYTORCH_VERSION_STRING_ONLY_IDENTITY)
        assertEquals("YES", HimPythonPyTorchRuntimeEnvironmentV1.PYTORCH_WHEEL_IDENTITY_BOUND)
        assertEquals("YES", HimPythonPyTorchRuntimeEnvironmentV1.PYTORCH_PACKAGE_SHA256_BOUND)
    }

    @Test
    fun pythonVersionFileDigestIsBound() {
        assertEquals("YES", HimPythonPyTorchRuntimeEnvironmentV1.PYTHON_VERSION_FILE_SHA256_BOUND)
        assertEquals(PYTHON_VERSION_FILE_SHA256, environment().pythonVersionFileSha256)
    }

    @Test
    fun environmentHasNoPublicAlternateFactory() {
        val factories = HimPythonPyTorchRuntimeEnvironmentV1::class.java.declaredMethods.filter {
            it.name == "create" && Modifier.isPublic(it.modifiers)
        }
        assertEquals(1, factories.size)
    }

    private fun declaredFieldNames(): Set<String> =
        HimPythonPyTorchRuntimeEnvironmentV1::class.java.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }
            .map { it.name }
            .toSet()

    private fun declaredMethodNames(): Set<String> =
        HimPythonPyTorchRuntimeEnvironmentV1::class.java.declaredMethods.map { it.name.lowercase() }.toSet()

    private fun declaredImplementationMethodNames(): Set<String> =
        declaredMethodNames().filterNot { it.startsWith("get") }.toSet()

    private fun declaredNames(): Set<String> =
        declaredFieldNames().map { it.lowercase() }.toSet() + declaredMethodNames()

    private fun environment(
        runtimeChannel: HimPythonPyTorchRuntimeEnvironmentV1.RuntimeChannel =
            HimPythonPyTorchRuntimeEnvironmentV1.RuntimeChannel.STABLE,
        pythonVersion: String = "3.13.14",
        pythonImplementation: String = "CPython",
        uvVersion: String = "0.12.2",
        targetOsFamily: String = "macOS",
        targetArchitecture: String = "arm64",
        pythonAbi: String = "cpython-313-darwin",
        pytorchVersion: String = "2.14.0",
        pytorchPackageSource: String = "https://download.pytorch.org/whl/cpu",
        pytorchWheelIdentity: String = "torch-2.14.0-cp313-cp313-macosx_14_0_arm64.whl",
        pytorchPackageSha256: HimSha256 = PACKAGE_SHA256,
        pytorchGitVersion: String = "08187d9e0fba026dc8217405802ab5381dc88d90",
        pyprojectSha256: HimSha256 = PYPROJECT_SHA256,
        uvLockSha256: HimSha256 = UV_LOCK_SHA256,
        pythonVersionFileSha256: HimSha256 = PYTHON_VERSION_FILE_SHA256,
        environmentImplementationFingerprint: HimSha256 = ENVIRONMENT_IMPLEMENTATION_FINGERPRINT,
    ): HimPythonPyTorchRuntimeEnvironmentV1 =
        HimPythonPyTorchRuntimeEnvironmentV1.create(
            runtimeChannel = runtimeChannel,
            pythonVersion = pythonVersion,
            pythonImplementation = pythonImplementation,
            uvVersion = uvVersion,
            targetOsFamily = targetOsFamily,
            targetArchitecture = targetArchitecture,
            pythonAbi = pythonAbi,
            pytorchVersion = pytorchVersion,
            pytorchPackageSource = pytorchPackageSource,
            pytorchWheelIdentity = pytorchWheelIdentity,
            pytorchPackageSha256 = pytorchPackageSha256,
            pytorchGitVersion = pytorchGitVersion,
            pyprojectSha256 = pyprojectSha256,
            uvLockSha256 = uvLockSha256,
            pythonVersionFileSha256 = pythonVersionFileSha256,
            environmentImplementationFingerprint = environmentImplementationFingerprint,
        )

    private fun digest(seed: String): HimSha256 = HimSha256(
        MessageDigest.getInstance("SHA-256")
            .digest(seed.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) },
    )

    companion object {
        private val PACKAGE_SHA256 = HimSha256("caf6359d64c0074bcb9f8641a169239118c8165a0a0fef79110c72bfd6474bec")
        private val PYPROJECT_SHA256 = HimSha256("eff3c46dcad347f3c7386f326c60c54bc4f678f59cff28567bc526f77dbc6255")
        private val UV_LOCK_SHA256 = HimSha256("fdf328788dbd8885cb0337783156733263e1f9b869f3794629f238fcfa5a55f6")
        private val PYTHON_VERSION_FILE_SHA256 = HimSha256("f8faecf2505680716c6279bf2cdec3d5a5ba2ba852f0d7df45d51ac1ce8d9ade")
        private val ENVIRONMENT_IMPLEMENTATION_FINGERPRINT = HimSha256("e".repeat(64))
    }
}
