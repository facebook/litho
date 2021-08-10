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
import static com.facebook.litho.SizeSpec.AT_MOST;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.rendercore.incrementalmount.IncrementalMountRenderCoreExtension.sBottomsComparator;
import static com.facebook.rendercore.incrementalmount.IncrementalMountRenderCoreExtension.sTopsComparator;
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
import com.facebook.rendercore.incrementalmount.IncrementalMountOutput;
import com.facebook.rendercore.incrementalmount.IncrementalMountRenderCoreExtension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    assertThat(layoutState.getOutputsOrderedByTopBounds().get(0).getBounds().top).isEqualTo(0);
    assertThat(layoutState.getOutputsOrderedByTopBounds().get(1).getBounds().top).isEqualTo(0);
    assertThat(layoutState.getOutputsOrderedByTopBounds().get(2).getBounds().top).isEqualTo(0);
    assertThat(layoutState.getOutputsOrderedByTopBounds().get(3).getBounds().top).isEqualTo(10);
    assertThat(layoutState.getOutputsOrderedByTopBounds().get(4).getBounds().top).isEqualTo(50);

    assertThat(layoutState.getOutputsOrderedByBottomBounds().get(0).getBounds().bottom)
        .isEqualTo(40);
    assertThat(layoutState.getOutputsOrderedByBottomBounds().get(1).getBounds().bottom)
        .isEqualTo(50);
    assertThat(layoutState.getOutputsOrderedByBottomBounds().get(2).getBounds().bottom)
        .isEqualTo(50);
    assertThat(layoutState.getOutputsOrderedByBottomBounds().get(3).getBounds().bottom)
        .isEqualTo(70);
    assertThat(layoutState.getOutputsOrderedByBottomBounds().get(4).getBounds().bottom)
        .isEqualTo(70);

    assertThat(layoutState.getOutputsOrderedByTopBounds().get(2).getIndex()).isEqualTo(2);
    assertThat(layoutState.getOutputsOrderedByTopBounds().get(3).getIndex()).isEqualTo(4);
    assertThat(layoutState.getOutputsOrderedByTopBounds().get(4).getIndex()).isEqualTo(3);

    assertThat(layoutState.getOutputsOrderedByBottomBounds().get(0).getIndex()).isEqualTo(4);
    assertThat(layoutState.getOutputsOrderedByBottomBounds().get(1).getIndex()).isEqualTo(2);
    assertThat(layoutState.getOutputsOrderedByBottomBounds().get(2).getIndex()).isEqualTo(1);
    assertThat(layoutState.getOutputsOrderedByBottomBounds().get(3).getIndex()).isEqualTo(3);
    assertThat(layoutState.getOutputsOrderedByBottomBounds().get(4).getIndex()).isEqualTo(0);
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

    assertThat(layoutState.getOutputsOrderedByTopBounds().get(0).getBounds().top).isEqualTo(0);
    assertThat(layoutState.getOutputsOrderedByTopBounds().get(1).getBounds().top).isEqualTo(0);
    assertThat(layoutState.getOutputsOrderedByTopBounds().get(2).getBounds().top).isEqualTo(0);
    assertThat(layoutState.getOutputsOrderedByTopBounds().get(3).getBounds().top).isEqualTo(0);

    assertThat(layoutState.getOutputsOrderedByBottomBounds().get(0).getBounds().bottom)
        .isEqualTo(50);
    assertThat(layoutState.getOutputsOrderedByBottomBounds().get(1).getBounds().bottom)
        .isEqualTo(50);
    assertThat(layoutState.getOutputsOrderedByBottomBounds().get(2).getBounds().bottom)
        .isEqualTo(50);
    assertThat(layoutState.getOutputsOrderedByBottomBounds().get(3).getBounds().bottom)
        .isEqualTo(50);

    assertThat(layoutState.getOutputsOrderedByTopBounds().get(0).getIndex()).isEqualTo(0);
    assertThat(layoutState.getOutputsOrderedByTopBounds().get(1).getIndex()).isEqualTo(1);
    assertThat(layoutState.getOutputsOrderedByTopBounds().get(2).getIndex()).isEqualTo(2);
    assertThat(layoutState.getOutputsOrderedByTopBounds().get(3).getIndex()).isEqualTo(3);

    assertThat(layoutState.getOutputsOrderedByBottomBounds().get(3).getIndex()).isEqualTo(0);
    assertThat(layoutState.getOutputsOrderedByBottomBounds().get(2).getIndex()).isEqualTo(1);
    assertThat(layoutState.getOutputsOrderedByBottomBounds().get(1).getIndex()).isEqualTo(2);
    assertThat(layoutState.getOutputsOrderedByBottomBounds().get(0).getIndex()).isEqualTo(3);
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

    IncrementalMountOutput[] outputs = new IncrementalMountOutput[4];
    outputs[0] = createIncrementMountOutput(0, 10, 0);
    outputs[1] = createIncrementMountOutput(0, 10, 1);
    outputs[2] = createIncrementMountOutput(0, 20, 2);
    outputs[3] = createIncrementMountOutput(0, 20, 3);

    // reflexive
    for (IncrementalMountOutput output : outputs) {
      assertThat(sTopsComparator.compare(output, output)).isEqualTo(0);
    }

    // symmetric
    for (int i = 0; i < 4; i++) {
      for (int j = i + 1; j < 4; j++) {
        assertThat(-1 * sTopsComparator.compare(outputs[j], outputs[i]))
            .isEqualTo(sTopsComparator.compare(outputs[i], outputs[j]));
      }
    }

    // transitivity
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        for (int k = 0; k < 4; k++) {
          if (i != j && j != k && i != k) {
            if (IncrementalMountRenderCoreExtension.sTopsComparator.compare(outputs[i], outputs[j])
                == IncrementalMountRenderCoreExtension.sTopsComparator.compare(
                    outputs[j], outputs[k])) {
              assertThat(sTopsComparator.compare(outputs[j], outputs[k]))
                  .isEqualTo(sTopsComparator.compare(outputs[i], outputs[j]));
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

    IncrementalMountOutput[] outputs = new IncrementalMountOutput[4];
    outputs[0] = createIncrementMountOutput(0, 10, 0);
    outputs[1] = createIncrementMountOutput(0, 10, 1);
    outputs[2] = createIncrementMountOutput(0, 20, 2);
    outputs[3] = createIncrementMountOutput(0, 20, 3);

    // reflexive
    for (IncrementalMountOutput output : outputs) {
      assertThat(sBottomsComparator.compare(output, output)).isEqualTo(0);
    }

    // symmetric
    for (int i = 0; i < 4; i++) {
      for (int j = i + 1; j < 4; j++) {
        assertThat(-1 * sBottomsComparator.compare(outputs[j], outputs[i]))
            .isEqualTo(sBottomsComparator.compare(outputs[i], outputs[j]));
      }
    }

    // transitivity
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        for (int k = 0; k < 4; k++) {
          if (i != j && j != k && i != k) {
            if (IncrementalMountRenderCoreExtension.sBottomsComparator.compare(
                    outputs[i], outputs[j])
                == IncrementalMountRenderCoreExtension.sBottomsComparator.compare(
                    outputs[j], outputs[k])) {
              assertThat(sBottomsComparator.compare(outputs[j], outputs[k]))
                  .isEqualTo(sBottomsComparator.compare(outputs[i], outputs[j]));
            }
          }
        }
      }
    }
  }

  @Test
  public void testTopsComparatorWithOverflow() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return SimpleMountSpecTester.create(c).build();
          }
        };

    final IncrementalMountOutput maxIntTop = createIncrementMountOutput(Integer.MAX_VALUE, 20, 0);
    final IncrementalMountOutput largeNegativeTop = createIncrementMountOutput(-2147483646, 20, 1);
    final IncrementalMountOutput minIntTop = createIncrementMountOutput(Integer.MIN_VALUE, 20, 2);
    final IncrementalMountOutput largePositiveTop = createIncrementMountOutput(2147483646, 20, 3);

    assertThat(sTopsComparator.compare(maxIntTop, largeNegativeTop)).isPositive();
    assertThat(sTopsComparator.compare(largeNegativeTop, maxIntTop)).isNegative();

    assertThat(sTopsComparator.compare(minIntTop, largePositiveTop)).isNegative();
    assertThat(sTopsComparator.compare(largePositiveTop, minIntTop)).isPositive();

    assertThat(sTopsComparator.compare(minIntTop, maxIntTop)).isNegative();
    assertThat(sTopsComparator.compare(maxIntTop, minIntTop)).isPositive();
  }

  @Test
  public void testBottomComparatorWithOverflow() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return SimpleMountSpecTester.create(c).build();
          }
        };

    final IncrementalMountOutput maxIntBottom =
        createIncrementMountOutput(20, Integer.MAX_VALUE, 0);
    final IncrementalMountOutput largeNegativeBottom =
        createIncrementMountOutput(20, -2147483646, 1);
    final IncrementalMountOutput minIntBottom =
        createIncrementMountOutput(20, Integer.MIN_VALUE, 2);
    final IncrementalMountOutput largePositiveBottom =
        createIncrementMountOutput(20, 2147483646, 3);

    assertThat(sBottomsComparator.compare(maxIntBottom, largeNegativeBottom)).isPositive();
    assertThat(sBottomsComparator.compare(largeNegativeBottom, maxIntBottom)).isNegative();

    assertThat(sBottomsComparator.compare(minIntBottom, largePositiveBottom)).isNegative();
    assertThat(sBottomsComparator.compare(largePositiveBottom, minIntBottom)).isPositive();

    assertThat(sBottomsComparator.compare(minIntBottom, maxIntBottom)).isNegative();
    assertThat(sBottomsComparator.compare(maxIntBottom, minIntBottom)).isPositive();
  }

  @Test
  public void testTopsComparatorAdheresToContract() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return SimpleMountSpecTester.create(c).build();
          }
        };

    final List<IncrementalMountOutput> nodes = new ArrayList<>();
    int currentIndex = 0;

    nodes.add(createIncrementMountOutput(0, 10, currentIndex++));
    nodes.add(createIncrementMountOutput(0, 10, currentIndex++));
    nodes.add(createIncrementMountOutput(21, 20, currentIndex++));
    nodes.add(createIncrementMountOutput(47, 20, currentIndex++));
    nodes.add(createIncrementMountOutput(21, 20, currentIndex++));
    nodes.add(createIncrementMountOutput(21, 10, currentIndex++));
    nodes.add(createIncrementMountOutput(-2147483628, 10, currentIndex++));
    nodes.add(createIncrementMountOutput(2147483647, 20, currentIndex++));
    nodes.add(createIncrementMountOutput(2147483647, 20, currentIndex++));
    nodes.add(createIncrementMountOutput(-2147483617, 10, currentIndex++));
    nodes.add(createIncrementMountOutput(-2147483617, 10, currentIndex++));
    nodes.add(createIncrementMountOutput(-2147483617, 20, currentIndex++));
    nodes.add(createIncrementMountOutput(-2147483617, 20, currentIndex++));
    nodes.add(createIncrementMountOutput(-2147483568, 10, currentIndex++));
    nodes.add(createIncrementMountOutput(-2147483557, 10, currentIndex++));
    nodes.add(createIncrementMountOutput(-2147483557, 20, currentIndex++));
    nodes.add(createIncrementMountOutput(2147483647, 20, currentIndex++));
    nodes.add(createIncrementMountOutput(2147483647, 10, currentIndex++));
    nodes.add(createIncrementMountOutput(-2147483646, 10, currentIndex++));
    nodes.add(createIncrementMountOutput(2147483647, 20, currentIndex++));
    nodes.add(createIncrementMountOutput(2147483647, 20, currentIndex++));
    nodes.add(createIncrementMountOutput(2147483647, 10, currentIndex++));
    nodes.add(createIncrementMountOutput(2147483647, 10, currentIndex++));
    nodes.add(createIncrementMountOutput(2147483647, 20, currentIndex++));
    nodes.add(createIncrementMountOutput(2147483647, 20, currentIndex++));
    nodes.add(createIncrementMountOutput(-2147483477, 10, currentIndex++));
    nodes.add(createIncrementMountOutput(-2147483278, 10, currentIndex++));
    nodes.add(createIncrementMountOutput(2147483647, 20, currentIndex++));
    nodes.add(createIncrementMountOutput(2147483647, 20, currentIndex++));
    nodes.add(createIncrementMountOutput(-2147483612, 10, currentIndex++));
    nodes.add(createIncrementMountOutput(-2147483611, 10, currentIndex++));
    nodes.add(createIncrementMountOutput(2147483647, 20, currentIndex++));

    Collections.sort(nodes, sTopsComparator);
  }

  @Test
  public void testBottomsComparatorAdheresToContract() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return SimpleMountSpecTester.create(c).build();
          }
        };

    final List<IncrementalMountOutput> nodes = new ArrayList<>();
    int currentIndex = 0;

    nodes.add(createIncrementMountOutput(20, 0, currentIndex++));
    nodes.add(createIncrementMountOutput(20, 0, currentIndex++));
    nodes.add(createIncrementMountOutput(20, 21, currentIndex++));
    nodes.add(createIncrementMountOutput(20, 47, currentIndex++));
    nodes.add(createIncrementMountOutput(20, 21, currentIndex++));
    nodes.add(createIncrementMountOutput(20, 21, currentIndex++));
    nodes.add(createIncrementMountOutput(20, -2147483628, currentIndex++));
    nodes.add(createIncrementMountOutput(20, 2147483647, currentIndex++));
    nodes.add(createIncrementMountOutput(20, 2147483647, currentIndex++));
    nodes.add(createIncrementMountOutput(20, -2147483617, currentIndex++));
    nodes.add(createIncrementMountOutput(20, -2147483617, currentIndex++));
    nodes.add(createIncrementMountOutput(20, -2147483617, currentIndex++));
    nodes.add(createIncrementMountOutput(20, -2147483617, currentIndex++));
    nodes.add(createIncrementMountOutput(20, -2147483568, currentIndex++));
    nodes.add(createIncrementMountOutput(20, -2147483557, currentIndex++));
    nodes.add(createIncrementMountOutput(20, -2147483557, currentIndex++));
    nodes.add(createIncrementMountOutput(20, 2147483647, currentIndex++));
    nodes.add(createIncrementMountOutput(20, 2147483647, currentIndex++));
    nodes.add(createIncrementMountOutput(20, -2147483646, currentIndex++));
    nodes.add(createIncrementMountOutput(20, 2147483647, currentIndex++));
    nodes.add(createIncrementMountOutput(20, 2147483647, currentIndex++));
    nodes.add(createIncrementMountOutput(20, 2147483647, currentIndex++));
    nodes.add(createIncrementMountOutput(20, 2147483647, currentIndex++));
    nodes.add(createIncrementMountOutput(20, 2147483647, currentIndex++));
    nodes.add(createIncrementMountOutput(20, 2147483647, currentIndex++));
    nodes.add(createIncrementMountOutput(20, -2147483477, currentIndex++));
    nodes.add(createIncrementMountOutput(20, -2147483278, currentIndex++));
    nodes.add(createIncrementMountOutput(20, 2147483647, currentIndex++));
    nodes.add(createIncrementMountOutput(20, 2147483647, currentIndex++));
    nodes.add(createIncrementMountOutput(20, -2147483612, currentIndex++));
    nodes.add(createIncrementMountOutput(20, -2147483611, currentIndex++));
    nodes.add(createIncrementMountOutput(20, 2147483647, currentIndex++));

    Collections.sort(nodes, sBottomsComparator);
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
            null,
            null,
            null,
            component,
            null,
            new Rect(0, top, 10, bottom),
            0,
            0,
            0,
            0,
            0,
            0,
            null);
    layoutOutput.setIndex(index);
    return LayoutOutput.create(layoutOutput, null, null, null, null);
  }

  private static IncrementalMountOutput createIncrementMountOutput(
      int top, int bottom, int index, long hostId) {
    return new IncrementalMountOutput((index + 1) * 1L, index, new Rect(0, top, 10, bottom), null);
  }

  private static IncrementalMountOutput createIncrementMountOutput(int top, int bottom, int index) {
    return createIncrementMountOutput(top, bottom, index, 0);
  }
}
