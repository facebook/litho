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

  private TestComponent mTestComponent;

  @Before
  public void setup() {
    mTestComponent = new TestComponent();
  }

  @Test
  public void testPositionAndSizeSet() {
    LayoutOutput layoutOutput =
        new LayoutOutput(null, null, mTestComponent, new Rect(0, 1, 3, 4), 0, 0, 0, -1, 0, 0, null);

    assertThat(layoutOutput.getBounds().left).isEqualTo(0);
    assertThat(layoutOutput.getBounds().top).isEqualTo(1);
    assertThat(layoutOutput.getBounds().right).isEqualTo(3);
    assertThat(layoutOutput.getBounds().bottom).isEqualTo(4);
  }

  @Test
  public void testHostMarkerSet() {
    LayoutOutput layoutOutput =
        new LayoutOutput(
            null, null, mTestComponent, new Rect(0, 1, 3, 4), 0, 0, 0, 10l, 0, 0, null);
    assertThat(layoutOutput.getHostMarker()).isEqualTo(10);
  }

  @Test
  public void testFlagsSet() {
    LayoutOutput layoutOutput =
        new LayoutOutput(null, null, mTestComponent, new Rect(0, 1, 3, 4), 0, 0, 1, 0, 0, 0, null);
    assertThat(layoutOutput.getFlags()).isEqualTo(1);
  }

  @Test
  public void testStableIdCalculation() {
    LayoutOutput layoutOutput =
        new LayoutOutput(null, null, mTestComponent, new Rect(0, 1, 3, 4), 0, 0, 0, 0, 0, 0, null);

    long stableId =
        calculateLayoutOutputId(layoutOutput, LEVEL_TEST, OutputUnitType.CONTENT, SEQ_TEST);

    long stableIdSeq2 =
        calculateLayoutOutputId(layoutOutput, LEVEL_TEST + 1, OutputUnitType.CONTENT, SEQ_TEST + 1);

    assertThat(toBinaryString(stableId)).isEqualTo("100000001000000000000000001");
    assertThat(toBinaryString(stableIdSeq2)).isEqualTo("100000010000000000000000010");
  }

  @Test
  public void testStableIdBackgroundType() {
    LayoutOutput layoutOutput =
        new LayoutOutput(null, null, mTestComponent, new Rect(0, 1, 3, 4), 0, 0, 0, 0, 0, 0, null);

    layoutOutput.setId(
        calculateLayoutOutputId(layoutOutput, LEVEL_TEST, OutputUnitType.BACKGROUND, SEQ_TEST));

    long stableId = layoutOutput.getId();
    assertThat(toBinaryString(stableId)).isEqualTo("100000001010000000000000001");
  }

  @Test
  public void testStableIdForegroundType() {
    LayoutOutput layoutOutput =
        new LayoutOutput(null, null, mTestComponent, new Rect(0, 1, 3, 4), 0, 0, 0, 0, 0, 0, null);

    layoutOutput.setId(
        calculateLayoutOutputId(layoutOutput, LEVEL_TEST, OutputUnitType.FOREGROUND, SEQ_TEST));

    long stableId = layoutOutput.getId();
    assertThat(toBinaryString(stableId)).isEqualTo("100000001100000000000000001");
  }

  @Test
  public void testStableIdHostType() {
    LayoutOutput layoutOutput =
        new LayoutOutput(null, null, mTestComponent, new Rect(0, 1, 3, 4), 0, 0, 0, 0, 0, 0, null);
    layoutOutput.setId(
        calculateLayoutOutputId(layoutOutput, LEVEL_TEST, OutputUnitType.HOST, SEQ_TEST));

    long stableId = layoutOutput.getId();
    assertThat(toBinaryString(stableId)).isEqualTo("100000001110000000000000001");
  }

  @Test
  public void testGetIdLevel() {
    LayoutOutput layoutOutput =
        new LayoutOutput(null, null, mTestComponent, new Rect(0, 1, 3, 4), 0, 0, 0, 0, 0, 0, null);

    layoutOutput.setId(
        calculateLayoutOutputId(layoutOutput, LEVEL_TEST, OutputUnitType.HOST, SEQ_TEST));
    assertThat(LEVEL_TEST).isEqualTo(getLevelFromId(layoutOutput.getId()));

    layoutOutput.setId(
        calculateLayoutOutputId(layoutOutput, MAX_LEVEL_TEST, OutputUnitType.CONTENT, SEQ_TEST));

    assertThat(MAX_LEVEL_TEST).isEqualTo(getLevelFromId(layoutOutput.getId()));
  }

  @Test
  public void testGetIdSequence() {
    LayoutOutput layoutOutput =
        new LayoutOutput(null, null, mTestComponent, new Rect(0, 1, 3, 4), 0, 0, 0, 0, 0, 0, null);

    layoutOutput.setId(
        calculateLayoutOutputId(layoutOutput, LEVEL_TEST, OutputUnitType.HOST, SEQ_TEST));
    assertThat(SEQ_TEST).isEqualTo(getSequenceFromId(layoutOutput.getId()));

    layoutOutput.setId(
        calculateLayoutOutputId(layoutOutput, LEVEL_TEST, OutputUnitType.CONTENT, MAX_SEQ_TEST));

    assertThat(MAX_SEQ_TEST).isEqualTo(getSequenceFromId(layoutOutput.getId()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void levelOutOfRangeTest() {
    LayoutOutput layoutOutput =
        new LayoutOutput(null, null, mTestComponent, new Rect(0, 1, 3, 4), 0, 0, 0, 0, 0, 0, null);

    layoutOutput.setId(
        LayoutStateOutputIdCalculator.calculateLayoutOutputId(
            layoutOutput, MAX_LEVEL_TEST + 1, OutputUnitType.HOST, SEQ_TEST));
  }

  @Test(expected = IllegalArgumentException.class)
  public void sequenceOutOfRangeTest() {
    LayoutOutput layoutOutput =
        new LayoutOutput(null, null, mTestComponent, new Rect(0, 1, 3, 4), 0, 0, 0, 0, 0, 0, null);

    layoutOutput.setId(
        LayoutStateOutputIdCalculator.calculateLayoutOutputId(
            layoutOutput, LEVEL_TEST, OutputUnitType.FOREGROUND, MAX_SEQ_TEST + 1));
  }

  @Test
  public void testGetMountBoundsNoHostTranslation() {
    LayoutOutput layoutOutput =
        new LayoutOutput(
            null, null, mTestComponent, new Rect(10, 10, 10, 10), 0, 0, 0, 0, 0, 0, null);

    Rect mountBounds = new Rect();
    layoutOutput.getMountBounds(mountBounds);

    assertThat(mountBounds).isEqualTo(layoutOutput.getBounds());
  }

  @Test
  public void testGetMountBoundsWithHostTranslation() {
    LayoutOutput layoutOutput =
        new LayoutOutput(
            null, null, mTestComponent, new Rect(10, 10, 10, 10), 5, 2, 0, 0, 0, 0, null);

    Rect mountBounds = new Rect();
    layoutOutput.getMountBounds(mountBounds);

    assertThat(mountBounds).isEqualTo(new Rect(5, 8, 5, 8));
  }
}
