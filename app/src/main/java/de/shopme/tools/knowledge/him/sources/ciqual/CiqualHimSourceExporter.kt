package de.shopme.tools.knowledge.him.sources.ciqual

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.BufferedOutputStream
import java.io.File
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPOutputStream

class CiqualHimSourceExporter(
    private val projector: CiqualHimSourceProjector =
        CiqualHimSourceProjector(),
    private val gson: Gson =
        GsonBuilder()
            .serializeNulls()
            .disableHtmlEscaping()
            .create()
) {

    fun export(
        sourceDirectory: File,
        outputFile: File
    ): CiqualHimSourceArtifact {
        val artifact =
            projector.project(sourceDirectory)

        val outputDirectory =
            outputFile.parentFile

        if (outputDirectory != null) {
            require(
                outputDirectory.mkdirs() ||
                        outputDirectory.isDirectory
            ) {
                "Could not create CIQUAL optimized source directory: " +
                        outputDirectory.absolutePath
            }
        }

        OutputStreamWriter(
            GZIPOutputStream(
                BufferedOutputStream(
                    outputFile.outputStream()
                )
            ),
            StandardCharsets.UTF_8
        ).use { writer ->
            gson.toJson(
                artifact,
                CiqualHimSourceArtifact::class.java,
                writer
            )
        }

        require(
            outputFile.isFile &&
                    outputFile.length() > 0L
        ) {
            "CIQUAL HIM source artifact was not created correctly: " +
                    outputFile.absolutePath
        }

        return artifact
    }
}
