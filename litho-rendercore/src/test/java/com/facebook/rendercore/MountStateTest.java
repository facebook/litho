/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.rendercore;

import static com.facebook.rendercore.RenderUnit.DelegateBinder.createDelegateBinder;
import static com.facebook.rendercore.TestBinder.TestBinder1;
import static com.facebook.rendercore.TestBinder.TestBinder2;
import static com.facebook.rendercore.TestBinder.TestBinder3;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.util.Pair;
import com.facebook.rendercore.TestBinderWithBindData.TestBinderWithBindData1;
import com.facebook.rendercore.TestBinderWithBindData.TestBinderWithBindData2;
import com.facebook.rendercore.TestBinderWithBindData.TestBinderWithBindData3;
import com.facebook.rendercore.renderunits.HostRenderUnit;
import com.facebook.rendercore.testing.LayoutResultWrappingNode;
import com.facebook.rendercore.testing.RenderCoreTestRule;
import com.facebook.rendercore.testing.SimpleLayoutResult;
import com.facebook.rendercore.testing.TestHostRenderUnit;
import com.facebook.rendercore.testing.TestHostView;
import com.facebook.rendercore.testing.TestNode;
import com.facebook.rendercore.testing.TestRenderUnit;
import com.facebook.rendercore.testing.ViewWrapperUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class MountStateTest {

  public final @Rule RenderCoreTestRule mRenderCoreTestRule = new RenderCoreTestRule();
  public final @Rule ExpectedException mExpectedException = ExpectedException.none();

  @Test
  public void testUnmountAll() {
    final Context c = RuntimeEnvironment.application;

    final TestNode root = new TestNode();
    final TestNode leaf = new TestNode(0, 0, 10, 10);
    final TestNode leafTwo = new TestNode(10, 0, 10, 10);

    root.addChild(leaf);
    root.addChild(leafTwo);

    final TestRenderUnit testRenderUnitOne = new TestRenderUnit();
    final TestRenderUnit testRenderUnitTwo = new TestRenderUnit();
    final TestBinder testBinderOne = new TestBinder();
    final TestBinder testBinderTwo = new TestBinder();

    testRenderUnitOne.addOptionalMountBinder(
        createDelegateBinder(testRenderUnitOne, testBinderOne));
    testRenderUnitTwo.addOptionalMountBinder(
        createDelegateBinder(testRenderUnitTwo, testBinderTwo));

    leaf.setRenderUnit(testRenderUnitOne);
    leafTwo.setRenderUnit(testRenderUnitTwo);

    RenderTree renderTree = createRenderTree(c, root);
    final MountState mountState = createMountState(c);

    mountState.mount(renderTree);
    mountState.unmountAllItems();

    assertThat(testBinderOne.wasBound).isTrue();
    assertThat(testBinderTwo.wasBound).isTrue();

    assertThat(testBinderOne.wasUnbound).isTrue();
    assertThat(testBinderTwo.wasUnbound).isTrue();
  }

  @Test
  public void testUnmountAllDoesntTraverseNestedHierarchies() {
    final Context c = RuntimeEnvironment.application;

    final TestHostView innerTestHost = new TestHostView(c);

    final TestNode root = new TestNode();
    final TestNode leaf = new TestNode(0, 0, 10, 10);
    final TestNode leafTwo = new TestNode(10, 0, 10, 10);

    root.addChild(leaf);
    root.addChild(leafTwo);

    final TestRenderUnit testRenderUnitOne =
        new TestRenderUnit() {
          @Override
          public View createContent(Context c) {
            return innerTestHost;
          }
        };

    final TestBinder testBinderOne = new TestBinder();
    testRenderUnitOne.addOptionalMountBinder(
        createDelegateBinder(testRenderUnitOne, testBinderOne));
    leaf.setRenderUnit(testRenderUnitOne);

    RenderTree renderTree = createRenderTree(c, root);
    final MountState mountState = createMountState(c);

    final TestNode innerTreeRoot = new TestNode();
    final TestNode innerLeaf = new TestNode(0, 0, 10, 10);
    innerTreeRoot.addChild(innerLeaf);
    RenderUnit innerRenderUnit = new TestRenderUnit();
    final TestBinder innerBinder = new TestBinder();
    innerRenderUnit.addOptionalMountBinder(createDelegateBinder(innerRenderUnit, innerBinder));
    innerLeaf.setRenderUnit(innerRenderUnit);
    RenderTree innerRenderTree = createRenderTree(c, innerTreeRoot);
    final MountState innerMountState = new MountState(innerTestHost);

    mountState.mount(renderTree);
    innerMountState.mount(innerRenderTree);
    mountState.unmountAllItems();

    assertThat(testBinderOne.wasBound).isTrue();
    assertThat(innerBinder.wasBound).isTrue();

    assertThat(testBinderOne.wasUnbound).isTrue();
    assertThat(innerBinder.wasUnbound).isFalse();

    innerMountState.unmountAllItems();
    assertThat(innerBinder.wasUnbound).isTrue();
  }

  @Test
  public void testBinderUnmountCallOrder() {
    final Context c = RuntimeEnvironment.application;

    final TestNode root = new TestNode();
    final TestNode leaf = new TestNode(0, 0, 10, 10);

    root.addChild(leaf);

    final List bindOrder = new ArrayList<>();
    final List unbindOrder = new ArrayList<>();

    // Using anonymous class to create another type.
    final TestBinder attachBinderOne = new TestBinder(bindOrder, unbindOrder) {};
    final TestBinder attachBinderTwo = new TestBinder(bindOrder, unbindOrder);

    final TestRenderUnit renderUnit = new TestRenderUnit();
    renderUnit.addAttachBinders(
        createDelegateBinder(renderUnit, attachBinderOne),
        createDelegateBinder(renderUnit, attachBinderTwo));

    final TestBinder mountBinderOne = new TestBinder(bindOrder, unbindOrder) {};
    final TestBinder mountBinderTwo = new TestBinder(bindOrder, unbindOrder);

    renderUnit.addOptionalMountBinders(
        createDelegateBinder(renderUnit, mountBinderOne),
        createDelegateBinder(renderUnit, mountBinderTwo));

    leaf.setRenderUnit(renderUnit);

    final RenderTree renderTree = createRenderTree(c, root);
    final TestHostView host = new TestHostView(c, bindOrder, unbindOrder);
    final MountState mountState = new MountState(host);

    mountState.mount(renderTree);
    mountState.unmountAllItems();

    assertThat(bindOrder)
        .containsExactly(mountBinderOne, mountBinderTwo, host, attachBinderOne, attachBinderTwo);
    assertThat(unbindOrder)
        .containsExactly(host, attachBinderTwo, attachBinderOne, mountBinderTwo, mountBinderOne);
  }

  @Test
  public void testOnUpdateMountItem_whenThereAreNoBinders() {
    final Context c = RuntimeEnvironment.application;
    final long id;
    MountState mountState;

    {
      final TestNode root = new TestNode();
      final TestNode leaf = new TestNode(0, 0, 10, 10);

      root.addChild(leaf);

      final TestRenderUnit renderUnit = new TestRenderUnit();
      id = renderUnit.getId();
      leaf.setRenderUnit(renderUnit);
      RenderTree renderTree = createRenderTree(c, root);
      mountState = createMountState(c);

      mountState.mount(renderTree);
    }

    {
      final TestNode newRoot = new TestNode();

      final TestNode newLeaf = new TestNode(10, 10, 10, 10);
      newLeaf.setLayoutData(new Object());

      final TestRenderUnit newRenderUnit = new TestRenderUnit();
      newRenderUnit.setId(id);
      newLeaf.setRenderUnit(newRenderUnit);

      RenderTree newRenderTree = createRenderTree(c, newRoot);
      mountState.mount(newRenderTree);
    }
  }

  @Test
  public void testBinderUnmountCallOrderOnUpdateMountItem_whenShouldUpdateReturnsFalse() {
    final Context c = RuntimeEnvironment.application;
    final long id;
    MountState mountState;

    {
      final TestNode root = new TestNode();
      final TestNode leaf = new TestNode(0, 0, 10, 10);

      root.addChild(leaf);

      final TestRenderUnit renderUnit = new TestRenderUnit();
      id = renderUnit.getId();
      leaf.setRenderUnit(renderUnit);
      RenderTree renderTree = createRenderTree(c, root);
      mountState = createMountState(c);

      mountState.mount(renderTree);
    }

    {
      final TestNode newRoot = new TestNode();
      final TestNode newLeaf = new TestNode(10, 10, 10, 10);

      newRoot.addChild(newLeaf);

      final List<TestBinder> bindOrder = new ArrayList<>();
      final List<TestBinder> unbindOrder = new ArrayList<>();

      final TestBinder mountBinder = new TestBinder(bindOrder, unbindOrder);
      final TestBinder attachBinder = new TestBinder(bindOrder, unbindOrder);
      final TestRenderUnit newRenderUnit = new TestRenderUnit();
      newRenderUnit.addOptionalMountBinder(createDelegateBinder(newRenderUnit, mountBinder));
      newRenderUnit.addAttachBinder(createDelegateBinder(newRenderUnit, attachBinder));

      newRenderUnit.setId(id);
      newLeaf.setRenderUnit(newRenderUnit);

      RenderTree newRenderTree = createRenderTree(c, newRoot);
      mountState.mount(newRenderTree);

      assertThat(unbindOrder).isEmpty();
    }
  }

  @Test
  public void testBinderUnmountCallOrderOnUpdateMountItem_whenShouldUpdateReturnsTrue() {
    final Context c = RuntimeEnvironment.application;
    final long id;
    MountState mountState;

    final List<TestBinder> bindOrder = new ArrayList<>();
    final List<TestBinder> unbindOrder = new ArrayList<>();
    final TestBinder1 mountBinder = new TestBinder1(bindOrder, unbindOrder);
    final TestBinder1 attachBinder = new TestBinder1(bindOrder, unbindOrder);
    {
      final TestNode root = new TestNode();
      final TestNode leaf = new TestNode(0, 0, 10, 10);

      root.addChild(leaf);

      final TestRenderUnit renderUnit = new TestRenderUnit();
      renderUnit.addOptionalMountBinder(createDelegateBinder(renderUnit, mountBinder));
      renderUnit.addAttachBinder(createDelegateBinder(renderUnit, attachBinder));
      id = renderUnit.getId();
      leaf.setRenderUnit(renderUnit);
      RenderTree renderTree = createRenderTree(c, root);
      mountState = createMountState(c);

      mountState.mount(renderTree);
      assertThat(bindOrder).containsExactly(mountBinder, attachBinder);
    }

    // Reset bind/unbind order for next mount.
    bindOrder.clear();
    unbindOrder.clear();

    {
      final TestNode newRoot = new TestNode();
      final TestNode newLeaf = new TestNode(10, 10, 10, 10);
      newLeaf.setLayoutData(new Object());

      newRoot.addChild(newLeaf);

      final TestBinder1 mountBinder1 = new TestBinder1(bindOrder, unbindOrder);
      final TestBinder2 mountBinder2 = new TestBinder2(bindOrder, unbindOrder);

      final TestBinder1 attachBinder1 = new TestBinder1(bindOrder, unbindOrder);
      final TestBinder2 attachBinder2 = new TestBinder2(bindOrder, unbindOrder);
      final TestBinder3 attachBinder3 = new TestBinder3(bindOrder, unbindOrder);

      final TestRenderUnit newRenderUnit = new TestRenderUnit();
      newRenderUnit.addOptionalMountBinders(
          createDelegateBinder(newRenderUnit, mountBinder1),
          createDelegateBinder(newRenderUnit, mountBinder2));
      newRenderUnit.addAttachBinders(
          createDelegateBinder(newRenderUnit, attachBinder1),
          createDelegateBinder(newRenderUnit, attachBinder2),
          createDelegateBinder(newRenderUnit, attachBinder3));

      newRenderUnit.setId(id);
      newLeaf.setRenderUnit(newRenderUnit);

      RenderTree newRenderTree = createRenderTree(c, newRoot);
      mountState.mount(newRenderTree);

      assertThat(unbindOrder).containsExactly(attachBinder, mountBinder);
      assertThat(bindOrder)
          .containsExactly(mountBinder1, mountBinder2, attachBinder1, attachBinder2, attachBinder3);
    }
  }

  @Test
  public void testUnboundBinderArrayReuseOnUpdateMountItems() {
    final Context c = RuntimeEnvironment.application;
    final long id1;
    final long id2;
    MountState mountState;

    final List<TestBinder> bindOrder = new ArrayList<>();
    final List<TestBinder> unbindOrder = new ArrayList<>();

    final TestBinder currentMountBinder1 = new TestBinder(bindOrder, unbindOrder);
    final TestBinder currentAttachBinder1 = new TestBinder(bindOrder, unbindOrder);
    {
      final TestNode root = new TestNode();

      // Adds 1st child
      {
        final TestNode leaf1 = new TestNode(0, 0, 10, 10);
        final TestRenderUnit renderUnit1 = new TestRenderUnit();
        renderUnit1.addOptionalMountBinder(createDelegateBinder(renderUnit1, currentMountBinder1));
        renderUnit1.addAttachBinder(createDelegateBinder(renderUnit1, currentAttachBinder1));
        id1 = renderUnit1.getId();
        leaf1.setRenderUnit(renderUnit1);
        root.addChild(leaf1);
      }

      // Adds 2nd child
      final TestBinder mountBinder2 = new TestBinder(bindOrder, unbindOrder);
      final TestBinder attachBinder2 = new TestBinder(bindOrder, unbindOrder);
      {
        final TestNode leaf2 = new TestNode(10, 10, 10, 10);
        final TestRenderUnit renderUnit2 = new TestRenderUnit();
        renderUnit2.addOptionalMountBinder(createDelegateBinder(renderUnit2, mountBinder2));
        renderUnit2.addAttachBinder(createDelegateBinder(renderUnit2, attachBinder2));
        id2 = renderUnit2.getId();
        leaf2.setRenderUnit(renderUnit2);
        root.addChild(leaf2);
      }

      RenderTree renderTree = createRenderTree(c, root);
      mountState = createMountState(c);

      mountState.mount(renderTree);
      assertThat(bindOrder)
          .containsExactly(currentMountBinder1, currentAttachBinder1, mountBinder2, attachBinder2);
    }

    // Reset bind/unbind orders for next mount.
    bindOrder.clear();
    unbindOrder.clear();

    {
      final TestNode newRoot = new TestNode();

      final TestBinder newMountBinder1 = new TestBinder(bindOrder, unbindOrder);
      final TestBinder newAttachBinder1 = new TestBinder(bindOrder, unbindOrder);

      final TestBinder mountBinder2 = new TestBinder(bindOrder, unbindOrder);
      final TestBinder attachBinder2 = new TestBinder(bindOrder, unbindOrder);

      // Adds 1st new child (should update = true)
      {
        final TestNode newLeaf1 = new TestNode(10, 10, 10, 10);
        newLeaf1.setLayoutData(new Object()); // makes should update return true

        final TestRenderUnit newRenderUnit = new TestRenderUnit();
        newRenderUnit.addOptionalMountBinder(createDelegateBinder(newRenderUnit, newMountBinder1));
        newRenderUnit.addAttachBinder(createDelegateBinder(newRenderUnit, newAttachBinder1));

        newRenderUnit.setId(id1);
        newLeaf1.setRenderUnit(newRenderUnit);

        newRoot.addChild(newLeaf1);
      }

      // Adds 2nd new child (should update = false)
      {
        final TestNode newLeaf2 = new TestNode(10, 10, 10, 10);

        final TestRenderUnit newRenderUnit = new TestRenderUnit();
        newRenderUnit.addOptionalMountBinder(createDelegateBinder(newRenderUnit, mountBinder2));
        newRenderUnit.addAttachBinder(createDelegateBinder(newRenderUnit, attachBinder2));

        newRenderUnit.setId(id2);
        newLeaf2.setRenderUnit(newRenderUnit);

        newRoot.addChild(newLeaf2);
      }

      RenderTree newRenderTree = createRenderTree(c, newRoot);
      mountState.mount(newRenderTree);

      assertThat(unbindOrder).containsExactly(currentAttachBinder1, currentMountBinder1);
      assertThat(bindOrder).containsExactly(newMountBinder1, newAttachBinder1);
    }
  }

  @Test
  public void onMountUnmountRenderTreeWithBindersForRoot_shouldCallBindersForRoot() {
    final Context c = RuntimeEnvironment.application;
    final MountState mountState;

    final TestNode node = new TestNode(0, 0, 10, 10);
    final TestRenderUnit unit = new TestRenderUnit();
    node.setRenderUnit(unit);

    final RenderTree tree = createRenderTree(c, node);

    final RenderUnit rootRenderUnit = tree.getRoot().getRenderUnit();

    final List<TestBinder> bindOrder = new ArrayList<>();
    final List<TestBinder> unbindOrder = new ArrayList<>();
    final TestBinder mountBinder = new TestBinder(bindOrder, unbindOrder);
    final TestBinder attachDetachBinder = new TestBinder(bindOrder, unbindOrder);

    rootRenderUnit.addOptionalMountBinder(createDelegateBinder(rootRenderUnit, mountBinder));
    rootRenderUnit.addAttachBinder(createDelegateBinder(rootRenderUnit, attachDetachBinder));

    mountState = createMountState(c);
    mountState.mount(tree);

    assertThat(bindOrder).containsExactly(mountBinder, attachDetachBinder);

    mountState.unmountAllItems();

    assertThat(unbindOrder).containsExactly(attachDetachBinder, mountBinder);

    bindOrder.clear();
    mountState.mount(tree);

    assertThat(bindOrder).containsExactly(mountBinder, attachDetachBinder);
  }

  @Test
  public void testItemsNotMovedWhenPositionInHostDoesntChange() {
    final Context c = RuntimeEnvironment.application;
    final TestNode node = new TestNode(0, 0, 100, 100);
    final TestNode hostNode = new TestNode(0, 0, 100, 100);
    final TestNode child = new TestNode(0, 0, 10, 10);
    final TestNode secondChild = new TestNode(0, 0, 10, 10);
    final TestHostView testHost = new TestHostView(c);

    final TestHostRenderUnit hostRenderUnit =
        new TestHostRenderUnit() {
          @Override
          public Host createContent(Context c) {
            return testHost;
          }
        };

    hostNode.setRenderUnit(hostRenderUnit);

    final TestRenderUnit childRenderUnit = new TestRenderUnit();
    child.setRenderUnit(childRenderUnit);

    final TestRenderUnit secondChildRenderUnit = new TestRenderUnit();
    secondChild.setRenderUnit(secondChildRenderUnit);

    node.addChild(hostNode);
    hostNode.addChild(child);
    hostNode.addChild(secondChild);
    final RenderTree renderTree = createRenderTree(c, node);
    final MountState mountState = createMountState(c);
    mountState.mount(renderTree);
    assertThat(testHost.getMoveCount()).isEqualTo(0);

    final TestNode newRoot = new TestNode(0, 0, 100, 100);
    final TestNode newSibling = new TestNode(0, 0, 100, 100);
    final TestRenderUnit newSiblingRenderUnit = new TestRenderUnit();
    newSibling.setRenderUnit(newSiblingRenderUnit);
    newRoot.addChild(newSibling);
    newRoot.addChild(hostNode);

    final RenderTree secondRenderTree = createRenderTree(c, newRoot);
    mountState.mount(secondRenderTree);
    assertThat(testHost.getMoveCount()).isEqualTo(0);
  }

  @Test
  public void onMountEmptyRenderTree_MountStateShouldHostExpectedState() {
    final Context c = RuntimeEnvironment.application;
    final MountState mountState = createMountState(c);

    assertThat(mountState.getMountItemCount()).describedAs("Number of mounted items").isEqualTo(0);
    assertThat(mountState.isRootItem(0)).describedAs("No item should not be root").isFalse();
    assertThat(mountState.isRootItem(1)).describedAs("No item should not be root").isFalse();
    assertThat(mountState.getMountItemAt(0)).describedAs("Root item").isNull();
    assertThat(mountState.getContentAt(1)).describedAs("1st item").isNull();
    assertThat(mountState.getContentAt(3)).describedAs("3rd item").isNull();
    assertThat(mountState.getContentById(1)).describedAs("1st item").isNull();
    assertThat(mountState.getContentById(3)).describedAs("3rd item").isNull();
    assertThat(mountState.getHosts()).describedAs("Number of Hosts").hasSize(0);

    final RenderTree tree =
        createRenderTree(
            c,
            new LayoutResultWrappingNode(
                SimpleLayoutResult.create().width(100).height(100).build()));

    mountState.mount(tree);

    assertThat(mountState.getMountItemCount()).describedAs("Number of mounted items").isEqualTo(1);
    assertThat(mountState.isRootItem(0)).describedAs("0th item should be root").isTrue();
    assertThat(mountState.isRootItem(1)).describedAs("1st item should be root").isFalse();
    assertThat(mountState.getMountItemAt(0)).describedAs("Root item").isNotNull();
    assertThat(mountState.getContentAt(0)).describedAs("0th item").isNotNull();
    assertThat(mountState.getContentAt(3)).describedAs("3rd item").isNull();
    assertThat(mountState.getContentById(0)).describedAs("0th item").isNotNull();
    assertThat(mountState.getContentById(3)).describedAs("3rd item").isNull();
    assertThat(mountState.getHosts()).describedAs("Number of Hosts").hasSize(1);
  }

  @Test
  public void onMountNestedRenderTree_MountStateShouldHostExpectedState() {
    final Context c = RuntimeEnvironment.application;
    final LayoutResult root =
        SimpleLayoutResult.create()
            .width(1000)
            .height(1000)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new ViewWrapperUnit(new TextView(c), 1))
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .y(100)
                    .width(400)
                    .height(400)
                    .child(
                        SimpleLayoutResult.create()
                            .renderUnit(new ViewWrapperUnit(new TextView(c), 2))
                            .x(100)
                            .y(100)
                            .width(100)
                            .height(100))
                    .child(
                        SimpleLayoutResult.create()
                            .renderUnit(new HostRenderUnit(3))
                            .x(200)
                            .y(200)
                            .width(200)
                            .height(200)
                            .child(
                                SimpleLayoutResult.create()
                                    .renderUnit(new ViewWrapperUnit(new TextView(c), 4))
                                    .x(100)
                                    .y(100)
                                    .width(100)
                                    .height(100))))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new HostRenderUnit(5))
                    .y(400)
                    .width(400)
                    .height(400)
                    .child(
                        SimpleLayoutResult.create()
                            .renderUnit(new ViewWrapperUnit(new TextView(c), 6))
                            .x(100)
                            .y(100)
                            .width(100)
                            .height(100))
                    .child(
                        SimpleLayoutResult.create()
                            .renderUnit(new HostRenderUnit(7))
                            .x(200)
                            .y(200)
                            .width(200)
                            .height(200)
                            .child(
                                SimpleLayoutResult.create()
                                    .renderUnit(new ViewWrapperUnit(new TextView(c), 8))
                                    .x(100)
                                    .y(100)
                                    .width(100)
                                    .height(100))))
            .build();

    final RenderTree renderTree = createRenderTree(c, new LayoutResultWrappingNode(root));
    final MountState mountState = createMountState(c);

    mountState.mount(renderTree);

    assertThat(mountState.needsRemount()).describedAs("Needs Remount").isFalse();
    assertThat(mountState.getMountItemCount()).describedAs("Number of mounted items").isEqualTo(9);
    assertThat(mountState.isRootItem(0)).describedAs("0th item should be root").isTrue();
    assertThat(mountState.isRootItem(1)).describedAs("1st item should not be root").isFalse();
    assertThat(mountState.getMountItemAt(0)).describedAs("root item").isNotNull();
    assertThat(mountState.getContentAt(1)).describedAs("1st item").isInstanceOf(TextView.class);
    assertThat(mountState.getContentAt(3)).describedAs("3rd item").isInstanceOf(Host.class);
    assertThat(mountState.getContentById(1)).describedAs("1st item").isInstanceOf(TextView.class);
    assertThat(mountState.getContentById(3)).describedAs("3rd item").isInstanceOf(Host.class);
    assertThat(mountState.getHosts()).describedAs("Number of Hosts").hasSize(4);

    mountState.unmountAllItems();

    assertThat(mountState.needsRemount()).describedAs("Needs Remount").isTrue();
    assertThat(mountState.getMountItemCount()).describedAs("Number of mounted items").isEqualTo(0);
    assertThat(mountState.isRootItem(0)).describedAs("No item should not be root").isFalse();
    assertThat(mountState.isRootItem(1)).describedAs("No item should not be root").isFalse();
    assertThat(mountState.getMountItemAt(0)).describedAs("Root item").isNull();
    assertThat(mountState.getContentAt(1)).describedAs("1st item").isNull();
    assertThat(mountState.getContentAt(3)).describedAs("3rd item").isNull();
    assertThat(mountState.getContentById(1)).describedAs("1st item").isNull();
    assertThat(mountState.getContentById(3)).describedAs("3rd item").isNull();
    assertThat(mountState.getHosts()).describedAs("Number of Hosts").hasSize(0);
  }

  @Test
  public void onAttachDetachMountState_MountStateShouldCallBinders() {
    final Context c = RuntimeEnvironment.application;

    final List<TestBinder<?>> bindOrder = new ArrayList<>();
    final List<TestBinder<?>> unbindOrder = new ArrayList<>();

    TestBinder bindBinder = new TestBinder<>(bindOrder, unbindOrder);
    TestBinder mountBinder = new TestBinder<>(bindOrder, unbindOrder);

    final LayoutResult root =
        SimpleLayoutResult.create()
            .width(200)
            .height(200)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(
                        new ViewWrapperUnit(new TextView(c), 1)
                            .addBindBinders(bindBinder)
                            .addMounBinders(mountBinder))
                    .width(100)
                    .height(100))
            .build();

    final RenderTree renderTree = createRenderTree(c, new LayoutResultWrappingNode(root));
    final MountState mountState = createMountState(c);

    mountState.mount(renderTree);
    mountState.detach();

    assertThat(bindOrder).containsExactly(mountBinder, bindBinder);
    assertThat(unbindOrder).containsExactly(bindBinder);

    bindOrder.clear();
    mountState.attach();

    assertThat(bindOrder).containsExactly(bindBinder);

    assertThat(mountState.getMountItemCount()).isEqualTo(2);

    final MountItem item = mountState.getMountItemAt(0);
    assertThat(item).isNotNull();

    mountState.unbindMountItem(item);

    assertThat(mountState.getMountItemCount()).isEqualTo(2);
    assertThat(bindOrder).containsExactly(bindBinder);
  }

  @Test
  public void onAttachDetachRootHostToWindow_mountStateCallsAttachDetachBinders() {
    final Context c = mRenderCoreTestRule.getContext();

    final List<TestBinder<?>> bindOrder = new ArrayList<>();
    final List<TestBinder<?>> unbindOrder = new ArrayList<>();

    TestBinder attachBinder = new TestBinder<>(bindOrder, unbindOrder);
    TestBinder mountBinder = new TestBinder<>(bindOrder, unbindOrder);

    final LayoutResult root =
        SimpleLayoutResult.create()
            .width(200)
            .height(200)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(
                        new ViewWrapperUnit(new TextView(c), 1)
                            .addBindBinders(attachBinder)
                            .addMounBinders(mountBinder))
                    .width(100)
                    .height(100))
            .build();

    final RootHostView rootHost = new RootHostView(c);

    mRenderCoreTestRule
        .useRootHost(rootHost)
        .useRootNode(new LayoutResultWrappingNode(root))
        .render();

    // Should bind both the mount/unmount and attach/detach binders
    assertThat(bindOrder)
        .describedAs("binder were bound in order")
        .containsExactly(mountBinder, attachBinder);

    // No unbinds
    assertThat(unbindOrder).describedAs("no binders should be unbound").isEmpty();

    bindOrder.clear();

    rootHost.onDetachedFromWindow();

    // Should unbind the attach/detach binder
    assertThat(unbindOrder)
        .describedAs("should only unbind attach binder")
        .containsExactly(attachBinder);

    // Should still have the view mounted
    assertThat(rootHost.getChildCount())
        .describedAs("should still have content mounted")
        .isEqualTo(1);

    unbindOrder.clear();

    rootHost.onAttachedToWindow();

    // Should bind the attach/detach binder again
    assertThat(bindOrder)
        .describedAs("should only rebind attach binders")
        .containsExactly(attachBinder);
  }

  @Test
  public void onAttachDetachRenderTreeHostToWindow_mountStateCallsAttachDetachBinders() {
    final Context c = mRenderCoreTestRule.getContext();

    final List<TestBinder<?>> bindOrder = new ArrayList<>();
    final List<TestBinder<?>> unbindOrder = new ArrayList<>();

    TestBinder attachBinder = new TestBinder<>(bindOrder, unbindOrder);
    TestBinder mountBinder = new TestBinder<>(bindOrder, unbindOrder);

    final LayoutResult root =
        SimpleLayoutResult.create()
            .width(200)
            .height(200)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(
                        new ViewWrapperUnit(new TextView(c), 1)
                            .addBindBinders(attachBinder)
                            .addMounBinders(mountBinder))
                    .width(100)
                    .height(100))
            .build();

    final RenderTreeHostView rootHost = new RenderTreeHostView(c);

    mRenderCoreTestRule
        .useRenderTreeHost(rootHost)
        .useRootNode(new LayoutResultWrappingNode(root))
        .renderWithRenderTreeHost();

    // Should bind both the mount/unmount and attach/detach binders
    assertThat(bindOrder)
        .describedAs("binder were bound in order")
        .containsExactly(mountBinder, attachBinder);

    // No unbinds
    assertThat(unbindOrder).describedAs("no binders should be unbound").isEmpty();

    bindOrder.clear();

    rootHost.onDetachedFromWindow();

    // Should unbind the attach/detach binder
    assertThat(unbindOrder)
        .describedAs("should only unbind attach binder")
        .containsExactly(attachBinder);

    // Should still have the view mounted
    assertThat(rootHost.getChildCount())
        .describedAs("should still have content mounted")
        .isEqualTo(1);

    unbindOrder.clear();

    rootHost.onAttachedToWindow();

    // Should bind the attach/detach binder again
    assertThat(bindOrder)
        .describedAs("should only rebind attach binders")
        .containsExactly(attachBinder);
  }

  @Test
  public void onNotifyMountUnmount_ShouldUpdateMountItemCount() {
    final Context c = RuntimeEnvironment.application;
    final LayoutResult root =
        SimpleLayoutResult.create()
            .width(1000)
            .height(1000)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new ViewWrapperUnit(new TextView(c), 1))
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new ViewWrapperUnit(new TextView(c), 2))
                    .x(200)
                    .y(200)
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new ViewWrapperUnit(new TextView(c), 3))
                    .x(300)
                    .y(300)
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new ViewWrapperUnit(new TextView(c), 4))
                    .x(400)
                    .y(400)
                    .width(100)
                    .height(100))
            .build();

    final RenderTree renderTree = createRenderTree(c, new LayoutResultWrappingNode(root));
    final MountState mountState = createMountState(c);

    mountState.mount(renderTree);

    assertThat(mountState.needsRemount()).describedAs("Needs Remount").isFalse();
    assertThat(mountState.getMountItemCount()).describedAs("Number of mounted items").isEqualTo(5);
    assertThat(mountState.getContentById(1))
        .describedAs("Item with id 1")
        .isInstanceOf(TextView.class);
    assertThat(mountState.getContentById(4))
        .describedAs("Item with id 4")
        .isInstanceOf(TextView.class);
    assertThat(mountState.getMountItemAt(4)).describedAs("4th MountItem").isNotNull();
    assertThat(mountState.getMountItemAt(4).getContent())
        .describedAs("4th item")
        .isInstanceOf(TextView.class);
    assertThat(mountState.getRenderUnitCount()).describedAs("Number of RenderUnits").isEqualTo(5);

    mountState.notifyUnmount(4);

    assertThat(mountState.getMountItemCount()).describedAs("Number of mounted items").isEqualTo(4);
    assertThat(mountState.getMountItemAt(4)).describedAs("4th MountItem").isNull();
    assertThat(mountState.getRenderUnitCount()).describedAs("Number of RenderUnits").isEqualTo(5);

    mountState.notifyMount(4);

    assertThat(mountState.getMountItemCount()).describedAs("Number of mounted items").isEqualTo(5);
    assertThat(mountState.getMountItemAt(4)).describedAs("4th MountItem").isNotNull();
    assertThat(mountState.getMountItemAt(4).getContent())
        .describedAs("4th item")
        .isInstanceOf(TextView.class);
    assertThat(mountState.getRenderUnitCount()).describedAs("Number of RenderUnits").isEqualTo(5);
  }

  @Test
  public void onMountUnitWithNonHostView_shouldThrowException() {
    mExpectedException.expect(RuntimeException.class);
    mExpectedException.expectMessage(
        "Trying to mount a RenderTreeNode, its parent should be a Host, but was 'LinearLayout'.\n"
            + "Parent RenderUnit: id=1; contentType='class android.widget.LinearLayout'.\n"
            + "Child RenderUnit: id=2; contentType='class android.view.View'.");
    final Context c = mRenderCoreTestRule.getContext();
    final LayoutResult root =
        SimpleLayoutResult.create()
            .renderUnit(new ViewWrapperUnit(new LinearLayout(c), 1))
            .width(100)
            .height(100)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new ViewWrapperUnit(new View(c), 2))
                    .width(100)
                    .height(100))
            .build();

    final RenderTree renderTree = createRenderTree(c, new LayoutResultWrappingNode(root));
    final MountState mountState = createMountState(c);

    mountState.mount(renderTree);
  }

  @Test
  public void onAttachDetachMountState_MountStateShouldCallBindersAndPassBindData() {
    final Context c = RuntimeEnvironment.application;

    final List<Pair<Object, Object>> bindOrder = new ArrayList<>();
    final List<Pair<Object, Object>> unbindOrder = new ArrayList<>();

    TestBinderWithBindData bindBinder = new TestBinderWithBindData<>(bindOrder, unbindOrder, 1);
    TestBinderWithBindData mountBinder = new TestBinderWithBindData<>(bindOrder, unbindOrder, 2);

    final LayoutResult root =
        SimpleLayoutResult.create()
            .width(200)
            .height(200)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(
                        new ViewWrapperUnit(new TextView(c), 1)
                            .addBindBinders(bindBinder)
                            .addMounBinders(mountBinder))
                    .width(100)
                    .height(100))
            .build();

    final RenderTree renderTree = createRenderTree(c, new LayoutResultWrappingNode(root));
    final MountState mountState = createMountState(c);

    mountState.mount(renderTree);

    // assert mount state is correct
    assertThat(mountState.getMountItemCount()).isEqualTo(2);

    // assert that bind was called in correct order and correct bind data was returned
    assertThat(bindOrder).hasSize(2);
    assertThat(bindOrder.get(0).second).isEqualTo(2);
    assertThat(bindOrder.get(1).second).isEqualTo(1);

    mountState.detach();

    // assert that unbind was called in correct order and correct bind data was passed after detach
    assertThat(unbindOrder).hasSize(1);
    assertThat(unbindOrder.get(0).second).isEqualTo(1);

    unbindOrder.clear();
    mountState.unmountAllItems();

    // assert that unbind was called in correct order and correct bind data was passed after unmount
    assertThat(unbindOrder).hasSize(1);
    assertThat(unbindOrder.get(0).second).isEqualTo(2);
  }

  @Test
  public void testBinderUnmountPassBindDataOnUpdateMountItem_whenShouldUpdateReturnsTrue() {
    final Context c = RuntimeEnvironment.application;
    final long id;
    MountState mountState;

    final List<Pair<Object, Object>> bindOrder = new ArrayList<>();
    final List<Pair<Object, Object>> unbindOrder = new ArrayList<>();

    final TestBinderWithBindData1 mountBinder =
        new TestBinderWithBindData1(bindOrder, unbindOrder, 1);
    final TestBinderWithBindData1 attachBinder =
        new TestBinderWithBindData1(bindOrder, unbindOrder, 2);
    {
      final TestNode root = new TestNode();
      final TestNode leaf = new TestNode(0, 0, 10, 10);

      root.addChild(leaf);

      final TestRenderUnit renderUnit = new TestRenderUnit();
      renderUnit.addOptionalMountBinder(createDelegateBinder(renderUnit, mountBinder));
      renderUnit.addAttachBinder(createDelegateBinder(renderUnit, attachBinder));
      id = renderUnit.getId();
      leaf.setRenderUnit(renderUnit);
      RenderTree renderTree = createRenderTree(c, root);
      mountState = createMountState(c);

      mountState.mount(renderTree);

      // assert mount state is correct
      assertThat(mountState.getMountItemCount()).isEqualTo(2);

      // assert that bind was called in correct order and correct bind data was returned
      assertThat(bindOrder).hasSize(2);
      assertThat(bindOrder.get(0).second).isEqualTo(1);
      assertThat(bindOrder.get(1).second).isEqualTo(2);
    }

    bindOrder.clear();
    unbindOrder.clear();
    {
      final TestNode newRoot = new TestNode();
      final TestNode newLeaf = new TestNode(10, 10, 10, 10);
      newLeaf.setLayoutData(new Object());

      newRoot.addChild(newLeaf);

      final TestBinderWithBindData1 mountBinder1 =
          new TestBinderWithBindData1(bindOrder, unbindOrder, 10);
      final TestBinderWithBindData2 mountBinder2 =
          new TestBinderWithBindData2(bindOrder, unbindOrder, 3);

      final TestBinderWithBindData1 attachBinder1 =
          new TestBinderWithBindData1(bindOrder, unbindOrder, 20);
      final TestBinderWithBindData2 attachBinder2 =
          new TestBinderWithBindData2(bindOrder, unbindOrder, 4);
      final TestBinderWithBindData3 attachBinder3 =
          new TestBinderWithBindData3(bindOrder, unbindOrder, 5);

      final TestRenderUnit newRenderUnit = new TestRenderUnit();
      newRenderUnit.addOptionalMountBinders(
          createDelegateBinder(newRenderUnit, mountBinder1),
          createDelegateBinder(newRenderUnit, mountBinder2));
      newRenderUnit.addAttachBinders(
          createDelegateBinder(newRenderUnit, attachBinder1),
          createDelegateBinder(newRenderUnit, attachBinder2),
          createDelegateBinder(newRenderUnit, attachBinder3));

      newRenderUnit.setId(id);
      newLeaf.setRenderUnit(newRenderUnit);

      RenderTree newRenderTree = createRenderTree(c, newRoot);
      mountState.mount(newRenderTree);

      // assert mount state is correct
      assertThat(mountState.getMountItemCount()).isEqualTo(2);

      // assert that unbind was called in correct order and correct bind data was passed after
      // update
      assertThat(unbindOrder).hasSize(2);
      assertThat(unbindOrder.get(0).second).isEqualTo(2);
      assertThat(unbindOrder.get(1).second).isEqualTo(1);

      // assert that bind was called in correct order and correct bind data was returned
      assertThat(bindOrder).hasSize(5);
      assertThat(bindOrder.get(0).second).isEqualTo(10);
      assertThat(bindOrder.get(1).second).isEqualTo(3);
      assertThat(bindOrder.get(2).second).isEqualTo(20);
      assertThat(bindOrder.get(3).second).isEqualTo(4);
      assertThat(bindOrder.get(4).second).isEqualTo(5);
    }
  }

  private static RenderTree createRenderTree(Context c, Node root) {
    final int widthSpec = View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY);
    final int heightSpec = View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY);
    LayoutContext layoutContext = new LayoutContext(c, null, -1, new LayoutCache(), null);
    LayoutResult result = root.calculateLayout(layoutContext, widthSpec, heightSpec);
    return Reducer.getReducedTree(c, result, widthSpec, heightSpec, RenderState.NO_ID, null);
  }

  private static MountState createMountState(Context c) {
    return new MountState(new TestHostView(c));
  }
}
