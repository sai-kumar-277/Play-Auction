package com.playauction.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.playauction.app.ui.navigation.AppNavGraph
import com.playauction.app.ui.theme.DarkBackground
import com.playauction.app.ui.theme.PlayAuctionTheme
import com.playauction.app.util.HapticManager
import com.playauction.app.util.SoundManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var soundManager: SoundManager

    @Inject
    lateinit var hapticManager: HapticManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlayAuctionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    val navController = rememberNavController()
                    AppNavGraph(
                        navController = navController,
                        soundManager = soundManager,
                        hapticManager = hapticManager
                    )
                }
            }
        }
    }
}
