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

import static com.facebook.litho.Column.create;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.yoga.YogaEdge.ALL;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.TestLayoutComponent;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.SimpleMountSpecTester;
import com.facebook.litho.widget.TestNullLayoutComponent;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class LayoutStateCalculateVisibilityOutputsTest {

  @Rule public final LithoViewRule mLithoViewRule = new LithoViewRule();

  private ComponentContext mContext;

  @Before
  public void setup() throws Exception {
    mContext = mLithoViewRule.getComponentTree().getContext();
    ComponentsPools.clearMountContentPools();
  }

  @Test
  public void testNoUnnecessaryVisibilityOutputs() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return create(c)
                .child(
                    create(c)
                        .child(
                            SimpleMountSpecTester.create(c).visibleHandler(c.newEventHandler(1))))
                .child(SimpleMountSpecTester.create(c).invisibleHandler(c.newEventHandler(2)))
                .child(SimpleMountSpecTester.create(c))
                .build();
          }
        };

    LayoutState layoutState =
        calculateLayoutState(
            mContext, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getVisibilityOutputCount()).isEqualTo(2);
  }

  @Test
  public void testNoUnnecessaryVisibilityOutputsWithFullImpression() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return create(c)
                .child(
                    create(c)
                        .child(
                            SimpleMountSpecTester.create(c).visibleHandler(c.newEventHandler(1))))
                .child(SimpleMountSpecTester.create(c).fullImpressionHandler(c.newEventHandler(3)))
                .child(SimpleMountSpecTester.create(c))
                .build();
          }
        };

    LayoutState layoutState =
        calculateLayoutState(
            mContext, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getVisibilityOutputCount()).isEqualTo(2);
  }

  @Test
  public void testNoUnnecessaryVisibilityOutputsWithFocused() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return create(c)
                .child(
                    create(c)
                        .child(
                            SimpleMountSpecTester.create(c).visibleHandler(c.newEventHandler(1))))
                .child(SimpleMountSpecTester.create(c).focusedHandler(c.newEventHandler(4)))
                .child(SimpleMountSpecTester.create(c))
                .build();
          }
        };

    LayoutState layoutState =
        calculateLayoutState(
            mContext, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getVisibilityOutputCount()).isEqualTo(2);
  }

  @Test
  public void testVisibilityOutputsForDelegateComponents() {
    final boolean isDelegate = true;
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return create(c)
                .child(
                    TestLayoutComponent.create(c, 0, 0, true, false, isDelegate)
                        .visibleHandler(c.newEventHandler(1)))
                .wrapInView()
                .build();
          }
        };

    LayoutState layoutState =
        calculateLayoutState(
            mContext, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getVisibilityOutputCount()).isEqualTo(1);
  }

  @Test
  public void testLayoutOutputsForDeepLayoutSpecs() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return create(c)
                .child(
                    create(c)
                        .child(TestLayoutComponent.create(c).visibleHandler(c.newEventHandler(1)))
                        .invisibleHandler(c.newEventHandler(2)))
                .child(
                    create(c)
                        .child(TestLayoutComponent.create(c).invisibleHandler(c.newEventHandler(1)))
                        .visibleHandler(c.newEventHandler(2)))
                .wrapInView()
                .build();
          }
        };

    LayoutState layoutState =
        calculateLayoutState(
            mContext, component, -1, makeSizeSpec(350, EXACTLY), makeSizeSpec(200, EXACTLY));

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
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return create(c)
                .child(
                    SimpleMountSpecTester.create(c)
                        .visibleHandler(c.newEventHandler(1))
                        .wrapInView())
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            mContext, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getVisibilityOutputCount()).isEqualTo(1);
  }

  @Test
  public void testLayoutOutputForRootWithNullLayout() {
    final Component componentWithNullLayout =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return null;
          }
        };

    LayoutState layoutState =
        calculateLayoutState(
            mContext,
            componentWithNullLayout,
            -1,
            makeSizeSpec(350, EXACTLY),
            makeSizeSpec(200, EXACTLY));

    assertThat(layoutState.getVisibilityOutputCount()).isEqualTo(0);
  }

  @Test
  public void testLayoutComponentForNestedTreeChildWithNullLayout() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return create(c)
                .paddingPx(ALL, 2)
                .child(TestNullLayoutComponent.create(c))
                .invisibleHandler(c.newEventHandler(2))
                .build();
          }
        };

    LayoutState layoutState =
        calculateLayoutState(
            mContext, component, -1, makeSizeSpec(350, EXACTLY), makeSizeSpec(200, EXACTLY));

    assertThat(layoutState.getVisibilityOutputCount()).isEqualTo(1);
  }

  private static LayoutState calculateLayoutState(
      ComponentContext context,
      Component component,
      int componentTreeId,
      int widthSpec,
      int heightSpec) {

    return LayoutState.calculate(
        context,
        component,
        null,
        componentTreeId,
        widthSpec,
        heightSpec,
        -1,
        false /* shouldGenerateDiffTree */,
        null /* previousDiffTreeRoot */,
        LayoutState.CalculateLayoutSource.TEST,
        null);
  }
}
