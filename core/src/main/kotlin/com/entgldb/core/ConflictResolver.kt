package com.entgldb.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Resolves write conflicts between a local and a remote [Document].
 *
 * Strategy: Last-Write-Wins (by [Document.timestamp]) plus a
 * **checked-wins** override: if either side has `checked = true` the
 * merged document also has `checked = true`.  This prevents a stale
 * remote from un-checking a locally completed item.
 */
object ConflictResolver {

    private val json = Json { ignoreUnknownKeys = true }

    fun resolve(local: Document, remote: Document): Document {
        val winner = if (remote.timestamp >= local.timestamp) remote else local

        val localChecked = parseChecked(local.data)
        val remoteChecked = parseChecked(remote.data)

        return if (localChecked || remoteChecked) {
            val patched = patchChecked(winner.data, true)
            winner.copy(data = patched)
        } else {
            winner
        }
    }

    private fun parseChecked(jsonStr: String): Boolean = runCatching {
        json.parseToJsonElement(jsonStr).jsonObject["checked"]?.jsonPrimitive?.boolean ?: false
    }.getOrDefault(false)

    private fun patchChecked(jsonStr: String, value: Boolean): String = runCatching {
        val obj = json.parseToJsonElement(jsonStr).jsonObject.toMutableMap()
        obj["checked"] = JsonPrimitive(value)
        JsonObject(obj).toString()
    }.getOrDefault(jsonStr)
}
