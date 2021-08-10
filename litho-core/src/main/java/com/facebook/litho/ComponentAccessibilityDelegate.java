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

package com.facebook.litho;

import static com.facebook.litho.LayoutOutput.getComponentContext;
import static com.facebook.litho.LayoutOutput.getLayoutOutput;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeProviderCompat;
import androidx.customview.widget.ExploreByTouchHelper;
import com.facebook.rendercore.MountItem;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Class that is used to set up accessibility for {@link ComponentHost}s. Virtual nodes are only
 * exposed if the component implements support for extra accessibility nodes.
 */
class ComponentAccessibilityDelegate extends ExploreByTouchHelper {
  private static final String TAG = "ComponentAccessibility";

  private final View mView;
  private NodeInfo mNodeInfo;
  private final AccessibilityDelegateCompat mSuperDelegate;
  private static final Rect sDefaultBounds = new Rect(0, 0, 1, 1);

  ComponentAccessibilityDelegate(
      View view, NodeInfo nodeInfo, boolean originalFocus, int originalImportantForAccessibility) {
    super(view);
    mView = view;
    mNodeInfo = nodeInfo;
    mSuperDelegate = new SuperDelegate();

    // We need to reset these two properties, as ExploreByTouchHelper sets focusable to "true" and
    // importantForAccessibility to "Yes" (if it is Auto). If we don't reset these it would force
    // every element that has this delegate attached to be focusable, and not allow for
    // announcement coalescing.
    mView.setFocusable(originalFocus);
    ViewCompat.setImportantForAccessibility(mView, originalImportantForAccessibility);
  }

  ComponentAccessibilityDelegate(
      View view, boolean originalFocus, int originalImportantForAccessibility) {
    this(view, null, originalFocus, originalImportantForAccessibility);
  }

  /**
   * {@link ComponentHost} contains the logic for setting the {@link NodeInfo} containing the {@link
   * EventHandler}s for its delegate instance whenever it is set/unset
   *
   * @see ComponentHost#setTag(int, Object)
   */
  void setNodeInfo(NodeInfo nodeInfo) {
    mNodeInfo = nodeInfo;
  }

  @Override
  public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat node) {
    final MountItem mountItem = getAccessibleMountItem(mView);

    if (mNodeInfo != null && mNodeInfo.getOnInitializeAccessibilityNodeInfoHandler() != null) {
      EventDispatcherUtils.dispatchOnInitializeAccessibilityNodeInfoEvent(
          mNodeInfo.getOnInitializeAccessibilityNodeInfoHandler(), host, node, mSuperDelegate);
    } else if (mountItem != null) {
      super.onInitializeAccessibilityNodeInfo(host, node);
      // Coalesce the accessible mount item's information with the
      // the root host view's as they are meant to behave as a single
      // node in the accessibility framework.
      final LayoutOutput layoutOutput = getLayoutOutput(mountItem);
      final Component component = layoutOutput.getComponent();
      final ComponentContext scopedContext = getComponentContext(mountItem.getRenderTreeNode());
      try {
        component.onPopulateAccessibilityNode(scopedContext, host, node);
      } catch (Exception e) {
        ComponentUtils.handle(scopedContext, e);
      }
    } else {
      super.onInitializeAccessibilityNodeInfo(host, node);
    }

    // If an accessibilityRole has been set, set the className here.  It's important that this
    // happens *after* any calls to super, since the super call will set a className of its own and
    // override this one.
    if (mNodeInfo != null && mNodeInfo.getAccessibilityRole() != null) {
      node.setClassName(mNodeInfo.getAccessibilityRole());
    }

    if (mNodeInfo != null && mNodeInfo.getAccessibilityRoleDescription() != null) {
      node.setRoleDescription(mNodeInfo.getAccessibilityRoleDescription());

      // if no role was explicitly specified, set a role of "NONE".  This allows the role
      // description to still be announced without changing any other behavior.
      if (mNodeInfo.getAccessibilityRole() == null) {
        node.setClassName(AccessibilityRole.NONE);
      }
    }

    if (mNodeInfo != null
        && mNodeInfo.getAccessibilityHeadingState() != NodeInfo.ACCESSIBILITY_HEADING_UNSET) {
      node.setHeading(
          mNodeInfo.getAccessibilityHeadingState() == NodeInfo.ACCESSIBILITY_HEADING_SET_TRUE);
    }
  }

  @Override
  protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
    final MountItem mountItem = getAccessibleMountItem(mView);
    if (mountItem == null) {
      return;
    }

    final LayoutOutput layoutOutput = getLayoutOutput(mountItem);
    final Component component = layoutOutput.getComponent();
    final ComponentContext scopedContext = getComponentContext(mountItem);

    try {
      final int extraAccessibilityNodesCount =
          component.getExtraAccessibilityNodesCount(scopedContext);

      // Expose extra accessibility nodes declared by the component to the
      // accessibility framework. The actual nodes will be populated in
      // {@link #onPopulateNodeForVirtualView}.
      for (int i = 0; i < extraAccessibilityNodesCount; i++) {
        virtualViewIds.add(i);
      }
    } catch (Exception e) {
      ComponentUtils.handle(scopedContext, e);
    }
  }

  @Override
  protected void onPopulateNodeForVirtualView(int virtualViewId, AccessibilityNodeInfoCompat node) {
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

    final LayoutOutput layoutOutput = getLayoutOutput(mountItem);
    final Component component = layoutOutput.getComponent();
    final ComponentContext scopedContext = getComponentContext(mountItem);

    node.setClassName(component.getClass().getName());

    try {
      if (virtualViewId >= component.getExtraAccessibilityNodesCount(scopedContext)) {
        Log.e(TAG, "Received unrecognized virtual view id: " + virtualViewId);

        // ExploreByTouchHelper insists that we set something.
        node.setContentDescription("");
        node.setBoundsInParent(getDefaultBounds());
        return;
      }

      component.onPopulateExtraAccessibilityNode(
          scopedContext, node, virtualViewId, bounds.left, bounds.top);
    } catch (Exception e) {
      ComponentUtils.handle(scopedContext, e);
    }
  }

  /**
   * Finds extra accessibility nodes under the given event coordinates. Returns {@link #INVALID_ID}
   * otherwise.
   */
  @Override
  protected int getVirtualViewAt(float x, float y) {
    final MountItem mountItem = getAccessibleMountItem(mView);
    if (mountItem == null) {
      return INVALID_ID;
    }

    final LayoutOutput layoutOutput = getLayoutOutput(mountItem);
    final Component component = layoutOutput.getComponent();
    final ComponentContext scopedContext = getComponentContext(mountItem);

    try {
      if (component.getExtraAccessibilityNodesCount(scopedContext) == 0) {
        return INVALID_ID;
      }

      final Drawable drawable = (Drawable) mountItem.getContent();
      final Rect bounds = drawable.getBounds();

      // Try to find an extra accessibility node that intersects with
      // the given coordinates.
      final int virtualViewId =
          component.getExtraAccessibilityNodeAt(
              scopedContext, (int) x - bounds.left, (int) y - bounds.top);

      return (virtualViewId >= 0 ? virtualViewId : INVALID_ID);
    } catch (Exception e) {
      ComponentUtils.handle(scopedContext, e);
      return INVALID_ID;
    }
  }

  @Override
  protected void onPopulateEventForVirtualView(int virtualViewId, AccessibilityEvent event) {
    // TODO (T10543861): ExploreByTouchHelper enforces subclasses to set a content description
    // or text on new events but components don't provide APIs to do so yet.
    event.setContentDescription("");
  }

  @Override
  protected boolean onPerformActionForVirtualView(int virtualViewId, int action, Bundle arguments) {
    return false;
  }

  /**
   * Returns a {AccessibilityNodeProviderCompat} if the host contains a component that implements
   * custom accessibility logic. Returns {@code NULL} otherwise. Components with accessibility
   * content are automatically wrapped in hosts by {@link LayoutState}.
   */
  @Override
  public @Nullable AccessibilityNodeProviderCompat getAccessibilityNodeProvider(View host) {
    final MountItem mountItem = getAccessibleMountItem(mView);
    if (mountItem != null
        && getLayoutOutput(mountItem).getComponent().implementsExtraAccessibilityNodes()) {
      return super.getAccessibilityNodeProvider(host);
    }

    return null;
  }

  private static @Nullable MountItem getAccessibleMountItem(View view) {
    if (!(view instanceof ComponentHost)) {
      return null;
    }

    return ((ComponentHost) view).getAccessibleMountItem();
  }

  @Override
  public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
    if (mNodeInfo != null && mNodeInfo.getOnInitializeAccessibilityEventHandler() != null) {
      EventDispatcherUtils.dispatchOnInitializeAccessibilityEvent(
          mNodeInfo.getOnInitializeAccessibilityEventHandler(), host, event, mSuperDelegate);
    } else {
      super.onInitializeAccessibilityEvent(host, event);
    }
  }

  @Override
  public void sendAccessibilityEvent(View host, int eventType) {
    if (mNodeInfo != null && mNodeInfo.getSendAccessibilityEventHandler() != null) {
      EventDispatcherUtils.dispatchSendAccessibilityEvent(
          mNodeInfo.getSendAccessibilityEventHandler(), host, eventType, mSuperDelegate);
    } else {
      super.sendAccessibilityEvent(host, eventType);
    }
  }

  @Override
  public void sendAccessibilityEventUnchecked(View host, AccessibilityEvent event) {
    if (mNodeInfo != null && mNodeInfo.getSendAccessibilityEventUncheckedHandler() != null) {
      EventDispatcherUtils.dispatchSendAccessibilityEventUnchecked(
          mNodeInfo.getSendAccessibilityEventUncheckedHandler(), host, event, mSuperDelegate);
    } else {
      super.sendAccessibilityEventUnchecked(host, event);
    }
  }

  @Override
  public boolean dispatchPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
    if (mNodeInfo != null && mNodeInfo.getDispatchPopulateAccessibilityEventHandler() != null) {
      return EventDispatcherUtils.dispatchDispatchPopulateAccessibilityEvent(
          mNodeInfo.getDispatchPopulateAccessibilityEventHandler(), host, event, mSuperDelegate);
    }

    return super.dispatchPopulateAccessibilityEvent(host, event);
  }

  @Override
  public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
    if (mNodeInfo != null && mNodeInfo.getOnPopulateAccessibilityEventHandler() != null) {
      EventDispatcherUtils.dispatchOnPopulateAccessibilityEvent(
          mNodeInfo.getOnPopulateAccessibilityEventHandler(), host, event, mSuperDelegate);
    } else {
      super.onPopulateAccessibilityEvent(host, event);
    }
  }

  @Override
  public boolean onRequestSendAccessibilityEvent(
      ViewGroup host, View child, AccessibilityEvent event) {
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
          mNodeInfo.getPerformAccessibilityActionHandler(), host, action, args, mSuperDelegate);
    }

    return super.performAccessibilityAction(host, action, args);
  }

  private static Rect getDefaultBounds() {
    return sDefaultBounds;
  }

  private class SuperDelegate extends AccessibilityDelegateCompat {

    @Override
    public boolean dispatchPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
      return ComponentAccessibilityDelegate.super.dispatchPopulateAccessibilityEvent(host, event);
    }

    @Override
    public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
      ComponentAccessibilityDelegate.super.onInitializeAccessibilityEvent(host, event);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat node) {
      ComponentAccessibilityDelegate.super.onInitializeAccessibilityNodeInfo(host, node);
    }

    @Override
    public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
      ComponentAccessibilityDelegate.super.onPopulateAccessibilityEvent(host, event);
    }

    @Override
    public boolean onRequestSendAccessibilityEvent(
        ViewGroup host, View child, AccessibilityEvent event) {
      return ComponentAccessibilityDelegate.super.onRequestSendAccessibilityEvent(
          host, child, event);
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
    public void sendAccessibilityEventUnchecked(View host, AccessibilityEvent event) {
      ComponentAccessibilityDelegate.super.sendAccessibilityEventUnchecked(host, event);
    }
  }
}
