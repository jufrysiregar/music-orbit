package com.musicorbit.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.musicorbit.android.ui.navigation.AppNavGraph
import com.musicorbit.android.ui.theme.MusicOrbitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicOrbitTheme {
                AppNavGraph()
            }
        }
    }
}
