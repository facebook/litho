/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.support.v4.util.SimpleArrayMap;
import com.facebook.litho.WorkingRangeContainer.RangeTuple;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class WorkingRangeContainerTest {
  private static final String NAME = "workingRangeName";
  private static final int COMPONENT_ID = 1024;

  private WorkingRangeContainer mWorkingRangeContainer;
  private WorkingRange mWorkingRange;
  private Component mComponent;

  @Before
  public void setup() {
    mWorkingRangeContainer = new WorkingRangeContainer();

    mWorkingRange = new TestWorkingRange();
    mComponent = mock(Component.class);
    when(mComponent.getId()).thenReturn(COMPONENT_ID);
  }

  @Test
  public void testRegisterWorkingRange() {
    mWorkingRangeContainer.registerWorkingRange(NAME, mWorkingRange, mComponent);

    final SimpleArrayMap<String, RangeTuple> workingRanges =
        mWorkingRangeContainer.getWorkingRanges();
    assertThat(workingRanges.size()).isEqualTo(1);

    final String key = workingRanges.keyAt(0);
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

  private static class TestWorkingRange implements WorkingRange {
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
      return !isInRange(position, firstVisibleIndex, lastVisibleIndex);
    }

    private static boolean isInRange(int position, int firstVisibleIndex, int lastVisibleIndex) {
      return (position >= firstVisibleIndex && position <= lastVisibleIndex);
    }
  }
}
