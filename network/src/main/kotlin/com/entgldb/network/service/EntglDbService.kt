package com.entgldb.network.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Foreground service stub for EntglDb P2P networking.
 * Full implementation will wire into the Ktor TCP/UDP layer.
 */
class EntglDbService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}
