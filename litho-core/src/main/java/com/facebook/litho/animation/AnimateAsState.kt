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
import com.facebook.rendercore.Dimen
import com.facebook.rendercore.graphics.Color

/**
 * Animates a value of type [T], automatically starting a new animation whenever [targetValue]
 * changes.
 *
 * This is the foundation hook for all declarative animations. It creates an [AnimatedValue]
 * internally and drives it via [useEffect] whenever the target or spec changes. The returned
 * [Value] integrates with Litho's state read tracking for automatic phase-skipping.
 *
 * Prefer the typed convenience hooks ([useAnimatedFloat], [useAnimatedInt], [useAnimatedColor],
 * [useAnimatedDimen]) over calling this directly.
 *
 * ```kotlin
 * val alpha = useAnimatedValue(
 *     targetValue = if (visible) 1f else 0f,
 *     typeConverter = FloatTypeConverter,
 * )
 * Style.alpha(alpha)
 * ```
 *
 * @param targetValue The value to animate toward. Changes trigger new animations.
 * @param typeConverter Converts between [T] and [Float] for animation computation.
 * @param spec The animation specification. Defaults to [spring].
 * @param label Optional label for debugging and tracing attribution.
 * @param playback Optional durable pause/resume control.
 * @param onSettled Receives the latest committed target after it settles. Initial composition is
 *   silent; superseded or unmounted generations do not notify. Delivery uses the latest callback.
 * @return A [Value] containing the current animated value.
 */
@ExperimentalLithoApi
@Hook
fun <T> ComponentScope.useAnimatedValue(
    targetValue: T,
    typeConverter: TypeConverter<T>,
    spec: FiniteAnimationSpec<T> = spring(),
    label: String = "",
    playback: AnimationPlayback? = null,
    onSettled: ((T) -> Unit)? = null,
): Value<T> {
  val hookKey = animationHookKey()
  val lithoTree = context.lithoTree ?: error("useAnimatedValue must be called within a LithoTree")
  val stateId = StateId(
      lithoTree.id,
      HookGlobalKey(
          context.globalKey,
          hookKey,
          if (context.isContextForLayout) RenderScope.Layout else RenderScope.Resolve,
      ),
  )

  // Retain the value and its completion owner together across renders and tree-state transfer.
  val (animatedValue, driver, settlement) =
      getUpdatedState {
        val settlement = AnimationSettlement(targetValue)
        createAnimatedValue(
            initialValue = targetValue,
            typeConverter = typeConverter,
            stateId = stateId,
            isLayoutContext = context.isContextForLayout,
            stateUpdater = lithoTree.stateUpdater,
            label = label,
        ) { value, driver ->
          value.onComplete = settlement::complete
          Triple(value, driver, settlement)
        }
      }

  val completionContext = context
  useEffect(targetValue, onSettled, completionContext, playback) {
    driver.bind(playback)
    if (settlement.commit(targetValue, onSettled, completionContext)) {
      animatedValue.snapTo(targetValue)
    }
    null
  }

  useEffect(targetValue, spec) {
    animatedValue.retarget(targetValue, spec, preserveEqualTargetTiming = settlement.isPending)
    if (!animatedValue.hasAnimator) settlement.complete()
    driver.updatePendingWork(animatedValue.hasAnimator)
    null
  }

  // Unregister and stop the animation when the component unmounts.
  useEffect(Unit) {
    onCleanup {
      settlement.unmount()
      animatedValue.stop()
      driver.unmount()
    }
  }

  return animatedValue
}

/**
 * Animates a [Float] value, automatically starting a new animation whenever [targetValue] changes.
 *
 * ```kotlin
 * val alpha = useAnimatedFloat(if (visible) 1f else 0f)
 * Style.alpha(alpha)
 * ```
 *
 * If the target changes while an animation is running, the current animation is interrupted with
 * velocity preservation for smooth retargeting.
 *
 * @param targetValue The value to animate toward. Changes trigger new animations.
 * @param spec The animation specification. Defaults to [spring].
 * @param label Optional label for debugging and tracing attribution.
 * @param playback Optional durable pause/resume control.
 * @return A [Value] containing the current animated value.
 */
@ExperimentalLithoApi
@Hook
fun ComponentScope.useAnimatedFloat(
    targetValue: Float,
    spec: FiniteAnimationSpec<Float> = spring(),
    label: String = "float",
    playback: AnimationPlayback? = null,
    onSettled: ((Float) -> Unit)? = null,
): Value<Float> =
    useAnimatedValue(targetValue, FloatTypeConverter, spec, label, playback, onSettled)

/**
 * Animates an [Int] value, automatically starting a new animation whenever [targetValue] changes.
 *
 * @param targetValue The value to animate toward. Changes trigger new animations.
 * @param spec The animation specification. Defaults to [spring].
 * @param label Optional label for debugging and tracing attribution.
 * @param playback Optional durable pause/resume control.
 * @return A [Value] containing the current animated value.
 */
@ExperimentalLithoApi
@Hook
fun ComponentScope.useAnimatedInt(
    targetValue: Int,
    spec: FiniteAnimationSpec<Int> = spring(),
    label: String = "int",
    playback: AnimationPlayback? = null,
    onSettled: ((Int) -> Unit)? = null,
): Value<Int> = useAnimatedValue(targetValue, IntTypeConverter, spec, label, playback, onSettled)

/**
 * Animates a [Color] value with per-channel interpolation, automatically starting a new animation
 * whenever [targetValue] changes.
 *
 * @param targetValue The color to animate toward. Changes trigger new animations.
 * @param spec The animation specification. Defaults to [spring].
 * @param label Optional label for debugging and tracing attribution.
 * @param playback Optional durable pause/resume control.
 * @return A [Value] containing the current interpolated color.
 */
@ExperimentalLithoApi
@Hook
fun ComponentScope.useAnimatedColor(
    targetValue: Color,
    spec: FiniteAnimationSpec<Color> = spring(),
    label: String = "color",
    playback: AnimationPlayback? = null,
    onSettled: ((Color) -> Unit)? = null,
): Value<Color> =
    useAnimatedValue(targetValue, ColorTypeConverter, spec, label, playback, onSettled)

/**
 * Animates a [Dimen] value, automatically starting a new animation whenever [targetValue] changes.
 *
 * Dimensions are converted to pixel values for animation computation and converted back to [Dimen]
 * using pixel units. The [resourceResolver][com.facebook.litho.ResourcesScope.resourceResolver] is
 * captured at creation time for dp/sp-to-px conversion.
 *
 * @param targetValue The dimension to animate toward. Changes trigger new animations.
 * @param spec The animation specification. Defaults to [spring].
 * @param label Optional label for debugging and tracing attribution.
 * @param playback Optional durable pause/resume control.
 * @return A [Value] containing the current animated dimension.
 */
@ExperimentalLithoApi
@Hook
fun ComponentScope.useAnimatedDimen(
    targetValue: Dimen,
    spec: FiniteAnimationSpec<Dimen> = spring(),
    label: String = "dimen",
    playback: AnimationPlayback? = null,
    onSettled: ((Dimen) -> Unit)? = null,
): Value<Dimen> = useAnimatedValue(
    targetValue,
    DimenTypeConverter(resourceResolver),
    spec,
    label,
    playback,
    onSettled,
)
