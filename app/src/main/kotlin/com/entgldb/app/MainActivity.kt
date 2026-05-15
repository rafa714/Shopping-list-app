package com.entgldb.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.entgldb.app.ui.ShoppingListScreen
import com.entgldb.app.ui.theme.ShoppingListTheme
import com.entgldb.network.service.EntglDbService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.util.Log

class MainActivity : ComponentActivity() {
    private lateinit var networkService: EntglDbService
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inizializziamo il server P2P
        networkService = EntglDbService { data ->
            Log.d("P2P_SYNC", "Dati ricevuti: $data")
        }

        // Avviamo il server
        lifecycleScope.launch {
            try {
                networkService.start()
            } catch (e: Exception) {
                Log.e("MainActivity", "Errore nell'avvio del server P2P", e)
            }
        }

        // MOSTRIAAMO L'INTERFACCIA (Essenziale!)
        enableEdgeToEdge()
        setContent {
            ShoppingListTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ShoppingListScreen()
                }
            }
        }
    }
}