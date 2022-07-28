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

package com.facebook.litho.widget.collection

import com.facebook.litho.Component
import com.facebook.litho.EmptyComponent
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.Text
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/** Tests for [Collection]'s children */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class CollectionIdTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  private fun emptyComponent(): EmptyComponent = EmptyComponent()

  private fun textComponent(): Component = Text.create(lithoViewRule.context).text("Hello").build()

  private fun LazyCollectionChildren.getIds(): List<Any?> = collectionChildren.map { it.id }

  @Test
  fun `test generated ids are unique`() {
    val ids =
        LazyCollectionChildren()
            .apply {
              add(null)
              add(emptyComponent())
              add(id = null, component = emptyComponent())
              add(deps = arrayOf()) { emptyComponent() }
            }
            .getIds()

    assertThat(ids).hasSameSizeAs(ids.distinct())
  }

  @Test
  fun `test generated ids are stable across recreations`() {
    val ids1 =
        LazyCollectionChildren()
            .apply {
              add(null)
              add(emptyComponent())
              add(id = null, component = emptyComponent())
              add(deps = arrayOf()) { emptyComponent() }
            }
            .getIds()

    val ids2 =
        LazyCollectionChildren()
            .apply {
              add(null)
              add(emptyComponent())
              add(id = null, component = emptyComponent())
              add(deps = arrayOf()) { emptyComponent() }
            }
            .getIds()

    assertThat(ids1).hasSameElementsAs(ids2)
  }

  @Test
  fun `test generated ids for same component type are stable`() {
    val ids1 =
        LazyCollectionChildren()
            .apply {
              add(textComponent())
              add(textComponent())
            }
            .getIds()

    val ids2 =
        LazyCollectionChildren()
            .apply {
              add(textComponent())
              // Insert new content. Should not affect ids of existing children
              add(emptyComponent())
              add(id = 1, component = textComponent())
              // End new content.
              add(textComponent())
            }
            .getIds()

    assertThat(ids2.first()).isEqualTo(ids1.first())
    assertThat(ids2.last()).isEqualTo(ids1.last())
  }
}
