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
import com.facebook.rendercore.Reducer.getReducedTree
import com.facebook.rendercore.extensions.RenderCoreExtension
import com.facebook.rendercore.testing.TestLayoutResultVisitor
import com.facebook.rendercore.testing.TestNode
import com.facebook.rendercore.testing.TestRenderCoreExtension
import com.facebook.rendercore.testing.TestRenderUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ReducerTest {

  @Test
  fun testHostFlattening() {
    val root = TestNode()
    val one = TestNode()
    val two = TestNode()
    val leaf = TestNode()
    val leafTwo = TestNode()
    root.addChild(one)
    one.addChild(two)
    two.addChild(leaf)
    root.addChild(leafTwo)
    leaf.setRenderUnit(TestRenderUnit())
    leafTwo.setRenderUnit(TestRenderUnit())
    val c: Context = RuntimeEnvironment.getApplication()
    val sizeConstraints = SizeConstraints.exact(100, 100)
    val layoutContext: LayoutContext<*> = LayoutContext<Any?>(c, null, -1, LayoutCache(), null)
    val result = root.calculateLayout(layoutContext, sizeConstraints)
    val renderTree = getReducedTree(layoutContext, result, sizeConstraints, RenderState.NO_ID)

    // We expect one RenderUnit for each of the leaves and one for the root.
    assertThat(renderTree.mountableOutputCount).isEqualTo(3)
  }

  @Test
  fun testViewTranslation() {
    val root = TestNode(0, 0, 200, 200)
    val leaf = TestNode(0, 0, 200, 100)
    val leafTwo = TestNode(0, 100, 200, 100)
    root.addChild(leaf)
    root.addChild(leafTwo)
    leaf.setRenderUnit(TestRenderUnit())
    leafTwo.setRenderUnit(TestRenderUnit())
    val c: Context = RuntimeEnvironment.getApplication()
    val sizeConstraints = SizeConstraints.exact(200, 200)
    val layoutContext: LayoutContext<*> = LayoutContext<Any?>(c, null, -1, LayoutCache(), null)
    val result = root.calculateLayout(layoutContext, sizeConstraints)
    val renderTree = getReducedTree(layoutContext, result, sizeConstraints, RenderState.NO_ID)

    // We expect one RenderUnit for each of the leaves, one for the root and one for the Host.
    assertThat(renderTree.mountableOutputCount).isEqualTo(3)
    assertThat(renderTree.getRenderTreeNodeAtIndex(0).bounds).isEqualTo(Rect(0, 0, 200, 200))
    assertThat(renderTree.getRenderTreeNodeAtIndex(1).bounds).isEqualTo(Rect(0, 0, 200, 100))
    assertThat(renderTree.getRenderTreeNodeAtIndex(2).bounds).isEqualTo(Rect(0, 100, 200, 200))
  }

  @Test
  fun whenReducedWithExtensions_shouldRunLayoutResultVisitors() {
    val root = TestNode(0, 0, 200, 200)
    val leaf = TestNode(0, 0, 200, 100)
    val leafTwo = TestNode(0, 100, 200, 100)
    root.addChild(leaf)
    root.addChild(leafTwo)
    leaf.setRenderUnit(TestRenderUnit())
    leafTwo.setRenderUnit(TestRenderUnit())
    val c: Context = RuntimeEnvironment.getApplication()
    val sizeConstraints = SizeConstraints.exact(200, 200)
    val e1: RenderCoreExtension<*, *> = TestRenderCoreExtension()
    val e2: RenderCoreExtension<*, *> = RenderCoreExtension<Any?, Any?>()
    val e3: RenderCoreExtension<*, *> = TestRenderCoreExtension(TestLayoutResultVisitor(), null)
    val extensions = arrayOf(e1, e2, e3)
    val layoutContext: LayoutContext<*> =
        LayoutContext<Any?>(c, null, -1, LayoutCache(), extensions)
    val result = root.calculateLayout(layoutContext, sizeConstraints)
    val renderTree = getReducedTree(layoutContext, result, sizeConstraints, RenderState.NO_ID)
    val results = renderTree.extensionResults
    assertThat(results).isNotNull
    assertThat(results).hasSize(3)
    results?.forEach { r ->
      if (r.first == e1) {
        assertThat(r.second).isNotNull
        assertThat(r.second).isInstanceOf(MutableList::class.java)
        assertThat(r.second as List<*>).hasSize(4)
      } else if (r.first == e2 || r.first == e3) {
        assertThat(r.second).isNull()
      } else {
        throw AssertionError("Unexpected extension found")
      }
    }
  }

  @Test
  fun whenReducedWithZeroSizedNonLeafNode_shouldRetainSubtree() {
    val root = TestNode(0, 0, 200, 200)
    val zeroSizedNonLeafNode = TestNode(0, 0, 0, 0)
    val leaf = TestNode(0, 0, 200, 100)
    val leafTwo = TestNode(0, 100, 200, 100)
    root.addChild(zeroSizedNonLeafNode)
    zeroSizedNonLeafNode.addChild(leaf)
    zeroSizedNonLeafNode.addChild(leafTwo)
    val idLeaf = 24
    val idLeafTwo = 25
    leaf.setRenderUnit(TestRenderUnit(idLeaf.toLong()))
    leafTwo.setRenderUnit(TestRenderUnit(idLeafTwo.toLong()))
    val c: Context = RuntimeEnvironment.getApplication()
    val sizeConstraints = SizeConstraints.exact(200, 200)
    val layoutContext: LayoutContext<*> = LayoutContext<Any?>(c, null, -1, LayoutCache(), null)
    val result = root.calculateLayout(layoutContext, sizeConstraints)
    val renderTree = getReducedTree(layoutContext, result, sizeConstraints, RenderState.NO_ID)

    // The root node and the two leaf nodes should be present in the mountable output.
    assertThat(renderTree.mountableOutputCount).isEqualTo(3)
    assertThat(renderTree.getRenderTreeNodeAtIndex(1).renderUnit.id).isEqualTo(idLeaf.toLong())
    assertThat(renderTree.getRenderTreeNodeAtIndex(2).renderUnit.id).isEqualTo(idLeafTwo.toLong())
  }
}
