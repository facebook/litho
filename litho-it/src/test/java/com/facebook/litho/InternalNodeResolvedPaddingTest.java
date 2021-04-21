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

import static com.facebook.rendercore.utils.MeasureSpecUtils.exactly;
import static com.facebook.yoga.YogaDirection.LTR;
import static com.facebook.yoga.YogaDirection.RTL;
import static com.facebook.yoga.YogaEdge.END;
import static com.facebook.yoga.YogaEdge.LEFT;
import static com.facebook.yoga.YogaEdge.RIGHT;
import static com.facebook.yoga.YogaEdge.START;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner.class)
public class InternalNodeResolvedPaddingTest {

  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();

  private Component.Builder builder;

  @Before
  public void setup() {
    final ComponentContext context = mLithoViewRule.getContext();
    context.setLayoutStateContextForTesting();
    builder = Column.create(context);
  }

  private InternalNodeResolvedPaddingTest padding(YogaEdge edge, int padding) {
    builder.paddingPx(edge, padding);
    return this;
  }

  private InternalNodeResolvedPaddingTest direction(YogaDirection direction) {
    builder.layoutDirection(direction);
    return this;
  }

  private LithoLayoutResult calculateLayout() {
    return Layout.createAndMeasureComponent(
            mLithoViewRule.getContext(), builder.build(), exactly(100), exactly(100))
        .mResult;
  }

  @Test
  public void testPaddingLeftWithUndefinedStartEnd() {
    LithoLayoutResult result = padding(LEFT, 10).direction(LTR).calculateLayout();
    assertThat(result.getPaddingLeft()).isEqualTo(10);
  }

  @Test
  public void testPaddingLeftWithDefinedStart() {
    LithoLayoutResult result = padding(START, 5).padding(LEFT, 10).direction(LTR).calculateLayout();
    assertThat(result.getPaddingLeft()).isEqualTo(5);
  }

  @Test
  public void testPaddingLeftWithDefinedEnd() {
    LithoLayoutResult result = padding(END, 5).padding(LEFT, 10).direction(LTR).calculateLayout();
    assertThat(result.getPaddingLeft()).isEqualTo(10);
  }

  @Test
  public void testPaddingLeftWithDefinedStartInRtl() {
    LithoLayoutResult result = padding(START, 5).padding(LEFT, 10).direction(RTL).calculateLayout();
    assertThat(result.getPaddingLeft()).isEqualTo(10);
  }

  @Test
  public void testPaddingLeftWithDefinedEndInRtl() {
    LithoLayoutResult result = padding(END, 5).padding(LEFT, 10).direction(RTL).calculateLayout();
    assertThat(result.getPaddingLeft()).isEqualTo(5);
  }

  @Test
  public void testPaddingRightWithUndefinedStartEnd() {
    LithoLayoutResult result = padding(RIGHT, 10).direction(LTR).calculateLayout();
    assertThat(result.getPaddingRight()).isEqualTo(10);
  }

  @Test
  public void testPaddingRightWithDefinedStart() {
    padding(START, 5).padding(RIGHT, 10).direction(LTR);
    LithoLayoutResult result = calculateLayout();
    assertThat(result.getPaddingRight()).isEqualTo(10);
  }

  @Test
  public void testPaddingRightWithDefinedEnd() {
    LithoLayoutResult result = padding(END, 5).padding(RIGHT, 10).direction(LTR).calculateLayout();
    assertThat(result.getPaddingRight()).isEqualTo(5);
  }

  @Test
  public void testPaddingRightWithDefinedStartInRtl() {
    LithoLayoutResult result =
        padding(START, 5).padding(RIGHT, 10).direction(RTL).calculateLayout();
    assertThat(result.getPaddingRight()).isEqualTo(5);
  }

  @Test
  public void testPaddingRightWithDefinedEndInRtl() {
    LithoLayoutResult result = padding(END, 5).padding(RIGHT, 10).direction(RTL).calculateLayout();
    assertThat(result.getPaddingRight()).isEqualTo(10);
  }
}
