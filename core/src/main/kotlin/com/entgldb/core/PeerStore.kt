package com.entgldb.core

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Storage backend contract for EntglDb.
 *
 * Implementations: [com.entgldb.persistence.sqlite.SqlitePeerStore]
 */
interface PeerStore {
    /** Emits the set of collection names that changed (local write or remote apply). */
    val changesApplied: SharedFlow<Set<String>>

    /** True when the local store is ready to read/write. */
    val isReady: StateFlow<Boolean>

    /** Persist a locally-authored document. Emits on [changesApplied]. */
    suspend fun saveDocument(doc: Document)

    /**
     * Apply a document received from a remote peer.
     * Runs conflict resolution before persisting.
     */
    suspend fun applyRemoteDocument(doc: Document)

    /** Return all non-deleted documents for the given collection. */
    suspend fun getDocuments(collection: String): List<Document>

    /** Return a single document or null if not found. */
    suspend fun getDocument(collection: String, key: String): Document?
}
