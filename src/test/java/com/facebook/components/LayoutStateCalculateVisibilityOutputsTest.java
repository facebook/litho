/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import android.content.Context;

import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.components.testing.TestDrawableComponent;
import com.facebook.components.testing.TestLayoutComponent;
import com.facebook.components.testing.TestNullLayoutComponent;
import com.facebook.yoga.YogaEdge;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;

@PrepareForTest({Component.class})
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*"})
@RunWith(ComponentsTestRunner.class)
public class LayoutStateCalculateVisibilityOutputsTest {

  @Rule
  public PowerMockRule mPowerMockRule = new PowerMockRule();

  @Before
  public void setup() throws Exception {
  }

  @Test
  public void testNoUnnecessaryVisibilityOutputs() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c)
            .child(
                Container.create(c)
                    .child(
                        TestDrawableComponent.create(c)
                            .withLayout()
                            .visibleHandler(c.newEventHandler(1))))
            .child(
                TestDrawableComponent.create(c)
                    .withLayout()
                    .invisibleHandler(c.newEventHandler(2)))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY));

    assertEquals(2, layoutState.getVisibilityOutputCount());
  }

  @Test
  public void testNoUnnecessaryVisibilityOutputsWithFullImpression() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c)
            .child(
                Container.create(c)
                    .child(
                        TestDrawableComponent.create(c)
                            .withLayout()
                            .visibleHandler(c.newEventHandler(1))))
            .child(
                TestDrawableComponent.create(c)
                    .withLayout()
                    .fullImpressionHandler(c.newEventHandler(3)))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY));

    assertEquals(2, layoutState.getVisibilityOutputCount());
  }

  @Test
  public void testNoUnnecessaryVisibilityOutputsWithFocused() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c)
            .child(
                Container.create(c)
                    .child(
                        TestDrawableComponent.create(c)
                            .withLayout()
                            .visibleHandler(c.newEventHandler(1))))
            .child(
                TestDrawableComponent.create(c)
                    .withLayout()
                    .focusedHandler(c.newEventHandler(4)))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY));

    assertEquals(2, layoutState.getVisibilityOutputCount());
  }

  @Test
  public void testVisibilityOutputsForDelegateComponents() {
    final boolean isDelegate = true;
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c)
            .child(
                TestLayoutComponent.create(c, 0, 0, true, true, false, isDelegate)
                    .withLayout()
                    .visibleHandler(c.newEventHandler(1)))
            .wrapInView()
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY));

    assertEquals(1, layoutState.getVisibilityOutputCount());
  }

  @Test
  public void testLayoutOutputsForDeepLayoutSpecs() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c)
            .child(
                Container.create(c)
                    .child(
                        TestLayoutComponent.create(c)
                            .withLayout()
                            .visibleHandler(c.newEventHandler(1)))
                    .invisibleHandler(c.newEventHandler(2)))
            .child(
                Container.create(c)
                    .child(
                        TestLayoutComponent.create(c)
                            .withLayout()
                            .invisibleHandler(c.newEventHandler(1)))
                    .visibleHandler(c.newEventHandler(2)))
            .wrapInView()
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY));

    // Check total layout outputs.
    assertEquals(4, layoutState.getVisibilityOutputCount());

    // Check number of Components with VisibleEvent handlers.
    int visibleHandlerCount = 0;
    for (int i = 0; i < layoutState.getVisibilityOutputCount(); i++) {
      if (layoutState.getVisibilityOutputAt(i).getVisibleEventHandler() != null) {
        visibleHandlerCount += 1;
      }
    }

    assertEquals(2, visibleHandlerCount);
  }

  @Test
  public void testLayoutOutputsForForceWrappedComponent() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c)
            .child(
                TestDrawableComponent.create(c)
                    .withLayout()
                    .visibleHandler(c.newEventHandler(1))
                    .wrapInView())
            .build();
      }
    };

    final LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY));

    assertEquals(1, layoutState.getVisibilityOutputCount());
  }

  @Test
  public void testLayoutOutputForRootWithNullLayout() {
    final Component componentWithNullLayout = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return null;
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        componentWithNullLayout,
        -1,
        SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY));

    assertEquals(0, layoutState.getVisibilityOutputCount());
  }

  @Test
  public void testLayoutComponentForNestedTreeChildWithNullLayout() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c)
            .paddingPx(YogaEdge.ALL, 2)
