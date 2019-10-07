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

package com.facebook.litho.sections;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class ChangeSetStatsTest {

  @Test
  public void testChangeSetStatsMergeEmptyLeftHandSide() {
    final ChangeSet.ChangeSetStats a = new ChangeSet.ChangeSetStats();
    final ChangeSet.ChangeSetStats b = new ChangeSet.ChangeSetStats(1, 2, 3, 4, 5, 6, 7, 8);

    assertThat(ChangeSet.ChangeSetStats.merge(a, b)).isEqualTo(b);
  }

  @Test
  public void testChangeSetStatsMergeEmptyRightHandSide() {
    final ChangeSet.ChangeSetStats a = new ChangeSet.ChangeSetStats(1, 2, 3, 4, 5, 6, 7, 8);
    final ChangeSet.ChangeSetStats b = new ChangeSet.ChangeSetStats();

    assertThat(ChangeSet.ChangeSetStats.merge(a, b)).isEqualTo(a);
  }

  @Test
  public void testChangeSetStatsFromMoveChange() {
    final Change c = Change.move(0, 10);
    final ChangeSet.ChangeSetStats stats = ChangeSet.ChangeSetStats.fromChange(c, 1);

    assertThat(stats).isEqualTo(new ChangeSet.ChangeSetStats(1, 0, 0, 0, 0, 0, 0, 1));
  }

  @Test
  public void testChangeSetStatsFromInsertChange() {
    final Change c = Change.insert(0, null);
    final ChangeSet.ChangeSetStats stats = ChangeSet.ChangeSetStats.fromChange(c, 1);

    assertThat(stats).isEqualTo(new ChangeSet.ChangeSetStats(1, 1, 0, 0, 0, 0, 0, 0));
  }

  @Test
  public void testChangeSetStatsFromInsertRangeChange() {
    final Change c = Change.insertRange(1, 5, null);
    final ChangeSet.ChangeSetStats stats = ChangeSet.ChangeSetStats.fromChange(c, 5);

    assertThat(stats).isEqualTo(new ChangeSet.ChangeSetStats(5, 0, 5, 0, 0, 0, 0, 0));
  }

  @Test
  public void testChangeSetStatsFromRemoveChange() {
    final Change c = Change.remove(0);
    final ChangeSet.ChangeSetStats stats = ChangeSet.ChangeSetStats.fromChange(c, 1);

    assertThat(stats).isEqualTo(new ChangeSet.ChangeSetStats(1, 0, 0, 1, 0, 0, 0, 0));
  }

  @Test
  public void testChangeSetStatsFromRemoveRangeChange() {
    final Change c = Change.removeRange(0, 5);
    final ChangeSet.ChangeSetStats stats = ChangeSet.ChangeSetStats.fromChange(c, 5);

    assertThat(stats).isEqualTo(new ChangeSet.ChangeSetStats(5, 0, 0, 0, 5, 0, 0, 0));
  }

  @Test
  public void testChangeSetStatsFromUpdateChange() {
    final Change c = Change.update(0, null);
    final ChangeSet.ChangeSetStats stats = ChangeSet.ChangeSetStats.fromChange(c, 1);

    assertThat(stats).isEqualTo(new ChangeSet.ChangeSetStats(1, 0, 0, 0, 0, 1, 0, 0));
  }

  @Test
  public void testChangeSetStatsFromUpdateRangeChange() {
    final Change c = Change.updateRange(0, 8, null);
    final ChangeSet.ChangeSetStats stats = ChangeSet.ChangeSetStats.fromChange(c, 8);

    assertThat(stats).isEqualTo(new ChangeSet.ChangeSetStats(8, 0, 0, 0, 0, 0, 8, 0));
  }

  @Test
  public void testChangeSetStatsFromCombinedChange() {
    final Change c0 = Change.insert(0, null);
    final Change c1 = Change.remove(0);
    final Change c2 = Change.insertRange(0, 2, null);

    ChangeSet.ChangeSetStats stats = ChangeSet.ChangeSetStats.fromChange(c0, 1);
    stats = stats.merge(ChangeSet.ChangeSetStats.fromChange(c1, 1));
    stats = stats.merge(ChangeSet.ChangeSetStats.fromChange(c2, 2));

    assertThat(stats).isEqualTo(new ChangeSet.ChangeSetStats(4, 1, 2, 1, 0, 0, 0, 0));
  }
}
