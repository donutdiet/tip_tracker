package com.example.tiptracker

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.tiptracker.ui.RootActivity
import com.example.tiptracker.utils.setEdgeToEdgeConfig
import com.example.tiptracker.ui.theme.TipTrackerTheme

class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        setEdgeToEdgeConfig()
        super.onCreate(savedInstanceState)
        setContent {
            TipTrackerTheme {
                RootActivity()
            }
        }
    }
}