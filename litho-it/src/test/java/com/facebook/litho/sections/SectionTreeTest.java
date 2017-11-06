/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections;

import static com.facebook.litho.testing.sections.TestSectionCreator.TestSection;
import static org.assertj.core.api.Java6Assertions.assertThat;
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
    assertThat(changeSetHandler.wereChangesHandled()).isTrue();
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
    assertThat(changeSetHandler.wereChangesHandled()).isTrue();

    changeSetHandler.clear();

    tree.setRoot(section.makeShallowCopy(false));
    assertThat(changeSetHandler.wereChangesHandled()).isFalse();
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

    assertThat(changeSetHandler.wereChangesHandled()).isFalse();

    final Section secondSection = TestSectionCreator.createChangeSetSection(
        0,
        "leaf1",
        true,
        Change.update(0, makeComponentInfo()));
    tree.setRoot(secondSection);

    assertThat(changeSetHandler.wereChangesHandled()).isTrue();
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

    assertThat(changeSetHandler.wereChangesHandled()).isTrue();
    assertThat(changeSetHandler.getNumChanges()).isEqualTo(9);
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
    assertThat(changeSetHandler.wereChangesHandled()).isTrue();

    final StateUpdate stateUpdate = new StateUpdate();
    changeSetHandler.clear();
    tree.updateState("key", stateUpdate);

    assertThat(stateUpdate.mUpdateStateCalled).isTrue();
    assertThat(changeSetHandler.wereChangesHandled()).isFalse();
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
    assertThat(changeSetHandler.wereChangesHandled()).isTrue();

    final StateUpdate lazyStateUpdate = new StateUpdate();
    changeSetHandler.clear();
    tree.updateStateLazy("key", lazyStateUpdate);

    assertThat(lazyStateUpdate.mUpdateStateCalled).isFalse();

    final StateUpdate stateUpdate = new StateUpdate();
    tree.updateState("key", stateUpdate);

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
    assertThat(changeSetHandler.wereChangesHandled()).isTrue();

    final StateUpdate stateUpdate = new StateUpdate();
    changeSetHandler.clear();
    tree.release();
    tree.updateState("key", stateUpdate);

    assertThat(stateUpdate.mUpdateStateCalled).isFalse();
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
    assertThat(changeSetHandler.wereChangesHandled()).isTrue();
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
    assertThat(changeSetHandler.wereChangesHandled()).isTrue();

    final StateUpdate stateUpdate = new StateUpdate();
    changeSetHandler.clear();
    tree.updateStateAsync("key", stateUpdate);
    mChangeSetThreadShadowLooper.runOneTask();

    assertThat(stateUpdate.mUpdateStateCalled).isTrue();
    assertThat(changeSetHandler.wereChangesHandled()).isFalse();
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

  private static RenderInfo makeComponentInfo() {
    return ComponentRenderInfo.create().component(mock(Component.class)).build();
  }
}
