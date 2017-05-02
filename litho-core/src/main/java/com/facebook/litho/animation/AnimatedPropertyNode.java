/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.animation;

import java.lang.ref.WeakReference;

import com.facebook.litho.dataflow.ValueNode;

/**
 * A ValueNode that allows getting and/or setting the value of a specific property (x, y, scale,
 * text color, etc) on a given mount item (View or Drawable).
 *
 * If there is no input hooked up to this node, it will output the current value of this property
 * on the current mount item. Otherwise, this node will set the property of the given mount item to
 * that input value and pass on that value as an output.
 */
public class AnimatedPropertyNode extends ValueNode {

  private final AnimatedProperty mAnimatedProperty;
  private WeakReference<Object> mMountItem;

  public AnimatedPropertyNode(Object mountItem, AnimatedProperty animatedProperty) {
    mMountItem = new WeakReference<>(mountItem);
    mAnimatedProperty = animatedProperty;
  }

  /**
   * Sets the mount item that this {@link AnimatedPropertyNode} updates a value on.
   */
  public void setMountItem(Object mountItem) {
    mMountItem = new WeakReference<>(mountItem);
  }

  @Override
  public float calculateValue(long frameTimeNanos) {
    final Object mountItem = mMountItem.get();

    if (mountItem == null) {
      // This should probably be handled more gracefully, but for now it's probably important to
      // understand when this is happening so that we know what the proper behavior should be.
      throw new RuntimeException("Mount item went away while we were animating it!");
    }

    if (!hasInput()) {
      return mAnimatedProperty.get(mountItem);
    }

    float value = getInput().getValue();
    mAnimatedProperty.set(mountItem, value);

    return value;
  }
}
