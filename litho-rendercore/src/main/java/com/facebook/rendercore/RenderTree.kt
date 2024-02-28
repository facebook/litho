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

import android.util.LongSparseArray
import android.util.Pair
import com.facebook.rendercore.extensions.RenderCoreExtension
import java.util.Locale

/** TODO add javadoc */
class RenderTree(
    val root: RenderTreeNode,
    private val flatList: Array<RenderTreeNode>,
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

  private val idToIndexMap = LongSparseArray<Int>()

  init {
    for (i in 0 until flatList.size) {
      assertNoDuplicateRenderUnits(i)
      idToIndexMap.put(flatList[i].renderUnit.id, i)
    }
  }

  /**
   * Throws an exception if this RenderTree already has a RenderUnit with the same ID as the one at
   * the given index.
   */
  private fun assertNoDuplicateRenderUnits(newNodeIndex: Int) {
    val newNode = flatList[newNodeIndex]
    if (idToIndexMap[newNode.renderUnit.id] == null) {
      return
    }
    val existingNodeIndex = idToIndexMap[newNode.renderUnit.id]
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
            generateDebugString()))
  }

  fun getRenderTreeNodeIndex(renderUnitId: Long): Int {
    return idToIndexMap[renderUnitId, -1]
  }

  fun getRenderTreeNodeAtIndex(index: Int): RenderTreeNode {
    return flatList[index]
  }

  private fun generateDebugString(): String {
    val l = Locale.US

    return buildString {
      append("RenderTree details:\n")
      append(String.format(l, "%s\n", sizeConstraints.toString()))
      append(String.format(l, "Full child list (size = %d):\n", flatList.size))
      for (node in flatList) {
        append(String.format(l, "%s\n", node.generateDebugString(this@RenderTree)))
      }
    }
  }

  companion object {
    @JvmStatic
    fun create(
        root: RenderTreeNode,
        flatList: Array<RenderTreeNode>,
        sizeConstraints: Long,
        renderStateId: Int,
        extensionResults: List<Pair<RenderCoreExtension<*, *>, Any>>?,
        debugData: Any?
    ): RenderTree {
      return RenderTree(
          root,
          flatList,
          SizeConstraints.Helper.encode(sizeConstraints),
          renderStateId,
          extensionResults,
          debugData,
      )
    }
  }
}
