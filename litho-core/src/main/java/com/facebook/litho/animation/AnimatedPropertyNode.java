// Copyright 2004-present Facebook. All Rights Reserved.

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
public class AnimatedPropertyNode extends ValueNode<Float> {

  private final WeakReference<Object> mMountItem;
  private final AnimatedProperty mAnimatedProperty;

  public AnimatedPropertyNode(Object mountItem, AnimatedProperty animatedProperty) {
    mMountItem = new WeakReference<>(mountItem);
    mAnimatedProperty = animatedProperty;
  }

  @Override
  public Float calculateValue(long frameTimeNanos) {
    final Object mountItem = mMountItem.get();

    if (mountItem == null) {
      // This should probably be handled more gracefully, but for now it's probably important to
      // understand when this is happening so that we know what the proper behavior should be.
      throw new RuntimeException("Mount item went away while we were animating it!");
    }

    if (!hasInput()) {
      return mAnimatedProperty.get(mountItem);
    }

    float value = (float) getInput().getValue();
    mAnimatedProperty.set(mountItem, value);

    return value;
  }
}
