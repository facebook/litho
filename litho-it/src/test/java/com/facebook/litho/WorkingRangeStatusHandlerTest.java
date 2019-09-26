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

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class WorkingRangeStatusHandlerTest {
  private static final String NAME = "workingRangeName";
  private static final String GLOBAL_KEY = "globalKey";

  private WorkingRangeStatusHandler mWorkingRangeStateHandler;
  private Component mComponent;

  @Before
  public void setup() {
    mWorkingRangeStateHandler = new WorkingRangeStatusHandler();
    mComponent = mock(Component.class);
    when(mComponent.getGlobalKey()).thenReturn(GLOBAL_KEY);
  }

  @Test
  public void testIsNotInRange() {
    mWorkingRangeStateHandler.setStatus(
        NAME, mComponent, WorkingRangeStatusHandler.STATUS_OUT_OF_RANGE);

    boolean notInRange = !mWorkingRangeStateHandler.isInRange(NAME, mComponent);
    assertThat(notInRange).isEqualTo(true);
  }

  @Test
  public void testIsInRange() {
    mWorkingRangeStateHandler.setStatus(
        NAME, mComponent, WorkingRangeStatusHandler.STATUS_IN_RANGE);

    boolean inRange = mWorkingRangeStateHandler.isInRange(NAME, mComponent);
    assertThat(inRange).isEqualTo(true);
  }

  @Test
  public void testSetEnteredRangeState() {
    mWorkingRangeStateHandler.setStatus(
        NAME, mComponent, WorkingRangeStatusHandler.STATUS_OUT_OF_RANGE);
    boolean notInRange = !mWorkingRangeStateHandler.isInRange(NAME, mComponent);
    assertThat(notInRange).isEqualTo(true);

    mWorkingRangeStateHandler.setEnteredRangeStatus(NAME, mComponent);
    boolean inRange = mWorkingRangeStateHandler.isInRange(NAME, mComponent);
    assertThat(inRange).isEqualTo(true);
  }

  @Test
  public void testSetExitedRangeState() {
    mWorkingRangeStateHandler.setStatus(
        NAME, mComponent, WorkingRangeStatusHandler.STATUS_IN_RANGE);
    boolean inRange = mWorkingRangeStateHandler.isInRange(NAME, mComponent);
    assertThat(inRange).isEqualTo(true);

    mWorkingRangeStateHandler.setExitedRangeStatus(NAME, mComponent);
    boolean notInRange = !mWorkingRangeStateHandler.isInRange(NAME, mComponent);
    assertThat(notInRange).isEqualTo(true);
  }
}
