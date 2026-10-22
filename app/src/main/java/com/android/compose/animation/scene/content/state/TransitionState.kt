package com.android.compose.animation.scene.content.state

import com.android.compose.animation.scene.SceneKey

sealed interface TransitionState {
    class Idle(val currentScene: SceneKey) : TransitionState

    sealed class Transition(val fromScene: SceneKey, val toScene: SceneKey) : TransitionState {
        class ChangeScene(fromScene: SceneKey, toScene: SceneKey, val currentScene: SceneKey) :
            Transition(fromScene, toScene)
    }
}
