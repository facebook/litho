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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.SimpleMountSpecTester
import java.lang.ref.WeakReference
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class ComponentTreeRegistryTest {

  private lateinit var context: ComponentContext

  @Before
  fun setup() {
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    ComponentTreeRegistry.clear()
    ComponentTreeRegistry.enabled = true
  }

  @After
  fun tearDown() {
    ComponentTreeRegistry.enabled = false
    ComponentTreeRegistry.clear()
  }

  @Test
  fun `does not record trees when disabled`() {
    ComponentTreeRegistry.enabled = false

    val tree = createTree()

    assertThat(infoFor(tree)).isNull()
  }

  @Test
  fun `keeps released tree with its root name`() {
    val component = SimpleMountSpecTester()
    val tree = ComponentTree.create(context, component).build()
    assertThat(requireNotNull(infoFor(tree)).isReleased).isFalse()

    tree.release()

    val info = requireNotNull(infoFor(tree))
    assertThat(info.isReleased).isTrue()
    assertThat(info.rootName).isEqualTo(component.simpleName)
    assertThat(info.releasedAtMs).isGreaterThanOrEqualTo(info.createdAtMs)
  }

  @Test
  fun `forEach skips released trees`() {
    val live = createTree()
    val released = createTree().also { it.release() }

    val visited = mutableListOf<ComponentTree>()
    ComponentTreeRegistry.forEach { visited.add(it) }

    assertThat(visited).contains(live).doesNotContain(released)
  }

  @Test
  fun `countByState separates attached, detached and released trees`() {
    val attached = createTree()
    val lithoView = LithoView(context)
    lithoView.componentTree = attached
    lithoView.onAttachedToWindowForTest()
    val detached = createTree()
    val released = createTree().also { it.release() }

    val counts = ComponentTreeRegistry.countByState()

    assertThat(counts.attached).isEqualTo(1)
    assertThat(counts.detached).isEqualTo(1)
    assertThat(counts.released).isEqualTo(1)
    // Keeps the trees strongly reachable until after counting.
    assertThat(listOf(attached, detached, released)).doesNotContainNull()
  }

  @Test
  fun `drops released tree once it is garbage collected`() {
    val (id, ref) = createAndReleaseTree()

    awaitCollected(ref)

    assertThat(ComponentTreeRegistry.snapshot().map { it.id }).doesNotContain(id)
  }

  private fun createTree(): ComponentTree =
      ComponentTree.create(context, SimpleMountSpecTester()).build()

  // Keeps the tree out of the calling test's stack frame so it can be collected.
  private fun createAndReleaseTree(): Pair<Int, WeakReference<ComponentTree>> {
    val tree = createTree()
    tree.release()
    return tree.id to WeakReference(tree)
  }

  private fun infoFor(tree: ComponentTree): ComponentTreeRegistry.TreeInfo? =
      ComponentTreeRegistry.snapshot().find { it.id == tree.id }

  private fun awaitCollected(ref: WeakReference<*>) {
    repeat(20) {
      if (ref.get() == null) {
        return
      }
      Runtime.getRuntime().gc()
      Thread.sleep(10)
    }
    fail<Unit>("Released ComponentTree was not garbage collected")
  }
}
