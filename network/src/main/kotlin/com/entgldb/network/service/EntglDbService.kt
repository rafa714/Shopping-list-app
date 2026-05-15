package com.entgldb.network.service

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.ktor.server.routing.post
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import io.ktor.server.application.call

class EntglDbService(private val onUpdateReceived: (String) -> Unit) {

    private val server = embeddedServer(Netty, port = 8080) {
        routing {
            post(path= "/update") {
                val data = call.receiveText()
                Log.d("EntglDbService", "Received update: $data")
                onUpdateReceived(data)
                call.respondText("OK")
            }
        }
    }

    suspend fun start() = withContext(Dispatchers.IO) {
        server.start(wait = true)
        Log.d("EntglDbService", "Server started on port 8080")
    }

    fun stop() {
        server.stop(1000, 2000)
        Log.d("EntglDbService", "Server stopped")
    }
}
