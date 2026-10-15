/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.animation

import androidx.annotation.MainThread
import com.facebook.litho.ComponentScope
import com.facebook.litho.HookGlobalKey
import com.facebook.litho.RenderScope
import com.facebook.litho.Value
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.annotations.Hook
import com.facebook.litho.getUpdatedState
import com.facebook.litho.onCleanup
import com.facebook.litho.state.StateId
import com.facebook.litho.useEffect

/**
 * Imperative wrapper around [AnimatedValue].
 *
 * Unlike [useAnimatedValue], callers drive an [Animatable] explicitly from event handlers and bind
 * callbacks via [animateTo], [snapTo], and [stop].
 */
@ExperimentalLithoApi
class Animatable<T>
internal constructor(
    internal val animatedValue: AnimatedValue<T>,
) {

  /** The current animated value, suitable for passing to `Style.*` APIs. */
  val value: Value<T>
    get() = animatedValue

  /** True if an animation is actively running. */
  val isRunning: Boolean
    get() = animatedValue.isRunning

  /** Starts an animation toward [target] using [spec]. Must be called on the main thread. */
  @MainThread
  fun animateTo(target: T, spec: FiniteAnimationSpec<T>) {
    animatedValue.retarget(target, spec)
    if (animatedValue.hasAnimator) {
      AnimationController.register(animatedValue.frameAnimation)
    }
  }

  /**
   * Immediately sets the value to [target] without animation. Must be called on the main thread.
   */
  @MainThread
  fun snapTo(target: T) {
    animatedValue.snapTo(target)
    AnimationController.unregister(animatedValue.frameAnimation)
  }

  /** Stops the current animation at its present value. Must be called on the main thread. */
  @MainThread
  fun stop() {
    animatedValue.stop()
    AnimationController.unregister(animatedValue.frameAnimation)
  }
}

/**
 * Creates an [Animatable] for imperative animation control.
 *
 * Use this when animation should be triggered directly from a callback instead of being inferred
 * from a target value during render.
 */
@ExperimentalLithoApi
@Hook
fun <T> ComponentScope.useAnimatable(
    initialValue: T,
    typeConverter: TypeConverter<T>,
    label: String = "",
): Animatable<T> {
  val hookKey = animationHookKey()
  val lithoTree = context.lithoTree ?: error("useAnimatable must be called within a LithoTree")
  val stateId = StateId(
      lithoTree.id,
      HookGlobalKey(
          context.globalKey,
          hookKey,
          if (context.isContextForLayout) RenderScope.Layout else RenderScope.Resolve,
      ),
  )

  val animatedValue = getUpdatedState {
    AnimatedValue(
        initialValue = initialValue,
        typeConverter = typeConverter,
        stateId = stateId,
        isLayoutContext = context.isContextForLayout,
        stateUpdater = lithoTree.stateUpdater,
        label = label,
    )
  }

  useEffect(Unit) {
    onCleanup {
      animatedValue.stop()
      AnimationController.unregister(animatedValue.frameAnimation)
    }
  }

  return Animatable(animatedValue)
}
