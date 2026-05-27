package cn.livestream.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import cn.livestream.app.ui.player.PlayerScreen
import cn.livestream.app.ui.theme.LiveStreamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LiveStreamTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    PlayerScreen()
                }
            }
        }
    }
}