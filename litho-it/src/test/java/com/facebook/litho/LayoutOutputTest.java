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

  private LayoutOutput createLayoutOutput(long id, @OutputUnitType int outputType) {
    return createLayoutOutput(id, outputType, 0, 0);
  }

  private LayoutOutput createLayoutOutput(
      long id, @OutputUnitType int outputType, int flags, long hostMarker) {
    return createLayoutOutputWithBoundsAndHostTranslation(
        id, outputType, new Rect(0, 1, 3, 4), 0, 0, flags, hostMarker);
  }

  private LayoutOutput createLayoutOutputWithBoundsAndHostTranslation(
      long id,
      @OutputUnitType int outputType,
      Rect rect,
      int hostTranslationX,
      int hostTranslationY,
      int flags,
      long hostMarker) {
    return new LayoutOutput(
        mTestComponent,
        null,
        null,
        rect,
        0,
        flags,
        hostMarker,
        0,
        LayoutOutput.STATE_UNKNOWN,
        null);
  }

  @Test
  public void testHostMarkerSet() {
    LayoutOutput layoutOutput = createLayoutOutput(0, OutputUnitType.CONTENT, 0, 10L);
    assertThat(layoutOutput.getHostMarker()).isEqualTo(10);
  }

  @Test
  public void testFlagsSet() {
    LayoutOutput layoutOutput = createLayoutOutput(0, OutputUnitType.CONTENT, 1, 0);
    assertThat(layoutOutput.getFlags()).isEqualTo(1);
  }

  @Test
  public void testStableIdCalculation() {
    LayoutOutput layoutOutput = createLayoutOutput(0, OutputUnitType.CONTENT);

    long stableId =
        calculateLayoutOutputId(
            layoutOutput.getComponent(), LEVEL_TEST, OutputUnitType.CONTENT, SEQ_TEST);

    long stableIdSeq2 =
        calculateLayoutOutputId(
            layoutOutput.getComponent(), LEVEL_TEST + 1, OutputUnitType.CONTENT, SEQ_TEST + 1);

    assertThat(toBinaryString(stableId))
        .isEqualTo(toBinaryString(mTestComponent.getTypeId()) + "000000010000000000000000001");
    assertThat(toBinaryString(stableIdSeq2))
        .isEqualTo(toBinaryString(mTestComponent.getTypeId()) + "000000100000000000000000010");
  }

  @Test
  public void testStableIdBackgroundType() {
    long stableId =
        calculateLayoutOutputId(mTestComponent, LEVEL_TEST, OutputUnitType.BACKGROUND, SEQ_TEST);
    assertThat(toBinaryString(stableId))
        .isEqualTo(toBinaryString(mTestComponent.getTypeId()) + "000000010010000000000000001");
  }

  @Test
  public void testStableIdForegroundType() {
    long stableId =
        calculateLayoutOutputId(mTestComponent, LEVEL_TEST, OutputUnitType.FOREGROUND, SEQ_TEST);
    assertThat(toBinaryString(stableId))
        .isEqualTo(toBinaryString(mTestComponent.getTypeId()) + "000000010100000000000000001");
  }

  @Test
  public void testStableIdHostType() {
    long stableId =
        calculateLayoutOutputId(mTestComponent, LEVEL_TEST, OutputUnitType.HOST, SEQ_TEST);
    assertThat(toBinaryString(stableId))
        .isEqualTo(toBinaryString(mTestComponent.getTypeId()) + "000000010110000000000000001");
  }

  @Test
  public void testStableIdBorderType() {
    long stableId =
        calculateLayoutOutputId(mTestComponent, LEVEL_TEST, OutputUnitType.BORDER, SEQ_TEST);
    assertThat(toBinaryString(stableId))
        .isEqualTo(toBinaryString(mTestComponent.getTypeId()) + "000000011000000000000000001");
  }

  @Test
  public void testGetIdLevel() {
    final long id_0 =
        calculateLayoutOutputId(mTestComponent, LEVEL_TEST, OutputUnitType.HOST, SEQ_TEST);
    assertThat(LEVEL_TEST).isEqualTo(getLevelFromId(id_0));

    final long id_1 =
        calculateLayoutOutputId(mTestComponent, MAX_LEVEL_TEST, OutputUnitType.CONTENT, SEQ_TEST);

    assertThat(MAX_LEVEL_TEST).isEqualTo(getLevelFromId(id_1));
  }

  @Test
  public void testGetIdSequence() {
    final long id_0 =
        calculateLayoutOutputId(mTestComponent, LEVEL_TEST, OutputUnitType.HOST, SEQ_TEST);

    assertThat(SEQ_TEST).isEqualTo(getSequenceFromId(id_0));

    final long id_1 =
        calculateLayoutOutputId(mTestComponent, LEVEL_TEST, OutputUnitType.CONTENT, MAX_SEQ_TEST);

    assertThat(MAX_SEQ_TEST).isEqualTo(getSequenceFromId(id_1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void levelOutOfRangeTest() {
    createLayoutOutput(
        LayoutStateOutputIdCalculator.calculateLayoutOutputId(
            mTestComponent, MAX_LEVEL_TEST + 1, OutputUnitType.HOST, SEQ_TEST),
        OutputUnitType.HOST);
  }

  @Test(expected = IllegalArgumentException.class)
  public void sequenceOutOfRangeTest() {
    createLayoutOutput(
        LayoutStateOutputIdCalculator.calculateLayoutOutputId(
            mTestComponent, LEVEL_TEST, OutputUnitType.FOREGROUND, MAX_SEQ_TEST + 1),
        OutputUnitType.FOREGROUND);
  }

  @Test
  public void testGetMountBoundsNoHostTranslation() {
    Rect mountBounds = new Rect();
    LithoRenderUnit.getMountBounds(mountBounds, new Rect(10, 10, 10, 10), 0, 0);
    assertThat(mountBounds).isEqualTo(new Rect(10, 10, 10, 10));
  }

  @Test
  public void testGetMountBoundsWithHostTranslation() {
    Rect mountBounds = new Rect();
    LithoRenderUnit.getMountBounds(mountBounds, new Rect(10, 10, 10, 10), 5, 2);
    assertThat(mountBounds).isEqualTo(new Rect(5, 8, 5, 8));
  }
}
