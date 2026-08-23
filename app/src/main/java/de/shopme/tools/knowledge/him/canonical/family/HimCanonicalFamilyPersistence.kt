package de.shopme.tools.knowledge.him.canonical.family

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class HimCanonicalFamilyPersistence(
    private val gson: Gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create(),
) {

    fun serialize(value: Any): ByteArray =
        (gson.toJson(value) + "\n").toByteArray(Charsets.UTF_8)

    fun readRegistry(file: File): HimEntityIdRegistry =
        read(file, HimEntityIdRegistry::class.java)

    fun readAuthority(file: File): HimCanonicalFamilyAuthority =
        read(file, HimCanonicalFamilyAuthority::class.java)

    fun writeNew(file: File, content: ByteArray) {
        require(!file.exists()) {
            "Persistent HIM authority artifact already exists: ${file.absolutePath}"
        }

        val parent = requireNotNull(file.parentFile)
        require(parent.exists() || parent.mkdirs()) {
            "Could not create HIM authority directory: ${parent.absolutePath}"
        }

        val temporary =
            Files.createTempFile(
                parent.toPath(),
                ".${file.name}.",
                ".tmp",
            )

        try {
            Files.write(temporary, content)
            try {
                Files.move(
                    temporary,
                    file.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, file.toPath())
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun <T> read(file: File, type: Class<T>): T {
        require(file.isFile) {
            "HIM authority artifact is missing: ${file.absolutePath}"
        }
        return requireNotNull(gson.fromJson(file.readText(), type)) {
            "HIM authority artifact is invalid JSON: ${file.absolutePath}"
        }
    }
}
