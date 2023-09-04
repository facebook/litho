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
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.SparseArray
import androidx.core.view.ViewCompat
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.Whitebox
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.logging.TestComponentsReporter
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.testing.unspecified
import com.facebook.litho.widget.ComponentWithState
import com.facebook.litho.widget.Text
import com.facebook.yoga.YogaDirection
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(LithoTestRunner::class)
class LithoNodeTest {

  @JvmField @Rule val legacyLithoViewRule = LegacyLithoViewRule()

  private fun acquireInternalNode(): LithoNode {
    val context = legacyLithoViewRule.context
    legacyLithoViewRule
        .attachToWindow()
        .setRootAndSizeSpecSync(Column.create(context).build(), unspecified(0), unspecified(0))
        .measure()
        .layout()
    val root = requireNotNull(legacyLithoViewRule.currentRootNode)
    return root.node
  }

  private val componentsReporter = TestComponentsReporter()

  @Before
  fun setup() {
    ComponentsReporter.provide(componentsReporter)
  }

  @Test
  fun testLayoutDirectionFlag() {
    val node = acquireInternalNode()
    node.layoutDirection(YogaDirection.INHERIT)
    assertThat(isFlagSet(node, "PFLAG_LAYOUT_DIRECTION_IS_SET")).isTrue
    clearFlag(node, "PFLAG_LAYOUT_DIRECTION_IS_SET")
    assertEmptyFlags(node)
  }

  @Test
  fun testAddingAllAttributes() {
    val node = spy(acquireInternalNode())
    val nodeInfo: NodeInfo = mock()
    whenever(node.mutableNodeInfo()).thenReturn(nodeInfo)
    val clickHandler: EventHandler<ClickEvent> = mock()
    val longClickHandler: EventHandler<LongClickEvent> = mock()
    val touchHandler: EventHandler<TouchEvent> = mock()
    val interceptTouchHandler: EventHandler<InterceptTouchEvent> = mock()
    val focusChangedHandler: EventHandler<FocusChangedEvent> = mock()
    val viewTag = Any()
    val viewTags = SparseArray<Any>()
    val dispatchPopulateAccessibilityEventHandler:
        EventHandler<DispatchPopulateAccessibilityEventEvent> =
        mock()
    val onInitializeAccessibilityEventHandler: EventHandler<OnInitializeAccessibilityEventEvent> =
        mock()
    val onInitializeAccessibilityNodeInfoHandler:
        EventHandler<OnInitializeAccessibilityNodeInfoEvent> =
        mock()
    val onPopulateAccessibilityEventHandler: EventHandler<OnPopulateAccessibilityEventEvent> =
        mock()
    val onRequestSendAccessibilityEventHandler: EventHandler<OnRequestSendAccessibilityEventEvent> =
        mock()
    val performAccessibilityActionHandler: EventHandler<PerformAccessibilityActionEvent> = mock()
    val sendAccessibilityEventHandler: EventHandler<SendAccessibilityEventEvent> = mock()
    val sendAccessibilityEventUncheckedHandler: EventHandler<SendAccessibilityEventUncheckedEvent> =
        mock()
    val toBeAppliedInfo = NodeInfo()
    toBeAppliedInfo.clickHandler = clickHandler
    toBeAppliedInfo.focusChangeHandler = focusChangedHandler
    toBeAppliedInfo.longClickHandler = longClickHandler
    toBeAppliedInfo.touchHandler = touchHandler
    toBeAppliedInfo.interceptTouchHandler = interceptTouchHandler
    toBeAppliedInfo.setFocusable(true)
    toBeAppliedInfo.setSelected(false)
    toBeAppliedInfo.setEnabled(false)
    toBeAppliedInfo.setAccessibilityHeading(false)
    toBeAppliedInfo.contentDescription = "test"
    toBeAppliedInfo.viewTag = viewTag
    toBeAppliedInfo.viewTags = viewTags
    toBeAppliedInfo.shadowElevation = 60f
    toBeAppliedInfo.ambientShadowColor = Color.RED
    toBeAppliedInfo.spotShadowColor = Color.BLUE
    toBeAppliedInfo.clipToOutline = false
    toBeAppliedInfo.accessibilityRole = AccessibilityRole.BUTTON
    toBeAppliedInfo.accessibilityRoleDescription = "Test Role Description"
    toBeAppliedInfo.dispatchPopulateAccessibilityEventHandler =
        dispatchPopulateAccessibilityEventHandler
    toBeAppliedInfo.onInitializeAccessibilityEventHandler = onInitializeAccessibilityEventHandler
    toBeAppliedInfo.onInitializeAccessibilityNodeInfoHandler =
        onInitializeAccessibilityNodeInfoHandler
    toBeAppliedInfo.onPopulateAccessibilityEventHandler = onPopulateAccessibilityEventHandler
    toBeAppliedInfo.onRequestSendAccessibilityEventHandler = onRequestSendAccessibilityEventHandler
    toBeAppliedInfo.performAccessibilityActionHandler = performAccessibilityActionHandler
    toBeAppliedInfo.sendAccessibilityEventHandler = sendAccessibilityEventHandler
    toBeAppliedInfo.sendAccessibilityEventUncheckedHandler = sendAccessibilityEventUncheckedHandler
    node.applyNodeInfo(toBeAppliedInfo)
    verify(nodeInfo).clickHandler = clickHandler
    verify(nodeInfo).focusChangeHandler = focusChangedHandler
    verify(nodeInfo).longClickHandler = longClickHandler
    verify(nodeInfo).touchHandler = touchHandler
    verify(nodeInfo).interceptTouchHandler = interceptTouchHandler
    verify(nodeInfo).setFocusable(true)
    verify(nodeInfo).setSelected(false)
    verify(nodeInfo).setEnabled(false)
    verify(nodeInfo).setAccessibilityHeading(false)
    verify(nodeInfo).contentDescription = "test"
    verify(nodeInfo).viewTag = viewTag
    verify(nodeInfo).viewTags = viewTags
    verify(nodeInfo).shadowElevation = 60f
    verify(nodeInfo).ambientShadowColor = Color.RED
    verify(nodeInfo).spotShadowColor = Color.BLUE
    verify(nodeInfo).clipToOutline = false
    verify(nodeInfo).accessibilityRole = AccessibilityRole.BUTTON
    verify(nodeInfo).accessibilityRoleDescription = "Test Role Description"
    verify(nodeInfo).dispatchPopulateAccessibilityEventHandler =
        dispatchPopulateAccessibilityEventHandler
    verify(nodeInfo).onInitializeAccessibilityEventHandler = onInitializeAccessibilityEventHandler
    verify(nodeInfo).onInitializeAccessibilityNodeInfoHandler =
        onInitializeAccessibilityNodeInfoHandler
    verify(nodeInfo).onPopulateAccessibilityEventHandler = onPopulateAccessibilityEventHandler
    verify(nodeInfo).onRequestSendAccessibilityEventHandler = onRequestSendAccessibilityEventHandler
    verify(nodeInfo).performAccessibilityActionHandler = performAccessibilityActionHandler
    verify(nodeInfo).sendAccessibilityEventHandler = sendAccessibilityEventHandler
    verify(nodeInfo).sendAccessibilityEventUncheckedHandler = sendAccessibilityEventUncheckedHandler
  }

  @Test
  fun testApplyNodeInfo() {
    val node = acquireInternalNode()
    assertThat(node.nodeInfo == null).describedAs("Should be NULL at the beginning").isTrue
    val toBeAppliedInfo = NodeInfo()
    toBeAppliedInfo.alpha = 0.5f
    toBeAppliedInfo.setEnabled(true)
    toBeAppliedInfo.setClickable(true)
    node.applyNodeInfo(toBeAppliedInfo)
    assertThat(node.nodeInfo === toBeAppliedInfo)
        .describedAs("Should be the same instance as passing")
        .isTrue
    assertThat(node.nodeInfo?.alpha == 0.5f)
        .describedAs("Properties should be copied into the NodeInfo of the LithoNode")
        .isTrue
    assertThat(node.nodeInfo?.enabledState == NodeInfo.ENABLED_SET_TRUE)
        .describedAs("Properties should be copied into the NodeInfo of the LithoNode")
        .isTrue
    assertThat(node.nodeInfo?.clickableState == NodeInfo.CLICKABLE_SET_TRUE)
        .describedAs("Properties should be copied into the NodeInfo of the LithoNode")
        .isTrue
    val toBeAppliedInfo2 = NodeInfo()
    toBeAppliedInfo2.scale = 1.0f
    toBeAppliedInfo2.setSelected(true)
    toBeAppliedInfo2.setClickable(false)
    node.applyNodeInfo(toBeAppliedInfo2)
    assertThat(node.nodeInfo?.alpha == 0.5f)
        .describedAs("Properties should not be overridden")
        .isTrue
    assertThat(node.nodeInfo?.enabledState == NodeInfo.ENABLED_SET_TRUE)
        .describedAs("Properties should not be overridden")
        .isTrue
    assertThat(node.nodeInfo?.clickableState == NodeInfo.CLICKABLE_SET_FALSE)
        .describedAs("Properties should be overridden")
        .isTrue
    assertThat(node.nodeInfo?.scale == 1.0f).describedAs("Properties should be merged").isTrue
    assertThat(node.nodeInfo?.selectedState == NodeInfo.SELECTED_SET_TRUE)
        .describedAs("Properties should be merged")
        .isTrue
    node.applyNodeInfo(null)
    assertThat(node.nodeInfo != null)
        .describedAs("Should be the same after merging a NULL object")
        .isTrue
  }

  @Test
  fun testMutableNodeInfo() {
    val node = acquireInternalNode()
    val nodeInfo1 = node.mutableNodeInfo()
    assertThat(nodeInfo1 != null).describedAs("Should never return NULL").isTrue
    assertThat(node.nodeInfo === nodeInfo1)
        .describedAs("Should be the same instance as getNodeInfo returned")
        .isTrue
    val nodeInfo2 = node.mutableNodeInfo()
    assertThat(nodeInfo2 === nodeInfo1)
        .describedAs("Should return the same instance no matter how many times it is called")
        .isTrue
    val initNodeIfo = NodeInfo()
    initNodeIfo.alpha = 0.5f
    initNodeIfo.setEnabled(true)
    initNodeIfo.setClickable(true)
    node.applyNodeInfo(initNodeIfo)
    val mutableNodeInfo = node.mutableNodeInfo()
    assertThat(initNodeIfo !== mutableNodeInfo)
        .describedAs("Should return a copy of the initNodeInfo")
        .isTrue
    assertThat(mutableNodeInfo.alpha == 0.5f)
        .describedAs("Properties should be transferred into a copy of the initNodeInfo")
        .isTrue
    assertThat(mutableNodeInfo.enabledState == NodeInfo.ENABLED_SET_TRUE)
        .describedAs("Properties should be transferred into a copy of the initNodeInfo")
        .isTrue
    assertThat(mutableNodeInfo.clickableState == NodeInfo.CLICKABLE_SET_TRUE)
        .describedAs("Properties should be transferred into a copy of the initNodeInfo")
        .isTrue
    mutableNodeInfo.scale = 1.0f
    assertThat(node.nodeInfo != null)
        .describedAs("Should be the same state as mutableNodeInfo")
        .isTrue
    assertThat(node.nodeInfo?.scale == 1.0f)
        .describedAs("Property of the nodeInfo should also be updated")
        .isTrue
  }

  @Test
  fun testImportantForAccessibilityFlag() {
    val node = acquireInternalNode()
    node.importantForAccessibility(ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO)
    assertThat(isFlagSet(node, "PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET")).isTrue
    clearFlag(node, "PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET")
    assertEmptyFlags(node)
  }

  @Test
  fun testBackgroundFlag() {
    val node = acquireInternalNode()
    node.backgroundColor(0xFFFF0000.toInt())
    assertThat(isFlagSet(node, "PFLAG_BACKGROUND_IS_SET")).isTrue
    clearFlag(node, "PFLAG_BACKGROUND_IS_SET")
    assertEmptyFlags(node)
  }

  @Test
  fun testForegroundFlag() {
    val node = acquireInternalNode()
    node.foregroundColor(0xFFFF0000.toInt())
    assertThat(isFlagSet(node, "PFLAG_FOREGROUND_IS_SET")).isTrue
    clearFlag(node, "PFLAG_FOREGROUND_IS_SET")
    assertEmptyFlags(node)
  }

  @Test
  fun testTransitionKeyFlag() {
    val node = acquireInternalNode()
    node.transitionKey("key", "")
    assertThat(isFlagSet(node, "PFLAG_TRANSITION_KEY_IS_SET")).isTrue
    clearFlag(node, "PFLAG_TRANSITION_KEY_IS_SET")
    assertEmptyFlags(node)
  }

  @Test
  fun testCopyIntoNodeSetFlags() {
    val orig = acquireNestedTreeHolder()
    val dest = acquireInternalNode()
    orig.importantForAccessibility(0)
    orig.duplicateParentState(true)
    orig.background(ColorDrawable())
    orig.foreground(null)
    orig.visibleHandler(null)
    orig.focusedHandler(null)
    orig.fullImpressionHandler(null)
    orig.invisibleHandler(null)
    orig.unfocusedHandler(null)
    orig.visibilityChangedHandler(null)
    orig.transferInto(dest)
    assertThat(isFlagSet(dest, "PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET")).isTrue
    assertThat(isFlagSet(dest, "PFLAG_BACKGROUND_IS_SET")).isTrue
    assertThat(isFlagSet(dest, "PFLAG_FOREGROUND_IS_SET")).isTrue
    assertThat(isFlagSet(dest, "PFLAG_VISIBLE_HANDLER_IS_SET")).isTrue
    assertThat(isFlagSet(dest, "PFLAG_FOCUSED_HANDLER_IS_SET")).isTrue
    assertThat(isFlagSet(dest, "PFLAG_FULL_IMPRESSION_HANDLER_IS_SET")).isTrue
    assertThat(isFlagSet(dest, "PFLAG_INVISIBLE_HANDLER_IS_SET")).isTrue
    assertThat(isFlagSet(dest, "PFLAG_UNFOCUSED_HANDLER_IS_SET")).isTrue
    assertThat(isFlagSet(dest, "PFLAG_VISIBLE_RECT_CHANGED_HANDLER_IS_SET")).isTrue
  }

  @Test
  fun testComponentCreateAndRetrieveCachedLayoutLS_measure() {
    val baseContext = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val c =
        ComponentContextUtils.withComponentTree(
            baseContext, ComponentTree.create(baseContext).build())
    val resolveContext = c.setRenderStateContextForTests()
    val resultCache = resolveContext.cache
    val unspecifiedSizeSpec = unspecified(0)
    val exactSizeSpec = exactly(50)
    val textComponent = Text.create(c).textSizePx(16).text("test").build()
    val textSize = Size()
    textComponent.measure(c, exactSizeSpec, unspecifiedSizeSpec, textSize)
    assertThat(resultCache.getCachedResult(textComponent)).isNotNull
    val cachedLayout = resultCache.getCachedResult(textComponent)
    assertThat(cachedLayout).isNotNull
    assertThat(cachedLayout?.widthSpec).isEqualTo(exactSizeSpec)
    assertThat(cachedLayout?.heightSpec).isEqualTo(unspecifiedSizeSpec)
    resultCache.clearCache(textComponent)
    assertThat(resultCache.getCachedResult(textComponent)).isNull()
  }

  @Test
  fun testComponentCreateAndRetrieveCachedLayoutLS_measureMightNotCacheInternalNode() {
    val baseContext = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val c =
        ComponentContextUtils.withComponentTree(
            baseContext, ComponentTree.create(baseContext).build())
    val resolveContext = c.setRenderStateContextForTests()
    val resultCache = resolveContext.cache
    val unspecifiedSizeSpec = unspecified(0)
    val exactSizeSpec = exactly(50)
    val textComponent = Text.create(c).textSizePx(16).text("test").build()
    val textSize = Size()
    textComponent.measureMightNotCacheInternalNode(c, exactSizeSpec, unspecifiedSizeSpec, textSize)
    assertThat(resultCache.getCachedResult(textComponent)).isNotNull
    val cachedLayout = resultCache.getCachedResult(textComponent)
    assertThat(cachedLayout).isNotNull
    assertThat(cachedLayout?.widthSpec).isEqualTo(exactSizeSpec)
    assertThat(cachedLayout?.heightSpec).isEqualTo(unspecifiedSizeSpec)
    resultCache.clearCache(textComponent)
    assertThat(resultCache.getCachedResult(textComponent)).isNull()
  }

  @Test
  fun testMeasureMightNotCacheInternalNode_ContextWithoutStateHandler_returnsMeasurement() {
    if (ComponentsConfiguration.enableLayoutCaching) {
      return
    }
    val c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    c.setRenderStateContextForTests()
    val component =
        Column.create(c)
            .child(Text.create(c).textSizePx(16).text("test"))
            .child(ComponentWithState.create(c))
            .build()
    val textSize = Size()
    component.measureMightNotCacheInternalNode(c, exactly(50), unspecified(0), textSize)
    assertThat(textSize.width).isEqualTo(50)
    assertThat(textSize.height).isGreaterThan(0)
  }

  @Test
  fun testMeasureMightNotCacheInternalNode_ContextWithoutLayoutStateContextOrStateHandler_returnsMeasurement() {
    if (ComponentsConfiguration.enableLayoutCaching) {
      return
    }
    val c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    c.setRenderStateContextForTests()
    val component =
        Column.create(c)
            .child(Text.create(c).textSizePx(16).text("test"))
            .child(ComponentWithState.create(c))
            .build()
    val textSize = Size()
    component.measureMightNotCacheInternalNode(c, exactly(50), unspecified(0), textSize)
    assertThat(textSize.width).isEqualTo(50)
    assertThat(textSize.height).isGreaterThan(0)
  }

  companion object {
    private fun acquireNestedTreeHolder(): NestedTreeHolder = NestedTreeHolder(null)

    private fun isFlagSet(node: LithoNode, flagName: String): Boolean {
      val flagPosition = Whitebox.getInternalState<Long>(LithoNode::class.java, flagName)
      val flags = Whitebox.getInternalState<Long>(node, "mPrivateFlags")
      return flags and flagPosition != 0L
    }

    private fun clearFlag(node: LithoNode, flagName: String) {
      val flagPosition = Whitebox.getInternalState<Long>(LithoNode::class.java, flagName)
      var flags = Whitebox.getInternalState<Long>(node, "mPrivateFlags")
      flags = flags and flagPosition.inv()
      Whitebox.setInternalState(node, "mPrivateFlags", flags)
    }

    private fun assertEmptyFlags(node: LithoNode) {
      assertThat(Whitebox.getInternalState<Any>(node, "mPrivateFlags") as Long).isEqualTo(0L)
    }
  }
}
