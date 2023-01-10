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

import android.animation.StateListAnimator
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.SparseArray
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.annotations.ImportantForAccessibility
import com.facebook.litho.drawable.ComparableColorDrawable
import com.facebook.litho.it.R.drawable.background_with_padding
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.TestViewComponent
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.Text
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaDirection
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaPositionType
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@RunWith(LithoTestRunner::class)
class CommonPropsTest {

  @JvmField @Rule val legacyLithoViewRule: LegacyLithoViewRule = LegacyLithoViewRule()
  private lateinit var node: LithoNode
  private lateinit var commonProps: CommonProps
  private lateinit var componentContext: ComponentContext

  @Before
  fun setup() {
    node = mock()
    commonProps = CommonProps()
    componentContext = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
  }

  @Test
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  fun testSetPropsAndBuild() {
    commonProps.layoutDirection(YogaDirection.INHERIT)
    commonProps.alignSelf(YogaAlign.AUTO)
    commonProps.positionType(YogaPositionType.ABSOLUTE)
    commonProps.flex(2f)
    commonProps.flexGrow(3f)
    commonProps.flexShrink(4f)
    commonProps.flexBasisPx(5)
    commonProps.flexBasisPercent(6f)
    commonProps.importantForAccessibility(
        ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_AUTO)
    commonProps.duplicateParentState(false)
    commonProps.marginPx(YogaEdge.ALL, 5)
    commonProps.marginPx(YogaEdge.RIGHT, 6)
    commonProps.marginPx(YogaEdge.LEFT, 4)
    commonProps.marginPercent(YogaEdge.ALL, 10f)
    commonProps.marginPercent(YogaEdge.VERTICAL, 12f)
    commonProps.marginPercent(YogaEdge.RIGHT, 5f)
    commonProps.marginAuto(YogaEdge.LEFT)
    commonProps.marginAuto(YogaEdge.TOP)
    commonProps.marginAuto(YogaEdge.RIGHT)
    commonProps.marginAuto(YogaEdge.BOTTOM)
    commonProps.paddingPx(YogaEdge.ALL, 1)
    commonProps.paddingPx(YogaEdge.RIGHT, 2)
    commonProps.paddingPx(YogaEdge.LEFT, 3)
    commonProps.paddingPercent(YogaEdge.VERTICAL, 7f)
    commonProps.paddingPercent(YogaEdge.RIGHT, 6f)
    commonProps.paddingPercent(YogaEdge.ALL, 5f)
    commonProps.border(Border.create(componentContext).build())
    commonProps.positionPx(YogaEdge.ALL, 11)
    commonProps.positionPx(YogaEdge.RIGHT, 12)
    commonProps.positionPx(YogaEdge.LEFT, 13)
    commonProps.positionPercent(YogaEdge.VERTICAL, 17f)
    commonProps.positionPercent(YogaEdge.RIGHT, 16f)
    commonProps.positionPercent(YogaEdge.ALL, 15f)
    commonProps.widthPx(5)
    commonProps.widthPercent(50f)
    commonProps.minWidthPx(15)
    commonProps.minWidthPercent(100f)
    commonProps.maxWidthPx(25)
    commonProps.maxWidthPercent(26f)
    commonProps.heightPx(30)
    commonProps.heightPercent(31f)
    commonProps.minHeightPx(32)
    commonProps.minHeightPercent(33f)
    commonProps.maxHeightPx(34)
    commonProps.maxHeightPercent(35f)
    commonProps.aspectRatio(20f)
    commonProps.touchExpansionPx(YogaEdge.RIGHT, 22)
    commonProps.touchExpansionPx(YogaEdge.LEFT, 23)
    commonProps.touchExpansionPx(YogaEdge.ALL, 21)
    val background = ComparableColorDrawable.create(Color.RED)
    commonProps.background(background)
    val foreground = ComparableColorDrawable.create(Color.BLACK)
    commonProps.foreground(foreground)
    commonProps.wrapInView()
    val clickHandler: EventHandler<ClickEvent> = mock()
    val longClickHandler: EventHandler<LongClickEvent> = mock()
    val touchHandler: EventHandler<TouchEvent> = mock()
    val interceptTouchHandler: EventHandler<InterceptTouchEvent> = mock()
    val focusChangedHandler: EventHandler<FocusChangedEvent> = mock()
    commonProps.clickHandler(clickHandler)
    commonProps.focusChangeHandler(focusChangedHandler)
    commonProps.longClickHandler(longClickHandler)
    commonProps.touchHandler(touchHandler)
    commonProps.interceptTouchHandler(interceptTouchHandler)
    commonProps.focusable(true)
    commonProps.clickable(true)
    commonProps.selected(false)
    commonProps.enabled(false)
    commonProps.visibleHeightRatio(55f)
    commonProps.visibleWidthRatio(56f)
    commonProps.accessibilityHeading(false)
    val visibleHandler: EventHandler<VisibleEvent> = mock()
    val focusedHandler: EventHandler<FocusedVisibleEvent> = mock()
    val unfocusedHandler: EventHandler<UnfocusedVisibleEvent> = mock()
    val fullImpressionHandler: EventHandler<FullImpressionVisibleEvent> = mock()
    val invisibleHandler: EventHandler<InvisibleEvent> = mock()
    val visibleRectChangedHandler: EventHandler<VisibilityChangedEvent> = mock()
    commonProps.visibleHandler(visibleHandler)
    commonProps.focusedHandler(focusedHandler)
    commonProps.unfocusedHandler(unfocusedHandler)
    commonProps.fullImpressionHandler(fullImpressionHandler)
    commonProps.invisibleHandler(invisibleHandler)
    commonProps.visibilityChangedHandler(visibleRectChangedHandler)
    commonProps.contentDescription("test")
    val viewTag = Any()
    val viewTags = SparseArray<Any>()
    commonProps.viewTag(viewTag)
    commonProps.viewTags(viewTags)
    commonProps.shadowElevationPx(60f)
    commonProps.clipToOutline(false)
    commonProps.transitionKey("transitionKey", "")
    commonProps.testKey("testKey")
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
    val onPopulateAccessibiliNodeHandler: EventHandler<OnPopulateAccessibilityNodeEvent> = mock()
    val onRequestSendAccessibilityEventHandler: EventHandler<OnRequestSendAccessibilityEventEvent> =
        mock()
    val performAccessibilityActionHandler: EventHandler<PerformAccessibilityActionEvent> = mock()
    val sendAccessibilityEventHandler: EventHandler<SendAccessibilityEventEvent> = mock()
    val sendAccessibilityEventUncheckedHandler: EventHandler<SendAccessibilityEventUncheckedEvent> =
        mock()
    commonProps.accessibilityRole(AccessibilityRole.BUTTON)
    commonProps.accessibilityRoleDescription("Test Role Description")
    commonProps.dispatchPopulateAccessibilityEventHandler(dispatchPopulateAccessibilityEventHandler)
    commonProps.onInitializeAccessibilityEventHandler(onInitializeAccessibilityEventHandler)
    commonProps.onInitializeAccessibilityNodeInfoHandler(onInitializeAccessibilityNodeInfoHandler)
    commonProps.onPopulateAccessibilityEventHandler(onPopulateAccessibilityEventHandler)
    commonProps.onPopulateAccessibilityNodeHandler(onPopulateAccessibiliNodeHandler)
    commonProps.onRequestSendAccessibilityEventHandler(onRequestSendAccessibilityEventHandler)
    commonProps.performAccessibilityActionHandler(performAccessibilityActionHandler)
    commonProps.sendAccessibilityEventHandler(sendAccessibilityEventHandler)
    commonProps.sendAccessibilityEventUncheckedHandler(sendAccessibilityEventUncheckedHandler)
    val stateListAnimator: StateListAnimator = mock()
    commonProps.stateListAnimator(stateListAnimator)
    commonProps.copyInto(componentContext, node)
    val output: LayoutProps = Mockito.spy<LayoutProps>(LayoutProps::class.java)
    commonProps.copyLayoutProps(output)
    verify(output).layoutDirection(YogaDirection.INHERIT)
    verify(output).alignSelf(YogaAlign.AUTO)
    verify(output).positionType(YogaPositionType.ABSOLUTE)
    verify(output).flex(2f)
    verify(output).flexGrow(3f)
    verify(output).flexShrink(4f)
    verify(output).flexBasisPx(5)
    verify(output).flexBasisPercent(6f)
    verify(node)
        .importantForAccessibility(ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_AUTO)
    verify(node).duplicateParentState(false)
    verify(output).marginPx(YogaEdge.ALL, 5)
    verify(output).marginPx(YogaEdge.RIGHT, 6)
    verify(output).marginPx(YogaEdge.LEFT, 4)
    verify(output).marginPercent(YogaEdge.ALL, 10f)
    verify(output).marginPercent(YogaEdge.VERTICAL, 12f)
    verify(output).marginPercent(YogaEdge.RIGHT, 5f)
    verify(output).marginAuto(YogaEdge.LEFT)
    verify(output).marginAuto(YogaEdge.TOP)
    verify(output).marginAuto(YogaEdge.RIGHT)
    verify(output).marginAuto(YogaEdge.BOTTOM)
    verify(output).paddingPx(YogaEdge.ALL, 1)
    verify(output).paddingPx(YogaEdge.RIGHT, 2)
    verify(output).paddingPx(YogaEdge.LEFT, 3)
    verify(output).paddingPercent(YogaEdge.VERTICAL, 7f)
    verify(output).paddingPercent(YogaEdge.RIGHT, 6f)
    verify(output).paddingPercent(YogaEdge.ALL, 5f)
    verify(node).border(any<Border>())
    verify(output).positionPx(YogaEdge.ALL, 11)
    verify(output).positionPx(YogaEdge.RIGHT, 12)
    verify(output).positionPx(YogaEdge.LEFT, 13)
    verify(output).positionPercent(YogaEdge.VERTICAL, 17f)
    verify(output).positionPercent(YogaEdge.RIGHT, 16f)
    verify(output).positionPercent(YogaEdge.ALL, 15f)
    verify(output).widthPx(5)
    verify(output).widthPercent(50f)
    verify(output).minWidthPx(15)
    verify(output).minWidthPercent(100f)
    verify(output).maxWidthPx(25)
    verify(output).maxWidthPercent(26f)
    verify(output).heightPx(30)
    verify(output).heightPercent(31f)
    verify(output).minHeightPx(32)
    verify(output).minHeightPercent(33f)
    verify(output).maxHeightPx(34)
    verify(output).maxHeightPercent(35f)
    verify(output).aspectRatio(20f)
    verify(node).touchExpansionPx(YogaEdge.RIGHT, 22)
    verify(node).touchExpansionPx(YogaEdge.LEFT, 23)
    verify(node).touchExpansionPx(YogaEdge.ALL, 21)
    verify(node).background(background)
    verify(node).foreground(foreground)
    verify(node).wrapInView()
    verify(node).applyNodeInfo(any<NodeInfo>())
    verify(node).visibleHeightRatio(55f)
    verify(node).visibleWidthRatio(56f)
    verify(node).visibleHandler(visibleHandler)
    verify(node).focusedHandler(focusedHandler)
    verify(node).unfocusedHandler(unfocusedHandler)
    verify(node).fullImpressionHandler(fullImpressionHandler)
    verify(node).invisibleHandler(invisibleHandler)
    verify(node).visibilityChangedHandler(visibleRectChangedHandler)
    verify(node).transitionKey(eq("transitionKey"), ArgumentMatchers.anyString())
    verify(node).testKey("testKey")
    verify(node).stateListAnimator(stateListAnimator)
  }

  @Test
  fun testSetScalePropsWrapsInView() {
    commonProps.scale(5f)
    commonProps.copyInto(componentContext, node)
    verify(node).applyNodeInfo(any<NodeInfo>())
    verify(node).wrapInView()
  }

  @Test
  fun testSetFullScalePropsDoesNotWrapInView() {
    commonProps.scale(0.5f)
    commonProps.scale(1f)
    commonProps.copyInto(componentContext, node)
    verify(node).applyNodeInfo(any<NodeInfo>())
    verify(node, never()).wrapInView()
  }

  @Test
  fun testSetAlphaPropsWrapsInView() {
    commonProps.alpha(5f)
    commonProps.copyInto(componentContext, node)
    verify(node).applyNodeInfo(any<NodeInfo>())
    verify(node).wrapInView()
  }

  @Test
  fun testSetFullAlphaPropsDoesNotWrapInView() {
    commonProps.alpha(5f)
    commonProps.alpha(1f)
    commonProps.copyInto(componentContext, node)
    verify(node).applyNodeInfo(any<NodeInfo>())
    verify(node, never()).wrapInView()
  }

  @Test
  fun testSetRotationPropsWrapsInView() {
    commonProps.rotation(5f)
    commonProps.copyInto(componentContext, node)
    verify(node).applyNodeInfo(any<NodeInfo>())
    verify(node).wrapInView()
  }

  @Test
  fun testSetZeroRotationPropsDoesNotWrapInView() {
    commonProps.rotation(1f)
    commonProps.rotation(0f)
    commonProps.copyInto(componentContext, node)
    verify(node).applyNodeInfo(any<NodeInfo>())
    verify(node, never()).wrapInView()
  }

  @Test
  fun testPaddingFromDrawable() {
    val c = legacyLithoViewRule.context
    val component =
        Column.create(c)
            .child(Text.create(c).text("Hello World").backgroundRes(background_with_padding))
            .build()
    legacyLithoViewRule.attachToWindow().setRoot(component).measure().layout()
    val result = legacyLithoViewRule.currentRootNode!!.getChildAt(0)
    assertThat(result.paddingLeft).isEqualTo(48)
    assertThat(result.paddingTop).isEqualTo(0)
    assertThat(result.paddingRight).isEqualTo(0)
    assertThat(result.paddingBottom).isEqualTo(0)
  }

  @Test
  fun testPaddingFromDrawableIsOverwritten() {
    val c = legacyLithoViewRule.context
    val component =
        Column.create(c)
            .child(
                Text.create(c)
                    .text("Hello World")
                    .backgroundRes(background_with_padding)
                    .paddingPx(YogaEdge.LEFT, 8)
                    .paddingPx(YogaEdge.RIGHT, 8)
                    .paddingPx(YogaEdge.TOP, 8)
                    .paddingPx(YogaEdge.BOTTOM, 8))
            .build()
    legacyLithoViewRule.attachToWindow().setRoot(component).measure().layout()
    val result = legacyLithoViewRule.currentRootNode!!.getChildAt(0)
    assertThat(result.paddingLeft).isEqualTo(8)
    assertThat(result.paddingTop).isEqualTo(8)
    assertThat(result.paddingRight).isEqualTo(8)
    assertThat(result.paddingBottom).isEqualTo(8)
  }

  @Test
  fun testSameObjectEquivalentTo() {
    assertThat(commonProps.isEquivalentTo(commonProps)).isEqualTo(true)
  }

  @Test
  fun testNullObjectEquivalentTo() {
    assertThat(commonProps.isEquivalentTo(null)).isEqualTo(false)
  }

  @Test
  fun testDifferentObjectWithSameContentEquivalentTo() {
    commonProps = CommonProps()
    setCommonProps(commonProps)
    val mCommonProps2 = CommonProps()
    setCommonProps(mCommonProps2)
    assertThat(commonProps.isEquivalentTo(mCommonProps2)).isEqualTo(true)
  }

  @Test
  fun testDifferentObjectWithDifferentContentEquivalentTo() {
    commonProps = CommonProps()
    setCommonProps(commonProps)
    commonProps.duplicateParentState(false)
    val mCommonProps2 = CommonProps()
    setCommonProps(mCommonProps2)
    mCommonProps2.duplicateParentState(true)
    assertThat(commonProps.isEquivalentTo(mCommonProps2)).isEqualTo(false)
  }

  private fun setCommonProps(commonProps: CommonProps) {
    commonProps.layoutDirection(YogaDirection.INHERIT)
    commonProps.alignSelf(YogaAlign.AUTO)
    commonProps.positionType(YogaPositionType.ABSOLUTE)
    commonProps.flex(2f)
    commonProps.flexGrow(3f)
    commonProps.flexShrink(4f)
    commonProps.flexBasisPx(5)
    commonProps.flexBasisPercent(6f)
    commonProps.importantForAccessibility(
        ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_AUTO)
    commonProps.duplicateParentState(false)
    commonProps.marginPx(YogaEdge.ALL, 5)
    commonProps.marginPx(YogaEdge.RIGHT, 6)
    commonProps.marginPx(YogaEdge.LEFT, 4)
    commonProps.marginPercent(YogaEdge.ALL, 10f)
    commonProps.marginPercent(YogaEdge.VERTICAL, 12f)
    commonProps.marginPercent(YogaEdge.RIGHT, 5f)
    commonProps.marginAuto(YogaEdge.LEFT)
    commonProps.marginAuto(YogaEdge.TOP)
    commonProps.marginAuto(YogaEdge.RIGHT)
    commonProps.marginAuto(YogaEdge.BOTTOM)
    commonProps.paddingPx(YogaEdge.ALL, 1)
    commonProps.paddingPx(YogaEdge.RIGHT, 2)
    commonProps.paddingPx(YogaEdge.LEFT, 3)
    commonProps.paddingPercent(YogaEdge.VERTICAL, 7f)
    commonProps.paddingPercent(YogaEdge.RIGHT, 6f)
    commonProps.paddingPercent(YogaEdge.ALL, 5f)
    commonProps.border(Border.create(componentContext).build())
    commonProps.positionPx(YogaEdge.ALL, 11)
    commonProps.positionPx(YogaEdge.RIGHT, 12)
    commonProps.positionPx(YogaEdge.LEFT, 13)
    commonProps.positionPercent(YogaEdge.VERTICAL, 17f)
    commonProps.positionPercent(YogaEdge.RIGHT, 16f)
    commonProps.positionPercent(YogaEdge.ALL, 15f)
    commonProps.widthPx(5)
    commonProps.widthPercent(50f)
    commonProps.minWidthPx(15)
    commonProps.minWidthPercent(100f)
    commonProps.maxWidthPx(25)
    commonProps.maxWidthPercent(26f)
    commonProps.heightPx(30)
    commonProps.heightPercent(31f)
    commonProps.minHeightPx(32)
    commonProps.minHeightPercent(33f)
    commonProps.maxHeightPx(34)
    commonProps.maxHeightPercent(35f)
    commonProps.aspectRatio(20f)
    commonProps.touchExpansionPx(YogaEdge.RIGHT, 22)
    commonProps.touchExpansionPx(YogaEdge.LEFT, 23)
    commonProps.touchExpansionPx(YogaEdge.ALL, 21)
    val background = ComparableColorDrawable.create(Color.RED)
    commonProps.background(background)
    val foreground = ComparableColorDrawable.create(Color.BLACK)
    commonProps.foreground(foreground)
    commonProps.wrapInView()
    val content = TestViewComponent.create(componentContext).build()
    val clickHandler = EventHandler<ClickEvent>(content, 3)
    val longClickHandler = EventHandler<LongClickEvent>(content, 3)
    val touchHandler = EventHandler<TouchEvent>(content, 3)
    val interceptTouchHandler = EventHandler<InterceptTouchEvent>(content, 3)
    val focusChangedHandler = EventHandler<FocusChangedEvent>(content, 3)
    commonProps.clickHandler(clickHandler)
    commonProps.focusChangeHandler(focusChangedHandler)
    commonProps.longClickHandler(longClickHandler)
    commonProps.touchHandler(touchHandler)
    commonProps.interceptTouchHandler(interceptTouchHandler)
    commonProps.focusable(true)
    commonProps.clickable(true)
    commonProps.selected(false)
    commonProps.enabled(false)
    commonProps.visibleHeightRatio(55f)
    commonProps.visibleWidthRatio(56f)
    commonProps.accessibilityHeading(false)
    val visibleHandler = EventHandler<VisibleEvent>(content, 3)
    val focusedHandler = EventHandler<FocusedVisibleEvent>(content, 3)
    val unfocusedHandler = EventHandler<UnfocusedVisibleEvent>(content, 3)
    val fullImpressionHandler = EventHandler<FullImpressionVisibleEvent>(content, 3)
    val invisibleHandler = EventHandler<InvisibleEvent>(content, 3)
    val visibleRectChangedHandler = EventHandler<VisibilityChangedEvent>(content, 3)
    commonProps.visibleHandler(visibleHandler)
    commonProps.focusedHandler(focusedHandler)
    commonProps.unfocusedHandler(unfocusedHandler)
    commonProps.fullImpressionHandler(fullImpressionHandler)
    commonProps.invisibleHandler(invisibleHandler)
    commonProps.visibilityChangedHandler(visibleRectChangedHandler)
    commonProps.contentDescription("test")
    commonProps.viewTag("Hello World")
    commonProps.shadowElevationPx(60f)
    commonProps.clipToOutline(false)
    commonProps.transitionKey("transitionKey", "")
    commonProps.testKey("testKey")
    val dispatchPopulateAccessibilityEventHandler =
        EventHandler<DispatchPopulateAccessibilityEventEvent>(content, 3)
    val onInitializeAccessibilityEventHandler =
        EventHandler<OnInitializeAccessibilityEventEvent>(content, 3)
    val onInitializeAccessibilityNodeInfoHandler =
        EventHandler<OnInitializeAccessibilityNodeInfoEvent>(content, 3)
    val onPopulateAccessibilityEventHandler =
        EventHandler<OnPopulateAccessibilityEventEvent>(content, 3)
    val onPopulateAccessibilityNodeHandler =
        EventHandler<OnPopulateAccessibilityNodeEvent>(content, 3)
    val onRequestSendAccessibilityEventHandler =
        EventHandler<OnRequestSendAccessibilityEventEvent>(content, 3)
    val performAccessibilityActionHandler =
        EventHandler<PerformAccessibilityActionEvent>(content, 3)
    val sendAccessibilityEventHandler = EventHandler<SendAccessibilityEventEvent>(content, 3)
    val sendAccessibilityEventUncheckedHandler =
        EventHandler<SendAccessibilityEventUncheckedEvent>(content, 3)
    commonProps.accessibilityRole(AccessibilityRole.BUTTON)
    commonProps.accessibilityRoleDescription("Test Role Description")
    commonProps.dispatchPopulateAccessibilityEventHandler(dispatchPopulateAccessibilityEventHandler)
    commonProps.onInitializeAccessibilityEventHandler(onInitializeAccessibilityEventHandler)
    commonProps.onInitializeAccessibilityNodeInfoHandler(onInitializeAccessibilityNodeInfoHandler)
    commonProps.onPopulateAccessibilityEventHandler(onPopulateAccessibilityEventHandler)
    commonProps.onPopulateAccessibilityNodeHandler(onPopulateAccessibilityNodeHandler)
    commonProps.onRequestSendAccessibilityEventHandler(onRequestSendAccessibilityEventHandler)
    commonProps.performAccessibilityActionHandler(performAccessibilityActionHandler)
    commonProps.sendAccessibilityEventHandler(sendAccessibilityEventHandler)
    commonProps.sendAccessibilityEventUncheckedHandler(sendAccessibilityEventUncheckedHandler)

    /*
         Copied from ViewNodeInfo class TODO: (T33421916) We need compare StateListAnimators more accurately
    */
    //    final StateListAnimator stateListAnimator = mock(StateListAnimator.class);
    //    commonProps.stateListAnimator(stateListAnimator);
    commonProps.copyInto(componentContext, node)
  }
}
