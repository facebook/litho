/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.RuntimeEnvironment.application;

import android.animation.StateListAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.SparseArray;
import androidx.annotation.AttrRes;
import androidx.annotation.StyleRes;
import com.facebook.litho.annotations.ImportantForAccessibility;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.drawable.ComparableColorDrawable;
import com.facebook.litho.drawable.ComparableDrawable;
import com.facebook.litho.testing.TestComponent;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestSizeDependentComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaPositionType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(ComponentsTestRunner.class)
public class LayoutStateCreateTreeTest {
  private ComponentContext mComponentContext;

  @Before
  public void setup() throws Exception {
    mComponentContext = new ComponentContext(RuntimeEnvironment.application);
    mComponentContext.setLayoutStateReferenceWrapperForTesting();
  }

  @After
  public void after() {
    ComponentsConfiguration.isConsistentComponentHierarchyExperimentEnabled = false;
    mComponentContext = null;
  }

  @Test
  public void testSimpleLayoutCreatesExpectedInternalNodeTree() {
    final Component component =
        new InlineLayoutSpec(mComponentContext) {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return Column.create(c)
                .child(Column.create(c).child(TestDrawableComponent.create(c)))
                .build();
          }
        };

    InternalNode node = LayoutState.createTree(component, mComponentContext, null);
    assertThat(node.getChildCount()).isEqualTo(1);
    assertThat(node.getTailComponent()).isEqualTo(component);
    node = node.getChildAt(0);
    assertThat(node.getChildCount()).isEqualTo(1);
    assertThat(node.getTailComponent()).isInstanceOf(Column.class);
    node = node.getChildAt(0);
    assertThat(node.getChildCount()).isEqualTo(0);
    assertThat(node.getTailComponent()).isInstanceOf(TestDrawableComponent.class);
  }

  @Test
  public void simpleLayoutCreatesExpectedInternalNodeTreeWithConsistentHierarchyExperiment() {
    ComponentsConfiguration.isConsistentComponentHierarchyExperimentEnabled = true;

    final Component component =
        new InlineLayoutSpec(mComponentContext) {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return Column.create(c)
                .child(Column.create(c).child(TestDrawableComponent.create(c)))
                .build();
          }
        };

    InternalNode node = LayoutState.createTree(component, mComponentContext, null);
    assertThat(node.getChildCount()).isEqualTo(1);
    assertThat(node.getHeadComponent()).isEqualTo(component);
    assertThat(node.getTailComponent()).isInstanceOf(Column.class);
    node = node.getChildAt(0);
    assertThat(node.getChildCount()).isEqualTo(1);
    assertThat(node.getTailComponent()).isInstanceOf(Column.class);
    node = node.getChildAt(0);
    assertThat(node.getChildCount()).isEqualTo(0);
    assertThat(node.getTailComponent()).isInstanceOf(TestDrawableComponent.class);
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
                            TestDrawableComponent.create(c)
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

    InternalNode node = LayoutState.createTree(component, mComponentContext, null);
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

    InternalNode node = LayoutState.createTree(component, mComponentContext, null);
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
                        return TestDrawableComponent.create(c)
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

    InternalNode node = LayoutState.createTree(component, mComponentContext, null);
    assertThat(node.getChildCount()).isEqualTo(0);
    assertThat(node.getTailComponent()).isInstanceOf(TestDrawableComponent.class);
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

    InternalNode node = LayoutState.createTree(component, mComponentContext, null);
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
    final ComparableDrawable background = ComparableColorDrawable.create(Color.RED);
    final ComparableDrawable foreground = ComparableColorDrawable.create(Color.BLACK);
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
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return TestDrawableComponentWithMockInternalNode.create(c)
                .layoutDirection(YogaDirection.INHERIT)
                .alignSelf(YogaAlign.AUTO)
                .positionType(YogaPositionType.ABSOLUTE)
                .flex(2)
                .flexGrow(3)
                .flexShrink(4)
                .flexBasisPx(5)
                .flexBasisPercent(6)
                .importantForAccessibility(
                    ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_AUTO)
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
                .dispatchPopulateAccessibilityEventHandler(
                    dispatchPopulateAccessibilityEventHandler)
                .onInitializeAccessibilityEventHandler(onInitializeAccessibilityEventHandler)
                .onInitializeAccessibilityNodeInfoHandler(onInitializeAccessibilityNodeInfoHandler)
                .onPopulateAccessibilityEventHandler(onPopulateAccessibilityEventHandler)
                .onRequestSendAccessibilityEventHandler(onRequestSendAccessibilityEventHandler)
                .performAccessibilityActionHandler(performAccessibilityActionHandler)
                .sendAccessibilityEventHandler(sendAccessibilityEventHandler)
                .sendAccessibilityEventUncheckedHandler(sendAccessibilityEventUncheckedHandler)
                .stateListAnimator(stateListAnimator)
                .build();
          }
        };

    component.setScopedContext(mComponentContext);

    InternalNode node = LayoutState.createTree(component, mComponentContext, null);
    NodeInfo nodeInfo = node.getOrCreateNodeInfo();

    verify(node).layoutDirection(YogaDirection.INHERIT);
    verify(node).alignSelf(YogaAlign.AUTO);
    verify(node).positionType(YogaPositionType.ABSOLUTE);
    verify(node).flex(2);
    verify(node).flexGrow(3);
    verify(node).flexShrink(4);
    verify(node).flexBasisPx(5);
    verify(node).flexBasisPercent(6);

    verify(node)
        .importantForAccessibility(ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    verify(node).duplicateParentState(false);

    verify(node).marginPx(YogaEdge.ALL, 5);
    verify(node).marginPx(YogaEdge.RIGHT, 6);
    verify(node).marginPx(YogaEdge.LEFT, 4);
    verify(node).marginPercent(YogaEdge.ALL, 10);
    verify(node).marginPercent(YogaEdge.VERTICAL, 12);
    verify(node).marginPercent(YogaEdge.RIGHT, 5);
    verify(node).marginAuto(YogaEdge.LEFT);
    verify(node).marginAuto(YogaEdge.TOP);
    verify(node).marginAuto(YogaEdge.RIGHT);
    verify(node).marginAuto(YogaEdge.BOTTOM);

    verify(node).paddingPx(YogaEdge.ALL, 1);
    verify(node).paddingPx(YogaEdge.RIGHT, 2);
    verify(node).paddingPx(YogaEdge.LEFT, 3);
    verify(node).paddingPercent(YogaEdge.VERTICAL, 7);
    verify(node).paddingPercent(YogaEdge.RIGHT, 6);
    verify(node).paddingPercent(YogaEdge.ALL, 5);

    verify(node).positionPx(YogaEdge.ALL, 11);
    verify(node).positionPx(YogaEdge.RIGHT, 12);
    verify(node).positionPx(YogaEdge.LEFT, 13);
    verify(node).positionPercent(YogaEdge.VERTICAL, 17);
    verify(node).positionPercent(YogaEdge.RIGHT, 16);
    verify(node).positionPercent(YogaEdge.ALL, 15);

    verify(node).widthPx(5);
    verify(node).widthPercent(50);
    verify(node).minWidthPx(15);
    verify(node).minWidthPercent(100);
    verify(node).maxWidthPx(25);
    verify(node).maxWidthPercent(26);

    verify(node).heightPx(30);
    verify(node).heightPercent(31);
    verify(node).minHeightPx(32);
    verify(node).minHeightPercent(33);
    verify(node).maxHeightPx(34);
    verify(node).maxHeightPercent(35);

    verify(node).aspectRatio(20);

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
    verify(node).transitionKey("transitionKey");
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

  @Test
  public void testCopyPropsOnlyCalledOnce() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return Column.create(c).child(Column.create(c).flexGrow(1)).build();
          }
        };

    final ComponentContext c = new MockInternalNodeComponentContext(application);

    final InternalNode root = LayoutState.createAndMeasureTreeForComponent(c, component, 800, 600);

    assertThat(root.getChildAt(0) instanceof TestInternalNode).isTrue();
    assertThat(((TestInternalNode) root.getChildAt(0)).mFlexGrowCounter).isEqualTo(1);
  }

  private static class TestDrawableComponentWithMockInternalNode extends TestComponent {

    @Override
    protected boolean canResolve() {
      return true;
    }

    protected ComponentLayout resolve(ComponentContext c) {
      InternalNode node = mock(InternalNode.class);
      NodeInfo nodeInfo = mock(NodeInfo.class);
      when(node.getOrCreateNodeInfo()).thenReturn(nodeInfo);
      return node;
    }

    public static TestDrawableComponentWithMockInternalNode.Builder create(ComponentContext c) {
      Builder builder = new Builder();
      builder.mComponent = new TestDrawableComponentWithMockInternalNode();
      builder.init(c, 0, 0, builder.mComponent);

      return builder;
    }

    public static class Builder
        extends com.facebook.litho.Component.Builder<
            TestDrawableComponentWithMockInternalNode.Builder> {

      private Component mComponent;

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

  private class MockInternalNodeComponentContext extends ComponentContext {

    private MockInternalNodeComponentContext(Context context) {
      super(context);
      setLayoutStateReferenceWrapperForTesting();
    }

    InternalNode newLayoutBuilder(@AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
      return new TestInternalNode(this);
    }

    @Override
    ComponentContext makeNewCopy() {
      MockInternalNodeComponentContext copy =
          new MockInternalNodeComponentContext(this.getAndroidContext());
      copy.setLayoutStateReferenceWrapperForTesting();

      return copy;
    }
  }

  private class TestInternalNode extends DefaultInternalNode {
    private int mFlexGrowCounter;

    protected TestInternalNode(ComponentContext componentContext) {
      super(componentContext);
    }

    @Override
    public void flexGrow(float flex) {
      mFlexGrowCounter++;
    }
  }
}
