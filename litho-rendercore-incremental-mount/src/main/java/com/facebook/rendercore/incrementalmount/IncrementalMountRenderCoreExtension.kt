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

package com.facebook.rendercore.incrementalmount

import android.graphics.Rect
import androidx.collection.MutableLongSet
import com.facebook.rendercore.BinderKey
import com.facebook.rendercore.ClassBinderKey
import com.facebook.rendercore.LayoutResult
import com.facebook.rendercore.RenderCoreConfig
import com.facebook.rendercore.RenderTreeNode
import com.facebook.rendercore.RenderUnit
import com.facebook.rendercore.extensions.LayoutResultVisitor
import com.facebook.rendercore.extensions.MountExtension
import com.facebook.rendercore.extensions.RenderCoreExtension
import com.facebook.rendercore.incrementalmount.IncrementalMountExtension.Companion.instance
import com.facebook.rendercore.incrementalmount.IncrementalMountExtension.IncrementalMountExtensionState
import java.util.SortedSet
import java.util.TreeSet
import kotlin.math.max

class IncrementalMountRenderCoreExtension
@JvmOverloads
constructor(shouldMountZeroBoundsContent: Boolean = false) :
    RenderCoreExtension<IncrementalMountExtensionInput, IncrementalMountExtensionState>() {

  private val layoutResultVisitor: Visitor = Visitor(shouldMountZeroBoundsContent)

  override fun getLayoutVisitor(): LayoutResultVisitor<Results> {
    return layoutResultVisitor
  }

  override fun getMountExtension():
      MountExtension<IncrementalMountExtensionInput, IncrementalMountExtensionState> {
    return mountExtension
  }

  override fun createInput(): Results? {
    return Results()
  }

  class Results : IncrementalMountExtensionInput {
    private val outputs = LinkedHashMap<Long, IncrementalMountOutput>(8)
    private val outputsOrderedByTopBounds: SortedSet<IncrementalMountOutput> =
        TreeSet<IncrementalMountOutput>(TopsComparator)
    private val outputsOrderedByBottomBounds: SortedSet<IncrementalMountOutput> =
        TreeSet<IncrementalMountOutput>(BottomsComparator)
    private val renderUnitIdsWhichHostRenderTrees: MutableLongSet = MutableLongSet(4)

    private var outputsOrderedByTopBoundsList: MutableList<IncrementalMountOutput>? = null
    private var outputsOrderedByBottomBoundsList: MutableList<IncrementalMountOutput>? = null

    override fun getOutputsOrderedByTopBounds(): List<IncrementalMountOutput> {
      maybeInitializeList()
      return requireNotNull(outputsOrderedByTopBoundsList)
    }

    override fun getOutputsOrderedByBottomBounds(): List<IncrementalMountOutput> {
      maybeInitializeList()
      return requireNotNull(outputsOrderedByBottomBoundsList)
    }

    override fun getIncrementalMountOutputForId(id: Long): IncrementalMountOutput? {
      return outputs.get(id)
    }

    override fun getIncrementalMountOutputs(): Collection<IncrementalMountOutput> {
      return outputs.values
    }

    override fun getIncrementalMountOutputCount(): Int {
      return outputs.size
    }

    override fun renderUnitWithIdHostsRenderTrees(id: Long): Boolean {
      return renderUnitIdsWhichHostRenderTrees.contains(id)
    }

    fun addOutput(output: IncrementalMountOutput) {
      val existing = outputs.put(output.id, output)
      require(existing == null) {
        ("output with id=${output.id} already exists.\nindex=${existing?.index}${if (existing?.hostOutput != null) "\nhostId=" + existing.hostOutput.id else ""}\nbounds=${existing?.bounds}")
      }
      outputsOrderedByTopBounds.add(output)
      outputsOrderedByBottomBounds.add(output)
    }

    fun addRenderTreeHostId(id: Long) {
      renderUnitIdsWhichHostRenderTrees.add(id)
    }

    private fun maybeInitializeList() {
      if (outputsOrderedByTopBoundsList == null || outputsOrderedByBottomBoundsList == null) {
        outputsOrderedByTopBoundsList =
            ArrayList<IncrementalMountOutput>(outputsOrderedByTopBounds.size)
        outputsOrderedByBottomBoundsList =
            ArrayList<IncrementalMountOutput>(outputsOrderedByBottomBounds.size)

        val topIterator: MutableIterator<IncrementalMountOutput> =
            outputsOrderedByTopBounds.iterator()
        val bottomIterator: MutableIterator<IncrementalMountOutput> =
            outputsOrderedByBottomBounds.iterator()

        while (topIterator.hasNext() && bottomIterator.hasNext()) {
          outputsOrderedByTopBoundsList?.add(topIterator.next())
          outputsOrderedByBottomBoundsList?.add(bottomIterator.next())
        }
      }
    }
  }

  class Visitor(private val shouldMountZeroBoundsContent: Boolean) : LayoutResultVisitor<Results> {

    override fun visit(
        parent: RenderTreeNode?,
        result: LayoutResult,
        bounds: Rect,
        x: Int,
        y: Int,
        position: Int,
        state: Results?,
    ) {
      if (position == 0) {
        return
      }

      val unit = result.renderUnit
      if (unit == null) {
        return
      }

      val id = unit.id
      requireNotNull(parent) { "Parent was null for position=" + position }

      val host: IncrementalMountOutput? =
          state?.getIncrementalMountOutputForId(parent.renderUnit.id)

      val rect = Rect(
          x,
          y,
          if (shouldMountZeroBoundsContent) (x + max(bounds.width(), 1)) else (x + bounds.width()),
          if (shouldMountZeroBoundsContent) (y + max(bounds.height(), 1))
          else (y + bounds.height()),
      )

      val binderKey: BinderKey = ClassBinderKey.of(ExcludeFromIncrementalMountBinder::class.java)
      state?.addOutput(
          IncrementalMountOutput(
              id,
              position,
              rect,
              unit.findAttachBinderByKey<RenderUnit.Binder<*, *, *>?>(binderKey) != null,
              // debugKey is debug-only and unused in prod; some RenderUnits (e.g. Litho under
              // structural global keys) rebuild it via an expensive recursive toString(), so only
              // read it when debug tooling needs it.
              if (RenderCoreConfig.isDebugModeEnabled) unit.debugKey else null,
              host,
              unit.contentAllocator.getPoolKey(),
              parent.renderUnit.id,
          ),
      )
      if (unit.doesMountRenderTreeHosts()) {
        state?.addRenderTreeHostId(id)
      }
    }
  }

  companion object {
    @JvmField
    val TopsComparator: Comparator<IncrementalMountOutput> = Comparator { l, r ->
      val lhsTop = l.bounds.top
      val rhsTop = r.bounds.top

      if (lhsTop == rhsTop) {
        // Lower indices should be higher for tops so that they are mounted first if possible.
        if (l.index == r.index) 0 else if (l.index > r.index) 1 else -1
      } else {
        if (lhsTop > rhsTop) 1 else -1
      }
    }

    @JvmField
    val BottomsComparator: Comparator<IncrementalMountOutput> = Comparator { l, r ->
      val lhsBottom = l.bounds.bottom
      val rhsBottom = r.bounds.bottom

      if (lhsBottom == rhsBottom) {
        // Lower indices should be lower for bottoms so that they are mounted first if
        // possible.
        if (l.index == r.index) 0 else if (l.index < r.index) 1 else -1
      } else {
        if (lhsBottom > rhsBottom) 1 else -1
      }
    }

    private val mountExtension = instance
  }
}
