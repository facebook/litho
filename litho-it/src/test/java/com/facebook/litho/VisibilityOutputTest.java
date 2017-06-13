/**
 * Copyright (c) 2017-present, Facebook, Inc.
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

import static com.facebook.litho.LayoutStateOutputIdCalculator.calculateVisibilityOutputId;
import static com.facebook.litho.LayoutStateOutputIdCalculator.getLevelFromId;
import static com.facebook.litho.LayoutStateOutputIdCalculator.getSequenceFromId;
import static java.lang.Long.toBinaryString;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.assertj.core.api.Java6Assertions.assertThat;

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
    assertThat(mVisibilityOutput.getBounds().left).isEqualTo(0);
    assertThat(mVisibilityOutput.getBounds().top).isEqualTo(1);
    assertThat(mVisibilityOutput.getBounds().right).isEqualTo(3);
    assertThat(mVisibilityOutput.getBounds().bottom).isEqualTo(4);
  }

  @Test
  public void testRectBoundsSet() {
    Rect bounds = new Rect(0, 1, 3, 4);
    mVisibilityOutput.setBounds(bounds);
    assertThat(mVisibilityOutput.getBounds().left).isEqualTo(0);
    assertThat(mVisibilityOutput.getBounds().top).isEqualTo(1);
    assertThat(mVisibilityOutput.getBounds().right).isEqualTo(3);
    assertThat(mVisibilityOutput.getBounds().bottom).isEqualTo(4);
  }

  @Test
  public void testHandlersSet() {
    EventHandler visibleHandler = new EventHandler(null, 1);
    EventHandler invisibleHandler = new EventHandler(null, 2);

    mVisibilityOutput.setVisibleEventHandler(visibleHandler);
    mVisibilityOutput.setInvisibleEventHandler(invisibleHandler);
    assertThat(visibleHandler).isSameAs(mVisibilityOutput.getVisibleEventHandler());
    assertThat(invisibleHandler).isSameAs(mVisibilityOutput.getInvisibleEventHandler());

    mVisibilityOutput.release();
    assertThat(mVisibilityOutput.getVisibleEventHandler()).isNull();
    assertThat(mVisibilityOutput.getInvisibleEventHandler()).isNull();
  }

  @Test
  public void testStableIdCalculation() {
    mVisibilityOutput.setComponent(mComponent);

    long stableId = calculateVisibilityOutputId(
        mVisibilityOutput,
        LEVEL_TEST,
        SEQ_TEST);

    long stableIdSeq2 = calculateVisibilityOutputId(
        mVisibilityOutput,
        LEVEL_TEST + 1,
        SEQ_TEST + 1);

    assertThat(toBinaryString(stableId)).isEqualTo("100000001000000000000000001");
    assertThat(toBinaryString(stableIdSeq2)).isEqualTo("100000010000000000000000010");
  }

  @Test
  public void testGetIdLevel() {
    mVisibilityOutput.setComponent(mComponent);
    mVisibilityOutput.setId(
        calculateVisibilityOutputId(
            mVisibilityOutput,
            LEVEL_TEST,
            SEQ_TEST));
    assertThat(LEVEL_TEST).isEqualTo(getLevelFromId(mVisibilityOutput.getId()));

    mVisibilityOutput.setId(
        calculateVisibilityOutputId(
            mVisibilityOutput,
            MAX_LEVEL_TEST,
            SEQ_TEST));

    assertThat(MAX_LEVEL_TEST).isEqualTo(getLevelFromId(mVisibilityOutput.getId()));
  }

  @Test
  public void testGetIdSequence() {
    mVisibilityOutput.setComponent(mComponent);
    mVisibilityOutput.setId(
        calculateVisibilityOutputId(
            mVisibilityOutput,
            LEVEL_TEST,
            SEQ_TEST));
    assertThat(SEQ_TEST).isEqualTo(getSequenceFromId(mVisibilityOutput.getId()));

    mVisibilityOutput.setId(
        calculateVisibilityOutputId(
            mVisibilityOutput,
            LEVEL_TEST,
            MAX_SEQ_TEST));

    assertThat(MAX_SEQ_TEST).isEqualTo(getSequenceFromId(mVisibilityOutput.getId()));
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
