/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.litho

import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeProviderCompat
import androidx.customview.widget.ExploreByTouchHelper
import com.facebook.litho.LithoLayoutData.Companion.getInterStageProps
import com.facebook.rendercore.MountItem

/**
 * Class that is used to set up accessibility for [ComponentHost]s. Virtual nodes are only exposed
 * if the component implements support for extra accessibility nodes.
 */
internal class ComponentAccessibilityDelegate(
    private val view: View,
    private var nodeInfo: NodeInfo?,
    originalFocus: Boolean,
    originalImportantForAccessibility: Int
) : ExploreByTouchHelper(view) {

  private val superDelegate: AccessibilityDelegateCompat = SuperDelegate()

  init {

    // We need to reset these two properties, as ExploreByTouchHelper sets focusable to "true" and
    // importantForAccessibility to "Yes" (if it is Auto). If we don't reset these it would force
    // every element that has this delegate attached to be focusable, and not allow for
    // announcement coalescing.
    view.isFocusable = originalFocus
    ViewCompat.setImportantForAccessibility(view, originalImportantForAccessibility)
  }

  constructor(
      view: View,
      originalFocus: Boolean,
      originalImportantForAccessibility: Int
  ) : this(view, null, originalFocus, originalImportantForAccessibility)

  /**
   * [ComponentHost] contains the logic for setting the [NodeInfo] containing the [EventHandler]s
   * for its delegate instance whenever it is set/unset
   *
   * @see ComponentHost.setTag
   */
  fun setNodeInfo(nodeInfo: NodeInfo?) {
    this.nodeInfo = nodeInfo
  }

  override fun onInitializeAccessibilityNodeInfo(host: View, node: AccessibilityNodeInfoCompat) {
    val nodeInfo = nodeInfo
    val mountItem = getAccessibleMountItem(view)
    val handler = nodeInfo?.onInitializeAccessibilityNodeInfoHandler
    if (handler != null) {
      EventDispatcherUtils.dispatchOnInitializeAccessibilityNodeInfoEvent(
          handler, host, node, superDelegate)
      dispatchOnPopulateAccessibilityNodeEvent(host, node)
    } else if (mountItem != null) {
      super.onInitializeAccessibilityNodeInfo(host, node)
      // Coalesce the accessible mount item's information with the
      // the root host view's as they are meant to behave as a single
      // node in the accessibility framework.
      val component = LithoRenderUnit.getRenderUnit(mountItem).component
      val scopedContext = LithoRenderUnit.getComponentContext(mountItem.renderTreeNode)
      try {
        dispatchOnPopulateAccessibilityNodeEvent(host, node)
        if (component is SpecGeneratedComponent) {
          component.onPopulateAccessibilityNode(
              scopedContext, host, node, getInterStageProps(mountItem))
        }
      } catch (e: Exception) {
        if (scopedContext != null) {
          ComponentUtils.handle(scopedContext, e)
        }
      }
    } else {
      super.onInitializeAccessibilityNodeInfo(host, node)
    }

    // If an accessibilityRole has been set, set the className here.  It's important that this
    // happens *after* any calls to super, since the super call will set a className of its own and
    // override this one.
    nodeInfo?.accessibilityRole?.let { node.className = it }
    nodeInfo?.accessibilityRoleDescription?.let { description ->
      node.roleDescription = description

      // if no role was explicitly specified, set a role of "NONE".  This allows the role
      // description to still be announced without changing any other behavior.
      if (nodeInfo.accessibilityRole == null) {
        node.className = AccessibilityRole.NONE
      }
    }
    if (nodeInfo != null &&
        nodeInfo.accessibilityHeadingState != NodeInfo.ACCESSIBILITY_HEADING_UNSET) {
      node.isHeading = nodeInfo.accessibilityHeadingState == NodeInfo.ACCESSIBILITY_HEADING_SET_TRUE
    }
  }

  private fun dispatchOnPopulateAccessibilityNodeEvent(
      host: View,
      node: AccessibilityNodeInfoCompat
  ) {
    nodeInfo?.onPopulateAccessibilityNodeHandler?.let {
      EventDispatcherUtils.dispatchOnPopulateAccessibilityNode(it, host, node)
    }
  }

  private fun dispatchOnVirtualViewKeyboardFocusChangedEvent(
      host: View,
      node: AccessibilityNodeInfoCompat?,
      virtualViewId: Int,
      hasFocus: Boolean
  ) {
    nodeInfo?.onVirtualViewKeyboardFocusChangedHandler?.let {
      EventDispatcherUtils.dispatchVirtualViewKeyboardFocusChanged(
          it, host, node, virtualViewId, hasFocus, superDelegate)
    }
  }

  private fun dispatchOnPerformActionForVirtualViewEvent(
      host: View,
      node: AccessibilityNodeInfoCompat,
      virtualViewId: Int,
      action: Int,
      arguments: Bundle?
  ): Boolean =
      when (val handler = nodeInfo?.onPerformActionForVirtualViewHandler) {
        null -> false
        else ->
            EventDispatcherUtils.dispatchPerformActionForVirtualView(
                handler, host, node, virtualViewId, action, arguments)
      }

  override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>) {
    val mountItem = getAccessibleMountItem(view) ?: return
    val renderUnit = LithoRenderUnit.getRenderUnit(mountItem)
    if (renderUnit.component !is SpecGeneratedComponent) {
      return
    }
    val scopedContext = LithoRenderUnit.getComponentContext(mountItem)
    try {
      val extraAccessibilityNodesCount =
          renderUnit.component.getExtraAccessibilityNodesCount(
              scopedContext, getInterStageProps(mountItem))

      // Expose extra accessibility nodes declared by the component to the
      // accessibility framework. The actual nodes will be populated in
      // [onPopulateNodeForVirtualView].
      for (i in 0 until extraAccessibilityNodesCount) {
        virtualViewIds.add(i)
      }
    } catch (e: Exception) {
      if (scopedContext != null) {
        ComponentUtils.handle(scopedContext, e)
      }
    }
  }

  override fun onPopulateNodeForVirtualView(virtualViewId: Int, node: AccessibilityNodeInfoCompat) {
    val mountItem = getAccessibleMountItem(view)
    if (mountItem == null) {
      // ExploreByTouchHelper insists that we set something.
      node.contentDescription = ""
      node.setBoundsInParent(defaultBounds)
      return
    }
    val drawable = mountItem.content as Drawable
    val bounds = drawable.bounds
    val renderUnit = LithoRenderUnit.getRenderUnit(mountItem)
    val component = renderUnit.component as? SpecGeneratedComponent ?: return
    val scopedContext = LithoRenderUnit.getComponentContext(mountItem)
    node.className = component.javaClass.name
    try {
      if (virtualViewId >=
          component.getExtraAccessibilityNodesCount(scopedContext, getInterStageProps(mountItem))) {
        // ExploreByTouchHelper insists that we set something.
        node.contentDescription = ""
        node.setBoundsInParent(defaultBounds)
        return
      }
      component.onPopulateExtraAccessibilityNode(
          scopedContext,
          node,
          virtualViewId,
          bounds.left,
          bounds.top,
          getInterStageProps(mountItem))
    } catch (e: Exception) {
      if (scopedContext != null) {
        ComponentUtils.handle(scopedContext, e)
      }
    }
  }

  /**
   * Finds extra accessibility nodes under the given event coordinates. Returns [INVALID_ID]
   * otherwise.
   */
  override fun getVirtualViewAt(x: Float, y: Float): Int {
    val mountItem = getAccessibleMountItem(view) ?: return INVALID_ID
    val renderUnit = LithoRenderUnit.getRenderUnit(mountItem)
    if (renderUnit.component !is SpecGeneratedComponent) {
      return INVALID_ID
    }
    val component = renderUnit.component
    val scopedContext = LithoRenderUnit.getComponentContext(mountItem)
    return try {
      if (component.getExtraAccessibilityNodesCount(scopedContext, getInterStageProps(mountItem)) ==
          0) {
        return INVALID_ID
      }
      val drawable = mountItem.content as Drawable
      val bounds = drawable.bounds

      // Try to find an extra accessibility node that intersects with
      // the given coordinates.
      val virtualViewId =
          component.getExtraAccessibilityNodeAt(
              scopedContext,
              x.toInt() - bounds.left,
              y.toInt() - bounds.top,
              getInterStageProps(mountItem))
      if (virtualViewId >= 0) virtualViewId else INVALID_ID
    } catch (e: Exception) {
      if (scopedContext != null) {
        ComponentUtils.handle(scopedContext, e)
      }
      INVALID_ID
    }
  }

  override fun onVirtualViewKeyboardFocusChanged(virtualViewId: Int, hasFocus: Boolean) {
    val nodeProvider = getAccessibilityNodeProvider(view) ?: return
    val node = nodeProvider.findFocus(AccessibilityNodeInfoCompat.FOCUS_INPUT)
    val mountItem = getAccessibleMountItem(view) ?: return
    val renderUnit = LithoRenderUnit.getRenderUnit(mountItem)
    if (renderUnit.component !is SpecGeneratedComponent) {
      dispatchOnVirtualViewKeyboardFocusChangedEvent(view, node, virtualViewId, hasFocus)
      return
    }
    val component = renderUnit.component
    val scopedContext = LithoRenderUnit.getComponentContext(mountItem) ?: return
    try {
      if (virtualViewId >=
          component.getExtraAccessibilityNodesCount(scopedContext, getInterStageProps(mountItem))) {
        return
      }
      if (component.implementsKeyboardFocusChangeForVirtualViews()) {
        component.onVirtualViewKeyboardFocusChanged(
            scopedContext, view, node, virtualViewId, hasFocus, getInterStageProps(mountItem))
      }
    } catch (e: Exception) {
      ComponentUtils.handle(scopedContext, e)
    }
  }

  override fun onPopulateEventForVirtualView(virtualViewId: Int, event: AccessibilityEvent) {
    // TODO (T10543861): ExploreByTouchHelper enforces subclasses to set a content description
    // or text on new events but components don't provide APIs to do so yet.
    event.contentDescription = ""
  }

  override fun onPerformActionForVirtualView(
      virtualViewId: Int,
      action: Int,
      arguments: Bundle?
  ): Boolean {
    val nodeProvider = getAccessibilityNodeProvider(view) ?: return false
    val node = nodeProvider.findFocus(AccessibilityNodeInfoCompat.FOCUS_INPUT) ?: return false
    val mountItem = getAccessibleMountItem(view) ?: return false
    val renderUnit = LithoRenderUnit.getRenderUnit(mountItem)
    if (renderUnit.component !is SpecGeneratedComponent) {
      return dispatchOnPerformActionForVirtualViewEvent(
          view, node, virtualViewId, action, arguments)
    }
    val component = renderUnit.component
    val scopedContext = LithoRenderUnit.getComponentContext(mountItem) ?: return false
    try {
      if (virtualViewId >=
          component.getExtraAccessibilityNodesCount(scopedContext, getInterStageProps(mountItem))) {
        return false
      }
      if (component.implementsOnPerformActionForVirtualView()) {
        return component.onPerformActionForVirtualView(
            scopedContext,
            view,
            node,
            virtualViewId,
            action,
            arguments,
            getInterStageProps(mountItem))
      }
    } catch (e: Exception) {
      ComponentUtils.handle(scopedContext, e)
    }
    return false
  }

  /**
   * Returns a {AccessibilityNodeProviderCompat} if the host contains a component that implements
   * custom accessibility logic. Returns `NULL` otherwise. Components with accessibility content are
   * automatically wrapped in hosts by [LayoutState].
   */
  override fun getAccessibilityNodeProvider(host: View): AccessibilityNodeProviderCompat? {
    val mountItem = getAccessibleMountItem(view)
    if (mountItem != null) {
      val component = LithoRenderUnit.getRenderUnit(mountItem).component
      if (component is SpecGeneratedComponent && component.implementsExtraAccessibilityNodes()) {
        return super.getAccessibilityNodeProvider(host)
      }
    }
    return null
  }

  override fun onInitializeAccessibilityEvent(host: View, event: AccessibilityEvent) {
    when (val handler = nodeInfo?.onInitializeAccessibilityEventHandler) {
      null -> super.onInitializeAccessibilityEvent(host, event)
      else ->
          EventDispatcherUtils.dispatchOnInitializeAccessibilityEvent(
              handler, host, event, superDelegate)
    }
  }

  override fun sendAccessibilityEvent(host: View, eventType: Int) {
    when (val handler = nodeInfo?.sendAccessibilityEventHandler) {
      null -> super.sendAccessibilityEvent(host, eventType)
      else ->
          EventDispatcherUtils.dispatchSendAccessibilityEvent(
              handler, host, eventType, superDelegate)
    }
  }

  override fun sendAccessibilityEventUnchecked(host: View, event: AccessibilityEvent) {
    when (val handler = nodeInfo?.sendAccessibilityEventUncheckedHandler) {
      null -> super.sendAccessibilityEventUnchecked(host, event)
      else ->
          EventDispatcherUtils.dispatchSendAccessibilityEventUnchecked(
              handler, host, event, superDelegate)
    }
  }

  override fun dispatchPopulateAccessibilityEvent(host: View, event: AccessibilityEvent): Boolean =
      when (val handler = nodeInfo?.dispatchPopulateAccessibilityEventHandler) {
        null -> super.dispatchPopulateAccessibilityEvent(host, event)
        else ->
            EventDispatcherUtils.dispatchDispatchPopulateAccessibilityEvent(
                handler, host, event, superDelegate)
      }

  override fun onPopulateAccessibilityEvent(host: View, event: AccessibilityEvent) {
    when (val handler = nodeInfo?.onPopulateAccessibilityEventHandler) {
      null -> super.onPopulateAccessibilityEvent(host, event)
      else ->
          EventDispatcherUtils.dispatchOnPopulateAccessibilityEvent(
              handler, host, event, superDelegate)
    }
  }

  override fun onRequestSendAccessibilityEvent(
      host: ViewGroup,
      child: View,
      event: AccessibilityEvent
  ): Boolean =
      when (val handler = nodeInfo?.onRequestSendAccessibilityEventHandler) {
        null -> super.onRequestSendAccessibilityEvent(host, child, event)
        else ->
            EventDispatcherUtils.dispatchOnRequestSendAccessibilityEvent(
                handler, host, child, event, superDelegate)
      }

  override fun performAccessibilityAction(host: View, action: Int, args: Bundle?): Boolean =
      when (val handler = nodeInfo?.performAccessibilityActionHandler) {
        null -> super.performAccessibilityAction(host, action, args)
        else ->
            EventDispatcherUtils.dispatchPerformAccessibilityActionEvent(
                handler, host, action, args, superDelegate)
      }

  private inner class SuperDelegate : AccessibilityDelegateCompat() {
    override fun dispatchPopulateAccessibilityEvent(
        host: View,
        event: AccessibilityEvent
    ): Boolean {
      return super@ComponentAccessibilityDelegate.dispatchPopulateAccessibilityEvent(host, event)
    }

    override fun onInitializeAccessibilityEvent(host: View, event: AccessibilityEvent) {
      super@ComponentAccessibilityDelegate.onInitializeAccessibilityEvent(host, event)
    }

    override fun onInitializeAccessibilityNodeInfo(host: View, node: AccessibilityNodeInfoCompat) {
      super@ComponentAccessibilityDelegate.onInitializeAccessibilityNodeInfo(host, node)
    }

    override fun onPopulateAccessibilityEvent(host: View, event: AccessibilityEvent) {
      super@ComponentAccessibilityDelegate.onPopulateAccessibilityEvent(host, event)
    }

    override fun onRequestSendAccessibilityEvent(
        host: ViewGroup,
        child: View,
        event: AccessibilityEvent
    ): Boolean {
      return super@ComponentAccessibilityDelegate.onRequestSendAccessibilityEvent(
          host, child, event)
    }

    override fun performAccessibilityAction(host: View, action: Int, args: Bundle?): Boolean {
      return super@ComponentAccessibilityDelegate.performAccessibilityAction(host, action, args)
    }

    override fun sendAccessibilityEvent(host: View, eventType: Int) {
      super@ComponentAccessibilityDelegate.sendAccessibilityEvent(host, eventType)
    }

    override fun sendAccessibilityEventUnchecked(host: View, event: AccessibilityEvent) {
      super@ComponentAccessibilityDelegate.sendAccessibilityEventUnchecked(host, event)
    }
  }

  companion object {
    private const val TAG = "ComponentAccessibility"
    private val defaultBounds = Rect(0, 0, 1, 1)

    private fun getAccessibleMountItem(view: View): MountItem? =
        (view as? ComponentHost)?.accessibleMountItem

    @JvmStatic
    fun getInterStageProps(item: MountItem): InterStagePropsContainer? =
        getInterStageProps(item.renderTreeNode.layoutData)
  }
}
