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
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.facebook.litho.sections.logger.SectionComponentLogger;
import com.facebook.litho.testing.sections.TestSectionCreator;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.ComponentRenderInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

/** Tests {@link ChangeSetState} */
@RunWith(ComponentsTestRunner.class)
public class ChangeSetStateTest {
  private SectionContext mSectionContext;
  private SectionComponentLogger mSectionComponentLogger;
  private String mSectionTreeTag;
  private String mCurrentPrefix;
  private String mNextPrefix;

  @Before
  public void setup() {
    mSectionContext = new SectionContext(RuntimeEnvironment.application);
    mSectionComponentLogger = mock(SectionComponentLogger.class);
    mSectionTreeTag = "";
    mCurrentPrefix = "";
    mNextPrefix = "";
  }

  @Test
  public void testNewChangeSetGeneration() {
    final Section leaf1 = TestSectionCreator.createChangeSetComponent(
        "leaf1",
        Change.insert(0, ComponentRenderInfo.createEmpty()),
        Change.insert(1, ComponentRenderInfo.createEmpty()),
        Change.insert(2, ComponentRenderInfo.createEmpty()));

    final Section leaf2 = TestSectionCreator.createChangeSetComponent(
        "leaf2",
        Change.insert(0, ComponentRenderInfo.createEmpty()),
        Change.insert(1, ComponentRenderInfo.createEmpty()));

    final Section node = TestSectionCreator
        .createSectionComponent("node1", leaf1, leaf2);

    final Section leaf3 = TestSectionCreator.createChangeSetComponent(
        "leaf3",
        Change.insert(0, ComponentRenderInfo.createEmpty()),
        Change.insert(1, ComponentRenderInfo.createEmpty()));

    final Section leaf4 = TestSectionCreator.createChangeSetComponent(
        "leaf4",
        Change.insert(0, ComponentRenderInfo.createEmpty()),
        Change.insert(1, ComponentRenderInfo.createEmpty()));

    final Section node1 = TestSectionCreator
        .createSectionComponent("node2", leaf3, leaf4);

    final Section root = TestSectionCreator.createSectionComponent("root", node, node1);

    TestSectionCreator.createTree(root, mSectionContext);

    final ChangeSetState changeSetState =
        ChangeSetState.generateChangeSet(
            mSectionContext,
            null,
            root,
            mSectionComponentLogger,
            mSectionTreeTag,
            mCurrentPrefix,
            mNextPrefix);

    final ChangeSet changeSet = changeSetState.getChangeSet();

    assertEquals(changeSet.getChangeCount(), 9);
    assertEquals(changeSet.getCount(), 9);
    assertEquals(leaf1.getCount(), 3);
    assertEquals(leaf2.getCount(), 2);
    assertEquals(node.getCount(), 5);
    assertEquals(leaf3.getCount(), 2);
    assertEquals(leaf4.getCount(), 2);
    assertEquals(node1.getCount(), 4);
    assertEquals(root.getCount(), 9);

    for (int i = 0, size = changeSet.getChangeCount(); i< size; i++) {
      assertEquals(i, changeSet.getChangeAt(i).getIndex());
    }
  }

  @Test
  public void testRecreateSameTree() {
    final Section leaf1 = TestSectionCreator.createChangeSetComponent(
        "leaf1",
        Change.insert(0, ComponentRenderInfo.createEmpty()),
        Change.insert(1, ComponentRenderInfo.createEmpty()),
        Change.insert(2, ComponentRenderInfo.createEmpty()));

    final Section leaf2 = TestSectionCreator.createChangeSetComponent(
        "leaf2",
        Change.insert(0, ComponentRenderInfo.createEmpty()),
        Change.insert(1, ComponentRenderInfo.createEmpty()));

    final Section root = TestSectionCreator
        .createSectionComponent("node1", leaf1, leaf2);
    TestSectionCreator.createTree(root, mSectionContext);

    final ChangeSetState changeSetState =
        ChangeSetState.generateChangeSet(
            mSectionContext,
            null,
            root,
            mSectionComponentLogger,
            mSectionTreeTag,
            mCurrentPrefix,
            mNextPrefix);

    final ChangeSet changeSet = changeSetState.getChangeSet();

    assertEquals(changeSet.getChangeCount(), 5);
    assertEquals(changeSet.getCount(), 5);
    assertEquals(leaf1.getCount(), 3);
    assertEquals(leaf2.getCount(), 2);
    assertEquals(root.getCount(), 5);

    final ChangeSetState secondChangeSetState =
        ChangeSetState.generateChangeSet(
            mSectionContext,
            root,
            root,
            mSectionComponentLogger,
            mSectionTreeTag,
            mCurrentPrefix,
            mNextPrefix);

    final ChangeSet secondChangeSet = secondChangeSetState.getChangeSet();

    assertEquals(secondChangeSet.getChangeCount(), 0);
    assertEquals(secondChangeSet.getCount(), 5);
    assertEquals(leaf1.getCount(), 3);
    assertEquals(leaf2.getCount(), 2);
    assertEquals(root.getCount(), 5);
  }

  @Test
  public void testAddComponent() {
    final Section leaf1 = TestSectionCreator.createChangeSetComponent(
        "leaf1",
        Change.insert(0, ComponentRenderInfo.createEmpty()),
        Change.insert(1, ComponentRenderInfo.createEmpty()),
        Change.insert(2, ComponentRenderInfo.createEmpty()));

    final Section root = TestSectionCreator.createSectionComponent("node1", leaf1);
    TestSectionCreator.createTree(root, mSectionContext);

    final ChangeSetState changeSetState =
        ChangeSetState.generateChangeSet(
            mSectionContext,
            null,
            root,
            mSectionComponentLogger,
            mSectionTreeTag,
            mCurrentPrefix,
            mNextPrefix);

    changeSetState.getChangeSet();

    final Section leaf2 = TestSectionCreator.createChangeSetComponent(
        "leaf2",
        Change.insert(0, ComponentRenderInfo.createEmpty()),
        Change.insert(1, ComponentRenderInfo.createEmpty()));

    final Section newRoot = TestSectionCreator
        .createSectionComponent("node1", leaf1, leaf2);
    TestSectionCreator.createTree(newRoot, mSectionContext);

    final ChangeSetState secondChangeSetState =
        ChangeSetState.generateChangeSet(
            mSectionContext,
            root,
            newRoot,
            mSectionComponentLogger,
            mSectionTreeTag,
            mCurrentPrefix,
            mNextPrefix);

    final ChangeSet secondChangeSet = secondChangeSetState.getChangeSet();

    assertEquals(secondChangeSet.getChangeCount(), 2);
    assertEquals(secondChangeSet.getCount(), 5);
    assertEquals(leaf1.getCount(), 3);
    assertEquals(leaf2.getCount(), 2);
    assertEquals(newRoot.getCount(), 5);
  }

  @Test
  public void testUpdateComponent() {
    final Section leaf1 = TestSectionCreator.createChangeSetComponent(
        "leaf1",
        Change.insert(0, ComponentRenderInfo.createEmpty()),
        Change.insert(1, ComponentRenderInfo.createEmpty()));

    final Section root = TestSectionCreator.createSectionComponent("node1", leaf1);
    TestSectionCreator.createTree(root, mSectionContext);

    ChangeSetState.generateChangeSet(
        mSectionContext,
        null,
        root,
        mSectionComponentLogger,
        mSectionTreeTag,
        mCurrentPrefix,
        mNextPrefix);

    final Section newleaf1 = TestSectionCreator.createChangeSetComponent(
        "leaf1",
        Change.update(0, ComponentRenderInfo.createEmpty()),
        Change.update(1, ComponentRenderInfo.createEmpty()));

    final Section newRoot = TestSectionCreator
        .createSectionComponent("node1", newleaf1);
    TestSectionCreator.createTree(newRoot, mSectionContext);

    final ChangeSetState secondChangeSetState =
        ChangeSetState.generateChangeSet(
            mSectionContext,
            root,
            newRoot,
            mSectionComponentLogger,
            mSectionTreeTag,
            mCurrentPrefix,
            mNextPrefix);

    final ChangeSet secondChangeSet = secondChangeSetState.getChangeSet();

    assertEquals(secondChangeSet.getChangeCount(), 2);
    assertEquals(newleaf1.getCount(), 2);
    assertEquals(newRoot.getCount(), 2);
    assertEquals(secondChangeSet.getCount(), 2);
  }

  @Test
  public void testRemoveComponent() {
    final Section leaf1 = TestSectionCreator.createChangeSetComponent(
        "leaf1",
        Change.insert(0, ComponentRenderInfo.createEmpty()),
        Change.insert(1, ComponentRenderInfo.createEmpty()),
        Change.insert(2, ComponentRenderInfo.createEmpty()));

    final Section leaf2 = TestSectionCreator.createChangeSetComponent(
        "leaf2",
        Change.insert(0, ComponentRenderInfo.createEmpty()),
        Change.insert(1, ComponentRenderInfo.createEmpty()));

    final Section root = TestSectionCreator
        .createSectionComponent("node1", leaf1, leaf2);
    TestSectionCreator.createTree(root, mSectionContext);

    ChangeSetState.generateChangeSet(
        mSectionContext,
        null,
        root,
        mSectionComponentLogger,
        mSectionTreeTag,
        mCurrentPrefix,
        mNextPrefix);

    final Section newRoot = TestSectionCreator
        .createSectionComponent("node1", leaf1);
    TestSectionCreator.createTree(newRoot, mSectionContext);

    final ChangeSetState secondChangeSetState =
        ChangeSetState.generateChangeSet(
            mSectionContext,
            root,
            newRoot,
            mSectionComponentLogger,
            mSectionTreeTag,
            mCurrentPrefix,
            mNextPrefix);

    final ChangeSet secondChangeSet = secondChangeSetState.getChangeSet();

    assertEquals(secondChangeSet.getChangeCount(), 2);
    assertEquals(secondChangeSet.getCount(), 3);
    assertEquals(leaf1.getCount(), 3);
    assertEquals(newRoot.getCount(), 3);
    assertEquals(secondChangeSetState.getRemovedComponents().size(), 1);
    assertEquals(secondChangeSetState.getRemovedComponents().get(0), leaf2);
  }

  @Test
  public void testReverseComponents() {
    final int numChildren1 = 3;
    final Section leaf1 = createChangeSetComponent("leaf1", numChildren1);

    final int numChildren2 = 2;
    final Section leaf2 = createChangeSetComponent("leaf2", numChildren2);

    final int numChildren3 = 2;
    final Section leaf3 = createChangeSetComponent("leaf3", numChildren3);

    final int numChildren4 = 2;
    final Section leaf4 = createChangeSetComponent("leaf4", numChildren4);

    final int totalNumChildren = numChildren1 + numChildren2 + numChildren3 + numChildren4;

    final Section root = TestSectionCreator
        .createSectionComponent("node1", leaf1, leaf2, leaf3, leaf4);
    TestSectionCreator.createTree(root, mSectionContext);

    ChangeSetState.generateChangeSet(
        mSectionContext,
        null,
        root,
        mSectionComponentLogger,
        mSectionTreeTag,
        mCurrentPrefix,
        mNextPrefix);

    final Section newRoot = TestSectionCreator
        .createSectionComponent("node1", leaf4, leaf3, leaf2, leaf1);
    TestSectionCreator.createTree(newRoot, mSectionContext);

    final ChangeSetState secondChangeSetState =
        ChangeSetState.generateChangeSet(
            mSectionContext,
            root,
            newRoot,
            mSectionComponentLogger,
            mSectionTreeTag,
            mCurrentPrefix,
            mNextPrefix);

    final ChangeSet secondChangeSet = secondChangeSetState.getChangeSet();

    assertEquals(
        numChildren3 + numChildren2 + numChildren1,
        secondChangeSet.getChangeCount());
    assertEquals(totalNumChildren, secondChangeSet.getCount());
    assertEquals(totalNumChildren, newRoot.getCount());

    int changeIndex = 0;

    for (int i = 0; i < numChildren3; i++) {
      assertEquals(
          numChildren1 + numChildren2,
          secondChangeSet.getChangeAt(changeIndex).getIndex());
      assertEquals(
          totalNumChildren - 1,
          secondChangeSet.getChangeAt(changeIndex++).getToIndex());
    }

    for (int i = 0; i < numChildren2; i++) {
      assertEquals(numChildren1, secondChangeSet.getChangeAt(changeIndex).getIndex());
      assertEquals(
          totalNumChildren - 1,
          secondChangeSet.getChangeAt(changeIndex++).getToIndex());
    }

    for (int i = 0; i < numChildren1; i++) {
      assertEquals(
          0,
          secondChangeSet.getChangeAt(changeIndex).getIndex());
      assertEquals(
          totalNumChildren - 1,
          secondChangeSet.getChangeAt(changeIndex++).getToIndex());
    }
  }

  @Test
  public void testMoveAfter() {
    final int numChildren1 = 1;
    final Section leaf1 = createChangeSetComponent("leaf1", numChildren1);

    final int numChildren2 = 1;
    final Section leaf2 = createChangeSetComponent("leaf2", numChildren2);

    final int numChildren3 = 1;
    final Section leaf3 = createChangeSetComponent("leaf3", numChildren3);

    final int numChildren4 = 1;
    final Section leaf4 = createChangeSetComponent("leaf4", numChildren4);

    final int numChildren5 = 1;
    final Section leaf5 = createChangeSetComponent("leaf5", numChildren5);

    final int numChildren6 = 1;
    final Section leaf6 = createChangeSetComponent("leaf6", numChildren6);

    final Section root = TestSectionCreator
        .createSectionComponent("node1", leaf1, leaf2, leaf4, leaf5);
    TestSectionCreator.createTree(root, mSectionContext);

    ChangeSetState.generateChangeSet(
        mSectionContext,
        null,
        root,
        mSectionComponentLogger,
        mSectionTreeTag,
        mCurrentPrefix,
        mNextPrefix);

    final Section newRoot = TestSectionCreator
        .createSectionComponent("node1", leaf3, leaf4, leaf6, leaf5, leaf1);
    TestSectionCreator.createTree(newRoot, mSectionContext);

    final ChangeSetState secondChangeSetState =
        ChangeSetState.generateChangeSet(
            mSectionContext,
            root,
            newRoot,
            mSectionComponentLogger,
            mSectionTreeTag,
            mCurrentPrefix,
            mNextPrefix);

    final ChangeSet secondChangeSet = secondChangeSetState.getChangeSet();

    assertEquals(4, secondChangeSet.getChangeCount());
    assertEquals(5, secondChangeSet.getCount());
    assertEquals(5, newRoot.getCount());

    final Change move = secondChangeSet.getChangeAt(0);
    assertEquals(Change.MOVE, move.getType());
    assertEquals(0, move.getIndex());
    assertEquals(3, move.getToIndex());
  }

  @Test
  public void testNoMove() {
    final int numChildren1 = 1;
    final Section leaf1 = createChangeSetComponent("leaf1", numChildren1);

    final int numChildren2 = 1;
    final Section leaf2 = createChangeSetComponent("leaf2", numChildren2);

    final int numChildren3 = 1;
    final Section leaf3 = createChangeSetComponent("leaf3", numChildren3);

    final int numChildren4 = 1;
    final Section leaf4 = createChangeSetComponent("leaf4", numChildren4);

    final int numChildren5 = 1;
    final Section leaf5 = createChangeSetComponent("leaf5", numChildren5);

    final Section root = TestSectionCreator
        .createSectionComponent("node1", leaf1, leaf2, leaf3);
    TestSectionCreator.createTree(root, mSectionContext);

    ChangeSetState.generateChangeSet(
        mSectionContext,
        null,
        root,
        mSectionComponentLogger,
        mSectionTreeTag,
        mCurrentPrefix,
        mNextPrefix);

    final Section newRoot = TestSectionCreator
        .createSectionComponent("node1", leaf2, leaf4, leaf5, leaf3);
    TestSectionCreator.createTree(newRoot, mSectionContext);

    final ChangeSetState secondChangeSetState =
        ChangeSetState.generateChangeSet(
            mSectionContext,
            root,
            newRoot,
            mSectionComponentLogger,
            mSectionTreeTag,
            mCurrentPrefix,
            mNextPrefix);

    final ChangeSet secondChangeSet = secondChangeSetState.getChangeSet();

    for (int i = 0; i < secondChangeSet.getChangeCount(); i++) {
      assertTrue(secondChangeSet.getChangeAt(i).getType() != Change.MOVE);
    }
  }

  @Test
  public void testSwapComponents() {
    final int numChildren1 = 3;
    final Section leaf1 = createChangeSetComponent("leaf1", numChildren1);

    final int numChildren2 = 2;
    final Section leaf2 = createChangeSetComponent("leaf2", numChildren2);

    final int numChildren3 = 2;
    final Section leaf3 = createChangeSetComponent("leaf3", numChildren3);

    final int numChildren4 = 2;
    final Section leaf4 = createChangeSetComponent("leaf4", numChildren4);

    final int totalNumChildren = numChildren1 + numChildren2 + numChildren3 + numChildren4;

    final Section root = TestSectionCreator
        .createSectionComponent("node1", leaf1, leaf2, leaf3, leaf4);
    TestSectionCreator.createTree(root, mSectionContext);

    ChangeSetState.generateChangeSet(
        mSectionContext,
        null,
        root,
        mSectionComponentLogger,
        mSectionTreeTag,
        mCurrentPrefix,
        mNextPrefix);

    final Section newRoot = TestSectionCreator
        .createSectionComponent("node1", leaf2, leaf1, leaf4, leaf3);
    TestSectionCreator.createTree(newRoot, mSectionContext);

    final ChangeSetState secondChangeSetState =
        ChangeSetState.generateChangeSet(
            mSectionContext,
            root,
            newRoot,
            mSectionComponentLogger,
            mSectionTreeTag,
            mCurrentPrefix,
            mNextPrefix);

    final ChangeSet secondChangeSet = secondChangeSetState.getChangeSet();

    assertEquals(numChildren1 + numChildren3, secondChangeSet.getChangeCount());
    assertEquals(totalNumChildren, secondChangeSet.getCount());
    assertEquals(totalNumChildren, newRoot.getCount());

    int changeIndex = 0;

    for (int i = 0; i < numChildren1; i++) {
      assertEquals(
          0,
          secondChangeSet.getChangeAt(changeIndex).getIndex());
      assertEquals(
          numChildren1 + numChildren2 - 1,
          secondChangeSet.getChangeAt(changeIndex++).getToIndex());
    }

    for (int i = 0; i < numChildren3; i++) {
      assertEquals(
          numChildren1 + numChildren2,
          secondChangeSet.getChangeAt(changeIndex).getIndex());
      assertEquals(
          totalNumChildren - 1,
          secondChangeSet.getChangeAt(changeIndex++).getToIndex());
    }
  }

  @Test
  public void testMoveAndRemove() {
    final int numChildren1 = 3;
    final Section leaf1 = createChangeSetComponent("leaf1", numChildren1);

    final int numChildren2 = 2;
    final Section leaf2 = createChangeSetComponent("leaf2", numChildren2);

    final int numChildren3 = 2;
    final Section leaf3 = createChangeSetComponent("leaf3", numChildren3);

    final int totalNumChildren = numChildren1 + numChildren2 + numChildren3;

    final Section root = TestSectionCreator
        .createSectionComponent("node1", leaf1, leaf2, leaf3);
    TestSectionCreator.createTree(root, mSectionContext);

    ChangeSetState.generateChangeSet(
        mSectionContext,
        null,
        root,
        mSectionComponentLogger,
        mSectionTreeTag,
        mCurrentPrefix,
        mNextPrefix);

    final Section newRoot = TestSectionCreator
        .createSectionComponent("node1", leaf2, leaf1);
    TestSectionCreator.createTree(newRoot, mSectionContext);

    final ChangeSetState secondChangeSetState =
        ChangeSetState.generateChangeSet(
            mSectionContext,
            root,
            newRoot,
            mSectionComponentLogger,
            mSectionTreeTag,
            mCurrentPrefix,
            mNextPrefix);

    final ChangeSet secondChangeSet = secondChangeSetState.getChangeSet();

    assertEquals(numChildren1 + numChildren3, secondChangeSet.getChangeCount());
    assertEquals(totalNumChildren - numChildren3, secondChangeSet.getCount());
    assertEquals(1, secondChangeSetState.getRemovedComponents().size());
    assertEquals(leaf3, secondChangeSetState.getRemovedComponents().get(0));

    assertEquals(totalNumChildren - numChildren3, newRoot.getCount());

    int changeIndex = 0;

    for (int i = 0; i < numChildren1; i++) {
      assertEquals(0, secondChangeSet.getChangeAt(changeIndex).getIndex());
      assertEquals(
          numChildren1 + numChildren2 - 1,
          secondChangeSet.getChangeAt(changeIndex++).getToIndex());
    }
  }

  @Test
  public void testMoveAndInsert() {
    final int numChildren1 = 3;
    final Section leaf1 = createChangeSetComponent("leaf1", numChildren1);

    final int numChildren2 = 2;
    final Section leaf2 = createChangeSetComponent("leaf2", numChildren2);

    final int numChildren3 = 2;
    final Section leaf3 = createChangeSetComponent("leaf3", numChildren3);

    final int totalNumChildren = numChildren1 + numChildren2 + numChildren3;

    final Section root = TestSectionCreator
        .createSectionComponent("node1", leaf1, leaf2);
    TestSectionCreator.createTree(root, mSectionContext);

    ChangeSetState.generateChangeSet(
        mSectionContext,
        null,
        root,
        mSectionComponentLogger,
        mSectionTreeTag,
        mCurrentPrefix,
        mNextPrefix);

    final Section newRoot = TestSectionCreator
        .createSectionComponent("node1", leaf2, leaf3, leaf1);
    TestSectionCreator.createTree(newRoot, mSectionContext);

    final ChangeSetState secondChangeSetState =
        ChangeSetState.generateChangeSet(
            mSectionContext,
            root,
            newRoot,
            mSectionComponentLogger,
            mSectionTreeTag,
            mCurrentPrefix,
            mNextPrefix);

    final ChangeSet secondChangeSet = secondChangeSetState.getChangeSet();

    assertEquals(numChildren1 + numChildren3, secondChangeSet.getChangeCount());
    assertEquals(totalNumChildren, secondChangeSet.getCount());

    int changeIndex = 0;

    for (int i = 0; i < numChildren1; i++) {
      assertEquals(0, secondChangeSet.getChangeAt(changeIndex).getIndex());
      assertEquals(
          numChildren1 + numChildren2 - 1,
          secondChangeSet.getChangeAt(changeIndex++).getToIndex());
    }
  }

  @Test
  public void testMoveRemoveAndInsert() {
    final int numChildren1 = 3;
    final Section leaf1 = createChangeSetComponent("leaf1", numChildren1);

    final int numChildren2 = 2;
    final Section leaf2 = createChangeSetComponent("leaf2", numChildren2);

    final int numChildren3 = 2;
    final Section leaf3 = createChangeSetComponent("leaf3", numChildren3);

    final int numChildren4 = 2;
    final Section leaf4 = createChangeSetComponent("leaf4", numChildren4);

    final Section root = TestSectionCreator
        .createSectionComponent("node1", leaf1, leaf2, leaf3);
    TestSectionCreator.createTree(root, mSectionContext);

    ChangeSetState.generateChangeSet(
        mSectionContext,
        null,
        root,
        mSectionComponentLogger,
        mSectionTreeTag,
        mCurrentPrefix,
        mNextPrefix);

    final Section newRoot = TestSectionCreator
        .createSectionComponent("node1", leaf2, leaf1, leaf4);
    TestSectionCreator.createTree(newRoot, mSectionContext);

    final ChangeSetState secondChangeSetState =
        ChangeSetState.generateChangeSet(
            mSectionContext,
            root,
            newRoot,
            mSectionComponentLogger,
            mSectionTreeTag,
            mCurrentPrefix,
            mNextPrefix);

    final ChangeSet secondChangeSet = secondChangeSetState.getChangeSet();

    assertEquals(
        numChildren1 + numChildren3 + numChildren4,
        secondChangeSet.getChangeCount());
    assertEquals(numChildren1 + numChildren2 + numChildren4, secondChangeSet.getCount());

    int changeIndex = 0;

    for (int i = 0; i < numChildren1; i++) {
      assertEquals(0, secondChangeSet.getChangeAt(changeIndex).getIndex());
      assertEquals(
          numChildren1 + numChildren2 - 1,
          secondChangeSet.getChangeAt(changeIndex++).getToIndex());
    }

    assertEquals(1, secondChangeSetState.getRemovedComponents().size());
    assertEquals(leaf3, secondChangeSetState.getRemovedComponents().get(0));
  }

  private static Section createChangeSetComponent(String key, int numChildren) {
    Change[] changes = new Change[numChildren];
    for (int i = 0; i < numChildren; i++) {
      changes[i] = Change.insert(i, ComponentRenderInfo.createEmpty());
    }

    return TestSectionCreator.createChangeSetComponent(key, changes);
  }
}
