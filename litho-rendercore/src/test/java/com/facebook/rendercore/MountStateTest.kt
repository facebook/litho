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
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.util.Pair
import com.facebook.rendercore.Reducer.getReducedTree
import com.facebook.rendercore.RenderUnit.DelegateBinder
import com.facebook.rendercore.TestBinder.TestBinder1
import com.facebook.rendercore.TestBinder.TestBinder2
import com.facebook.rendercore.TestBinder.TestBinder3
import com.facebook.rendercore.TestBinderWithBindData.TestBinderWithBindData1
import com.facebook.rendercore.TestBinderWithBindData.TestBinderWithBindData2
import com.facebook.rendercore.TestBinderWithBindData.TestBinderWithBindData3
import com.facebook.rendercore.renderunits.HostRenderUnit
import com.facebook.rendercore.testing.LayoutResultWrappingNode
import com.facebook.rendercore.testing.RenderCoreTestRule
import com.facebook.rendercore.testing.SimpleLayoutResult
import com.facebook.rendercore.testing.TestHostRenderUnit
import com.facebook.rendercore.testing.TestHostView
import com.facebook.rendercore.testing.TestNode
import com.facebook.rendercore.testing.TestRenderUnit
import com.facebook.rendercore.testing.ViewWrapperUnit
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MountStateTest {
  @Rule @JvmField val renderCoreTestRule: RenderCoreTestRule = RenderCoreTestRule()
  @Rule @JvmField val expectedException: ExpectedException = ExpectedException.none()

  @Test
  fun testUnmountAll() {
    val c: Context = RuntimeEnvironment.application
    val root = TestNode()
    val leaf = TestNode(0, 0, 10, 10)
    val leafTwo = TestNode(10, 0, 10, 10)
    root.addChild(leaf)
    root.addChild(leafTwo)
    val testRenderUnitOne = TestRenderUnit()
    val testRenderUnitTwo = TestRenderUnit()
    val testBinderOne = TestBinder<Any?>()
    val testBinderTwo = TestBinder<Any?>()
    testRenderUnitOne.addOptionalMountBinder(
        DelegateBinder.createDelegateBinder<Any, View, Void>(testRenderUnitOne, testBinderOne))
    testRenderUnitTwo.addOptionalMountBinder(
        DelegateBinder.createDelegateBinder<Any, View, Void>(testRenderUnitTwo, testBinderTwo))
    leaf.setRenderUnit(testRenderUnitOne)
    leafTwo.setRenderUnit(testRenderUnitTwo)
    val renderTree = createRenderTree(c, root)
    val mountState = createMountState(c)
    mountState.mount(renderTree)
    mountState.unmountAllItems()
    assertThat(testBinderOne.wasBound).isTrue
    assertThat(testBinderTwo.wasBound).isTrue
    assertThat(testBinderOne.wasUnbound).isTrue
    assertThat(testBinderTwo.wasUnbound).isTrue
  }

  @Test
  fun testUnmountAllDoesntTraverseNestedHierarchies() {
    val c: Context = RuntimeEnvironment.application
    val innerTestHost = TestHostView(c)
    val root = TestNode()
    val leaf = TestNode(0, 0, 10, 10)
    val leafTwo = TestNode(10, 0, 10, 10)
    root.addChild(leaf)
    root.addChild(leafTwo)
    val testRenderUnitOne: TestRenderUnit =
        object : TestRenderUnit() {
          override fun createContent(c: Context): View = innerTestHost

          override fun getDescription(): String = "testRenderUnitOne"
        }
    val testBinderOne = TestBinder<Any?>()
    testRenderUnitOne.addOptionalMountBinder(
        DelegateBinder.createDelegateBinder<Any, View, Void>(testRenderUnitOne, testBinderOne))
    leaf.setRenderUnit(testRenderUnitOne)
    val renderTree = createRenderTree(c, root)
    val mountState = createMountState(c)
    val innerTreeRoot = TestNode()
    val innerLeaf = TestNode(0, 0, 10, 10)
    innerTreeRoot.addChild(innerLeaf)
    val innerRenderUnit = TestRenderUnit()
    val innerBinder = TestBinder<Any?>()
    innerRenderUnit.addOptionalMountBinder(
        DelegateBinder.createDelegateBinder<Any, View, Void>(innerRenderUnit, innerBinder))
    innerLeaf.setRenderUnit(innerRenderUnit)
    val innerRenderTree = createRenderTree(c, innerTreeRoot)
    val innerMountState = MountState(innerTestHost)
    mountState.mount(renderTree)
    innerMountState.mount(innerRenderTree)
    mountState.unmountAllItems()
    assertThat(testBinderOne.wasBound).isTrue
    assertThat(innerBinder.wasBound).isTrue
    assertThat(testBinderOne.wasUnbound).isTrue
    assertThat(innerBinder.wasUnbound).isFalse
    innerMountState.unmountAllItems()
    assertThat(innerBinder.wasUnbound).isTrue
  }

  @Test
  fun testBinderUnmountCallOrder() {
    val c: Context = RuntimeEnvironment.application
    val root = TestNode()
    val leaf = TestNode(0, 0, 10, 10)
    root.addChild(leaf)
    val bindOrder: MutableList<*> = ArrayList<Any?>()
    val unbindOrder: MutableList<*> = ArrayList<Any?>()

    // Using anonymous class to create another type.
    val attachBinderOne = object : TestBinder<Any?>(bindOrder, unbindOrder) {}
    val attachBinderTwo = TestBinder<Any?>(bindOrder, unbindOrder)
    val renderUnit = TestRenderUnit()
    renderUnit.addAttachBinders(
        DelegateBinder.createDelegateBinder<Any, View, Void>(renderUnit, attachBinderOne),
        DelegateBinder.createDelegateBinder<Any, View, Void>(renderUnit, attachBinderTwo))
    val mountBinderOne = object : TestBinder<Any?>(bindOrder, unbindOrder) {}
    val mountBinderTwo = TestBinder<Any?>(bindOrder, unbindOrder)
    renderUnit.addOptionalMountBinders(
        DelegateBinder.createDelegateBinder<Any, View, Void>(renderUnit, mountBinderOne),
        DelegateBinder.createDelegateBinder<Any, View, Void>(renderUnit, mountBinderTwo))
    leaf.setRenderUnit(renderUnit)
    val renderTree = createRenderTree(c, root)
    val host = TestHostView(c, bindOrder, unbindOrder)
    val mountState = MountState(host)
    mountState.mount(renderTree)
    mountState.unmountAllItems()
    assertThat(bindOrder)
        .containsExactly(mountBinderOne, mountBinderTwo, host, attachBinderOne, attachBinderTwo)
    assertThat(unbindOrder)
        .containsExactly(host, attachBinderTwo, attachBinderOne, mountBinderTwo, mountBinderOne)
  }

  @Test
  fun testOnUpdateMountItem_whenThereAreNoBinders() {
    val c: Context = RuntimeEnvironment.application
    val id: Long
    var mountState: MountState
    run {
      val root = TestNode()
      val leaf = TestNode(0, 0, 10, 10)
      root.addChild(leaf)
      val renderUnit = TestRenderUnit()
      id = renderUnit.id
      leaf.setRenderUnit(renderUnit)
      val renderTree = createRenderTree(c, root)
      mountState = createMountState(c)
      mountState.mount(renderTree)
    }
    run {
      val newRoot = TestNode()
      val newLeaf = TestNode(10, 10, 10, 10)
      newLeaf.setLayoutData(Any())
      val newRenderUnit = TestRenderUnit()
      newRenderUnit.id = id
      newLeaf.setRenderUnit(newRenderUnit)
      val newRenderTree = createRenderTree(c, newRoot)
      mountState.mount(newRenderTree)
    }
  }

  @Test
  fun testBinderUnmountCallOrderOnUpdateMountItem_whenShouldUpdateReturnsFalse() {
    val c: Context = RuntimeEnvironment.application
    val id: Long
    var mountState: MountState
    run {
      val root = TestNode()
      val leaf = TestNode(0, 0, 10, 10)
      root.addChild(leaf)
      val renderUnit = TestRenderUnit()
      id = renderUnit.id
      leaf.setRenderUnit(renderUnit)
      val renderTree = createRenderTree(c, root)
      mountState = createMountState(c)
      mountState.mount(renderTree)
    }
    run {
      val newRoot = TestNode()
      val newLeaf = TestNode(10, 10, 10, 10)
      newRoot.addChild(newLeaf)
      val bindOrder: MutableList<TestBinder<*>?> = ArrayList()
      val unbindOrder: MutableList<TestBinder<*>?> = ArrayList()
      val mountBinder = TestBinder<Any?>(bindOrder, unbindOrder)
      val attachBinder = TestBinder<Any?>(bindOrder, unbindOrder)
      val newRenderUnit = TestRenderUnit()
      newRenderUnit.addOptionalMountBinder(
          DelegateBinder.createDelegateBinder<Any, View, Void>(newRenderUnit, mountBinder))
      newRenderUnit.addAttachBinder(
          DelegateBinder.createDelegateBinder<Any, View, Void>(newRenderUnit, attachBinder))
      newRenderUnit.id = id
      newLeaf.setRenderUnit(newRenderUnit)
      val newRenderTree = createRenderTree(c, newRoot)
      mountState.mount(newRenderTree)
      assertThat(unbindOrder).isEmpty()
    }
  }

  @Test
  fun testBinderUnmountCallOrderOnUpdateMountItem_whenShouldUpdateReturnsTrue() {
    val c: Context = RuntimeEnvironment.application
    val id: Long
    var mountState: MountState
    val bindOrder: MutableList<TestBinder<*>?> = ArrayList()
    val unbindOrder: MutableList<TestBinder<*>?> = ArrayList()
    val mountBinder = TestBinder1(bindOrder, unbindOrder)
    val attachBinder = TestBinder1(bindOrder, unbindOrder)
    run {
      val root = TestNode()
      val leaf = TestNode(0, 0, 10, 10)
      root.addChild(leaf)
      val renderUnit = TestRenderUnit()
      renderUnit.addOptionalMountBinder(
          DelegateBinder.createDelegateBinder(renderUnit, mountBinder))
      renderUnit.addAttachBinder(DelegateBinder.createDelegateBinder(renderUnit, attachBinder))
      id = renderUnit.id
      leaf.setRenderUnit(renderUnit)
      val renderTree = createRenderTree(c, root)
      mountState = createMountState(c)
      mountState.mount(renderTree)
      assertThat<TestBinder<*>?>(bindOrder).containsExactly(mountBinder, attachBinder)
    }

    // Reset bind/unbind order for next mount.
    bindOrder.clear()
    unbindOrder.clear()
    run {
      val newRoot = TestNode()
      val newLeaf = TestNode(10, 10, 10, 10)
      newLeaf.setLayoutData(Any())
      newRoot.addChild(newLeaf)
      val mountBinder1 = TestBinder1(bindOrder, unbindOrder)
      val mountBinder2 = TestBinder2(bindOrder, unbindOrder)
      val attachBinder1 = TestBinder1(bindOrder, unbindOrder)
      val attachBinder2 = TestBinder2(bindOrder, unbindOrder)
      val attachBinder3 = TestBinder3(bindOrder, unbindOrder)
      val newRenderUnit = TestRenderUnit()
      newRenderUnit.addOptionalMountBinders(
          DelegateBinder.createDelegateBinder(newRenderUnit, mountBinder1),
          DelegateBinder.createDelegateBinder(newRenderUnit, mountBinder2))
      newRenderUnit.addAttachBinders(
          DelegateBinder.createDelegateBinder(newRenderUnit, attachBinder1),
          DelegateBinder.createDelegateBinder(newRenderUnit, attachBinder2),
          DelegateBinder.createDelegateBinder(newRenderUnit, attachBinder3))
      newRenderUnit.id = id
      newLeaf.setRenderUnit(newRenderUnit)
      val newRenderTree = createRenderTree(c, newRoot)
      mountState.mount(newRenderTree)
      assertThat(unbindOrder).containsExactly(attachBinder, mountBinder)
      assertThat<TestBinder<*>?>(bindOrder)
          .containsExactly(mountBinder1, mountBinder2, attachBinder1, attachBinder2, attachBinder3)
    }
  }

  @Test
  fun testUnboundBinderArrayReuseOnUpdateMountItems() {
    val c: Context = RuntimeEnvironment.application
    val id1: Long
    val id2: Long
    var mountState: MountState
    val bindOrder: MutableList<TestBinder<*>?> = ArrayList()
    val unbindOrder: MutableList<TestBinder<*>?> = ArrayList()
    val currentMountBinder1 = TestBinder<Any?>(bindOrder, unbindOrder)
    val currentAttachBinder1 = TestBinder<Any?>(bindOrder, unbindOrder)
    run {
      val root = TestNode()

      // Adds 1st child
      run {
        val leaf1 = TestNode(0, 0, 10, 10)
        val renderUnit1 = TestRenderUnit()
        renderUnit1.addOptionalMountBinder(
            DelegateBinder.createDelegateBinder<Any, View, Void>(renderUnit1, currentMountBinder1))
        renderUnit1.addAttachBinder(
            DelegateBinder.createDelegateBinder<Any, View, Void>(renderUnit1, currentAttachBinder1))
        id1 = renderUnit1.id
        leaf1.setRenderUnit(renderUnit1)
        root.addChild(leaf1)
      }

      // Adds 2nd child
      val mountBinder2 = TestBinder<Any?>(bindOrder, unbindOrder)
      val attachBinder2 = TestBinder<Any?>(bindOrder, unbindOrder)
      run {
        val leaf2 = TestNode(10, 10, 10, 10)
        val renderUnit2 = TestRenderUnit()
        renderUnit2.addOptionalMountBinder(
            DelegateBinder.createDelegateBinder<Any, View, Void>(renderUnit2, mountBinder2))
        renderUnit2.addAttachBinder(
            DelegateBinder.createDelegateBinder<Any, View, Void>(renderUnit2, attachBinder2))
        id2 = renderUnit2.id
        leaf2.setRenderUnit(renderUnit2)
        root.addChild(leaf2)
      }
      val renderTree = createRenderTree(c, root)
      mountState = createMountState(c)
      mountState.mount(renderTree)
      assertThat<TestBinder<*>?>(bindOrder)
          .containsExactly(currentMountBinder1, currentAttachBinder1, mountBinder2, attachBinder2)
    }

    // Reset bind/unbind orders for next mount.
    bindOrder.clear()
    unbindOrder.clear()
    run {
      val newRoot = TestNode()
      val newMountBinder1 = TestBinder<Any?>(bindOrder, unbindOrder)
      val newAttachBinder1 = TestBinder<Any?>(bindOrder, unbindOrder)
      val mountBinder2 = TestBinder<Any?>(bindOrder, unbindOrder)
      val attachBinder2 = TestBinder<Any?>(bindOrder, unbindOrder)

      // Adds 1st new child (should update = true)
      run {
        val newLeaf1 = TestNode(10, 10, 10, 10)
        newLeaf1.setLayoutData(Any()) // makes should update return true
        val newRenderUnit = TestRenderUnit()
        newRenderUnit.addOptionalMountBinder(
            DelegateBinder.createDelegateBinder<Any, View, Void>(newRenderUnit, newMountBinder1))
        newRenderUnit.addAttachBinder(
            DelegateBinder.createDelegateBinder<Any, View, Void>(newRenderUnit, newAttachBinder1))
        newRenderUnit.id = id1
        newLeaf1.setRenderUnit(newRenderUnit)
        newRoot.addChild(newLeaf1)
      }

      // Adds 2nd new child (should update = false)
      run {
        val newLeaf2 = TestNode(10, 10, 10, 10)
        val newRenderUnit = TestRenderUnit()
        newRenderUnit.addOptionalMountBinder(
            DelegateBinder.createDelegateBinder<Any, View, Void>(newRenderUnit, mountBinder2))
        newRenderUnit.addAttachBinder(
            DelegateBinder.createDelegateBinder<Any, View, Void>(newRenderUnit, attachBinder2))
        newRenderUnit.id = id2
        newLeaf2.setRenderUnit(newRenderUnit)
        newRoot.addChild(newLeaf2)
      }
      val newRenderTree = createRenderTree(c, newRoot)
      mountState.mount(newRenderTree)
      assertThat(unbindOrder).containsExactly(currentAttachBinder1, currentMountBinder1)
      assertThat<TestBinder<*>?>(bindOrder).containsExactly(newMountBinder1, newAttachBinder1)
    }
  }

  @Test
  fun onMountUnmountRenderTreeWithBindersForRoot_shouldCallBindersForRoot() {
    val c: Context = RuntimeEnvironment.application
    val node = TestNode(0, 0, 10, 10)
    val unit = TestRenderUnit()
    node.setRenderUnit(unit)
    val tree = createRenderTree(c, node)
    val rootRenderUnit = tree.root.renderUnit
    val bindOrder: MutableList<TestBinder<*>?> = ArrayList()
    val unbindOrder: MutableList<TestBinder<*>?> = ArrayList()
    val mountBinder = TestBinder<Any?>(bindOrder, unbindOrder)
    val attachDetachBinder = TestBinder<Any?>(bindOrder, unbindOrder)
    rootRenderUnit.addOptionalMountBinder(
        DelegateBinder.createDelegateBinder<Any, View, Void>(rootRenderUnit, mountBinder))
    rootRenderUnit.addAttachBinder(
        DelegateBinder.createDelegateBinder<Any, View, Void>(rootRenderUnit, attachDetachBinder))
    val mountState: MountState = createMountState(c)
    mountState.mount(tree)
    assertThat(bindOrder).containsExactly(mountBinder, attachDetachBinder)
    mountState.unmountAllItems()
    assertThat(unbindOrder).containsExactly(attachDetachBinder, mountBinder)
    bindOrder.clear()
    mountState.mount(tree)
    assertThat(bindOrder).containsExactly(mountBinder, attachDetachBinder)
  }

  @Test
  fun testItemsNotMovedWhenPositionInHostDoesntChange() {
    val c: Context = RuntimeEnvironment.application
    val node = TestNode(0, 0, 100, 100)
    val hostNode = TestNode(0, 0, 100, 100)
    val child = TestNode(0, 0, 10, 10)
    val secondChild = TestNode(0, 0, 10, 10)
    val testHost = TestHostView(c)
    val hostRenderUnit: TestHostRenderUnit =
        object : TestHostRenderUnit() {
          override fun createContent(c: Context): Host = testHost

          override fun getDescription(): String = "hostRenderUnit"
        }
    hostNode.setRenderUnit(hostRenderUnit)
    val childRenderUnit = TestRenderUnit()
    child.setRenderUnit(childRenderUnit)
    val secondChildRenderUnit = TestRenderUnit()
    secondChild.setRenderUnit(secondChildRenderUnit)
    node.addChild(hostNode)
    hostNode.addChild(child)
    hostNode.addChild(secondChild)
    val renderTree = createRenderTree(c, node)
    val mountState = createMountState(c)
    mountState.mount(renderTree)
    assertThat(testHost.moveCount).isEqualTo(0)
    val newRoot = TestNode(0, 0, 100, 100)
    val newSibling = TestNode(0, 0, 100, 100)
    val newSiblingRenderUnit = TestRenderUnit()
    newSibling.setRenderUnit(newSiblingRenderUnit)
    newRoot.addChild(newSibling)
    newRoot.addChild(hostNode)
    val secondRenderTree = createRenderTree(c, newRoot)
    mountState.mount(secondRenderTree)
    assertThat(testHost.moveCount).isEqualTo(0)
  }

  @Test
  fun onMountEmptyRenderTree_MountStateShouldHostExpectedState() {
    val c: Context = RuntimeEnvironment.application
    val mountState = createMountState(c)
    assertThat(mountState.getMountItemCount()).describedAs("Number of mounted items").isEqualTo(0)
    assertThat(mountState.isRootItem(0)).describedAs("No item should not be root").isFalse
    assertThat(mountState.isRootItem(1)).describedAs("No item should not be root").isFalse
    assertThat(mountState.getMountItemAt(0)).describedAs("Root item").isNull()
    assertThat(mountState.getContentAt(1)).describedAs("1st item").isNull()
    assertThat(mountState.getContentAt(3)).describedAs("3rd item").isNull()
    assertThat(mountState.getContentById(1)).describedAs("1st item").isNull()
    assertThat(mountState.getContentById(3)).describedAs("3rd item").isNull()
    assertThat(mountState.getHosts()).describedAs("Number of Hosts").hasSize(0)
    val tree =
        createRenderTree(
            c, LayoutResultWrappingNode(SimpleLayoutResult.create().width(100).height(100).build()))
    mountState.mount(tree)
    assertThat(mountState.getMountItemCount()).describedAs("Number of mounted items").isEqualTo(1)
    assertThat(mountState.isRootItem(0)).describedAs("0th item should be root").isTrue
    assertThat(mountState.isRootItem(1)).describedAs("1st item should be root").isFalse
    assertThat(mountState.getMountItemAt(0)).describedAs("Root item").isNotNull
    assertThat(mountState.getContentAt(0)).describedAs("0th item").isNotNull
    assertThat(mountState.getContentAt(3)).describedAs("3rd item").isNull()
    assertThat(mountState.getContentById(0)).describedAs("0th item").isNotNull
    assertThat(mountState.getContentById(3)).describedAs("3rd item").isNull()
    assertThat(mountState.getHosts()).describedAs("Number of Hosts").hasSize(1)
  }

  @Test
  fun onMountNestedRenderTree_MountStateShouldHostExpectedState() {
    val c: Context = RuntimeEnvironment.application
    val root: LayoutResult? =
        SimpleLayoutResult.create()
            .width(1000)
            .height(1000)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(ViewWrapperUnit(TextView(c), 1))
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .y(100)
                    .width(400)
                    .height(400)
                    .child(
                        SimpleLayoutResult.create()
                            .renderUnit(ViewWrapperUnit(TextView(c), 2))
                            .x(100)
                            .y(100)
                            .width(100)
                            .height(100))
                    .child(
                        SimpleLayoutResult.create()
                            .renderUnit(HostRenderUnit(3))
                            .x(200)
                            .y(200)
                            .width(200)
                            .height(200)
                            .child(
                                SimpleLayoutResult.create()
                                    .renderUnit(ViewWrapperUnit(TextView(c), 4))
                                    .x(100)
                                    .y(100)
                                    .width(100)
                                    .height(100))))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(HostRenderUnit(5))
                    .y(400)
                    .width(400)
                    .height(400)
                    .child(
                        SimpleLayoutResult.create()
                            .renderUnit(ViewWrapperUnit(TextView(c), 6))
                            .x(100)
                            .y(100)
                            .width(100)
                            .height(100))
                    .child(
                        SimpleLayoutResult.create()
                            .renderUnit(HostRenderUnit(7))
                            .x(200)
                            .y(200)
                            .width(200)
                            .height(200)
                            .child(
                                SimpleLayoutResult.create()
                                    .renderUnit(ViewWrapperUnit(TextView(c), 8))
                                    .x(100)
                                    .y(100)
                                    .width(100)
                                    .height(100))))
            .build()
    val renderTree = createRenderTree(c, LayoutResultWrappingNode(root))
    val mountState = createMountState(c)
    mountState.mount(renderTree)
    assertThat(mountState.needsRemount()).describedAs("Needs Remount").isFalse
    assertThat(mountState.getMountItemCount()).describedAs("Number of mounted items").isEqualTo(9)
    assertThat(mountState.isRootItem(0)).describedAs("0th item should be root").isTrue
    assertThat(mountState.isRootItem(1)).describedAs("1st item should not be root").isFalse
    assertThat(mountState.getMountItemAt(0)).describedAs("root item").isNotNull
    assertThat(mountState.getContentAt(1))
        .describedAs("1st item")
        .isInstanceOf(TextView::class.java)
    assertThat(mountState.getContentAt(3)).describedAs("3rd item").isInstanceOf(Host::class.java)
    assertThat(mountState.getContentById(1))
        .describedAs("1st item")
        .isInstanceOf(TextView::class.java)
    assertThat(mountState.getContentById(3)).describedAs("3rd item").isInstanceOf(Host::class.java)
    assertThat(mountState.getHosts()).describedAs("Number of Hosts").hasSize(4)
    mountState.unmountAllItems()
    assertThat(mountState.needsRemount()).describedAs("Needs Remount").isTrue
    assertThat(mountState.getMountItemCount()).describedAs("Number of mounted items").isEqualTo(0)
    assertThat(mountState.isRootItem(0)).describedAs("No item should not be root").isFalse
    assertThat(mountState.isRootItem(1)).describedAs("No item should not be root").isFalse
    assertThat(mountState.getMountItemAt(0)).describedAs("Root item").isNull()
    assertThat(mountState.getContentAt(1)).describedAs("1st item").isNull()
    assertThat(mountState.getContentAt(3)).describedAs("3rd item").isNull()
    assertThat(mountState.getContentById(1)).describedAs("1st item").isNull()
    assertThat(mountState.getContentById(3)).describedAs("3rd item").isNull()
    assertThat(mountState.getHosts()).describedAs("Number of Hosts").hasSize(0)
  }

  @Suppress("UNCHECKED_CAST")
  @Test
  fun onAttachDetachMountState_MountStateShouldCallBinders() {
    val c: Context = RuntimeEnvironment.application
    val bindOrder: MutableList<TestBinder<*>?> = ArrayList()
    val unbindOrder: MutableList<TestBinder<*>?> = ArrayList()
    val bindBinder = TestBinder<Any?>(bindOrder, unbindOrder)
    val mountBinder = TestBinder<Any?>(bindOrder, unbindOrder)
    val root: LayoutResult? =
        SimpleLayoutResult.create()
            .width(200)
            .height(200)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(
                        ViewWrapperUnit(TextView(c), 1)
                            .addBindBinders(
                                bindBinder as RenderUnit.Binder<ViewWrapperUnit, View, Void>)
                            .addMounBinders(
                                mountBinder as RenderUnit.Binder<ViewWrapperUnit, View, Void>))
                    .width(100)
                    .height(100))
            .build()
    val renderTree = createRenderTree(c, LayoutResultWrappingNode(root))
    val mountState = createMountState(c)
    mountState.mount(renderTree)
    mountState.detach()
    assertThat(bindOrder).containsExactly(mountBinder, bindBinder)
    assertThat(unbindOrder).containsExactly(bindBinder)
    bindOrder.clear()
    mountState.attach()
    assertThat(bindOrder).containsExactly(bindBinder)
    assertThat(mountState.getMountItemCount()).isEqualTo(2)
    val item = mountState.getMountItemAt(0)
    assertThat(item).isNotNull
    mountState.unbindMountItem(item!!)
    assertThat(mountState.getMountItemCount()).isEqualTo(2)
    assertThat(bindOrder).containsExactly(bindBinder)
  }

  @Suppress("UNCHECKED_CAST")
  @Test
  fun onAttachDetachRootHostToWindow_mountStateCallsAttachDetachBinders() {
    val c = renderCoreTestRule.context
    val bindOrder: MutableList<TestBinder<Any?>?> = ArrayList()
    val unbindOrder: MutableList<TestBinder<Any?>?> = ArrayList()
    val attachBinder = TestBinder<Any?>(bindOrder, unbindOrder)
    val mountBinder = TestBinder<Any?>(bindOrder, unbindOrder)
    val root: LayoutResult? =
        SimpleLayoutResult.create()
            .width(200)
            .height(200)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(
                        ViewWrapperUnit(TextView(c), 1)
                            .addBindBinders(
                                attachBinder as RenderUnit.Binder<ViewWrapperUnit, View, Void>)
                            .addMounBinders(
                                mountBinder as RenderUnit.Binder<ViewWrapperUnit, View, Void>))
                    .width(100)
                    .height(100))
            .build()
    val rootHost = RootHostView(c)
    renderCoreTestRule.useRootHost(rootHost).useRootNode(LayoutResultWrappingNode(root)).render()

    // Should bind both the mount/unmount and attach/detach binders
    assertThat(bindOrder).containsExactly(mountBinder, attachBinder)

    // No unbinds
    assertThat(unbindOrder).describedAs("no binders should be unbound").isEmpty()
    bindOrder.clear()
    rootHost.onDetachedFromWindow()

    // Should unbind the attach/detach binder
    assertThat(unbindOrder).containsExactly(attachBinder)

    // Should still have the view mounted
    assertThat(rootHost.childCount).describedAs("should still have content mounted").isEqualTo(1)
    unbindOrder.clear()
    rootHost.onAttachedToWindow()

    // Should bind the attach/detach binder again
    assertThat(bindOrder).containsExactly(attachBinder)
  }

  @Suppress("UNCHECKED_CAST")
  @Test
  fun onAttachDetachRenderTreeHostToWindow_mountStateCallsAttachDetachBinders() {
    val c = renderCoreTestRule.context
    val bindOrder: MutableList<TestBinder<*>?> = ArrayList()
    val unbindOrder: MutableList<TestBinder<*>?> = ArrayList()
    val attachBinder = TestBinder<Any?>(bindOrder, unbindOrder)
    val mountBinder = TestBinder<Any?>(bindOrder, unbindOrder)
    val root: LayoutResult? =
        SimpleLayoutResult.create()
            .width(200)
            .height(200)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(
                        ViewWrapperUnit(TextView(c), 1)
                            .addBindBinders(
                                attachBinder as RenderUnit.Binder<ViewWrapperUnit, View, Void>)
                            .addMounBinders(
                                mountBinder as RenderUnit.Binder<ViewWrapperUnit, View, Void>))
                    .width(100)
                    .height(100))
            .build()
    val rootHost = RenderTreeHostView(c)
    renderCoreTestRule
        .useRenderTreeHost(rootHost)
        .useRootNode(LayoutResultWrappingNode(root))
        .renderWithRenderTreeHost()

    // Should bind both the mount/unmount and attach/detach binders
    assertThat(bindOrder).containsExactly(mountBinder, attachBinder)

    // No unbinds
    assertThat(unbindOrder).describedAs("no binders should be unbound").isEmpty()
    bindOrder.clear()
    rootHost.onDetachedFromWindow()

    // Should unbind the attach/detach binder
    assertThat(unbindOrder).containsExactly(attachBinder)

    // Should still have the view mounted
    assertThat(rootHost.childCount).describedAs("should still have content mounted").isEqualTo(1)
    unbindOrder.clear()
    rootHost.onAttachedToWindow()

    // Should bind the attach/detach binder again
    assertThat(bindOrder).containsExactly(attachBinder)
  }

  @Test
  fun onNotifyMountUnmount_ShouldUpdateMountItemCount() {
    val c: Context = RuntimeEnvironment.application
    val root: LayoutResult? =
        SimpleLayoutResult.create()
            .width(1000)
            .height(1000)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(ViewWrapperUnit(TextView(c), 1))
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(ViewWrapperUnit(TextView(c), 2))
                    .x(200)
                    .y(200)
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(ViewWrapperUnit(TextView(c), 3))
                    .x(300)
                    .y(300)
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(ViewWrapperUnit(TextView(c), 4))
                    .x(400)
                    .y(400)
                    .width(100)
                    .height(100))
            .build()
    val renderTree = createRenderTree(c, LayoutResultWrappingNode(root))
    val mountState = createMountState(c)
    mountState.mount(renderTree)
    assertThat(mountState.needsRemount()).describedAs("Needs Remount").isFalse
    assertThat(mountState.getMountItemCount()).describedAs("Number of mounted items").isEqualTo(5)
    assertThat(mountState.getContentById(1))
        .describedAs("Item with id 1")
        .isInstanceOf(TextView::class.java)
    assertThat(mountState.getContentById(4))
        .describedAs("Item with id 4")
        .isInstanceOf(TextView::class.java)
    assertThat(mountState.getMountItemAt(4)).describedAs("4th MountItem").isNotNull
    assertThat(mountState.getMountItemAt(4)?.content)
        .describedAs("4th item")
        .isInstanceOf(TextView::class.java)
    assertThat(mountState.getRenderUnitCount()).describedAs("Number of RenderUnits").isEqualTo(5)
    mountState.notifyUnmount(4)
    assertThat(mountState.getMountItemCount()).describedAs("Number of mounted items").isEqualTo(4)
    assertThat(mountState.getMountItemAt(4)).describedAs("4th MountItem").isNull()
    assertThat(mountState.getRenderUnitCount()).describedAs("Number of RenderUnits").isEqualTo(5)
    mountState.notifyMount(4)
    assertThat(mountState.getMountItemCount()).describedAs("Number of mounted items").isEqualTo(5)
    assertThat(mountState.getMountItemAt(4)).describedAs("4th MountItem").isNotNull
    assertThat(mountState.getMountItemAt(4)?.content)
        .describedAs("4th item")
        .isInstanceOf(TextView::class.java)
    assertThat(mountState.getRenderUnitCount()).describedAs("Number of RenderUnits").isEqualTo(5)
  }

  @Test
  fun onMountUnitWithNonHostView_shouldThrowException() {
    expectedException.expect(RuntimeException::class.java)
    expectedException.expectMessage(
        """
              Trying to mount a RenderTreeNode, its parent should be a Host, but was 'LinearLayout'.
              Parent RenderUnit: id=1; contentType='class android.widget.LinearLayout'.
              Child RenderUnit: id=2; contentType='class android.view.View'.
              """
            .trimIndent())
    val c = renderCoreTestRule.context
    val root: LayoutResult? =
        SimpleLayoutResult.create()
            .renderUnit(ViewWrapperUnit(LinearLayout(c), 1))
            .width(100)
            .height(100)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(ViewWrapperUnit(View(c), 2))
                    .width(100)
                    .height(100))
            .build()
    val renderTree = createRenderTree(c, LayoutResultWrappingNode(root))
    val mountState = createMountState(c)
    mountState.mount(renderTree)
  }

  @Suppress("UNCHECKED_CAST")
  @Test
  fun onAttachDetachMountState_MountStateShouldCallBindersAndPassBindData() {
    val c: Context = RuntimeEnvironment.application
    val bindOrder: MutableList<Pair<Any?, Any?>?> = ArrayList()
    val unbindOrder: MutableList<Pair<Any?, Any?>?> = ArrayList()
    val bindBinder = TestBinderWithBindData<Any?>(bindOrder, unbindOrder, 1)
    val mountBinder = TestBinderWithBindData<Any?>(bindOrder, unbindOrder, 2)
    val root: LayoutResult? =
        SimpleLayoutResult.create()
            .width(200)
            .height(200)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(
                        ViewWrapperUnit(TextView(c), 1)
                            .addBindBinders(
                                bindBinder as RenderUnit.Binder<ViewWrapperUnit, View, Void>)
                            .addMounBinders(
                                mountBinder as RenderUnit.Binder<ViewWrapperUnit, View, Void>))
                    .width(100)
                    .height(100))
            .build()
    val renderTree = createRenderTree(c, LayoutResultWrappingNode(root))
    val mountState = createMountState(c)
    mountState.mount(renderTree)

    // assert mount state is correct
    assertThat(mountState.getMountItemCount()).isEqualTo(2)

    // assert that bind was called in correct order and correct bind data was returned
    assertThat(bindOrder).hasSize(2)
    assertThat(bindOrder[0]?.second).isEqualTo(2)
    assertThat(bindOrder[1]?.second).isEqualTo(1)
    mountState.detach()

    // assert that unbind was called in correct order and correct bind data was passed after detach
    assertThat(unbindOrder).hasSize(1)
    assertThat(unbindOrder[0]?.second).isEqualTo(1)
    unbindOrder.clear()
    mountState.unmountAllItems()

    // assert that unbind was called in correct order and correct bind data was passed after unmount
    assertThat(unbindOrder).hasSize(1)
    assertThat(unbindOrder[0]?.second).isEqualTo(2)
  }

  @Test
  fun testBinderUnmountPassBindDataOnUpdateMountItem_whenShouldUpdateReturnsTrue() {
    val c: Context = RuntimeEnvironment.application
    val id: Long
    var mountState: MountState
    val bindOrder: MutableList<Pair<Any?, Any?>?> = ArrayList()
    val unbindOrder: MutableList<Pair<Any?, Any?>?> = ArrayList()
    val mountBinder = TestBinderWithBindData1(bindOrder, unbindOrder, 1)
    val attachBinder = TestBinderWithBindData1(bindOrder, unbindOrder, 2)
    run {
      val root = TestNode()
      val leaf = TestNode(0, 0, 10, 10)
      root.addChild(leaf)
      val renderUnit = TestRenderUnit()
      renderUnit.addOptionalMountBinder(
          DelegateBinder.createDelegateBinder(renderUnit, mountBinder))
      renderUnit.addAttachBinder(DelegateBinder.createDelegateBinder(renderUnit, attachBinder))
      id = renderUnit.id
      leaf.setRenderUnit(renderUnit)
      val renderTree = createRenderTree(c, root)
      mountState = createMountState(c)
      mountState.mount(renderTree)

      // assert mount state is correct
      assertThat(mountState.getMountItemCount()).isEqualTo(2)

      // assert that bind was called in correct order and correct bind data was returned
      assertThat(bindOrder).hasSize(2)
      assertThat(bindOrder[0]?.second).isEqualTo(1)
      assertThat<Any?>(bindOrder[1]?.second).isEqualTo(2)
    }
    bindOrder.clear()
    unbindOrder.clear()
    run {
      val newRoot = TestNode()
      val newLeaf = TestNode(10, 10, 10, 10)
      newLeaf.setLayoutData(Any())
      newRoot.addChild(newLeaf)
      val mountBinder1 = TestBinderWithBindData1(bindOrder, unbindOrder, 10)
      val mountBinder2 = TestBinderWithBindData2(bindOrder, unbindOrder, 3)
      val attachBinder1 = TestBinderWithBindData1(bindOrder, unbindOrder, 20)
      val attachBinder2 = TestBinderWithBindData2(bindOrder, unbindOrder, 4)
      val attachBinder3 = TestBinderWithBindData3(bindOrder, unbindOrder, 5)
      val newRenderUnit = TestRenderUnit()
      newRenderUnit.addOptionalMountBinders(
          DelegateBinder.createDelegateBinder(newRenderUnit, mountBinder1),
          DelegateBinder.createDelegateBinder(newRenderUnit, mountBinder2))
      newRenderUnit.addAttachBinders(
          DelegateBinder.createDelegateBinder(newRenderUnit, attachBinder1),
          DelegateBinder.createDelegateBinder(newRenderUnit, attachBinder2),
          DelegateBinder.createDelegateBinder(newRenderUnit, attachBinder3))
      newRenderUnit.id = id
      newLeaf.setRenderUnit(newRenderUnit)
      val newRenderTree = createRenderTree(c, newRoot)
      mountState.mount(newRenderTree)

      // assert mount state is correct
      assertThat(mountState.getMountItemCount()).isEqualTo(2)

      // assert that unbind was called in correct order and correct bind data was passed after
      // update
      assertThat(unbindOrder).hasSize(2)
      assertThat(unbindOrder[0]?.second).isEqualTo(2)
      assertThat(unbindOrder[1]?.second).isEqualTo(1)

      // assert that bind was called in correct order and correct bind data was returned
      assertThat(bindOrder).hasSize(5)
      assertThat(bindOrder[0]?.second).isEqualTo(10)
      assertThat(bindOrder[1]?.second).isEqualTo(3)
      assertThat(bindOrder[2]?.second).isEqualTo(20)
      assertThat(bindOrder[3]?.second).isEqualTo(4)
      assertThat<Any?>(bindOrder[4]?.second).isEqualTo(5)
    }
  }

  companion object {
    private fun createRenderTree(c: Context, root: Node<Any?>): RenderTree {
      val sizeConstraints = SizeConstraints.exact(200, 200)
      val layoutContext = LayoutContext<Any?>(c, null, -1, LayoutCache(), null)
      val result = root.calculateLayout(layoutContext, sizeConstraints)
      return getReducedTree(c, result, sizeConstraints, RenderState.NO_ID, null)
    }

    private fun createMountState(c: Context): MountState {
      return MountState(TestHostView(c))
    }
  }
}
