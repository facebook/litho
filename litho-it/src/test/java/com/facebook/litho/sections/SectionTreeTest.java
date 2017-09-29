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
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import android.os.Looper;
import com.facebook.litho.Component;
import com.facebook.litho.sections.SectionLifecycle.StateContainer;
import com.facebook.litho.testing.sections.TestSectionCreator;
import com.facebook.litho.testing.sections.TestTarget;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
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

  @Test
  public void testSetRoot() {

    final Section section = TestSectionCreator.createChangeSetComponent(
        "leaf1",
        Change.insert(0, makeComponentInfo()));

    final TestTarget changeSetHandler = new TestTarget();
    SectionTree tree = SectionTree.create(mSectionContext, changeSetHandler)
        .build();

    tree.setRoot(section);
    assertTrue(changeSetHandler.wereChangesHandled());
  }

  @Test
  public void testSetSameRoot() {
    final Section section = TestSectionCreator.createSection(
        0,
        "leaf1",
        TestSectionCreator.createChangeSetLifecycle(
            Change.insert(0, makeComponentInfo())),
        true);

    final TestTarget changeSetHandler = new TestTarget();
    SectionTree tree = SectionTree.create(mSectionContext, changeSetHandler)
        .build();

    tree.setRoot(section);
    assertTrue(changeSetHandler.wereChangesHandled());

    changeSetHandler.clear();

    tree.setRoot(section.makeShallowCopy(false));
    assertFalse(changeSetHandler.wereChangesHandled());
  }

  @Test
  public void updateTree() {
    final Section section = TestSectionCreator.createChangeSetComponent(
        "leaf1",
        Change.insert(0, makeComponentInfo()));
    final TestTarget changeSetHandler = new TestTarget();
    SectionTree tree = SectionTree.create(mSectionContext, changeSetHandler)
        .build();

    tree.setRoot(section);
    changeSetHandler.clear();
    tree.setRoot(section);

    assertFalse(changeSetHandler.wereChangesHandled());

    final Section secondSection = TestSectionCreator.createChangeSetComponent(
        "leaf1",
        Change.update(0, makeComponentInfo()));
    tree.setRoot(secondSection);

    assertTrue(changeSetHandler.wereChangesHandled());
  }

  @Test(expected = IllegalStateException.class)
  public void testSetRootWithSameKeys() {
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

    assertTrue(changeSetHandler.wereChangesHandled());
    assertEquals(changeSetHandler.getNumChanges(), 9);
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

    assertTrue(((TestSectionCreator.TestSection) leaf1).refreshCalled);
    assertTrue(((TestSectionCreator.TestSection) leaf2).refreshCalled);
    assertTrue(((TestSectionCreator.TestSection) node).refreshCalled);
    assertTrue(((TestSectionCreator.TestSection) leaf3).refreshCalled);
    assertTrue(((TestSectionCreator.TestSection) leaf4).refreshCalled);
    assertTrue(((TestSectionCreator.TestSection) node1).refreshCalled);
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

    tree.viewPortChanged(3,9, 3, 9);
    assertEquals(((TestSectionCreator.TestSection) leaf1).firstVisibleIndex, -1);
    assertEquals(((TestSectionCreator.TestSection) leaf1).lastVisibleIndex, -1);
    assertEquals(((TestSectionCreator.TestSection) leaf1).firstFullyVisibleIndex, -1);
    assertEquals(((TestSectionCreator.TestSection) leaf1).lastFullyVisibleIndex, -1);

    assertEquals(((TestSectionCreator.TestSection) leaf2).firstVisibleIndex, 0);
    assertEquals(((TestSectionCreator.TestSection) leaf2).lastVisibleIndex, 1);
    assertEquals(((TestSectionCreator.TestSection) leaf2).firstFullyVisibleIndex, 0);
    assertEquals(((TestSectionCreator.TestSection) leaf2).lastFullyVisibleIndex, 1);

    assertEquals(((TestSectionCreator.TestSection) leaf3).firstVisibleIndex, 0);
    assertEquals(((TestSectionCreator.TestSection) leaf3).lastVisibleIndex, 0);
    assertEquals(((TestSectionCreator.TestSection) leaf3).firstFullyVisibleIndex, 0);
    assertEquals(((TestSectionCreator.TestSection) leaf3).lastFullyVisibleIndex, 0);

    assertEquals(((TestSectionCreator.TestSection) leaf4).firstVisibleIndex, 0);
    assertEquals(((TestSectionCreator.TestSection) leaf4).lastVisibleIndex, 3);
    assertEquals(((TestSectionCreator.TestSection) leaf4).firstFullyVisibleIndex, 0);
    assertEquals(((TestSectionCreator.TestSection) leaf4).lastFullyVisibleIndex, 3);

    assertEquals(((TestSectionCreator.TestSection) node).firstVisibleIndex, 3);
    assertEquals(((TestSectionCreator.TestSection) node).lastVisibleIndex, 4);
    assertEquals(((TestSectionCreator.TestSection) node).firstFullyVisibleIndex, 3);
    assertEquals(((TestSectionCreator.TestSection) node).lastFullyVisibleIndex, 4);

    assertEquals(((TestSectionCreator.TestSection) node1).firstVisibleIndex, 0);
    assertEquals(((TestSectionCreator.TestSection) node1).lastVisibleIndex, 4);
    assertEquals(((TestSectionCreator.TestSection) node1).firstFullyVisibleIndex, 0);
    assertEquals(((TestSectionCreator.TestSection) node1).lastFullyVisibleIndex, 4);

    ((TestSectionCreator.TestSection) leaf1).firstVisibleIndex = 0;
    ((TestSectionCreator.TestSection) leaf1).lastVisibleIndex = 0;
    ((TestSectionCreator.TestSection) leaf1).firstFullyVisibleIndex = 0;
    ((TestSectionCreator.TestSection) leaf1).lastFullyVisibleIndex = 0;

    tree.viewPortChanged(3,9, 3, 9);

    assertEquals(((TestSectionCreator.TestSection) leaf1).firstVisibleIndex, 0);
    assertEquals(((TestSectionCreator.TestSection) leaf1).lastVisibleIndex, 0);
    assertEquals(((TestSectionCreator.TestSection) leaf1).firstFullyVisibleIndex, 0);
    assertEquals(((TestSectionCreator.TestSection) leaf1).lastFullyVisibleIndex, 0);

    tree.viewPortChanged(6, 9, 7, 9);

    assertEquals(((TestSectionCreator.TestSection) leaf4).firstVisibleIndex, 0);
    assertEquals(((TestSectionCreator.TestSection) leaf4).lastVisibleIndex, 3);
    assertEquals(((TestSectionCreator.TestSection) leaf4).firstFullyVisibleIndex, 1);
    assertEquals(((TestSectionCreator.TestSection) leaf4).lastFullyVisibleIndex, 3);
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
    assertTrue(changeSetHandler.wereChangesHandled());

    final StateUpdate stateUpdate = new StateUpdate();
    changeSetHandler.clear();
    tree.updateState("key", stateUpdate);

    assertTrue(stateUpdate.mUpdateStateCalled);
    assertFalse(changeSetHandler.wereChangesHandled());
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
    assertTrue(changeSetHandler.wereChangesHandled());

    final StateUpdate lazyStateUpdate = new StateUpdate();
    changeSetHandler.clear();
    tree.updateStateLazy("key", lazyStateUpdate);

    assertFalse(lazyStateUpdate.mUpdateStateCalled);

    final StateUpdate stateUpdate = new StateUpdate();
    tree.updateState("key", stateUpdate);

    assertTrue(lazyStateUpdate.mUpdateStateCalled);
    assertTrue(stateUpdate.mUpdateStateCalled);
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
    assertTrue(changeSetHandler.wereChangesHandled());

    final StateUpdate stateUpdate = new StateUpdate();
    changeSetHandler.clear();
    tree.release();
    tree.updateState("key", stateUpdate);

    assertFalse(stateUpdate.mUpdateStateCalled);
  }

  private static class StateUpdate implements SectionLifecycle.StateUpdate {

    private boolean mUpdateStateCalled;

    @Override
    public void updateState(
        StateContainer stateContainer, Section section) {
      mUpdateStateCalled = true;
    }
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
    assertTrue(changeSetHandler.wereChangesHandled());
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
    assertTrue(changeSetHandler.wereChangesHandled());

    final StateUpdate stateUpdate = new StateUpdate();
    changeSetHandler.clear();
    tree.updateStateAsync("key", stateUpdate);
    mChangeSetThreadShadowLooper.runOneTask();

    assertTrue(stateUpdate.mUpdateStateCalled);
    assertFalse(changeSetHandler.wereChangesHandled());
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
    assertEquals(0, changeSetHandler.getFocusedTo());

    tree.requestFocus(leaf2, 0);
    assertEquals(3, changeSetHandler.getFocusedTo());

    tree.requestFocus(leaf2, 1);
    assertEquals(4, changeSetHandler.getFocusedTo());

    tree.requestFocus(node2, 0);
    assertEquals(5, changeSetHandler.getFocusedTo());

    tree.requestFocus(leaf4, 0);
    assertEquals(6, changeSetHandler.getFocusedTo());

    tree.requestFocus(leaf4, 2);
    assertEquals(8, changeSetHandler.getFocusedTo());
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
    assertEquals(0, changeSetHandler.getFocusedTo());
    assertEquals(100, changeSetHandler.getFocusedToOffset());

    tree.requestFocusWithOffset(leaf2, 0, 200);
    assertEquals(3, changeSetHandler.getFocusedTo());
    assertEquals(200, changeSetHandler.getFocusedToOffset());

    tree.requestFocusWithOffset(leaf2, 1, 300);
    assertEquals(4, changeSetHandler.getFocusedTo());
    assertEquals(300, changeSetHandler.getFocusedToOffset());

    tree.requestFocusWithOffset(node2, 0, 400);
    assertEquals(5, changeSetHandler.getFocusedTo());
    assertEquals(400, changeSetHandler.getFocusedToOffset());

    tree.requestFocusWithOffset(leaf4, 0, 500);
    assertEquals(6, changeSetHandler.getFocusedTo());
    assertEquals(500, changeSetHandler.getFocusedToOffset());

    tree.requestFocusWithOffset(leaf4, 2, 600);
    assertEquals(8, changeSetHandler.getFocusedTo());
    assertEquals(600, changeSetHandler.getFocusedToOffset());
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
    assertEquals(0, changeSetHandler.getFocusedTo());

    tree.requestFocusStart("rootnode1leaf2");
    assertEquals(3, changeSetHandler.getFocusedTo());

    tree.requestFocusStart("rootnode2leaf4");
    assertEquals(6, changeSetHandler.getFocusedTo());
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
    assertEquals(9, changeSetHandler.getFocusedTo());

    tree.requestFocusEnd("rootnode1leaf2");
    assertEquals(4, changeSetHandler.getFocusedTo());

    tree.requestFocusEnd("rootnode2leaf3");
    assertEquals(5, changeSetHandler.getFocusedTo());

    tree.requestFocusEnd("rootnode2leaf4");
    assertEquals(9, changeSetHandler.getFocusedTo());
  }

  private static RenderInfo makeComponentInfo() {
    return ComponentRenderInfo.create().component(mock(Component.class)).build();
  }
}
