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
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.facebook.litho.WorkingRangeContainer.RangeTuple;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class WorkingRangeContainerTest {
  private static final String NAME = "workingRangeName";

  private WorkingRangeContainer mWorkingRangeContainer;
  private WorkingRange mWorkingRange;
  private Component mComponent;
  private Component mComponent2;

  @Before
  public void setup() {
    mWorkingRangeContainer = new WorkingRangeContainer();

    mWorkingRange = new TestWorkingRange();
    mComponent = mock(Component.class);
    when(mComponent.getGlobalKey()).thenReturn("component");
    mComponent2 = mock(Component.class);
    when(mComponent2.getGlobalKey()).thenReturn("component2");
  }

  @Test
  public void testRegisterWorkingRange() {
    mWorkingRangeContainer.registerWorkingRange(NAME, mWorkingRange, mComponent);

    final Map<String, RangeTuple> workingRanges =
        mWorkingRangeContainer.getWorkingRangesForTestOnly();
    assertThat(workingRanges.size()).isEqualTo(1);

    final String key = workingRanges.keySet().iterator().next();
    assertThat(key).isEqualTo(NAME + "_" + mWorkingRange.hashCode());

    final RangeTuple rangeTuple = workingRanges.get(key);
    assertThat(rangeTuple.mWorkingRange).isEqualTo(mWorkingRange);
    assertThat(rangeTuple.mComponents.size()).isEqualTo(1);
    assertThat(rangeTuple.mComponents.get(0)).isEqualTo(mComponent);
  }

  @Test
  public void testIsEnteredRange() {
    RangeTuple rangeTuple = new RangeTuple(NAME, mWorkingRange, mComponent);
    WorkingRange workingRange = rangeTuple.mWorkingRange;

    assertThat(WorkingRangeContainer.isEnteringRange(workingRange, 0, 0, 1, 0, 1)).isEqualTo(true);
    assertThat(WorkingRangeContainer.isEnteringRange(workingRange, 0, 1, 2, 1, 2)).isEqualTo(false);
  }

  @Test
  public void testIsExitedRange() {
    RangeTuple rangeTuple = new RangeTuple(NAME, mWorkingRange, mComponent);
    WorkingRange workingRange = rangeTuple.mWorkingRange;

    assertThat(WorkingRangeContainer.isExitingRange(workingRange, 0, 0, 1, 0, 1)).isEqualTo(false);
    assertThat(WorkingRangeContainer.isExitingRange(workingRange, 0, 1, 2, 1, 2)).isEqualTo(true);
  }

  @Test
  public void testDispatchOnExitedRangeIfNeeded() {
    TestWorkingRange workingRange = new TestWorkingRange();
    mWorkingRangeContainer.registerWorkingRange(NAME, workingRange, mComponent);

    TestWorkingRange workingRange2 = new TestWorkingRange();
    mWorkingRangeContainer.registerWorkingRange(NAME, workingRange2, mComponent2);

    final WorkingRangeStatusHandler statusHandler = new WorkingRangeStatusHandler();
    statusHandler.setStatus(NAME, mComponent, WorkingRangeStatusHandler.STATUS_IN_RANGE);
    doNothing().when(mComponent).dispatchOnExitedRange(isA(String.class));

    statusHandler.setStatus(NAME, mComponent2, WorkingRangeStatusHandler.STATUS_OUT_OF_RANGE);
    doNothing().when(mComponent2).dispatchOnExitedRange(isA(String.class));

    mWorkingRangeContainer.dispatchOnExitedRangeIfNeeded(statusHandler);

    verify(mComponent, times(1)).dispatchOnExitedRange(NAME);
    verify(mComponent2, times(0)).dispatchOnExitedRange(NAME);
  }

  private static class TestWorkingRange implements WorkingRange {

    boolean isExitRangeCalled = false;

    @Override
    public boolean shouldEnterRange(
        int position,
        int firstVisibleIndex,
        int lastVisibleIndex,
        int firstFullyVisibleIndex,
        int lastFullyVisibleIndex) {
      return isInRange(position, firstVisibleIndex, lastVisibleIndex);
    }

    @Override
    public boolean shouldExitRange(
        int position,
        int firstVisibleIndex,
        int lastVisibleIndex,
        int firstFullyVisibleIndex,
        int lastFullyVisibleIndex) {
      isExitRangeCalled = true;
      return !isInRange(position, firstVisibleIndex, lastVisibleIndex);
    }

    private static boolean isInRange(int position, int firstVisibleIndex, int lastVisibleIndex) {
      return (position >= firstVisibleIndex && position <= lastVisibleIndex);
    }
  }
}
