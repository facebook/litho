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
