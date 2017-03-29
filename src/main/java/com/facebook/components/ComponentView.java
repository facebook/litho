/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.util.Deque;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.VisibleForTesting;
import android.support.v4.view.accessibility.AccessibilityManagerCompat;
import android.support.v4.view.accessibility.AccessibilityManagerCompat.AccessibilityStateChangeListenerCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;

import com.facebook.proguard.annotations.DoNotStrip;

import static android.content.Context.ACCESSIBILITY_SERVICE;
import static com.facebook.litho.AccessibilityUtils.isAccessibilityEnabled;

/**
 * A {@link ViewGroup} that can host the mounted state of a {@link Component}.
 */
public class ComponentView extends ComponentHost {
  private ComponentTree mComponent;
  private final MountState mMountState;
  private boolean mIsAttached;
  private final Rect mPreviousMountBounds = new Rect();

  private boolean mForceLayout;

  private final AccessibilityManager mAccessibilityManager;

  private final AccessibilityStateChangeListenerCompat mAccessibilityStateChangeListener =
      new AccessibilityStateChangeListenerCompat() {
        @Override
        public void onAccessibilityStateChanged(boolean enabled) {
          refreshAccessibilityDelegatesIfNeeded(enabled);

          requestLayout();
        }
      };

  private static final int[] sLayoutSize = new int[2];

  // Keep ComponentTree when detached from this view in case the ComponentTree is shared between
  // sticky header and RecyclerView's binder
  // TODO T14859077 Replace with proper solution
  private ComponentTree mTemporaryDetachedComponent;

  public ComponentView(Context context) {
    this(context, null);
  }

  public ComponentView(Context context, AttributeSet attrs) {
    this(new ComponentContext(context), attrs);
  }

  public ComponentView(ComponentContext context) {
    this(context, null);
  }

  public ComponentView(ComponentContext context, AttributeSet attrs) {
    super(context, attrs);

    mMountState = new MountState(this);
    mAccessibilityManager = (AccessibilityManager) context.getSystemService(ACCESSIBILITY_SERVICE);
  }

  private static void performLayoutOnChildrenIfNecessary(ComponentHost host) {
    for (int i = 0, count = host.getChildCount(); i < count; i++) {
      final View child = host.getChildAt(i);

      if (child.isLayoutRequested()) {
        // The hosting view doesn't allow children to change sizes dynamically as
        // this would conflict with the component's own layout calculations.
        child.measure(
            MeasureSpec.makeMeasureSpec(child.getWidth(), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(child.getHeight(), MeasureSpec.EXACTLY));
        child.layout(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
      }

      if (child instanceof ComponentHost) {
        performLayoutOnChildrenIfNecessary((ComponentHost) child);
      }
    }
  }

  void forceRelayout() {
    mForceLayout = true;
    requestLayout();
  }

  public void startTemporaryDetach() {
    mTemporaryDetachedComponent = mComponent;
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    onAttach();
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    onDetach();
  }

  @Override
  public void onStartTemporaryDetach() {
    super.onStartTemporaryDetach();
    onDetach();
  }

  @Override
  public void onFinishTemporaryDetach() {
    super.onFinishTemporaryDetach();
    onAttach();
  }

  private void onAttach() {
    if (!mIsAttached) {
      mIsAttached = true;

      if (mComponent != null) {
        mComponent.attach();
      }

      refreshAccessibilityDelegatesIfNeeded(isAccessibilityEnabled(getContext()));

      AccessibilityManagerCompat.addAccessibilityStateChangeListener(
          mAccessibilityManager,
          mAccessibilityStateChangeListener);
    }
  }

  private void onDetach() {
    if (mIsAttached) {
      mIsAttached = false;

      if (mComponent != null) {
        mMountState.detach();

        mComponent.detach();
      }

      AccessibilityManagerCompat.removeAccessibilityStateChangeListener(
          mAccessibilityManager,
          mAccessibilityStateChangeListener);
    }
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int width = MeasureSpec.getSize(widthMeasureSpec);
    int height = MeasureSpec.getSize(heightMeasureSpec);

    if (mTemporaryDetachedComponent != null && mComponent == null) {
      setComponent(mTemporaryDetachedComponent);
      mTemporaryDetachedComponent = null;
    }

    if (mComponent != null) {
      boolean forceRelayout = mForceLayout;
      mForceLayout = false;
      mComponent.measure(widthMeasureSpec, heightMeasureSpec, sLayoutSize, forceRelayout);

      width = sLayoutSize[0];
      height = sLayoutSize[1];
    }

    setMeasuredDimension(width, height);
  }

  @Override
  protected void performLayout(boolean changed, int left, int top, int right, int bottom) {

    if (mComponent != null) {
      boolean wasMountTriggered = mComponent.layout();

