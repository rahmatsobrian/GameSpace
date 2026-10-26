package com.android.axion.gamespace.gamebar.tiles

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import com.android.axion.gamespace.R
import com.android.axion.gamespace.compat.RefreshRate

/** Tile siklus kunci refresh rate: Auto -> 60 -> 90 -> 120 ... sesuai mode yang didukung layar. */
class RefreshRateTile(private val context: Context) : TileAction {
    override val id: String = "refresh_rate"
    override val icon: Int = R.drawable.materialsymbols_ic_speed_rounded_filled

    private val rates: List<Int> = listOf(0) + RefreshRate.supported(context).filter { it >= 60 }

    init {
        RefreshRate.sync(context)
    }

    override val label: String
        get() {
            val hz = RefreshRate.locked.intValue
            return if (hz == 0) context.getString(R.string.tile_refresh_rate) else "$hz Hz"
        }

    override val isEnabled: Boolean get() = RefreshRate.locked.intValue != 0

    @Composable
    override fun observeEnabled(): State<Boolean> = rememberUpdatedState(RefreshRate.locked.intValue != 0)

    override fun toggle() {
        val idx = rates.indexOf(RefreshRate.locked.intValue).let { if (it < 0) 0 else it }
        RefreshRate.apply(context, rates[(idx + 1) % rates.size])
    }
}
