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
import com.facebook.litho.testing.TestComponent
import com.facebook.litho.testing.TestSizeDependentComponent
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.SimpleMountSpecTester
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaDirection
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaPositionType
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@RunWith(LithoTestRunner::class)
class LayoutStateCreateTreeTest {

  private lateinit var componentContext: ComponentContext
  private lateinit var resolveContext: ResolveContext

  @Before
  fun setup() {
    componentContext = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    resolveContext = componentContext.setRenderStateContextForTests()
  }

  @Test
  fun simpleLayoutCreatesExpectedInternalNodeTree() {
    val component: Component =
        object : InlineLayoutSpec(componentContext) {
          override fun onCreateLayout(c: ComponentContext): Component =
              Column.create(c)
                  .child(Column.create(c).child(SimpleMountSpecTester.create(c)))
                  .build()
        }
    var node = Resolver.resolve(resolveContext, componentContext, component)
    assertThat(node?.childCount).isEqualTo(1)
    assertThat(node?.headComponent).isEqualTo(component)
    assertThat(node?.tailComponent).isInstanceOf(Column::class.java)
    node = node?.getChildAt(0)
    assertThat(node?.childCount).isEqualTo(1)
    assertThat(node?.tailComponent).isInstanceOf(Column::class.java)
    node = node?.getChildAt(0)
    assertThat(node?.childCount).isEqualTo(0)
    assertThat(node?.tailComponent).isInstanceOf(SimpleMountSpecTester::class.java)
  }

  @Test
  fun testHandlersAreAppliedToCorrectInternalNodes() {
    val clickHandler1: EventHandler<ClickEvent> = mock()
    val clickHandler2: EventHandler<ClickEvent> = mock()
    val clickHandler3: EventHandler<ClickEvent> = mock()
    val longClickHandler1: EventHandler<LongClickEvent> = mock()
    val longClickHandler2: EventHandler<LongClickEvent> = mock()
    val longClickHandler3: EventHandler<LongClickEvent> = mock()
    val touchHandler1: EventHandler<TouchEvent> = mock()
    val touchHandler2: EventHandler<TouchEvent> = mock()
    val touchHandler3: EventHandler<TouchEvent> = mock()
    val interceptTouchHandler1: EventHandler<InterceptTouchEvent> = mock()
    val interceptTouchHandler2: EventHandler<InterceptTouchEvent> = mock()
    val interceptTouchHandler3: EventHandler<InterceptTouchEvent> = mock()
    val focusChangedHandler1: EventHandler<FocusChangedEvent> = mock()
    val focusChangedHandler2: EventHandler<FocusChangedEvent> = mock()
    val focusChangedHandler3: EventHandler<FocusChangedEvent> = mock()
    val component: Component =
        object : InlineLayoutSpec(componentContext) {
          override fun onCreateLayout(c: ComponentContext): Component =
              Column.create(c)
                  .child(
                      Column.create(c)
                          .child(
                              SimpleMountSpecTester.create(c)
                                  .clickHandler(clickHandler1)
                                  .longClickHandler(longClickHandler1)
                                  .touchHandler(touchHandler1)
                                  .interceptTouchHandler(interceptTouchHandler1)
                                  .focusChangeHandler(focusChangedHandler1))
                          .clickHandler(clickHandler2)
                          .longClickHandler(longClickHandler2)
                          .touchHandler(touchHandler2)
                          .interceptTouchHandler(interceptTouchHandler2)
                          .focusChangeHandler(focusChangedHandler2))
                  .clickHandler(clickHandler3)
                  .longClickHandler(longClickHandler3)
                  .touchHandler(touchHandler3)
                  .interceptTouchHandler(interceptTouchHandler3)
                  .focusChangeHandler(focusChangedHandler3)
                  .build()
        }
    var node = Resolver.resolve(resolveContext, componentContext, component)
    assertThat(node?.nodeInfo?.clickHandler).isEqualTo(clickHandler3)
    assertThat(node?.nodeInfo?.longClickHandler).isEqualTo(longClickHandler3)
    assertThat(node?.nodeInfo?.touchHandler).isEqualTo(touchHandler3)
    assertThat(node?.nodeInfo?.interceptTouchHandler).isEqualTo(interceptTouchHandler3)
    assertThat(node?.nodeInfo?.focusChangeHandler).isEqualTo(focusChangedHandler3)
    node = node?.getChildAt(0)
    assertThat(node?.nodeInfo?.clickHandler).isEqualTo(clickHandler2)
    assertThat(node?.nodeInfo?.longClickHandler).isEqualTo(longClickHandler2)
    assertThat(node?.nodeInfo?.touchHandler).isEqualTo(touchHandler2)
    assertThat(node?.nodeInfo?.interceptTouchHandler).isEqualTo(interceptTouchHandler2)
    assertThat(node?.nodeInfo?.focusChangeHandler).isEqualTo(focusChangedHandler2)
    node = node?.getChildAt(0)
    assertThat(node?.nodeInfo?.clickHandler).isEqualTo(clickHandler1)
    assertThat(node?.nodeInfo?.longClickHandler).isEqualTo(longClickHandler1)
    assertThat(node?.nodeInfo?.touchHandler).isEqualTo(touchHandler1)
    assertThat(node?.nodeInfo?.interceptTouchHandler).isEqualTo(interceptTouchHandler1)
    assertThat(node?.nodeInfo?.focusChangeHandler).isEqualTo(focusChangedHandler1)
  }

  @Test
  fun testHandlersAreAppliedToCorrectInternalNodesForSizeDependentComponent() {
    val clickHandler1: EventHandler<ClickEvent> = mock()
    val clickHandler2: EventHandler<ClickEvent> = mock()
    val clickHandler3: EventHandler<ClickEvent> = mock()
    val longClickHandler1: EventHandler<LongClickEvent> = mock()
    val longClickHandler2: EventHandler<LongClickEvent> = mock()
    val longClickHandler3: EventHandler<LongClickEvent> = mock()
    val touchHandler1: EventHandler<TouchEvent> = mock()
    val touchHandler2: EventHandler<TouchEvent> = mock()
    val touchHandler3: EventHandler<TouchEvent> = mock()
    val interceptTouchHandler1: EventHandler<InterceptTouchEvent> = mock()
    val interceptTouchHandler2: EventHandler<InterceptTouchEvent> = mock()
    val interceptTouchHandler3: EventHandler<InterceptTouchEvent> = mock()
    val focusChangedHandler1: EventHandler<FocusChangedEvent> = mock()
    val focusChangedHandler2: EventHandler<FocusChangedEvent> = mock()
    val focusChangedHandler3: EventHandler<FocusChangedEvent> = mock()
    val component: Component =
        object : InlineLayoutSpec(componentContext) {
          override fun onCreateLayout(c: ComponentContext): Component =
              Column.create(c)
                  .child(
                      Column.create(c)
                          .child(
                              TestSizeDependentComponent.create(c)
                                  .clickHandler(clickHandler1)
                                  .longClickHandler(longClickHandler1)
                                  .touchHandler(touchHandler1)
                                  .interceptTouchHandler(interceptTouchHandler1)
                                  .focusChangeHandler(focusChangedHandler1))
                          .clickHandler(clickHandler2)
                          .longClickHandler(longClickHandler2)
                          .touchHandler(touchHandler2)
                          .interceptTouchHandler(interceptTouchHandler2)
                          .focusChangeHandler(focusChangedHandler2))
                  .clickHandler(clickHandler3)
                  .longClickHandler(longClickHandler3)
                  .touchHandler(touchHandler3)
                  .interceptTouchHandler(interceptTouchHandler3)
                  .focusChangeHandler(focusChangedHandler3)
                  .build()
        }
    var node = Resolver.resolve(resolveContext, componentContext, component)
    assertThat(node?.nodeInfo?.clickHandler).isEqualTo(clickHandler3)
    assertThat(node?.nodeInfo?.longClickHandler).isEqualTo(longClickHandler3)
    assertThat(node?.nodeInfo?.touchHandler).isEqualTo(touchHandler3)
    assertThat(node?.nodeInfo?.interceptTouchHandler).isEqualTo(interceptTouchHandler3)
    assertThat(node?.nodeInfo?.focusChangeHandler).isEqualTo(focusChangedHandler3)
    node = node?.getChildAt(0)
    assertThat(node?.nodeInfo?.clickHandler).isEqualTo(clickHandler2)
    assertThat(node?.nodeInfo?.longClickHandler).isEqualTo(longClickHandler2)
    assertThat(node?.nodeInfo?.touchHandler).isEqualTo(touchHandler2)
    assertThat(node?.nodeInfo?.interceptTouchHandler).isEqualTo(interceptTouchHandler2)
    assertThat(node?.nodeInfo?.focusChangeHandler).isEqualTo(focusChangedHandler2)
    node = node?.getChildAt(0)
    assertThat(node?.nodeInfo?.clickHandler).isEqualTo(clickHandler1)
    assertThat(node?.nodeInfo?.longClickHandler).isEqualTo(longClickHandler1)
    assertThat(node?.nodeInfo?.touchHandler).isEqualTo(touchHandler1)
    assertThat(node?.nodeInfo?.interceptTouchHandler).isEqualTo(interceptTouchHandler1)
    assertThat(node?.nodeInfo?.focusChangeHandler).isEqualTo(focusChangedHandler1)
  }

  @Test
  fun testOverridingHandlers() {
    val clickHandler1: EventHandler<ClickEvent> = mock()
    val clickHandler2: EventHandler<ClickEvent> = mock()
    val longClickHandler1: EventHandler<LongClickEvent> = mock()
    val longClickHandler2: EventHandler<LongClickEvent> = mock()
    val touchHandler1: EventHandler<TouchEvent> = mock()
    val touchHandler2: EventHandler<TouchEvent> = mock()
    val interceptTouchHandler1: EventHandler<InterceptTouchEvent> = mock()
    val interceptTouchHandler2: EventHandler<InterceptTouchEvent> = mock()
    val focusChangedHandler1: EventHandler<FocusChangedEvent> = mock()
    val focusChangedHandler2: EventHandler<FocusChangedEvent> = mock()
    val component: Component =
        object : InlineLayoutSpec(componentContext) {
          override fun onCreateLayout(c: ComponentContext): Component {
            return Wrapper.create(c)
                .delegate(
                    object : InlineLayoutSpec() {
                      override fun onCreateLayout(c: ComponentContext): Component =
                          SimpleMountSpecTester.create(c)
                              .clickHandler(clickHandler1)
                              .longClickHandler(longClickHandler1)
                              .touchHandler(touchHandler1)
                              .interceptTouchHandler(interceptTouchHandler1)
                              .focusChangeHandler(focusChangedHandler1)
                              .build()
                    })
                .clickHandler(clickHandler2)
                .longClickHandler(longClickHandler2)
                .touchHandler(touchHandler2)
                .interceptTouchHandler(interceptTouchHandler2)
                .focusChangeHandler(focusChangedHandler2)
                .build()
          }
        }
    val node = Resolver.resolve(resolveContext, componentContext, component)
    assertThat(node?.childCount).isEqualTo(0)
    assertThat(node?.tailComponent).isInstanceOf(SimpleMountSpecTester::class.java)
    assertThat(node?.nodeInfo?.clickHandler).isEqualTo(clickHandler2)
    assertThat(node?.nodeInfo?.longClickHandler).isEqualTo(longClickHandler2)
    assertThat(node?.nodeInfo?.touchHandler).isEqualTo(touchHandler2)
    assertThat(node?.nodeInfo?.interceptTouchHandler).isEqualTo(interceptTouchHandler2)
    assertThat(node?.nodeInfo?.focusChangeHandler).isEqualTo(focusChangedHandler2)
  }

  @Test
  fun testOverridingHandlersForSizeDependentComponent() {
    val clickHandler1: EventHandler<ClickEvent> = mock()
    val clickHandler2: EventHandler<ClickEvent> = mock()
    val longClickHandler1: EventHandler<LongClickEvent> = mock()
    val longClickHandler2: EventHandler<LongClickEvent> = mock()
    val touchHandler1: EventHandler<TouchEvent> = mock()
    val touchHandler2: EventHandler<TouchEvent> = mock()
    val interceptTouchHandler1: EventHandler<InterceptTouchEvent> = mock()
    val interceptTouchHandler2: EventHandler<InterceptTouchEvent> = mock()
    val focusChangedHandler1: EventHandler<FocusChangedEvent> = mock()
    val focusChangedHandler2: EventHandler<FocusChangedEvent> = mock()
    val component: Component =
        object : InlineLayoutSpec(componentContext) {
          override fun onCreateLayout(c: ComponentContext): Component {
            return Wrapper.create(c)
                .delegate(
                    object : InlineLayoutSpec() {
                      override fun onCreateLayout(c: ComponentContext): Component =
                          TestSizeDependentComponent.create(c)
                              .clickHandler(clickHandler1)
                              .longClickHandler(longClickHandler1)
                              .touchHandler(touchHandler1)
                              .interceptTouchHandler(interceptTouchHandler1)
                              .focusChangeHandler(focusChangedHandler1)
                              .build()
                    })
                .clickHandler(clickHandler2)
                .longClickHandler(longClickHandler2)
                .touchHandler(touchHandler2)
                .interceptTouchHandler(interceptTouchHandler2)
                .focusChangeHandler(focusChangedHandler2)
                .build()
          }
        }
    val node = Resolver.resolve(resolveContext, componentContext, component)
    assertThat(node?.childCount).isEqualTo(0)
    assertThat(node?.tailComponent).isInstanceOf(TestSizeDependentComponent::class.java)
    assertThat(node?.nodeInfo?.clickHandler).isEqualTo(clickHandler2)
    assertThat(node?.nodeInfo?.longClickHandler).isEqualTo(longClickHandler2)
    assertThat(node?.nodeInfo?.touchHandler).isEqualTo(touchHandler2)
    assertThat(node?.nodeInfo?.interceptTouchHandler).isEqualTo(interceptTouchHandler2)
    assertThat(node?.nodeInfo?.focusChangeHandler).isEqualTo(focusChangedHandler2)
  }

  @Test
  @Config(sdk = [Build.VERSION_CODES.LOLLIPOP])
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  fun testAddingAllAttributes() {
    val background = ComparableColorDrawable.create(Color.RED)
    val foreground = ComparableColorDrawable.create(Color.BLACK)
    val clickHandler: EventHandler<ClickEvent> = mock()
    val longClickHandler: EventHandler<LongClickEvent> = mock()
    val touchHandler: EventHandler<TouchEvent> = mock()
    val interceptTouchHandler: EventHandler<InterceptTouchEvent> = mock()
    val focusChangedHandler: EventHandler<FocusChangedEvent> = mock()
    val visibleHandler: EventHandler<VisibleEvent> = mock()
    val focusedHandler: EventHandler<FocusedVisibleEvent> = mock()
    val unfocusedHandler: EventHandler<UnfocusedVisibleEvent> = mock()
    val fullImpressionHandler: EventHandler<FullImpressionVisibleEvent> = mock()
    val invisibleHandler: EventHandler<InvisibleEvent> = mock()
    val visibleRectChangedHandler: EventHandler<VisibilityChangedEvent> = mock()
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
    val stateListAnimator = mock<StateListAnimator>()
    val component: Component =
        TestDrawableComponentWithMockInternalNode.create(componentContext)
            .layoutDirection(YogaDirection.INHERIT)
            .alignSelf(YogaAlign.AUTO)
            .positionType(YogaPositionType.ABSOLUTE)
            .flex(2f)
            .flexGrow(3f)
            .flexShrink(4f)
            .flexBasisPx(5)
            .flexBasisPercent(6f)
            .importantForAccessibility(ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_AUTO)
            .duplicateParentState(false)
            .marginPx(YogaEdge.ALL, 5)
            .marginPx(YogaEdge.RIGHT, 6)
            .marginPx(YogaEdge.LEFT, 4)
            .marginPercent(YogaEdge.ALL, 10f)
            .marginPercent(YogaEdge.VERTICAL, 12f)
            .marginPercent(YogaEdge.RIGHT, 5f)
            .marginAuto(YogaEdge.LEFT)
            .marginAuto(YogaEdge.TOP)
            .marginAuto(YogaEdge.RIGHT)
            .marginAuto(YogaEdge.BOTTOM)
            .paddingPx(YogaEdge.ALL, 1)
            .paddingPx(YogaEdge.RIGHT, 2)
            .paddingPx(YogaEdge.LEFT, 3)
            .paddingPercent(YogaEdge.VERTICAL, 7f)
            .paddingPercent(YogaEdge.RIGHT, 6f)
            .paddingPercent(YogaEdge.ALL, 5f)
            .positionPx(YogaEdge.ALL, 11)
            .positionPx(YogaEdge.RIGHT, 12)
            .positionPx(YogaEdge.LEFT, 13)
            .positionPercent(YogaEdge.VERTICAL, 17f)
            .positionPercent(YogaEdge.RIGHT, 16f)
            .positionPercent(YogaEdge.ALL, 15f)
            .widthPx(5)
            .widthPercent(50f)
            .minWidthPx(15)
            .minWidthPercent(100f)
            .maxWidthPx(25)
            .maxWidthPercent(26f)
            .heightPx(30)
            .heightPercent(31f)
            .minHeightPx(32)
            .minHeightPercent(33f)
            .maxHeightPx(34)
            .maxHeightPercent(35f)
            .aspectRatio(20f)
            .touchExpansionPx(YogaEdge.RIGHT, 22)
            .touchExpansionPx(YogaEdge.LEFT, 23)
            .touchExpansionPx(YogaEdge.ALL, 21)
            .background(background)
            .foreground(foreground)
            .wrapInView()
            .clickHandler(clickHandler)
            .focusChangeHandler(focusChangedHandler)
            .longClickHandler(longClickHandler)
            .touchHandler(touchHandler)
            .interceptTouchHandler(interceptTouchHandler)
            .focusable(true)
            .selected(false)
            .enabled(false)
            .accessibilityHeading(false)
            .visibleHeightRatio(55f)
            .visibleWidthRatio(56f)
            .visibleHandler(visibleHandler)
            .focusedHandler(focusedHandler)
            .unfocusedHandler(unfocusedHandler)
            .fullImpressionHandler(fullImpressionHandler)
            .invisibleHandler(invisibleHandler)
            .visibilityChangedHandler(visibleRectChangedHandler)
            .contentDescription("test")
            .viewTag(viewTag)
            .viewTags(viewTags)
            .shadowElevationPx(60f)
            .clipToOutline(false)
            .transitionKey("transitionKey")
            .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
            .testKey("testKey")
            .accessibilityRole(AccessibilityRole.BUTTON)
            .accessibilityRoleDescription("Test Role Description")
            .dispatchPopulateAccessibilityEventHandler(dispatchPopulateAccessibilityEventHandler)
            .onInitializeAccessibilityEventHandler(onInitializeAccessibilityEventHandler)
            .onInitializeAccessibilityNodeInfoHandler(onInitializeAccessibilityNodeInfoHandler)
            .onPopulateAccessibilityEventHandler(onPopulateAccessibilityEventHandler)
            .onRequestSendAccessibilityEventHandler(onRequestSendAccessibilityEventHandler)
            .performAccessibilityActionHandler(performAccessibilityActionHandler)
            .sendAccessibilityEventHandler(sendAccessibilityEventHandler)
            .sendAccessibilityEventUncheckedHandler(sendAccessibilityEventUncheckedHandler)
            .stateListAnimator(stateListAnimator)
            .build()
    val node = Resolver.resolve(resolveContext, componentContext, component)
    val output: LayoutProps = Mockito.spy(LayoutProps::class.java)
    (component as SpecGeneratedComponent).commonProps?.copyLayoutProps(output)
    verify(output).layoutDirection(YogaDirection.INHERIT)
    verify(output).alignSelf(YogaAlign.AUTO)
    verify(output).positionType(YogaPositionType.ABSOLUTE)
    verify(output).flex(2f)
    verify(output).flexGrow(3f)
    verify(output).flexShrink(4f)
    verify(output).flexBasisPx(5)
    verify(output).flexBasisPercent(6f)
    verify(node)
        ?.importantForAccessibility(ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_AUTO)
    verify(node)?.duplicateParentState(false)
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
    verify(node)?.touchExpansionPx(YogaEdge.RIGHT, 22)
    verify(node)?.touchExpansionPx(YogaEdge.LEFT, 23)
    verify(node)?.touchExpansionPx(YogaEdge.ALL, 21)
    verify(node)?.background(background)
    verify(node)?.foreground(foreground)
    verify(node)?.wrapInView()
    verify(node)?.applyNodeInfo(anyOrNull())
    verify(node)?.visibleHeightRatio(55f)
    verify(node)?.visibleWidthRatio(56f)
    verify(node)?.visibleHandler(visibleHandler)
    verify(node)?.focusedHandler(focusedHandler)
    verify(node)?.unfocusedHandler(unfocusedHandler)
    verify(node)?.fullImpressionHandler(fullImpressionHandler)
    verify(node)?.invisibleHandler(invisibleHandler)
    verify(node)?.visibilityChangedHandler(visibleRectChangedHandler)
    verify(node)?.transitionKey(eq("transitionKey"), anyOrNull<String>())
    verify(node)?.transitionKeyType(Transition.TransitionKeyType.GLOBAL)
    verify(node)?.testKey("testKey")
    verify(node)?.stateListAnimator(stateListAnimator)
  }

  private class TestDrawableComponentWithMockInternalNode : TestComponent() {
    protected fun canResolve(): Boolean = true

    override fun resolve(resolveContext: ResolveContext, c: ComponentContext): LithoNode {
      val result = mock<LithoLayoutResult>()
      val node = mock<LithoNode>()
      val nodeInfo = mock<NodeInfo>()
      whenever(node.mutableNodeInfo()).thenReturn(nodeInfo)
      whenever(
              node.calculateLayout(
                  anyOrNull(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt()))
          .thenReturn(result)
      whenever(result.node).thenReturn(node)
      return node
    }

    public override fun resolve(
        resolveContext: ResolveContext,
        scopedComponentInfo: ScopedComponentInfo,
        parentWidthSpec: Int,
        parentHeightSpec: Int,
        componentsLogger: ComponentsLogger?
    ): ComponentResolveResult {
      val lithoNode = resolve(resolveContext, scopedComponentInfo.context)
      return ComponentResolveResult(lithoNode, null)
    }

    class Builder(
        c: ComponentContext,
        defStyleAttr: Int,
        defStyleRes: Int,
        private var _component: Component
    ) : Component.Builder<Builder?>(c, defStyleAttr, defStyleRes, _component) {
      override fun setComponent(component: Component) {
        this._component = component
      }

      override fun getThis(): Builder = this

      override fun build(): Component = _component
    }

    companion object {
      @JvmStatic
      fun create(c: ComponentContext): Builder {
        return Builder(c, 0, 0, TestDrawableComponentWithMockInternalNode())
      }
    }
  }
}
