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

package com.facebook.litho

import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.MountDelegate
import com.facebook.rendercore.MountDelegateInput
import com.facebook.rendercore.MountDelegateTarget
import com.facebook.rendercore.RenderCoreSystrace
import com.facebook.rendercore.RenderTreeNode
import com.facebook.rendercore.RenderUnit
import com.facebook.rendercore.incrementalmount.IncrementalMountExtension
import com.facebook.rendercore.incrementalmount.IncrementalMountExtensionInput
import com.facebook.rendercore.incrementalmount.IncrementalMountOutput
import com.facebook.rendercore.testing.DrawableWrapperUnit
import java.util.ArrayList
import java.util.LinkedHashMap
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(LithoTestRunner::class)
class IncrementalMountExtensionTest {

  @Test
  fun onIteratingOnIncrementMountOutputs_shouldIterateByInsertionOrder() {
    val input = TestInput(10)
    input.incrementalMountOutputs.forEachIndexed { i, output ->
      assertThat(output.id).isEqualTo(input.getMountableOutputAt(i).renderUnit.id)
    }
  }

  @Test
  fun testDirtyMountWithEmptyRect() {
    val lithoView = mock<LithoView>()
    whenever(lithoView.height).thenReturn(50)
    val mountDelegate = mock<MountDelegate>()
    val mountDelegateTarget = mock<MountDelegateTarget>()
    whenever(mountDelegate.tracer).thenReturn(RenderCoreSystrace.getInstance())
    whenever(mountDelegate.mountDelegateTarget).thenReturn(mountDelegateTarget)
    val extension = IncrementalMountExtension.getInstance()
    val extensionState = extension.createExtensionState(mountDelegate)
    val state = extensionState.state
    mountDelegate.registerMountExtension(extension)
    val incrementalMountExtensionInput = TestInput(10)
    extension.beforeMount(extensionState, incrementalMountExtensionInput, Rect(0, 0, 10, 50))
    for (i in 0 until incrementalMountExtensionInput.mountableOutputCount) {
      val node = incrementalMountExtensionInput.getMountableOutputAt(i)
      extension.beforeMountItem(extensionState, node, i)
    }
    extension.afterMount(extensionState)
    assertThat(IncrementalMountExtension.getPreviousBottomsIndex(state)).isEqualTo(0)
    assertThat(IncrementalMountExtension.getPreviousTopsIndex(state)).isEqualTo(5)
    val incrementalMountExtensionInput2 = TestInput(3)
    extension.beforeMount(extensionState, incrementalMountExtensionInput2, Rect(0, 0, 0, 0))
    for (i in 0 until incrementalMountExtensionInput2.mountableOutputCount) {
      val node = incrementalMountExtensionInput2.getMountableOutputAt(i)
      extension.beforeMountItem(extensionState, node, i)
    }
    extension.afterMount(extensionState)
    extension.onVisibleBoundsChanged(extensionState, Rect(0, 0, 10, 50))
    assertThat(IncrementalMountExtension.getPreviousBottomsIndex(state)).isEqualTo(0)
    assertThat(IncrementalMountExtension.getPreviousTopsIndex(state)).isEqualTo(3)
  }

  @Test
  fun testDirtyMountWithEmptyRect_leftRightMatch() {
    val lithoView = mock<LithoView>()
    whenever(lithoView.height).thenReturn(50)
    val mountDelegate = mock<MountDelegate>()
    val mountDelegateTarget = mock<MountDelegateTarget>()
    whenever(mountDelegate.tracer).thenReturn(RenderCoreSystrace.getInstance())
    whenever(mountDelegate.mountDelegateTarget).thenReturn(mountDelegateTarget)
    val extension = IncrementalMountExtension.getInstance()
    mountDelegate.registerMountExtension(extension)
    val extensionState = extension.createExtensionState(mountDelegate)
    val state = extensionState.state
    mountDelegate.registerMountExtension(extension)
    val incrementalMountExtensionInput = TestInput(10)
    extension.beforeMount(extensionState, incrementalMountExtensionInput, Rect(0, 0, 10, 50))
    for (i in 0 until incrementalMountExtensionInput.mountableOutputCount) {
      val node = incrementalMountExtensionInput.getMountableOutputAt(i)
      extension.beforeMountItem(extensionState, node, i)
    }
    extension.afterMount(extensionState)
    assertThat(IncrementalMountExtension.getPreviousBottomsIndex(state)).isEqualTo(0)
    assertThat(IncrementalMountExtension.getPreviousTopsIndex(state)).isEqualTo(5)
    val incrementalMountExtensionInput2 = TestInput(3)
    extension.beforeMount(extensionState, incrementalMountExtensionInput2, Rect(0, 0, 10, 0))
    for (i in 0 until incrementalMountExtensionInput2.mountableOutputCount) {
      val node = incrementalMountExtensionInput2.getMountableOutputAt(i)
      extension.beforeMountItem(extensionState, node, i)
    }
    extension.afterMount(extensionState)
    extension.onVisibleBoundsChanged(extensionState, Rect(0, 0, 10, 50))
    assertThat(IncrementalMountExtension.getPreviousBottomsIndex(state)).isEqualTo(0)
    assertThat(IncrementalMountExtension.getPreviousTopsIndex(state)).isEqualTo(3)
  }

  internal inner class TestInput(private val count: Int) :
      IncrementalMountExtensionInput, MountDelegateInput {
    private val mountableOutputs: MutableList<RenderTreeNode> = ArrayList()
    private val _incrementalMountOutputs: MutableMap<Long, IncrementalMountOutput> = LinkedHashMap()
    private val tops: MutableList<IncrementalMountOutput> = ArrayList()
    private val bottoms: MutableList<IncrementalMountOutput> = ArrayList()

    override fun getMountableOutputCount(): Int = count

    override fun getOutputsOrderedByTopBounds(): List<IncrementalMountOutput> = tops

    override fun getOutputsOrderedByBottomBounds(): List<IncrementalMountOutput> = bottoms

    override fun getIncrementalMountOutputForId(id: Long): IncrementalMountOutput? =
        _incrementalMountOutputs[id]

    override fun getIncrementalMountOutputs(): Collection<IncrementalMountOutput> =
        _incrementalMountOutputs.values

    override fun getIncrementalMountOutputCount(): Int = count

    override fun getPositionForId(id: Long): Int = 0

    override fun renderUnitWithIdHostsRenderTrees(id: Long): Boolean = true

    override fun getMountableOutputAt(position: Int): RenderTreeNode = mountableOutputs[position]

    init {
      for (i in 0 until count) {
        val bounds = Rect(0, i * 10, 10, (i + 1) * 10)
        val renderTreeNode = mock<RenderTreeNode>()
        whenever(renderTreeNode.layoutData).thenReturn(mock<LithoAnimtableItem>())
        whenever(renderTreeNode.getAbsoluteBounds(anyOrNull<Rect>())).thenReturn(bounds)
        val renderUnit: RenderUnit<*> = DrawableWrapperUnit(ColorDrawable(Color.BLACK), i.toLong())
        whenever(renderTreeNode.renderUnit).thenReturn(renderUnit)
        mountableOutputs.add(renderTreeNode)
        val incrementalMountOutput =
            IncrementalMountOutput(
                i.toLong(),
                i,
                bounds,
                false,
                if (i != 0) _incrementalMountOutputs[(i - 1).toLong()] else null)
        _incrementalMountOutputs[incrementalMountOutput.id] = incrementalMountOutput
        tops.add(incrementalMountOutput)
        bottoms.add(incrementalMountOutput)
      }
    }
  }
}
