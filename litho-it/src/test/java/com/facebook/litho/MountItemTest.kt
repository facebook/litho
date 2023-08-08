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
import android.graphics.Rect
import android.util.SparseArray
import android.view.View
import androidx.core.view.ViewCompat
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.facebook.litho.MountSpecLithoRenderUnit.Companion.STATE_UNKNOWN
import com.facebook.litho.testing.TestDrawableComponent
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.SimpleMountSpecTester
import com.facebook.rendercore.MountItem
import com.facebook.rendercore.RenderTreeNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Tests [MountItem] */
@RunWith(LithoTestRunner::class)
class MountItemTest {

  private lateinit var mountItem: MountItem
  private lateinit var component: SpecGeneratedComponent
  private lateinit var componentHost: ComponentHost
  private lateinit var content: Any
  private lateinit var contentDescription: CharSequence
  private lateinit var viewTag: Any
  private lateinit var viewTags: SparseArray<Any>
  private lateinit var clickHandler: EventHandler<ClickEvent>
  private lateinit var longClickHandler: EventHandler<LongClickEvent>
  private lateinit var focusChangeHandler: EventHandler<FocusChangedEvent>
  private lateinit var touchHandler: EventHandler<TouchEvent>
  private lateinit var dispatchPopulateAccessibilityEventHandler:
      EventHandler<DispatchPopulateAccessibilityEventEvent>
  private var flags = 0
  private lateinit var context: ComponentContext
  private lateinit var nodeInfo: NodeInfo

  @Before
  fun setup() {
    context = ComponentContext(getApplicationContext<Context>())
    component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component =
              SimpleMountSpecTester.create(c).build()
        }
    componentHost = ComponentHost(getApplicationContext<Context>())
    content = View(getApplicationContext())
    contentDescription = "contentDescription"
    viewTag = "tag"
    viewTags = SparseArray()
    clickHandler = EventHandler<ClickEvent>(component, 5)
    longClickHandler = EventHandler<LongClickEvent>(component, 3)
    focusChangeHandler = EventHandler<FocusChangedEvent>(component, 9)
    touchHandler = EventHandler<TouchEvent>(component, 1)
    dispatchPopulateAccessibilityEventHandler =
        EventHandler<DispatchPopulateAccessibilityEventEvent>(component, 7)
    flags = 114
    nodeInfo = NodeInfo()
    nodeInfo.contentDescription = contentDescription
    nodeInfo.clickHandler = clickHandler
    nodeInfo.longClickHandler = longClickHandler
    nodeInfo.focusChangeHandler = focusChangeHandler
    nodeInfo.touchHandler = touchHandler
    nodeInfo.viewTag = viewTag
    nodeInfo.viewTags = viewTags
    mountItem = create(content)
  }

  private fun create(content: Any): MountItem =
      MountItemTestHelper.create(
          component, content, nodeInfo, null, flags, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES)

  @Test
  fun testIsBound() {
    mountItem.setIsBound(true)
    assertThat(mountItem.isBound).isTrue
    mountItem.setIsBound(false)
    assertThat(mountItem.isBound).isFalse
  }

  @Test
  fun testGetters() {
    assertThat(LithoRenderUnit.getRenderUnit(mountItem).component).isSameAs(component)
    assertThat(mountItem.content).isSameAs(content)
    assertThat(LithoRenderUnit.getRenderUnit(mountItem).nodeInfo?.contentDescription)
        .isSameAs(contentDescription)
    assertThat(LithoRenderUnit.getRenderUnit(mountItem).nodeInfo?.clickHandler)
        .isSameAs(clickHandler)
    assertThat(LithoRenderUnit.getRenderUnit(mountItem).nodeInfo?.focusChangeHandler)
        .isSameAs(focusChangeHandler)
    assertThat(LithoRenderUnit.getRenderUnit(mountItem).nodeInfo?.touchHandler)
        .isSameAs(touchHandler)
    assertThat(LithoRenderUnit.getRenderUnit(mountItem).flags).isEqualTo(flags)
    assertThat(LithoRenderUnit.getRenderUnit(mountItem).importantForAccessibility)
        .isEqualTo(ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES)
  }

  @Test
  fun testFlags() {
    flags =
        (LithoRenderUnit.LAYOUT_FLAG_DUPLICATE_PARENT_STATE or
            LithoRenderUnit.LAYOUT_FLAG_DISABLE_TOUCHABLE)
    mountItem = create(content)
    assertThat(
            LithoRenderUnit.isDuplicateParentState(LithoRenderUnit.getRenderUnit(mountItem).flags))
        .isTrue
    assertThat(LithoRenderUnit.isTouchableDisabled(LithoRenderUnit.getRenderUnit(mountItem).flags))
        .isTrue
    flags = 0
    mountItem = create(content)
    assertThat(
            LithoRenderUnit.isDuplicateParentState(LithoRenderUnit.getRenderUnit(mountItem).flags))
        .isFalse
    assertThat(LithoRenderUnit.isTouchableDisabled(LithoRenderUnit.getRenderUnit(mountItem).flags))
        .isFalse
  }

  @Test
  fun testViewFlags() {
    val view = View(getApplicationContext())
    view.isClickable = true
    view.isEnabled = true
    view.isLongClickable = true
    view.isFocusable = false
    view.isSelected = false
    mountItem = create(view)
    assertThat(
            LithoMountData.isViewClickable(
                LithoMountData.getMountData(mountItem).defaultAttributeValuesFlags))
        .isTrue
    assertThat(
            LithoMountData.isViewEnabled(
                LithoMountData.getMountData(mountItem).defaultAttributeValuesFlags))
        .isTrue
    assertThat(
            LithoMountData.isViewLongClickable(
                LithoMountData.getMountData(mountItem).defaultAttributeValuesFlags))
        .isTrue
    assertThat(
            LithoMountData.isViewFocusable(
                LithoMountData.getMountData(mountItem).defaultAttributeValuesFlags))
        .isFalse
    assertThat(
            LithoMountData.isViewSelected(
                LithoMountData.getMountData(mountItem).defaultAttributeValuesFlags))
        .isFalse
    view.isClickable = false
    view.isEnabled = false
    view.isLongClickable = false
    view.isFocusable = true
    view.isSelected = true
    mountItem = create(view)
    assertThat(
            LithoMountData.isViewClickable(
                LithoMountData.getMountData(mountItem).defaultAttributeValuesFlags))
        .isFalse
    assertThat(
            LithoMountData.isViewEnabled(
                LithoMountData.getMountData(mountItem).defaultAttributeValuesFlags))
        .isFalse
    assertThat(
            LithoMountData.isViewLongClickable(
                LithoMountData.getMountData(mountItem).defaultAttributeValuesFlags))
        .isFalse
    assertThat(
            LithoMountData.isViewFocusable(
                LithoMountData.getMountData(mountItem).defaultAttributeValuesFlags))
        .isTrue
    assertThat(
            LithoMountData.isViewSelected(
                LithoMountData.getMountData(mountItem).defaultAttributeValuesFlags))
        .isTrue
  }

  @Test
  fun testIsAccessibleWithNullComponent() {
    val mountItem = create(content)
    assertThat(LithoRenderUnit.getRenderUnit(mountItem).isAccessible).isFalse
  }

  @Test
  fun testIsAccessibleWithAccessibleComponent() {
    val mountItem: MountItem =
        MountItemTestHelper.create(
            TestDrawableComponent.create(context, true, true, true /* implementsAccessibility */)
                .build(),
            content,
            nodeInfo,
            null,
            flags,
            ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO)
    assertThat(LithoRenderUnit.getRenderUnit(mountItem).isAccessible).isTrue
  }

  @Test
  fun testIsAccessibleWithDisabledAccessibleComponent() {
    val mountItem: MountItem =
        MountItemTestHelper.create(
            TestDrawableComponent.create(context, true, true, true /* implementsAccessibility */)
                .build(),
            content,
            nodeInfo,
            null,
            flags,
            ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO)
    assertThat(LithoRenderUnit.getRenderUnit(mountItem).isAccessible).isFalse
  }

  @Test
  fun testIsAccessibleWithAccessibilityEventHandler() {
    val mountItem: MountItem =
        MountItemTestHelper.create(
            TestDrawableComponent.create(context, true, true, true /* implementsAccessibility */)
                .build(),
            content,
            nodeInfo,
            null,
            flags,
            ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO)
    assertThat(LithoRenderUnit.getRenderUnit(mountItem).isAccessible).isTrue
  }

  @Test
  fun testIsAccessibleWithNonAccessibleComponent() {
    assertThat(LithoRenderUnit.getRenderUnit(mountItem).isAccessible).isFalse
  }

  @Test
  fun testUpdateDoesntChangeFlags() {
    val unit: LithoRenderUnit =
        MountSpecLithoRenderUnit.create(
            0, component, null, context, nodeInfo, 0, 0, STATE_UNKNOWN, null)
    val node: RenderTreeNode = create(unit, Rect(0, 0, 0, 0), null, null)
    val view = View(getApplicationContext())
    val mountItem = MountItem(node, view)
    mountItem.setMountData(LithoMountData(view))
    assertThat(
            LithoMountData.isViewClickable(
                LithoMountData.getMountData(mountItem).defaultAttributeValuesFlags))
        .isFalse
    view.isClickable = true
    mountItem.update(node)
    assertThat(
            LithoMountData.isViewClickable(
                LithoMountData.getMountData(mountItem).defaultAttributeValuesFlags))
        .isFalse
  }
}
