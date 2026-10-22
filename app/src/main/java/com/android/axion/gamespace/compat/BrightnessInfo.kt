package com.android.axion.gamespace.compat

/** Pengganti android.hardware.display.BrightnessInfo (hidden). Nilai linear 0..1. */
data class BrightnessInfo(
    val brightness: Float,
    val brightnessMinimum: Float,
    val brightnessMaximum: Float,
)
