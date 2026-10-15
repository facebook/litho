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

import android.view.animation.Interpolator
import com.facebook.litho.dataflow.ConstantNode
import com.facebook.litho.dataflow.InterpolatorNode
import com.facebook.litho.dataflow.MappingNode
import com.facebook.litho.dataflow.TimingNode

/** Animation for the transition of a single [PropertyAnimation] over a fixed amount of time. */
class TimingTransition
@JvmOverloads
constructor(
    private val durationMs: Int,
    private val propertyAnimation: PropertyAnimation,
    private val interpolator: Interpolator? = null,
) : TransitionAnimationBinding() {

  override fun collectTransitioningProperties(outList: ArrayList<PropertyAnimation>) {
    outList.add(propertyAnimation)
  }

  override fun setupBinding(resolver: Resolver) {
    val timingNode = TimingNode(durationMs)
    val initial = ConstantNode(resolver.getCurrentState(propertyAnimation.propertyHandle))
    val end = ConstantNode(propertyAnimation.targetValue)
    val mappingNode = MappingNode()

    if (interpolator != null) {
      val interpolatorNode = InterpolatorNode(interpolator)
      addBinding(timingNode, interpolatorNode)
      addBinding(interpolatorNode, mappingNode)
    } else {
      addBinding(timingNode, mappingNode)
    }
    addBinding(initial, mappingNode, MappingNode.INITIAL_INPUT)
    addBinding(end, mappingNode, MappingNode.END_INPUT)
    addBinding(mappingNode, resolver.getAnimatedPropertyNode(propertyAnimation.propertyHandle))
  }
}
