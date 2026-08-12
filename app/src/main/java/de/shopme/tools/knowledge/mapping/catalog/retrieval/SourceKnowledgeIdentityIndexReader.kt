package de.shopme.tools.knowledge.mapping.catalog.retrieval

import com.google.gson.Gson
import java.io.File

class SourceKnowledgeIdentityIndexReader(
    private val gson: Gson =
        Gson()
) {

    fun forEach(
        file: File,
        consumer:
            (SourceKnowledgeIdentityRecord) -> Unit
    ) {

        require(
            file.isFile
        ) {
            "Source identity index does not exist: " +
                    file.absolutePath
        }

        file.bufferedReader()
            .useLines { lines ->

                lines
                    .filter(
                        String::isNotBlank
                    )
                    .forEach { line ->

                        consumer(
                            gson.fromJson(
                                line,
                                SourceKnowledgeIdentityRecord::class.java
                            )
                        )
                    }
            }
    }
}