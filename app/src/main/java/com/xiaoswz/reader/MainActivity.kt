package com.xiaoswz.reader

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.xiaoswz.reader.data.AppContext
import com.xiaoswz.reader.ui.AppRoot
import com.xiaoswz.reader.ui.reader.VolumeKeyBus
import com.xiaoswz.reader.ui.theme.SurfReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContext.init(application)
        CrashLogger.install(this)
        enableEdgeToEdge()
        setContent {
            SurfReaderTheme {
                AppRoot()
            }
        }
    }

    /**
     * 阅读器在前台且启用音量键翻页时，音量键转发给阅读器消费
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (VolumeKeyBus.readerActive && VolumeKeyBus.pagingEnabled &&
            (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
        ) {
            VolumeKeyBus.events.tryEmit(keyCode)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
