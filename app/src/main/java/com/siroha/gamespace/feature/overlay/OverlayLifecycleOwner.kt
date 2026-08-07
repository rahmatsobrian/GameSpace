package com.siroha.gamespace.feature.overlay

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

/**
 * A ComposeView normally inherits its LifecycleOwner/ViewModelStoreOwner/
 * SavedStateRegistryOwner from the hosting Activity via
 * ViewTreeLifecycleOwner and friends. A Service's overlay window has none
 * of that, so this supplies all three by hand and gets attached via
 * `View.setViewTreeLifecycleOwner(this)` etc. before the ComposeView's
 * content is set — do that attachment before setContent, or Compose has
 * nothing to read state from and won't compose anything.
 *
 * This is the least-certain piece of this whole overlay feature to get
 * exactly right without a compiler in the loop — the individual pieces
 * (LifecycleRegistry, SavedStateRegistryController, the ViewTree*
 * extension setters) are each well-documented on their own, but this
 * specific combination for a Service-hosted overlay hasn't been
 * build-verified here. If Compose renders blank inside the overlay
 * window, this file is the first place to check.
 */
class OverlayLifecycleOwner :
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore = ViewModelStore()

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    fun performRestore(savedState: Bundle?) {
        savedStateRegistryController.performRestore(savedState)
    }

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }
}
