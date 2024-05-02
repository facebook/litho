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

package com.facebook.litho.transition

import com.facebook.litho.ComponentScope
import com.facebook.litho.Diff
import com.facebook.litho.Transition
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.annotations.Hook
import com.facebook.rendercore.transitions.TransitionUtils

/** Defines one or more [Transition] animations for the given component */
@Hook
fun ComponentScope.useTransition(transition: Transition?) {
  transition ?: return
  TransitionUtils.setOwnerKey(transition, context.globalKey)
  val transitionsList = transitions ?: ArrayList<Transition>()
  transitionsList.add(transition)
  transitions = transitionsList
}

/**
 * Defines one or more [Transition] animations for the given component and allows for the
 * [Transition] to be re-evaluated based on changes in the [deps]
 *
 * [deps] should contain any props, states or derived values that are either used within the
 * [createTransition] lambda, or are needed to trigger a re-evaluation of the transition. For
 * example, when a component needs to be animated every time a boolean state changes, that state
 * should be added to [deps], even if the state itself isn't necessarily referenced inside
 * [createTransition].
 *
 * @param deps A list of dependencies that will trigger a re-evaluation of the transition.
 * @param createTransition A lambda that creates the [Transition] object.
 */
@Hook
@ExperimentalLithoApi
fun ComponentScope.useTransition(
    vararg deps: Any?,
    createTransition: UseTransitionScope.() -> Transition?
) {
  error("Not yet implemented")
}

/**
 * Defines single or multiple [Transition] animations for the given component.
 *
 * It is an error to call [useTransition] without deps parameter.
 *
 * Note: the cases where the transition should be applied unconditionally, or only on first render
 * can be achieved by passing `Any()` or `Unit` as deps respectively. The unconditional case can
 * also be achieved by using the simple [useTransition] function that takes a direct [Transition]
 * object.
 */
// This deprecated-error function shadows the varargs overload so that the varargs version is not
// used without deps parameters.
@Deprecated(USE_TRANSITION_NO_DEPS_ERROR, level = DeprecationLevel.ERROR)
@Hook
fun ComponentScope.useTransition(createTransition: UseTransitionScope.() -> Transition?) {
  error(USE_TRANSITION_NO_DEPS_ERROR)
}

interface UseTransitionScope {
  /**
   * Returns a [Diff] of previous and current values of the supplied [input].
   *
   * Note: while this method is not a hook, it exhibits one important similarity in that it must be
   * called unconditionally in order to ensure correctness across multiple invocations.
   *
   * @param input The input value whose diff is to be calculated. This input must have already been
   *   defined as a dependency of the [useTransition] call.
   * @return A [Diff] of previous and current values of the supplied [input].
   * @see useTransition
   */
  fun <T> diffOf(input: T): Diff<T>
}

private const val USE_TRANSITION_NO_DEPS_ERROR =
    "useTransition must provide 'deps' parameter that determines when the transition should be recalculated."
