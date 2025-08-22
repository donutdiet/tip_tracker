package com.example.tiptracker

import android.app.Application

// For the AndroidManifest.xml android:name I guess...
class TipTracker : Application() {

    /**
     * AppContainer instance used by the rest of classes to obtain dependencies
     */
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
    }
}