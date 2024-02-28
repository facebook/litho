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

import android.util.Pair
import androidx.collection.LongSparseArray
import com.facebook.rendercore.extensions.RenderCoreExtension
import java.util.Locale

/** TODO add javadoc */
class RenderTree(
    val root: RenderTreeNode,
    private val flatList: Array<RenderTreeNode>,
    idToIndexMap: LongSparseArray<Int>?,
    val sizeConstraints: SizeConstraints,
    val renderStateId: Int,
    val extensionResults: List<Pair<RenderCoreExtension<*, *>, Any>>?,
    // This will vary by framework and will be null outside of debug builds
    val debugData: Any?
) {

  val width: Int
    get() = root.bounds.width()

  val height: Int
    get() = root.bounds.height()

  val mountableOutputCount: Int
    get() = flatList.size

  private val idToIndexMap: LongSparseArray<Int>

  init {
    if (idToIndexMap != null) {
      this.idToIndexMap = idToIndexMap
    } else {
      this.idToIndexMap = LongSparseArray(flatList.size)
      for (i in flatList.indices) {
        assertNoDuplicateRenderUnits(i, this.idToIndexMap, this@RenderTree)
        this.idToIndexMap.put(flatList[i].renderUnit.id, i)
      }
    }
  }

  fun getRenderTreeNodeIndex(renderUnitId: Long): Int {
    return idToIndexMap.get(renderUnitId, -1)
  }

  fun getRenderTreeNodeAtIndex(index: Int): RenderTreeNode {
    return flatList[index]
  }

  companion object {
    @JvmStatic
    fun create(
        root: RenderTreeNode,
        flatList: Array<RenderTreeNode>,
        idToIndexMap: LongSparseArray<Int>?,
        sizeConstraints: Long,
        renderStateId: Int,
        extensionResults: List<Pair<RenderCoreExtension<*, *>, Any>>?,
        debugData: Any?
    ): RenderTree {
      return RenderTree(
          root,
          flatList,
          idToIndexMap,
          SizeConstraints.Helper.encode(sizeConstraints),
          renderStateId,
          extensionResults,
          debugData,
      )
    }

    /**
     * Throws an exception if this RenderTree already has a RenderUnit with the same ID as the one
     * at the given index.
     */
    private fun assertNoDuplicateRenderUnits(
        newNodeIndex: Int,
        idToIndexMap: LongSparseArray<Int>,
        renderTree: RenderTree,
    ) {
      val flatList = renderTree.flatList
      val newNode = flatList[newNodeIndex]
      val existingNodeIndex = idToIndexMap.get(newNode.renderUnit.id, -1)
      if (existingNodeIndex == -1) {
        return
      } else {
        val existingNode = flatList[existingNodeIndex]
        throw IllegalStateException(
            String.format(
                Locale.US,
                """
              RenderTrees must not have RenderUnits with the same ID:
              Attempted to add item with existing ID at index %d: %s
              Existing item at index %d: %s
              Full RenderTree: %s
              """
                    .trimIndent(),
                newNodeIndex,
                newNode.generateDebugString(null),
                existingNodeIndex,
                existingNode.generateDebugString(null),
                generateDebugString(renderTree)))
      }
    }

    private fun generateDebugString(renderTree: RenderTree): String {
      val l = Locale.US
      return buildString {
        append("RenderTree details:\n")
        append(String.format(l, "Full child list (size = %d):\n", renderTree.flatList.size))
        for (node in renderTree.flatList) {
          append(String.format(l, "%s\n", node.generateDebugString(renderTree)))
        }
      }
    }
  }
}
