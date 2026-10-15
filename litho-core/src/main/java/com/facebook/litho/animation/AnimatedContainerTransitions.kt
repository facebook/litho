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

import com.facebook.litho.annotations.ExperimentalLithoApi
import kotlin.time.Duration

/**
 * The visibility lifecycle states for enter/exit transitions, shared by the animated container
 * APIs.
 *
 * Using three states instead of a Boolean lets enter and exit specify independent "invisible"
 * values for the same property (e.g. `fadeIn(initialAlpha = 0.2f)` + `fadeOut(targetAlpha =
 * 0.5f)`): [BeforeEnter] and [AfterExit] occupy separate slots in the per-state value mapping, so
 * they never conflict. Modeled after Compose's `EnterExitState`.
 */
@ExperimentalLithoApi
enum class EnterExitState {
  /** Content is about to enter — not yet visible. */
  BeforeEnter,

  /** Content is fully visible. */
  Visible,

  /** Content has exited — no longer visible. */
  AfterExit,
}

/**
 * Describes the origin point for scale and clip transforms as fractional coordinates within the
 * component's bounds.
 *
 * @param pivotFractionX Horizontal position as a fraction of width (0 = start, 1 = end).
 * @param pivotFractionY Vertical position as a fraction of height (0 = top, 1 = bottom).
 */
@ExperimentalLithoApi
data class TransformOrigin(
    val pivotFractionX: Float,
    val pivotFractionY: Float,
) {
  companion object {
    val Center: TransformOrigin = TransformOrigin(0.5f, 0.5f)
    val TopStart: TransformOrigin = TransformOrigin(0f, 0f)
    val TopCenter: TransformOrigin = TransformOrigin(0.5f, 0f)
    val TopEnd: TransformOrigin = TransformOrigin(1f, 0f)
    val CenterStart: TransformOrigin = TransformOrigin(0f, 0.5f)
    val CenterEnd: TransformOrigin = TransformOrigin(1f, 0.5f)
    val BottomStart: TransformOrigin = TransformOrigin(0f, 1f)
    val BottomCenter: TransformOrigin = TransformOrigin(0.5f, 1f)
    val BottomEnd: TransformOrigin = TransformOrigin(1f, 1f)
  }
}

// ── Internal animation data descriptors ──

@ExperimentalLithoApi
internal data class FadeData(
    val alpha: Float,
    val spec: FiniteAnimationSpec<Float>,
)

@ExperimentalLithoApi
internal data class ScaleData(
    val scale: Float,
    val transformOrigin: TransformOrigin,
    val spec: FiniteAnimationSpec<Float>,
)

@ExperimentalLithoApi
internal data class SlideData(
    val offsetX: ((fullWidth: Int) -> Int)?,
    val offsetY: ((fullHeight: Int) -> Int)?,
    val spec: FiniteAnimationSpec<Float>,
    /**
     * When true, the offset lambdas receive the enclosing **container's** extent instead of the
     * content's own — used by `slideIntoContainer`/`slideOutOfContainer`, where the slide is
     * measured across the container's bounds.
     */
    val relativeToContainer: Boolean = false,
)

@ExperimentalLithoApi
internal data class ChangeSizeData(
    val anchor: TransformOrigin,
    val animateWidth: Boolean,
    val animateHeight: Boolean,
    val spec: FiniteAnimationSpec<Float>,
)

/**
 * Composite animation descriptor with nullable per-effect fields. The [plus] operator merges two
 * descriptors with "last wins" semantics.
 */
@ExperimentalLithoApi
@PublishedApi
internal data class TransitionData(
    val fade: FadeData? = null,
    val scale: ScaleData? = null,
    val slide: SlideData? = null,
    val changeSize: ChangeSizeData? = null,
) {
  operator fun plus(other: TransitionData): TransitionData = TransitionData(
      fade = other.fade ?: fade,
      scale = other.scale ?: scale,
      slide = other.slide ?: slide,
      changeSize = other.changeSize ?: changeSize,
  )
}

// ── EnterTransition / ExitTransition ──

/**
 * Describes how content enters the render tree. Compose multiple effects with [plus]:
 * ```kotlin
 * fadeIn(tween(300.ms)) + slideInVertically { -it }
 * ```
 */
@ExperimentalLithoApi
class EnterTransition
@PublishedApi
internal constructor(
    @PublishedApi internal val data: TransitionData,
) {
  /** Merges two enter transitions. When both define the same effect, the right-hand side wins. */
  operator fun plus(other: EnterTransition): EnterTransition = EnterTransition(data + other.data)

  companion object {
    /** No enter animation — content appears immediately. */
    val None: EnterTransition = EnterTransition(TransitionData())
  }
}

/**
 * Describes how content exits the render tree. Compose multiple effects with [plus]:
 * ```kotlin
 * fadeOut(tween(200.ms)) + slideOutVertically { it }
 * ```
 */
@ExperimentalLithoApi
class ExitTransition
@PublishedApi
internal constructor(
    @PublishedApi internal val data: TransitionData,
) {
  /** Merges two exit transitions. When both define the same effect, the right-hand side wins. */
  operator fun plus(other: ExitTransition): ExitTransition = ExitTransition(data + other.data)

  companion object {
    /** No exit animation — content is removed immediately. */
    val None: ExitTransition = ExitTransition(TransitionData())
  }
}

// ── Factory functions ──

// ── Fade ──

/**
 * Fades content in from [initialAlpha] to fully opaque.
 *
 * @param spec The animation spec for the fade. Defaults to [spring].
 * @param initialAlpha The starting alpha value (0 = transparent, 1 = opaque).
 */
@ExperimentalLithoApi
fun fadeIn(
    spec: FiniteAnimationSpec<Float> = spring(),
    initialAlpha: Float = 0f,
): EnterTransition =
    EnterTransition(TransitionData(fade = FadeData(alpha = initialAlpha, spec = spec)))

/**
 * Fades content in over [duration] — a shortcut for `fadeIn(tween(duration), initialAlpha)`.
 *
 * @param duration How long the fade runs.
 * @param initialAlpha The starting alpha value (0 = transparent, 1 = opaque).
 */
@ExperimentalLithoApi
fun fadeIn(duration: Duration, initialAlpha: Float = 0f): EnterTransition =
    fadeIn(spec = tween(duration), initialAlpha = initialAlpha)

/**
 * Fades content out from fully opaque to [targetAlpha].
 *
 * @param spec The animation spec for the fade. Defaults to [spring].
 * @param targetAlpha The ending alpha value (0 = transparent, 1 = opaque).
 */
@ExperimentalLithoApi
fun fadeOut(
    spec: FiniteAnimationSpec<Float> = spring(),
    targetAlpha: Float = 0f,
): ExitTransition =
    ExitTransition(TransitionData(fade = FadeData(alpha = targetAlpha, spec = spec)))

/**
 * Fades content out over [duration] — a shortcut for `fadeOut(tween(duration), targetAlpha)`.
 *
 * @param duration How long the fade runs.
 * @param targetAlpha The ending alpha value (0 = transparent, 1 = opaque).
 */
@ExperimentalLithoApi
fun fadeOut(duration: Duration, targetAlpha: Float = 0f): ExitTransition =
    fadeOut(spec = tween(duration), targetAlpha = targetAlpha)

// ── Scale ──

/**
 * Scales content in from [initialScale] to full size.
 *
 * @param spec The animation spec for the scale. Defaults to [spring].
 * @param initialScale The starting scale factor (0 = invisible, 1 = full size).
 * @param transformOrigin The pivot point for the scale transform.
 */
@ExperimentalLithoApi
fun scaleIn(
    spec: FiniteAnimationSpec<Float> = spring(),
    initialScale: Float = 0f,
    transformOrigin: TransformOrigin = TransformOrigin.Center,
): EnterTransition = EnterTransition(
    TransitionData(
        scale = ScaleData(scale = initialScale, transformOrigin = transformOrigin, spec = spec),
    ),
)

/**
 * Scales content in over [duration] — a shortcut for `scaleIn(tween(duration), initialScale,
 * transformOrigin)`.
 *
 * @param duration How long the scale runs.
 * @param initialScale The starting scale factor (0 = invisible, 1 = full size).
 * @param transformOrigin The pivot point for the scale transform.
 */
@ExperimentalLithoApi
fun scaleIn(
    duration: Duration,
    initialScale: Float = 0f,
    transformOrigin: TransformOrigin = TransformOrigin.Center,
): EnterTransition =
    scaleIn(spec = tween(duration), initialScale = initialScale, transformOrigin = transformOrigin)

/**
 * Scales content out from full size to [targetScale].
 *
 * @param spec The animation spec for the scale. Defaults to [spring].
 * @param targetScale The ending scale factor (0 = invisible, 1 = full size).
 * @param transformOrigin The pivot point for the scale transform.
 */
@ExperimentalLithoApi
fun scaleOut(
    spec: FiniteAnimationSpec<Float> = spring(),
    targetScale: Float = 0f,
    transformOrigin: TransformOrigin = TransformOrigin.Center,
): ExitTransition = ExitTransition(
    TransitionData(
        scale = ScaleData(scale = targetScale, transformOrigin = transformOrigin, spec = spec),
    ),
)

/**
 * Scales content out over [duration] — a shortcut for `scaleOut(tween(duration), targetScale,
 * transformOrigin)`.
 *
 * @param duration How long the scale runs.
 * @param targetScale The ending scale factor (0 = invisible, 1 = full size).
 * @param transformOrigin The pivot point for the scale transform.
 */
@ExperimentalLithoApi
fun scaleOut(
    duration: Duration,
    targetScale: Float = 0f,
    transformOrigin: TransformOrigin = TransformOrigin.Center,
): ExitTransition =
    scaleOut(spec = tween(duration), targetScale = targetScale, transformOrigin = transformOrigin)

// ── Slide ──

/**
 * Slides content in vertically. The [initialOffsetY] lambda receives the full height of the
 * component and returns the pixel offset to start from.
 *
 * @param spec The animation spec for the slide. Defaults to [spring].
 * @param initialOffsetY Returns the initial Y offset in pixels. Defaults to sliding down from above
 *   (`-fullHeight`).
 */
@ExperimentalLithoApi
fun slideInVertically(
    spec: FiniteAnimationSpec<Float> = spring(),
    initialOffsetY: (fullHeight: Int) -> Int = { -it },
): EnterTransition = EnterTransition(
    TransitionData(slide = SlideData(offsetX = null, offsetY = initialOffsetY, spec = spec)),
)

/**
 * Slides content in vertically over [duration] — a shortcut for `slideInVertically(tween(duration),
 * initialOffsetY)`.
 *
 * @param duration How long the slide runs.
 * @param initialOffsetY Returns the initial Y offset in pixels. Defaults to sliding down from above
 *   (`-fullHeight`).
 */
@ExperimentalLithoApi
fun slideInVertically(
    duration: Duration,
    initialOffsetY: (fullHeight: Int) -> Int = { -it },
): EnterTransition = slideInVertically(spec = tween(duration), initialOffsetY = initialOffsetY)

/**
 * Slides content out vertically. The [targetOffsetY] lambda receives the full height of the
 * component and returns the pixel offset to end at.
 *
 * @param spec The animation spec for the slide. Defaults to [spring].
 * @param targetOffsetY Returns the target Y offset in pixels. Defaults to sliding up above
 *   (`-fullHeight`).
 */
@ExperimentalLithoApi
fun slideOutVertically(
    spec: FiniteAnimationSpec<Float> = spring(),
    targetOffsetY: (fullHeight: Int) -> Int = { -it },
): ExitTransition = ExitTransition(
    TransitionData(slide = SlideData(offsetX = null, offsetY = targetOffsetY, spec = spec)),
)

/**
 * Slides content out vertically over [duration] — a shortcut for
 * `slideOutVertically(tween(duration), targetOffsetY)`.
 *
 * @param duration How long the slide runs.
 * @param targetOffsetY Returns the target Y offset in pixels. Defaults to sliding up above
 *   (`-fullHeight`).
 */
@ExperimentalLithoApi
fun slideOutVertically(
    duration: Duration,
    targetOffsetY: (fullHeight: Int) -> Int = { -it },
): ExitTransition = slideOutVertically(spec = tween(duration), targetOffsetY = targetOffsetY)

/**
 * Slides content in horizontally. The [initialOffsetX] lambda receives the full width of the
 * component and returns the pixel offset to start from.
 *
 * @param spec The animation spec for the slide. Defaults to [spring].
 * @param initialOffsetX Returns the initial X offset in pixels. Defaults to sliding in from the
 *   start (`-fullWidth`).
 */
@ExperimentalLithoApi
fun slideInHorizontally(
    spec: FiniteAnimationSpec<Float> = spring(),
    initialOffsetX: (fullWidth: Int) -> Int = { -it },
): EnterTransition = EnterTransition(
    TransitionData(slide = SlideData(offsetX = initialOffsetX, offsetY = null, spec = spec)),
)

/**
 * Slides content in horizontally over [duration] — a shortcut for
 * `slideInHorizontally(tween(duration), initialOffsetX)`.
 *
 * @param duration How long the slide runs.
 * @param initialOffsetX Returns the initial X offset in pixels. Defaults to sliding in from the
 *   start (`-fullWidth`).
 */
@ExperimentalLithoApi
fun slideInHorizontally(
    duration: Duration,
    initialOffsetX: (fullWidth: Int) -> Int = { -it },
): EnterTransition = slideInHorizontally(spec = tween(duration), initialOffsetX = initialOffsetX)

/**
 * Slides content out horizontally. The [targetOffsetX] lambda receives the full width of the
 * component and returns the pixel offset to end at.
 *
 * @param spec The animation spec for the slide. Defaults to [spring].
 * @param targetOffsetX Returns the target X offset in pixels. Defaults to sliding out to the start
 *   (`-fullWidth`).
 */
@ExperimentalLithoApi
fun slideOutHorizontally(
    spec: FiniteAnimationSpec<Float> = spring(),
    targetOffsetX: (fullWidth: Int) -> Int = { -it },
): ExitTransition = ExitTransition(
    TransitionData(slide = SlideData(offsetX = targetOffsetX, offsetY = null, spec = spec)),
)

/**
 * Slides content out horizontally over [duration] — a shortcut for
 * `slideOutHorizontally(tween(duration), targetOffsetX)`.
 *
 * @param duration How long the slide runs.
 * @param targetOffsetX Returns the target X offset in pixels. Defaults to sliding out to the start
 *   (`-fullWidth`).
 */
@ExperimentalLithoApi
fun slideOutHorizontally(
    duration: Duration,
    targetOffsetX: (fullWidth: Int) -> Int = { -it },
): ExitTransition = slideOutHorizontally(spec = tween(duration), targetOffsetX = targetOffsetX)

// ── Container-relative slide (AnimatedContent) ──

/** The direction content moves during a container-relative slide. */
@ExperimentalLithoApi
enum class SlideDirection {
  Left,
  Right,
  Up,
  Down,
}

/**
 * Slides the target content into the container, entering from the edge opposite [towards] and
 * moving in the [towards] direction to its resting position. Unlike [slideInHorizontally] /
 * [slideInVertically] (which offset by the content's own size), the slide is measured across the
 * **container's** bounds. Only meaningful inside [AnimatedContent]'s `transitionSpec`.
 *
 * @param towards The direction the entering content moves.
 * @param spec The animation spec for the slide. Defaults to [spring].
 * @param initialOffset Maps the container's full-slide extent in pixels to the initial offset.
 *   Defaults to a full slide (`{ it }`).
 */
@ExperimentalLithoApi
fun <T> AnimatedContentTransitionScope<T>.slideIntoContainer(
    towards: SlideDirection,
    spec: FiniteAnimationSpec<Float> = spring(),
    initialOffset: (offsetForFullSlide: Int) -> Int = { it },
): EnterTransition = EnterTransition(
    TransitionData(slide = containerSlide(towards, spec, initialOffset, entering = true)),
)

/**
 * Slides the target content into the container over [duration] — a shortcut for
 * `slideIntoContainer(towards, tween(duration), initialOffset)`.
 *
 * @param towards The direction the entering content moves.
 * @param duration How long the slide runs.
 * @param initialOffset Maps the container's full-slide extent in pixels to the initial offset.
 *   Defaults to a full slide (`{ it }`).
 */
@ExperimentalLithoApi
fun <T> AnimatedContentTransitionScope<T>.slideIntoContainer(
    towards: SlideDirection,
    duration: Duration,
    initialOffset: (offsetForFullSlide: Int) -> Int = { it },
): EnterTransition =
    slideIntoContainer(towards, spec = tween(duration), initialOffset = initialOffset)

/**
 * Slides the outgoing content out of the container in the [towards] direction. The slide is
 * measured across the **container's** bounds (see [slideIntoContainer]). Only meaningful inside
 * [AnimatedContent]'s `transitionSpec`.
 *
 * @param towards The direction the exiting content moves.
 * @param spec The animation spec for the slide. Defaults to [spring].
 * @param targetOffset Maps the container's full-slide extent in pixels to the target offset.
 *   Defaults to a full slide (`{ it }`).
 */
@ExperimentalLithoApi
fun <T> AnimatedContentTransitionScope<T>.slideOutOfContainer(
    towards: SlideDirection,
    spec: FiniteAnimationSpec<Float> = spring(),
    targetOffset: (offsetForFullSlide: Int) -> Int = { it },
): ExitTransition = ExitTransition(
    TransitionData(slide = containerSlide(towards, spec, targetOffset, entering = false)),
)

/**
 * Slides the outgoing content out of the container over [duration] — a shortcut for
 * `slideOutOfContainer(towards, tween(duration), targetOffset)`.
 *
 * @param towards The direction the exiting content moves.
 * @param duration How long the slide runs.
 * @param targetOffset Maps the container's full-slide extent in pixels to the target offset.
 *   Defaults to a full slide (`{ it }`).
 */
@ExperimentalLithoApi
fun <T> AnimatedContentTransitionScope<T>.slideOutOfContainer(
    towards: SlideDirection,
    duration: Duration,
    targetOffset: (offsetForFullSlide: Int) -> Int = { it },
): ExitTransition =
    slideOutOfContainer(towards, spec = tween(duration), targetOffset = targetOffset)

/**
 * Builds a container-relative [SlideData]. An entering slide starts on the edge opposite [towards]
 * (so it travels toward [towards]); an exiting slide ends on the [towards] edge — so entering and
 * exiting use opposite signs for the same direction. Magnitude is the caller's [offset] applied to
 * the container extent, resolved at bind time (see `Style.slideBinder`).
 */
@ExperimentalLithoApi
private fun containerSlide(
    towards: SlideDirection,
    spec: FiniteAnimationSpec<Float>,
    offset: (Int) -> Int,
    entering: Boolean,
): SlideData {
  val sign = if (entering) 1 else -1
  return when (towards) {
    SlideDirection.Left ->
        SlideData({ full -> sign * offset(full) }, null, spec, relativeToContainer = true)
    SlideDirection.Right ->
        SlideData({ full -> -sign * offset(full) }, null, spec, relativeToContainer = true)
    SlideDirection.Up ->
        SlideData(null, { full -> sign * offset(full) }, spec, relativeToContainer = true)
    SlideDirection.Down ->
        SlideData(null, { full -> -sign * offset(full) }, spec, relativeToContainer = true)
  }
}

// ── Expand/Shrink (clip-based) ──

/**
 * Expands content vertically by animating a clip rect from zero height to full height.
 *
 * @param spec The animation spec for the expansion. Defaults to [spring].
 * @param expandFrom The anchor point that stays fixed during expansion.
 */
@ExperimentalLithoApi
fun expandVertically(
    spec: FiniteAnimationSpec<Float> = spring(),
    expandFrom: TransformOrigin = TransformOrigin.TopCenter,
): EnterTransition = EnterTransition(
    TransitionData(
        changeSize =
            ChangeSizeData(
                anchor = expandFrom,
                animateWidth = false,
                animateHeight = true,
                spec = spec,
            ),
    ),
)

/**
 * Expands content vertically over [duration] — a shortcut for `expandVertically(tween(duration),
 * expandFrom)`.
 *
 * @param duration How long the expansion runs.
 * @param expandFrom The anchor point that stays fixed during expansion.
 */
@ExperimentalLithoApi
fun expandVertically(
    duration: Duration,
    expandFrom: TransformOrigin = TransformOrigin.TopCenter,
): EnterTransition = expandVertically(spec = tween(duration), expandFrom = expandFrom)

/**
 * Shrinks content vertically by animating a clip rect from full height to zero height.
 *
 * @param spec The animation spec for the shrink. Defaults to [spring].
 * @param shrinkTowards The anchor point that stays fixed during shrink.
 */
@ExperimentalLithoApi
fun shrinkVertically(
    spec: FiniteAnimationSpec<Float> = spring(),
    shrinkTowards: TransformOrigin = TransformOrigin.TopCenter,
): ExitTransition = ExitTransition(
    TransitionData(
        changeSize =
            ChangeSizeData(
                anchor = shrinkTowards,
                animateWidth = false,
                animateHeight = true,
                spec = spec,
            ),
    ),
)

/**
 * Shrinks content vertically over [duration] — a shortcut for `shrinkVertically(tween(duration),
 * shrinkTowards)`.
 *
 * @param duration How long the shrink runs.
 * @param shrinkTowards The anchor point that stays fixed during shrink.
 */
@ExperimentalLithoApi
fun shrinkVertically(
    duration: Duration,
    shrinkTowards: TransformOrigin = TransformOrigin.TopCenter,
): ExitTransition = shrinkVertically(spec = tween(duration), shrinkTowards = shrinkTowards)

/**
 * Expands content horizontally by animating a clip rect from zero width to full width.
 *
 * @param spec The animation spec for the expansion. Defaults to [spring].
 * @param expandFrom The anchor point that stays fixed during expansion.
 */
@ExperimentalLithoApi
fun expandHorizontally(
    spec: FiniteAnimationSpec<Float> = spring(),
    expandFrom: TransformOrigin = TransformOrigin.CenterStart,
): EnterTransition = EnterTransition(
    TransitionData(
        changeSize =
            ChangeSizeData(
                anchor = expandFrom,
                animateWidth = true,
                animateHeight = false,
                spec = spec,
            ),
    ),
)

/**
 * Expands content horizontally over [duration] — a shortcut for
 * `expandHorizontally(tween(duration), expandFrom)`.
 *
 * @param duration How long the expansion runs.
 * @param expandFrom The anchor point that stays fixed during expansion.
 */
@ExperimentalLithoApi
fun expandHorizontally(
    duration: Duration,
    expandFrom: TransformOrigin = TransformOrigin.CenterStart,
): EnterTransition = expandHorizontally(spec = tween(duration), expandFrom = expandFrom)

/**
 * Shrinks content horizontally by animating a clip rect from full width to zero width.
 *
 * @param spec The animation spec for the shrink. Defaults to [spring].
 * @param shrinkTowards The anchor point that stays fixed during shrink.
 */
@ExperimentalLithoApi
fun shrinkHorizontally(
    spec: FiniteAnimationSpec<Float> = spring(),
    shrinkTowards: TransformOrigin = TransformOrigin.CenterStart,
): ExitTransition = ExitTransition(
    TransitionData(
        changeSize =
            ChangeSizeData(
                anchor = shrinkTowards,
                animateWidth = true,
                animateHeight = false,
                spec = spec,
            ),
    ),
)

/**
 * Shrinks content horizontally over [duration] — a shortcut for
 * `shrinkHorizontally(tween(duration), shrinkTowards)`.
 *
 * @param duration How long the shrink runs.
 * @param shrinkTowards The anchor point that stays fixed during shrink.
 */
@ExperimentalLithoApi
fun shrinkHorizontally(
    duration: Duration,
    shrinkTowards: TransformOrigin = TransformOrigin.CenterStart,
): ExitTransition = shrinkHorizontally(spec = tween(duration), shrinkTowards = shrinkTowards)

/**
 * Expands content in both directions by animating a clip rect from zero size to full size.
 *
 * @param spec The animation spec for the expansion. Defaults to [spring].
 * @param expandFrom The anchor point that stays fixed during expansion.
 */
@ExperimentalLithoApi
fun expandIn(
    spec: FiniteAnimationSpec<Float> = spring(),
    expandFrom: TransformOrigin = TransformOrigin.TopStart,
): EnterTransition = EnterTransition(
    TransitionData(
        changeSize =
            ChangeSizeData(
                anchor = expandFrom,
                animateWidth = true,
                animateHeight = true,
                spec = spec,
            ),
    ),
)

/**
 * Expands content in both directions over [duration] — a shortcut for `expandIn(tween(duration),
 * expandFrom)`.
 *
 * @param duration How long the expansion runs.
 * @param expandFrom The anchor point that stays fixed during expansion.
 */
@ExperimentalLithoApi
fun expandIn(
    duration: Duration,
    expandFrom: TransformOrigin = TransformOrigin.TopStart,
): EnterTransition = expandIn(spec = tween(duration), expandFrom = expandFrom)

/**
 * Shrinks content out in both directions by animating a clip rect from full size to zero size.
 *
 * @param spec The animation spec for the shrink. Defaults to [spring].
 * @param shrinkTowards The anchor point that stays fixed during shrink.
 */
@ExperimentalLithoApi
fun shrinkOut(
    spec: FiniteAnimationSpec<Float> = spring(),
    shrinkTowards: TransformOrigin = TransformOrigin.TopStart,
): ExitTransition = ExitTransition(
    TransitionData(
        changeSize =
            ChangeSizeData(
                anchor = shrinkTowards,
                animateWidth = true,
                animateHeight = true,
                spec = spec,
            ),
    ),
)

/**
 * Shrinks content out in both directions over [duration] — a shortcut for
 * `shrinkOut(tween(duration), shrinkTowards)`.
 *
 * @param duration How long the shrink runs.
 * @param shrinkTowards The anchor point that stays fixed during shrink.
 */
@ExperimentalLithoApi
fun shrinkOut(
    duration: Duration,
    shrinkTowards: TransformOrigin = TransformOrigin.TopStart,
): ExitTransition = shrinkOut(spec = tween(duration), shrinkTowards = shrinkTowards)
