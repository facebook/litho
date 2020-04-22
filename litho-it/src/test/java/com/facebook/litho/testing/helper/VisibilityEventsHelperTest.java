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

package com.facebook.litho.testing.helper;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.EventHandler;
import com.facebook.litho.FocusedVisibleEvent;
import com.facebook.litho.FullImpressionVisibleEvent;
import com.facebook.litho.InvisibleEvent;
import com.facebook.litho.Row;
import com.facebook.litho.UnfocusedVisibleEvent;
import com.facebook.litho.VisibleEvent;
import com.facebook.litho.testing.TestLayoutComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

/** Tests {@link VisibilityEventsHelper} */
@RunWith(ComponentsTestRunner.class)
public class VisibilityEventsHelperTest {

  @Mock public EventHandler<VisibleEvent> mVisibleEventEventHandler;
  @Mock public EventHandler<InvisibleEvent> mInvisibleEventEventHandler;
  @Mock public EventHandler<FocusedVisibleEvent> mFocusedVisibleEventEventHandler;
  @Mock public EventHandler<UnfocusedVisibleEvent> mUnfocusedVisibleEventEventHandler;
  @Mock public EventHandler<FullImpressionVisibleEvent> mFullImpressionEventEventHandler;

  private ComponentContext mContext;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void triggerVisibleEventForEventsShouldDispatchHandler() {
    assertThat(
            VisibilityEventsHelper.triggerVisibilityEvent(
                getComponentTreeWithHandlers(), VisibleEvent.class))
        .isTrue();

    verify(mVisibleEventEventHandler).dispatchEvent(any(VisibleEvent.class));
  }

  @Test
  public void triggerInvisibleEventForEventsShouldDispatchHandler() {
    assertThat(
            VisibilityEventsHelper.triggerVisibilityEvent(
                getComponentTreeWithHandlers(), InvisibleEvent.class))
        .isTrue();

    verify(mInvisibleEventEventHandler).dispatchEvent(any(InvisibleEvent.class));
  }

  @Test
  public void triggerFocusedEventForEventsShouldDispatchHandler() {
    assertThat(
            VisibilityEventsHelper.triggerVisibilityEvent(
                getComponentTreeWithHandlers(), FocusedVisibleEvent.class))
        .isTrue();

    verify(mFocusedVisibleEventEventHandler).dispatchEvent(any(FocusedVisibleEvent.class));
  }

  @Test
  public void triggerUnfocusedEventForEventsShouldDispatchHandler() {
    assertThat(
            VisibilityEventsHelper.triggerVisibilityEvent(
                getComponentTreeWithHandlers(), UnfocusedVisibleEvent.class))
        .isTrue();

    verify(mUnfocusedVisibleEventEventHandler).dispatchEvent(any(UnfocusedVisibleEvent.class));
  }

  @Test
  public void triggerFullImpressionEventForEventsShouldDispatchHandler() {
    assertThat(
            VisibilityEventsHelper.triggerVisibilityEvent(
                getComponentTreeWithHandlers(), FullImpressionVisibleEvent.class))
        .isTrue();

    verify(mFullImpressionEventEventHandler).dispatchEvent(any(FullImpressionVisibleEvent.class));
  }

  @Test
  public void triggerEventWithoutHandlerShouldNotDispatchHandler() {
    Component component = TestLayoutComponent.create(mContext).build();
    assertThat(
            VisibilityEventsHelper.triggerVisibilityEvent(
                getComponentTree(component), VisibleEvent.class))
        .isFalse();
  }

  @Test
  public void triggerEventShouldFindFirstHandler() {
    Component component =
        Row.create(mContext)
            .child(Row.create(mContext).build())
            .child(Row.create(mContext).visibleHandler(mVisibleEventEventHandler).build())
            .build();
    assertThat(
            VisibilityEventsHelper.triggerVisibilityEvent(
                getComponentTree(component), VisibleEvent.class))
        .isTrue();

    verify(mVisibleEventEventHandler).dispatchEvent(any(VisibleEvent.class));
  }

  private Component getComponentWithHandlers() {
    return Row.create(mContext)
        .visibleHandler(mVisibleEventEventHandler)
        .invisibleHandler(mInvisibleEventEventHandler)
        .focusedHandler(mFocusedVisibleEventEventHandler)
        .unfocusedHandler(mUnfocusedVisibleEventEventHandler)
        .fullImpressionHandler(mFullImpressionEventEventHandler)
        .build();
  }

  private ComponentTree getComponentTree(Component component) {
    return ComponentTestHelper.mountComponent(mContext, component).getComponentTree();
  }

  private ComponentTree getComponentTreeWithHandlers() {
    return getComponentTree(getComponentWithHandlers());
  }
}
