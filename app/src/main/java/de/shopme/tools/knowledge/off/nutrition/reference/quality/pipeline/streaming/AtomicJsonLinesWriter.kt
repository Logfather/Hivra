package de.shopme.tools.knowledge.off.nutrition.reference.quality.pipeline.streaming

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.BufferedWriter
import java.io.Closeable
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class AtomicJsonLinesWriter<T>(
    outputFile: File,
    private val gson: Gson =
        GsonBuilder()
            .disableHtmlEscaping()
            .create()
) : Closeable {

    private val canonicalOutputFile =
        outputFile.canonicalFile

    private val outputDirectory =
        canonicalOutputFile.parentFile

    private val temporaryFile: File

    private val writer: BufferedWriter

    private var itemCount =
        0L

    private var completed =
        false

    init {
        if (outputDirectory != null) {
            Files.createDirectories(
                outputDirectory.toPath()
            )
        }

        temporaryFile =
            if (outputDirectory != null) {
                File(
                    outputDirectory,
                    canonicalOutputFile.name + ".tmp"
                )
            } else {
                File(
                    canonicalOutputFile.name + ".tmp"
                )
            }

        Files.deleteIfExists(
            temporaryFile.toPath()
        )

        writer =
            Files.newBufferedWriter(
                temporaryFile.toPath(),
                StandardCharsets.UTF_8
            )
    }

    fun write(
        item: T
    ) {
        check(!completed) {
            "JSONL writer has already been completed."
        }

        writer.write(
            gson.toJson(item)
        )

        writer.newLine()

        itemCount++
    }

    fun writeAll(
        items: Iterable<T>
    ) {
        items.forEach(::write)
    }

    fun complete(): JsonLinesWriteResult {
        check(!completed) {
            "JSONL writer has already been completed."
        }

        writer.flush()
        writer.close()

        replaceAtomicallyWhenSupported(
            source = temporaryFile,
            target = canonicalOutputFile
        )

        completed =
            true

        return JsonLinesWriteResult(
            outputFile = canonicalOutputFile,
            itemCount = itemCount,
            writtenByteCount = canonicalOutputFile.length()
        )
    }

    override fun close() {
        if (completed) {
            return
        }

        runCatching {
            writer.close()
        }

        Files.deleteIfExists(
            temporaryFile.toPath()
        )
    }

    private fun replaceAtomicallyWhenSupported(
        source: File,
        target: File
    ) {
        try {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }
}