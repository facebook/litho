/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static com.facebook.litho.Column.create;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.yoga.YogaEdge.ALL;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.robolectric.RuntimeEnvironment.application;

import android.content.Context;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestLayoutComponent;
import com.facebook.litho.testing.TestNullLayoutComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class LayoutStateCalculateVisibilityOutputsTest {

  @Before
  public void setup() throws Exception {
  }

  @Test
  public void testNoUnnecessaryVisibilityOutputs() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return create(c)
            .child(
                create(c)
                    .child(
                        TestDrawableComponent.create(c)
                            .visibleHandler(c.newEventHandler(1))))
            .child(
                TestDrawableComponent.create(c)
                    .invisibleHandler(c.newEventHandler(2)))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        application,
        component,
        -1,
        makeSizeSpec(100, EXACTLY),
        makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getVisibilityOutputCount()).isEqualTo(2);
  }

  @Test
  public void testNoUnnecessaryVisibilityOutputsWithFullImpression() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return create(c)
            .child(
                create(c)
                    .child(
                        TestDrawableComponent.create(c)
                            .visibleHandler(c.newEventHandler(1))))
            .child(
                TestDrawableComponent.create(c)
                    .fullImpressionHandler(c.newEventHandler(3)))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        application,
        component,
        -1,
        makeSizeSpec(100, EXACTLY),
        makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getVisibilityOutputCount()).isEqualTo(2);
  }

  @Test
  public void testNoUnnecessaryVisibilityOutputsWithFocused() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return create(c)
            .child(
                create(c)
                    .child(
                        TestDrawableComponent.create(c)
                            .visibleHandler(c.newEventHandler(1))))
            .child(
                TestDrawableComponent.create(c)
                    .focusedHandler(c.newEventHandler(4)))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        application,
        component,
        -1,
        makeSizeSpec(100, EXACTLY),
        makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getVisibilityOutputCount()).isEqualTo(2);
  }

  @Test
  public void testVisibilityOutputsForDelegateComponents() {
    final boolean isDelegate = true;
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return create(c)
            .child(
                TestLayoutComponent.create(c, 0, 0, true, true, false, isDelegate)
                    .visibleHandler(c.newEventHandler(1)))
            .wrapInView()
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        application,
        component,
        -1,
        makeSizeSpec(100, EXACTLY),
        makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getVisibilityOutputCount()).isEqualTo(1);
  }

  @Test
  public void testLayoutOutputsForDeepLayoutSpecs() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return create(c)
            .child(
                create(c)
                    .child(
                        TestLayoutComponent.create(c)
                            .visibleHandler(c.newEventHandler(1)))
                    .invisibleHandler(c.newEventHandler(2)))
            .child(
                create(c)
                    .child(
                        TestLayoutComponent.create(c)
                            .invisibleHandler(c.newEventHandler(1)))
                    .visibleHandler(c.newEventHandler(2)))
            .wrapInView()
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        application,
        component,
        -1,
        makeSizeSpec(350, EXACTLY),
        makeSizeSpec(200, EXACTLY));

    // Check total layout outputs.
    assertThat(layoutState.getVisibilityOutputCount()).isEqualTo(4);

    // Check number of Components with VisibleEvent handlers.
    int visibleHandlerCount = 0;
    for (int i = 0; i < layoutState.getVisibilityOutputCount(); i++) {
      if (layoutState.getVisibilityOutputAt(i).getVisibleEventHandler() != null) {
        visibleHandlerCount += 1;
      }
    }

    assertThat(visibleHandlerCount).isEqualTo(2);
  }

  @Test
  public void testLayoutOutputsForForceWrappedComponent() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return create(c)
            .child(
                TestDrawableComponent.create(c)
                    .visibleHandler(c.newEventHandler(1))
                    .wrapInView())
            .build();
      }
    };

    final LayoutState layoutState = calculateLayoutState(
        application,
        component,
        -1,
        makeSizeSpec(100, EXACTLY),
        makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getVisibilityOutputCount()).isEqualTo(1);
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
        application,
        componentWithNullLayout,
        -1,
        makeSizeSpec(350, EXACTLY),
        makeSizeSpec(200, EXACTLY));

    assertThat(layoutState.getVisibilityOutputCount()).isEqualTo(0);
  }

  @Test
  public void testLayoutComponentForNestedTreeChildWithNullLayout() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return create(c)
            .paddingPx(ALL, 2)
            .child(new TestNullLayoutComponent())
            .invisibleHandler(c.newEventHandler(2))
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        application,
        component,
        -1,
        makeSizeSpec(350, EXACTLY),
        makeSizeSpec(200, EXACTLY));

    assertThat(layoutState.getVisibilityOutputCount()).isEqualTo(1);
  }

  private static LayoutState calculateLayoutState(
      Context context,
      Component component,
      int componentTreeId,
      int widthSpec,
      int heightSpec) {

    return LayoutState.calculate(
        new ComponentContext(context),
        component,
        componentTreeId,
        widthSpec,
        heightSpec,
        false /* shouldGenerateDiffTree */,
        null /* previousDiffTreeRoot */,
        false /* canPrefetchDisplayLists */,
        false /* canCacheDrawingDisplayLists */,
        true /* clipChildren */);
  }
}
