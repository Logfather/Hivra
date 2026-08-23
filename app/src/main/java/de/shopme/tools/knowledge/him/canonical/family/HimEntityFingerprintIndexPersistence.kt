package de.shopme.tools.knowledge.him.canonical.family

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class HimEntityFingerprintIndexPersistence(
    private val gson: Gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create(),
) {

    fun serialize(index: HimEntityFingerprintIndex): ByteArray =
        (gson.toJson(index) + "\n").toByteArray(Charsets.UTF_8)

    fun read(file: File): HimEntityFingerprintIndex {
        require(file.isFile) {
            "Entity Fingerprint Search Index is missing: ${file.absolutePath}"
        }
        return requireNotNull(
            gson.fromJson(file.readText(), HimEntityFingerprintIndex::class.java)
        ) {
            "Entity Fingerprint Search Index is invalid JSON: ${file.absolutePath}"
        }
    }

    fun write(file: File, content: ByteArray) {
        val parent = requireNotNull(file.parentFile)
        require(parent.exists() || parent.mkdirs()) {
            "Could not create Entity Fingerprint Index directory: ${parent.absolutePath}"
        }

        val temporary =
            Files.createTempFile(parent.toPath(), ".${file.name}.", ".tmp")

        try {
            Files.write(temporary, content)
            try {
                Files.move(
                    temporary,
                    file.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(
                    temporary,
                    file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }
}
