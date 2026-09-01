package de.shopme.tools.knowledge.him.training.runtime

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.security.MessageDigest

private const val RESOLUTION_REFERENCE_PREFIX = "local-model-artifact-resolution:v1:"

/**
 * Resolves an already bound model to three explicitly named, locally verified files.
 * This type performs identity verification only; it never loads or changes model data.
 */
class HimLocalModelArtifactResolverV1 private constructor() {
    enum class ArtifactRole {
        BASE_MODEL,
        TOKENIZER,
        MODEL_CONFIGURATION,
    }

    enum class FailureReason {
        INVALID_ARTIFACT_ROOT,
        INVALID_ARTIFACT_PATH,
        ARTIFACT_NOT_FOUND,
        ARTIFACT_NOT_REGULAR_FILE,
        ARTIFACT_SYMLINK_REJECTED,
        ARTIFACT_ROOT_ESCAPE,
        ARTIFACT_DIGEST_MISMATCH,
    }

    data class Request(
        val modelBinding: HimModelBindingV1,
        val artifactRoot: Path,
        val baseModelPath: Path,
        val tokenizerPath: Path,
        val modelConfigurationPath: Path,
    )

    class VerifiedArtifact internal constructor(
        val role: ArtifactRole,
        val expectedDigest: HimSha256,
        val actualDigest: HimSha256,
        val path: Path,
        val relativePath: Path,
        val byteSize: Long,
    ) {
        override fun equals(other: Any?): Boolean =
            other is VerifiedArtifact &&
                role == other.role &&
                expectedDigest == other.expectedDigest &&
                actualDigest == other.actualDigest &&
                path == other.path &&
                relativePath == other.relativePath &&
                byteSize == other.byteSize

        override fun hashCode(): Int =
            listOf(role, expectedDigest, actualDigest, path, relativePath, byteSize).hashCode()

        override fun toString(): String =
            "VerifiedArtifact(role=$role, relativePath=$relativePath, byteSize=$byteSize)"
    }

    class Resolution internal constructor(
        val modelBinding: HimModelBindingV1,
        val baseModel: VerifiedArtifact,
        val tokenizer: VerifiedArtifact,
        val modelConfiguration: VerifiedArtifact,
        val logicalDigest: HimSha256,
        val resolutionReference: String,
    ) {
        val contractId: String
            get() = CONTRACT_ID

        val version: String
            get() = VERSION

        val state: String
            get() = STATE
    }

    sealed interface Result {
        data class Completed(val value: Resolution) : Result

        data class Failed(
            val reason: FailureReason,
            val safeContext: String,
        ) : Result {
            init {
                require(safeContext.isNotBlank())
                require(safeContext.none { it.isISOControl() })
            }
        }
    }

    private sealed interface RootOutcome {
        data class Success(val root: Path) : RootOutcome
        data class Failure(val result: Result.Failed) : RootOutcome
    }

    private sealed interface ArtifactOutcome {
        data class Success(val artifact: VerifiedArtifact) : ArtifactOutcome
        data class Failure(val result: Result.Failed) : ArtifactOutcome
    }

    companion object {
        const val CONTRACT_ID = "HIM_LOCAL_MODEL_ARTIFACT_RESOLVER_V1"
        const val VERSION = "1"
        const val STATE = "LOCAL_MODEL_ARTIFACTS_VERIFIED"
        const val REQUIRED_ARTIFACT_ROLE_COUNT = 3
        const val MODEL_ARTIFACT_RESOLUTION_REQUIRES_MODEL_BINDING = "YES"
        const val UNBOUNDED_FILESYSTEM_SEARCH = "0"
        const val ARTIFACT_ROOT_ESCAPE = "0"
        const val SYMLINK_POLICY = "REJECT_ALL_SYMLINKED_ARTIFACT_PATHS"
        const val ARTIFACT_DIGEST_VERIFICATION = "SHA256_EXACT"
        const val ABSOLUTE_PATH_IN_LOGICAL_DIGEST = "NO"
        const val VERIFIED_AT_RESOLUTION_TIME = "YES"
        const val ARTIFACT_CONTENT_MUTATIONS = "0"
        const val MODEL_CONTENT_INTERPRETATION = "0"

        @JvmStatic
        fun resolve(request: Request): Result {
            val root = when (val outcome = validateRoot(request.artifactRoot)) {
                is RootOutcome.Success -> outcome.root
                is RootOutcome.Failure -> return outcome.result
            }

            val baseModel = when (
                val outcome = verifyArtifact(
                    root = root,
                    requestedPath = request.baseModelPath,
                    role = ArtifactRole.BASE_MODEL,
                    expectedDigest = request.modelBinding.baseModelArtifactDigest,
                )
            ) {
                is ArtifactOutcome.Success -> outcome.artifact
                is ArtifactOutcome.Failure -> return outcome.result
            }
            val tokenizer = when (
                val outcome = verifyArtifact(
                    root = root,
                    requestedPath = request.tokenizerPath,
                    role = ArtifactRole.TOKENIZER,
                    expectedDigest = request.modelBinding.tokenizerArtifactDigest,
                )
            ) {
                is ArtifactOutcome.Success -> outcome.artifact
                is ArtifactOutcome.Failure -> return outcome.result
            }
            val modelConfiguration = when (
                val outcome = verifyArtifact(
                    root = root,
                    requestedPath = request.modelConfigurationPath,
                    role = ArtifactRole.MODEL_CONFIGURATION,
                    expectedDigest = request.modelBinding.modelConfigurationArtifactDigest,
                )
            ) {
                is ArtifactOutcome.Success -> outcome.artifact
                is ArtifactOutcome.Failure -> return outcome.result
            }

            val digest = resolutionDigest(request.modelBinding, baseModel, tokenizer, modelConfiguration)
            return Result.Completed(
                Resolution(
                    modelBinding = request.modelBinding,
                    baseModel = baseModel,
                    tokenizer = tokenizer,
                    modelConfiguration = modelConfiguration,
                    logicalDigest = digest,
                    resolutionReference = "$RESOLUTION_REFERENCE_PREFIX${digest.value}",
                ),
            )
        }

        private fun validateRoot(requestedRoot: Path): RootOutcome {
            val absoluteRoot = try {
                requestedRoot.toAbsolutePath().normalize()
            } catch (_: RuntimeException) {
                return RootOutcome.Failure(failure(FailureReason.INVALID_ARTIFACT_ROOT, "root"))
            }
            return try {
                when {
                    !Files.exists(absoluteRoot, LinkOption.NOFOLLOW_LINKS) ->
                        RootOutcome.Failure(failure(FailureReason.INVALID_ARTIFACT_ROOT, "root"))
                    Files.isSymbolicLink(absoluteRoot) ->
                        RootOutcome.Failure(failure(FailureReason.ARTIFACT_SYMLINK_REJECTED, "root"))
                    !Files.isDirectory(absoluteRoot, LinkOption.NOFOLLOW_LINKS) ->
                        RootOutcome.Failure(failure(FailureReason.INVALID_ARTIFACT_ROOT, "root"))
                    else -> {
                        val realRoot = absoluteRoot.toRealPath()
                        if (!Files.isDirectory(realRoot, LinkOption.NOFOLLOW_LINKS)) {
                            RootOutcome.Failure(failure(FailureReason.INVALID_ARTIFACT_ROOT, "root"))
                        } else {
                            RootOutcome.Success(realRoot)
                        }
                    }
                }
            } catch (_: IOException) {
                RootOutcome.Failure(failure(FailureReason.INVALID_ARTIFACT_ROOT, "root"))
            } catch (_: SecurityException) {
                RootOutcome.Failure(failure(FailureReason.INVALID_ARTIFACT_ROOT, "root"))
            }
        }

        private fun verifyArtifact(
            root: Path,
            requestedPath: Path,
            role: ArtifactRole,
            expectedDigest: HimSha256,
        ): ArtifactOutcome {
            if (requestedPath.isAbsolute || requestedPath.nameCount == 0) {
                return ArtifactOutcome.Failure(failure(FailureReason.INVALID_ARTIFACT_PATH, role.name))
            }
            if (requestedPath.any { it.toString() == ".." }) {
                return ArtifactOutcome.Failure(failure(FailureReason.INVALID_ARTIFACT_PATH, role.name))
            }

            val relativePath = requestedPath.normalize()
            if (relativePath.nameCount == 0 || relativePath.startsWith("..")) {
                return ArtifactOutcome.Failure(failure(FailureReason.INVALID_ARTIFACT_PATH, role.name))
            }
            val candidate = root.resolve(relativePath).normalize()
            if (!candidate.startsWith(root) || candidate == root) {
                return ArtifactOutcome.Failure(failure(FailureReason.ARTIFACT_ROOT_ESCAPE, role.name))
            }

            return try {
                var current = root
                relativePath.forEachIndexed { index, part ->
                    current = current.resolve(part)
                    if (Files.isSymbolicLink(current)) {
                        val target = try {
                            current.toRealPath()
                        } catch (_: IOException) {
                            return ArtifactOutcome.Failure(
                                failure(FailureReason.ARTIFACT_SYMLINK_REJECTED, role.name),
                            )
                        }
                        val reason = if (target.startsWith(root)) {
                            FailureReason.ARTIFACT_SYMLINK_REJECTED
                        } else {
                            FailureReason.ARTIFACT_ROOT_ESCAPE
                        }
                        return ArtifactOutcome.Failure(failure(reason, role.name))
                    }
                    if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                        return ArtifactOutcome.Failure(failure(FailureReason.ARTIFACT_NOT_FOUND, role.name))
                    }
                    if (index < relativePath.nameCount - 1 &&
                        !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)
                    ) {
                        return ArtifactOutcome.Failure(failure(FailureReason.ARTIFACT_NOT_FOUND, role.name))
                    }
                }

                val realPath = candidate.toRealPath()
                if (!realPath.startsWith(root)) {
                    return ArtifactOutcome.Failure(failure(FailureReason.ARTIFACT_ROOT_ESCAPE, role.name))
                }
                if (!Files.isRegularFile(realPath, LinkOption.NOFOLLOW_LINKS)) {
                    return ArtifactOutcome.Failure(failure(FailureReason.ARTIFACT_NOT_REGULAR_FILE, role.name))
                }

                val actualDigest = HimSha256(streamingSha256(realPath))
                if (actualDigest != expectedDigest) {
                    return ArtifactOutcome.Failure(failure(FailureReason.ARTIFACT_DIGEST_MISMATCH, role.name))
                }
                ArtifactOutcome.Success(
                    VerifiedArtifact(
                        role = role,
                        expectedDigest = expectedDigest,
                        actualDigest = actualDigest,
                        path = realPath,
                        relativePath = relativePath,
                        byteSize = Files.size(realPath),
                    ),
                )
            } catch (_: IOException) {
                ArtifactOutcome.Failure(failure(FailureReason.ARTIFACT_NOT_FOUND, role.name))
            } catch (_: SecurityException) {
                ArtifactOutcome.Failure(failure(FailureReason.ARTIFACT_NOT_FOUND, role.name))
            }
        }

        private fun streamingSha256(path: Path): String {
            val digest = MessageDigest.getInstance("SHA-256")
            Files.newInputStream(path).use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    if (read > 0) digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
        }

        private fun resolutionDigest(
            modelBinding: HimModelBindingV1,
            baseModel: VerifiedArtifact,
            tokenizer: VerifiedArtifact,
            modelConfiguration: VerifiedArtifact,
        ): HimSha256 {
            val canonical = buildString {
                field("contract", CONTRACT_ID)
                field("version", VERSION)
                field("state", STATE)
                field("model-binding-digest", modelBinding.logicalDigest.value)
                field("model-binding-reference", modelBinding.modelBindingReference)
                artifactFields("base-model", baseModel)
                artifactFields("tokenizer", tokenizer)
                artifactFields("model-configuration", modelConfiguration)
            }
            return HimSha256(
                MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toByteArray(Charsets.UTF_8))
                    .joinToString("") { "%02x".format(it.toInt() and 0xff) },
            )
        }

        private fun StringBuilder.artifactFields(prefix: String, artifact: VerifiedArtifact) {
            field("$prefix-role", artifact.role.name)
            field("$prefix-expected-digest", artifact.expectedDigest.value)
            field("$prefix-actual-digest", artifact.actualDigest.value)
            field("$prefix-relative-path", artifact.relativePath.toString())
        }

        private fun StringBuilder.field(key: String, value: String) {
            append(key).append('=').append(value.length).append(':').append(value).append('\n')
        }

        private fun failure(reason: FailureReason, role: String): Result.Failed =
            Result.Failed(reason = reason, safeContext = "role=$role")
    }
}
