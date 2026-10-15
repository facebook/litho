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

import android.graphics.Rect
import android.view.View
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.Value
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.flexbox.alignSelf
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.onCleanup
import com.facebook.litho.useEffect
import com.facebook.litho.useState
import com.facebook.yoga.YogaAlign
import kotlin.math.abs

/**
 * A container that animates its [content] entering and exiting the render tree.
 *
 * When [visible] transitions `false -> true`, the content is mounted and the [enter] animation
 * plays. When [visible] transitions `true -> false`, the [exit] animation plays and the content is
 * kept mounted (in this container's own subtree) until the animation settles, then unmounted. This
 * is the "container-owned retention" model — the same one Jetpack Compose uses by keeping exiting
 * content in composition. No `DisappearingHost` / rendercore machinery is involved.
 *
 * ```kotlin
 * child(AnimatedVisibility(visible = show) { Text(text = "Hello") })
 *
 * child(
 *     AnimatedVisibility(
 *         visible = expanded,
 *         enter = fadeIn(tween(300.ms)) + slideInVertically { -it },
 *         exit = fadeOut(tween(200.ms)) + slideOutVertically { it },
 *     ) { ExpandedContent() })
 * ```
 *
 * @param visible Whether the content should be visible.
 * @param enter The enter animation. Defaults to [fadeIn].
 * @param exit The exit animation. Defaults to [fadeOut].
 * @param style Style applied to the animation wrapper.
 * @param label Debug label for the underlying transition.
 * @param content Builder for the child component to animate.
 */
@ExperimentalLithoApi
class AnimatedVisibility(
    private val visible: Boolean,
    private val enter: EnterTransition = fadeIn(),
    private val exit: ExitTransition = fadeOut(),
    private val style: Style = Style,
    private val label: String = "AnimatedVisibility",
    private val content: ComponentScope.() -> Component?,
) : KComponent() {

  override fun ComponentScope.render(): Component? {
    val keepMounted = useState { visible }

    // Map the Boolean `visible` prop to the three-state lifecycle:
    // - BeforeEnter: never been visible (keepMounted false, visible false)
    // - Visible: currently visible
    // - AfterExit: was visible, now exiting (keepMounted still true from enter)
    val targetState =
        when {
          visible -> EnterExitState.Visible
          keepMounted.value -> EnterExitState.AfterExit
          else -> EnterExitState.BeforeEnter
        }

    val transition = useAnimationGroup(targetState, label = label)

    // Allocate effect slots unconditionally (before the unmount guard) so the slot count is stable.
    val progresses = buildEnterExitProgresses(transition, enter.data, exit.data)

    val hasExitAnimation =
        with(exit.data) { fade != null || scale != null || slide != null || changeSize != null }

    // Keep-mounted lifecycle:
    // - enter (or re-show mid-exit): keep mounted, drop any pending settle unmount;
    // - exit with no animation: unmount immediately;
    // - exit with animation: retain, and unmount on settle — but only if still hidden (idempotent
    //   against a reverse-mid-exit, where `visible` flips back to true before the group settles).
    useEffect(visible) {
      if (visible) {
        keepMounted.update(true)
        transition.onSettled = null
        null
      } else if (!hasExitAnimation) {
        keepMounted.update(false)
        null
      } else {
        transition.onSettled = { if (!visible) keepMounted.update(false) }
        onCleanup { transition.onSettled = null }
      }
    }

    if (!visible && !keepMounted.value) {
      return null
    }

    val child = content()
    if (child == null) {
      return null
    }
    return applyEnterExitContent(
        progresses,
        transition,
        enter.data,
        exit.data,
        Style.alignSelf(YogaAlign.FLEX_START) + style,
        child,
    )
  }
}

/**
 * The per-effect animated progress values for an enter/exit transition (null when not declared).
 */
@ExperimentalLithoApi
internal class EnterExitProgresses(
    val fade: Value<Float>?,
    val scale: Value<Float>?,
    val slide: Value<Float>?,
    val changeSize: Value<Float>?,
)

/**
 * Allocates the animation slots for the enter/exit effects, driven by [transition].
 *
 * **Must be called unconditionally on every render** (before any early `return null`): the group's
 * slot count/order must be invariant across renders, which `useAnimationGroup` enforces. Building
 * these after an early return (e.g. while unmounted) would change the slot count and trip the
 * stable-snapshot check.
 */
@ExperimentalLithoApi
internal fun buildEnterExitProgresses(
    transition: AnimationGroup<EnterExitState>,
    enterData: TransitionData,
    exitData: TransitionData,
): EnterExitProgresses {
  // Claim all four slots every render, in a fixed order, so the group's count/order is invariant.
  // Each effect is exposed for binding only when actually declared; an absent effect keeps its
  // (no-op snap) slot but is returned as null, so no binder runs for it.
  val fade = createFadeAnimation(transition, enterData.fade, exitData.fade)
  val scale = createScaleAnimation(transition, enterData.scale, exitData.scale)
  val slide = createSlideAnimation(transition, enterData.slide, exitData.slide)
  val changeSize = createChangeSizeAnimation(transition, enterData.changeSize, exitData.changeSize)
  return EnterExitProgresses(
      fade = if (enterData.fade != null || exitData.fade != null) fade else null,
      scale = if (enterData.scale != null || exitData.scale != null) scale else null,
      slide = if (enterData.slide != null || exitData.slide != null) slide else null,
      changeSize =
          if (enterData.changeSize != null || exitData.changeSize != null) changeSize else null,
  )
}

/**
 * Wraps [child] in a [Row] with the [progresses] applied as bind-phase view-property binders.
 * Shared by [AnimatedVisibility] and `AnimatedContent`'s slots. Call after content has been
 * resolved.
 */
@ExperimentalLithoApi
internal fun applyEnterExitContent(
    progresses: EnterExitProgresses,
    transition: AnimationGroup<EnterExitState>,
    enterData: TransitionData,
    exitData: TransitionData,
    baseStyle: Style,
    child: Component,
): Component {
  var style = baseStyle
  progresses.fade?.let { style = style.alpha(it) }
  progresses.scale?.let { scale ->
    val isExiting = transition.targetState == EnterExitState.AfterExit
    val origin =
        if (isExiting) {
          exitData.scale?.transformOrigin
              ?: enterData.scale?.transformOrigin
              ?: TransformOrigin.Center
        } else {
          enterData.scale?.transformOrigin
              ?: exitData.scale?.transformOrigin
              ?: TransformOrigin.Center
        }
    style = style.scaleWithOriginBinder(scale, origin)
  }
  progresses.slide?.let { style = style.slideBinder(it, enterData.slide, exitData.slide) }
  progresses.changeSize?.let {
    style = style.clipChangeSizeBinder(it, enterData.changeSize, exitData.changeSize)
  }
  return Row(style = style) { child(child) }
}

// ── Per-effect animation builders ──
//
// Each maps an EnterExitState to a target value. Fade and scale map each state to a direct value.
// Slide and changeSize use signed encoding (BeforeEnter = -1, Visible = 0, AfterExit = 1) so the
// bind-time binders can tell enter from exit and pick the right offset/anchor.

@ExperimentalLithoApi
private fun createFadeAnimation(
    transition: AnimationGroup<EnterExitState>,
    enterFade: FadeData?,
    exitFade: FadeData?,
): Value<Float> =
    transition.animateFloat(
        label = "fade",
        transitionSpec = {
          when {
            EnterExitState.BeforeEnter.isTransitioningTo(EnterExitState.Visible) ->
                enterFade?.spec ?: defaultSpec
            EnterExitState.AfterExit.isTransitioningTo(EnterExitState.Visible) ->
                enterFade?.spec ?: defaultSpec
            EnterExitState.Visible.isTransitioningTo(EnterExitState.AfterExit) ->
                exitFade?.spec ?: defaultSpec
            else -> defaultSpec
          }
        },
    ) { state ->
      when (state) {
        EnterExitState.BeforeEnter -> enterFade?.alpha ?: 1f
        EnterExitState.Visible -> 1f
        EnterExitState.AfterExit -> exitFade?.alpha ?: 1f
      }
    }

@ExperimentalLithoApi
private fun createScaleAnimation(
    transition: AnimationGroup<EnterExitState>,
    enterScale: ScaleData?,
    exitScale: ScaleData?,
): Value<Float> =
    transition.animateFloat(
        label = "scale",
        transitionSpec = {
          when {
            EnterExitState.BeforeEnter.isTransitioningTo(EnterExitState.Visible) ->
                enterScale?.spec ?: defaultSpec
            EnterExitState.AfterExit.isTransitioningTo(EnterExitState.Visible) ->
                enterScale?.spec ?: defaultSpec
            EnterExitState.Visible.isTransitioningTo(EnterExitState.AfterExit) ->
                exitScale?.spec ?: defaultSpec
            else -> defaultSpec
          }
        },
    ) { state ->
      when (state) {
        EnterExitState.BeforeEnter -> enterScale?.scale ?: 1f
        EnterExitState.Visible -> 1f
        EnterExitState.AfterExit -> exitScale?.scale ?: 1f
      }
    }

@ExperimentalLithoApi
private fun createSlideAnimation(
    transition: AnimationGroup<EnterExitState>,
    enterSlide: SlideData?,
    exitSlide: SlideData?,
): Value<Float> =
    transition.animateFloat(
        label = "slide",
        transitionSpec = {
          when {
            EnterExitState.BeforeEnter.isTransitioningTo(EnterExitState.Visible) ->
                enterSlide?.spec ?: defaultSpec
            EnterExitState.AfterExit.isTransitioningTo(EnterExitState.Visible) ->
                enterSlide?.spec ?: defaultSpec
            EnterExitState.Visible.isTransitioningTo(EnterExitState.AfterExit) ->
                exitSlide?.spec ?: defaultSpec
            else -> defaultSpec
          }
        },
    ) { state ->
      when (state) {
        EnterExitState.BeforeEnter -> -1f
        EnterExitState.Visible -> 0f
        EnterExitState.AfterExit -> 1f
      }
    }

@ExperimentalLithoApi
private fun createChangeSizeAnimation(
    transition: AnimationGroup<EnterExitState>,
    enterChangeSize: ChangeSizeData?,
    exitChangeSize: ChangeSizeData?,
): Value<Float> =
    transition.animateFloat(
        label = "changeSize",
        transitionSpec = {
          when {
            EnterExitState.BeforeEnter.isTransitioningTo(EnterExitState.Visible) ->
                enterChangeSize?.spec ?: defaultSpec
            EnterExitState.AfterExit.isTransitioningTo(EnterExitState.Visible) ->
                enterChangeSize?.spec ?: defaultSpec
            EnterExitState.Visible.isTransitioningTo(EnterExitState.AfterExit) ->
                exitChangeSize?.spec ?: defaultSpec
            else -> defaultSpec
          }
        },
    ) { state ->
      when (state) {
        EnterExitState.BeforeEnter -> -1f
        EnterExitState.Visible -> 0f
        EnterExitState.AfterExit -> 1f
      }
    }

/** Lazily-initialized default spec (a snap) for effect slots that don't specify their own. */
@ExperimentalLithoApi private val defaultSpec: FiniteAnimationSpec<Float> by lazy { snap() }

// ── Bind-time binders (mount-content view-property writes) ──

/** Applies scale + pivot, resolving the pivot from [origin] against the view's measured size. */
@ExperimentalLithoApi
private fun Style.scaleWithOriginBinder(scale: Value<Float>, origin: TransformOrigin): Style =
    onBindViewWithValue(name = { "scale" }, value = scale, defaultValue = 1f) { s: Float ->
      this.pivotX = this.width * origin.pivotFractionX
      this.pivotY = this.height * origin.pivotFractionY
      this.scaleX = s
      this.scaleY = s
    }

/**
 * Applies translation from signed progress. Negative (entering) uses the enter offset; positive
 * (exiting) uses the exit offset. The magnitude is the interpolation fraction.
 */
@ExperimentalLithoApi
private fun Style.slideBinder(
    progress: Value<Float>,
    enterSlide: SlideData?,
    exitSlide: SlideData?,
): Style =
    onBindViewWithValue(name = { "slide" }, value = progress, defaultValue = 0f) { p: Float ->
      val fraction = abs(p)
      if (fraction < 0.001f) {
        this.translationX = 0f
        this.translationY = 0f
      } else {
        // Entering (p < 0) uses the enter offset, exiting (p > 0) the exit offset, each falling
        // back to the other. Resolve per axis so each offset lambda and its basis come from one
        // SlideData: container-relative measures across the parent (container host) bounds;
        // content-relative across the view's own size (defensive fallback to own size).
        val primary = if (p < 0f) enterSlide else exitSlide
        val fallback = if (p < 0f) exitSlide else enterSlide
        val parentView = this.parent as? View
        val sourceX = primary?.takeIf { it.offsetX != null } ?: fallback
        val sourceY = primary?.takeIf { it.offsetY != null } ?: fallback
        val widthBasis =
            if (sourceX?.relativeToContainer == true) (parentView?.width ?: this.width)
            else this.width
        val heightBasis =
            if (sourceY?.relativeToContainer == true) (parentView?.height ?: this.height)
            else this.height
        val offsetX = sourceX?.offsetX?.invoke(widthBasis) ?: 0
        val offsetY = sourceY?.offsetY?.invoke(heightBasis) ?: 0
        this.translationX = offsetX * fraction
        this.translationY = offsetY * fraction
      }
    }

/**
 * Applies a clip-based size animation from signed progress. Negative (entering) uses the enter
 * configuration; positive (exiting) the exit configuration. Visible fraction is `1 - abs(p)`.
 */
@ExperimentalLithoApi
private fun Style.clipChangeSizeBinder(
    progress: Value<Float>,
    enterData: ChangeSizeData?,
    exitData: ChangeSizeData?,
): Style =
    onBindViewWithValue(name = { "changeSize" }, value = progress, defaultValue = 0f) { p: Float ->
      val fraction = 1f - abs(p)
      if (fraction >= 1f) {
        this.clipBounds = null
      } else {
        val data = if (p <= 0f) (enterData ?: exitData!!) else (exitData ?: enterData!!)
        val clipW = if (data.animateWidth) (this.width * fraction).toInt() else this.width
        val clipH = if (data.animateHeight) (this.height * fraction).toInt() else this.height
        val left = ((this.width - clipW) * data.anchor.pivotFractionX).toInt()
        val top = ((this.height - clipH) * data.anchor.pivotFractionY).toInt()
        this.clipBounds = Rect(left, top, left + clipW, top + clipH)
      }
    }
