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
import static com.facebook.litho.Column.create;
import static com.facebook.litho.LayoutState.sBottomsComparator;
import static com.facebook.litho.LayoutState.sTopsComparator;
import static com.facebook.litho.SizeSpec.AT_MOST;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.yoga.YogaEdge.BOTTOM;
import static com.facebook.yoga.YogaEdge.TOP;
import static com.facebook.yoga.YogaPositionType.ABSOLUTE;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.content.Context;
import android.graphics.Rect;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.SimpleMountSpecTester;
import com.facebook.rendercore.RenderTreeNode;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class LayoutStateCalculateTopsAndBottomsTest {

  @Test
  public void testCalculateTopsAndBottoms() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return create(c)
                .child(create(c).child(SimpleMountSpecTester.create(c).wrapInView().heightPx(50)))
                .child(SimpleMountSpecTester.create(c).heightPx(20))
                .child(
                    SimpleMountSpecTester.create(c)
                        .positionType(ABSOLUTE)
                        .positionPx(TOP, 10)
                        .positionPx(BOTTOM, 30))
                .build();
          }
        };

    LayoutState layoutState =
        calculateLayoutState(
            getApplicationContext(),
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, AT_MOST));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(5);

    assertThat(layoutState.getMountableOutputTops().get(0).getAbsoluteBounds(new Rect()).top)
        .isEqualTo(0);
    assertThat(layoutState.getMountableOutputTops().get(1).getAbsoluteBounds(new Rect()).top)
        .isEqualTo(0);
    assertThat(layoutState.getMountableOutputTops().get(2).getAbsoluteBounds(new Rect()).top)
        .isEqualTo(0);
    assertThat(layoutState.getMountableOutputTops().get(3).getAbsoluteBounds(new Rect()).top)
        .isEqualTo(10);
    assertThat(layoutState.getMountableOutputTops().get(4).getAbsoluteBounds(new Rect()).top)
        .isEqualTo(50);

    assertThat(layoutState.getMountableOutputBottoms().get(0).getAbsoluteBounds(new Rect()).bottom)
        .isEqualTo(40);
    assertThat(layoutState.getMountableOutputBottoms().get(1).getAbsoluteBounds(new Rect()).bottom)
        .isEqualTo(50);
    assertThat(layoutState.getMountableOutputBottoms().get(2).getAbsoluteBounds(new Rect()).bottom)
        .isEqualTo(50);
    assertThat(layoutState.getMountableOutputBottoms().get(3).getAbsoluteBounds(new Rect()).bottom)
        .isEqualTo(70);
    assertThat(layoutState.getMountableOutputBottoms().get(4).getAbsoluteBounds(new Rect()).bottom)
        .isEqualTo(70);

    assertThat(layoutState.getMountableOutputAt(2).getLayoutData())
        .isSameAs(layoutState.getMountableOutputTops().get(2).getLayoutData());
    assertThat(layoutState.getMountableOutputAt(4).getLayoutData())
        .isSameAs(layoutState.getMountableOutputTops().get(3).getLayoutData());
    assertThat(layoutState.getMountableOutputAt(3).getLayoutData())
        .isSameAs(layoutState.getMountableOutputTops().get(4).getLayoutData());

    assertThat(layoutState.getMountableOutputAt(4).getLayoutData())
        .isSameAs(layoutState.getMountableOutputBottoms().get(0).getLayoutData());
    assertThat(layoutState.getMountableOutputAt(2).getLayoutData())
        .isSameAs(layoutState.getMountableOutputBottoms().get(1).getLayoutData());
    assertThat(layoutState.getMountableOutputAt(1).getLayoutData())
        .isSameAs(layoutState.getMountableOutputBottoms().get(2).getLayoutData());
    assertThat(layoutState.getMountableOutputAt(3).getLayoutData())
        .isSameAs(layoutState.getMountableOutputBottoms().get(3).getLayoutData());
    assertThat(layoutState.getMountableOutputAt(0).getLayoutData())
        .isSameAs(layoutState.getMountableOutputBottoms().get(4).getLayoutData());
  }

  @Test
  public void testCalculateTopsAndBottomsWhenEqual() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return create(c)
                .child(SimpleMountSpecTester.create(c).heightPx(50))
                .child(
                    SimpleMountSpecTester.create(c)
                        .wrapInView()
                        .positionType(ABSOLUTE)
                        .positionPx(TOP, 0)
                        .positionPx(BOTTOM, 0))
                .build();
          }
        };

    LayoutState layoutState =
        calculateLayoutState(
            getApplicationContext(),
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, AT_MOST));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(4);

    assertThat(layoutState.getMountableOutputTops().get(0).getAbsoluteBounds(new Rect()).top)
        .isEqualTo(0);
    assertThat(layoutState.getMountableOutputTops().get(1).getAbsoluteBounds(new Rect()).top)
        .isEqualTo(0);
    assertThat(layoutState.getMountableOutputTops().get(2).getAbsoluteBounds(new Rect()).top)
        .isEqualTo(0);
    assertThat(layoutState.getMountableOutputTops().get(3).getAbsoluteBounds(new Rect()).top)
        .isEqualTo(0);

    assertThat(layoutState.getMountableOutputBottoms().get(0).getAbsoluteBounds(new Rect()).bottom)
        .isEqualTo(50);
    assertThat(layoutState.getMountableOutputBottoms().get(1).getAbsoluteBounds(new Rect()).bottom)
        .isEqualTo(50);
    assertThat(layoutState.getMountableOutputBottoms().get(2).getAbsoluteBounds(new Rect()).bottom)
        .isEqualTo(50);
    assertThat(layoutState.getMountableOutputBottoms().get(3).getAbsoluteBounds(new Rect()).bottom)
        .isEqualTo(50);

    assertThat(layoutState.getMountableOutputAt(0).getLayoutData())
        .isSameAs(layoutState.getMountableOutputTops().get(0).getLayoutData());
    assertThat(layoutState.getMountableOutputAt(1).getLayoutData())
        .isSameAs(layoutState.getMountableOutputTops().get(1).getLayoutData());
    assertThat(layoutState.getMountableOutputAt(2).getLayoutData())
        .isSameAs(layoutState.getMountableOutputTops().get(2).getLayoutData());
    assertThat(layoutState.getMountableOutputAt(3).getLayoutData())
        .isSameAs(layoutState.getMountableOutputTops().get(3).getLayoutData());

    assertThat(layoutState.getMountableOutputAt(0).getLayoutData())
        .isSameAs(layoutState.getMountableOutputBottoms().get(3).getLayoutData());
    assertThat(layoutState.getMountableOutputAt(1).getLayoutData())
        .isSameAs(layoutState.getMountableOutputBottoms().get(2).getLayoutData());
    assertThat(layoutState.getMountableOutputAt(2).getLayoutData())
        .isSameAs(layoutState.getMountableOutputBottoms().get(1).getLayoutData());
    assertThat(layoutState.getMountableOutputAt(3).getLayoutData())
        .isSameAs(layoutState.getMountableOutputBottoms().get(0).getLayoutData());
  }

  @Test
  public void testTopsComparatorIsEquivalenceRelation() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return SimpleMountSpecTester.create(c).build();
          }
        };

    RenderTreeNode[] layoutOutputs = new RenderTreeNode[4];
    layoutOutputs[0] = createLayoutOutput(component, 0, 10, 0);
    layoutOutputs[1] = createLayoutOutput(component, 0, 10, 1);
    layoutOutputs[2] = createLayoutOutput(component, 0, 20, 2);
    layoutOutputs[3] = createLayoutOutput(component, 0, 20, 3);

    // reflexive
    for (RenderTreeNode layoutOutput : layoutOutputs) {
      assertThat(sTopsComparator.compare(layoutOutput, layoutOutput)).isEqualTo(0);
    }

    // symmetric
    for (int i = 0; i < 4; i++) {
      for (int j = i + 1; j < 4; j++) {
        assertThat(-1 * sTopsComparator.compare(layoutOutputs[j], layoutOutputs[i]))
            .isEqualTo(sTopsComparator.compare(layoutOutputs[i], layoutOutputs[j]));
      }
    }

    // transitivity
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        for (int k = 0; k < 4; k++) {
          if (i != j && j != k && i != k) {
            if (LayoutState.sTopsComparator.compare(layoutOutputs[i], layoutOutputs[j])
                == LayoutState.sTopsComparator.compare(layoutOutputs[j], layoutOutputs[k])) {
              assertThat(sTopsComparator.compare(layoutOutputs[j], layoutOutputs[k]))
                  .isEqualTo(sTopsComparator.compare(layoutOutputs[i], layoutOutputs[j]));
            }
          }
        }
      }
    }
  }

  @Test
  public void testBottomsComparatorIsEquivalenceRelation() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return SimpleMountSpecTester.create(c).build();
          }
        };
    ;

    RenderTreeNode[] layoutOutputs = new RenderTreeNode[4];
    layoutOutputs[0] = createLayoutOutput(component, 0, 10, 0);
    layoutOutputs[1] = createLayoutOutput(component, 0, 10, 1);
    layoutOutputs[2] = createLayoutOutput(component, 0, 20, 2);
    layoutOutputs[3] = createLayoutOutput(component, 0, 20, 3);

    // reflexive
    for (RenderTreeNode layoutOutput : layoutOutputs) {
      assertThat(sBottomsComparator.compare(layoutOutput, layoutOutput)).isEqualTo(0);
    }

    // symmetric
    for (int i = 0; i < 4; i++) {
      for (int j = i + 1; j < 4; j++) {
        assertThat(-1 * sBottomsComparator.compare(layoutOutputs[j], layoutOutputs[i]))
            .isEqualTo(sBottomsComparator.compare(layoutOutputs[i], layoutOutputs[j]));
      }
    }

    // transitivity
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        for (int k = 0; k < 4; k++) {
          if (i != j && j != k && i != k) {
            if (LayoutState.sBottomsComparator.compare(layoutOutputs[i], layoutOutputs[j])
                == LayoutState.sBottomsComparator.compare(layoutOutputs[j], layoutOutputs[k])) {
              assertThat(sBottomsComparator.compare(layoutOutputs[j], layoutOutputs[k]))
                  .isEqualTo(sBottomsComparator.compare(layoutOutputs[i], layoutOutputs[j]));
            }
          }
        }
      }
    }
  }

  private static LayoutState calculateLayoutState(
      Context context, Component component, int componentTreeId, int widthSpec, int heightSpec) {

    return LayoutState.calculate(
        new ComponentContext(context),
        component,
        componentTreeId,
        widthSpec,
        heightSpec,
        LayoutState.CalculateLayoutSource.TEST);
  }

  private static RenderTreeNode createLayoutOutput(
      Component component, int top, int bottom, int index) {
    LayoutOutput layoutOutput =
        new LayoutOutput(
            null, null, component, new Rect(0, top, 10, bottom), 0, 0, 0, 0, 0, 0, null);
    layoutOutput.setIndex(index);
    return LayoutOutput.create(layoutOutput, null, null, null);
  }
}
