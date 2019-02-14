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

package com.facebook.litho.sections;

import static com.facebook.litho.testing.sections.TestSectionCreator.TestSection;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

import android.os.Looper;
import com.facebook.litho.Component;
import com.facebook.litho.StateContainer;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.sections.TestSectionCreator;
import com.facebook.litho.testing.sections.TestTarget;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.ChangeSetCompleteCallback;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;
import com.facebook.litho.widget.SmoothScrollAlignmentType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

/** Tests {@link SectionTree} */
@RunWith(ComponentsTestRunner.class)
public class SectionTreeTest {

  private SectionContext mSectionContext;
  private ShadowLooper mChangeSetThreadShadowLooper;

  @Before
  public void setup() throws Exception {
    mSectionContext = new SectionContext(RuntimeEnvironment.application);
    mChangeSetThreadShadowLooper = Shadows.shadowOf(
        (Looper) Whitebox.invokeMethod(
            SectionTree.class,
            "getDefaultChangeSetThreadLooper"));
  }

  @After
  public void tearDown() {
    // If a test fails, make sure the shadow looper gets cleared out anyway so it doesn't impact
    // other tests.
    mChangeSetThreadShadowLooper.runToEndOfTasks();
  }

  @Test
  public void testSetRoot() {

    final Section section = TestSectionCreator.createChangeSetComponent(
        "leaf1",
        Change.insert(0, makeComponentInfo()));

    final TestTarget changeSetHandler = new TestTarget();
    SectionTree tree = SectionTree.create(mSectionContext, changeSetHandler)
        .build();

    tree.setRoot(section);
    assertChangeSetHandled(changeSetHandler);
  }

  @Test
  public void testSetSameRoot() {
    final Section section = TestSectionCreator.createChangeSetSection(
        0,
        "leaf1",
        true,
        Change.insert(0, makeComponentInfo()));

    final TestTarget changeSetHandler = new TestTarget();
    SectionTree tree = SectionTree.create(mSectionContext, changeSetHandler)
        .build();

    tree.setRoot(section);
    assertChangeSetHandled(changeSetHandler);

    changeSetHandler.clear();

    tree.setRoot(section.makeShallowCopy(false));
    assertChangeSetNotSeen(changeSetHandler);
  }

  @Test
  public void updateTree() {
    final Section section = TestSectionCreator.createChangeSetSection(
        0,
        "leaf1",
       true,
       Change.insert(0, makeComponentInfo()));

    final TestTarget changeSetHandler = new TestTarget();
    SectionTree tree = SectionTree.create(mSectionContext, changeSetHandler)
        .build();

    tree.setRoot(section);
    changeSetHandler.clear();
    tree.setRoot(section);

    assertChangeSetNotSeen(changeSetHandler);

    final Section secondSection = TestSectionCreator.createChangeSetSection(
        0,
        "leaf1",
        true,
        Change.update(0, makeComponentInfo()));
    tree.setRoot(secondSection);

    assertChangeSetHandled(changeSetHandler);
  }

  @Test
  public void testUniqueGlobalKeys() {
    final Section leaf1 = TestSectionCreator.createChangeSetComponent(
        "leaf1",
        Change.insert(0, ComponentRenderInfo.createEmpty()),
        Change.insert(1, ComponentRenderInfo.createEmpty()),
        Change.insert(2, ComponentRenderInfo.createEmpty()));

    final Section leaf2 = TestSectionCreator.createChangeSetComponent(
        "leaf1",
        Change.insert(0, ComponentRenderInfo.createEmpty()),
        Change.insert(1, ComponentRenderInfo.createEmpty()));

    final Section root = TestSectionCreator
        .createSectionComponent("node1", leaf1, leaf2);

    final TestTarget changeSetHandler = new TestTarget();

    SectionTree tree = SectionTree.create(mSectionContext, changeSetHandler)
        .build();

    tree.setRoot(root);

    assertThat(leaf1.getGlobalKey()).isEqualTo("node1leaf1");
    assertThat(leaf2.getGlobalKey()).isEqualTo("node1leaf10");
  }

  @Test
  public void testSetRootWithComplexTree() {
    final Section leaf1 = TestSectionCreator.createChangeSetComponent(
        "leaf1",
        Change.insert(0, makeComponentInfo()),
        Change.insert(1, makeComponentInfo()),
        Change.insert(2, makeComponentInfo()));

    final Section leaf2 = TestSectionCreator.createChangeSetComponent(
        "leaf2",
        Change.insert(0, makeComponentInfo()),
        Change.insert(1, makeComponentInfo()));

    final Section node = TestSectionCreator
        .createSectionComponent("node1", leaf1, leaf2);

    final Section leaf3 = TestSectionCreator.createChangeSetComponent(
        "leaf3",
        Change.insert(0, makeComponentInfo()),
        Change.insert(1, makeComponentInfo()));

    final Section leaf4 = TestSectionCreator.createChangeSetComponent(
        "leaf4",
        Change.insert(0, makeComponentInfo()),
        Change.insert(1, makeComponentInfo()));

    final Section node1 = TestSectionCreator
        .createSectionComponent("node2", leaf3, leaf4);

    final TestTarget changeSetHandler = new TestTarget();

    final Section root = TestSectionCreator.createSectionComponent("root", node, node1);
    SectionTree tree = SectionTree.create(mSectionContext, changeSetHandler)
        .build();

    tree.setRoot(root);

    assertChangeSetHandled(changeSetHandler);
    assertThat(changeSetHandler.getNumChanges()).isEqualTo(9);
  }

  @Test
  public void testSetRootOfDifferentType() {
    final Section leaf1 =
        TestSectionCreator.createChangeSetComponent(
            "leaf1", Change.insert(0, makeComponentInfo()), Change.insert(1, makeComponentInfo()));

    final Section leaf2 =
        TestSectionCreator.createChangeSetComponent("leaf2", Change.insert(0, makeComponentInfo()));

    final Section root1 = TestSectionCreator.createSectionComponent("node1", leaf1, leaf2);

    final Section root2 =
        TestSectionCreator.createChangeSetComponent("leaf3", Change.insert(0, makeComponentInfo()));

    final TestTarget changeSetHandler = new TestTarget();

    SectionTree tree = SectionTree.create(mSectionContext, changeSetHandler).build();

    tree.setRoot(root1);
    assertChangeSetHandled(changeSetHandler);
    assertThat(changeSetHandler.getNumChanges()).isEqualTo(3);

    tree.setRoot(root2);
    assertChangeSetHandled(changeSetHandler);
    assertThat(changeSetHandler.getNumChanges()).isEqualTo(7); // count is cumulative

    tree.setRoot(root1);
    assertChangeSetHandled(changeSetHandler);
    assertThat(changeSetHandler.getNumChanges()).isEqualTo(11); // count is cumulative
  }

  @Test
  public void testRefresh() {
    final Section leaf1 = TestSectionCreator.createChangeSetComponent(
        "leaf1");

    final Section leaf2 = TestSectionCreator.createChangeSetComponent(
        "leaf2");

    final Section node = TestSectionCreator
        .createSectionComponent("node1", leaf1, leaf2);

    final Section leaf3 = TestSectionCreator.createChangeSetComponent(
        "leaf3");

    final Section leaf4 = TestSectionCreator.createChangeSetComponent(
        "leaf4");

    final Section node1 = TestSectionCreator
        .createSectionComponent("node2", leaf3, leaf4);

    final TestTarget changeSetHandler = new TestTarget();

    final Section root = TestSectionCreator.createSectionComponent("root", node, node1);
    SectionTree tree = SectionTree.create(mSectionContext, changeSetHandler)
        .build();

    tree.setRoot(root);

    tree.refresh();

    assertThat(((TestSection) leaf1).refreshCalled).isTrue();
    assertThat(((TestSection) leaf2).refreshCalled).isTrue();
    assertThat(((TestSection) node).refreshCalled).isTrue();
    assertThat(((TestSection) leaf3).refreshCalled).isTrue();
    assertThat(((TestSection) leaf4).refreshCalled).isTrue();
    assertThat(((TestSection) node1).refreshCalled).isTrue();

    assertChangeSetNotSeen(changeSetHandler);
  }

  @Test
  public void testRefreshWithEmptyTree() {
    final TestTarget changeSetHandler = new TestTarget();
    final SectionTree tree = SectionTree.create(mSectionContext, changeSetHandler)
        .build();

    tree.refresh();
  }

  @Test
  public void testViewPortChanged() {
    final Section leaf1 = TestSectionCreator.createChangeSetComponent(
        "leaf1",
        Change.insert(0, makeComponentInfo()),
        Change.insert(1, makeComponentInfo()),
        Change.insert(2, makeComponentInfo()));

    final Section leaf2 = TestSectionCreator.createChangeSetComponent(
        "leaf2",
        Change.insert(0, makeComponentInfo()),
        Change.insert(1, makeComponentInfo()));

    final Section node = TestSectionCreator
        .createSectionComponent("node1", leaf1, leaf2);

    final Section leaf3 = TestSectionCreator.createChangeSetComponent(
        "leaf3",
        Change.insert(0, makeComponentInfo()));

    final Section leaf4 = TestSectionCreator.createChangeSetComponent(
        "leaf4",
        Change.insert(0, makeComponentInfo()),
        Change.insert(1, makeComponentInfo()),
        Change.insert(2, makeComponentInfo()),
        Change.insert(3, makeComponentInfo()));

    final Section node1 = TestSectionCreator
        .createSectionComponent("node2", leaf3, leaf4);

    final TestTarget changeSetHandler = new TestTarget();

    final Section root = TestSectionCreator.createSectionComponent("root", node, node1);
    SectionTree tree = SectionTree.create(mSectionContext, changeSetHandler)
        .build();

    tree.setRoot(root);

    tree.viewPortChangedFromScrolling(3, 9, 3, 9);
    assertThat(((TestSection) leaf1).firstVisibleIndex).isEqualTo(-1);
    assertThat(((TestSection) leaf1).lastVisibleIndex).isEqualTo(-1);
    assertThat(((TestSection) leaf1).firstFullyVisibleIndex).isEqualTo(-1);
    assertThat(((TestSection) leaf1).lastFullyVisibleIndex).isEqualTo(-1);

    assertThat(((TestSection) leaf2).firstVisibleIndex).isEqualTo(0);
    assertThat(((TestSection) leaf2).lastVisibleIndex).isEqualTo(1);
    assertThat(((TestSection) leaf2).firstFullyVisibleIndex).isEqualTo(0);
    assertThat(((TestSection) leaf2).lastFullyVisibleIndex).isEqualTo(1);

    assertThat(((TestSection) leaf3).firstVisibleIndex).isEqualTo(0);
    assertThat(((TestSection) leaf3).lastVisibleIndex).isEqualTo(0);
    assertThat(((TestSection) leaf3).firstFullyVisibleIndex).isEqualTo(0);
    assertThat(((TestSection) leaf3).lastFullyVisibleIndex).isEqualTo(0);

    assertThat(((TestSection) leaf4).firstVisibleIndex).isEqualTo(0);
    assertThat(((TestSection) leaf4).lastVisibleIndex).isEqualTo(3);
    assertThat(((TestSection) leaf4).firstFullyVisibleIndex).isEqualTo(0);
    assertThat(((TestSection) leaf4).lastFullyVisibleIndex).isEqualTo(3);

    assertThat(((TestSection) node).firstVisibleIndex).isEqualTo(3);
    assertThat(((TestSection) node).lastVisibleIndex).isEqualTo(4);
    assertThat(((TestSection) node).firstFullyVisibleIndex).isEqualTo(3);
    assertThat(((TestSection) node).lastFullyVisibleIndex).isEqualTo(4);

    assertThat(((TestSection) node1).firstVisibleIndex).isEqualTo(0);
    assertThat(((TestSection) node1).lastVisibleIndex).isEqualTo(4);
    assertThat(((TestSection) node1).firstFullyVisibleIndex).isEqualTo(0);
    assertThat(((TestSection) node1).lastFullyVisibleIndex).isEqualTo(4);

    ((TestSectionCreator.TestSection) leaf1).firstVisibleIndex = 0;
    ((TestSectionCreator.TestSection) leaf1).lastVisibleIndex = 0;
    ((TestSectionCreator.TestSection) leaf1).firstFullyVisibleIndex = 0;
    ((TestSectionCreator.TestSection) leaf1).lastFullyVisibleIndex = 0;

    tree.viewPortChangedFromScrolling(3, 9, 3, 9);

    assertThat(((TestSection) leaf1).firstVisibleIndex).isEqualTo(0);
    assertThat(((TestSection) leaf1).lastVisibleIndex).isEqualTo(0);
    assertThat(((TestSection) leaf1).firstFullyVisibleIndex).isEqualTo(0);
    assertThat(((TestSection) leaf1).lastFullyVisibleIndex).isEqualTo(0);

    tree.viewPortChangedFromScrolling(6, 9, 7, 9);

    assertThat(((TestSection) leaf4).firstVisibleIndex).isEqualTo(0);
    assertThat(((TestSection) leaf4).lastVisibleIndex).isEqualTo(3);
    assertThat(((TestSection) leaf4).firstFullyVisibleIndex).isEqualTo(1);
    assertThat(((TestSection) leaf4).lastFullyVisibleIndex).isEqualTo(3);
  }

  @Test
  public void testStateUpdate() {
    final Section section = TestSectionCreator.createChangeSetComponent(
        "leaf1",
        Change.insert(0, makeComponentInfo()));
    section.setKey("key");

    final TestTarget changeSetHandler = new TestTarget();
    SectionTree tree = SectionTree.create(mSectionContext, changeSetHandler)
        .build();

    tree.setRoot(section);
    assertChangeSetHandled(changeSetHandler);

    final StateUpdate stateUpdate = new StateUpdate();
    changeSetHandler.clear();
    tree.updateState("key", stateUpdate, "test");

    assertThat(stateUpdate.mUpdateStateCalled).isTrue();
    assertChangeSetNotSeen(changeSetHandler);
  }

  @Test
  public void testLazyStateUpdate() {
    final Section section = TestSectionCreator.createChangeSetComponent(
        "leaf1",
        Change.insert(0, makeComponentInfo()));
    section.setKey("key");

    final TestTarget changeSetHandler = new TestTarget();
    SectionTree tree = SectionTree.create(mSectionContext, changeSetHandler)
        .build();

    tree.setRoot(section);
    assertChangeSetHandled(changeSetHandler);

    final StateUpdate lazyStateUpdate = new StateUpdate();
    changeSetHandler.clear();
    tree.updateStateLazy("key", lazyStateUpdate);

    assertThat(lazyStateUpdate.mUpdateStateCalled).isFalse();

    final StateUpdate stateUpdate = new StateUpdate();
    tree.updateState("key", stateUpdate, "test");

    assertThat(lazyStateUpdate.mUpdateStateCalled).isTrue();
    assertThat(stateUpdate.mUpdateStateCalled).isTrue();
  }

  @Test
  public void testStateUpdateOnReleasedTree() {
    final Section section = TestSectionCreator.createChangeSetComponent(
        "leaf1",
        Change.insert(0, makeComponentInfo()));
    section.setKey("key");

    final TestTarget changeSetHandler = new TestTarget();
    SectionTree tree = SectionTree.create(mSectionContext, changeSetHandler)
        .build();

    tree.setRoot(section);
    assertChangeSetHandled(changeSetHandler);

    final StateUpdate stateUpdate = new StateUpdate();
    changeSetHandler.clear();
    tree.release();
    tree.updateState("key", stateUpdate, "test");

    assertThat(stateUpdate.mUpdateStateCalled).isFalse();
  }

  @Test
  public void testSetRootAsync() {
    final Section section = TestSectionCreator.createChangeSetComponent(
        "leaf1",
        Change.insert(0, makeComponentInfo()));
    final TestTarget changeSetHandler = new TestTarget();
    SectionTree tree = SectionTree.create(mSectionContext, changeSetHandler)
        .build();

    tree.setRootAsync(section);
    mChangeSetThreadShadowLooper.runOneTask();
    assertChangeSetHandled(changeSetHandler);
  }

  @Test
  public void testUpdateStateAsync() {
    final Section section = TestSectionCreator.createChangeSetComponent(
        "leaf1",
        Change.insert(0, makeComponentInfo()));
    section.setKey("key");

    final TestTarget changeSetHandler = new TestTarget();
    SectionTree tree = SectionTree.create(mSectionContext, changeSetHandler)
        .build();

    tree.setRoot(section);
    assertChangeSetHandled(changeSetHandler);

    final StateUpdate stateUpdate = new StateUpdate();
    changeSetHandler.clear();
    tree.updateStateAsync("key", stateUpdate, "test");
    assertThat(stateUpdate.mUpdateStateCalled).isFalse();

    mChangeSetThreadShadowLooper.runOneTask();

    assertThat(stateUpdate.mUpdateStateCalled).isTrue();
    assertChangeSetNotSeen(changeSetHandler);
  }

  @Test
  public void testUpdateStateAsyncButForceSyncUpdates() {
    final Section section =
        TestSectionCreator.createChangeSetComponent("leaf1", Change.insert(0, makeComponentInfo()));
    section.setKey("key");

    final TestTarget changeSetHandler = new TestTarget();
    SectionTree tree =
        SectionTree.create(mSectionContext, changeSetHandler).forceSyncStateUpdates(true).build();

    tree.setRoot(section);
    assertChangeSetHandled(changeSetHandler);

    final StateUpdate stateUpdate = new StateUpdate();
    changeSetHandler.clear();
    tree.updateStateAsync("key", stateUpdate, "test");

    assertThat(stateUpdate.mUpdateStateCalled).isTrue();
    assertChangeSetNotSeen(changeSetHandler);
  }

  @Test(expected = IllegalStateException.class)
  public void testRequestFocusBeforeDataBound() {
    final Section section = TestSectionCreator.createChangeSetComponent("test");
    final TestTarget changeSetHandler = new TestTarget();

    SectionTree tree = SectionTree.create(mSectionContext, changeSetHandler)
        .build();
    tree.requestFocus(section, 0);
  }

  @Test(expected = IllegalStateException.class)
  public void testRequestFocusIllegalIndex() {
    final Section section = TestSectionCreator.createChangeSetComponent("test");
    final TestTarget changeSetHandler = new TestTarget();

    SectionTree tree = SectionTree.create(mSectionContext, changeSetHandler)
        .build();
    tree.requestFocus(section, 1);
  }

  @Test
  public void testRequestFocusIndex() {
    final Section leaf1 = TestSectionCreator.createChangeSetComponent(
        "leaf1",
        Change.insert(0, makeComponentInfo()),
        Change.insert(1, makeComponentInfo()),
        Change.insert(2, makeComponentInfo()));

    final Section leaf2 = TestSectionCreator.createChangeSetComponent(
        "leaf2",
        Change.insert(0, makeComponentInfo()),
        Change.insert(1, makeComponentInfo()));

    final Section node1 = TestSectionCreator
        .createSectionComponent("node1", leaf1, leaf2);

    final Section leaf3 = TestSectionCreator.createChangeSetComponent(
        "leaf3",
        Change.insert(0, makeComponentInfo()));

    final Section leaf4 = TestSectionCreator.createChangeSetComponent(
        "leaf4",
        Change.insert(0, makeComponentInfo()),
        Change.insert(1, makeComponentInfo()),
        Change.insert(2, makeComponentInfo()),
        Change.insert(3, makeComponentInfo()));

    final Section node2 = TestSectionCreator
        .createSectionComponent("node2", leaf3, leaf4);

    final TestTarget changeSetHandler = new TestTarget();

    final Section root = TestSectionCreator.createSectionComponent("root", node1, node2);
    SectionTree tree = SectionTree.create(mSectionContext, changeSetHandler)
        .build();
    tree.setRoot(root);

    tree.requestFocus(node1, 0);
    assertThat(0).isEqualTo(changeSetHandler.getFocusedTo());

    tree.requestFocus(leaf2, 0);
    assertThat(3).isEqualTo(changeSetHandler.getFocusedTo());

    tree.requestFocus(leaf2, 1);
    assertThat(4).isEqualTo(changeSetHandler.getFocusedTo());

    tree.requestFocus(node2, 0);
    assertThat(5).isEqualTo(changeSetHandler.getFocusedTo());

    tree.requestFocus(leaf4, 0);
    assertThat(6).isEqualTo(changeSetHandler.getFocusedTo());

    tree.requestFocus(leaf4, 2);
    assertThat(8).isEqualTo(changeSetHandler.getFocusedTo());
  }

  @Test(expected = IllegalStateException.class)
  public void testRequestFocusWithOffsetBeforeDataBound() {
    final Section section = TestSectionCreator.createChangeSetComponent("test");
    final TestTarget changeSetHandler = new TestTarget();

    SectionTree tree = SectionTree.create(mSectionContext, changeSetHandler)
        .build();
    tree.requestFocusWithOffset(section, 0, 0);
  }

  @Test(expected = IllegalStateException.class)
  public void testRequestFocusWithOffsetIllegalIndex() {
    final Section section = TestSectionCreator.createChangeSetComponent("test");
    final TestTarget changeSetHandler = new TestTarget();

    SectionTree tree = SectionTree.create(mSectionContext, changeSetHandler)
        .build();
    tree.requestFocusWithOffset(section, 1, 0);
  }

  @Test
  public void testRequestFocusIndexWithOffset() {
    final Section leaf1 = TestSectionCreator.createChangeSetComponent(
        "leaf1",
        Change.insert(0, makeComponentInfo()),
        Change.insert(1, makeComponentInfo()),
        Change.insert(2, makeComponentInfo()));

    final Section leaf2 = TestSectionCreator.createChangeSetComponent(
        "leaf2",
        Change.insert(0, makeComponentInfo()),
        Change.insert(1, makeComponentInfo()));

    final Section node1 = TestSectionCreator
        .createSectionComponent("node1", leaf1, leaf2);

    final Section leaf3 = TestSectionCreator.createChangeSetComponent(
        "leaf3",
        Change.insert(0, makeComponentInfo()));

    final Section leaf4 = TestSectionCreator.createChangeSetComponent(
        "leaf4",
        Change.insert(0, makeComponentInfo()),
        Change.insert(1, makeComponentInfo()),
        Change.insert(2, makeComponentInfo()),
        Change.insert(3, makeComponentInfo()));

    final Section node2 = TestSectionCreator
        .createSectionComponent("node2", leaf3, leaf4);

    final TestTarget changeSetHandler = new TestTarget();

    final Section root = TestSectionCreator.createSectionComponent("root", node1, node2);
    SectionTree tree = SectionTree.create(mSectionContext, changeSetHandler)
        .build();
    tree.setRoot(root);

    tree.requestFocusWithOffset(node1, 0, 100);
    assertThat(0).isEqualTo(changeSetHandler.getFocusedTo());
    assertThat(100).isEqualTo(changeSetHandler.getFocusedToOffset());

    tree.requestFocusWithOffset(leaf2, 0, 200);
    assertThat(3).isEqualTo(changeSetHandler.getFocusedTo());
    assertThat(200).isEqualTo(changeSetHandler.getFocusedToOffset());

    tree.requestFocusWithOffset(leaf2, 1, 300);
    assertThat(4).isEqualTo(changeSetHandler.getFocusedTo());
    assertThat(300).isEqualTo(changeSetHandler.getFocusedToOffset());

    tree.requestFocusWithOffset(node2, 0, 400);
    assertThat(5).isEqualTo(changeSetHandler.getFocusedTo());
    assertThat(400).isEqualTo(changeSetHandler.getFocusedToOffset());

    tree.requestFocusWithOffset(leaf4, 0, 500);
    assertThat(6).isEqualTo(changeSetHandler.getFocusedTo());
    assertThat(500).isEqualTo(changeSetHandler.getFocusedToOffset());

    tree.requestFocusWithOffset(leaf4, 2, 600);
    assertThat(8).isEqualTo(changeSetHandler.getFocusedTo());
    assertThat(600).isEqualTo(changeSetHandler.getFocusedToOffset());
  }

  @Test
  public void testRequestFocusFirst() {
    final Section leaf1 = TestSectionCreator.createChangeSetComponent(
        "leaf1",
        Change.insert(0, makeComponentInfo()),
        Change.insert(1, makeComponentInfo()),
        Change.insert(2, makeComponentInfo()));

    final Section leaf2 = TestSectionCreator.createChangeSetComponent(
        "leaf2",
        Change.insert(0, makeComponentInfo()),
        Change.insert(1, makeComponentInfo()));

    final Section node1 = TestSectionCreator
        .createSectionComponent("node1", leaf1, leaf2);

    final Section leaf3 = TestSectionCreator.createChangeSetComponent(
        "leaf3",
        Change.insert(0, makeComponentInfo()));

    final Section leaf4 = TestSectionCreator.createChangeSetComponent(
        "leaf4",
        Change.insert(0, makeComponentInfo()),
        Change.insert(1, makeComponentInfo()),
        Change.insert(2, makeComponentInfo()),
        Change.insert(3, makeComponentInfo()));

    final Section node2 = TestSectionCreator
        .createSectionComponent("node2", leaf3, leaf4);

    final TestTarget changeSetHandler = new TestTarget();

    final Section root = TestSectionCreator.createSectionComponent("root", node1, node2);
    SectionTree tree = SectionTree.create(mSectionContext, changeSetHandler)
        .build();
    tree.setRoot(root);

    tree.requestFocusStart("rootnode1leaf1");
    assertThat(0).isEqualTo(changeSetHandler.getFocusedTo());

    tree.requestFocusStart("rootnode1leaf2");
    assertThat(3).isEqualTo(changeSetHandler.getFocusedTo());

    tree.requestFocusStart("rootnode2leaf4");
    assertThat(6).isEqualTo(changeSetHandler.getFocusedTo());
  }

  @Test
  public void testRequestFocusEnd() {
    final Section leaf1 = TestSectionCreator.createChangeSetComponent(
        "leaf1",
        Change.insert(0, makeComponentInfo()),
        Change.insert(1, makeComponentInfo()),
        Change.insert(2, makeComponentInfo()));

    final Section leaf2 = TestSectionCreator.createChangeSetComponent(
        "leaf2",
        Change.insert(0, makeComponentInfo()),
        Change.insert(1, makeComponentInfo()));

    final Section node1 = TestSectionCreator
        .createSectionComponent("node1", leaf1, leaf2);

    final Section leaf3 = TestSectionCreator.createChangeSetComponent(
        "leaf3",
        Change.insert(0, makeComponentInfo()));

    final Section leaf4 = TestSectionCreator.createChangeSetComponent(
        "leaf4",
        Change.insert(0, makeComponentInfo()),
        Change.insert(1, makeComponentInfo()),
        Change.insert(2, makeComponentInfo()),
        Change.insert(3, makeComponentInfo()));

    final Section node2 = TestSectionCreator
        .createSectionComponent("node2", leaf3, leaf4);

    final TestTarget changeSetHandler = new TestTarget();

    final Section root = TestSectionCreator.createSectionComponent("root", node1, node2);
    SectionTree tree = SectionTree.create(mSectionContext, changeSetHandler)
        .build();
    tree.setRoot(root);

    tree.requestFocusEnd("rootnode2");
    assertThat(9).isEqualTo(changeSetHandler.getFocusedTo());

    tree.requestFocusEnd("rootnode1leaf2");
    assertThat(4).isEqualTo(changeSetHandler.getFocusedTo());

    tree.requestFocusEnd("rootnode2leaf3");
    assertThat(5).isEqualTo(changeSetHandler.getFocusedTo());

    tree.requestFocusEnd("rootnode2leaf4");
    assertThat(9).isEqualTo(changeSetHandler.getFocusedTo());
  }

  @Test
  public void testIsSectionIndexValid() {
    final Section leaf1 =
        TestSectionCreator.createChangeSetComponent(
            "leaf1", Change.insert(0, makeComponentInfo()), Change.insert(1, makeComponentInfo()));

    final Section node1 = TestSectionCreator.createSectionComponent("node1", leaf1);

    final TestTarget changeSetHandler = new TestTarget();

    final Section root = TestSectionCreator.createSectionComponent("root", node1);
    SectionTree tree = SectionTree.create(mSectionContext, changeSetHandler).build();
    tree.setRoot(root);

    assertTrue(tree.isSectionIndexValid("rootnode1leaf1", 0));
    assertTrue(tree.isSectionIndexValid("rootnode1leaf1", 1));
    assertFalse(tree.isSectionIndexValid("rootnode1leaf1", -1));
    assertFalse(tree.isSectionIndexValid("rootnode1leaf1", 2));
  }

  @Test(expected = RuntimeException.class)
  public void testCannotForceBothSyncAndAsyncStateUpdates() {
    SectionTree.create(mSectionContext, new TestTarget())
        .forceSyncStateUpdates(true)
        .asyncStateUpdates(true)
        .build();
  }

  @Test
  public void testAsyncChangesetWithoutBackgroundChangesetSupportCalledFromMainThread()
      throws InterruptedException {
    final Section leaf1 =
        TestSectionCreator.createChangeSetComponent(
            "leaf1", Change.insert(0, makeComponentInfo()), Change.insert(1, makeComponentInfo()));

    final Section node1 = TestSectionCreator.createSectionComponent("node1", leaf1);

    final ThreadCheckingTarget target = new ThreadCheckingTarget(false);

    final Section root = TestSectionCreator.createSectionComponent("root", node1);
    final SectionTree tree = SectionTree.create(mSectionContext, target).build();
    final CountDownLatch latch = new CountDownLatch(1);
    final long mainThreadId = Thread.currentThread().getId();

    new Thread(
            new Runnable() {
              @Override
              public void run() {
                tree.setRoot(root);
                latch.countDown();
              }
            })
        .start();

    assertThat(latch.await(5000, TimeUnit.MILLISECONDS)).isTrue();

    ShadowLooper.runMainLooperOneTask();

    assertThat(target.getNumInserts()).isEqualTo(2);

    final List<Long> interactionThreadIds = target.getInteractionThreadIds();
    assertThat(interactionThreadIds).isNotEmpty();
    for (long id : interactionThreadIds) {
      assertThat(id).isEqualTo(mainThreadId);
    }
  }

  @Test
  public void testAsyncChangesetWithBackgroundChangesetSupportCalledFromBackgroundThread()
      throws InterruptedException {
    final Section leaf1 =
        TestSectionCreator.createChangeSetComponent(
            "leaf1", Change.insert(0, makeComponentInfo()), Change.insert(1, makeComponentInfo()));

    final Section node1 = TestSectionCreator.createSectionComponent("node1", leaf1);

    final ThreadCheckingTarget target = new ThreadCheckingTarget(true);

    final Section root = TestSectionCreator.createSectionComponent("root", node1);
    final SectionTree tree = SectionTree.create(mSectionContext, target).build();
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicLong bgThreadId = new AtomicLong();

    new Thread(
            new Runnable() {
              @Override
              public void run() {
                bgThreadId.set(Thread.currentThread().getId());
                tree.setRoot(root);
                latch.countDown();
              }
            })
        .start();

    assertThat(latch.await(5000, TimeUnit.MILLISECONDS)).isTrue();

    assertThat(target.getNumInserts()).isEqualTo(2);

    final List<Long> interactionThreadIds = target.getInteractionThreadIds();
    assertThat(interactionThreadIds).isNotEmpty();
    for (long id : interactionThreadIds) {
      assertThat(id).isEqualTo(bgThreadId.get());
    }
  }

  @Test
  public void testMainThreadChangesetWithBackgroundChangesetSupportCalledFromMainThread() {
    final Section leaf1 =
        TestSectionCreator.createChangeSetComponent(
            "leaf1", Change.insert(0, makeComponentInfo()), Change.insert(1, makeComponentInfo()));

    final Section node1 = TestSectionCreator.createSectionComponent("node1", leaf1);

    final ThreadCheckingTarget target = new ThreadCheckingTarget(true);

    final Section root = TestSectionCreator.createSectionComponent("root", node1);
    final SectionTree tree = SectionTree.create(mSectionContext, target).build();
    final long mainThreadId = Thread.currentThread().getId();

    tree.setRoot(root);

    assertThat(target.getNumInserts()).isEqualTo(2);

    final List<Long> interactionThreadIds = target.getInteractionThreadIds();
    assertThat(interactionThreadIds).isNotEmpty();
    for (long id : interactionThreadIds) {
      assertThat(id).isEqualTo(mainThreadId);
    }
  }

  @Test
  public void testCachedValues() {
    final ThreadCheckingTarget target = new ThreadCheckingTarget(true);

    SectionTree sectionTree = SectionTree.create(mSectionContext, target).build();
    assertThat(sectionTree.getCachedValue("key1")).isNull();
    sectionTree.putCachedValue("key1", "value1");
    assertThat(sectionTree.getCachedValue("key1")).isEqualTo("value1");
    assertThat(sectionTree.getCachedValue("key2")).isNull();
  }

  private static void assertChangeSetHandled(TestTarget testTarget) {
    assertThat(testTarget.wereChangesHandled()).isTrue();
    assertThat(testTarget.wasNotifyChangeSetCompleteCalledWithChangedData()).isTrue();
  }

  private static void assertChangeSetNotSeen(TestTarget testTarget) {
    assertThat(testTarget.wereChangesHandled()).isFalse();
    assertThat(testTarget.wasNotifyChangeSetCompleteCalledWithChangedData()).isFalse();
  }

  private static class StateUpdate implements SectionLifecycle.StateUpdate {

    private boolean mUpdateStateCalled;

    @Override
    public void updateState(StateContainer stateContainer) {
      mUpdateStateCalled = true;
    }
  }

  private static RenderInfo makeComponentInfo() {
    return ComponentRenderInfo.create().component(mock(Component.class)).build();
  }

  private static class ThreadCheckingTarget implements SectionTree.Target {

    private final boolean mSupportsBackgroundChangeSets;
    private final ArrayList<Long> mThreadIds = new ArrayList<>();
    private final AtomicInteger mNumInserts = new AtomicInteger(0);

    private ThreadCheckingTarget(boolean supportsBackgroundChangeSets) {
      mSupportsBackgroundChangeSets = supportsBackgroundChangeSets;
    }

    private void recordInteraction() {
      mThreadIds.add(Thread.currentThread().getId());
    }

    public List<Long> getInteractionThreadIds() {
      return mThreadIds;
    }

    public int getNumInserts() {
      return mNumInserts.get();
    }

    @Override
    public void insert(int index, RenderInfo renderInfo) {
      recordInteraction();
      mNumInserts.addAndGet(1);
    }

    @Override
    public void insertRange(int index, int count, List<RenderInfo> renderInfos) {
      recordInteraction();
      mNumInserts.addAndGet(renderInfos.size());
    }

    @Override
    public void update(int index, RenderInfo renderInfo) {
      recordInteraction();
    }

    @Override
    public void updateRange(int index, int count, List<RenderInfo> renderInfos) {
      recordInteraction();
    }

    @Override
    public void delete(int index) {
      recordInteraction();
    }

    @Override
    public void deleteRange(int index, int count) {
      recordInteraction();
    }

    @Override
    public void move(int fromPosition, int toPosition) {
      recordInteraction();
    }

    @Override
    public void notifyChangeSetComplete(
        boolean isDataChanged, ChangeSetCompleteCallback changeSetCompleteCallback) {
      recordInteraction();
    }

    @Override
    public void requestFocus(int index) {}

    @Override
    public void requestSmoothFocus(int index, int offset, SmoothScrollAlignmentType type) {}

    @Override
    public void requestFocusWithOffset(int index, int offset) {}

    @Override
    public boolean supportsBackgroundChangeSets() {
      return mSupportsBackgroundChangeSets;
    }
  }
}
