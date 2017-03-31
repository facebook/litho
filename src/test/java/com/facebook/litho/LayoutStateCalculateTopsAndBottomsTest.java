/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import com.facebook.yoga.YogaAlign;

import com.facebook.yoga.YogaFlexDirection;

import android.content.Context;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaPositionType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

@RunWith(ComponentsTestRunner.class)
public class LayoutStateCalculateTopsAndBottomsTest {

  @Test
  public void testCalculateTopsAndBottoms() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(
                        TestDrawableComponent.create(c)
                            .withLayout().flexShrink(0)
                            .wrapInView()
                            .heightPx(50)))
            .child(
                TestDrawableComponent.create(c)
                    .withLayout().flexShrink(0)
                    .heightPx(20))
            .child(
                TestDrawableComponent.create(c)
                    .withLayout().flexShrink(0)
                    .positionType(YogaPositionType.ABSOLUTE)
                    .positionPx(YogaEdge.TOP, 10)
                    .positionPx(YogaEdge.BOTTOM, 30))
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(100, SizeSpec.AT_MOST));

    assertEquals(5, layoutState.getMountableOutputCount());

    assertEquals(0, layoutState.getMountableOutputTops().get(0).getBounds().top);
    assertEquals(0, layoutState.getMountableOutputTops().get(1).getBounds().top);
    assertEquals(0, layoutState.getMountableOutputTops().get(2).getBounds().top);
    assertEquals(10, layoutState.getMountableOutputTops().get(3).getBounds().top);
    assertEquals(50, layoutState.getMountableOutputTops().get(4).getBounds().top);

    assertEquals(40, layoutState.getMountableOutputBottoms().get(0).getBounds().bottom);
    assertEquals(50, layoutState.getMountableOutputBottoms().get(1).getBounds().bottom);
    assertEquals(50, layoutState.getMountableOutputBottoms().get(2).getBounds().bottom);
    assertEquals(70, layoutState.getMountableOutputBottoms().get(3).getBounds().bottom);
    assertEquals(70, layoutState.getMountableOutputBottoms().get(4).getBounds().bottom);

    assertSame(layoutState.getMountableOutputAt(2), layoutState.getMountableOutputTops().get(2));
    assertSame(layoutState.getMountableOutputAt(4), layoutState.getMountableOutputTops().get(3));
    assertSame(layoutState.getMountableOutputAt(3), layoutState.getMountableOutputTops().get(4));

    assertSame(layoutState.getMountableOutputAt(4), layoutState.getMountableOutputBottoms().get(0));
    assertSame(layoutState.getMountableOutputAt(2), layoutState.getMountableOutputBottoms().get(1));
    assertSame(layoutState.getMountableOutputAt(1), layoutState.getMountableOutputBottoms().get(2));
    assertSame(layoutState.getMountableOutputAt(3), layoutState.getMountableOutputBottoms().get(3));
    assertSame(layoutState.getMountableOutputAt(0), layoutState.getMountableOutputBottoms().get(4));
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
      assertEquals(0, LayoutState.sTopsComparator.compare(layoutOutput, layoutOutput));
    }

    // symmetric
    for (int i = 0; i < 4; i++) {
      for (int j = i + 1; j < 4; j++) {
        assertEquals(
            LayoutState.sTopsComparator.compare(layoutOutputs[i], layoutOutputs[j]),
            -1 * LayoutState.sTopsComparator.compare(layoutOutputs[j], layoutOutputs[i]));
      }
    }

    // transitivity
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        for (int k = 0; k < 4; k++) {
          if (i != j && j != k && i != k) {
            if (LayoutState.sTopsComparator.compare(layoutOutputs[i], layoutOutputs[j]) ==
                LayoutState.sTopsComparator.compare(layoutOutputs[j], layoutOutputs[k])) {
              assertEquals(
                  LayoutState.sTopsComparator.compare(layoutOutputs[i], layoutOutputs[j]),
                  LayoutState.sTopsComparator.compare(layoutOutputs[j], layoutOutputs[k]));
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
      assertEquals(0, LayoutState.sBottomsComparator.compare(layoutOutput, layoutOutput));
    }

    // symmetric
    for (int i = 0; i < 4; i++) {
      for (int j = i + 1; j < 4; j++) {
        assertEquals(
            LayoutState.sBottomsComparator.compare(layoutOutputs[i], layoutOutputs[j]),
            -1 * LayoutState.sBottomsComparator.compare(layoutOutputs[j], layoutOutputs[i]));
      }
    }

    // transitivity
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        for (int k = 0; k < 4; k++) {
          if (i != j && j != k && i != k) {
            if (LayoutState.sBottomsComparator.compare(layoutOutputs[i], layoutOutputs[j]) ==
                LayoutState.sBottomsComparator.compare(layoutOutputs[j], layoutOutputs[k])) {
              assertEquals(
                  LayoutState.sBottomsComparator.compare(layoutOutputs[i], layoutOutputs[j]),
                  LayoutState.sBottomsComparator.compare(layoutOutputs[j], layoutOutputs[k]));
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
        null);
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
