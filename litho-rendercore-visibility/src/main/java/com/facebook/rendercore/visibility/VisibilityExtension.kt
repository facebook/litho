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

package com.facebook.rendercore.visibility

import android.annotation.SuppressLint
import android.graphics.Rect
import androidx.collection.LongSet
import androidx.collection.MutableLongSet
import com.facebook.rendercore.LayoutResult
import com.facebook.rendercore.RenderTreeNode
import com.facebook.rendercore.extensions.LayoutResultVisitor
import com.facebook.rendercore.extensions.RenderCoreExtension
import com.facebook.rendercore.visibility.VisibilityMountExtension.VisibilityMountExtensionState

class VisibilityExtension(factory: VisibilityOutput.Factory<*>) :
    RenderCoreExtension<VisibilityExtension.Results, VisibilityMountExtensionState>() {

  private val visitor: Visitor = Visitor(factory)
  private val mountExtension: VisibilityMountExtension<Results> =
      VisibilityMountExtension.getInstance()

  override fun getLayoutVisitor(): Visitor {
    return visitor
  }

  override fun getMountExtension(): VisibilityMountExtension<Results> {
    return mountExtension
  }

  override fun createInput(): Results {
    return Results()
  }

  class Results : VisibilityExtensionInput {

    private val outputs: MutableList<VisibilityOutput> = ArrayList(8)
    private val _renderUnitIdsWhichHostRenderTrees: MutableLongSet? = MutableLongSet(4)

    override val visibilityOutputs: List<VisibilityOutput>
      get() = outputs

    override val renderUnitIdsWhichHostRenderTrees: LongSet?
      get() = _renderUnitIdsWhichHostRenderTrees

    override val isProcessingVisibilityOutputsEnabled: Boolean
      get() = true

    internal fun addOutput(output: VisibilityOutput?) {
      if (output != null) {
        outputs.add(output)
      }
    }

    internal fun addRenderUnitIdWhichHostsRenderTree(id: Long) {
      _renderUnitIdsWhichHostRenderTrees?.add(id)
    }

    override val visibilityBoundsTransformer: VisibilityBoundsTransformer?
      get() = null
  }

  class Visitor(private val factory: VisibilityOutput.Factory<*>) : LayoutResultVisitor<Results> {

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
      if (state != null) {
        val absoluteBounds = Rect(x, y, x + bounds.width(), y + bounds.height())
        // This cast is safe because the Factory is only used with LayoutResult instances.
        // The factory is declared as Factory<*> to allow flexibility in the VisibilityExtension
        // constructor, but at runtime it will always receive LayoutResult objects from this
        // visitor.
        // See: https://kotlinlang.org/docs/generics.html#type-erasure
        @SuppressLint("KotlinGenericsCast")
        @Suppress("UNCHECKED_CAST")
        val untypedFactory = factory as VisibilityOutput.Factory<LayoutResult>
        state.addOutput(untypedFactory.createVisibilityOutput(result, absoluteBounds))
        val unit = result.renderUnit
        if (unit != null && unit.doesMountRenderTreeHosts()) {
          state.addRenderUnitIdWhichHostsRenderTree(unit.id)
        }
      }
    }
  }
}
