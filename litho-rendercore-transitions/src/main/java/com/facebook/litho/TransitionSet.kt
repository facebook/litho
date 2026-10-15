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

package com.facebook.litho

import com.facebook.litho.animation.AnimationBinding

/** A set of [Transition]s. */
abstract class TransitionSet : Transition {

  val children: ArrayList<Transition> = ArrayList()

  internal constructor(vararg children: Transition?) {
    for (i in children.indices) {
      addChild(children[i])
    }
  }

  internal constructor(children: List<Transition?>) {
    for (i in children.indices) {
      addChild(children[i])
    }
  }

  private fun addChild(child: Transition?) {
    if (child is BaseTransitionUnitsBuilder) {
      val transitions: ArrayList<out Transition> = child.transitionUnits
      if (transitions.size > 1) {
        children.add(ParallelTransitionSet(transitions))
      } else {
        children.add(transitions[0])
      }
    } else if (child != null) {
      children.add(child)
    }
  }

  internal abstract fun createAnimation(
      children: @JvmSuppressWildcards List<AnimationBinding>,
  ): AnimationBinding
}
