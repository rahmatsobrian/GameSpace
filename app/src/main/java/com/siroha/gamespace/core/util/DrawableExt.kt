package com.siroha.gamespace.core.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * App icons come back from PackageManager as a plain [Drawable], which
 * Compose has no direct way to render. Pulling in Coil for this one local
 * (not network) conversion would be a real dependency for a problem this
 * function solves in a dozen lines.
 */
fun Drawable.toImageBitmap(): ImageBitmap {
    if (this is BitmapDrawable && bitmap != null) {
        return bitmap.asImageBitmap()
    }
    val width = intrinsicWidth.coerceAtLeast(1)
    val height = intrinsicHeight.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap.asImageBitmap()
}
