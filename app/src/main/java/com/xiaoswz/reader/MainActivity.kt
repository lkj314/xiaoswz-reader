package com.xiaoswz.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.xiaoswz.reader.ui.AppRoot
import com.xiaoswz.reader.ui.theme.SurfReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SurfReaderTheme {
                AppRoot()
            }
        }
    }
}
