package com.entgldb.app.models

import java.util.UUID

data class ShoppingListItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val quantity: String = "",
    val category: String = "General",
    val checked: Boolean = false
)
