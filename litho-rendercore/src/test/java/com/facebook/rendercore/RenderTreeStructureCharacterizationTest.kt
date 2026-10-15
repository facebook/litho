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
import android.widget.TextView
import com.facebook.rendercore.testing.LayoutResultWrappingNode
import com.facebook.rendercore.testing.SimpleLayoutResult
import com.facebook.rendercore.testing.TestHostRenderUnit
import com.facebook.rendercore.testing.ViewWrapperUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Characterization tests for the structure [Reducer] produces in a [RenderTree].
 *
 * These pin *current* behaviour rather than desired behaviour. Every value below was recorded from
 * the existing implementation, not derived from a specification — the point is to make any change
 * in what the reducer emits visible as a test failure, so a refactor has to justify it rather than
 * quietly absorb it.
 *
 * ## Why the snapshot shape
 *
 * Each tree is reduced to a flat list of [NodeShape], one per mountable output, covering everything
 * a consumer can observe about a node's place in the tree: its index, its parent, its slot within
 * that parent, its depth, and both its parent-relative and absolute bounds. Asserting the whole
 * list with `containsExactly` means a change in *ordering* fails just as loudly as a change in
 * values — flatList order is load-bearing (it is what `MountState` iterates, and the reducer's
 * pre-order is what lets absolute positions accumulate in one pass).
 *
 * ## Maintaining these under refactors
 *
 * [shapeOf] is the only part that should ever need to change. If a refactor moves where this data
 * lives — say off [RenderTreeNode] and onto [RenderTree] — update the reads inside [shapeOf] and
 * leave the expected values alone. **An expected value that has to change is a behaviour change**,
 * and should be called out in the diff that changes it rather than edited in passing.
 */
@RunWith(RobolectricTestRunner::class)
class RenderTreeStructureCharacterizationTest {

  private val context: Context = RuntimeEnvironment.application

  /**
   * Everything observable about one node's position in the tree. Deliberately a value type: the
   * assertions compare data, not object identity, so they survive the node objects themselves being
   * reallocated or reused.
   */
  private data class NodeShape(
      val index: Int,
      val id: Long,
      val parentId: Long,
      val positionInParent: Int,
      val depth: Int,
      val bounds: String,
      val absoluteBounds: String,
      val childIds: List<Long>,
  )

  private fun shapeOf(tree: RenderTree, index: Int): NodeShape {
    val node = tree.getRenderTreeNodeAtIndex(index)
    val absolute = Rect()
    node.getAbsoluteBounds(absolute)
    return NodeShape(
        index = index,
        id = node.renderUnit.id,
        parentId = node.parent?.renderUnit?.id ?: NO_PARENT,
        positionInParent = node.positionInParent,
        depth = node.getHierarchyDepth(),
        bounds = node.bounds.toShortString(),
        absoluteBounds = absolute.toShortString(),
        childIds = (0 until node.childrenCount).map { node.getChildAt(it).renderUnit.id },
    )
  }

  private fun shapesOf(tree: RenderTree): List<NodeShape> =
      (0 until tree.mountableOutputCount).map { shapeOf(tree, it) }

  private fun reduce(root: SimpleLayoutResult.Builder): RenderTree {
    val sizeConstraints = SizeConstraints.exact(ROOT_SIZE, ROOT_SIZE)
    val layoutContext = LayoutContext<Any?>(context, null, -1, LayoutCache(), null)
    val node = LayoutResultWrappingNode(root.build())
    val result = node.calculateLayout(layoutContext, sizeConstraints)
    return Reducer.getReducedTree(layoutContext, result, sizeConstraints, RenderState.NO_ID)
  }

  /** A leaf that mounts a view. */
  private fun leaf(id: Long, x: Int, y: Int, width: Int, height: Int) =
      SimpleLayoutResult.create()
          .renderUnit(ViewWrapperUnit(TextView(context), id))
          .x(x)
          .y(y)
          .width(width)
          .height(height)

  /** A node that mounts a host, and therefore becomes a parent in the reduced tree. */
  private fun host(id: Long, x: Int, y: Int, width: Int, height: Int) =
      SimpleLayoutResult.create()
          .renderUnit(TestHostRenderUnit(id))
          .x(x)
          .y(y)
          .width(width)
          .height(height)

  @Test
  fun flatSiblings_positionsIncrementAndAbsoluteBoundsMatchLocal() {
    // All three children hang off the root host, so their absolute bounds equal their local bounds
    // and their slots are simply their emission order.
    val tree = reduce(
        SimpleLayoutResult.create()
            .width(ROOT_SIZE)
            .height(ROOT_SIZE)
            .child(leaf(id = 1, x = 0, y = 0, width = 100, height = 10))
            .child(leaf(id = 2, x = 0, y = 10, width = 100, height = 10))
            .child(leaf(id = 3, x = 0, y = 20, width = 100, height = 10)),
    )

    assertThat(shapesOf(tree))
        .containsExactly(
            NodeShape(
                0,
                ROOT_HOST_ID,
                NO_PARENT,
                0,
                0,
                "[0,0][200,200]",
                "[0,0][200,200]",
                listOf(1L, 2L, 3L),
            ),
            NodeShape(1, 1L, ROOT_HOST_ID, 0, 1, "[0,0][100,10]", "[0,0][100,10]", emptyList()),
            NodeShape(
                2,
                2L,
                ROOT_HOST_ID,
                1,
                1,
                "[0,10][100,20]",
                "[0,10][100,20]",
                emptyList(),
            ),
            NodeShape(
                3,
                3L,
                ROOT_HOST_ID,
                2,
                1,
                "[0,20][100,30]",
                "[0,20][100,30]",
                emptyList(),
            ),
        )
  }

  @Test
  fun nestedHosts_absoluteBoundsAccumulateAndDepthTracksNesting() {
    // A host inside a host: each level's offset compounds into the descendant's absolute bounds,
    // while its local bounds stay relative to its own parent.
    val tree = reduce(
        SimpleLayoutResult.create()
            .width(ROOT_SIZE)
            .height(ROOT_SIZE)
            .child(
                host(id = 10, x = 5, y = 5, width = 150, height = 150)
                    .child(leaf(id = 11, x = 3, y = 3, width = 50, height = 50))
                    .child(
                        host(id = 12, x = 20, y = 20, width = 100, height = 100)
                            .child(leaf(id = 13, x = 7, y = 7, width = 30, height = 30)),
                    ),
            ),
    )

    assertThat(shapesOf(tree))
        .containsExactly(
            NodeShape(
                0,
                ROOT_HOST_ID,
                NO_PARENT,
                0,
                0,
                "[0,0][200,200]",
                "[0,0][200,200]",
                listOf(10L),
            ),
            NodeShape(
                1,
                10L,
                ROOT_HOST_ID,
                0,
                1,
                "[5,5][155,155]",
                "[5,5][155,155]",
                listOf(11L, 12L),
            ),
            NodeShape(2, 11L, 10L, 0, 2, "[3,3][53,53]", "[8,8][58,58]", emptyList()),
            NodeShape(
                3,
                12L,
                10L,
                1,
                2,
                "[20,20][120,120]",
                "[25,25][125,125]",
                listOf(13L),
            ),
            NodeShape(4, 13L, 12L, 0, 3, "[7,7][37,37]", "[32,32][62,62]", emptyList()),
        )
  }

  @Test
  fun layoutOnlyNodesAreFlattened_childrenReparentOntoTheNearestMountedHost() {
    // A node with no render unit produces no output; its children are hoisted onto the nearest
    // ancestor that does mount, and their offsets are folded in. This flattening is the reason a
    // node's parent in the reduced tree is not simply its parent in the layout tree.
    val tree = reduce(
        SimpleLayoutResult.create()
            .width(ROOT_SIZE)
            .height(ROOT_SIZE)
            .child(
                host(id = 20, x = 5, y = 7, width = 190, height = 190)
                    .child(
                        SimpleLayoutResult.create()
                            .x(10)
                            .y(10)
                            .width(180)
                            .height(180)
                            .child(leaf(id = 21, x = 2, y = 2, width = 40, height = 40))
                            .child(leaf(id = 22, x = 2, y = 50, width = 40, height = 40)),
                    ),
            ),
    )

    assertThat(shapesOf(tree))
        .containsExactly(
            NodeShape(
                0,
                ROOT_HOST_ID,
                NO_PARENT,
                0,
                0,
                "[0,0][200,200]",
                "[0,0][200,200]",
                listOf(20L),
            ),
            NodeShape(
                1,
                20L,
                ROOT_HOST_ID,
                0,
                1,
                "[5,7][195,197]",
                "[5,7][195,197]",
                listOf(21L, 22L),
            ),
            NodeShape(
                2,
                21L,
                20L,
                0,
                2,
                "[12,12][52,52]",
                "[17,19][57,59]",
                emptyList(),
            ),
            NodeShape(
                3,
                22L,
                20L,
                1,
                2,
                "[12,60][52,100]",
                "[17,67][57,107]",
                emptyList(),
            ),
        )
  }

  @Test
  fun flatListIsPreOrder_everyParentPrecedesItsDescendants() {
    // The single-pass absolute-bounds accumulation depends on this ordering, and so does anything
    // that walks the flat list expecting to have seen a node's parent already.
    val tree = reduce(
        SimpleLayoutResult.create()
            .width(ROOT_SIZE)
            .height(ROOT_SIZE)
            .child(
                host(id = 30, x = 0, y = 0, width = 100, height = 100)
                    .child(leaf(id = 31, x = 1, y = 1, width = 10, height = 10)),
            )
            .child(
                host(id = 32, x = 100, y = 0, width = 100, height = 100)
                    .child(leaf(id = 33, x = 1, y = 1, width = 10, height = 10)),
            ),
    )

    assertThat(shapesOf(tree).map { it.id }).containsExactly(ROOT_HOST_ID, 30L, 31L, 32L, 33L)
    val indexById = (0 until tree.mountableOutputCount).associateBy { shapeOf(tree, it).id }
    for (index in 0 until tree.mountableOutputCount) {
      val shape = shapeOf(tree, index)
      if (shape.parentId == NO_PARENT) continue
      assertThat(checkNotNull(indexById[shape.parentId]))
          .describedAs("parent of node id=%d (index %d) must precede it", shape.id, index)
          .isLessThan(index)
    }
  }

  @Test
  fun idLookupResolvesEveryEmittedNode() {
    val tree = reduce(
        SimpleLayoutResult.create()
            .width(ROOT_SIZE)
            .height(ROOT_SIZE)
            .child(
                host(id = 40, x = 0, y = 0, width = 100, height = 100)
                    .child(leaf(id = 41, x = 1, y = 1, width = 10, height = 10)),
            ),
    )

    assertThat(shapesOf(tree).map { it.id }).containsExactly(ROOT_HOST_ID, 40L, 41L)
    for (index in 0 until tree.mountableOutputCount) {
      val id = tree.getRenderTreeNodeAtIndex(index).renderUnit.id
      assertThat(tree.getRenderTreeNodeIndex(id))
          .describedAs("index round-trip for node id=%d", id)
          .isEqualTo(index)
    }
    assertThat(tree.getRenderTreeNodeIndex(UNKNOWN_ID))
        .describedAs("unknown id")
        .isEqualTo(NOT_FOUND)
  }

  private companion object {
    private const val ROOT_SIZE = 200
    private const val ROOT_HOST_ID = MountState.ROOT_HOST_ID
    private const val NO_PARENT = -1L
    private const val UNKNOWN_ID = 999_999L
    private const val NOT_FOUND = -1
  }
}
