package com.entgldb.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onStart
import java.util.UUID

/**
 * Logical node in the EntglDb mesh.
 * Wraps a [PeerStore] and exposes reactive collection queries.
 *
 * Usage:
 * ```
 * val store = SqlitePeerStore(context, "my-db")
 * val node  = EntglDbNode(store)
 * node.watchCollection("todos").collect { items -> ... }
 * ```
 */
class EntglDbNode(val store: PeerStore) {

    val nodeId: String = UUID.randomUUID().toString()

    /**
     * Returns a [Flow] that emits the full list of documents in [collection]
     * immediately on collection and on every subsequent change.
     */
    fun watchCollection(collection: String): Flow<List<Document>> = channelFlow {
        send(store.getDocuments(collection))
        store.changesApplied
            .filter { collection in it }
            .collect { send(store.getDocuments(collection)) }
    }
}
