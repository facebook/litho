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
      return;
    }

    lifecycle.onPopulateExtraAccessibilityNode(
        node,
        virtualViewId,
        bounds.left,
        bounds.top,
        component);
  }

  /**
   * Finds extra accessibility nodes under the given event coordinates.
   * Returns {@link #INVALID_ID} otherwise.
   */
  @Override
  protected int getVirtualViewAt(float x, float y) {
    final MountItem mountItem = getAccessibleMountItem(mView);
    if (mountItem == null) {
      return INVALID_ID;
    }

    final Component<?> component = mountItem.getComponent();
    final ComponentLifecycle lifecycle = component.getLifecycle();

    if (lifecycle.getExtraAccessibilityNodesCount(component) == 0) {
      return INVALID_ID;
    }

    final Drawable drawable = (Drawable) mountItem.getContent();
    final Rect bounds = drawable.getBounds();

    // Try to find an extra accessibility node that intersects with
    // the given coordinates.
    final int virtualViewId = lifecycle.getExtraAccessibilityNodeAt(
        (int) x - bounds.left,
        (int) y - bounds.top,
        component);

    return (virtualViewId >= 0 ? virtualViewId : INVALID_ID);
  }

  @Override
  protected void onPopulateEventForVirtualView(int virtualViewId, AccessibilityEvent event) {
    // TODO (T10543861): ExploreByTouchHelper enforces subclasses to set a content description
    // or text on new events but components don't provide APIs to do so yet.
    event.setContentDescription("");
  }

  @Override
  protected boolean onPerformActionForVirtualView(
      int virtualViewId,
      int action,
      Bundle arguments) {
    return false;
  }

  /**
   * Returns a {AccessibilityNodeProviderCompat} if the host contains a component
   * that implements custom accessibility logic. Returns {@code NULL} otherwise.
   * Components with accessibility content are automatically wrapped in hosts by
   * {@link LayoutState}.
   */
  @Override
  public AccessibilityNodeProviderCompat getAccessibilityNodeProvider(View host) {
    final MountItem mountItem = getAccessibleMountItem(mView);
    if (mountItem != null
        && mountItem.getComponent().getLifecycle().implementsExtraAccessibilityNodes()) {
      return super.getAccessibilityNodeProvider(host);
    }

    return null;
  }

  private static MountItem getAccessibleMountItem(View view) {
    if (!(view instanceof ComponentHost)) {
      return null;
    }

    return ((ComponentHost) view).getAccessibleMountItem();
  }

  @Override
  public void onInitializeAccessibilityEvent(
      View host, AccessibilityEvent event) {
    if (mNodeInfo != null && mNodeInfo.getOnInitializeAccessibilityEventHandler() != null) {
      EventDispatcherUtils.dispatchOnInitializeAccessibilityEvent(
          mNodeInfo.getOnInitializeAccessibilityEventHandler(),
          host,
          event,
          mSuperDelegate);
    } else {
      super.onInitializeAccessibilityEvent(host, event);
    }
  }

  @Override
  public void sendAccessibilityEvent(View host, int eventType) {
    if (mNodeInfo != null && mNodeInfo.getSendAccessibilityEventHandler() != null) {
      EventDispatcherUtils.dispatchSendAccessibilityEvent(
          mNodeInfo.getSendAccessibilityEventHandler(),
          host,
          eventType,
          mSuperDelegate);
    } else {
      super.sendAccessibilityEvent(host, eventType);
    }
  }

  @Override
  public void sendAccessibilityEventUnchecked(View host, AccessibilityEvent event) {
    if (mNodeInfo != null && mNodeInfo.getSendAccessibilityEventUncheckedHandler() != null) {
      EventDispatcherUtils.dispatchSendAccessibilityEventUnchecked(
          mNodeInfo.getSendAccessibilityEventUncheckedHandler(),
          host,
          event,
          mSuperDelegate);
    } else {
      super.sendAccessibilityEventUnchecked(host, event);
    }
  }

  @Override
  public boolean dispatchPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
    if (mNodeInfo != null && mNodeInfo.getDispatchPopulateAccessibilityEventHandler() != null ) {
      return EventDispatcherUtils.dispatchDispatchPopulateAccessibilityEvent(
          mNodeInfo.getDispatchPopulateAccessibilityEventHandler(),
          host,
          event,
          mSuperDelegate);
    }

    return super.dispatchPopulateAccessibilityEvent(host, event);
  }

  @Override
  public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
    if (mNodeInfo != null && mNodeInfo.getOnPopulateAccessibilityEventHandler() != null) {
      EventDispatcherUtils.dispatchOnPopulateAccessibilityEvent(
          mNodeInfo.getOnPopulateAccessibilityEventHandler(),
          host,
          event,
          mSuperDelegate);
    } else {
      super.onPopulateAccessibilityEvent(host, event);
    }
  }

  @Override
  public boolean onRequestSendAccessibilityEvent(
      ViewGroup host,
      View child,
      AccessibilityEvent event) {
    if (mNodeInfo != null && mNodeInfo.getOnRequestSendAccessibilityEventHandler() != null) {
      return EventDispatcherUtils.dispatchOnRequestSendAccessibilityEvent(
          mNodeInfo.getOnRequestSendAccessibilityEventHandler(),
          host,
          child,
          event,
          mSuperDelegate);
    }

    return super.onRequestSendAccessibilityEvent(host, child, event);
  }

  @Override
  public boolean performAccessibilityAction(View host, int action, Bundle args) {
    if (mNodeInfo != null && mNodeInfo.getPerformAccessibilityActionHandler() != null) {
      return EventDispatcherUtils.dispatchPerformAccessibilityActionEvent(
          mNodeInfo.getPerformAccessibilityActionHandler(),
          host,
          action,
          args,
          mSuperDelegate);
    }

    return super.performAccessibilityAction(host, action, args);
  }

  private static Rect getDefaultBounds() {
    synchronized(sDefaultBounds) {
      if (sDefaultBounds == null) {
        sDefaultBounds = new Rect(0, 0, 1, 1);
      }
    }

    return sDefaultBounds;
  }

  private class SuperDelegate extends AccessibilityDelegateCompat {

    @Override
    public boolean dispatchPopulateAccessibilityEvent(
        View host, AccessibilityEvent event) {
      return ComponentAccessibilityDelegate.super.dispatchPopulateAccessibilityEvent(host, event);
    }

    @Override
    public void onInitializeAccessibilityEvent(
        View host, AccessibilityEvent event) {
      ComponentAccessibilityDelegate.super.onInitializeAccessibilityEvent(host, event);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(
        View host, AccessibilityNodeInfoCompat node) {
      ComponentAccessibilityDelegate.super.onInitializeAccessibilityNodeInfo(host, node);
    }

    @Override
    public void onPopulateAccessibilityEvent(
        View host, AccessibilityEvent event) {
      ComponentAccessibilityDelegate.super.onPopulateAccessibilityEvent(host, event);
    }

    @Override
    public boolean onRequestSendAccessibilityEvent(
        ViewGroup host, View child, AccessibilityEvent event) {
      return ComponentAccessibilityDelegate.super.onRequestSendAccessibilityEvent(
          host,
          child,
          event);
    }

    @Override
    public boolean performAccessibilityAction(View host, int action, Bundle args) {
      return ComponentAccessibilityDelegate.super.performAccessibilityAction(host, action, args);
    }

    @Override
    public void sendAccessibilityEvent(View host, int eventType) {
      ComponentAccessibilityDelegate.super.sendAccessibilityEvent(host, eventType);
    }

    @Override
    public void sendAccessibilityEventUnchecked(
        View host, AccessibilityEvent event) {
      ComponentAccessibilityDelegate.super.sendAccessibilityEventUnchecked(host, event);
    }
  }
}
