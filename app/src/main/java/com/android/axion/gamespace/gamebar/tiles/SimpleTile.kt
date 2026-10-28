package com.android.axion.gamespace.gamebar.tiles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState

/** Tile serbaguna: label/status dibaca lewat lambda, dan UI disegarkan ulang setelah toggle. */
class SimpleTile(
    override val id: String,
    override val icon: Int,
    private val labelProvider: () -> String,
    private val stateProvider: () -> Boolean,
    private val onToggle: () -> Unit,
) : TileAction {
    private val version = mutableIntStateOf(0)

    override val label: String
        get() {
            val v = version.intValue
            return if (v >= 0) labelProvider() else ""
        }

    override val isEnabled: Boolean
        get() {
            val v = version.intValue
            return v >= 0 && stateProvider()
        }

    @Composable
    override fun observeEnabled(): State<Boolean> = rememberUpdatedState(isEnabled)

    override fun toggle() {
        onToggle()
        version.intValue = version.intValue + 1
    }

    fun refresh() {
        version.intValue = version.intValue + 1
    }
}
