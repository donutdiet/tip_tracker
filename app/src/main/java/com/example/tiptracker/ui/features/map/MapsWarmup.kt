package com.example.tiptracker.ui.features.map

import android.content.Context
import com.google.android.gms.maps.MapsInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The first map in a process loads the Maps SDK's code from Google Play services, which froze
 * the map's opening for ~0.7 s (in release builds too). Starting that load off the main thread
 * while the user is on the Logs list, where the Map button is, has it done by the time they tap.
 */
object MapsWarmup {
    private val started = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Starts loading the Maps SDK, once per process. Safe to call repeatedly. */
    fun start(context: Context) {
        if (!started.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        scope.launch {
            try {
                MapsInitializer.initialize(appContext, MapsInitializer.Renderer.LATEST, null)
            } catch (e: Exception) {
                // Not fatal: the map loads the SDK itself when it opens
                started.set(false)
                android.util.Log.w("MapsWarmup", "Couldn't preload the Maps SDK", e)
            }
        }
    }
}
