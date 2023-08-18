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
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.content.Context;
import android.view.View;
import androidx.annotation.Nullable;
import com.facebook.rendercore.extensions.ExtensionState;
import com.facebook.rendercore.extensions.OnItemCallbacks;
import com.facebook.rendercore.extensions.RenderCoreExtension;
import com.facebook.rendercore.testing.TestMountExtension;
import com.facebook.rendercore.testing.TestNode;
import com.facebook.rendercore.testing.TestRenderCoreExtension;
import com.facebook.rendercore.testing.TestRenderUnit;
import com.facebook.rendercore.testing.TestRootHostView;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class MountStateWithDelegateTest {
  @Test
  public void testUnmountAll() {
    final Context c = RuntimeEnvironment.application;

    final TestNode root = new TestNode();
    final TestNode leaf = new TestNode(0, 0, 10, 10);

    root.addChild(leaf);

    final TestRenderUnit testRenderUnitOne = new TestRenderUnit();
    final TestRenderUnit testRenderUnitTwo = new TestRenderUnit();
    final TestBinder testBinderOne = new TestBinder();
    final TestBinder testBinderTwo = new TestBinder();

    testRenderUnitOne.addOptionalMountBinder(
        createDelegateBinder(testRenderUnitOne, testBinderOne));
    testRenderUnitTwo.addOptionalMountBinder(
        createDelegateBinder(testRenderUnitTwo, testBinderTwo));

    leaf.setRenderUnit(testRenderUnitOne);

    final RenderCoreExtension[] extensions = new RenderCoreExtension[1];
    final TestMountExtensionWithAcquire mountExtension = new TestMountExtensionWithAcquire();
    extensions[0] = new TestRenderCoreExtension(mountExtension);

    RenderTree renderTree = createRenderTree(c, root, extensions);

    final MountState mountState = createMountState(c);

    mountState.mount(renderTree);

    long id1 = testRenderUnitOne.getId();

    ExtensionState state = mountState.getMountDelegate().getExtensionStates().get(0);
    mountExtension.acquire(state, id1, 1);
    assertThat(state.ownsReference(id1)).isTrue();

    mountState.unmountAllItems();
    assertThat(state.ownsReference(id1)).isFalse();
    assertThat(mountState.getMountDelegate().getExtensionStates()).isEmpty();

    mountState.mount(renderTree);
    state = mountState.getMountDelegate().getExtensionStates().get(0);

    mountExtension.acquire(state, id1, 1);
    assertThat(state.ownsReference(id1)).isTrue();

    mountState.unmountAllItems();

    assertThat(state.ownsReference(id1)).isFalse();
    assertThat(mountState.getMountDelegate().getExtensionStates()).isEmpty();
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

    final RenderCoreExtension[] extensions = new RenderCoreExtension[1];
    final TestMountExtensionWithAcquire mountExtension =
        new TestMountExtensionWithAcquire(bindOrder, unbindOrder);
    extensions[0] = new TestRenderCoreExtension(mountExtension);

    final RenderTree renderTree = createRenderTree(c, root, extensions);
    final TestRootHostView host = new TestRootHostView(c, bindOrder, unbindOrder);
    final MountState mountState = new MountState(host);

    mountState.mount(renderTree);
    mountState.unmountAllItems();

    assertThat(bindOrder)
        .containsExactly(
            mountExtension, // root mount
            mountExtension, // root bind
            mountBinderOne,
            mountBinderTwo,
            mountExtension,
            host,
            attachBinderOne,
            attachBinderTwo,
            mountExtension);
    assertThat(unbindOrder)
        .containsExactly(
            host,
            mountExtension,
            attachBinderTwo,
            attachBinderOne,
            mountExtension,
            mountBinderTwo,
            mountBinderOne,
            mountExtension, // root unbind
            mountExtension); // root unmount
  }

  @Test
  public void testBinderUnmountCallOrderDuringUpdate() {
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

    final RenderCoreExtension[] extensions = new RenderCoreExtension[1];
    final TestMountExtensionWithAcquire mountExtension =
        new TestMountExtensionWithAcquire(bindOrder, unbindOrder);
    extensions[0] = new TestRenderCoreExtension(mountExtension);

    final RenderTree renderTree = createRenderTree(c, root, extensions);
    final TestRootHostView host = new TestRootHostView(c, bindOrder, unbindOrder);
    final MountState mountState = new MountState(host);

    mountState.mount(renderTree);

    bindOrder.clear();
    unbindOrder.clear();

    // Need to create a new tree to run an update flow.
    final TestNode newRoot = new TestNode();
    final TestNode newLeaf = new TestNode(0, 0, 10, 10);
    newLeaf.setLayoutData(new Object());

    newRoot.addChild(newLeaf);

    // use the same id so that the render unit gets update on it.
    final TestRenderUnit newRenderUnit = new TestRenderUnit(renderUnit.getId());
    newRenderUnit.addAttachBinders(
        createDelegateBinder(newRenderUnit, attachBinderOne),
        createDelegateBinder(newRenderUnit, attachBinderTwo));

    newRenderUnit.addOptionalMountBinders(
        createDelegateBinder(newRenderUnit, mountBinderOne),
        createDelegateBinder(newRenderUnit, mountBinderTwo));

    newLeaf.setRenderUnit(newRenderUnit);

    final RenderTree newRenderTree = createRenderTree(c, newRoot, extensions);

    mountState.mount(newRenderTree);

    assertThat(unbindOrder)
        .containsExactly(
            mountExtension,
            attachBinderTwo,
            attachBinderOne,
            mountExtension,
            mountBinderTwo,
            mountBinderOne);
    assertThat(bindOrder)
        .containsExactly(
            mountBinderOne,
            mountBinderTwo,
            mountExtension,
            attachBinderOne,
            attachBinderTwo,
            mountExtension);
  }

  private static RenderTree createRenderTree(
      Context c, TestNode root, final RenderCoreExtension[] extensions) {
    final int widthSpec = View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY);
    final int heightSpec = View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY);
    LayoutContext layoutContext = new LayoutContext(c, null, -1, new LayoutCache(), extensions);
    LayoutResult result = root.calculateLayout(layoutContext, widthSpec, heightSpec);

    return Reducer.getReducedTree(c, result, widthSpec, heightSpec, RenderState.NO_ID, extensions);
  }

  private static MountState createMountState(Context c) {
    return new MountState(new RootHostView(c));
  }

  private class TestMountExtensionWithAcquire extends TestMountExtension
      implements OnItemCallbacks<Object> {

    private final List bindOrder;
    private final List unbindOrder;

    public TestMountExtensionWithAcquire() {
      this(new ArrayList(), new ArrayList());
    }

    public TestMountExtensionWithAcquire(List bindOrder, List unbindOrder) {
      this.bindOrder = bindOrder;
      this.unbindOrder = unbindOrder;
    }

    public void acquire(ExtensionState state, long id, int position) {
      state.acquireMountReference(id, true);
    }

    public void release(ExtensionState state, long id, int position) {
      state.releaseMountReference(id, true);
    }

    @Override
    public void onUnmount(ExtensionState state) {
      state.releaseAllAcquiredReferences();
    }

    @Override
    public boolean shouldUpdateItem(
        final ExtensionState<Object> extensionState,
        final RenderUnit<?> previousRenderUnit,
        final @Nullable Object previousLayoutData,
        final RenderUnit<?> nextRenderUnit,
        final @Nullable Object nextLayoutData) {
      return true;
    }

    @Override
    public void onBindItem(
        final ExtensionState<Object> extensionState,
        final RenderUnit<?> renderUnit,
        final Object content,
        final @Nullable Object layoutData) {
      bindOrder.add(this);
    }

    @Override
    public void onUnbindItem(
        final ExtensionState<Object> extensionState,
        final RenderUnit<?> renderUnit,
        final Object content,
        final @Nullable Object layoutData) {
      unbindOrder.add(this);
    }

    @Override
    public void onUnmountItem(
        final ExtensionState<Object> extensionState,
        final RenderUnit<?> renderUnit,
        final Object content,
        final @Nullable Object layoutData) {
      unbindOrder.add(this);
    }

    @Override
    public void onMountItem(
        final ExtensionState<Object> extensionState,
        final RenderUnit<?> renderUnit,
        final Object content,
        final @Nullable Object layoutData) {
      bindOrder.add(this);
    }

    @Override
    public void onBoundsAppliedToItem(
        final ExtensionState<Object> extensionState,
        final RenderUnit<?> renderUnit,
        final Object content,
        final @Nullable Object layoutData) {}

    @Override
    public void beforeMountItem(
        ExtensionState<Object> extensionState, RenderTreeNode renderTreeNode, int index) {}
  }
}
