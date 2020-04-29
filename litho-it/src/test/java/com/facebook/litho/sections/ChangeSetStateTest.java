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

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.sections.Change.MOVE;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.facebook.litho.sections.logger.SectionsDebugLogger;
import com.facebook.litho.testing.sections.TestSectionCreator;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.ComponentRenderInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests {@link ChangeSetState} */
@RunWith(ComponentsTestRunner.class)
public class ChangeSetStateTest {
  private SectionContext mSectionContext;
  private SectionsDebugLogger mSectionsDebugLogger;
  private String mSectionTreeTag;
  private String mCurrentPrefix;
  private String mNextPrefix;

  @Before
  public void setup() {
    mSectionContext = new SectionContext(getApplicationContext());
    mSectionsDebugLogger = mock(SectionsDebugLogger.class);
    mSectionTreeTag = "";
    mCurrentPrefix = "";
    mNextPrefix = "";
  }

  @Test
  public void testNewChangeSetGeneration() {
    final Section leaf1 =
        TestSectionCreator.createChangeSetComponent(
            "leaf1",
            Change.insert(0, ComponentRenderInfo.createEmpty()),
            Change.insert(1, ComponentRenderInfo.createEmpty()),
            Change.insert(2, ComponentRenderInfo.createEmpty()));

    final Section leaf2 =
        TestSectionCreator.createChangeSetComponent(
            "leaf2",
            Change.insert(0, ComponentRenderInfo.createEmpty()),
            Change.insert(1, ComponentRenderInfo.createEmpty()));

    final Section node = TestSectionCreator.createSectionComponent("node1", leaf1, leaf2);

    final Section leaf3 =
        TestSectionCreator.createChangeSetComponent(
            "leaf3",
            Change.insert(0, ComponentRenderInfo.createEmpty()),
            Change.insert(1, ComponentRenderInfo.createEmpty()));

    final Section leaf4 =
        TestSectionCreator.createChangeSetComponent(
            "leaf4",
            Change.insert(0, ComponentRenderInfo.createEmpty()),
            Change.insert(1, ComponentRenderInfo.createEmpty()));

    final Section node1 = TestSectionCreator.createSectionComponent("node2", leaf3, leaf4);

    final Section root = TestSectionCreator.createSectionComponent("root", node, node1);

    TestSectionCreator.createTree(root, mSectionContext);

    final ChangeSetState changeSetState =
        ChangeSetState.generateChangeSet(
            mSectionContext,
            null,
            root,
            mSectionsDebugLogger,
            mSectionTreeTag,
            mCurrentPrefix,
            mNextPrefix,
            false);

    final ChangeSet changeSet = changeSetState.getChangeSet();

    assertThat(changeSet.getChangeCount()).isEqualTo(9);
    assertThat(changeSet.getCount()).isEqualTo(9);
    assertThat(leaf1.getCount()).isEqualTo(3);
    assertThat(leaf2.getCount()).isEqualTo(2);
    assertThat(node.getCount()).isEqualTo(5);
    assertThat(leaf3.getCount()).isEqualTo(2);
    assertThat(leaf4.getCount()).isEqualTo(2);
    assertThat(node1.getCount()).isEqualTo(4);
    assertThat(root.getCount()).isEqualTo(9);

    for (int i = 0, size = changeSet.getChangeCount(); i < size; i++) {
      assertThat(i).isEqualTo(changeSet.getChangeAt(i).getIndex());
    }
  }

  @Test
  public void testRecreateSameTree() {
    final Section leaf1 =
        TestSectionCreator.createChangeSetComponent(
            "leaf1",
            Change.insert(0, ComponentRenderInfo.createEmpty()),
            Change.insert(1, ComponentRenderInfo.createEmpty()),
            Change.insert(2, ComponentRenderInfo.createEmpty()));

    final Section leaf2 =
        TestSectionCreator.createChangeSetComponent(
            "leaf2",
            Change.insert(0, ComponentRenderInfo.createEmpty()),
            Change.insert(1, ComponentRenderInfo.createEmpty()));

    final Section root = TestSectionCreator.createSectionComponent("node1", leaf1, leaf2);
    TestSectionCreator.createTree(root, mSectionContext);

    final ChangeSetState changeSetState =
        ChangeSetState.generateChangeSet(
            mSectionContext,
            null,
            root,
            mSectionsDebugLogger,
            mSectionTreeTag,
            mCurrentPrefix,
            mNextPrefix,
            false);

    final ChangeSet changeSet = changeSetState.getChangeSet();

    assertThat(changeSet.getChangeCount()).isEqualTo(5);
    assertThat(changeSet.getCount()).isEqualTo(5);
    assertThat(leaf1.getCount()).isEqualTo(3);
    assertThat(leaf2.getCount()).isEqualTo(2);
    assertThat(root.getCount()).isEqualTo(5);

    final ChangeSetState secondChangeSetState =
        ChangeSetState.generateChangeSet(
            mSectionContext,
            root,
            root,
            mSectionsDebugLogger,
            mSectionTreeTag,
            mCurrentPrefix,
            mNextPrefix,
            false);

    final ChangeSet secondChangeSet = secondChangeSetState.getChangeSet();

    assertThat(secondChangeSet.getChangeCount()).isEqualTo(0);
    assertThat(secondChangeSet.getCount()).isEqualTo(5);
    assertThat(leaf1.getCount()).isEqualTo(3);
    assertThat(leaf2.getCount()).isEqualTo(2);
    assertThat(root.getCount()).isEqualTo(5);
  }

  @Test
  public void testAddComponent() {
    final Section leaf1 =
        TestSectionCreator.createChangeSetComponent(
            "leaf1",
            Change.insert(0, ComponentRenderInfo.createEmpty()),
            Change.insert(1, ComponentRenderInfo.createEmpty()),
            Change.insert(2, ComponentRenderInfo.createEmpty()));

    final Section root = TestSectionCreator.createSectionComponent("node1", true, leaf1);
    TestSectionCreator.createTree(root, mSectionContext);

    final ChangeSetState changeSetState =
        ChangeSetState.generateChangeSet(
            mSectionContext,
            null,
            root,
            mSectionsDebugLogger,
            mSectionTreeTag,
            mCurrentPrefix,
            mNextPrefix,
            false);

    changeSetState.getChangeSet();

    final Section leaf2 =
        TestSectionCreator.createChangeSetComponent(
            "leaf2",
            Change.insert(0, ComponentRenderInfo.createEmpty()),
            Change.insert(1, ComponentRenderInfo.createEmpty()));

    final Section newRoot = TestSectionCreator.createSectionComponent("node1", true, leaf1, leaf2);
    TestSectionCreator.createTree(newRoot, mSectionContext);

    final ChangeSetState secondChangeSetState =
        ChangeSetState.generateChangeSet(
            mSectionContext,
            root,
            newRoot,
            mSectionsDebugLogger,
            mSectionTreeTag,
            mCurrentPrefix,
            mNextPrefix,
            false);

    final ChangeSet secondChangeSet = secondChangeSetState.getChangeSet();

    assertThat(secondChangeSet.getChangeCount()).isEqualTo(2);
    assertThat(secondChangeSet.getCount()).isEqualTo(5);
    assertThat(leaf1.getCount()).isEqualTo(3);
    assertThat(leaf2.getCount()).isEqualTo(2);
    assertThat(newRoot.getCount()).isEqualTo(5);
  }

  @Test
  public void testUpdateComponent() {
    final Section leaf1 =
        TestSectionCreator.createChangeSetComponent(
            "leaf1",
            true,
            Change.insert(0, ComponentRenderInfo.createEmpty()),
            Change.insert(1, ComponentRenderInfo.createEmpty()));

    final Section root = TestSectionCreator.createSectionComponent("node1", true, leaf1);
    TestSectionCreator.createTree(root, mSectionContext);

    ChangeSetState.generateChangeSet(
        mSectionContext,
        null,
        root,
        mSectionsDebugLogger,
        mSectionTreeTag,
        mCurrentPrefix,
        mNextPrefix,
        false);

    final Section newleaf1 =
        TestSectionCreator.createChangeSetComponent(
            "leaf1",
            Change.update(0, ComponentRenderInfo.createEmpty()),
            Change.update(1, ComponentRenderInfo.createEmpty()));

    final Section newRoot = TestSectionCreator.createSectionComponent("node1", newleaf1);
    TestSectionCreator.createTree(newRoot, mSectionContext);

    final ChangeSetState secondChangeSetState =
        ChangeSetState.generateChangeSet(
            mSectionContext,
            root,
            newRoot,
            mSectionsDebugLogger,
            mSectionTreeTag,
            mCurrentPrefix,
            mNextPrefix,
            false);

    final ChangeSet secondChangeSet = secondChangeSetState.getChangeSet();

    assertThat(secondChangeSet.getChangeCount()).isEqualTo(2);
    assertThat(newleaf1.getCount()).isEqualTo(2);
    assertThat(newRoot.getCount()).isEqualTo(2);
    assertThat(secondChangeSet.getCount()).isEqualTo(2);
  }

  @Test
  public void testRemoveComponent() {
    final Section leaf1 =
        TestSectionCreator.createChangeSetComponent(
            "leaf1",
            Change.insert(0, ComponentRenderInfo.createEmpty()),
            Change.insert(1, ComponentRenderInfo.createEmpty()),
            Change.insert(2, ComponentRenderInfo.createEmpty()));

    final Section leaf2 =
        TestSectionCreator.createChangeSetComponent(
            "leaf2",
            Change.insert(0, ComponentRenderInfo.createEmpty()),
            Change.insert(1, ComponentRenderInfo.createEmpty()));

    final Section root = TestSectionCreator.createSectionComponent("node1", true, leaf1, leaf2);
    TestSectionCreator.createTree(root, mSectionContext);

    ChangeSetState.generateChangeSet(
        mSectionContext,
        null,
        root,
        mSectionsDebugLogger,
        mSectionTreeTag,
        mCurrentPrefix,
        mNextPrefix,
        false);

    final Section newRoot = TestSectionCreator.createSectionComponent("node1", true, leaf1);
    TestSectionCreator.createTree(newRoot, mSectionContext);

    final ChangeSetState secondChangeSetState =
        ChangeSetState.generateChangeSet(
            mSectionContext,
            root,
            newRoot,
            mSectionsDebugLogger,
            mSectionTreeTag,
            mCurrentPrefix,
            mNextPrefix,
            false);

    final ChangeSet secondChangeSet = secondChangeSetState.getChangeSet();

    assertThat(secondChangeSet.getChangeCount()).isEqualTo(2);
    assertThat(secondChangeSet.getCount()).isEqualTo(3);
    assertThat(leaf1.getCount()).isEqualTo(3);
    assertThat(newRoot.getCount()).isEqualTo(3);
    assertThat(secondChangeSetState.getRemovedComponents().size()).isEqualTo(1);
    assertThat(secondChangeSetState.getRemovedComponents().get(0)).isEqualTo(leaf2);
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

    final Section root =
        TestSectionCreator.createSectionComponent("node1", true, leaf1, leaf2, leaf3, leaf4);
    TestSectionCreator.createTree(root, mSectionContext);

    ChangeSetState.generateChangeSet(
        mSectionContext,
        null,
        root,
        mSectionsDebugLogger,
        mSectionTreeTag,
        mCurrentPrefix,
        mNextPrefix,
        false);

    final Section newRoot =
        TestSectionCreator.createSectionComponent("node1", true, leaf4, leaf3, leaf2, leaf1);
    TestSectionCreator.createTree(newRoot, mSectionContext);

    final ChangeSetState secondChangeSetState =
        ChangeSetState.generateChangeSet(
            mSectionContext,
            root,
            newRoot,
            mSectionsDebugLogger,
            mSectionTreeTag,
            mCurrentPrefix,
            mNextPrefix,
            false);

    final ChangeSet secondChangeSet = secondChangeSetState.getChangeSet();

    assertThat(numChildren3 + numChildren2 + numChildren1)
        .isEqualTo(secondChangeSet.getChangeCount());
    assertThat(totalNumChildren).isEqualTo(secondChangeSet.getCount());
    assertThat(totalNumChildren).isEqualTo(newRoot.getCount());

    int changeIndex = 0;

    for (int i = 0; i < numChildren3; i++) {
      assertThat(numChildren1 + numChildren2)
          .isEqualTo(secondChangeSet.getChangeAt(changeIndex).getIndex());
      assertThat(totalNumChildren - 1)
          .isEqualTo(secondChangeSet.getChangeAt(changeIndex++).getToIndex());
    }

    for (int i = 0; i < numChildren2; i++) {
      assertThat(numChildren1).isEqualTo(secondChangeSet.getChangeAt(changeIndex).getIndex());
      assertThat(totalNumChildren - 1)
          .isEqualTo(secondChangeSet.getChangeAt(changeIndex++).getToIndex());
    }

    for (int i = 0; i < numChildren1; i++) {
      assertThat(0).isEqualTo(secondChangeSet.getChangeAt(changeIndex).getIndex());
      assertThat(totalNumChildren - 1)
          .isEqualTo(secondChangeSet.getChangeAt(changeIndex++).getToIndex());
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

    final Section root =
        TestSectionCreator.createSectionComponent("node1", true, leaf1, leaf2, leaf4, leaf5);
    TestSectionCreator.createTree(root, mSectionContext);

    ChangeSetState.generateChangeSet(
        mSectionContext,
        null,
        root,
        mSectionsDebugLogger,
        mSectionTreeTag,
        mCurrentPrefix,
        mNextPrefix,
        false);

    final Section newRoot =
        TestSectionCreator.createSectionComponent("node1", true, leaf3, leaf4, leaf6, leaf5, leaf1);
    TestSectionCreator.createTree(newRoot, mSectionContext);

    final ChangeSetState secondChangeSetState =
        ChangeSetState.generateChangeSet(
            mSectionContext,
            root,
            newRoot,
            mSectionsDebugLogger,
            mSectionTreeTag,
            mCurrentPrefix,
            mNextPrefix,
            false);

    final ChangeSet secondChangeSet = secondChangeSetState.getChangeSet();

    assertThat(4).isEqualTo(secondChangeSet.getChangeCount());
    assertThat(5).isEqualTo(secondChangeSet.getCount());
    assertThat(5).isEqualTo(newRoot.getCount());

    final Change move = secondChangeSet.getChangeAt(0);
    assertThat(MOVE).isEqualTo(move.getType());
    assertThat(0).isEqualTo(move.getIndex());
    assertThat(3).isEqualTo(move.getToIndex());
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

    final Section root = TestSectionCreator.createSectionComponent("node1", leaf1, leaf2, leaf3);
    TestSectionCreator.createTree(root, mSectionContext);

    ChangeSetState.generateChangeSet(
        mSectionContext,
        null,
        root,
        mSectionsDebugLogger,
        mSectionTreeTag,
        mCurrentPrefix,
        mNextPrefix,
        false);

    final Section newRoot =
        TestSectionCreator.createSectionComponent("node1", leaf2, leaf4, leaf5, leaf3);
    TestSectionCreator.createTree(newRoot, mSectionContext);

    final ChangeSetState secondChangeSetState =
        ChangeSetState.generateChangeSet(
            mSectionContext,
            root,
            newRoot,
            mSectionsDebugLogger,
            mSectionTreeTag,
            mCurrentPrefix,
            mNextPrefix,
            false);

    final ChangeSet secondChangeSet = secondChangeSetState.getChangeSet();

    for (int i = 0; i < secondChangeSet.getChangeCount(); i++) {
      assertThat(secondChangeSet.getChangeAt(i).getType() != MOVE).isTrue();
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

    final Section root =
        TestSectionCreator.createSectionComponent("node1", true, leaf1, leaf2, leaf3, leaf4);
    TestSectionCreator.createTree(root, mSectionContext);

    ChangeSetState.generateChangeSet(
        mSectionContext,
        null,
        root,
        mSectionsDebugLogger,
        mSectionTreeTag,
        mCurrentPrefix,
        mNextPrefix,
        false);

    final Section newRoot =
        TestSectionCreator.createSectionComponent("node1", true, leaf2, leaf1, leaf4, leaf3);
    TestSectionCreator.createTree(newRoot, mSectionContext);

    final ChangeSetState secondChangeSetState =
        ChangeSetState.generateChangeSet(
            mSectionContext,
            root,
            newRoot,
            mSectionsDebugLogger,
            mSectionTreeTag,
            mCurrentPrefix,
            mNextPrefix,
            false);

    final ChangeSet secondChangeSet = secondChangeSetState.getChangeSet();

    assertThat(numChildren1 + numChildren3).isEqualTo(secondChangeSet.getChangeCount());
    assertThat(totalNumChildren).isEqualTo(secondChangeSet.getCount());
    assertThat(totalNumChildren).isEqualTo(newRoot.getCount());

    int changeIndex = 0;

    for (int i = 0; i < numChildren1; i++) {
      assertThat(0).isEqualTo(secondChangeSet.getChangeAt(changeIndex).getIndex());
      assertThat(numChildren1 + numChildren2 - 1)
          .isEqualTo(secondChangeSet.getChangeAt(changeIndex++).getToIndex());
    }

    for (int i = 0; i < numChildren3; i++) {
      assertThat(numChildren1 + numChildren2)
          .isEqualTo(secondChangeSet.getChangeAt(changeIndex).getIndex());
      assertThat(totalNumChildren - 1)
          .isEqualTo(secondChangeSet.getChangeAt(changeIndex++).getToIndex());
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

    final Section root =
        TestSectionCreator.createSectionComponent("node1", true, leaf1, leaf2, leaf3);
    TestSectionCreator.createTree(root, mSectionContext);

    ChangeSetState.generateChangeSet(
        mSectionContext,
        null,
        root,
        mSectionsDebugLogger,
        mSectionTreeTag,
        mCurrentPrefix,
        mNextPrefix,
        false);

    final Section newRoot = TestSectionCreator.createSectionComponent("node1", true, leaf2, leaf1);
    TestSectionCreator.createTree(newRoot, mSectionContext);

    final ChangeSetState secondChangeSetState =
        ChangeSetState.generateChangeSet(
            mSectionContext,
            root,
            newRoot,
            mSectionsDebugLogger,
            mSectionTreeTag,
            mCurrentPrefix,
            mNextPrefix,
            false);

    final ChangeSet secondChangeSet = secondChangeSetState.getChangeSet();

    assertThat(numChildren1 + numChildren3).isEqualTo(secondChangeSet.getChangeCount());
    assertThat(totalNumChildren - numChildren3).isEqualTo(secondChangeSet.getCount());
    assertThat(1).isEqualTo(secondChangeSetState.getRemovedComponents().size());
    assertThat(leaf3).isEqualTo(secondChangeSetState.getRemovedComponents().get(0));

    assertThat(totalNumChildren - numChildren3).isEqualTo(newRoot.getCount());

    int changeIndex = 0;

    for (int i = 0; i < numChildren1; i++) {
      assertThat(0).isEqualTo(secondChangeSet.getChangeAt(changeIndex).getIndex());
      assertThat(numChildren1 + numChildren2 - 1)
          .isEqualTo(secondChangeSet.getChangeAt(changeIndex++).getToIndex());
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

    final Section root = TestSectionCreator.createSectionComponent("node1", true, leaf1, leaf2);
    TestSectionCreator.createTree(root, mSectionContext);

    ChangeSetState.generateChangeSet(
        mSectionContext,
        null,
        root,
        mSectionsDebugLogger,
        mSectionTreeTag,
        mCurrentPrefix,
        mNextPrefix,
        false);

    final Section newRoot =
        TestSectionCreator.createSectionComponent("node1", true, leaf2, leaf3, leaf1);
    TestSectionCreator.createTree(newRoot, mSectionContext);

    final ChangeSetState secondChangeSetState =
        ChangeSetState.generateChangeSet(
            mSectionContext,
            root,
            newRoot,
            mSectionsDebugLogger,
            mSectionTreeTag,
            mCurrentPrefix,
            mNextPrefix,
            false);

    final ChangeSet secondChangeSet = secondChangeSetState.getChangeSet();

    assertThat(numChildren1 + numChildren3).isEqualTo(secondChangeSet.getChangeCount());
    assertThat(totalNumChildren).isEqualTo(secondChangeSet.getCount());

    int changeIndex = 0;

    for (int i = 0; i < numChildren1; i++) {
      assertThat(0).isEqualTo(secondChangeSet.getChangeAt(changeIndex).getIndex());
      assertThat(numChildren1 + numChildren2 - 1)
          .isEqualTo(secondChangeSet.getChangeAt(changeIndex++).getToIndex());
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

    final Section root =
        TestSectionCreator.createSectionComponent("node1", true, leaf1, leaf2, leaf3);
    TestSectionCreator.createTree(root, mSectionContext);

    ChangeSetState.generateChangeSet(
        mSectionContext,
        null,
        root,
        mSectionsDebugLogger,
        mSectionTreeTag,
        mCurrentPrefix,
        mNextPrefix,
        false);

    final Section newRoot =
        TestSectionCreator.createSectionComponent("node1", true, leaf2, leaf1, leaf4);
    TestSectionCreator.createTree(newRoot, mSectionContext);

    final ChangeSetState secondChangeSetState =
        ChangeSetState.generateChangeSet(
            mSectionContext,
            root,
            newRoot,
            mSectionsDebugLogger,
            mSectionTreeTag,
            mCurrentPrefix,
            mNextPrefix,
            false);

    final ChangeSet secondChangeSet = secondChangeSetState.getChangeSet();

    assertThat(numChildren1 + numChildren3 + numChildren4)
        .isEqualTo(secondChangeSet.getChangeCount());
    assertThat(numChildren1 + numChildren2 + numChildren4).isEqualTo(secondChangeSet.getCount());

    int changeIndex = 0;

    for (int i = 0; i < numChildren1; i++) {
      assertThat(0).isEqualTo(secondChangeSet.getChangeAt(changeIndex).getIndex());
      assertThat(numChildren1 + numChildren2 - 1)
          .isEqualTo(secondChangeSet.getChangeAt(changeIndex++).getToIndex());
    }

    assertThat(1).isEqualTo(secondChangeSetState.getRemovedComponents().size());
    assertThat(leaf3).isEqualTo(secondChangeSetState.getRemovedComponents().get(0));
  }

  private static Section createChangeSetComponent(String key, int numChildren) {
    Change[] changes = new Change[numChildren];
    for (int i = 0; i < numChildren; i++) {
      changes[i] = Change.insert(i, ComponentRenderInfo.createEmpty());
    }

    return TestSectionCreator.createChangeSetComponent(key, changes);
  }
}
