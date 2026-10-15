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

package com.facebook.litho.testing.viewtree

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.google.common.base.Joiner
import com.google.common.base.Predicate
import com.google.common.base.Predicates
import com.google.common.collect.ImmutableList
import kotlin.math.max
import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Assertions

/**
 * Assertions which require checking an entire view tree
 *
 * NOTE: Assertions looking for visible attributes are limited to checking the visibility of the
 * nodes, but do not check actual layout. So a visible view might have 0 pixels available for it in
 * actual app code and still pass the checks done here
 */
class ViewTreeAssert private constructor(actual: ViewTree) :
    AbstractAssert<ViewTreeAssert, ViewTree>(actual, ViewTreeAssert::class.java) {

  /**
   * Tests if any view in the hierarchy under the root, for which the path is visible, has the
   * requested piece of text as its text
   *
   * @param text the text to search for
   * @return the assertions object
   */
  fun hasVisibleText(text: String): ViewTreeAssert {
    val path = getPathToVisibleText(text)

    Assertions.assertThat(path)
        .overridingErrorMessage(if (path == null) getHasVisibleTextErrorMessage(text) else "")
        .isNotNull()

    return this
  }

  private fun getHasVisibleTextErrorMessage(text: String): String {
    var errorMsg =
        String.format(
            "Cannot find text \"%s\" in view hierarchy:%n%s. ",
            text,
            checkNotNull(actual).makeString(ViewExtractors.GET_TEXT_FUNCTION),
        )

    val similarPath = getPathToVisibleSimilarText(text)
    errorMsg +=
        if (similarPath != null) {
          String.format(
              "\nHowever, a near-match was found: \"%s\"",
              ViewExtractors.GET_TEXT_FUNCTION.apply(similarPath[similarPath.size - 1]),
          )
        } else {
          "\nNo near-match was found."
        }
    return errorMsg
  }

  /**
   * Tests if any view in the hierarchy under the root, for which the path is visible, has the
   * requested piece of text as its text and has a tag set on that TextView with the given tag id
   * and tag value.
   *
   * @param text the text to search for
   * @param tagId the tag to look for on the TextView containing the searched text
   * @param tagValue the expected value of the tag associated with tagId
   * @return the assertions object
   */
  fun hasVisibleTextWithTag(text: String, tagId: Int, tagValue: Any): ViewTreeAssert {
    val path = getPathToVisibleTextWithTag(text, tagId, tagValue)

    Assertions.assertThat(path)
        .overridingErrorMessage(
            "Cannot find text \"%s\" with tagId \"%d\" and value:%s in view hierarchy:%n%s",
            text,
            tagId,
            tagValue.toString(),
            checkNotNull(actual).makeString(ViewExtractors.GET_TEXT_FUNCTION),
        )
        .isNotNull()

    return this
  }

  /**
   * Tests if any view has visible text identified by the resource id
   *
   * @param resourceId resource id of the text
   * @return the assertions object
   */
  fun hasVisibleText(resourceId: Int): ViewTreeAssert {
    return hasVisibleText(
        ApplicationProvider.getApplicationContext<android.content.Context>()
            .resources
            .getString(resourceId),
    )
  }

  /**
   * Tests that all views in the hierarchy under the root, for which the path is visible, do not
   * have text equal to the given string
   *
   * @param text the text to search for
   * @return the assertions object
   */
  fun doesNotHaveVisibleText(text: String): ViewTreeAssert {
    val path = getPathToVisibleText(text)

    Assertions.assertThat(path)
        .overridingErrorMessage(
            "Found text \"%s\" in view hierarchy for path: %s",
            text,
            makeString(path),
        )
        .isNull()

    return this
  }

  /**
   * Tests if any view hierarchy under the root has the given view tag and value.
   *
   * @param tagId the id to look for
   * @param tagValue the value that the id should have
   * @return the assertions object
   */
  fun hasViewTag(tagId: Int, tagValue: Any): ViewTreeAssert {
    val path = getPathToViewTag(tagId, tagValue)

    Assertions.assertThat(path)
        .overridingErrorMessage(
            "Cannot find tag id \"%d\" with tag value \"%s\" in view hierarchy:%n%s",
            tagId,
            tagValue,
            checkNotNull(actual).makeString(ViewExtractors.generateGetViewTagFunction(tagId)),
        )
        .isNotNull()

    return this
  }

  /**
   * Tests if any view hierarchy under the root has the given contentDescription.
   *
   * @param contentDescription the contentDescription to search for
   * @return the assertions object
   */
  fun hasContentDescription(contentDescription: String): ViewTreeAssert {
    val path = getPathToContentDescription(contentDescription)

    Assertions.assertThat(path)
        .overridingErrorMessage(
            "Cannot find content description \"%s\" in view hierarchy:%n%s",
            contentDescription,
            checkNotNull(actual).makeString(ViewExtractors.GET_CONTENT_DESCRIPTION_FUNCTION),
        )
        .isNotNull()

    return this
  }

  /**
   * Tests if any view hierarchy under the root do not has the given contentDescription.
   *
   * @param contentDescription the contentDescription to search for
   * @return the assertions object
   */
  fun hasNoContentDescription(contentDescription: String): ViewTreeAssert {
    val path = getPathToContentDescription(contentDescription)

    Assertions.assertThat(path)
        .overridingErrorMessage(
            "Found content description \"%s\" in view hierarchy:%n%s while the content description" +
                " should not exist. ",
            contentDescription,
            checkNotNull(actual).makeString(ViewExtractors.GET_CONTENT_DESCRIPTION_FUNCTION),
            path,
        )
        .isNull()

    return this
  }

  /**
   * Tests that all views in the hierarchy under the root, for which the path is visible, do not
   * have text equal to the string matching the given resource id
   *
   * @param resourceId resource id of the text
   * @return the assertions object
   */
  fun doesNotHaveVisibleText(resourceId: Int): ViewTreeAssert {
    return doesNotHaveVisibleText(
        ApplicationProvider.getApplicationContext<android.content.Context>()
            .resources
            .getString(resourceId),
    )
  }

  /**
   * Tests if any view hierarchy under the root has the given contentDescription.
   *
   * @param resourceId the resId of the contentDescription to search for
   * @return the assertions object
   */
  fun hasContentDescription(resourceId: Int): ViewTreeAssert {
    return hasContentDescription(
        ApplicationProvider.getApplicationContext<android.content.Context>()
            .resources
            .getString(resourceId),
    )
  }

  private fun getPathToVisibleSimilarText(text: String): ImmutableList<View>? {
    return checkNotNull(actual)
        .findChild(
            Predicates.and(
                ViewPredicates.isVisible(),
                ViewPredicates.hasTextMatchingPredicate(
                    Predicate { input ->
                      val maxEditDistance = max(3, text.length / 4)
                      LevenshteinDistance.getLevenshteinDistance(text, input, maxEditDistance) <=
                          maxEditDistance
                    },
                ),
            ),
            ViewPredicates.isVisible(),
        )
  }

  private fun getPathToVisibleText(text: String): ImmutableList<View>? {
    return checkNotNull(actual)
        .findChild(ViewPredicates.hasVisibleText(text), ViewPredicates.isVisible())
  }

  private fun getPathToVisibleTextWithTag(
      text: String,
      tagId: Int,
      tagValue: Any,
  ): ImmutableList<View>? {
    return checkNotNull(actual)
        .findChild(
            ViewPredicates.hasVisibleTextWithTag(text, tagId, tagValue),
            ViewPredicates.isVisible(),
        )
  }

  private fun getPathToViewTag(tagId: Int, tagValue: Any): ImmutableList<View>? {
    return checkNotNull(actual).findChild(ViewPredicates.hasTag(tagId, tagValue))
  }

  private fun getPathToContentDescription(contentDescription: String): ImmutableList<View>? {
    return checkNotNull(actual).findChild(ViewPredicates.hasContentDescription(contentDescription))
  }

  /**
   * Tests if any view hierarchy under the root has the given accessibility role.
   *
   * @param accessibilityRole the accessibility role to search for
   * @return the assertions object
   */
  fun hasAccessibilityRole(accessibilityRole: String): ViewTreeAssert {
    AccessibilityTestHelper.assertHasAccessibilityRole(checkNotNull(actual), accessibilityRole)
    return this
  }

  /**
   * Tests if any view in the hierarchy under the root, for which the path is visible, has text that
   * matches the given regular expression
   *
   * @param pattern the regular expression to match against
   * @return the assertions object
   */
  fun hasVisibleTextMatching(pattern: String): ViewTreeAssert {
    val path = getPathToVisibleMatchingText(pattern)

    Assertions.assertThat(path)
        .overridingErrorMessage(
            "Cannot find text matching \"%s\" in view hierarchy:%n%s",
            pattern,
            checkNotNull(actual).makeString(ViewExtractors.GET_TEXT_FUNCTION),
        )
        .isNotNull()

    return this
  }

  /**
   * Tests that all views in the hierarchy under the root, for which the path is visible, do not
   * have text that matches against the given regular expression
   *
   * @param pattern the regular expression to match against
   * @return the assertions object(
   */
  fun doesNotHaveVisibleTextMatching(pattern: String): ViewTreeAssert {
    val path = getPathToVisibleMatchingText(pattern)

    Assertions.assertThat(path)
        .overridingErrorMessage(
            "Found pattern \"%s\" in view hierarchy for path: %s",
            pattern,
            makeString(path),
        )
        .isNull()

    return this
  }

  /**
   * Tests that all views in the hierarchy under the root, for which the path is visible, do not
   * have any text appearing on them
   *
   * @return the assertions object
   */
  fun doesNotHaveVisibleText(): ViewTreeAssert {
    val path = getPathToVisibleMatchingText(".+")

    Assertions.assertThat(path)
        .overridingErrorMessage(
            "Found text \"%s\" in view hierarchy for path: %s",
            getTextProof(path),
            makeString(path),
        )
        .isNull()

    return this
  }

  private fun getTextProof(path: ImmutableList<View>?): String {
    if (path == null) {
      return ""
    }

    val last = path[path.size - 1]
    return (last as TextView).text.toString()
  }

  private fun getPathToVisibleMatchingText(pattern: String): ImmutableList<View>? {
    return checkNotNull(actual)
        .findChild(ViewPredicates.hasVisibleMatchingText(pattern), ViewPredicates.isVisible())
  }

  private fun makeString(path: Iterable<View>?): String {
    return if (path != null) Joiner.on(" -> ").join(path) else ""
  }

  /**
   * Tests if any view in the hierarchy under the root, for which the path is visible, is displaying
   * the requested drawable by the given resource id.
   *
   * For this assertion to work, Robolectric must be immediately available and be able to load the
   * drawable corresponding to this resource id.
   *
   * @param resourceId the resource id of the drawable to look for
   * @return the assertions object
   */
  fun hasVisibleDrawable(resourceId: Int): ViewTreeAssert {
    @Suppress("DEPRECATION")
    hasVisibleDrawable(
        ApplicationProvider.getApplicationContext<android.content.Context>()
            .resources
            .getDrawable(resourceId),
    )
    return this
  }

  /**
   * Tests if any view in the hierarchy under the root, for which the path is visible, is displaying
   * the requested drawable
   *
   * @param drawable the drawable to look for
   * @return the assertions object
   */
  fun hasVisibleDrawable(drawable: Drawable): ViewTreeAssert {
    val path = getPathToVisibleWithDrawable(drawable)

    Assertions.assertThat(path)
        .overridingErrorMessage(
            "Did not find drawable %s in view hierarchy:%n%s",
            drawable,
            checkNotNull(actual).makeString(ViewExtractors.GET_DRAWABLE_FUNCTION),
        )
        .isNotNull()

    return this
  }

  /**
   * Tests all views in the hierarchy under the root, for which the path is visible, do not have the
   * requested drawable by the given resource id. For this assertion to work, Robolectric must be
   * immediately available and be able to load the drawable corresponding to this resource id.
   *
   * @param resourceId the resource id of the drawable to look for
   * @return the assertions object
   */
  fun doesNotHaveVisibleDrawable(resourceId: Int): ViewTreeAssert {
    @Suppress("DEPRECATION")
    doesNotHaveVisibleDrawable(
        ApplicationProvider.getApplicationContext<android.content.Context>()
            .resources
            .getDrawable(resourceId),
    )
    return this
  }

  /**
   * Tests all views in the hierarchy under the root, for which the path is visible, are not
   * displaying the requested drawable
   *
   * @param drawable the drawable to look for
   * @return the assertions object
   */
  fun doesNotHaveVisibleDrawable(drawable: Drawable): ViewTreeAssert {
    val path = getPathToVisibleWithDrawable(drawable)

    Assertions.assertThat(path)
        .overridingErrorMessage(
            "Found drawable %s in view hierarchy:%n%s",
            drawable,
            checkNotNull(actual).makeString(ViewExtractors.GET_DRAWABLE_FUNCTION),
        )
        .isNull()

    return this
  }

  /** Whether there is a visible view in the hierarchy with the given id. */
  fun hasVisibleViewWithId(viewId: Int): ViewTreeAssert {
    val path = getPathToVisibleWithId(viewId)

    Assertions.assertThat(path)
        .overridingErrorMessage(
            "Did not find visible view with id \"%s=%d\":%n%s",
            ViewTreeUtil.getResourceName(viewId),
            viewId,
            checkNotNull(actual).makeString(ViewExtractors.GET_VIEW_ID_FUNCTION),
        )
        .isNotNull()

    return this
  }

  /** Whether there is not a visible view in the hierarchy with the given id. */
  fun doesNotHaveVisibleViewWithId(viewId: Int): ViewTreeAssert {
    val path = getPathToVisibleWithId(viewId)

    Assertions.assertThat(path)
        .overridingErrorMessage(
            "Found visible view with id \"%s=%d\":%n%s",
            ViewTreeUtil.getResourceName(viewId),
            viewId,
            checkNotNull(actual).makeString(ViewExtractors.GET_VIEW_ID_FUNCTION),
        )
        .isNull()

    return this
  }

  @Suppress("UNCHECKED_CAST")
  fun <V : View> hasVisible(clazz: Class<V>, predicate: Predicate<V>): ViewTreeAssert {
    val conjunction =
        Predicates.and(
            Predicates.instanceOf(clazz),
            ViewPredicates.isVisible(),
            predicate as Predicate<View>,
        )

    val path = checkNotNull(actual).findChild(conjunction, ViewPredicates.isVisible())

    Assertions.assertThat(path)
        .overridingErrorMessage(
            "Did not find view for which given predicate is true in view hierarchy:%n%s",
            actual.makeString(null),
        )
        .isNotNull()

    return this
  }

  fun <V : View> doesNotHaveVisible(clazz: Class<V>, predicate: Predicate<Any>): ViewTreeAssert {
    val conjunction =
        Predicates.and(Predicates.instanceOf(clazz), ViewPredicates.isVisible(), predicate)

    val path = checkNotNull(actual).findChild(conjunction, ViewPredicates.isVisible())

    Assertions.assertThat(path)
        .overridingErrorMessage(
            "Found a view for which given predicate is true in view hierarchy:%n%s",
            actual.makeString(null),
        )
        .isNull()

    return this
  }

  private fun getPathToVisibleWithDrawable(drawable: Drawable): ImmutableList<View>? {
    return checkNotNull(actual)
        .findChild(ViewPredicates.hasVisibleDrawable(drawable), ViewPredicates.isVisible())
  }

  private fun getPathToVisibleWithId(viewId: Int): ImmutableList<View>? {
    return checkNotNull(actual)
        .findChild(ViewPredicates.hasVisibleId(viewId), ViewPredicates.isVisible())
  }

  companion object {
    @JvmStatic
    fun assertThat(actual: ViewTree): ViewTreeAssert {
      return ViewTreeAssert(actual)
    }
  }
}
