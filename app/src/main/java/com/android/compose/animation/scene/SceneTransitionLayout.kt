package com.android.compose.animation.scene

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputChange
import com.android.compose.animation.scene.content.state.TransitionState
import kotlinx.coroutines.CoroutineScope

/*
 * Implementasi sederhana API SceneTransitionLayout dari AOSP (yang tidak tersedia di Maven).
 * Hanya cukup untuk dua scene GameBar: perpindahan memakai crossfade dengan durasi dari
 * definisi `transitions { }`.
 */

class SceneKey(val debugName: String) {
    override fun toString() = "SceneKey($debugName)"
}

class ElementKey(val debugName: String) {
    override fun toString() = "ElementKey($debugName)"
}

interface SwipeDetector {
    fun detectSwipe(change: PointerInputChange): Boolean
}

class InterruptionResult(val animateFrom: SceneKey, val chain: Boolean = true)

interface InterruptionHandler {
    fun onInterruption(
        interrupted: TransitionState.Transition.ChangeScene,
        newTargetScene: SceneKey,
    ): InterruptionResult?
}

class PropertyTransformationBuilder {
    fun fade(element: ElementKey) {}
}

class TransitionBuilder {
    var spec: FiniteAnimationSpec<Float> = tween(durationMillis = 180)
    fun fractionRange(
        start: Float = 0f,
        end: Float = 1f,
        builder: PropertyTransformationBuilder.() -> Unit,
    ) {
        PropertyTransformationBuilder().builder()
    }
}

class SceneTransitions internal constructor(
    internal val specs: Map<Pair<SceneKey, SceneKey>, FiniteAnimationSpec<Float>>,
    val interruptionHandler: InterruptionHandler?,
) {
    internal fun specFor(from: SceneKey, to: SceneKey): FiniteAnimationSpec<Float> =
        specs[from to to] ?: tween(durationMillis = 180)
}

class SceneTransitionsBuilder {
    var interruptionHandler: InterruptionHandler? = null
    internal val specs = mutableMapOf<Pair<SceneKey, SceneKey>, FiniteAnimationSpec<Float>>()

    fun from(from: SceneKey, to: SceneKey, builder: TransitionBuilder.() -> Unit) {
        val b = TransitionBuilder().apply(builder)
        specs[from to to] = b.spec
    }
}

fun transitions(builder: SceneTransitionsBuilder.() -> Unit): SceneTransitions {
    val b = SceneTransitionsBuilder().apply(builder)
    return SceneTransitions(b.specs.toMap(), b.interruptionHandler)
}

@Stable
class MutableSceneTransitionLayoutState internal constructor(
    initialScene: SceneKey,
    val transitions: SceneTransitions,
) {
    var currentScene: SceneKey by mutableStateOf(initialScene)
        internal set
    internal var previousScene: SceneKey = initialScene

    @Suppress("UNUSED_PARAMETER")
    fun setTargetScene(scene: SceneKey, coroutineScope: CoroutineScope) {
        if (scene == currentScene) return
        previousScene = currentScene
        currentScene = scene
    }
}

@Composable
fun rememberMutableSceneTransitionLayoutState(
    initialScene: SceneKey,
    transitions: SceneTransitions,
): MutableSceneTransitionLayoutState =
    remember { MutableSceneTransitionLayoutState(initialScene, transitions) }

interface ContentScope {
    fun Modifier.element(key: ElementKey): Modifier = this
}

private object ContentScopeImpl : ContentScope

class SceneTransitionLayoutScope internal constructor() {
    internal val scenes = mutableMapOf<SceneKey, @Composable ContentScope.() -> Unit>()

    @Suppress("UNUSED_PARAMETER")
    fun scene(key: SceneKey, content: @Composable ContentScope.() -> Unit) {
        scenes[key] = content
    }
}

@Composable
fun SceneTransitionLayout(
    state: MutableSceneTransitionLayoutState,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") swipeDetector: SwipeDetector? = null,
    content: SceneTransitionLayoutScope.() -> Unit,
) {
    val scope = SceneTransitionLayoutScope().apply(content)
    val spec = state.transitions.specFor(state.previousScene, state.currentScene)
    Box(modifier) {
        Crossfade(targetState = state.currentScene, animationSpec = spec, label = "gamebar_scene") { target ->
            val body = scope.scenes[target]
            if (body != null) with(ContentScopeImpl) { body() }
        }
    }
}
