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

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.animation.StateListAnimator;
import android.annotation.TargetApi;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.SparseArray;
import com.facebook.litho.annotations.ImportantForAccessibility;
import com.facebook.litho.drawable.ComparableColorDrawable;
import com.facebook.litho.testing.TestComponent;
import com.facebook.litho.testing.TestSizeDependentComponent;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.SimpleMountSpecTester;
import com.facebook.rendercore.RenderState;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaPositionType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(LithoTestRunner.class)
public class LayoutStateCreateTreeTest {
  private ComponentContext mComponentContext;
  private LayoutStateContext mLayoutStateContext;

  @Before
  public void setup() throws Exception {
    mComponentContext = new ComponentContext(getApplicationContext());
    mLayoutStateContext = LayoutStateContext.getTestInstance(mComponentContext);
    mComponentContext.setLayoutStateContext(mLayoutStateContext);
  }

  @After
  public void after() {
    mComponentContext = null;
  }

  @Test
  public void simpleLayoutCreatesExpectedInternalNodeTree() {
    final Component component =
        new InlineLayoutSpec(mComponentContext) {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return Column.create(c)
                .child(Column.create(c).child(SimpleMountSpecTester.create(c)))
                .build();
          }
        };

    InternalNode node = Layout.create(mLayoutStateContext, mComponentContext, component);
    assertThat(node.getChildCount()).isEqualTo(1);
    assertThat(node.getHeadComponent()).isEqualTo(component);
    assertThat(node.getTailComponent()).isInstanceOf(Column.class);
    node = node.getChildAt(0);
    assertThat(node.getChildCount()).isEqualTo(1);
    assertThat(node.getTailComponent()).isInstanceOf(Column.class);
    node = node.getChildAt(0);
    assertThat(node.getChildCount()).isEqualTo(0);
    assertThat(node.getTailComponent()).isInstanceOf(SimpleMountSpecTester.class);
  }

  @Test
  public void testHandlersAreAppliedToCorrectInternalNodes() {
    final EventHandler<ClickEvent> clickHandler1 = mock(EventHandler.class);
    final EventHandler<ClickEvent> clickHandler2 = mock(EventHandler.class);
    final EventHandler<ClickEvent> clickHandler3 = mock(EventHandler.class);
    final EventHandler<LongClickEvent> longClickHandler1 = mock(EventHandler.class);
    final EventHandler<LongClickEvent> longClickHandler2 = mock(EventHandler.class);
    final EventHandler<LongClickEvent> longClickHandler3 = mock(EventHandler.class);
    final EventHandler<TouchEvent> touchHandler1 = mock(EventHandler.class);
    final EventHandler<TouchEvent> touchHandler2 = mock(EventHandler.class);
    final EventHandler<TouchEvent> touchHandler3 = mock(EventHandler.class);
    final EventHandler<InterceptTouchEvent> interceptTouchHandler1 = mock(EventHandler.class);
    final EventHandler<InterceptTouchEvent> interceptTouchHandler2 = mock(EventHandler.class);
    final EventHandler<InterceptTouchEvent> interceptTouchHandler3 = mock(EventHandler.class);
    final EventHandler<FocusChangedEvent> focusChangedHandler1 = mock(EventHandler.class);
    final EventHandler<FocusChangedEvent> focusChangedHandler2 = mock(EventHandler.class);
    final EventHandler<FocusChangedEvent> focusChangedHandler3 = mock(EventHandler.class);

    final Component component =
        new InlineLayoutSpec(mComponentContext) {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return Column.create(c)
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
                .build();
          }
        };

    InternalNode node = Layout.create(mLayoutStateContext, mComponentContext, component);
    assertThat(node.getNodeInfo().getClickHandler()).isEqualTo(clickHandler3);
    assertThat(node.getNodeInfo().getLongClickHandler()).isEqualTo(longClickHandler3);
    assertThat(node.getNodeInfo().getTouchHandler()).isEqualTo(touchHandler3);
    assertThat(node.getNodeInfo().getInterceptTouchHandler()).isEqualTo(interceptTouchHandler3);
    assertThat(node.getNodeInfo().getFocusChangeHandler()).isEqualTo(focusChangedHandler3);

    node = node.getChildAt(0);
    assertThat(node.getNodeInfo().getClickHandler()).isEqualTo(clickHandler2);
    assertThat(node.getNodeInfo().getLongClickHandler()).isEqualTo(longClickHandler2);
    assertThat(node.getNodeInfo().getTouchHandler()).isEqualTo(touchHandler2);
    assertThat(node.getNodeInfo().getInterceptTouchHandler()).isEqualTo(interceptTouchHandler2);
    assertThat(node.getNodeInfo().getFocusChangeHandler()).isEqualTo(focusChangedHandler2);

    node = node.getChildAt(0);
    assertThat(node.getNodeInfo().getClickHandler()).isEqualTo(clickHandler1);
    assertThat(node.getNodeInfo().getLongClickHandler()).isEqualTo(longClickHandler1);
    assertThat(node.getNodeInfo().getTouchHandler()).isEqualTo(touchHandler1);
    assertThat(node.getNodeInfo().getInterceptTouchHandler()).isEqualTo(interceptTouchHandler1);
    assertThat(node.getNodeInfo().getFocusChangeHandler()).isEqualTo(focusChangedHandler1);
  }

  @Test
  public void testHandlersAreAppliedToCorrectInternalNodesForSizeDependentComponent() {
    final EventHandler<ClickEvent> clickHandler1 = mock(EventHandler.class);
    final EventHandler<ClickEvent> clickHandler2 = mock(EventHandler.class);
    final EventHandler<ClickEvent> clickHandler3 = mock(EventHandler.class);
    final EventHandler<LongClickEvent> longClickHandler1 = mock(EventHandler.class);
    final EventHandler<LongClickEvent> longClickHandler2 = mock(EventHandler.class);
    final EventHandler<LongClickEvent> longClickHandler3 = mock(EventHandler.class);
    final EventHandler<TouchEvent> touchHandler1 = mock(EventHandler.class);
    final EventHandler<TouchEvent> touchHandler2 = mock(EventHandler.class);
    final EventHandler<TouchEvent> touchHandler3 = mock(EventHandler.class);
    final EventHandler<InterceptTouchEvent> interceptTouchHandler1 = mock(EventHandler.class);
    final EventHandler<InterceptTouchEvent> interceptTouchHandler2 = mock(EventHandler.class);
    final EventHandler<InterceptTouchEvent> interceptTouchHandler3 = mock(EventHandler.class);
    final EventHandler<FocusChangedEvent> focusChangedHandler1 = mock(EventHandler.class);
    final EventHandler<FocusChangedEvent> focusChangedHandler2 = mock(EventHandler.class);
    final EventHandler<FocusChangedEvent> focusChangedHandler3 = mock(EventHandler.class);

    final Component component =
        new InlineLayoutSpec(mComponentContext) {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return Column.create(c)
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
                .build();
          }
        };

    InternalNode node = Layout.create(mLayoutStateContext, mComponentContext, component);
    assertThat(node.getNodeInfo().getClickHandler()).isEqualTo(clickHandler3);
    assertThat(node.getNodeInfo().getLongClickHandler()).isEqualTo(longClickHandler3);
    assertThat(node.getNodeInfo().getTouchHandler()).isEqualTo(touchHandler3);
    assertThat(node.getNodeInfo().getInterceptTouchHandler()).isEqualTo(interceptTouchHandler3);
    assertThat(node.getNodeInfo().getFocusChangeHandler()).isEqualTo(focusChangedHandler3);

    node = node.getChildAt(0);
    assertThat(node.getNodeInfo().getClickHandler()).isEqualTo(clickHandler2);
    assertThat(node.getNodeInfo().getLongClickHandler()).isEqualTo(longClickHandler2);
    assertThat(node.getNodeInfo().getTouchHandler()).isEqualTo(touchHandler2);
    assertThat(node.getNodeInfo().getInterceptTouchHandler()).isEqualTo(interceptTouchHandler2);
    assertThat(node.getNodeInfo().getFocusChangeHandler()).isEqualTo(focusChangedHandler2);

    node = node.getChildAt(0);
    assertThat(node.getNodeInfo().getClickHandler()).isEqualTo(clickHandler1);
    assertThat(node.getNodeInfo().getLongClickHandler()).isEqualTo(longClickHandler1);
    assertThat(node.getNodeInfo().getTouchHandler()).isEqualTo(touchHandler1);
    assertThat(node.getNodeInfo().getInterceptTouchHandler()).isEqualTo(interceptTouchHandler1);
    assertThat(node.getNodeInfo().getFocusChangeHandler()).isEqualTo(focusChangedHandler1);
  }

  @Test
  public void testOverridingHandlers() {
    final EventHandler<ClickEvent> clickHandler1 = mock(EventHandler.class);
    final EventHandler<ClickEvent> clickHandler2 = mock(EventHandler.class);
    final EventHandler<LongClickEvent> longClickHandler1 = mock(EventHandler.class);
    final EventHandler<LongClickEvent> longClickHandler2 = mock(EventHandler.class);
    final EventHandler<TouchEvent> touchHandler1 = mock(EventHandler.class);
    final EventHandler<TouchEvent> touchHandler2 = mock(EventHandler.class);
    final EventHandler<InterceptTouchEvent> interceptTouchHandler1 = mock(EventHandler.class);
    final EventHandler<InterceptTouchEvent> interceptTouchHandler2 = mock(EventHandler.class);
    final EventHandler<FocusChangedEvent> focusChangedHandler1 = mock(EventHandler.class);
    final EventHandler<FocusChangedEvent> focusChangedHandler2 = mock(EventHandler.class);

    final Component component =
        new InlineLayoutSpec(mComponentContext) {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return Wrapper.create(c)
                .delegate(
                    new InlineLayoutSpec() {
                      @Override
                      protected Component onCreateLayout(ComponentContext c) {
                        return SimpleMountSpecTester.create(c)
                            .clickHandler(clickHandler1)
                            .longClickHandler(longClickHandler1)
                            .touchHandler(touchHandler1)
                            .interceptTouchHandler(interceptTouchHandler1)
                            .focusChangeHandler(focusChangedHandler1)
                            .build();
                      }
                    })
                .clickHandler(clickHandler2)
                .longClickHandler(longClickHandler2)
                .touchHandler(touchHandler2)
                .interceptTouchHandler(interceptTouchHandler2)
                .focusChangeHandler(focusChangedHandler2)
                .build();
          }
        };

    InternalNode node = Layout.create(mLayoutStateContext, mComponentContext, component);
    assertThat(node.getChildCount()).isEqualTo(0);
    assertThat(node.getTailComponent()).isInstanceOf(SimpleMountSpecTester.class);
    assertThat(node.getNodeInfo().getClickHandler()).isEqualTo(clickHandler2);
    assertThat(node.getNodeInfo().getLongClickHandler()).isEqualTo(longClickHandler2);
    assertThat(node.getNodeInfo().getTouchHandler()).isEqualTo(touchHandler2);
    assertThat(node.getNodeInfo().getInterceptTouchHandler()).isEqualTo(interceptTouchHandler2);
    assertThat(node.getNodeInfo().getFocusChangeHandler()).isEqualTo(focusChangedHandler2);
  }

  @Test
  public void testOverridingHandlersForSizeDependentComponent() {
    final EventHandler<ClickEvent> clickHandler1 = mock(EventHandler.class);
    final EventHandler<ClickEvent> clickHandler2 = mock(EventHandler.class);
    final EventHandler<LongClickEvent> longClickHandler1 = mock(EventHandler.class);
    final EventHandler<LongClickEvent> longClickHandler2 = mock(EventHandler.class);
    final EventHandler<TouchEvent> touchHandler1 = mock(EventHandler.class);
    final EventHandler<TouchEvent> touchHandler2 = mock(EventHandler.class);
    final EventHandler<InterceptTouchEvent> interceptTouchHandler1 = mock(EventHandler.class);
    final EventHandler<InterceptTouchEvent> interceptTouchHandler2 = mock(EventHandler.class);
    final EventHandler<FocusChangedEvent> focusChangedHandler1 = mock(EventHandler.class);
    final EventHandler<FocusChangedEvent> focusChangedHandler2 = mock(EventHandler.class);

    final Component component =
        new InlineLayoutSpec(mComponentContext) {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return Wrapper.create(c)
                .delegate(
                    new InlineLayoutSpec() {
                      @Override
                      protected Component onCreateLayout(ComponentContext c) {
                        return TestSizeDependentComponent.create(c)
                            .clickHandler(clickHandler1)
                            .longClickHandler(longClickHandler1)
                            .touchHandler(touchHandler1)
                            .interceptTouchHandler(interceptTouchHandler1)
                            .focusChangeHandler(focusChangedHandler1)
                            .build();
                      }
                    })
                .clickHandler(clickHandler2)
                .longClickHandler(longClickHandler2)
                .touchHandler(touchHandler2)
                .interceptTouchHandler(interceptTouchHandler2)
                .focusChangeHandler(focusChangedHandler2)
                .build();
          }
        };

    InternalNode node = Layout.create(mLayoutStateContext, mComponentContext, component);
    assertThat(node.getChildCount()).isEqualTo(0);
    assertThat(node.getTailComponent()).isInstanceOf(TestSizeDependentComponent.class);
    assertThat(node.getNodeInfo().getClickHandler()).isEqualTo(clickHandler2);
    assertThat(node.getNodeInfo().getLongClickHandler()).isEqualTo(longClickHandler2);
    assertThat(node.getNodeInfo().getTouchHandler()).isEqualTo(touchHandler2);
    assertThat(node.getNodeInfo().getInterceptTouchHandler()).isEqualTo(interceptTouchHandler2);
    assertThat(node.getNodeInfo().getFocusChangeHandler()).isEqualTo(focusChangedHandler2);
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public void testAddingAllAttributes() {
    final Drawable background = ComparableColorDrawable.create(Color.RED);
    final Drawable foreground = ComparableColorDrawable.create(Color.BLACK);
    final EventHandler<ClickEvent> clickHandler = mock(EventHandler.class);
    final EventHandler<LongClickEvent> longClickHandler = mock(EventHandler.class);
    final EventHandler<TouchEvent> touchHandler = mock(EventHandler.class);
    final EventHandler<InterceptTouchEvent> interceptTouchHandler = mock(EventHandler.class);
    final EventHandler<FocusChangedEvent> focusChangedHandler = mock(EventHandler.class);
    final EventHandler<VisibleEvent> visibleHandler = mock(EventHandler.class);
    final EventHandler<FocusedVisibleEvent> focusedHandler = mock(EventHandler.class);
    final EventHandler<UnfocusedVisibleEvent> unfocusedHandler = mock(EventHandler.class);
    final EventHandler<FullImpressionVisibleEvent> fullImpressionHandler = mock(EventHandler.class);
    final EventHandler<InvisibleEvent> invisibleHandler = mock(EventHandler.class);
    final EventHandler<VisibilityChangedEvent> visibleRectChangedHandler = mock(EventHandler.class);
    final Object viewTag = new Object();
    final SparseArray<Object> viewTags = new SparseArray<>();
    final EventHandler<DispatchPopulateAccessibilityEventEvent>
        dispatchPopulateAccessibilityEventHandler = mock(EventHandler.class);
    final EventHandler<OnInitializeAccessibilityEventEvent> onInitializeAccessibilityEventHandler =
        mock(EventHandler.class);
    final EventHandler<OnInitializeAccessibilityNodeInfoEvent>
        onInitializeAccessibilityNodeInfoHandler = mock(EventHandler.class);
    final EventHandler<OnPopulateAccessibilityEventEvent> onPopulateAccessibilityEventHandler =
        mock(EventHandler.class);
    final EventHandler<OnRequestSendAccessibilityEventEvent>
        onRequestSendAccessibilityEventHandler = mock(EventHandler.class);
    final EventHandler<PerformAccessibilityActionEvent> performAccessibilityActionHandler =
        mock(EventHandler.class);
    final EventHandler<SendAccessibilityEventEvent> sendAccessibilityEventHandler =
        mock(EventHandler.class);
    final EventHandler<SendAccessibilityEventUncheckedEvent>
        sendAccessibilityEventUncheckedHandler = mock(EventHandler.class);
    final StateListAnimator stateListAnimator = mock(StateListAnimator.class);

    final Component component =
        TestDrawableComponentWithMockInternalNode.create(mComponentContext)
            .layoutDirection(YogaDirection.INHERIT)
            .alignSelf(YogaAlign.AUTO)
            .positionType(YogaPositionType.ABSOLUTE)
            .flex(2)
            .flexGrow(3)
            .flexShrink(4)
            .flexBasisPx(5)
            .flexBasisPercent(6)
            .importantForAccessibility(ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_AUTO)
            .duplicateParentState(false)
            .marginPx(YogaEdge.ALL, 5)
            .marginPx(YogaEdge.RIGHT, 6)
            .marginPx(YogaEdge.LEFT, 4)
            .marginPercent(YogaEdge.ALL, 10)
            .marginPercent(YogaEdge.VERTICAL, 12)
            .marginPercent(YogaEdge.RIGHT, 5)
            .marginAuto(YogaEdge.LEFT)
            .marginAuto(YogaEdge.TOP)
            .marginAuto(YogaEdge.RIGHT)
            .marginAuto(YogaEdge.BOTTOM)
            .paddingPx(YogaEdge.ALL, 1)
            .paddingPx(YogaEdge.RIGHT, 2)
            .paddingPx(YogaEdge.LEFT, 3)
            .paddingPercent(YogaEdge.VERTICAL, 7)
            .paddingPercent(YogaEdge.RIGHT, 6)
            .paddingPercent(YogaEdge.ALL, 5)
            .positionPx(YogaEdge.ALL, 11)
            .positionPx(YogaEdge.RIGHT, 12)
            .positionPx(YogaEdge.LEFT, 13)
            .positionPercent(YogaEdge.VERTICAL, 17)
            .positionPercent(YogaEdge.RIGHT, 16)
            .positionPercent(YogaEdge.ALL, 15)
            .widthPx(5)
            .widthPercent(50)
            .minWidthPx(15)
            .minWidthPercent(100)
            .maxWidthPx(25)
            .maxWidthPercent(26)
            .heightPx(30)
            .heightPercent(31)
            .minHeightPx(32)
            .minHeightPercent(33)
            .maxHeightPx(34)
            .maxHeightPercent(35)
            .aspectRatio(20)
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
            .visibleHeightRatio(55)
            .visibleWidthRatio(56)
            .visibleHandler(visibleHandler)
            .focusedHandler(focusedHandler)
            .unfocusedHandler(unfocusedHandler)
            .fullImpressionHandler(fullImpressionHandler)
            .invisibleHandler(invisibleHandler)
            .visibilityChangedHandler(visibleRectChangedHandler)
            .contentDescription("test")
            .viewTag(viewTag)
            .viewTags(viewTags)
            .shadowElevationPx(60)
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
            .build();

    component.setScopedContext(mComponentContext);

    final InternalNode node = Layout.create(mLayoutStateContext, mComponentContext, component);
    final NodeInfo nodeInfo = node.getOrCreateNodeInfo();
    final CopyableLayoutProps input = component.getCommonProps().getLayoutProps();
    final LayoutProps output = spy(LayoutProps.class);

    input.copyInto(output);

    verify(output).layoutDirection(YogaDirection.INHERIT);
    verify(output).alignSelf(YogaAlign.AUTO);
    verify(output).positionType(YogaPositionType.ABSOLUTE);
    verify(output).flex(2);
    verify(output).flexGrow(3);
    verify(output).flexShrink(4);
    verify(output).flexBasisPx(5);
    verify(output).flexBasisPercent(6);

    verify(node)
        .importantForAccessibility(ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    verify(node).duplicateParentState(false);

    verify(output).marginPx(YogaEdge.ALL, 5);
    verify(output).marginPx(YogaEdge.RIGHT, 6);
    verify(output).marginPx(YogaEdge.LEFT, 4);
    verify(output).marginPercent(YogaEdge.ALL, 10);
    verify(output).marginPercent(YogaEdge.VERTICAL, 12);
    verify(output).marginPercent(YogaEdge.RIGHT, 5);
    verify(output).marginAuto(YogaEdge.LEFT);
    verify(output).marginAuto(YogaEdge.TOP);
    verify(output).marginAuto(YogaEdge.RIGHT);
    verify(output).marginAuto(YogaEdge.BOTTOM);

    verify(output).paddingPx(YogaEdge.ALL, 1);
    verify(output).paddingPx(YogaEdge.RIGHT, 2);
    verify(output).paddingPx(YogaEdge.LEFT, 3);
    verify(output).paddingPercent(YogaEdge.VERTICAL, 7);
    verify(output).paddingPercent(YogaEdge.RIGHT, 6);
    verify(output).paddingPercent(YogaEdge.ALL, 5);

    verify(output).positionPx(YogaEdge.ALL, 11);
    verify(output).positionPx(YogaEdge.RIGHT, 12);
    verify(output).positionPx(YogaEdge.LEFT, 13);
    verify(output).positionPercent(YogaEdge.VERTICAL, 17);
    verify(output).positionPercent(YogaEdge.RIGHT, 16);
    verify(output).positionPercent(YogaEdge.ALL, 15);

    verify(output).widthPx(5);
    verify(output).widthPercent(50);
    verify(output).minWidthPx(15);
    verify(output).minWidthPercent(100);
    verify(output).maxWidthPx(25);
    verify(output).maxWidthPercent(26);

    verify(output).heightPx(30);
    verify(output).heightPercent(31);
    verify(output).minHeightPx(32);
    verify(output).minHeightPercent(33);
    verify(output).maxHeightPx(34);
    verify(output).maxHeightPercent(35);

    verify(output).aspectRatio(20);

    verify(node).touchExpansionPx(YogaEdge.RIGHT, 22);
    verify(node).touchExpansionPx(YogaEdge.LEFT, 23);
    verify(node).touchExpansionPx(YogaEdge.ALL, 21);

    verify(node).background(background);
    verify(node).foreground(foreground);

    verify(node).wrapInView();

    verify(nodeInfo).setClickHandler(clickHandler);
    verify(nodeInfo).setFocusChangeHandler(focusChangedHandler);
    verify(nodeInfo).setLongClickHandler(longClickHandler);
    verify(nodeInfo).setTouchHandler(touchHandler);
    verify(nodeInfo).setInterceptTouchHandler(interceptTouchHandler);

    verify(nodeInfo).setFocusable(true);
    verify(nodeInfo).setSelected(false);
    verify(nodeInfo).setEnabled(false);
    verify(node).visibleHeightRatio(55);
    verify(node).visibleWidthRatio(56);
    verify(nodeInfo).setAccessibilityHeading(false);

    verify(node).visibleHandler(visibleHandler);
    verify(node).focusedHandler(focusedHandler);
    verify(node).unfocusedHandler(unfocusedHandler);
    verify(node).fullImpressionHandler(fullImpressionHandler);
    verify(node).invisibleHandler(invisibleHandler);
    verify(node).visibilityChangedHandler(visibleRectChangedHandler);

    verify(nodeInfo).setContentDescription("test");

    verify(nodeInfo).setViewTag(viewTag);
    verify(nodeInfo).setViewTags(viewTags);

    verify(nodeInfo).setShadowElevation(60);

    verify(nodeInfo).setClipToOutline(false);
    verify(node).transitionKey(eq("transitionKey"), nullable(String.class));
    verify(node).transitionKeyType(Transition.TransitionKeyType.GLOBAL);
    verify(node).testKey("testKey");

    verify(nodeInfo).setAccessibilityRole(AccessibilityRole.BUTTON);
    verify(nodeInfo).setAccessibilityRoleDescription("Test Role Description");
    verify(nodeInfo)
        .setDispatchPopulateAccessibilityEventHandler(dispatchPopulateAccessibilityEventHandler);
    verify(nodeInfo)
        .setOnInitializeAccessibilityEventHandler(onInitializeAccessibilityEventHandler);
    verify(nodeInfo)
        .setOnInitializeAccessibilityNodeInfoHandler(onInitializeAccessibilityNodeInfoHandler);
    verify(nodeInfo).setOnPopulateAccessibilityEventHandler(onPopulateAccessibilityEventHandler);
    verify(nodeInfo)
        .setOnRequestSendAccessibilityEventHandler(onRequestSendAccessibilityEventHandler);
    verify(nodeInfo).setPerformAccessibilityActionHandler(performAccessibilityActionHandler);
    verify(nodeInfo).setSendAccessibilityEventHandler(sendAccessibilityEventHandler);
    verify(nodeInfo)
        .setSendAccessibilityEventUncheckedHandler(sendAccessibilityEventUncheckedHandler);

    verify(node).stateListAnimator(stateListAnimator);
  }

  private static class TestDrawableComponentWithMockInternalNode extends TestComponent {

    @Override
    protected boolean canResolve() {
      return true;
    }

    protected InternalNode resolve(LayoutStateContext layoutContext, ComponentContext c) {
      LithoLayoutResult result = mock(LithoLayoutResult.class);
      InputOnlyInternalNode node = mock(InputOnlyInternalNode.class);
      NodeInfo nodeInfo = mock(NodeInfo.class);
      when(node.getOrCreateNodeInfo()).thenReturn(nodeInfo);
      when(node.calculateLayout(any(RenderState.LayoutContext.class), anyInt(), anyInt()))
          .thenReturn(result);
      when(result.getInternalNode()).thenReturn(node);
      return node;
    }

    public static TestDrawableComponentWithMockInternalNode.Builder create(ComponentContext c) {
      Component component = new TestDrawableComponentWithMockInternalNode();
      return new Builder(c, 0, 0, component);
    }

    public static class Builder
        extends com.facebook.litho.Component.Builder<
            TestDrawableComponentWithMockInternalNode.Builder> {

      private Component mComponent;

      private Builder(ComponentContext c, int defStyleAttr, int defStyleRes, Component component) {
        super(c, defStyleAttr, defStyleRes, component);
        mComponent = component;
      }

      @Override
      protected void setComponent(Component component) {
        mComponent = component;
      }

      @Override
      public Builder getThis() {
        return this;
      }

      @Override
      public Component build() {
        return mComponent;
      }
    }
  }
}
