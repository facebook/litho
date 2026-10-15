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

import com.facebook.litho.dataflow.ConstantNode
import com.facebook.litho.dataflow.SpringNode
import com.facebook.litho.dataflow.ValueNode

/** Animation for the transition of the position of a mount content on a quadratic Bezier curve. */
class BezierTransition(
    private val xPropertyAnimation: PropertyAnimation,
    private val yPropertyAnimation: PropertyAnimation,
    private val controlX: Float,
    private val controlY: Float,
) : TransitionAnimationBinding() {

  /**
   * Creates a quadratic Bezier transition. The control x/y are used to configure the shape of the
   * curve. Because we don't know the start/end positions beforehand, the control point is defined
   * in terms of the distance between the start and end points of this animation. It is NOT in terms
   * of pixels or dp.
   *
   * Specifically, controlPointX=0 will give the control point the x position of initial position of
   * the curve. controlPointX=1 will give the control point the x position of the end of the curve.
   * controlPointX=.5 will give it the x position midway between the start and end x positions.
   * Increasing the value beyond 1 or below 0 will move the control point beyond the end x position
   * or before the start x position, respectively, while values between 0 and 1 will place the point
   * in between the start and end x positions.
   *
   * All of the above also applies to the controlPointY value as well.
   *
   * For good looking curves, you want to make sure controlPointX != controlPointY (or else the
   * curve won't curve since it lies on the straight line between the start and end points).
   */
  override fun collectTransitioningProperties(outList: ArrayList<PropertyAnimation>) {
    outList.add(xPropertyAnimation)
    outList.add(yPropertyAnimation)
  }

  override fun setupBinding(resolver: Resolver) {
    val startX = resolver.getCurrentState(xPropertyAnimation.propertyHandle)
    val endX = xPropertyAnimation.targetValue
    val startY = resolver.getCurrentState(yPropertyAnimation.propertyHandle)
    val endY = yPropertyAnimation.targetValue
    val controlXValue = (endX - startX) * controlX + startX
    val controlYValue = (endY - startY) * controlY + startY

    val springNode = SpringNode()
    val xBezierNode = BezierNode(startX, endX, controlXValue)
    val yBezierNode = BezierNode(startY, endY, controlYValue)

    addBinding(ConstantNode(0f), springNode, SpringNode.INITIAL_INPUT)
    addBinding(ConstantNode(1f), springNode, SpringNode.END_INPUT)
    addBinding(springNode, xBezierNode)
    addBinding(springNode, yBezierNode)
    addBinding(xBezierNode, resolver.getAnimatedPropertyNode(xPropertyAnimation.propertyHandle))
    addBinding(yBezierNode, resolver.getAnimatedPropertyNode(yPropertyAnimation.propertyHandle))
  }

  private class BezierNode(
      private val initial: Float,
      private val end: Float,
      private val controlPoint: Float,
  ) : ValueNode() {

    override fun calculateValue(frameTimeNanos: Long): Float {
      val t = getInput().value
      // Bezier math from Wikipedia: https://goo.gl/MvrMei
      return (1 - t) * (1 - t) * initial + 2 * t * (1 - t) * controlPoint + t * t * end
    }
  }
}
