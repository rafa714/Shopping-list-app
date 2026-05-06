package com.entgldb.app.db

import android.content.Context
import com.entgldb.app.models.ShoppingListItem
import com.entgldb.core.Document
import com.entgldb.core.EntglDbNode
import com.entgldb.persistence.sqlite.SqlitePeerStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

enum class SyncStatus { SYNCED, SYNCING, DISCONNECTED }

/**
 * Application-level repository backed by [SqlitePeerStore] + [EntglDbNode].
 *
 * Call [init] once from [Application.onCreate] before accessing any other member.
 * Replace [SqlitePeerStore] with the full network-enabled implementation when
 * the :network module is ready.
 */
object ShoppingRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var _store: SqlitePeerStore? = null
    private var _node: EntglDbNode? = null

    private val store: SqlitePeerStore
        get() = _store ?: error("ShoppingRepository.init(context) not called")

    private val node: EntglDbNode
        get() = _node ?: error("ShoppingRepository.init(context) not called")

    private val _syncStatus = MutableStateFlow(SyncStatus.DISCONNECTED)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    fun init(context: Context) {
        if (_store != null) return
        val s = SqlitePeerStore(context, "shopping_db")
        val n = EntglDbNode(s)
        _store = s
        _node = n

        // Mirror changesApplied into the connection badge state
        s.changesApplied
            .onEach {
                _syncStatus.value = SyncStatus.SYNCING
                delay(400)
                _syncStatus.value = SyncStatus.SYNCED
            }
            .launchIn(scope)

        // Mark SYNCED once the store is ready
        s.isReady
            .onEach { ready -> if (ready) _syncStatus.value = SyncStatus.SYNCED }
            .launchIn(scope)
    }

    /** Reactive list of shopping items. Emits on every local or remote change. */
    val items: Flow<List<ShoppingListItem>>
        get() = node.watchCollection(COLLECTION).map { docs ->
            docs.map { it.toShoppingItem() }
        }

    suspend fun addItem(item: ShoppingListItem) {
        store.saveDocument(item.toDocument())
    }

    suspend fun toggleChecked(id: String) {
        val existing = store.getDocument(COLLECTION, id) ?: return
        val current = existing.toShoppingItem()
        store.saveDocument(current.copy(checked = !current.checked).toDocument(existing.timestamp + 1))
    }

    suspend fun deleteItem(id: String) {
        val existing = store.getDocument(COLLECTION, id) ?: return
        store.saveDocument(existing.copy(deleted = true, timestamp = existing.timestamp + 1))
    }

    // ── Serialization helpers ────────────────────────────────────────────────

    private const val COLLECTION = "shopping_items"

    private fun ShoppingListItem.toDocument(ts: Long = System.currentTimeMillis()): Document {
        val json = buildJsonObject {
            put("name", name)
            put("quantity", quantity)
            put("category", category)
            put("checked", checked)
        }.toString()
        return Document(collection = COLLECTION, key = id, data = json, timestamp = ts)
    }

    private fun Document.toShoppingItem(): ShoppingListItem {
        val obj = runCatching {
            Json.parseToJsonElement(data).jsonObject
        }.getOrDefault(JsonObject(emptyMap()))

        return ShoppingListItem(
            id       = key,
            name     = obj["name"]?.jsonPrimitive?.content ?: "",
            quantity = obj["quantity"]?.jsonPrimitive?.content ?: "",
            category = obj["category"]?.jsonPrimitive?.content ?: "General",
            checked  = obj["checked"]?.jsonPrimitive?.boolean ?: false
        )
    }
}
