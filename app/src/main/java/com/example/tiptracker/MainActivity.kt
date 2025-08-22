package com.example.tiptracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.tiptracker.ui.AppTabsNavHost
import com.example.tiptracker.ui.theme.TipTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TipTrackerTheme {
                AppTabsNavHost()
            }
        }
    }
}