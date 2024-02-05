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

package com.facebook.rendercore

import android.content.Context
import android.graphics.Rect
import android.view.View
import com.facebook.rendercore.Reducer.getReducedTree
import com.facebook.rendercore.RenderUnit.DelegateBinder
import com.facebook.rendercore.extensions.ExtensionState
import com.facebook.rendercore.extensions.OnItemCallbacks
import com.facebook.rendercore.extensions.RenderCoreExtension
import com.facebook.rendercore.testing.TestMountExtension
import com.facebook.rendercore.testing.TestNode
import com.facebook.rendercore.testing.TestRenderCoreExtension
import com.facebook.rendercore.testing.TestRenderUnit
import com.facebook.rendercore.testing.TestRootHostView
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MountStateWithDelegateTest {
  @Test
  fun testUnmountAll() {
    val c: Context = RuntimeEnvironment.application
    val root = TestNode()
    val leaf = TestNode(0, 0, 10, 10)
    root.addChild(leaf)
    val testRenderUnitOne = TestRenderUnit()
    val testRenderUnitTwo = TestRenderUnit()
    val testBinderOne = TestBinder<Any>()
    val testBinderTwo = TestBinder<Any>()
    testRenderUnitOne.addOptionalMountBinder(
        DelegateBinder.createDelegateBinder<Any, View, Any>(testRenderUnitOne, testBinderOne))
    testRenderUnitTwo.addOptionalMountBinder(
        DelegateBinder.createDelegateBinder<Any, View, Any>(testRenderUnitTwo, testBinderTwo))
    leaf.setRenderUnit(testRenderUnitOne)
    val mountExtension = TestMountExtensionWithAcquire()
    val extensions: Array<RenderCoreExtension<*, *>> =
        arrayOf(TestRenderCoreExtension(mountExtension))
    val renderTree = createRenderTree(c, root, extensions)
    val mountState = createMountState(c)
    mountState.mount(renderTree)
    val id1 = testRenderUnitOne.id
    mountState.getMountDelegate()?.extensionStates?.get(0)?.let { state ->
      mountExtension.acquire(state, id1, 1)
      assertThat(state.ownsReference(id1)).isTrue
      mountState.unmountAllItems()
      assertThat(state.ownsReference(id1)).isFalse
      assertThat(mountState.getMountDelegate()?.extensionStates).isNotNull
      assertThat(mountState.getMountDelegate()?.extensionStates).isEmpty()
      mountState.mount(renderTree)
    }
    mountState.getMountDelegate()?.extensionStates?.get(0)?.let { state ->
      mountExtension.acquire(state, id1, 1)
      assertThat(state.ownsReference(id1)).isTrue
      mountState.unmountAllItems()
      assertThat(state.ownsReference(id1)).isFalse
      assertThat(mountState.getMountDelegate()?.extensionStates).isNotNull
      assertThat(mountState.getMountDelegate()?.extensionStates).isEmpty()
    }
  }

  @Test
  fun testBinderUnmountCallOrder() {
    val c: Context = RuntimeEnvironment.application
    val root = TestNode()
    val leaf = TestNode(0, 0, 10, 10)
    root.addChild(leaf)
    val bindOrder: MutableList<Any> = ArrayList()
    val unbindOrder: MutableList<Any> = ArrayList()

    // Using anonymous class to create another type.
    val attachBinderOne = object : TestBinder<Any>(bindOrder, unbindOrder) {}
    val attachBinderTwo = TestBinder<Any>(bindOrder, unbindOrder)
    val renderUnit = TestRenderUnit()
    renderUnit.addAttachBinders(
        DelegateBinder.createDelegateBinder<Any, View, Any>(renderUnit, attachBinderOne),
        DelegateBinder.createDelegateBinder<Any, View, Any>(renderUnit, attachBinderTwo))
    val mountBinderOne = object : TestBinder<Any>(bindOrder, unbindOrder) {}
    val mountBinderTwo = TestBinder<Any>(bindOrder, unbindOrder)
    renderUnit.addOptionalMountBinders(
        DelegateBinder.createDelegateBinder<Any, View, Any>(renderUnit, mountBinderOne),
        DelegateBinder.createDelegateBinder<Any, View, Any>(renderUnit, mountBinderTwo))
    leaf.setRenderUnit(renderUnit)
    val mountExtension = TestMountExtensionWithAcquire(bindOrder, unbindOrder)
    val extensions: Array<RenderCoreExtension<*, *>> =
        arrayOf(TestRenderCoreExtension(mountExtension))
    val renderTree = createRenderTree(c, root, extensions)
    val host = TestRootHostView(c, bindOrder, unbindOrder)
    val mountState = MountState(host)
    mountState.mount(renderTree)
    mountState.unmountAllItems()
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
            mountExtension)
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
            mountExtension) // root unmount
  }

  @Test
  fun testBinderUnmountCallOrderDuringUpdate() {
    val c: Context = RuntimeEnvironment.application
    val root = TestNode()
    val leaf = TestNode(0, 0, 10, 10)
    root.addChild(leaf)
    val bindOrder: MutableList<Any> = ArrayList()
    val unbindOrder: MutableList<Any> = ArrayList()

    // Using anonymous class to create another type.
    val attachBinderOne = object : TestBinder<Any>(bindOrder, unbindOrder) {}
    val attachBinderTwo = TestBinder<Any>(bindOrder, unbindOrder)
    val renderUnit = TestRenderUnit()
    renderUnit.addAttachBinders(
        DelegateBinder.createDelegateBinder<Any, View, Any>(renderUnit, attachBinderOne),
        DelegateBinder.createDelegateBinder<Any, View, Any>(renderUnit, attachBinderTwo))
    val mountBinderOne = object : TestBinder<Any>(bindOrder, unbindOrder) {}
    val mountBinderTwo = TestBinder<Any>(bindOrder, unbindOrder)
    renderUnit.addOptionalMountBinders(
        DelegateBinder.createDelegateBinder<Any, View, Any>(renderUnit, mountBinderOne),
        DelegateBinder.createDelegateBinder<Any, View, Any>(renderUnit, mountBinderTwo))
    leaf.setRenderUnit(renderUnit)
    val mountExtension = TestMountExtensionWithAcquire(bindOrder, unbindOrder)
    val extensions: Array<RenderCoreExtension<*, *>> =
        arrayOf(TestRenderCoreExtension(mountExtension))
    val renderTree = createRenderTree(c, root, extensions)
    val host = TestRootHostView(c, bindOrder, unbindOrder)
    val mountState = MountState(host)
    mountState.mount(renderTree)
    bindOrder.clear()
    unbindOrder.clear()

    // Need to create a new tree to run an update flow.
    val newRoot = TestNode()
    val newLeaf = TestNode(0, 0, 10, 10)
    newLeaf.setLayoutData(Any())
    newRoot.addChild(newLeaf)

    // use the same id so that the render unit gets update on it.
    val newRenderUnit = TestRenderUnit(renderUnit.id)
    newRenderUnit.addAttachBinders(
        DelegateBinder.createDelegateBinder<Any, View, Any>(newRenderUnit, attachBinderOne),
        DelegateBinder.createDelegateBinder<Any, View, Any>(newRenderUnit, attachBinderTwo))
    newRenderUnit.addOptionalMountBinders(
        DelegateBinder.createDelegateBinder<Any, View, Any>(newRenderUnit, mountBinderOne),
        DelegateBinder.createDelegateBinder<Any, View, Any>(newRenderUnit, mountBinderTwo))
    newLeaf.setRenderUnit(newRenderUnit)
    val newRenderTree = createRenderTree(c, newRoot, extensions)
    mountState.mount(newRenderTree)
    assertThat(unbindOrder)
        .containsExactly(
            mountExtension,
            attachBinderTwo,
            attachBinderOne,
            mountExtension,
            mountBinderTwo,
            mountBinderOne)
    assertThat(bindOrder)
        .containsExactly(
            mountBinderOne,
            mountBinderTwo,
            mountExtension,
            attachBinderOne,
            attachBinderTwo,
            mountExtension)
  }

  private inner class TestMountExtensionWithAcquire
  @JvmOverloads
  constructor(
      private val bindOrder: MutableList<Any> = ArrayList(),
      private val unbindOrder: MutableList<Any> = ArrayList()
  ) : TestMountExtension(), OnItemCallbacks<Any?> {
    fun acquire(state: ExtensionState<*>, id: Long, position: Int) {
      state.acquireMountReference(id, true)
    }

    fun release(state: ExtensionState<*>, id: Long, position: Int) {
      state.releaseMountReference(id, true)
    }

    override fun onUnmount(state: ExtensionState<Any>) {
      state.releaseAllAcquiredReferences()
    }

    override fun shouldUpdateItem(
        extensionState: ExtensionState<Any?>,
        previousRenderUnit: RenderUnit<*>,
        previousLayoutData: Any?,
        nextRenderUnit: RenderUnit<*>,
        nextLayoutData: Any?
    ): Boolean {
      return true
    }

    override fun onBindItem(
        extensionState: ExtensionState<Any?>,
        renderUnit: RenderUnit<*>,
        content: Any,
        layoutData: Any?
    ) {
      bindOrder.add(this)
    }

    override fun onUnbindItem(
        extensionState: ExtensionState<Any?>,
        renderUnit: RenderUnit<*>,
        content: Any,
        layoutData: Any?
    ) {
      unbindOrder.add(this)
    }

    override fun onUnmountItem(
        extensionState: ExtensionState<Any?>,
        renderUnit: RenderUnit<*>,
        content: Any,
        layoutData: Any?
    ) {
      unbindOrder.add(this)
    }

    override fun onMountItem(
        extensionState: ExtensionState<Any?>,
        renderUnit: RenderUnit<*>,
        content: Any,
        layoutData: Any?
    ) {
      bindOrder.add(this)
    }

    override fun onBoundsAppliedToItem(
        extensionState: ExtensionState<Any?>,
        renderUnit: RenderUnit<*>,
        content: Any,
        layoutData: Any?
    ) = Unit

    override fun beforeMountItem(
        extensionState: ExtensionState<Any?>,
        renderTreeNode: RenderTreeNode,
        index: Int
    ) = Unit

    override fun beforeMount(state: ExtensionState<Any>, o: Any?, localVisibleRect: Rect?) = Unit
  }

  companion object {
    private fun createRenderTree(
        c: Context,
        root: TestNode,
        extensions: Array<RenderCoreExtension<*, *>>
    ): RenderTree {
      val sizeConstraints = SizeConstraints.exact(200, 200)
      val layoutContext: LayoutContext<*> =
          LayoutContext<Any?>(c, null, -1, LayoutCache(), extensions)
      val result = root.calculateLayout(layoutContext, sizeConstraints)
      return getReducedTree(c, result, sizeConstraints, RenderState.NO_ID, extensions)
    }

    private fun createMountState(c: Context): MountState {
      return MountState(RootHostView(c))
    }
  }
}
