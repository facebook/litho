/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class LayoutStateCreateTreeTest {
  private ComponentContext mComponentContext;

  @Before
  public void setup() throws Exception {
    mComponentContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testSimpleLayoutCreatesExpectedInternalNodeTree() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(final ComponentContext c) {
            return Column.create(c)
                .child(Column.create(c).child(TestDrawableComponent.create(c)))
                .build();
          }
        };

    InternalNode node = LayoutState.createTree(component, mComponentContext);
    assertThat(node.getChildCount()).isEqualTo(1);
    assertThat(node.getRootComponent()).isEqualTo(component);
    node = node.getChildAt(0);
    assertThat(node.getChildCount()).isEqualTo(1);
    assertThat(node.getRootComponent()).isNull();
    node = node.getChildAt(0);
    assertThat(node.getChildCount()).isEqualTo(0);
    assertThat(node.getRootComponent().getLifecycle()).isInstanceOf(TestDrawableComponent.class);
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
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(final ComponentContext c) {
            return Column.create(c)
                .child(
                    Column.create(c)
                        .child(
                            TestDrawableComponent.create(c)
                                .withLayout()
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

    InternalNode node = LayoutState.createTree(component, mComponentContext);
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
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(final ComponentContext c) {
            return Layout.create(
                    c,
                    new InlineLayoutSpec() {
                      @Override
                      protected ComponentLayout onCreateLayout(ComponentContext c) {
                        return TestDrawableComponent.create(c)
                            .withLayout()
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

    InternalNode node = LayoutState.createTree(component, mComponentContext);
    assertThat(node.getChildCount()).isEqualTo(0);
    assertThat(node.getRootComponent().getLifecycle()).isInstanceOf(TestDrawableComponent.class);
    assertThat(node.getNodeInfo().getClickHandler()).isEqualTo(clickHandler2);
    assertThat(node.getNodeInfo().getLongClickHandler()).isEqualTo(longClickHandler2);
    assertThat(node.getNodeInfo().getTouchHandler()).isEqualTo(touchHandler2);
    assertThat(node.getNodeInfo().getInterceptTouchHandler()).isEqualTo(interceptTouchHandler2);
    assertThat(node.getNodeInfo().getFocusChangeHandler()).isEqualTo(focusChangedHandler2);
  }
}
