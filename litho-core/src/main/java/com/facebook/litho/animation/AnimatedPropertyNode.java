/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.animation;

import com.facebook.litho.dataflow.ValueNode;
import java.lang.ref.WeakReference;

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
  private WeakReference<Object> mMountContent;

  public AnimatedPropertyNode(Object mountContent, AnimatedProperty animatedProperty) {
    mMountContent = new WeakReference<>(mountContent);
    mAnimatedProperty = animatedProperty;
  }

  /**
   * Sets the mount content that this {@link AnimatedPropertyNode} updates a value on.
   */
  public void setMountContent(Object mountContent) {
    mMountContent = new WeakReference<>(mountContent);
    if (mountContent != null) {
      mAnimatedProperty.set(mountContent, getValue());
    }
  }

  public void setValue(float value) {
    super.setValue(value);
    final Object mountContent = mMountContent.get();
    if (mountContent != null) {
      mAnimatedProperty.set(mountContent, value);
    }
  }

  @Override
  public float calculateValue(long frameTimeNanos) {
    final Object mountContent = mMountContent.get();
    final boolean hasInput = hasInput();

    if (mountContent == null) {
      if (hasInput) {
        return getInput().getValue();
      }

      // If we have no input and have lost our mount content, just return our last known value.
      return getValue();
    }

    if (!hasInput) {
      return mAnimatedProperty.get(mountContent);
    }

    final float value = getInput().getValue();
    mAnimatedProperty.set(mountContent, value);

    return value;
  }
}
