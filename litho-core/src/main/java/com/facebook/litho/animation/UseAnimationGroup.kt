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
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.annotations.Hook
import com.facebook.litho.getUpdatedState
import com.facebook.litho.onCleanup
import com.facebook.litho.useEffect

/**
 * Coordinates multiple animated properties that respond to a shared [targetState].
 *
 * This is the Litho equivalent of Compose's `updateTransition`. The returned [AnimationGroup]
 * provides [animateFloat][AnimationGroup.animateFloat], [animateInt][AnimationGroup.animateInt],
 * [animateColor][AnimationGroup.animateColor], and [animateDimen][AnimationGroup.animateDimen]
 * methods for adding animations that all retarget together when [targetState] changes.
 *
 * ```kotlin
 * val transition = useAnimationGroup(expanded)
 *
 * val height = transition.animateDimen(
 *     transitionSpec = {
 *         when {
 *             false.isTransitioningTo(true) -> spring(stiffness = Stiffness.Low)
 *             else -> tween(200.ms)
 *         }
 *     },
 * ) { state -> if (state) 200.dp else 56.dp }
 *
 * val alpha = transition.animateFloat { state -> if (state) 1f else 0f }
 * ```
 *
 * @param targetState The state to animate toward. Changes trigger retargeting of all animations.
 * @param trigger Controls when the animation begins receiving frame callbacks. Defaults to
 *   [AnimationTrigger.Immediate].
 * @param label Optional label for debugging/tracing.
 * @return An [AnimationGroup] for adding animations.
 * @see AnimationGroup
 * @see AnimationGroupRecord
 * @see AnimationGroupState
 * @see Segment
 */
@ExperimentalLithoApi
@Hook
fun <S> ComponentScope.useAnimationGroup(
    targetState: S,
    trigger: AnimationTrigger = AnimationTrigger.Immediate,
    label: String = "transition",
): AnimationGroup<S> {
  val baseHookKey = animationHookKey()
  val lithoTree = context.lithoTree ?: error("useAnimationGroup must be called within a LithoTree")

  val groupState = getUpdatedState {
    AnimationGroupState<S>(
        initialState = targetState,
        treeId = lithoTree.id,
        globalKey = context.globalKey,
        baseHookKey = baseHookKey,
        isLayoutContext = context.isContextForLayout,
        stateUpdater = lithoTree.stateUpdater,
        label = label,
    )
  }

  val group = AnimationGroup(groupState, targetState, resourceResolver)

  // Commit this render's immutable declaration into the runtime, creating the long-lived
  // AnimatedValues on the main thread. Runs every render (group is fresh each time) and before the
  // retarget effect below, so retarget reads the just-committed declaration.
  useEffect(group) {
    groupState.commit(group.createRecord())
    null
  }

  // Retarget on the main thread, reading the just-committed declaration (the commit effect above
  // ran first) rather than racing concurrent resolves. Registration is deferred to the
  // AnimationTrigger: a fresh group needs a start handle; an already-active one keeps its frames; a
  // partial retarget (some slots settled while others run) re-registers only the settled ones.
  useEffect(targetState) {
    if (groupState.retarget()) {
      val handle = AnimationHandle {
        for (animatedValue in groupState.animatedValues) {
          AnimationController.register(animatedValue.frameAnimation)
        }
      }
      val cleanup = trigger.onReady(handle)
      return@useEffect onCleanup {
        handle.invalidate()
        cleanup?.onCleanup()
      }
    }
    null
  }

  // Clean up on unmount
  useEffect(Unit) {
    onCleanup {
      for (animatedValue in groupState.animatedValues) {
        animatedValue.stop()
        AnimationController.unregister(animatedValue.frameAnimation)
      }
      groupState.onSettled = null
    }
  }

  return group
}
