/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

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

  ComponentAccessibilityDelegate(View view, NodeInfo nodeInfo) {
    super(view);
    mView = view;
    mNodeInfo = nodeInfo;
    mSuperDelegate = new SuperDelegate();
  }

  ComponentAccessibilityDelegate(View view) {
    this(view, null);
  }

  /**
   * {@link ComponentHost} contains the logic for setting the {@link NodeInfo} containing the
   * {@link EventHandler}s for its delegate instance whenever it is set/unset
   *
   * @see ComponentHost#setTag(int, Object)
   */
  void setNodeInfo(NodeInfo nodeInfo) {
    mNodeInfo = nodeInfo;
  }

  @Override
  public void onInitializeAccessibilityNodeInfo(
      View host,
      AccessibilityNodeInfoCompat node) {
    final MountItem mountItem = getAccessibleMountItem(mView);

    if (
      mNodeInfo != null
      && mNodeInfo.getOnInitializeAccessibilityNodeInfoHandler() != null) {
      EventDispatcherUtils.dispatchOnInitializeAccessibilityNodeInfoEvent(
          mNodeInfo.getOnInitializeAccessibilityNodeInfoHandler(),
          host,
          node,
          mSuperDelegate);
    } else if (mountItem != null) {
      super.onInitializeAccessibilityNodeInfo(host, node);
      // Coalesce the accessible mount item's information with the
      // the root host view's as they are meant to behave as a single
      // node in the accessibility framework.
      final Component<?> component = mountItem.getComponent();
      component.getLifecycle().onPopulateAccessibilityNode(node, component);
    } else {
      super.onInitializeAccessibilityNodeInfo(host, node);
    }
  }

  @Override
  protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
    final MountItem mountItem = getAccessibleMountItem(mView);
    if (mountItem == null) {
      return;
    }

    final Component<?> component = mountItem.getComponent();

    final int extraAccessibilityNodesCount =
        component.getLifecycle().getExtraAccessibilityNodesCount(component);

    // Expose extra accessibility nodes declared by the component to the
    // accessibility framework. The actual nodes will be populated in
    // {@link #onPopulateNodeForVirtualView}.
    for (int i = 0; i < extraAccessibilityNodesCount; i++) {
      virtualViewIds.add(i);
    }
  }

  @Override
  protected void onPopulateNodeForVirtualView(
      int virtualViewId,
      AccessibilityNodeInfoCompat node) {
    final MountItem mountItem = getAccessibleMountItem(mView);
    if (mountItem == null) {
      Log.e(TAG, "No accessible mount item found for view: " + mView);

      // ExploreByTouchHelper insists that we set something.
      node.setContentDescription("");
      node.setBoundsInParent(getDefaultBounds());
      return;
    }

    final Drawable drawable = (Drawable) mountItem.getContent();
    final Rect bounds = drawable.getBounds();

    final Component<?> component = mountItem.getComponent();
    final ComponentLifecycle lifecycle = component.getLifecycle();

    node.setClassName(lifecycle.getClass().getName());

    if (virtualViewId >= lifecycle.getExtraAccessibilityNodesCount(component)) {
      Log.e(TAG, "Received unrecognized virtual view id: " + virtualViewId);

      // ExploreByTouchHelper insists that we set something.
      node.setContentDescription("");
      node.setBoundsInParent(getDefaultBounds());
