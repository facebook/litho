/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */


package com.facebook.litho.animation;

import com.facebook.litho.dataflow.ConstantNode;
import com.facebook.litho.dataflow.SpringNode;
import com.facebook.litho.dataflow.ValueNode;
import com.facebook.litho.internal.ArraySet;

/**
 * Animation for the transition of the position of a mount content on a quadratic Bezier curve.
 */
public class BezierTransition extends TransitionAnimationBinding {

  private final ComponentProperty mXProperty;
  private final ComponentProperty mYProperty;
  private final float mControlX;
  private final float mControlY;

  /**
   * Creates a quadratic Bezier transition. The control x/y are used to configure the shape of the
   * curve. Because we don't know the start/end positions beforehand, the control point is defined
   * in terms of the distance between the start and end points of this animation. It is NOT in
   * terms of pixels or dp.
   *
   * Specifically, controlPointX=0 will give the control point the x position of initial position
   * of the curve. controlPointX=1 will give the control point the x position of the end of the
   * curve. controlPointX=.5 will give it the x position midway between the start and end x
   * positions. Increasing the value beyond 1 or below 0 will move the control point beyond the
   * end x position or before the start x position, respectively, while values between 0 and 1
   * will place the point in between the start and end x positions.
   *
   * All of the above also applies to the controlPointY value as well.
   *
   * For good looking curves, you want to make sure controlPointX != controlPointY (or else the
   * curve won't curve since it lies on the straight line between the start and end points).
   */
  public BezierTransition(
      ComponentProperty xProperty,
      ComponentProperty yProperty,
      float controlX,
      float controlY) {
    mXProperty = xProperty;
    mYProperty = yProperty;
    mControlX = controlX;
    mControlY = controlY;
  }

  @Override
  public void collectTransitioningProperties(ArraySet<ComponentProperty> outSet) {
    outSet.add(mXProperty);
    outSet.add(mYProperty);
  }

  @Override
  protected void setupBinding(Resolver resolver) {
    final float startX = resolver.getCurrentState(mXProperty);
    final float endX = resolver.getEndState(mXProperty);
    final float startY = resolver.getCurrentState(mYProperty);
    final float endY = resolver.getEndState(mYProperty);
    final float controlX = (endX - startX) * mControlX + startX;
    final float controlY = (endY - startY) * mControlY + startY;

    SpringNode springNode = new SpringNode();
    BezierNode xBezierNode = new BezierNode(startX, endX, controlX);
    BezierNode yBezierNode = new BezierNode(startY, endY, controlY);

    addBinding(new ConstantNode(0f), springNode, SpringNode.INITIAL_INPUT);
    addBinding(new ConstantNode(1f), springNode, SpringNode.END_INPUT);
    addBinding(springNode, xBezierNode);
    addBinding(springNode, yBezierNode);
    addBinding(xBezierNode, resolver.getAnimatedPropertyNode(mXProperty));
    addBinding(yBezierNode, resolver.getAnimatedPropertyNode(mYProperty));
  }

  private static class BezierNode extends ValueNode {

    private final float mInitial;
    private final float mEnd;
    private final float mControlPoint;

    public BezierNode(float initial, float end, float controlPoint) {
      mInitial = initial;
      mEnd = end;
      mControlPoint = controlPoint;
    }

    @Override
    protected float calculateValue(long frameTimeNanos) {
      float t = getInput().getValue();
      // Bezier math from Wikipedia: https://goo.gl/MvrMei
      return (1 - t) * (1 - t) * mInitial + 2 * t * (1 - t) * mControlPoint + t * t * mEnd;
    }
  }
}
