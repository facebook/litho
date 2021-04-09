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

import static com.facebook.litho.LayoutStateOutputIdCalculator.calculateLayoutOutputId;
import static com.facebook.litho.LayoutStateOutputIdCalculator.getLevelFromId;
import static com.facebook.litho.LayoutStateOutputIdCalculator.getSequenceFromId;
import static java.lang.Long.toBinaryString;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.graphics.Rect;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class LayoutOutputTest {

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
  }

  private TestComponent mTestComponent;

  @Before
  public void setup() {
    mTestComponent = new TestComponent();
  }

  private LayoutOutput createLayoutOutput() {
    return createLayoutOutput(0, 0);
  }

  private LayoutOutput createLayoutOutput(int flags, long hostMarker) {
    return createLayoutOutputWithBoundsAndHostTranslation(
        new Rect(0, 1, 3, 4), 0, 0, flags, hostMarker);
  }

  private LayoutOutput createLayoutOutputWithBoundsAndHostTranslation(
      Rect rect, int hostTranslationX, int hostTranslationY, int flags, long hostMarker) {
    return new LayoutOutput(
        null,
        null,
        null,
        mTestComponent,
        null,
        rect,
        hostTranslationX,
        hostTranslationY,
        flags,
        hostMarker,
        0,
        0,
        null);
  }

  @Test
  public void testPositionAndSizeSet() {
    LayoutOutput layoutOutput = createLayoutOutput(0, -1);
    assertThat(layoutOutput.getBounds().left).isEqualTo(0);
    assertThat(layoutOutput.getBounds().top).isEqualTo(1);
    assertThat(layoutOutput.getBounds().right).isEqualTo(3);
    assertThat(layoutOutput.getBounds().bottom).isEqualTo(4);
  }

  @Test
  public void testHostMarkerSet() {
    LayoutOutput layoutOutput = createLayoutOutput(0, 10L);
    assertThat(layoutOutput.getHostMarker()).isEqualTo(10);
  }

  @Test
  public void testFlagsSet() {
    LayoutOutput layoutOutput = createLayoutOutput(1, 0);
    assertThat(layoutOutput.getFlags()).isEqualTo(1);
  }

  @Test
  public void testStableIdCalculation() {
    LayoutOutput layoutOutput = createLayoutOutput();

    long stableId =
        calculateLayoutOutputId(layoutOutput, LEVEL_TEST, OutputUnitType.CONTENT, SEQ_TEST);

    long stableIdSeq2 =
        calculateLayoutOutputId(layoutOutput, LEVEL_TEST + 1, OutputUnitType.CONTENT, SEQ_TEST + 1);

    assertThat(toBinaryString(stableId))
        .isEqualTo(toBinaryString(mTestComponent.getTypeId()) + "000000010000000000000000001");
    assertThat(toBinaryString(stableIdSeq2))
        .isEqualTo(toBinaryString(mTestComponent.getTypeId()) + "000000100000000000000000010");
  }

  @Test
  public void testStableIdBackgroundType() {
    LayoutOutput layoutOutput = createLayoutOutput();
    layoutOutput.setId(
        calculateLayoutOutputId(layoutOutput, LEVEL_TEST, OutputUnitType.BACKGROUND, SEQ_TEST));

    long stableId = layoutOutput.getId();
    assertThat(toBinaryString(stableId))
        .isEqualTo(toBinaryString(mTestComponent.getTypeId()) + "000000010010000000000000001");
  }

  @Test
  public void testStableIdForegroundType() {
    LayoutOutput layoutOutput = createLayoutOutput();
    layoutOutput.setId(
        calculateLayoutOutputId(layoutOutput, LEVEL_TEST, OutputUnitType.FOREGROUND, SEQ_TEST));

    long stableId = layoutOutput.getId();
    assertThat(toBinaryString(stableId))
        .isEqualTo(toBinaryString(mTestComponent.getTypeId()) + "000000010100000000000000001");
  }

  @Test
  public void testStableIdHostType() {
    LayoutOutput layoutOutput = createLayoutOutput();
    layoutOutput.setId(
        calculateLayoutOutputId(layoutOutput, LEVEL_TEST, OutputUnitType.HOST, SEQ_TEST));

    long stableId = layoutOutput.getId();
    assertThat(toBinaryString(stableId))
        .isEqualTo(toBinaryString(mTestComponent.getTypeId()) + "000000010110000000000000001");
  }

  @Test
  public void testStableIdBorderType() {
    LayoutOutput layoutOutput = createLayoutOutput();
    layoutOutput.setId(
        calculateLayoutOutputId(layoutOutput, LEVEL_TEST, OutputUnitType.BORDER, SEQ_TEST));

    long stableId = layoutOutput.getId();
    assertThat(toBinaryString(stableId))
        .isEqualTo(toBinaryString(mTestComponent.getTypeId()) + "000000011000000000000000001");
  }

  @Test
  public void testGetIdLevel() {
    LayoutOutput layoutOutput = createLayoutOutput();
    layoutOutput.setId(
        calculateLayoutOutputId(layoutOutput, LEVEL_TEST, OutputUnitType.HOST, SEQ_TEST));
    assertThat(LEVEL_TEST).isEqualTo(getLevelFromId(layoutOutput.getId()));

    layoutOutput.setId(
        calculateLayoutOutputId(layoutOutput, MAX_LEVEL_TEST, OutputUnitType.CONTENT, SEQ_TEST));

    assertThat(MAX_LEVEL_TEST).isEqualTo(getLevelFromId(layoutOutput.getId()));
  }

  @Test
  public void testGetIdSequence() {
    LayoutOutput layoutOutput = createLayoutOutput();
    layoutOutput.setId(
        calculateLayoutOutputId(layoutOutput, LEVEL_TEST, OutputUnitType.HOST, SEQ_TEST));
    assertThat(SEQ_TEST).isEqualTo(getSequenceFromId(layoutOutput.getId()));

    layoutOutput.setId(
        calculateLayoutOutputId(layoutOutput, LEVEL_TEST, OutputUnitType.CONTENT, MAX_SEQ_TEST));

    assertThat(MAX_SEQ_TEST).isEqualTo(getSequenceFromId(layoutOutput.getId()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void levelOutOfRangeTest() {
    LayoutOutput layoutOutput = createLayoutOutput();
    layoutOutput.setId(
        LayoutStateOutputIdCalculator.calculateLayoutOutputId(
            layoutOutput, MAX_LEVEL_TEST + 1, OutputUnitType.HOST, SEQ_TEST));
  }

  @Test(expected = IllegalArgumentException.class)
  public void sequenceOutOfRangeTest() {
    LayoutOutput layoutOutput = createLayoutOutput();
    layoutOutput.setId(
        LayoutStateOutputIdCalculator.calculateLayoutOutputId(
            layoutOutput, LEVEL_TEST, OutputUnitType.FOREGROUND, MAX_SEQ_TEST + 1));
  }

  @Test
  public void testGetMountBoundsNoHostTranslation() {
    LayoutOutput layoutOutput =
        createLayoutOutputWithBoundsAndHostTranslation(new Rect(10, 10, 10, 10), 0, 0, 0, 0);

    Rect mountBounds = new Rect();
    layoutOutput.getMountBounds(mountBounds);

    assertThat(mountBounds).isEqualTo(layoutOutput.getBounds());
  }

  @Test
  public void testGetMountBoundsWithHostTranslation() {
    LayoutOutput layoutOutput =
        createLayoutOutputWithBoundsAndHostTranslation(new Rect(10, 10, 10, 10), 5, 2, 0, 0);

    Rect mountBounds = new Rect();
    layoutOutput.getMountBounds(mountBounds);

    assertThat(mountBounds).isEqualTo(new Rect(5, 8, 5, 8));
  }
}
