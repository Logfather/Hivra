package de.shopme.tools.knowledge.him.sources.glycemicindex

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.BufferedOutputStream
import java.io.File
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPOutputStream

class GlycemicIndexHimSourceExporter(
    private val projector: GlycemicIndexHimSourceProjector = GlycemicIndexHimSourceProjector(),
    private val gson: Gson = GsonBuilder().serializeNulls().disableHtmlEscaping().create(),
) {

    fun export(sourceFile: File, outputFile: File): GlycemicIndexHimSourceArtifact {
        val artifact = projector.project(sourceFile)
        outputFile.parentFile?.let { directory ->
            require(directory.mkdirs() || directory.isDirectory) {
                "Could not create Glycemic Index optimized source directory: ${directory.absolutePath}"
            }
        }
        OutputStreamWriter(
            GZIPOutputStream(BufferedOutputStream(outputFile.outputStream())),
            StandardCharsets.UTF_8,
        ).use { writer ->
            gson.toJson(artifact, GlycemicIndexHimSourceArtifact::class.java, writer)
        }
        require(outputFile.isFile && outputFile.length() > 0L)
        return artifact
    }
}
