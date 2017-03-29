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

@RunWith(ComponentsTestRunner.class)
public class LayoutOutputTest {

  private static final int LIFECYCLE_TEST_ID = 1;
  private static final int LEVEL_TEST = 1;
  private static final int SEQ_TEST = 1;
  private static final int MAX_LEVEL_TEST = 255;
  private static final int MAX_SEQ_TEST = 65535;

  private static class TestComponent<L extends ComponentLifecycle> extends Component<L> {
    public TestComponent(L component) {
      super(component);
    }

    @Override
    public String getSimpleName() {
      return "TestComponent";
    }
  }

  private final ComponentLifecycle mLifecycle = new ComponentLifecycle() {
    @Override
    int getId() {
      return LIFECYCLE_TEST_ID;
    }
  };
  private LayoutOutput mLayoutOutput;

  @Before
  public void setup() {
    mLayoutOutput = new LayoutOutput();
  }

  @Test
  public void testPositionAndSizeSet() {
    mLayoutOutput.setBounds(0, 1, 3, 4);
    assertEquals(0, mLayoutOutput.getBounds().left);
    assertEquals(1, mLayoutOutput.getBounds().top);
    assertEquals(3, mLayoutOutput.getBounds().right);
    assertEquals(4, mLayoutOutput.getBounds().bottom);
  }

  @Test
  public void testHostMarkerSet() {
    mLayoutOutput.setHostMarker(10l);
    assertEquals(10, mLayoutOutput.getHostMarker());
  }

  @Test
  public void testFlagsSet() {
    mLayoutOutput.setFlags(1);
    assertEquals(1, mLayoutOutput.getFlags());
  }

  @Test
  public void testStableIdCalculation() {
    ComponentLifecycle lifecycle = new ComponentLifecycle() {
      @Override
      int getId() {
        return LIFECYCLE_TEST_ID;
      }
    };
    Component component = new TestComponent(lifecycle) {};

    mLayoutOutput.setComponent(component);

    long stableId = LayoutStateOutputIdCalculator.calculateLayoutOutputId(
        mLayoutOutput,
        LEVEL_TEST,
        LayoutOutput.TYPE_CONTENT,
        SEQ_TEST);

    long stableIdSeq2 = LayoutStateOutputIdCalculator.calculateLayoutOutputId(
        mLayoutOutput,
        LEVEL_TEST + 1,
        LayoutOutput.TYPE_CONTENT,
        SEQ_TEST + 1);

    assertEquals("100000001000000000000000001", Long.toBinaryString(stableId));
    assertEquals("100000010000000000000000010", Long.toBinaryString(stableIdSeq2));
  }

  @Test
  public void testStableIdBackgroundType() {
    ComponentLifecycle lifecycle = new ComponentLifecycle() {
      @Override
      int getId() {
        return LIFECYCLE_TEST_ID;
      }
    };
    Component component = new TestComponent(lifecycle) {};

    mLayoutOutput.setComponent(component);
    mLayoutOutput.setId(
        LayoutStateOutputIdCalculator.calculateLayoutOutputId(
            mLayoutOutput,
            LEVEL_TEST,
            LayoutOutput.TYPE_BACKGROUND,
            SEQ_TEST));

    long stableId = mLayoutOutput.getId();
    assertEquals("100000001010000000000000001", Long.toBinaryString(stableId));
  }

  @Test
  public void testStableIdForegroundType() {
    ComponentLifecycle lifecycle = new ComponentLifecycle() {
      @Override
      int getId() {
        return LIFECYCLE_TEST_ID;
      }
    };
    Component component = new TestComponent(lifecycle) {};

    mLayoutOutput.setComponent(component);
    mLayoutOutput.setId(
        LayoutStateOutputIdCalculator.calculateLayoutOutputId(
            mLayoutOutput,
            LEVEL_TEST,
            LayoutOutput.TYPE_FOREGROUND,
            SEQ_TEST));

    long stableId = mLayoutOutput.getId();
    assertEquals("100000001100000000000000001", Long.toBinaryString(stableId));
  }

  @Test
  public void testStableIdHostType() {
    ComponentLifecycle lifecycle = new ComponentLifecycle() {
      @Override
      int getId() {
        return LIFECYCLE_TEST_ID;
      }
    };
    Component component = new TestComponent(lifecycle) {};

    mLayoutOutput.setComponent(component);
    mLayoutOutput.setId(
        LayoutStateOutputIdCalculator.calculateLayoutOutputId(
            mLayoutOutput,
            LEVEL_TEST,
            LayoutOutput.TYPE_HOST,
            SEQ_TEST));

    long stableId = mLayoutOutput.getId();
    assertEquals("100000001110000000000000001", Long.toBinaryString(stableId));
  }

  @Test
  public void testGetIdLevel() {
    ComponentLifecycle lifecycle = new ComponentLifecycle() {
      @Override
      int getId() {
        return LIFECYCLE_TEST_ID;
      }
    };
    Component component = new TestComponent(lifecycle) {};

    mLayoutOutput.setComponent(component);
    mLayoutOutput.setId(
        LayoutStateOutputIdCalculator.calculateLayoutOutputId(
            mLayoutOutput,
            LEVEL_TEST,
            LayoutOutput.TYPE_HOST,
            SEQ_TEST));
    assertEquals(LayoutStateOutputIdCalculator.getLevelFromId(mLayoutOutput.getId()), LEVEL_TEST);

    mLayoutOutput.setId(
        LayoutStateOutputIdCalculator.calculateLayoutOutputId(
            mLayoutOutput,
            MAX_LEVEL_TEST,
            LayoutOutput.TYPE_CONTENT,
            SEQ_TEST));

    assertEquals(LayoutStateOutputIdCalculator.getLevelFromId(mLayoutOutput.getId()), MAX_LEVEL_TEST);
  }

  @Test
  public void testGetIdSequence() {
    ComponentLifecycle lifecycle = new ComponentLifecycle() {
      @Override
      int getId() {
        return LIFECYCLE_TEST_ID;
      }
    };
    Component component = new TestComponent(lifecycle) {};

    mLayoutOutput.setComponent(component);
    mLayoutOutput.setId(
        LayoutStateOutputIdCalculator.calculateLayoutOutputId(
            mLayoutOutput,
            LEVEL_TEST,
            LayoutOutput.TYPE_HOST,
            SEQ_TEST));
    assertEquals(LayoutStateOutputIdCalculator.getSequenceFromId(mLayoutOutput.getId()), SEQ_TEST);

    mLayoutOutput.setId(
        LayoutStateOutputIdCalculator.calculateLayoutOutputId(
            mLayoutOutput,
            LEVEL_TEST,
            LayoutOutput.TYPE_CONTENT,
            MAX_SEQ_TEST));

    assertEquals(LayoutStateOutputIdCalculator.getSequenceFromId(mLayoutOutput.getId()), MAX_SEQ_TEST);
  }

  @Test(expected = IllegalArgumentException.class)
  public void levelOutOfRangeTest() {
    ComponentLifecycle lifecycle = new ComponentLifecycle() {
      @Override
      int getId() {
        return LIFECYCLE_TEST_ID;
      }
    };
    Component component = new TestComponent(lifecycle) {};

    mLayoutOutput.setComponent(component);
    mLayoutOutput.setId(
        LayoutStateOutputIdCalculator.calculateLayoutOutputId(
            mLayoutOutput,
            MAX_LEVEL_TEST + 1,
            LayoutOutput.TYPE_HOST,
            SEQ_TEST));
  }

  @Test(expected = IllegalArgumentException.class)
  public void sequenceOutOfRangeTest() {
    ComponentLifecycle lifecycle = new ComponentLifecycle() {
      @Override
      int getId() {
        return LIFECYCLE_TEST_ID;
      }
    };
    Component component = new TestComponent(lifecycle) {};

    mLayoutOutput.setComponent(component);
    mLayoutOutput.setId(
