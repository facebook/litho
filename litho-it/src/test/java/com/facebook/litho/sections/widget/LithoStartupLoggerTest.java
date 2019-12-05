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

package com.facebook.litho.sections.widget;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.LithoStartupLogger;
import org.junit.Before;
import org.junit.Test;

/** Tests {@link com.facebook.litho.LithoStartupLogger} */
public class LithoStartupLoggerTest {

  private TestLithoStartupLogger mTestLithoStartupLogger;

  @Before
  public void setup() {
    mTestLithoStartupLogger = new TestLithoStartupLogger();
  }

  @Test
  public void markPoint_pointsTraced() {
    mTestLithoStartupLogger.markPoint("_event1", "_stage1");
    mTestLithoStartupLogger.markPoint("_event2", "_stage2");
    mTestLithoStartupLogger.markPoint("_event3", "_stage4", "attr1");

    assertThat(mTestLithoStartupLogger.tracePointCount()).isEqualTo(3);
    assertThat(mTestLithoStartupLogger.getTracedPointAt(0)).isEqualTo("litho_event1_stage1");
    assertThat(mTestLithoStartupLogger.getTracedPointAt(1)).isEqualTo("litho_event2_stage2");
    assertThat(mTestLithoStartupLogger.getTracedPointAt(2)).isEqualTo("litho_attr1_event3_stage4");
  }

  @Test
  public void markPoint_noDuplicatePoints() {
    mTestLithoStartupLogger.markPoint("_event1", "_stage1");
    mTestLithoStartupLogger.markPoint("_event1", "_stage1");

    assertThat(mTestLithoStartupLogger.tracePointCount()).isEqualTo(1);
    assertThat(mTestLithoStartupLogger.getTracedPointAt(0)).isEqualTo("litho_event1_stage1");
  }

  @Test
  public void markPoint_endStageWithoutStartNotTraced() {
    mTestLithoStartupLogger.markPoint("_event1", LithoStartupLogger.END);

    assertThat(mTestLithoStartupLogger.tracePointCount()).isEqualTo(0);

    mTestLithoStartupLogger.markPoint("_event1", LithoStartupLogger.START);
    mTestLithoStartupLogger.markPoint("_event1", LithoStartupLogger.END);

    assertThat(mTestLithoStartupLogger.tracePointCount()).isEqualTo(2);
    assertThat(mTestLithoStartupLogger.getTracedPointAt(0)).isEqualTo("litho_event1_start");
    assertThat(mTestLithoStartupLogger.getTracedPointAt(1)).isEqualTo("litho_event1_end");
  }
}
