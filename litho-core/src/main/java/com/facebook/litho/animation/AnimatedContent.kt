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

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.layout.ContentPosition
import com.facebook.litho.layout.StackLayout
import com.facebook.litho.onCleanup
import com.facebook.litho.useEffect
import com.facebook.litho.useState
import com.facebook.rendercore.IntSize
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaJustify

/**
 * Describes how an [AnimatedContent] transitions between two states: how the target content enters
 * and how the outgoing content exits, plus an optional size animation.
 *
 * @param targetContentEnter How the new (target) content enters.
 * @param initialContentExit How the previous (outgoing) content exits.
 * @param sizeTransform Optional container size animation between content sizes. (Reserved — the
 *   default is visual-only; layout-affecting size animation is opt-in and gated on benchmarks.)
 */
@ExperimentalLithoApi
class ContentTransform
internal constructor(
    val targetContentEnter: EnterTransition,
    val initialContentExit: ExitTransition,
    val sizeTransform: SizeTransform? = null,
)

/**
 * Produces the [AnimationSpec] for an [AnimatedContent] container size animation between two
 * content sizes. (Reserved for the opt-in, layout-affecting size-morph path.)
 */
@ExperimentalLithoApi
fun interface SizeTransform {
  fun createAnimationSpec(initialSize: IntSize, targetSize: IntSize): AnimationSpec<Float>
}

/**
 * Scope for [AnimatedContent.transitionSpec]. Exposes the [initialState] and [targetState] of the
 * current transition (so the spec can vary by direction, e.g. via [isTransitioningTo] or
 * `slideIntoContainer`) and collects the enter/exit specs that make up the [ContentTransform].
 *
 * The spec lambda declares the transition by setting [enter] / [exit]; anything not set defaults to
 * `None`. Assigning the same property more than once is last-wins, matching the per-effect merge
 * semantics of [EnterTransition.plus].
 */
@ExperimentalLithoApi
class AnimatedContentTransitionScope<T>
internal constructor(
    val initialState: T,
    val targetState: T,
) {
  /** How the target content enters. Last assignment wins; defaults to no enter animation. */
  var enter: EnterTransition = EnterTransition.None

  /** How the outgoing content exits. Last assignment wins; defaults to no exit animation. */
  var exit: ExitTransition = ExitTransition.None

  /** True if this transition is from `this` value to [target]. */
  infix fun T.isTransitioningTo(target: T): Boolean =
      this == initialState && target == this@AnimatedContentTransitionScope.targetState

  @PublishedApi
  internal fun buildContentTransform(): ContentTransform =
      ContentTransform(targetContentEnter = enter, initialContentExit = exit)
}

/**
 * A container that animates between content for different [targetState] values. The target content
 * enters while previously-keyed content exits; both are kept in the container's own subtree (an
 * overlapping [StackLayout]) until each exit settles — the same container-owned retention model as
 * [AnimatedVisibility]. No `DisappearingHost` / rendercore machinery is involved.
 *
 * Content is keyed by [contentKey]; reusing the same key does not animate. The default transition
 * is a cross-fade.
 *
 * ```kotlin
 * child(AnimatedContent(targetState = page) { p -> PageContent(p) })
 * ```
 *
 * Note: the default is **visual-only** (no layout-affecting size animation). `SizeTransform` is
 * part of the API surface but the size-morph path is opt-in and not yet wired (pending perf
 * validation); the container currently sizes to the target content.
 *
 * @param targetState The state whose content should currently be shown.
 * @param transitionSpec Declares the transition via [AnimatedContentTransitionScope.enter] /
 *   [AnimatedContentTransitionScope.exit]. Defaults to a cross-fade.
 * @param contentKey Identity for a state's content; reusing a key does not animate.
 * @param contentPosition Alignment of overlapping content.
 * @param style Style applied to the container.
 * @param label Debug label for the underlying transitions.
 * @param content Builder for the content of a given state.
 */
@ExperimentalLithoApi
class AnimatedContent<T>(
    private val targetState: T,
    private val transitionSpec: AnimatedContentTransitionScope<T>.() -> Unit = {
      enter = fadeIn()
      exit = fadeOut()
    },
    private val contentKey: (T) -> Any? = { it },
    private val contentPosition: ContentPosition = ContentPosition.TopStart,
    private val style: Style = Style,
    private val label: String = "AnimatedContent",
    private val content: ComponentScope.(T) -> Component?,
) : KComponent() {

  override fun ComponentScope.render(): Component {
    // The active slots: states currently rendered, with the target last. Outgoing states are
    // retained here until their exit settles, then removed.
    val activeStates = useState { listOf(targetState) }
    val targetKey = contentKey(targetState)

    // The state the current target is transitioning *from*, exposed to `transitionSpec` as
    // `initialState`. Tracked explicitly so it stays stable for the target's lifetime. Deriving it
    // from `activeStates` is unreliable: as outgoing slots finish exiting, the list shrinks and
    // eventually collapses to just the target, which would make `initialState == targetState` and
    // flip the transition's apparent direction mid-animation.
    val initialState = useState { targetState }

    // The initial content appears without an enter animation (matching Compose); only content shown
    // after the first composition enters. Without this, the initial content would start mid-enter
    // (alpha 0) and, never ticked to Visible, would have a no-op exit and be released instantly.
    val hasStarted = useState { false }
    useEffect(Unit) {
      hasStarted.update(true)
      null
    }

    // On a target change, record the outgoing target as the new initialState and move the target to
    // last (current). Reusing the same key leaves the list untouched, so no new slot and no
    // animation.
    useEffect(targetKey) {
      val current = activeStates.value
      val lastKey = current.lastOrNull()?.let { contentKey(it) }
      if (lastKey != targetKey) {
        current.lastOrNull()?.let { initialState.update(it) }
        activeStates.update { states ->
          states.filterNot { contentKey(it) == targetKey } + targetState
        }
      }
      null
    }

    val states = activeStates.value
    val transform =
        AnimatedContentTransitionScope(initialState.value, targetState)
            .apply(transitionSpec)
            .buildContentTransform()

    // Iterate the active states in reverse so the target (last) is declared FIRST — it stays in
    // normal flow and sizes the container, while the outgoing slots follow as absolute overlays.
    // zIndex is the state's own index (target last → highest), so the target still draws on top;
    // stable per-slot keys preserve hook state across reorders.
    return StackLayout(
        alignItems = contentPosition.toAlignItems(),
        justifyContent = contentPosition.toJustifyContent(),
        style = style,
    ) {
      for (i in states.indices.reversed()) {
        val state = states[i]
        val key = contentKey(state)
        val isTarget = key == targetKey
        child(
            component =
                AnimatedContentSlot(
                    state = state,
                    enter = transform.targetContentEnter,
                    exit = transform.initialContentExit,
                    isTarget = isTarget,
                    animateEnter = hasStarted.value,
                    label = label,
                    onExitSettled = {
                      activeStates.update { current -> current.filterNot { contentKey(it) == key } }
                    },
                    content = content,
                ),
            zIndex = i,
            key = key.toString(),
        )
      }
    }
  }
}

/**
 * One content slot of an [AnimatedContent]. Plays the enter animation when it is the target and the
 * exit animation otherwise, reporting back via [onExitSettled] when its exit settles so the
 * container can drop it. A slot that was never the target (superseded before it was shown, e.g. in
 * a rapid `A -> B -> C`) has no real exit to play and releases immediately.
 */
@ExperimentalLithoApi
internal class AnimatedContentSlot<T>(
    private val state: T,
    private val enter: EnterTransition,
    private val exit: ExitTransition,
    private val isTarget: Boolean,
    private val animateEnter: Boolean,
    private val label: String,
    private val onExitSettled: () -> Unit,
    private val content: ComponentScope.(T) -> Component?,
) : KComponent() {

  override fun ComponentScope.render(): Component? {
    val hasEnterAnimation =
        with(enter.data) { fade != null || scale != null || slide != null || changeSize != null }
    val hasExitAnimation =
        with(exit.data) { fade != null || scale != null || slide != null || changeSize != null }
    val wasTarget = useState { isTarget }
    val started = useState { !hasEnterAnimation || !animateEnter }

    val driveState =
        when {
          !isTarget -> EnterExitState.AfterExit
          started.value -> EnterExitState.Visible
          else -> EnterExitState.BeforeEnter
        }

    val transition = useAnimationGroup(driveState, label = label)
    // Allocate effect slots unconditionally (before the content-null guard) for stable slot count.
    val progresses = buildEnterExitProgresses(transition, enter.data, exit.data)

    // Begin the enter once mounted (BeforeEnter -> Visible) when this slot is the target.
    useEffect(Unit) {
      if (isTarget) {
        started.update(true)
      }
      null
    }

    // Exit/settle handling, idempotent against re-targeting.
    useEffect(isTarget) {
      if (isTarget) {
        transition.onSettled = null
        null
      } else if (!wasTarget.value || !hasExitAnimation) {
        // Never shown, or no exit animation to play (e.g. exit = None): nothing to animate, so
        // release immediately. Without this, an exit with no animated values would never fire
        // `onSettled` (it is driven by AnimatedValue.onComplete), leaving the outgoing content
        // mounted on top of the new content forever.
        onExitSettled()
        null
      } else {
        transition.onSettled = { onExitSettled() }
        onCleanup { transition.onSettled = null }
      }
    }

    val child = content(this, state) ?: return null
    return applyEnterExitContent(progresses, transition, enter.data, exit.data, Style, child)
  }
}

private fun ContentPosition.toAlignItems(): YogaAlign =
    when (this) {
      ContentPosition.TopStart,
      ContentPosition.CenterStart,
      ContentPosition.BottomStart -> YogaAlign.FLEX_START
      ContentPosition.TopCenter,
      ContentPosition.Center,
      ContentPosition.BottomCenter -> YogaAlign.CENTER
      ContentPosition.TopEnd,
      ContentPosition.CenterEnd,
      ContentPosition.BottomEnd -> YogaAlign.FLEX_END
    }

private fun ContentPosition.toJustifyContent(): YogaJustify =
    when (this) {
      ContentPosition.TopStart,
      ContentPosition.TopCenter,
      ContentPosition.TopEnd -> YogaJustify.FLEX_START
      ContentPosition.CenterStart,
      ContentPosition.Center,
      ContentPosition.CenterEnd -> YogaJustify.CENTER
      ContentPosition.BottomStart,
      ContentPosition.BottomCenter,
      ContentPosition.BottomEnd -> YogaJustify.FLEX_END
    }
