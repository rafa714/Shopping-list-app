package com.entgldb.core

/**
 * An immutable document stored in EntglDb.
 * [data] is a raw JSON string representing the document payload.
 */
data class Document(
    val collection: String,
    val key: String,
    val data: String,
    val timestamp: Long = System.currentTimeMillis(),
    val deleted: Boolean = false
) {
    companion object {
        fun create(collection: String, key: String, data: String): Document =
            Document(collection = collection, key = key, data = data)
    }
}
