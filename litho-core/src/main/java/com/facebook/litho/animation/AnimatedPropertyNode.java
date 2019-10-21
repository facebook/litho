/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho.animation;

import android.graphics.drawable.Drawable;
import android.view.View;
import com.facebook.litho.OutputUnitsAffinityGroup;
import com.facebook.litho.dataflow.ValueNode;
import java.lang.ref.WeakReference;
import javax.annotation.Nullable;

/**
 * A ValueNode that allows getting and/or setting the value of a specific property (x, y, scale,
 * text color, etc) on a given mount content (View or Drawable).
 *
 * <p>If there is no input hooked up to this node, it will output the current value of this property
 * on the current mount content. Otherwise, this node will set the property of the given mount
 * content to that input value and pass on that value as an output.
 */
public class AnimatedPropertyNode extends ValueNode {

  private final AnimatedProperty mAnimatedProperty;
  private final OutputUnitsAffinityGroup<WeakReference<Object>> mMountContentGroup =
      new OutputUnitsAffinityGroup<>();
  private boolean mUsingRenderThread;

  public AnimatedPropertyNode(
      OutputUnitsAffinityGroup<Object> mountContentGroup, AnimatedProperty animatedProperty) {
    setMountContentGroupInner(mountContentGroup);
    mAnimatedProperty = animatedProperty;
  }

  /** Sets the mount content that this {@link AnimatedPropertyNode} updates a value on. */
  public void setMountContentGroup(OutputUnitsAffinityGroup<Object> mountContentGroup) {
    setMountContentGroupInner(mountContentGroup);
    setValueInner(getValue());
  }

  @Override
  public void setValue(float value) {
    super.setValue(value);
    setValueInner(value);
  }

  @Override
  public float calculateValue(long frameTimeNanos) {
    boolean hasInput = hasInput();
    final Object mountContent = resolveReference(mMountContentGroup.getMostSignificantUnit());

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
    setValueInner(value);

    return value;
  }

  /**
   * Sets if the content is being animated on the Render thread, which means that the further passed
   * are not be applied to the content, but just to be recorded
   */
  void setUsingRenderThread(boolean usingRenderThread) {
    this.mUsingRenderThread = usingRenderThread;
  }

  /**
   * If the mount content is just a single {@link android.view.View}- returns that view, otherwise
   * returns null. Used by {@link RenderThreadTransition} as for now we can only run RT animations
   * on View
   */
  @Nullable
  View getSingleTargetView() {
    View view = null;
    for (int i = 0, size = mMountContentGroup.size(); i < size; i++) {
      final Object mountContent = resolveReference(mMountContentGroup.getAt(i));
      if (mountContent == null) {
        continue;
      }
      if (view != null || !(mountContent instanceof View)) {
        return null;
      }
      view = (View) mountContent;
    }
    return view;
  }

  private void setMountContentGroupInner(OutputUnitsAffinityGroup<Object> mountContentGroup) {
    mMountContentGroup.clean();
    if (mountContentGroup == null) {
      return;
    }
    for (int i = 0, size = mountContentGroup.size(); i < size; ++i) {
      final WeakReference<Object> mountContentRef = new WeakReference<>(mountContentGroup.getAt(i));
      mMountContentGroup.add(mountContentGroup.typeAt(i), mountContentRef);
    }
  }

  private void setValueInner(float value) {
    if (mUsingRenderThread) {
      return;
    }

    for (int i = 0, size = mMountContentGroup.size(); i < size; i++) {
      final Object mountContent = resolveReference(mMountContentGroup.getAt(i));
      if (mountContent != null) {
        mAnimatedProperty.set(mountContent, value);
      }
    }
  }

  @Nullable
  private static Object resolveReference(WeakReference<Object> mountContentRef) {
    final Object mountContent = mountContentRef != null ? mountContentRef.get() : null;
    if (mountContent == null) {
      return null;
    }
    if ((mountContent instanceof Drawable) && ((Drawable) mountContent).getCallback() == null) {
      // MountContent have been already unmounted but we still hold reference to it, so let's clear
      // up the reference and return null. Actual unmounting of a view can be deferred, so don't
      // check it here
      mountContentRef.clear();
      return null;
    }
    return mountContent;
  }
}
