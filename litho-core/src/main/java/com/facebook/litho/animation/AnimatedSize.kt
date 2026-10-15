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

@file:OptIn(ExperimentalLithoApi::class)

package com.facebook.litho.animation

import android.graphics.Rect
import com.facebook.litho.ComponentScope
import com.facebook.litho.Style
import com.facebook.litho.Value
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.annotations.Hook
import com.facebook.litho.view.clipChildren
import com.facebook.rendercore.Dimen
import com.facebook.rendercore.IntSize
import com.facebook.rendercore.px
import kotlin.math.abs
import kotlin.math.roundToInt

/*
 * Internal size-morph helpers shared by the Animated* components: a coupled IntSize animation hook
 * plus the layout/clip Style bindings that consume it. Kept self-contained and `internal` for now so
 * the Animated* components stay focused; the pieces may graduate to public APIs later (see the
 * deferred follow-ups for value-based widthPx/heightPx and a public Value.map).
 */

/**
 * Animates an [IntSize] as one coupled value — width and height share a single animation fraction
 * (see [IntSizeTypeConverter]), so they stay coordinated — restarting whenever [targetValue]
 * changes.
 */
@Hook
internal fun ComponentScope.useAnimatedSize(
    targetValue: IntSize,
    spec: FiniteAnimationSpec<IntSize> = spring(),
    trigger: AnimationTrigger = AnimationTrigger.Immediate,
    label: String = "size",
): Value<IntSize> = useAnimatedValue(targetValue, IntSizeTypeConverter, spec, trigger, label)

/**
 * Drives the host's layout width and height from a single animated [size]. Each axis is read lazily
 * at layout time (via [IntSizeAxisValue], which forwards the underlying animated value straight
 * through), so it stays phase-correct: a per-frame update re-runs only layout, not resolve.
 */
internal fun Style.size(size: Value<IntSize>): Style =
    width(IntSizeAxisValue(size, isWidth = true)).height(IntSizeAxisValue(size, isWidth = false))

/**
 * Clips the host's drawing to its [driver] (animating) size, re-fired each frame as [driver]
 * changes. The clip rect is taken from the driver value — the same animated size that drives the
 * host's layout via [size] — rather than from the mounted `view.width`/`view.height`. The size
 * update (layout phase) and this clip update (bind phase) are separate passes, so the view bounds
 * lag the driver by a frame; reading them would leave the clip a sliver smaller than the settled
 * size and crop content the host has already grown to fit. Pairs `clipChildren` with a per-frame
 * `clipBounds` because `clipChildren` alone is unreliable in Litho (flattened content is drawn into
 * the host rather than mounted as clippable child views), so the `clipBounds` rect catches overflow
 * for both flattened and view-mounted content.
 */
internal fun Style.clipToBounds(driver: Value<IntSize>): Style =
    clipChildren(true).onBindViewWithValue(
        name = { "clipToBounds" },
        value = driver,
        defaultValue = IntSize(0, 0),
    ) { size: IntSize ->
      clipBounds =
          if (size.width > 0 && size.height > 0) Rect(0, 0, size.width, size.height) else null
    }

/**
 * A lazy [Dimen] projection of one axis of an animated [IntSize] — width when [isWidth], else
 * height. Reads [source] on every access (never caches) so the underlying animation's read-tracking
 * and per-frame freshness are preserved when consumed at layout time.
 *
 * Equality is by ([source], [isWidth]) — both stable across renders ([source] is the hook-state
 * animated value, equal by `stateId`) — so a fresh instance allocated each render stays
 * prop-equivalent to the previous one, preserving Litho's layout caching across incidental
 * re-renders. (A general `Value.map` is a deferred follow-up.)
 */
private class IntSizeAxisValue(
    private val source: Value<IntSize>,
    private val isWidth: Boolean,
) : Value<Dimen> {
  override val value: Dimen
    get() = (if (isWidth) source.value.width else source.value.height).px

  override fun equals(other: Any?): Boolean =
      other is IntSizeAxisValue && isWidth == other.isWidth && source == other.source

  override fun hashCode(): Int = 31 * source.hashCode() + isWidth.hashCode()
}

/**
 * A [Value] that scales [natural] on the animated axes by the visible fraction `1 - |progress|`
 * (other axes stay at their natural extent). Lets a reflowing (layout-affecting) expand/shrink ride
 * the same signed `changeSize` progress the clip path uses: read lazily at layout via [Style.size],
 * a per-frame update re-runs only layout.
 *
 * Equality is by all inputs — [progress] is the hook-state animated value (equal by `stateId`) and
 * [natural] is the measured content size — so a fresh instance per render stays prop-equivalent to
 * the previous one, preserving layout caching across incidental re-renders.
 */
internal class ChangeSizeValue(
    private val progress: Value<Float>,
    private val natural: IntSize,
    private val animateWidth: Boolean,
    private val animateHeight: Boolean,
) : Value<IntSize> {
  override val value: IntSize
    get() {
      val fraction = (1f - abs(progress.value)).coerceIn(0f, 1f)
      val width = if (animateWidth) (natural.width * fraction).roundToInt() else natural.width
      val height = if (animateHeight) (natural.height * fraction).roundToInt() else natural.height
      return IntSize(width, height)
    }

  override fun equals(other: Any?): Boolean =
      other is ChangeSizeValue &&
          progress == other.progress &&
          natural == other.natural &&
          animateWidth == other.animateWidth &&
          animateHeight == other.animateHeight

  override fun hashCode(): Int {
    var result = progress.hashCode()
    result = 31 * result + natural.hashCode()
    result = 31 * result + animateWidth.hashCode()
    result = 31 * result + animateHeight.hashCode()
    return result
  }
}

/**
 * [TypeConverter] for [IntSize]: width and height are two independent channels, so a spring drives
 * and settles each axis on its own — two sizes that share a diagonal no longer collide.
 */
internal object IntSizeTypeConverter : TypeConverter<IntSize> {
  override fun newVector(): AnimationVector = AnimationVector2D()

  override fun toVector(value: IntSize, vector: AnimationVector) {
    vector[0] = value.width.toFloat()
    vector[1] = value.height.toFloat()
  }

  override fun fromVector(vector: AnimationVector): IntSize =
      IntSize(vector[0].roundToInt(), vector[1].roundToInt())
}
