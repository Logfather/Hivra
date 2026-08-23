package de.shopme.tools.knowledge.him.canonical.family

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.security.MessageDigest

data class HimProductOnlyCanonical(
    val itemname: String,
    val normalized: String,
    val taxonomyPaths: List<List<String>>,
)

data class HimProductOnlyCanonicalMaster(
    val path: String,
    val contentSha256: String,
    val records: List<HimProductOnlyCanonical>,
)

class HimProductOnlyCanonicalMasterReader(
    private val gson: Gson = Gson(),
) {

    fun read(paths: HimCanonicalFamilyPaths): HimProductOnlyCanonicalMaster {
        val file = paths.productOnlyMaster

        require(file.exists()) {
            "Product-Only Canonical Master does not exist: ${file.absolutePath}"
        }
        require(file.isFile) {
            "Product-Only Canonical Master is not a regular file: ${file.absolutePath}"
        }
        require(file.length() > 0L) {
            "Product-Only Canonical Master is empty: ${file.absolutePath}"
        }

        val bytes = file.readBytes()
        val sha256 = sha256(bytes)

        require(sha256 == EXPECTED_CONTENT_SHA256) {
            "Unexpected Product-Only Canonical Master SHA-256: $sha256"
        }

        val type = object : TypeToken<List<HimProductOnlyCanonical>>() {}.type
        val records: List<HimProductOnlyCanonical> =
            gson.fromJson(bytes.toString(Charsets.UTF_8), type)

        require(records.size == EXPECTED_RECORD_COUNT) {
            "Unexpected Product-Only Canonical Master record count: ${records.size}"
        }
        require(records.all { it.itemname.isNotBlank() })
        require(records.all { it.normalized.isNotBlank() })
        require(records.map { it.normalized }.distinct().size == records.size) {
            "Product-Only Canonical Master contains duplicate normalized values."
        }

        return HimProductOnlyCanonicalMaster(
            path = HimCanonicalFamilyPaths.PRODUCT_ONLY_MASTER_PATH,
            contentSha256 = sha256,
            records = records,
        )
    }

    companion object {
        const val EXPECTED_RECORD_COUNT = 1384

        const val EXPECTED_CONTENT_SHA256 =
            "922e3fc71a624a94d6787d772e40bba2e31e102212e16c4b315bd9f5dfa30f4f"

        fun sha256(bytes: ByteArray): String =
            MessageDigest
                .getInstance("SHA-256")
                .digest(bytes)
                .joinToString("") { byte ->
                    "%02x".format(byte.toInt() and 0xff)
                }

        fun sha256(file: File): String = sha256(file.readBytes())
    }
}
