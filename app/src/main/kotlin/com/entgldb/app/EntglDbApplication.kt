package com.entgldb.app

import android.app.Application
import com.entgldb.app.db.ShoppingRepository

class EntglDbApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ShoppingRepository.init(this)
    }
}
