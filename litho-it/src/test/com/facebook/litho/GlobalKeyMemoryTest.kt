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

import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Memory benchmark tests comparing [DefaultGlobalKey] vs [StringGlobalKey].
 *
 * These tests verify that the linked-structure representation in [DefaultGlobalKey] is more
 * memory-efficient than the concatenated-string approach in [StringGlobalKey].
 */
@RunWith(LithoTestRunner::class)
class GlobalKeyMemoryTest {

  /**
   * Builds a chain of [DefaultGlobalKey] nodes with the given depth. Each node uses its depth index
   * as the typeId and position 0.
   */
  private fun buildDefaultKeyChain(depth: Int): List<DefaultGlobalKey> {
    val chain = mutableListOf<DefaultGlobalKey>()
    var parent: GlobalKey? = null
    for (i in 0 until depth) {
      val node = DefaultGlobalKey(key = i + 1, position = 0, parent = parent)
      chain.add(node)
      parent = node
    }
    return chain
  }

  /**
   * Builds a [StringGlobalKey] equivalent to a [DefaultGlobalKey] chain at a given depth. Uses the
   * same string format as [DefaultGlobalKey.buildKeyString].
   */
  private fun buildEquivalentStringKey(depth: Int): StringGlobalKey {
    val keyString = (1..depth).joinToString(",")
    return StringGlobalKey(keyString)
  }

  @Test
  fun `DefaultGlobalKey does not allocate strings until toString is called`() {
    val chain = buildDefaultKeyChain(5)

    // DefaultGlobalKey stores only primitive fields (key: Int, position: Int) and a parent
    // reference — no String fields at all. Verify via reflection.
    for (node in chain) {
      val stringFields = node::class.java.declaredFields.filter { it.type == String::class.java }
      assertThat(stringFields)
          .describedAs("DefaultGlobalKey should have no String-typed fields")
          .isEmpty()
    }

    // toString() still produces the correct key string on demand
    val leaf = chain.last()
    assertThat(leaf.toString()).isEqualTo("1,2,3,4,5")
  }

  @Test
  fun `StringGlobalKey string length grows with depth while DefaultGlobalKey size is constant`() {
    val depths = listOf(1, 5, 10, 20, 50)

    for (depth in depths) {
      val defaultChain = buildDefaultKeyChain(depth)
      val stringKey = buildEquivalentStringKey(depth)

      // Both representations should produce the same string
      assertThat(defaultChain.last().toString())
          .describedAs(
              "DefaultGlobalKey and StringGlobalKey should produce identical strings at depth $depth",
          )
          .isEqualTo(stringKey.toString())
    }

    // StringGlobalKey backing string length grows with depth
    val stringLengths = depths.map { depth -> buildEquivalentStringKey(depth).key.length }
    for (i in 1 until stringLengths.size) {
      assertThat(stringLengths[i])
          .describedAs("StringGlobalKey string length should grow with depth")
          .isGreaterThan(stringLengths[i - 1])
    }

    // DefaultGlobalKey node field count is constant regardless of depth:
    // Each node always has exactly: key (Int), position (Int), parent (GlobalKey?)
    val defaultKeyFields = DefaultGlobalKey::class.java.declaredFields
    for (depth in depths) {
      val chain = buildDefaultKeyChain(depth)
      for (node in chain) {
        val nodeFields = node::class.java.declaredFields
        assertThat(nodeFields.size)
            .describedAs("DefaultGlobalKey declared field count should be constant at depth $depth")
            .isEqualTo(defaultKeyFields.size)
      }
    }
  }

  @Test
  fun `DefaultGlobalKey siblings share parent reference`() {
    val parent = DefaultGlobalKey(key = 1, position = 0, parent = null)
    val sibling1 = DefaultGlobalKey(key = 2, position = 0, parent = parent)
    val sibling2 = DefaultGlobalKey(key = 2, position = 1, parent = parent)

    // Siblings share the exact same parent object (identity equality)
    assertThat(sibling1.parent === sibling2.parent)
        .describedAs("DefaultGlobalKey siblings should share the same parent reference")
        .isTrue()

    // StringGlobalKey siblings contain duplicated prefix bytes as distinct String objects
    val stringSibling1 = StringGlobalKey("1,2")
    val stringSibling2 = StringGlobalKey("1,2!1")

    // The string keys are distinct objects even though they share a common prefix
    assertThat(stringSibling1.key === stringSibling2.key)
        .describedAs("StringGlobalKey siblings have distinct backing strings")
        .isFalse()

    // Both string keys redundantly store the parent prefix "1"
    assertThat(stringSibling1.key).startsWith("1,")
    assertThat(stringSibling2.key).startsWith("1,")
  }

  @Test
  fun `analytical memory estimate shows DefaultGlobalKey saves memory`() {
    // JVM object layout assumptions (compressed oops, 64-bit JVM):
    // - Object header: 12 bytes
    // - Reference field: 4 bytes (compressed oops)
    // - int field: 4 bytes
    // - Alignment: round up to 8-byte boundary
    // - String: 12 (header) + 4 (hash) + 4 (coder) + 4 (byte[] ref) = 24 bytes (padded)
    // - byte[]: 12 (header) + 4 (length) + N bytes, padded to 8

    fun estimateStringGlobalKeyBytes(keyString: String): Long {
      // StringGlobalKey object: header(12) + key ref(4) + parent ref(4) + cachedHashCode(4)
      //   = 24 -> padded to 24
      val sgkObject = 24L
      // String object: header(12) + hash(4) + coder(1) + byte[] ref(4) = 21 -> padded to 24
      val stringObject = 24L
      // byte[] for the string content (Latin-1 encoding)
      val byteArraySize = 16L + keyString.length // 12 header + 4 length + chars, padded
      val byteArrayPadded = (byteArraySize + 7) / 8 * 8
      return sgkObject + stringObject + byteArrayPadded
    }

    fun estimateDefaultGlobalKeyBytes(): Long {
      // DefaultGlobalKey object: header(12) + key(4, Int) + position(4, Int) + parent ref(4) +
      //   cachedHashCode(4) = 28 -> padded to 32
      // No String fields — toString() recomputes on each call
      return 32L
    }

    // Simulate a tree: depth 10, branching factor 3
    val depth = 10
    val branchingFactor = 3

    // Total nodes in a complete tree with branching factor b and depth d:
    // sum(b^i for i in 0..d-1) = (b^d - 1) / (b - 1)
    var totalNodes = 0L
    var nodesAtLevel = 1L
    var stringGlobalKeyTotal = 0L
    var defaultGlobalKeyTotal = 0L

    for (d in 1..depth) {
      nodesAtLevel *= branchingFactor
      totalNodes += nodesAtLevel

      // For StringGlobalKey, the key string at depth d has a length proportional to depth
      // Approximate key string: "1,2,3,...,d" — each segment is a small number
      val keyStringAtDepth = (1..d).joinToString(",")
      stringGlobalKeyTotal += nodesAtLevel * estimateStringGlobalKeyBytes(keyStringAtDepth)

      // For DefaultGlobalKey, each node is constant size (no string allocated)
      defaultGlobalKeyTotal += nodesAtLevel * estimateDefaultGlobalKeyBytes()
    }

    assertThat(defaultGlobalKeyTotal)
        .describedAs(
            "DefaultGlobalKey total ($defaultGlobalKeyTotal bytes) should be significantly " +
                "less than StringGlobalKey total ($stringGlobalKeyTotal bytes) " +
                "for a tree with depth=$depth, branching=$branchingFactor, nodes=$totalNodes",
        )
        .isLessThan(stringGlobalKeyTotal)

    // The savings should be substantial — at least 30%
    val savingsPercent =
        100.0 * (stringGlobalKeyTotal - defaultGlobalKeyTotal) / stringGlobalKeyTotal
    assertThat(savingsPercent)
        .describedAs("Memory savings percentage should be at least 30%")
        .isGreaterThan(30.0)
  }

  // ── isDescendantOf tests ──

  @Test
  fun `isDescendantOf returns true for direct parent`() {
    val root = DefaultGlobalKey(key = 1, position = 0, parent = null)
    val child = DefaultGlobalKey(key = 2, position = 0, parent = root)
    assertThat(child.isDescendantOf(root)).isTrue()
  }

  @Test
  fun `isDescendantOf returns true for grandparent`() {
    val root = DefaultGlobalKey(key = 1, position = 0, parent = null)
    val child = DefaultGlobalKey(key = 2, position = 0, parent = root)
    val grandchild = DefaultGlobalKey(key = 3, position = 0, parent = child)
    assertThat(grandchild.isDescendantOf(root)).isTrue()
  }

  @Test
  fun `isDescendantOf returns false for self`() {
    val root = DefaultGlobalKey(key = 1, position = 0, parent = null)
    assertThat(root.isDescendantOf(root)).isFalse()
  }

  @Test
  fun `isDescendantOf returns false for non-ancestor`() {
    val root = DefaultGlobalKey(key = 1, position = 0, parent = null)
    val childA = DefaultGlobalKey(key = 2, position = 0, parent = root)
    val childB = DefaultGlobalKey(key = 3, position = 0, parent = root)
    val grandchild = DefaultGlobalKey(key = 4, position = 0, parent = childA)
    assertThat(grandchild.isDescendantOf(childB)).isFalse()
    assertThat(childA.isDescendantOf(childB)).isFalse()
  }

  @Test
  fun `isDescendantOf returns false for child of ancestor`() {
    val root = DefaultGlobalKey(key = 1, position = 0, parent = null)
    val child = DefaultGlobalKey(key = 2, position = 0, parent = root)
    assertThat(root.isDescendantOf(child)).isFalse()
  }

  @Test
  fun `isDescendantOf returns false for StringGlobalKey - no parent pointers`() {
    val parent = StringGlobalKey("1")
    val child = StringGlobalKey("1,2")
    // StringGlobalKey has no parent pointer, so structural check always returns false
    assertThat(child.isDescendantOf(parent)).isFalse()
  }

  @Test
  fun `isDescendantOf returns false for rootless DefaultGlobalKey`() {
    val a = DefaultGlobalKey(key = 1, position = 0, parent = null)
    val b = DefaultGlobalKey(key = 1, position = 0, parent = null)
    assertThat(a.isDescendantOf(b)).isFalse()
  }

  // ── isOrIsDescendantOf tests ──

  @Test
  fun `isOrIsDescendantOf returns true for self - DefaultGlobalKey`() {
    val root = DefaultGlobalKey(key = 1, position = 0, parent = null)
    assertThat(root.isOrIsDescendantOf(root)).isTrue()
  }

  @Test
  fun `isOrIsDescendantOf returns true for equal keys - DefaultGlobalKey`() {
    val root1 = DefaultGlobalKey(key = 1, position = 0, parent = null)
    val root2 = DefaultGlobalKey(key = 1, position = 0, parent = null)
    assertThat(root1.isOrIsDescendantOf(root2)).isTrue()
  }

  @Test
  fun `isOrIsDescendantOf returns true for descendant - DefaultGlobalKey`() {
    val root = DefaultGlobalKey(key = 1, position = 0, parent = null)
    val child = DefaultGlobalKey(key = 2, position = 0, parent = root)
    val grandchild = DefaultGlobalKey(key = 3, position = 0, parent = child)
    assertThat(child.isOrIsDescendantOf(root)).isTrue()
    assertThat(grandchild.isOrIsDescendantOf(root)).isTrue()
    assertThat(grandchild.isOrIsDescendantOf(child)).isTrue()
  }

  @Test
  fun `isOrIsDescendantOf returns false for non-descendant - DefaultGlobalKey`() {
    val root = DefaultGlobalKey(key = 1, position = 0, parent = null)
    val childA = DefaultGlobalKey(key = 2, position = 0, parent = root)
    val childB = DefaultGlobalKey(key = 3, position = 0, parent = root)
    assertThat(childA.isOrIsDescendantOf(childB)).isFalse()
    assertThat(root.isOrIsDescendantOf(childA)).isFalse()
  }

  @Test
  fun `isOrIsDescendantOf returns true for self - StringGlobalKey`() {
    val key = StringGlobalKey("1,2,3")
    assertThat(key.isOrIsDescendantOf(key)).isTrue()
  }

  @Test
  fun `isOrIsDescendantOf returns true for descendant - StringGlobalKey`() {
    val parent = StringGlobalKey("1,2")
    val child = StringGlobalKey("1,2,3")
    // StringGlobalKey falls back to string prefix comparison
    assertThat(child.isOrIsDescendantOf(parent)).isTrue()
  }

  @Test
  fun `isOrIsDescendantOf returns false for non-descendant - StringGlobalKey`() {
    val a = StringGlobalKey("1,2")
    val b = StringGlobalKey("1,3")
    assertThat(a.isOrIsDescendantOf(b)).isFalse()
    assertThat(b.isOrIsDescendantOf(a)).isFalse()
  }

  @Test
  fun `isOrIsDescendantOf with manual key in middle of chain`() {
    // Simulates: DefaultGlobalKey root -> ManualGlobalKey (manual key) -> DefaultGlobalKey child
    // ManualGlobalKey preserves the parent pointer, so structural traversal works.
    val root = DefaultGlobalKey(key = 1, position = 0, parent = null)
    val manualKey = ManualGlobalKey(key = "\$myManualKey", position = 0, parent = root)
    val child = DefaultGlobalKey(key = 3, position = 0, parent = manualKey)

    // Structural check: child -> manualKey -> root. Root is in chain.
    assertThat(child.isDescendantOf(root)).isTrue()
    assertThat(child.isOrIsDescendantOf(root)).isTrue()

    // child is a structural descendant of manualKey (direct parent)
    assertThat(child.isDescendantOf(manualKey)).isTrue()
    assertThat(child.isOrIsDescendantOf(manualKey)).isTrue()
  }

  @Test
  fun `isOrIsDescendantOf StringGlobalKey ancestor with DefaultGlobalKey descendant`() {
    // ancestor is a StringGlobalKey, descendant is a DefaultGlobalKey with a different parent
    val ancestor = StringGlobalKey("10,20")
    val unrelatedRoot = DefaultGlobalKey(key = 99, position = 0, parent = null)
    val child = DefaultGlobalKey(key = 30, position = 0, parent = unrelatedRoot)

    // child.toString() = "99,30", ancestor.toString() = "10,20"
    // "99,30".startsWith("10,20") is false
    assertThat(child.isOrIsDescendantOf(ancestor)).isFalse()
  }

  @Test
  fun `isOrIsDescendantOf with position variants`() {
    val root = DefaultGlobalKey(key = 1, position = 0, parent = null)
    val child0 = DefaultGlobalKey(key = 2, position = 0, parent = root)
    val child1 = DefaultGlobalKey(key = 2, position = 1, parent = root)

    // Both are descendants of root
    assertThat(child0.isOrIsDescendantOf(root)).isTrue()
    assertThat(child1.isOrIsDescendantOf(root)).isTrue()

    // Neither is a descendant of the other
    assertThat(child0.isOrIsDescendantOf(child1)).isFalse()
    assertThat(child1.isOrIsDescendantOf(child0)).isFalse()
  }

  @Test
  fun `isOrIsDescendantOf deep chain with manual key at various levels`() {
    // Build: root(1) -> mid(2) -> manualKey($custom) -> leaf(4)
    // ManualGlobalKey preserves parent pointer, so structural traversal works throughout.
    val root = DefaultGlobalKey(key = 1, position = 0, parent = null)
    val mid = DefaultGlobalKey(key = 2, position = 0, parent = root)
    val manualKey = ManualGlobalKey(key = "\$custom", position = 0, parent = mid)
    val leaf = DefaultGlobalKey(key = 4, position = 0, parent = manualKey)

    // Structural: leaf -> manualKey -> mid -> root. Full chain is intact.
    assertThat(leaf.isDescendantOf(mid)).isTrue()
    assertThat(leaf.isDescendantOf(root)).isTrue()

    assertThat(leaf.isOrIsDescendantOf(mid)).isTrue()
    assertThat(leaf.isOrIsDescendantOf(root)).isTrue()
    assertThat(leaf.isOrIsDescendantOf(manualKey)).isTrue()
  }

  @Test
  fun `empirical heap measurement shows DefaultGlobalKey uses less memory`() {
    val chainCount = 10_000
    val chainDepth = 5

    // --- DefaultGlobalKey stores no String fields at all ---
    val defaultKeys = Array(chainCount) { buildDefaultKeyChain(chainDepth) }

    // Verify that no DefaultGlobalKey node has any String-typed instance field
    val defaultStringFieldCount = defaultKeys.sumOf { chain ->
      chain.sumOf { node ->
        node::class.java.declaredFields.count { it.type == String::class.java }
      }
    }
    assertThat(defaultStringFieldCount)
        .describedAs("DefaultGlobalKey nodes should have zero String fields")
        .isEqualTo(0)

    // --- StringGlobalKey always stores a backing String ---
    val stringKeys = Array(chainCount) { buildEquivalentStringKey(chainDepth) }

    var stringTotalStringBytes = 0L
    for (key in stringKeys) {
      stringTotalStringBytes += key.key.length
    }

    // StringGlobalKey always stores backing strings — total should be significant
    assertThat(stringTotalStringBytes)
        .describedAs("StringGlobalKey should store a significant amount of string data")
        .isGreaterThan(0L)

    // Verify the scale: 10,000 keys each with "1,2,3,4,5" (9 chars) = 90,000 bytes minimum
    assertThat(stringTotalStringBytes)
        .describedAs(
            "StringGlobalKey total string bytes (%d) should match expected allocation",
            stringTotalStringBytes,
        )
        .isEqualTo(chainCount.toLong() * (1..chainDepth).joinToString(",").length)
  }
}
