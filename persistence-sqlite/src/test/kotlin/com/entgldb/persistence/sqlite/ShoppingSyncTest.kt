package com.entgldb.persistence.sqlite

import androidx.test.core.app.ApplicationProvider
import com.entgldb.core.Document
import com.entgldb.core.EntglDbNode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies that a document added to one [SqlitePeerStore] instance
 * propagates correctly to a second instance via [SqlitePeerStore.applyRemoteDocument].
 *
 * Uses Robolectric so no physical device or emulator is needed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ShoppingSyncTest {

    private lateinit var store1: SqlitePeerStore
    private lateinit var store2: SqlitePeerStore
    private lateinit var node1: EntglDbNode
    private lateinit var node2: EntglDbNode

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        store1 = SqlitePeerStore(ctx, "test_db_1")
        store2 = SqlitePeerStore(ctx, "test_db_2")
        node1 = EntglDbNode(store1)
        node2 = EntglDbNode(store2)
    }

    @Test
    fun `item added to node1 appears in node2 after sync`() = runBlocking {
        val itemJson = """{"name":"Milk","quantity":"1L","category":"Dairy","checked":false}"""
        val doc = Document.create("shopping_items", "item-milk", itemJson)

        store1.saveDocument(doc)

        // Simulate sync: push documents from store1 to store2
        val docs = store1.getDocuments("shopping_items")
        for (d in docs) store2.applyRemoteDocument(d)

        val items2 = store2.getDocuments("shopping_items")
        assertEquals(1, items2.size)
        assertEquals("item-milk", items2[0].key)
        assertTrue(items2[0].data.contains("Milk"))
    }

    @Test
    fun `checked-wins conflict resolution keeps item checked`() = runBlocking {
        val base = System.currentTimeMillis()

        // Node1 has the item as checked=true (older timestamp)
        val local = Document(
            collection = "shopping_items",
            key        = "item-1",
            data       = """{"name":"Eggs","quantity":"6","category":"Dairy","checked":true}""",
            timestamp  = base
        )
        store1.saveDocument(local)

        // Node2 receives a remote update with checked=false but newer timestamp
        val remote = Document(
            collection = "shopping_items",
            key        = "item-1",
            data       = """{"name":"Eggs","quantity":"6","category":"Dairy","checked":false}""",
            timestamp  = base + 1000
        )
        // Apply the remote document — ConflictResolver must keep checked=true
        store1.applyRemoteDocument(remote)

        val result = store1.getDocument("shopping_items", "item-1")
        val checkedInResult = result?.data?.contains("\"checked\":true") ?: false
        assertTrue("checked-wins rule must keep checked=true", checkedInResult)
    }

    @Test
    fun `watchCollection emits updated list after remote sync`() = runBlocking {
        val itemJson = """{"name":"Bread","quantity":"1 loaf","category":"Bakery","checked":false}"""
        val doc = Document.create("shopping_items", "item-bread", itemJson)

        // Observe node2's collection before and after sync
        store2.applyRemoteDocument(doc)

        val items = node2.watchCollection("shopping_items").first()
        assertEquals(1, items.size)
        assertEquals("item-bread", items[0].key)
    }
}
