package com.siroha.gamespace.core.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val BrandDarkScheme = darkColorScheme(
    primary = BrandCyan,
    onPrimary = SurfaceDarkBase,
    secondary = BrandCyanDim,
    background = SurfaceDarkBase,
    onBackground = OnSurfaceDark,
    surface = SurfaceDarkElevated,
    onSurface = OnSurfaceDark,
    error = StatusDenied,
)

private val BrandLightScheme = lightColorScheme(
    primary = BrandCyanDim,
    onPrimary = SurfaceLightElevated,
    secondary = BrandCyan,
    background = SurfaceLightBase,
    onBackground = OnSurfaceLight,
    surface = SurfaceLightElevated,
    onSurface = OnSurfaceLight,
    error = StatusDenied,
)

/**
 * @param dynamicColor Material You — wallpaper-derived palette on Android
 *   12+. Below API 31 this parameter has no effect and the branded scheme
 *   is used regardless, which is the "fallback so it still works on older
 *   Android" the spec asks for — there's nothing else to fall back to since
 *   dynamic color simply doesn't exist pre-12.
 * @param amoled True black background instead of the softer default dark
 *   surface. Only meaningful when [darkTheme] is true.
 */
@Composable
fun GameSpaceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    amoled: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    var colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> BrandDarkScheme
        else -> BrandLightScheme
    }

    if (darkTheme && amoled) {
        colorScheme = colorScheme.copy(background = SurfaceDarkAmoled, surface = SurfaceDarkAmoled)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
