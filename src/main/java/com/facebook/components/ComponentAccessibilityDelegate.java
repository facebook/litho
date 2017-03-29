/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import java.util.List;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.view.accessibility.AccessibilityNodeProviderCompat;
import android.support.v4.widget.ExploreByTouchHelper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;

/**
 * Class that is used to set up accessibility for {@link ComponentHost}s.
 * Virtual nodes are only exposed if the component implements support for
 * extra accessibility nodes.
 */
class ComponentAccessibilityDelegate extends ExploreByTouchHelper {
  private static final String TAG = "ComponentAccessibility";

  private final View mView;
  private NodeInfo mNodeInfo;
  private final AccessibilityDelegateCompat mSuperDelegate;
  private static Rect sDefaultBounds;
