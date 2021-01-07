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

import static com.facebook.litho.sections.Change.MOVE;
import static com.facebook.litho.sections.ChangeSet.acquireChangeSet;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

/** Tests {@link ChangeSet} */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner.class)
public class ChangeSetTest {

  @Test
  public void testAddChange() {
    final ChangeSet changeSet = ChangeSet.acquireChangeSet(null, false);

    changeSet.addChange(Change.insert(0, ComponentRenderInfo.createEmpty()));
    assertThat(changeSet.getCount()).isEqualTo(1);

    changeSet.addChange(Change.remove(0));
    assertThat(changeSet.getCount()).isEqualTo(0);

    changeSet.addChange(Change.insert(0, ComponentRenderInfo.createEmpty()));
    assertThat(changeSet.getCount()).isEqualTo(1);
    changeSet.addChange(Change.update(0, ComponentRenderInfo.createEmpty()));
    assertThat(changeSet.getCount()).isEqualTo(1);

    changeSet.addChange(Change.insert(0, ComponentRenderInfo.createEmpty()));
    assertThat(changeSet.getCount()).isEqualTo(2);
    changeSet.addChange(Change.move(0, 1));
    assertThat(changeSet.getCount()).isEqualTo(2);
  }

  @Test
  public void testRangedChange() throws Exception {
    final ChangeSet changeSet = ChangeSet.acquireChangeSet(null, false);

    changeSet.addChange(Change.insertRange(0, 10, dummyComponentInfos(10)));
    assertThat(changeSet.getCount()).isEqualTo(10);

    changeSet.addChange(Change.removeRange(4, 4));
    assertThat(changeSet.getCount()).isEqualTo(6);

    changeSet.addChange(Change.removeRange(0, 6));
    assertThat(changeSet.getCount()).isEqualTo(0);

    changeSet.addChange(Change.insertRange(0, 8, dummyComponentInfos(8)));
    changeSet.addChange(Change.insertRange(0, 3, dummyComponentInfos(3)));
    assertThat(changeSet.getCount()).isEqualTo(11);

    changeSet.addChange(Change.updateRange(7, 3, dummyComponentInfos(3)));
    assertThat(changeSet.getCount()).isEqualTo(11);

    changeSet.move(9, 1);
    assertThat(changeSet.getCount()).isEqualTo(11);
  }

  private List<RenderInfo> dummyComponentInfos(int count) {
    ArrayList<RenderInfo> renderInfos = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      renderInfos.add(ComponentRenderInfo.createEmpty());
    }
    return renderInfos;
  }

  @Test
  public void testInitialCount() {
    assertThat(acquireChangeSet(10, null, false).getCount()).isEqualTo(10);
    assertThat(acquireChangeSet(null, false).getCount()).isEqualTo(0);
  }

  @Test
  public void testMerge() {
    final ChangeSet changeSet = ChangeSet.acquireChangeSet(null, false);
    changeSet.addChange(Change.insert(0, ComponentRenderInfo.createEmpty()));
    changeSet.addChange(Change.insert(1, ComponentRenderInfo.createEmpty()));
    changeSet.addChange(Change.insert(2, ComponentRenderInfo.createEmpty()));

    final ChangeSet secondChangeSet = ChangeSet.acquireChangeSet(null, false);
    secondChangeSet.addChange(Change.insert(0, ComponentRenderInfo.createEmpty()));
    secondChangeSet.addChange(Change.insert(1, ComponentRenderInfo.createEmpty()));
    secondChangeSet.addChange(Change.insert(2, ComponentRenderInfo.createEmpty()));
    secondChangeSet.addChange(Change.move(0, 1));

    final ChangeSet mergedChangeSet = ChangeSet.merge(changeSet, secondChangeSet);

    assertThat(changeSet.getCount()).isEqualTo(3);
    assertThat(secondChangeSet.getCount()).isEqualTo(3);

    for (int i = 0; i < 3; i++) {
      assertThat(changeSet.getChangeAt(i).getIndex()).isEqualTo(i);
    }

    for (int i = 0; i < 3; i++) {
      assertThat(secondChangeSet.getChangeAt(i).getIndex()).isEqualTo(i);
    }

    assertThat(mergedChangeSet.getCount()).isEqualTo(6);
    assertThat(mergedChangeSet.getChangeCount()).isEqualTo(7);

    for (int i = 0; i < 6; i++) {
      assertThat(mergedChangeSet.getChangeAt(i).getIndex()).isEqualTo(i);
    }

    assertThat(mergedChangeSet.getChangeAt(6).getType()).isEqualTo(MOVE);
    assertThat(mergedChangeSet.getChangeAt(6).getIndex()).isEqualTo(3);
    assertThat(mergedChangeSet.getChangeAt(6).getToIndex()).isEqualTo(4);
  }

  @Test
  public void testRelease() {
    final ChangeSet changeSet = ChangeSet.acquireChangeSet(null, false);
    changeSet.addChange(Change.insert(0, ComponentRenderInfo.createEmpty()));
    changeSet.addChange(Change.insert(1, ComponentRenderInfo.createEmpty()));
    changeSet.addChange(Change.insert(2, ComponentRenderInfo.createEmpty()));

    changeSet.release();
    assertThat(changeSet.getCount()).isEqualTo(0);
    assertThat(changeSet.getChangeCount()).isEqualTo(0);
  }

  @Test
  public void testAddChangeWithData() {
    final ChangeSet changeSet = ChangeSet.acquireChangeSet(null, false);
    final Object data1 = new Object();

    changeSet.addChange(Change.insert(0, ComponentRenderInfo.createEmpty(), data1));
    assertThat(changeSet.getCount()).isEqualTo(1);
    assertThat(changeSet.getChanges().get(0).getPrevData()).isNull();
    assertThat(changeSet.getChanges().get(0).getNextData()).isEqualTo(ImmutableList.of(data1));

    changeSet.addChange(Change.remove(0, data1));
    assertThat(changeSet.getCount()).isEqualTo(0);
    assertThat(changeSet.getChanges().get(1).getPrevData()).isEqualTo(ImmutableList.of(data1));
    assertThat(changeSet.getChanges().get(1).getNextData()).isNull();

    final Object data2 = new Object();
    changeSet.addChange(Change.insert(0, ComponentRenderInfo.createEmpty(), data2));
    assertThat(changeSet.getCount()).isEqualTo(1);
    assertThat(changeSet.getChanges().get(2).getPrevData()).isNull();
    assertThat(changeSet.getChanges().get(2).getNextData()).isEqualTo(ImmutableList.of(data2));

    final Object data3 = new Object();
    changeSet.addChange(Change.update(0, ComponentRenderInfo.createEmpty(), data2, data3));
    assertThat(changeSet.getCount()).isEqualTo(1);
    assertThat(changeSet.getChanges().get(3).getPrevData()).isEqualTo(ImmutableList.of(data2));
    assertThat(changeSet.getChanges().get(3).getNextData()).isEqualTo(ImmutableList.of(data3));

    final Object data4 = new Object();
    changeSet.addChange(Change.insert(0, ComponentRenderInfo.createEmpty(), data4));
    assertThat(changeSet.getCount()).isEqualTo(2);
    assertThat(changeSet.getChanges().get(4).getPrevData()).isNull();
    assertThat(changeSet.getChanges().get(4).getNextData()).isEqualTo(ImmutableList.of(data4));

    changeSet.addChange(Change.move(0, 1, data4));
    assertThat(changeSet.getCount()).isEqualTo(2);
    assertThat(changeSet.getChanges().get(5).getPrevData()).isEqualTo(ImmutableList.of(data4));
    assertThat(changeSet.getChanges().get(5).getNextData()).isEqualTo(ImmutableList.of(data4));
  }
}
