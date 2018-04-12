/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.animation;

import com.facebook.litho.OutputUnitType;
import com.facebook.litho.OutputUnitsAffinityGroup;
import com.facebook.litho.dataflow.ValueNode;

/**
 * A ValueNode that allows getting and/or setting the value of a specific property (x, y, scale,
 * text color, etc) on a given mount content (View or Drawable).
 *
 * If there is no input hooked up to this node, it will output the current value of this property
 * on the current mount content. Otherwise, this node will set the property of the given mount
 * content to that input value and pass on that value as an output.
 */
public class AnimatedPropertyNode extends ValueNode {

  private final AnimatedProperty mAnimatedProperty;
  private OutputUnitsAffinityGroup<Object> mMountContentGroup;

  // T0D0: get rid of this when TransitionManager is ready
  public AnimatedPropertyNode(Object mountContent, AnimatedProperty animatedProperty) {
    this(wrapIntoGroup(mountContent), animatedProperty);
  }

  public AnimatedPropertyNode(
      OutputUnitsAffinityGroup<Object> mountContentGroup, AnimatedProperty animatedProperty) {
    mMountContentGroup = mountContentGroup;
    mAnimatedProperty = animatedProperty;
  }

  // T0D0: get rid of this when TransitionManager is ready
  public void setMountContent(Object mountContent) {
    setMountContentGroup(wrapIntoGroup(mountContent));
  }

  /** Sets the mount content that this {@link AnimatedPropertyNode} updates a value on. */
  public void setMountContentGroup(OutputUnitsAffinityGroup<Object> mountContentGroup) {
    mMountContentGroup = mountContentGroup;
    if (mountContentGroup != null) {
      setValueInner(getValue());
    }
  }

  @Override
  public void setValue(float value) {
    super.setValue(value);
    if (mMountContentGroup != null) {
      setValueInner(value);
    }
  }

  @Override
  public float calculateValue(long frameTimeNanos) {
    boolean hasInput = hasInput();

    if (mMountContentGroup == null || mMountContentGroup.isEmpty()) {
      if (hasInput) {
        return getInput().getValue();
      }

      // If we have no input and have lost our mount content, just return our last known value.
      return getValue();
    }

    if (!hasInput) {
      final Object mountContent = getMostSignificantMountContent();
      return mAnimatedProperty.get(mountContent);
    }

    final float value = getInput().getValue();
    setValueInner(value);

    return value;
  }

  private void setValueInner(float value) {
    for (int i = 0, size = mMountContentGroup.size(); i < size; i++) {
      final Object mountContent = mMountContentGroup.getAt(i);
      mAnimatedProperty.set(mountContent, value);
    }
  }

  private Object getMostSignificantMountContent() {
    Object mountContent = mMountContentGroup.get(OutputUnitType.HOST);
    if (mountContent != null) {
      return mountContent;
    }

    mountContent = mMountContentGroup.get(OutputUnitType.CONTENT);
    if (mountContent != null) {
      return mountContent;
    }

    mountContent = mMountContentGroup.get(OutputUnitType.BACKGROUND);
    if (mountContent != null) {
      return mountContent;
    }

    mountContent = mMountContentGroup.get(OutputUnitType.FOREGROUND);
    if (mountContent != null) {
      return mountContent;
    }

    return mMountContentGroup.get(OutputUnitType.BORDER);
  }

  // T0D0: get rid of this when TransitionManager is ready
  private static OutputUnitsAffinityGroup<Object> wrapIntoGroup(Object mountContent) {
    if (mountContent == null) {
      return null;
    }
    OutputUnitsAffinityGroup<Object> mountContentGroup = new OutputUnitsAffinityGroup<>();
    mountContentGroup.add(OutputUnitType.HOST, mountContent);
    return mountContentGroup;
  }
}
