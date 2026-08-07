package com.siroha.gamespace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siroha.gamespace.core.navigation.GameSpaceNavHost
import com.siroha.gamespace.core.navigation.Screen
import com.siroha.gamespace.core.theme.GameSpaceTheme
import com.siroha.gamespace.data.settings.DarkModePreference
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate() / setContent — this is what
        // stops a blank white window from flashing before Compose is ready.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GameSpaceApp()
        }
    }
}

@Composable
private fun GameSpaceApp(viewModel: MainViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()

    // settings == null means DataStore's first read hasn't landed yet —
    // rendering nothing for that one frame instead of picking a default
    // is what stops a returning user from briefly seeing the onboarding
    // screen flash before Home takes over.
    val currentSettings = settings ?: return

    val darkTheme = when (currentSettings.darkMode) {
        DarkModePreference.LIGHT -> false
        DarkModePreference.DARK -> true
        DarkModePreference.SYSTEM -> systemDark
    }

    GameSpaceTheme(
        darkTheme = darkTheme,
        dynamicColor = currentSettings.dynamicColorEnabled,
        amoled = currentSettings.amoledEnabled
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            GameSpaceNavHost(
                startDestination = if (currentSettings.onboardingCompleted) {
                    Screen.Home.route
                } else {
                    Screen.SystemAccess.route
                }
            )
        }
    }
}
