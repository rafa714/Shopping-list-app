package com.entgldb.persistence.sqlite

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.entgldb.core.ConflictResolver
import com.entgldb.core.Document
import com.entgldb.core.PeerStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * SQLite-backed [PeerStore] for Android.
 *
 * Schema (version 1):
 * ```
 * CREATE TABLE documents (
 *   collection TEXT NOT NULL,
 *   key        TEXT NOT NULL,
 *   data       TEXT NOT NULL,
 *   timestamp  INTEGER NOT NULL,
 *   deleted    INTEGER NOT NULL DEFAULT 0,
 *   PRIMARY KEY (collection, key)
 * )
 * ```
 */
class SqlitePeerStore(context: Context, dbName: String = "entgldb") : PeerStore {

    private val helper = object : SQLiteOpenHelper(context.applicationContext, dbName, null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS documents (
                    collection TEXT NOT NULL,
                    key        TEXT NOT NULL,
                    data       TEXT NOT NULL,
                    timestamp  INTEGER NOT NULL,
                    deleted    INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (collection, key)
                )
                """.trimIndent()
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    }

    private val _changesApplied = MutableSharedFlow<Set<String>>(extraBufferCapacity = 64)
    override val changesApplied: SharedFlow<Set<String>> = _changesApplied.asSharedFlow()

    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        helper.writableDatabase
        _isReady.value = true
    }

    override suspend fun saveDocument(doc: Document): Unit = withContext(Dispatchers.IO) {
        val db = helper.writableDatabase
        db.insertWithOnConflict(
            "documents",
            null,
            doc.toContentValues(),
            SQLiteDatabase.CONFLICT_REPLACE
        )
        _changesApplied.tryEmit(setOf(doc.collection))
    }

    override suspend fun applyRemoteDocument(doc: Document): Unit = withContext(Dispatchers.IO) {
        val existing = getDocument(doc.collection, doc.key)
        val resolved = if (existing != null) ConflictResolver.resolve(existing, doc) else doc
        val db = helper.writableDatabase
        db.insertWithOnConflict(
            "documents",
            null,
            resolved.toContentValues(),
            SQLiteDatabase.CONFLICT_REPLACE
        )
        _changesApplied.tryEmit(setOf(resolved.collection))
    }

    override suspend fun getDocuments(collection: String): List<Document> =
        withContext(Dispatchers.IO) {
            val db = helper.readableDatabase
            val cursor = db.query(
                "documents",
                null,
                "collection = ? AND deleted = 0",
                arrayOf(collection),
                null,
                null,
                "timestamp DESC"
            )
            val result = mutableListOf<Document>()
            cursor.use {
                while (it.moveToNext()) {
                    result += Document(
                        collection = it.getString(it.getColumnIndexOrThrow("collection")),
                        key        = it.getString(it.getColumnIndexOrThrow("key")),
                        data       = it.getString(it.getColumnIndexOrThrow("data")),
                        timestamp  = it.getLong(it.getColumnIndexOrThrow("timestamp")),
                        deleted    = it.getInt(it.getColumnIndexOrThrow("deleted")) != 0
                    )
                }
            }
            result
        }

    override suspend fun getDocument(collection: String, key: String): Document? =
        withContext(Dispatchers.IO) {
            val db = helper.readableDatabase
            val cursor = db.query(
                "documents",
                null,
                "collection = ? AND key = ?",
                arrayOf(collection, key),
                null, null, null
            )
            cursor.use {
                if (it.moveToFirst()) {
                    Document(
                        collection = it.getString(it.getColumnIndexOrThrow("collection")),
                        key        = it.getString(it.getColumnIndexOrThrow("key")),
                        data       = it.getString(it.getColumnIndexOrThrow("data")),
                        timestamp  = it.getLong(it.getColumnIndexOrThrow("timestamp")),
                        deleted    = it.getInt(it.getColumnIndexOrThrow("deleted")) != 0
                    )
                } else null
            }
        }

    private fun Document.toContentValues() = ContentValues().apply {
        put("collection", collection)
        put("key", key)
        put("data", data)
        put("timestamp", timestamp)
        put("deleted", if (deleted) 1 else 0)
    }
}
