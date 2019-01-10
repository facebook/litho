/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

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

  private static class TestComponent extends Component {

    protected TestComponent() {
      super("TestComponent");
    }

    @Override
    public boolean isEquivalentTo(Component other) {
      return this == other;
    }

    @Override
    int getTypeId() {
      return LIFECYCLE_TEST_ID;
    }
  }

  private LayoutOutput mLayoutOutput;
  private TestComponent mTestComponent;

  @Before
  public void setup() {
    mLayoutOutput = new LayoutOutput();
    mTestComponent = new TestComponent();
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
    mLayoutOutput.setComponent(mTestComponent);

    long stableId =
        calculateLayoutOutputId(mLayoutOutput, LEVEL_TEST, OutputUnitType.CONTENT, SEQ_TEST);

    long stableIdSeq2 =
        calculateLayoutOutputId(
            mLayoutOutput, LEVEL_TEST + 1, OutputUnitType.CONTENT, SEQ_TEST + 1);

    assertThat(toBinaryString(stableId)).isEqualTo("100000001000000000000000001");
    assertThat(toBinaryString(stableIdSeq2)).isEqualTo("100000010000000000000000010");
  }

  @Test
  public void testStableIdBackgroundType() {
    mLayoutOutput.setComponent(mTestComponent);
    mLayoutOutput.setId(
        calculateLayoutOutputId(mLayoutOutput, LEVEL_TEST, OutputUnitType.BACKGROUND, SEQ_TEST));

    long stableId = mLayoutOutput.getId();
    assertThat(toBinaryString(stableId)).isEqualTo("100000001010000000000000001");
  }

  @Test
  public void testStableIdForegroundType() {
    mLayoutOutput.setComponent(mTestComponent);
    mLayoutOutput.setId(
        calculateLayoutOutputId(mLayoutOutput, LEVEL_TEST, OutputUnitType.FOREGROUND, SEQ_TEST));

    long stableId = mLayoutOutput.getId();
    assertThat(toBinaryString(stableId)).isEqualTo("100000001100000000000000001");
  }

  @Test
  public void testStableIdHostType() {
    mLayoutOutput.setComponent(mTestComponent);
    mLayoutOutput.setId(
        calculateLayoutOutputId(mLayoutOutput, LEVEL_TEST, OutputUnitType.HOST, SEQ_TEST));

    long stableId = mLayoutOutput.getId();
    assertThat(toBinaryString(stableId)).isEqualTo("100000001110000000000000001");
  }

  @Test
  public void testGetIdLevel() {
    mLayoutOutput.setComponent(mTestComponent);
    mLayoutOutput.setId(
        calculateLayoutOutputId(mLayoutOutput, LEVEL_TEST, OutputUnitType.HOST, SEQ_TEST));
    assertThat(LEVEL_TEST).isEqualTo(getLevelFromId(mLayoutOutput.getId()));

    mLayoutOutput.setId(
        calculateLayoutOutputId(mLayoutOutput, MAX_LEVEL_TEST, OutputUnitType.CONTENT, SEQ_TEST));

    assertThat(MAX_LEVEL_TEST).isEqualTo(getLevelFromId(mLayoutOutput.getId()));
  }

  @Test
  public void testGetIdSequence() {
    mLayoutOutput.setComponent(mTestComponent);
    mLayoutOutput.setId(
        calculateLayoutOutputId(mLayoutOutput, LEVEL_TEST, OutputUnitType.HOST, SEQ_TEST));
    assertThat(SEQ_TEST).isEqualTo(getSequenceFromId(mLayoutOutput.getId()));

    mLayoutOutput.setId(
        calculateLayoutOutputId(mLayoutOutput, LEVEL_TEST, OutputUnitType.CONTENT, MAX_SEQ_TEST));

    assertThat(MAX_SEQ_TEST).isEqualTo(getSequenceFromId(mLayoutOutput.getId()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void levelOutOfRangeTest() {
    mLayoutOutput.setComponent(mTestComponent);
    mLayoutOutput.setId(
        LayoutStateOutputIdCalculator.calculateLayoutOutputId(
            mLayoutOutput, MAX_LEVEL_TEST + 1, OutputUnitType.HOST, SEQ_TEST));
  }

  @Test(expected = IllegalArgumentException.class)
  public void sequenceOutOfRangeTest() {
    mLayoutOutput.setComponent(mTestComponent);
    mLayoutOutput.setId(
        LayoutStateOutputIdCalculator.calculateLayoutOutputId(
            mLayoutOutput, LEVEL_TEST, OutputUnitType.FOREGROUND, MAX_SEQ_TEST + 1));
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
