/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho.sections.widget

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.litho.Component
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.widget.EmptyComponent
import com.facebook.litho.widget.Text
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/** Tests for [Collection]'s children */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(AndroidJUnit4::class)
class CollectionIdTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  private fun emptyComponent(): Component = EmptyComponent.create(lithoViewRule.context).build()

  private fun textComponent(): Component = Text.create(lithoViewRule.context).text("Hello").build()

  private fun CollectionContainerScope.getIds(): List<Any?> = collectionChildrenModels.map { it.id }

  @Test
  fun `test generated ids are unique`() {
    val ids =
        CollectionContainerScope()
            .apply {
              child(null)
              child(emptyComponent())
              child(id = null, component = emptyComponent())
              child(deps = arrayOf()) { emptyComponent() }
            }
            .getIds()

    assertThat(ids).hasSameSizeAs(ids.distinct())
  }

  @Test
  fun `test generated ids are stable across recreations`() {
    val ids1 =
        CollectionContainerScope()
            .apply {
              child(null)
              child(emptyComponent())
              child(id = null, component = emptyComponent())
              child(deps = arrayOf()) { emptyComponent() }
            }
            .getIds()

    val ids2 =
        CollectionContainerScope()
            .apply {
              child(null)
              child(emptyComponent())
              child(id = null, component = emptyComponent())
              child(deps = arrayOf()) { emptyComponent() }
            }
            .getIds()

    assertThat(ids1).hasSameElementsAs(ids2)
  }

  @Test
  fun `test generated ids for same component type are stable`() {
    val ids1 =
        CollectionContainerScope()
            .apply {
              child(textComponent())
              child(textComponent())
            }
            .getIds()

    val ids2 =
        CollectionContainerScope()
            .apply {
              child(textComponent())
              child(emptyComponent()) // <-- Should not affect ids of other children
              child(
                  id = 1,
                  component = textComponent()) // <-- Should not affect ids of other children
              child(textComponent())
            }
            .getIds()

    assertThat(ids2).containsAll(ids1)
  }

  @Test
  fun `test generated ids in SubCollections are not impacted by other children`() {
    val ids1 =
        CollectionContainerScope()
            .apply {
              subCollection(id = "something") {
                child(null)
                child(emptyComponent())
                child(id = 1, component = emptyComponent())
                child(id = null, component = emptyComponent())
                child(deps = arrayOf()) { emptyComponent() }
              }
            }
            .getIds()

    val ids2 =
        CollectionContainerScope()
            .apply {
              child(emptyComponent()) // <-- Should not affect id assignment in SubCollection
              subCollection(id = "something") {
                child(null)
                child(emptyComponent())
                child(id = 1, component = emptyComponent())
                child(id = null, component = emptyComponent())
                child(deps = arrayOf()) { emptyComponent() }
              }
            }
            .getIds()

    assertThat(ids1).hasSameElementsAs(ids2.subList(1, ids2.size))
  }

  @Test
  fun `test ids in SubCollections do not clash`() {
    val ids1 =
        CollectionContainerScope()
            .apply {
              subCollection(id = "something") {
                child(null)
                child(emptyComponent())
                child(id = 1, component = emptyComponent())
                child(id = null, component = emptyComponent())
                child(deps = arrayOf()) { emptyComponent() }
              }
            }
            .getIds()

    val ids2 =
        CollectionContainerScope()
            .apply {
              subCollection(id = "something else") {
                child(null)
                child(emptyComponent())
                child(id = 1, component = emptyComponent())
                child(id = null, component = emptyComponent())
                child(deps = arrayOf()) { emptyComponent() }
              }
            }
            .getIds()

    assertThat(ids1).doesNotContainAnyElementsOf(ids2)
  }

  @Test
  fun `test ids in nested SubCollections do not clash`() {
    val ids1 =
        CollectionContainerScope()
            .apply {
              subCollection(id = "something") {
                subCollection(id = 2) {
                  child(null)
                  child(emptyComponent())
                  child(id = 1, component = emptyComponent())
                  child(id = null, component = emptyComponent())
                  child(deps = arrayOf()) { emptyComponent() }
                }
              }
            }
            .getIds()

    val ids2 =
        CollectionContainerScope()
            .apply {
              subCollection(id = "something else") {
                subCollection(id = 2) {
                  child(null)
                  child(emptyComponent())
                  child(id = 1, component = emptyComponent())
                  child(id = null, component = emptyComponent())
                  child(deps = arrayOf()) { emptyComponent() }
                }
              }
            }
            .getIds()

    assertThat(ids1).doesNotContainAnyElementsOf(ids2)
  }
}
