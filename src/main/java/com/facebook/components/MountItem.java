/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.facebook.litho.displaylist.DisplayList;

import static android.support.v4.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static android.support.v4.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO;

/**
 * Represents a mounted UI element in a {@link MountState}. It holds a
 * key and a content instance which might be any type of UI element
 * supported by the framework e.g. {@link Drawable}.
 */
class MountItem {

  static final int FLAG_DUPLICATE_PARENT_STATE = 1 << 0;
  static final int FLAG_VIEW_CLICKABLE = 1 << 1;
  static final int FLAG_VIEW_LONG_CLICKABLE = 1 << 2;
  static final int FLAG_VIEW_FOCUSABLE = 1 << 3;

  private NodeInfo mNodeInfo;
  private ViewNodeInfo mViewNodeInfo;
  private Component<?> mComponent;
  private Object mContent;
  private ComponentHost mHost;
  private boolean mIsBound;
  private int mImportantForAccessibility;
  private DisplayListDrawable mDisplayListDrawable;

  // ComponentHost flags defined in the LayoutOutput specifying
  // the behaviour of this item when mounted.
  private int mFlags;

  void init(
      Component<?> component,
      MountItem mountItem,
      LayoutOutput layoutOutput) {
    init(
        component,
        mountItem.getHost(),
        mountItem.getContent(),
        layoutOutput,
        mountItem.getDisplayListDrawable());
  }

  void init(
      Component<?> component,
      ComponentHost host,
      Object content,
      LayoutOutput layoutOutput,
      DisplayListDrawable displayListDrawable) {
    init(
        component,
        host,
        content,
        layoutOutput.getNodeInfo(),
        layoutOutput.getViewNodeInfo(),
        acquireDisplayListDrawableIfNeeded(
            content,
            layoutOutput.getDisplayList(),
            displayListDrawable),
        layoutOutput.getFlags(),
        layoutOutput.getImportantForAccessibility());
  }

  void init(
      Component<?> component,
      ComponentHost host,
      Object content,
      NodeInfo nodeInfo,
      ViewNodeInfo viewNodeInfo,
      DisplayListDrawable displayListDrawable,
