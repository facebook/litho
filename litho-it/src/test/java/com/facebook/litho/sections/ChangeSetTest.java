/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections;

import static junit.framework.Assert.assertEquals;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests {@link ChangeSet} */
@RunWith(ComponentsTestRunner.class)
public class ChangeSetTest {

  @Test
  public void testAddChange() {
    final ChangeSet changeSet = ChangeSet.acquireChangeSet();

    changeSet.addChange(Change.insert(0, ComponentRenderInfo.createEmpty()));
    assertEquals(changeSet.getCount(), 1);

    changeSet.addChange(Change.remove(0));
    assertEquals(changeSet.getCount(), 0);

    changeSet.addChange(Change.insert(0, ComponentRenderInfo.createEmpty()));
    assertEquals(changeSet.getCount(), 1);
    changeSet.addChange(Change.update(0, ComponentRenderInfo.createEmpty()));
    assertEquals(changeSet.getCount(), 1);

    changeSet.addChange(Change.insert(0, ComponentRenderInfo.createEmpty()));
    assertEquals(changeSet.getCount(), 2);
    changeSet.addChange(Change.move(0, 1));
    assertEquals(changeSet.getCount(), 2);
  }

  @Test
  public void testRangedChange() throws Exception {
    final ChangeSet changeSet = ChangeSet.acquireChangeSet();

    changeSet.addChange(Change.insertRange(0, 10, dummyComponentInfos(10)));
    assertEquals(changeSet.getCount(), 10);

    changeSet.addChange(Change.removeRange(4, 4));
    assertEquals(changeSet.getCount(), 6);

    changeSet.addChange(Change.removeRange(0, 6));
    assertEquals(changeSet.getCount(), 0);

    changeSet.addChange(Change.insertRange(0, 8, dummyComponentInfos(8)));
    changeSet.addChange(Change.insertRange(0, 3, dummyComponentInfos(3)));
    assertEquals(changeSet.getCount(), 11);

    changeSet.addChange(Change.updateRange(7, 3, dummyComponentInfos(3)));
    assertEquals(changeSet.getCount(), 11);

    changeSet.move(9,1);
    assertEquals(changeSet.getCount(), 11);
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
    assertEquals(ChangeSet.acquireChangeSet(10).getCount(), 10);
    assertEquals(ChangeSet.acquireChangeSet().getCount(), 0);
  }

  @Test
  public void testMerge() {
    final ChangeSet changeSet = ChangeSet.acquireChangeSet();
    changeSet.addChange(Change.insert(0, ComponentRenderInfo.createEmpty()));
    changeSet.addChange(Change.insert(1, ComponentRenderInfo.createEmpty()));
    changeSet.addChange(Change.insert(2, ComponentRenderInfo.createEmpty()));

    final ChangeSet secondChangeSet = ChangeSet.acquireChangeSet();
    secondChangeSet.addChange(Change.insert(0, ComponentRenderInfo.createEmpty()));
    secondChangeSet.addChange(Change.insert(1, ComponentRenderInfo.createEmpty()));
    secondChangeSet.addChange(Change.insert(2, ComponentRenderInfo.createEmpty()));
    secondChangeSet.addChange(Change.move(0, 1));

    final ChangeSet mergedChangeSet = ChangeSet.merge(changeSet, secondChangeSet);

    assertEquals(changeSet.getCount(), 3);
    assertEquals(secondChangeSet.getCount(), 3);

    for (int i = 0; i < 3; i++) {
      assertEquals(changeSet.getChangeAt(i).getIndex(), i);
    }

    for (int i = 0; i < 3; i++) {
      assertEquals(secondChangeSet.getChangeAt(i).getIndex(), i);
    }

    assertEquals(mergedChangeSet.getCount(), 6);
    assertEquals(mergedChangeSet.getChangeCount(), 7);

    for (int i = 0; i < 6; i++) {
      assertEquals(mergedChangeSet.getChangeAt(i).getIndex(), i);
    }

    assertEquals(mergedChangeSet.getChangeAt(6).getType(), Change.MOVE);
    assertEquals(mergedChangeSet.getChangeAt(6).getIndex(), 3);
    assertEquals(mergedChangeSet.getChangeAt(6).getToIndex(), 4);
  }

  @Test
  public void testRelease() {
    final ChangeSet changeSet = ChangeSet.acquireChangeSet();
    changeSet.addChange(Change.insert(0, ComponentRenderInfo.createEmpty()));
    changeSet.addChange(Change.insert(1, ComponentRenderInfo.createEmpty()));
    changeSet.addChange(Change.insert(2, ComponentRenderInfo.createEmpty()));

    changeSet.release();
    assertEquals(changeSet.getCount(), 0);
    assertEquals(changeSet.getChangeCount(), 0);
  }
}
