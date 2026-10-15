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

import android.content.Context
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.facebook.rendercore.text.RCTextView
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class ViewAttributesViewBinderTest {

  @Test
  fun `bind sets the view attributes`() {
    val context = RuntimeEnvironment.getApplication() as Context

    val model =
        ViewAttributesViewBinder.Model(
            nodeInfo = null,
            viewAttributes =
                ViewAttributes().apply { contentDescription = "my-content-description" },
            isRootHost = false,
        )

    val view = ComponentHost(context, null)
    ViewAttributesViewBinder.bind(context, view, model, null)
    Assertions.assertThat(view.contentDescription).isEqualTo("my-content-description")
  }

  @Test
  fun `unbind resets previously set view attributes`() {
    val context = RuntimeEnvironment.getApplication() as Context

    val model =
        ViewAttributesViewBinder.Model(
            nodeInfo = null,
            viewAttributes =
                ViewAttributes().apply { contentDescription = "my-content-description" },
            isRootHost = false,
        )

    val view = ComponentHost(context, null)
    val bindData = ViewAttributesViewBinder.bind(context, view, model, null)
    Assertions.assertThat(view.contentDescription).isEqualTo("my-content-description")

    ViewAttributesViewBinder.unbind(context, view, model, null, bindData)
    Assertions.assertThat(view.contentDescription).isNull()
  }

  @Test
  fun `bind and unbind transfer and clear node info`() {
    val context = RuntimeEnvironment.getApplication() as Context
    val nodeInfo = NodeInfo().apply { stateDescription = "state" }
    val model =
        ViewAttributesViewBinder.Model(
            nodeInfo = nodeInfo,
            viewAttributes = ViewAttributes(),
            isRootHost = false,
        )
    val view = View(context)

    val bindData = ViewAttributesViewBinder.bind(context, view, model, null)
    Assertions.assertThat(view.getTag(ComponentHost.COMPONENT_NODE_INFO_ID)).isSameAs(nodeInfo)

    ViewAttributesViewBinder.unbind(context, view, model, null, bindData)
    Assertions.assertThat(view.getTag(ComponentHost.COMPONENT_NODE_INFO_ID)).isNull()
  }

  @Test
  fun `delegate backed node info only changes view behavior when accessibility is enabled`() {
    val context = RuntimeEnvironment.getApplication() as Context
    val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val shadowManager = Shadows.shadowOf(manager)
    val model =
        ViewAttributesViewBinder.Model(
            nodeInfo = NodeInfo().apply { accessibilityRole = AccessibilityRole.BUTTON },
            viewAttributes = ViewAttributes(),
            isRootHost = false,
        )

    try {
      shadowManager.setEnabled(false)
      shadowManager.setTouchExplorationEnabled(false)
      AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()
      val disabledView = View(context)
      val originalDelegate = object : AccessibilityDelegateCompat() {}
      ViewCompat.setAccessibilityDelegate(disabledView, originalDelegate)
      val originalFocusable = disabledView.isFocusable
      val originalClickable = disabledView.isClickable
      val originalImportantForAccessibility = disabledView.importantForAccessibility
      val disabledBindData = ViewAttributesViewBinder.bind(context, disabledView, model, null)

      Assertions.assertThat(ViewCompat.getAccessibilityDelegate(disabledView))
          .isSameAs(originalDelegate)
      Assertions.assertThat(disabledView.isFocusable).isEqualTo(originalFocusable)
      Assertions.assertThat(disabledView.isClickable).isEqualTo(originalClickable)
      Assertions.assertThat(disabledView.importantForAccessibility)
          .isEqualTo(originalImportantForAccessibility)
      ViewAttributesViewBinder.unbind(context, disabledView, model, null, disabledBindData)
      Assertions.assertThat(ViewCompat.getAccessibilityDelegate(disabledView))
          .isSameAs(originalDelegate)
      Assertions.assertThat(disabledView.isFocusable).isEqualTo(originalFocusable)
      Assertions.assertThat(disabledView.isClickable).isEqualTo(originalClickable)
      Assertions.assertThat(disabledView.importantForAccessibility)
          .isEqualTo(originalImportantForAccessibility)

      shadowManager.setEnabled(true)
      shadowManager.setTouchExplorationEnabled(true)
      AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()
      val enabledView = View(context)
      ViewAttributesViewBinder.bind(context, enabledView, model, null)

      val delegate = ViewCompat.getAccessibilityDelegate(enabledView)
      Assertions.assertThat(delegate).isInstanceOf(ComponentAccessibilityDelegate::class.java)
      val nodeInfo = AccessibilityNodeInfoCompat.obtain()
      delegate?.onInitializeAccessibilityNodeInfo(enabledView, nodeInfo)
      Assertions.assertThat(nodeInfo.className).isEqualTo(AccessibilityRole.BUTTON)
    } finally {
      shadowManager.setEnabled(false)
      shadowManager.setTouchExplorationEnabled(false)
      AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()
    }
  }

  @Test
  fun `bind chains and unbind restores the view accessibility delegate`() {
    val context = RuntimeEnvironment.getApplication() as Context
    val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val shadowManager = Shadows.shadowOf(manager)
    val originalDelegate =
        object : AccessibilityDelegateCompat() {
          override fun onInitializeAccessibilityNodeInfo(
              host: View,
              info: AccessibilityNodeInfoCompat,
          ) {
            info.className = "original"
            info.addAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD)
          }
        }
    val model =
        ViewAttributesViewBinder.Model(
            nodeInfo = NodeInfo().apply { stateDescription = "state" },
            viewAttributes = ViewAttributes(),
            isRootHost = false,
        )
    val view = View(context)
    ViewCompat.setAccessibilityDelegate(view, originalDelegate)

    try {
      shadowManager.setEnabled(true)
      shadowManager.setTouchExplorationEnabled(true)
      AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()
      val bindData = ViewAttributesViewBinder.bind(context, view, model, null)

      val lithoDelegate = ViewCompat.getAccessibilityDelegate(view)
      Assertions.assertThat(lithoDelegate).isInstanceOf(ComponentAccessibilityDelegate::class.java)
      val mountedInfo = AccessibilityNodeInfoCompat.obtain()
      lithoDelegate?.onInitializeAccessibilityNodeInfo(view, mountedInfo)
      Assertions.assertThat(mountedInfo.className).isEqualTo("original")
      Assertions.assertThat(mountedInfo.stateDescription).isEqualTo("state")
      Assertions.assertThat(mountedInfo.actionList.map { it.id })
          .contains(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD)

      ViewAttributesViewBinder.unbind(context, view, model, null, bindData)

      val restoredDelegate = ViewCompat.getAccessibilityDelegate(view)
      Assertions.assertThat(restoredDelegate).isSameAs(originalDelegate)
      val unmountedInfo = AccessibilityNodeInfoCompat.obtain()
      restoredDelegate?.onInitializeAccessibilityNodeInfo(view, unmountedInfo)
      Assertions.assertThat(unmountedInfo.className).isEqualTo("original")
      Assertions.assertThat(unmountedInfo.stateDescription).isNull()
      Assertions.assertThat(unmountedInfo.actionList.map { it.id })
          .contains(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD)
    } finally {
      shadowManager.setEnabled(false)
      shadowManager.setTouchExplorationEnabled(false)
      AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()
    }
  }

  @Test
  fun `bind does not create a delegate cycle when the view rewraps Litho`() {
    val context = RuntimeEnvironment.getApplication() as Context
    val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val shadowManager = Shadows.shadowOf(manager)
    val model =
        ViewAttributesViewBinder.Model(
            nodeInfo = NodeInfo().apply { stateDescription = "state" },
            viewAttributes = ViewAttributes(),
            isRootHost = false,
        )
    val view = RCTextView(context)
    val originalDelegate = checkNotNull(ViewCompat.getAccessibilityDelegate(view))

    try {
      shadowManager.setEnabled(true)
      shadowManager.setTouchExplorationEnabled(true)
      AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()
      val bindData = ViewAttributesViewBinder.bind(context, view, model, null)

      val rewrappedDelegate = ViewCompat.getAccessibilityDelegate(view)
      Assertions.assertThat(rewrappedDelegate).isSameAs(originalDelegate)
      val mountedInfo = AccessibilityNodeInfoCompat.obtain()
      rewrappedDelegate?.onInitializeAccessibilityNodeInfo(view, mountedInfo)
      Assertions.assertThat(mountedInfo.stateDescription).isEqualTo("state")

      ViewAttributesViewBinder.unbind(context, view, model, null, bindData)

      val restoredDelegate = ViewCompat.getAccessibilityDelegate(view)
      Assertions.assertThat(restoredDelegate).isSameAs(originalDelegate)
      val unmountedInfo = AccessibilityNodeInfoCompat.obtain()
      restoredDelegate?.onInitializeAccessibilityNodeInfo(view, unmountedInfo)
      Assertions.assertThat(unmountedInfo.stateDescription).isNull()
    } finally {
      shadowManager.setEnabled(false)
      shadowManager.setTouchExplorationEnabled(false)
      AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()
    }
  }

  @Test
  fun `should update if view attributes are different`() {
    val firstModel =
        ViewAttributesViewBinder.Model(
            nodeInfo = null,
            viewAttributes =
                ViewAttributes().apply { contentDescription = "my-content-description" },
            isRootHost = false,
        )

    Assertions.assertThat(ViewAttributesViewBinder.shouldUpdate(firstModel, firstModel, null, null))
        .isFalse()

    val secondModel =
        ViewAttributesViewBinder.Model(
            nodeInfo = null,
            viewAttributes =
                ViewAttributes().apply { contentDescription = "my-different-description" },
            isRootHost = false,
        )

    Assertions.assertThat(
            ViewAttributesViewBinder.shouldUpdate(firstModel, secondModel, null, null),
        )
        .isTrue()
  }

  @Test
  fun `should not update if node info is equivalent`() {
    val firstModel =
        ViewAttributesViewBinder.Model(
            nodeInfo = NodeInfo().apply { stateDescription = "state" },
            viewAttributes = ViewAttributes(),
            isRootHost = false,
        )
    val secondModel =
        ViewAttributesViewBinder.Model(
            nodeInfo = NodeInfo().apply { stateDescription = "state" },
            viewAttributes = ViewAttributes(),
            isRootHost = false,
        )

    Assertions.assertThat(
            ViewAttributesViewBinder.shouldUpdate(firstModel, secondModel, null, null),
        )
        .isFalse()
  }

  @Test
  fun `should update if node info changes or is removed`() {
    val firstModel =
        ViewAttributesViewBinder.Model(
            nodeInfo = NodeInfo().apply { stateDescription = "first" },
            viewAttributes = ViewAttributes(),
            isRootHost = false,
        )
    val changedModel =
        ViewAttributesViewBinder.Model(
            nodeInfo = NodeInfo().apply { stateDescription = "second" },
            viewAttributes = ViewAttributes(),
            isRootHost = false,
        )
    val removedModel =
        ViewAttributesViewBinder.Model(
            nodeInfo = null,
            viewAttributes = ViewAttributes(),
            isRootHost = false,
        )

    Assertions.assertThat(
            ViewAttributesViewBinder.shouldUpdate(firstModel, changedModel, null, null),
        )
        .isTrue()
    Assertions.assertThat(
            ViewAttributesViewBinder.shouldUpdate(firstModel, removedModel, null, null),
        )
        .isTrue()
  }
}
