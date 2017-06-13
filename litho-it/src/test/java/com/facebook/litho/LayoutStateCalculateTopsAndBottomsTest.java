/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.content.Context;

import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;

import org.junit.Test;
import org.junit.runner.RunWith;

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
import static org.robolectric.RuntimeEnvironment.application;

@RunWith(ComponentsTestRunner.class)
public class LayoutStateCalculateTopsAndBottomsTest {

  @Test
  public void testCalculateTopsAndBottoms() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return create(c)
            .child(
                create(c)
                    .child(
                        TestDrawableComponent.create(c)
                            .withLayout()
                            .wrapInView()
                            .heightPx(50)))
            .child(
                TestDrawableComponent.create(c)
                    .withLayout()
                    .heightPx(20))
            .child(
                TestDrawableComponent.create(c)
                    .withLayout()
                    .positionType(ABSOLUTE)
                    .positionPx(TOP, 10)
                    .positionPx(BOTTOM, 30))
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        application,
        component,
        -1,
        makeSizeSpec(100, EXACTLY),
        makeSizeSpec(100, AT_MOST));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(5);

    assertThat(layoutState.getMountableOutputTops().get(0).getBounds().top).isEqualTo(0);
    assertThat(layoutState.getMountableOutputTops().get(1).getBounds().top).isEqualTo(0);
    assertThat(layoutState.getMountableOutputTops().get(2).getBounds().top).isEqualTo(0);
    assertThat(layoutState.getMountableOutputTops().get(3).getBounds().top).isEqualTo(10);
    assertThat(layoutState.getMountableOutputTops().get(4).getBounds().top).isEqualTo(50);

    assertThat(layoutState.getMountableOutputBottoms().get(0).getBounds().bottom).isEqualTo(40);
    assertThat(layoutState.getMountableOutputBottoms().get(1).getBounds().bottom).isEqualTo(50);
    assertThat(layoutState.getMountableOutputBottoms().get(2).getBounds().bottom).isEqualTo(50);
    assertThat(layoutState.getMountableOutputBottoms().get(3).getBounds().bottom).isEqualTo(70);
    assertThat(layoutState.getMountableOutputBottoms().get(4).getBounds().bottom).isEqualTo(70);

    assertThat(layoutState.getMountableOutputAt(2)).isSameAs(layoutState.getMountableOutputTops().get(2));
    assertThat(layoutState.getMountableOutputAt(4)).isSameAs(layoutState.getMountableOutputTops().get(3));
    assertThat(layoutState.getMountableOutputAt(3)).isSameAs(layoutState.getMountableOutputTops().get(4));

    assertThat(layoutState.getMountableOutputAt(4)).isSameAs(layoutState.getMountableOutputBottoms().get(0));
    assertThat(layoutState.getMountableOutputAt(2)).isSameAs(layoutState.getMountableOutputBottoms().get(1));
    assertThat(layoutState.getMountableOutputAt(1)).isSameAs(layoutState.getMountableOutputBottoms().get(2));
    assertThat(layoutState.getMountableOutputAt(3)).isSameAs(layoutState.getMountableOutputBottoms().get(3));
    assertThat(layoutState.getMountableOutputAt(0)).isSameAs(layoutState.getMountableOutputBottoms().get(4));
  }

  @Test
  public void testTopsComparatorIsEquivalenceRelation() {
    LayoutOutput[] layoutOutputs = new LayoutOutput[4];
    layoutOutputs[0] = createLayoutOutput(0, 20, false);
    layoutOutputs[1] = createLayoutOutput(0, 20, true);
    layoutOutputs[2] = createLayoutOutput(10, 20, false);
    layoutOutputs[3] = createLayoutOutput(10, 20, true);

    // reflexive
    for (LayoutOutput layoutOutput : layoutOutputs) {
      assertThat(sTopsComparator.compare(layoutOutput, layoutOutput)).isEqualTo(0);
    }

    // symmetric
    for (int i = 0; i < 4; i++) {
      for (int j = i + 1; j < 4; j++) {
        assertThat(-1 * sTopsComparator.compare(layoutOutputs[j], layoutOutputs[i])).isEqualTo(sTopsComparator.compare(layoutOutputs[i], layoutOutputs[j]));
      }
    }

    // transitivity
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        for (int k = 0; k < 4; k++) {
          if (i != j && j != k && i != k) {
            if (LayoutState.sTopsComparator.compare(layoutOutputs[i], layoutOutputs[j]) ==
                LayoutState.sTopsComparator.compare(layoutOutputs[j], layoutOutputs[k])) {
              assertThat(sTopsComparator.compare(layoutOutputs[j], layoutOutputs[k])).isEqualTo(sTopsComparator.compare(layoutOutputs[i], layoutOutputs[j]));
            }
          }
        }
      }
    }
  }

  @Test
  public void testBottomsComparatorIsEquivalenceRelation() {
    LayoutOutput[] layoutOutputs = new LayoutOutput[4];
    layoutOutputs[0] = createLayoutOutput(0, 10, false);
    layoutOutputs[1] = createLayoutOutput(0, 10, true);
    layoutOutputs[2] = createLayoutOutput(0, 20, false);
    layoutOutputs[3] = createLayoutOutput(0, 20, true);

    // reflexive
    for (LayoutOutput layoutOutput : layoutOutputs) {
      assertThat(sBottomsComparator.compare(layoutOutput, layoutOutput)).isEqualTo(0);
    }

    // symmetric
    for (int i = 0; i < 4; i++) {
      for (int j = i + 1; j < 4; j++) {
        assertThat(-1 * sBottomsComparator.compare(layoutOutputs[j], layoutOutputs[i])).isEqualTo(sBottomsComparator.compare(layoutOutputs[i], layoutOutputs[j]));
      }
    }

    // transitivity
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        for (int k = 0; k < 4; k++) {
          if (i != j && j != k && i != k) {
            if (LayoutState.sBottomsComparator.compare(layoutOutputs[i], layoutOutputs[j]) ==
                LayoutState.sBottomsComparator.compare(layoutOutputs[j], layoutOutputs[k])) {
              assertThat(sBottomsComparator.compare(layoutOutputs[j], layoutOutputs[k])).isEqualTo(sBottomsComparator.compare(layoutOutputs[i], layoutOutputs[j]));
            }
          }
        }
      }
    }
  }

  private static LayoutState calculateLayoutState(
      Context context,
      Component<?> component,
      int componentTreeId,
      int widthSpec,
      int heightSpec) {

    return LayoutState.calculate(
        new ComponentContext(context),
        component,
        componentTreeId,
        widthSpec,
        heightSpec,
        false,
        false,
        null,
        false);
  }

  private static LayoutOutput createLayoutOutput(int top, int bottom, boolean isHostSpec) {
    LayoutOutput layoutOutput = new LayoutOutput();
    layoutOutput.setBounds(0, top, 10, bottom);
    if (isHostSpec) {
      layoutOutput.setComponent(HostComponent.create());
    }

    return layoutOutput;
  }
}
