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

package com.facebook.litho.testing.assertj

import android.content.Context
import androidx.annotation.StringRes
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.Component
import com.facebook.litho.LithoView
import com.facebook.litho.testing.TestCollection
import com.facebook.litho.testing.TestCollectionItem
import com.facebook.litho.testing.viewtree.ViewPredicates
import com.facebook.litho.testing.viewtree.ViewTree
import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Assertions

class TestCollectionAssert(testCollection: TestCollection) :
    AbstractAssert<TestCollectionAssert, TestCollection>(
        testCollection, TestCollectionAssert::class.java) {

  /** Assert the number of items in the collection. */
  fun hasSize(size: Int): TestCollectionAssert {
    Assertions.assertThat(actual.items).hasSize(size)
    return this
  }

  /** Assert a condition on a child at a given index. */
  fun onChild(withIndex: Int, condition: (TestCollectionItem) -> Boolean): TestCollectionAssert {
    Assertions.assertThat(condition(actual.items[withIndex])).isTrue
    return this
  }

  /** Assert a condition on a child with a given id. */
  fun onChild(withId: Any, condition: (TestCollectionItem) -> Boolean): TestCollectionAssert {
    Assertions.assertThat(condition(actual.items.first { it.id == withId })).isTrue
    return this
  }

  /** Assert a condition on the first child. */
  fun onFirstChild(condition: (TestCollectionItem) -> Boolean): TestCollectionAssert {
    Assertions.assertThat(condition(actual.items.first())).isTrue
    return this
  }

  /** Assert a condition on the last child. */
  fun onLastChild(condition: (TestCollectionItem) -> Boolean): TestCollectionAssert {
    Assertions.assertThat(condition(actual.items.last())).isTrue
    return this
  }

  /** Assert a condition on every item. */
  fun onChildren(condition: (List<TestCollectionItem>) -> Boolean): TestCollectionAssert {
    Assertions.assertThat(condition(actual.items)).isTrue
    return this
  }

  /** Assert a condition on all items filtered by the given predicate. */
  fun onChildren(
      matching: (TestCollectionItem) -> Boolean,
      condition: (List<TestCollectionItem>) -> Boolean
  ): TestCollectionAssert {
    Assertions.assertThat(condition(actual.items.filter(matching))).isTrue
    return this
  }

  /**
   * Assert the presence of equivalent components. Child parameters like `id` and `isSticky` are not
   * compared.
   */
  fun containsComponents(vararg components: Component): TestCollectionAssert {
    components.forEach { component ->
      val equivalentComponent = actual.items.firstOrNull { it.component.isEquivalentTo(component) }
      Assertions.assertThat(equivalentComponent).isNotNull
    }
    return this
  }

  /**
   * Assert the absence of equivalent components. Child parameters like `id` and`isSticky` are not
   * compared.
   */
  fun doesNotContainComponents(vararg components: Component): TestCollectionAssert {
    components.forEach { component ->
      val equivalentComponent = actual.items.firstOrNull { it.component.isEquivalentTo(component) }
      Assertions.assertThat(equivalentComponent).isNull()
    }
    return this
  }

  /**
   * Assert the components are exactly equivalent to the given list. Child parameters like `id` and
   * `isSticky` are not compared.
   */
  fun containsExactlyComponents(vararg components: Component): TestCollectionAssert {
    actual.items.zip(components).forEach { (a, b) ->
      Assertions.assertThat(a.component.isEquivalentTo(b)).isTrue
    }
    return this
  }

  /** Assert visible views within the collection has the [text] as its text. */
  fun hasVisibleText(text: String): TestCollectionAssert {
    Assertions.assertThat(actual.getLithoViews().hasVisibleText(text))
        .overridingErrorMessage("Expected visible text \"<%s>\", but was not found.", text)
        .isTrue
    return this
  }

  /** Assert visible views within the collection has the [textRes]'s text as its text. */
  fun hasVisibleText(@StringRes textRes: Int): TestCollectionAssert =
      hasVisibleText(
          ApplicationProvider.getApplicationContext<Context>().resources.getString(textRes))

  /** Assert visible views within the collection does not have the [text] as its text. */
  fun doesNotHaveVisibleText(text: String): TestCollectionAssert {
    Assertions.assertThat(actual.getLithoViews().hasVisibleText(text))
        .overridingErrorMessage("Did not expect visible text \"<%s>\"", text)
        .isFalse
    return this
  }

  /** Assert visible views within the collection does not have the [textRes]'s text as its text. */
  fun doesNotHaveVisibleText(@StringRes textRes: Int): TestCollectionAssert =
      doesNotHaveVisibleText(
          ApplicationProvider.getApplicationContext<Context>().resources.getString(textRes))

  private fun LithoView.hasVisibleText(text: String): Boolean {
    val viewTree = ViewTree.of(this)
    val children =
        viewTree.findChild(ViewPredicates.hasVisibleText(text), ViewPredicates.isVisible())
    return children?.isNotEmpty() == true
  }

  private fun List<LithoView>.hasVisibleText(text: String): Boolean {
    for (view in this) {
      if (view.hasVisibleText(text)) {
        return true
      }
    }
    return false
  }

  companion object {
    fun assertThat(actual: TestCollection): TestCollectionAssert = TestCollectionAssert(actual)
  }
}
