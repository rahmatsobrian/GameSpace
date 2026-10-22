package com.android.axion.gamespace.compat

/** Pengganti android.window.TaskFpsCallback (hidden/signature). */
abstract class TaskFpsCallback {
    abstract fun onFpsReported(fps: Float)
}
