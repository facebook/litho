/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static com.facebook.litho.LayoutOutput.TYPE_BACKGROUND;
import static com.facebook.litho.LayoutOutput.TYPE_CONTENT;
import static com.facebook.litho.LayoutOutput.TYPE_FOREGROUND;
import static com.facebook.litho.LayoutOutput.TYPE_HOST;
import static com.facebook.litho.LayoutStateOutputIdCalculator.calculateLayoutOutputId;
import static com.facebook.litho.LayoutStateOutputIdCalculator.getLevelFromId;
import static com.facebook.litho.LayoutStateOutputIdCalculator.getSequenceFromId;
import static java.lang.Long.toBinaryString;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.graphics.Rect;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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
    int getTypeId() {
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
    assertThat(mLayoutOutput.getBounds().left).isEqualTo(0);
    assertThat(mLayoutOutput.getBounds().top).isEqualTo(1);
    assertThat(mLayoutOutput.getBounds().right).isEqualTo(3);
    assertThat(mLayoutOutput.getBounds().bottom).isEqualTo(4);
  }

  @Test
  public void testHostMarkerSet() {
    mLayoutOutput.setHostMarker(10l);
    assertThat(mLayoutOutput.getHostMarker()).isEqualTo(10);
  }

  @Test
  public void testFlagsSet() {
    mLayoutOutput.setFlags(1);
    assertThat(mLayoutOutput.getFlags()).isEqualTo(1);
  }

  @Test
  public void testStableIdCalculation() {
    ComponentLifecycle lifecycle = new ComponentLifecycle() {
      @Override
      int getTypeId() {
        return LIFECYCLE_TEST_ID;
      }
    };
    Component component = new TestComponent(lifecycle) {
    };

    mLayoutOutput.setComponent(component);

    long stableId = calculateLayoutOutputId(
        mLayoutOutput,
        LEVEL_TEST,
        TYPE_CONTENT,
        SEQ_TEST);

    long stableIdSeq2 = calculateLayoutOutputId(
        mLayoutOutput,
        LEVEL_TEST + 1,
        TYPE_CONTENT,
        SEQ_TEST + 1);

    assertThat(toBinaryString(stableId)).isEqualTo("100000001000000000000000001");
    assertThat(toBinaryString(stableIdSeq2)).isEqualTo("100000010000000000000000010");
  }

  @Test
  public void testStableIdBackgroundType() {
    ComponentLifecycle lifecycle = new ComponentLifecycle() {
      @Override
      int getTypeId() {
        return LIFECYCLE_TEST_ID;
      }
    };
    Component component = new TestComponent(lifecycle) {
    };

    mLayoutOutput.setComponent(component);
    mLayoutOutput.setId(
        calculateLayoutOutputId(
            mLayoutOutput,
            LEVEL_TEST,
            TYPE_BACKGROUND,
            SEQ_TEST));

    long stableId = mLayoutOutput.getId();
    assertThat(toBinaryString(stableId)).isEqualTo("100000001010000000000000001");
  }

  @Test
  public void testStableIdForegroundType() {
    ComponentLifecycle lifecycle = new ComponentLifecycle() {
      @Override
      int getTypeId() {
        return LIFECYCLE_TEST_ID;
      }
    };
    Component component = new TestComponent(lifecycle) {
    };

    mLayoutOutput.setComponent(component);
    mLayoutOutput.setId(
        calculateLayoutOutputId(
            mLayoutOutput,
            LEVEL_TEST,
            TYPE_FOREGROUND,
            SEQ_TEST));

    long stableId = mLayoutOutput.getId();
    assertThat(toBinaryString(stableId)).isEqualTo("100000001100000000000000001");
  }

  @Test
  public void testStableIdHostType() {
    ComponentLifecycle lifecycle = new ComponentLifecycle() {
      @Override
      int getTypeId() {
        return LIFECYCLE_TEST_ID;
      }
    };
    Component component = new TestComponent(lifecycle) {
    };

    mLayoutOutput.setComponent(component);
    mLayoutOutput.setId(
        calculateLayoutOutputId(
            mLayoutOutput,
            LEVEL_TEST,
            TYPE_HOST,
            SEQ_TEST));

    long stableId = mLayoutOutput.getId();
    assertThat(toBinaryString(stableId)).isEqualTo("100000001110000000000000001");
  }

  @Test
  public void testGetIdLevel() {
    ComponentLifecycle lifecycle = new ComponentLifecycle() {
      @Override
      int getTypeId() {
        return LIFECYCLE_TEST_ID;
      }
    };
    Component component = new TestComponent(lifecycle) {
    };

    mLayoutOutput.setComponent(component);
    mLayoutOutput.setId(
        calculateLayoutOutputId(
            mLayoutOutput,
            LEVEL_TEST,
            TYPE_HOST,
            SEQ_TEST));
    assertThat(LEVEL_TEST).isEqualTo(getLevelFromId(mLayoutOutput.getId()));

    mLayoutOutput.setId(
        calculateLayoutOutputId(
            mLayoutOutput,
            MAX_LEVEL_TEST,
            TYPE_CONTENT,
            SEQ_TEST));

    assertThat(MAX_LEVEL_TEST).isEqualTo(getLevelFromId(mLayoutOutput.getId()));
  }

  @Test
  public void testGetIdSequence() {
    ComponentLifecycle lifecycle = new ComponentLifecycle() {
      @Override
      int getTypeId() {
        return LIFECYCLE_TEST_ID;
      }
    };
    Component component = new TestComponent(lifecycle) {
    };

    mLayoutOutput.setComponent(component);
    mLayoutOutput.setId(
        calculateLayoutOutputId(
            mLayoutOutput,
            LEVEL_TEST,
            TYPE_HOST,
            SEQ_TEST));
    assertThat(SEQ_TEST).isEqualTo(getSequenceFromId(mLayoutOutput.getId()));

    mLayoutOutput.setId(
        calculateLayoutOutputId(
            mLayoutOutput,
            LEVEL_TEST,
            TYPE_CONTENT,
            MAX_SEQ_TEST));

    assertThat(MAX_SEQ_TEST).isEqualTo(getSequenceFromId(mLayoutOutput.getId()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void levelOutOfRangeTest() {
    ComponentLifecycle lifecycle = new ComponentLifecycle() {
      @Override
      int getTypeId() {
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
      int getTypeId() {
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
            MAX_SEQ_TEST + 1));
  }

  @Test
  public void testGetMountBoundsNoHostTranslation() {
    mLayoutOutput.setBounds(10, 10, 10, 10);

    Rect mountBounds = new Rect();
    mLayoutOutput.getMountBounds(mountBounds);

    assertThat(mountBounds).isEqualTo(mLayoutOutput.getBounds());
  }

  @Test
  public void testGetMountBoundsWithHostTranslation() {
    mLayoutOutput.setBounds(10, 10, 10, 10);
    mLayoutOutput.setHostTranslationX(5);
    mLayoutOutput.setHostTranslationY(2);

    Rect mountBounds = new Rect();
    mLayoutOutput.getMountBounds(mountBounds);

    assertThat(mountBounds).isEqualTo(new Rect(5, 8, 5, 8));
  }
}
