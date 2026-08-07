package com.siroha.gamespace.core.theme

import androidx.compose.ui.graphics.Color

// ---- Brand / surfaces ---------------------------------------------------
// Deep charcoal rather than flat #000 as the *default* dark background —
// true AMOLED black is a separate opt-in (see Theme.kt), not the baseline,
// so the app doesn't read as "dark theme by neglect."
val BrandCyan = Color(0xFF5CE1E6)       // primary accent — matches the launcher glyph
val BrandCyanDim = Color(0xFF2E9AA0)    // pressed/disabled state of the accent

val SurfaceDarkBase = Color(0xFF0B0D0E)
val SurfaceDarkElevated = Color(0xFF14171A)
val SurfaceDarkAmoled = Color(0xFF000000)

val SurfaceLightBase = Color(0xFFF7F8F9)
val SurfaceLightElevated = Color(0xFFFFFFFF)

val OnSurfaceDark = Color(0xFFE7EAEB)
val OnSurfaceLight = Color(0xFF14171A)

// ---- Semantic metric colors ----------------------------------------------
// Used by the monitoring dashboard (floating assistant + Home stats) so a
// number's *color family* tells you what it is at a glance, independent of
// its label. Not used anywhere yet in this foundation slice — defined now
// because it's a systemic design decision, not a per-screen one.
val MetricCpu = Color(0xFFFF8A65)
val MetricGpu = Color(0xFFB388FF)
val MetricRam = Color(0xFF4FC3F7)
val MetricNetwork = Color(0xFF81C784)
val MetricBattery = Color(0xFFFFD54F)

// Thermal is a severity gradient, not one fixed hue — normal/warm/hot.
val ThermalNormal = Color(0xFF4FC3F7)
val ThermalWarm = Color(0xFFFFD54F)
val ThermalHot = Color(0xFFE57373)

// ---- Status colors (privilege states, pass/fail-style indicators) --------
val StatusGranted = Color(0xFF81C784)
val StatusWarning = Color(0xFFFFD54F)
val StatusDenied = Color(0xFFE57373)
val StatusNeutral = Color(0xFF8A9296)
