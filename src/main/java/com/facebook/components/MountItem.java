/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.facebook.components.displaylist.DisplayList;

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
      int flags,
      int importantForAccessibility) {
    mComponent = component;
    mContent = content;
    mHost = host;
    mFlags = flags;
    mImportantForAccessibility = importantForAccessibility;
    mDisplayListDrawable = displayListDrawable;

    if (mNodeInfo != null) {
      mNodeInfo.release();
      mNodeInfo = null;
    }

    if (nodeInfo != null) {
      mNodeInfo = nodeInfo.acquireRef();
    }

    if (mViewNodeInfo != null) {
      mViewNodeInfo.release();
      mViewNodeInfo = null;
    }

    if (viewNodeInfo != null) {
      mViewNodeInfo = viewNodeInfo.acquireRef();
    }

    if (mContent instanceof View) {
      final View view = (View) mContent;

      if (view.isClickable()) {
        mFlags |= FLAG_VIEW_CLICKABLE;
      }

      if (view.isLongClickable()) {
        mFlags |= FLAG_VIEW_LONG_CLICKABLE;
      }

      if (view.isFocusable()) {
        mFlags |= FLAG_VIEW_FOCUSABLE;
      }
    }
  }

  private DisplayListDrawable acquireDisplayListDrawableIfNeeded(
      Object content,
      DisplayList displayList,
      DisplayListDrawable convertDisplayListDrawable) {
    if (displayList != null) {
      if (convertDisplayListDrawable != null) {
        convertDisplayListDrawable.setWrappedDrawable((Drawable) content, displayList);
      } else  {
        convertDisplayListDrawable = ComponentsPools.acquireDisplayListDrawable(
            (Drawable) content, displayList);
      }
      convertDisplayListDrawable.suppressInvalidations(true);
    } else if (convertDisplayListDrawable != null) {
      convertDisplayListDrawable.setWrappedDrawable
          ((Drawable) content, null);
    }

    return convertDisplayListDrawable;
  }

  Component<?> getComponent() {
    return mComponent;
  }

  ComponentHost getHost() {
    return mHost;
  }

  Object getContent() {
    return mContent;
  }

  int getFlags() {
    return mFlags;
  }

  int getImportantForAccessibility() {
    return mImportantForAccessibility;
  }

  NodeInfo getNodeInfo() {
    return mNodeInfo;
  }

  ViewNodeInfo getViewNodeInfo() {
    return mViewNodeInfo;
  }

  boolean isAccessible() {
    if (mComponent == null) {
      return false;
    }

    if (mImportantForAccessibility == IMPORTANT_FOR_ACCESSIBILITY_NO) {
      return false;
    }

