package com.siroha.gamespace.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.siroha.gamespace.feature.blocker.BlockerScreen
import com.siroha.gamespace.feature.booster.BoosterScreen
import com.siroha.gamespace.feature.devicestatus.DeviceStatusScreen
import com.siroha.gamespace.feature.home.HomeScreen
import com.siroha.gamespace.feature.settings.SettingsScreen
import com.siroha.gamespace.feature.systemaccess.SystemAccessScreen

sealed class Screen(val route: String) {
    data object SystemAccess : Screen("system_access")
    data object Home : Screen("home")
    data object DeviceStatus : Screen("device_status")
    data object Booster : Screen("booster")
    data object Blocker : Screen("blocker")
    data object Settings : Screen("settings")
}

@Composable
fun GameSpaceNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.SystemAccess.route
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.SystemAccess.route) {
            SystemAccessScreen(
                onContinue = {
                    navController.navigate(Screen.Home.route) {
                        // System Access is a one-time gate, not a screen you'd
                        // ever want back-navigation to return to.
                        popUpTo(Screen.SystemAccess.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onOpenDeviceStatus = { navController.navigate(Screen.DeviceStatus.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.DeviceStatus.route) {
            DeviceStatusScreen(
                onOpenSystemAccess = { navController.navigate(Screen.SystemAccess.route) },
                onOpenBooster = { navController.navigate(Screen.Booster.route) },
                onOpenBlocker = { navController.navigate(Screen.Blocker.route) }
            )
        }
        composable(Screen.Booster.route) {
            BoosterScreen()
        }
        composable(Screen.Blocker.route) {
            BlockerScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
