/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.graphics.Rect;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertSame;

@RunWith(ComponentsTestRunner.class)
public class VisibilityOutputTest {

  private static final int LIFECYCLE_TEST_ID = 1;
  private static final int LEVEL_TEST = 1;
  private static final int SEQ_TEST = 1;
  private static final int MAX_LEVEL_TEST = 255;
  private static final int MAX_SEQ_TEST = 65535;

  private final ComponentLifecycle mLifecycle = new ComponentLifecycle() {
    @Override
    int getId() {
      return LIFECYCLE_TEST_ID;
    }
  };
  private Component<?> mComponent;
  private VisibilityOutput mVisibilityOutput;

  @Before
  public void setup() {
    mVisibilityOutput = new VisibilityOutput();

    mComponent = new Component(mLifecycle) {
      @Override
      public String getSimpleName() {
        return "TestComponent";
      }
    };
  }

  @Test
  public void testPositionAndSizeSet() {
    mVisibilityOutput.setBounds(0, 1, 3, 4);
    assertEquals(0, mVisibilityOutput.getBounds().left);
    assertEquals(1, mVisibilityOutput.getBounds().top);
    assertEquals(3, mVisibilityOutput.getBounds().right);
    assertEquals(4, mVisibilityOutput.getBounds().bottom);
  }

  @Test
  public void testRectBoundsSet() {
    Rect bounds = new Rect(0, 1, 3, 4);
    mVisibilityOutput.setBounds(bounds);
    assertEquals(0, mVisibilityOutput.getBounds().left);
    assertEquals(1, mVisibilityOutput.getBounds().top);
    assertEquals(3, mVisibilityOutput.getBounds().right);
    assertEquals(4, mVisibilityOutput.getBounds().bottom);
  }

  @Test
  public void testHandlersSet() {
    EventHandler visibleHandler = new EventHandler(null, 1);
    EventHandler invisibleHandler = new EventHandler(null, 2);

    mVisibilityOutput.setVisibleEventHandler(visibleHandler);
    mVisibilityOutput.setInvisibleEventHandler(invisibleHandler);
    assertSame(visibleHandler, mVisibilityOutput.getVisibleEventHandler());
    assertSame(invisibleHandler, mVisibilityOutput.getInvisibleEventHandler());

    mVisibilityOutput.release();
    assertNull(mVisibilityOutput.getVisibleEventHandler());
    assertNull(mVisibilityOutput.getInvisibleEventHandler());
  }

  @Test
  public void testStableIdCalculation() {
    mVisibilityOutput.setComponent(mComponent);

    long stableId = LayoutStateOutputIdCalculator.calculateVisibilityOutputId(
        mVisibilityOutput,
        LEVEL_TEST,
        SEQ_TEST);

    long stableIdSeq2 = LayoutStateOutputIdCalculator.calculateVisibilityOutputId(
        mVisibilityOutput,
        LEVEL_TEST + 1,
        SEQ_TEST + 1);

    assertEquals("100000001000000000000000001", Long.toBinaryString(stableId));
    assertEquals("100000010000000000000000010", Long.toBinaryString(stableIdSeq2));
  }

  @Test
  public void testGetIdLevel() {
    mVisibilityOutput.setComponent(mComponent);
    mVisibilityOutput.setId(
        LayoutStateOutputIdCalculator.calculateVisibilityOutputId(
            mVisibilityOutput,
            LEVEL_TEST,
            SEQ_TEST));
    assertEquals(
        LayoutStateOutputIdCalculator.getLevelFromId(mVisibilityOutput.getId()),
        LEVEL_TEST);

    mVisibilityOutput.setId(
        LayoutStateOutputIdCalculator.calculateVisibilityOutputId(
            mVisibilityOutput,
            MAX_LEVEL_TEST,
            SEQ_TEST));

    assertEquals(
        LayoutStateOutputIdCalculator.getLevelFromId(mVisibilityOutput.getId()),
        MAX_LEVEL_TEST);
  }

  @Test
  public void testGetIdSequence() {
    mVisibilityOutput.setComponent(mComponent);
    mVisibilityOutput.setId(
        LayoutStateOutputIdCalculator.calculateVisibilityOutputId(
            mVisibilityOutput,
            LEVEL_TEST,
            SEQ_TEST));
    assertEquals(LayoutStateOutputIdCalculator.getSequenceFromId(mVisibilityOutput.getId()), SEQ_TEST);

    mVisibilityOutput.setId(
        LayoutStateOutputIdCalculator.calculateVisibilityOutputId(
            mVisibilityOutput,
            LEVEL_TEST,
            MAX_SEQ_TEST));

    assertEquals(
        LayoutStateOutputIdCalculator.getSequenceFromId(mVisibilityOutput.getId()),
        MAX_SEQ_TEST);
  }

  @Test(expected = IllegalArgumentException.class)
  public void levelOutOfRangeTest() {
    mVisibilityOutput.setComponent(mComponent);
    mVisibilityOutput.setId(
        LayoutStateOutputIdCalculator.calculateVisibilityOutputId(
            mVisibilityOutput,
            MAX_LEVEL_TEST + 1,
            SEQ_TEST));
  }

  @Test(expected = IllegalArgumentException.class)
  public void sequenceOutOfRangeTest() {
    mVisibilityOutput.setComponent(mComponent);
    mVisibilityOutput.setId(
        LayoutStateOutputIdCalculator.calculateVisibilityOutputId(
            mVisibilityOutput,
            LEVEL_TEST,
            MAX_SEQ_TEST + 1));
  }
}
