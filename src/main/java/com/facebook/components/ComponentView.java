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
