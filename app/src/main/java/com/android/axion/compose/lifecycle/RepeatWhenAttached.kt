package com.android.axion.compose.lifecycle

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private class ViewOwner : SavedStateRegistryOwner, ViewModelStoreOwner {
    private val registry = LifecycleRegistry(this)
    private val controller = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    fun start() {
        controller.performAttach()
        controller.performRestore(null)
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun destroy() {
        registry.currentState = Lifecycle.State.DESTROYED
        store.clear()
    }
}

/**
 * Pengganti `repeatWhenAttached` dari library sistem: menyiapkan Lifecycle/SavedState/ViewModelStore
 * untuk view (mis. ComposeView di overlay window) selama view menempel ke window.
 */
fun View.repeatWhenAttached(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    block: suspend LifecycleOwner.(View) -> Unit,
): DisposableHandle {
    val view = this
    var owner: ViewOwner? = null
    var job: Job? = null

    val listener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            if (job != null) return
            val o = ViewOwner()
            owner = o
            view.setViewTreeLifecycleOwner(o)
            view.setViewTreeSavedStateRegistryOwner(o)
            view.setViewTreeViewModelStoreOwner(o)
            o.start()
            job = CoroutineScope(Dispatchers.Main.immediate + coroutineContext).launch {
                o.block(view)
            }
        }

        override fun onViewDetachedFromWindow(v: View) {
            job?.cancel()
            job = null
            owner?.destroy()
            owner = null
        }
    }
    addOnAttachStateChangeListener(listener)
    if (isAttachedToWindow) listener.onViewAttachedToWindow(view)

    return DisposableHandle {
        removeOnAttachStateChangeListener(listener)
        job?.cancel()
        job = null
        owner?.destroy()
        owner = null
    }
}
