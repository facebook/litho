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

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.TextView
import com.facebook.litho.ComponentHost
import com.facebook.rendercore.text.RCTextView
import com.google.common.base.Predicate
import com.google.common.base.Predicates
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.regex.Pattern
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowView

/** A collection of useful predicates over Android views for tests */
object ViewPredicates {

  /**
   * Returns a predicate that returns true if the applied on view's text is equal to the given text.
   * substring.
   *
   * @param predicate the predicate with which to test the text
   * @return the predicate
   */
  @JvmStatic
  fun hasTextMatchingPredicate(predicate: Predicate<String>): Predicate<View> {
    return Predicate { input ->
      if (predicate.apply(extractString(input))) {
        return@Predicate true
      }

      if (input is ComponentHost) {
        return@Predicate ComponentQueries.hasTextMatchingPredicate(input, predicate)
      }

      false
    }
  }

  /**
   * Returns a predicate that returns true if the applied on view's text is equal to the given text.
   * substring.
   *
   * @param text the text to check
   * @return the predicate
   */
  @JvmStatic
  fun hasText(text: String): Predicate<View> {
    return hasTextMatchingPredicate(Predicates.equalTo(text))
  }

  @JvmStatic
  fun hasTag(tagId: Int, tagValue: Any): Predicate<View> {
    return Predicate { input ->
      val tag = input.getTag(tagId)
      tag != null && tag == tagValue
    }
  }

  @JvmStatic
  fun hasContentDescription(contentDescription: String): Predicate<View> {
    return Predicate { input ->
      if (input is ComponentHost) {
        val contentDescriptions = input.contentDescriptions
        contentDescriptions.contains(contentDescription)
      } else {
        contentDescription == input.contentDescription
      }
    }
  }

  @JvmStatic
  fun hasAccessibilityRole(accessibilityRole: String): Predicate<View> {
    return NodeInfoTestHelper.createPropertyPredicate(
        "accessibilityRole",
        accessibilityRole,
    ) {
      it.accessibilityRole
    }
  }

  @JvmStatic
  fun hasVisibleText(text: String): Predicate<View> {
    return Predicates.and(isVisible(), hasText(text))
  }

  @JvmStatic
  fun hasVisibleTextWithTag(text: String, tagId: Int, tagValue: Any): Predicate<View> {
    return Predicates.and(hasVisibleText(text), hasTag(tagId, tagValue))
  }

  @JvmStatic
  fun matchesText(text: String): Predicate<View> {
    val pattern = Pattern.compile(text)

    return Predicate { input ->
      if (pattern.matcher(extractString(input)).find()) {
        return@Predicate true
      }

      if (input is ComponentHost) {
        return@Predicate ComponentQueries.matchesPattern(input, pattern)
      }

      false
    }
  }

  @JvmStatic
  fun hasVisibleMatchingText(text: String): Predicate<View> {
    return Predicates.and(isVisible(), matchesText(text))
  }

  @JvmStatic
  fun isVisible(): Predicate<View> {
    return Predicate { input -> input.visibility == View.VISIBLE }
  }

  @Suppress("UNCHECKED_CAST")
  @JvmStatic
  fun isClass(clazz: Class<out View>): Predicate<View> {
    return Predicates.instanceOf<View>(clazz) as Predicate<View>
  }

  /** Tries to extract the description of a drawn drawable from a canvas (legacy graphics mode) */
  @JvmStatic
  fun getDrawnDrawableDescription(drawable: Drawable): String {
    val canvas = Canvas()
    drawable.draw(canvas)
    val shadowCanvas = Shadows.shadowOf(canvas)
    return shadowCanvas.description
  }

  private fun extractString(view: View): String {
    val text: CharSequence? =
        when (view) {
          is TextView -> view.text
          is RCTextView -> view.text
          else -> null
        }
    return text?.toString() ?: ""
  }

  @JvmStatic
  fun hasDrawable(drawable: Drawable): Predicate<View> {
    return Predicate { input ->
      if (input is ComponentHost) {
        return@Predicate ComponentQueries.hasDrawable(input, drawable)
      }

      if (ShadowView.useRealGraphics()) {
        // Native graphics mode: use bitmap comparison
        BitmapComparisonUtils.viewMatchesDrawable(input, drawable)
      } else {
        // Legacy graphics mode: use ShadowCanvas description
        val drawnDrawableDescription =
            extractResourceIdFromDrawnDescription(getDrawnDrawableDescription(drawable))
        val drawnViewDescription =
            extractResourceIdFromDrawnDescription(getDrawnViewDescription(input))
        drawnDrawableDescription.isNotEmpty() &&
            drawnViewDescription.contains(drawnDrawableDescription)
      }
    }
  }

  private fun extractResourceIdFromDrawnDescription(description: String): String {
    val resourceIdIndex = description.indexOf("resource:")
    if (resourceIdIndex == -1) {
      return description
    }
    return description.substring(resourceIdIndex)
  }

  @JvmStatic
  fun hasVisibleDrawable(drawable: Drawable): Predicate<View> {
    return Predicates.and(isVisible(), hasDrawable(drawable))
  }

  /** @return A Predicate which is true if the view is visible and has the given id. */
  @JvmStatic
  fun hasVisibleId(viewId: Int): Predicate<View> {
    return Predicates.and(isVisible(), hasId(viewId))
  }

  /**
   * Tries to extract the description of a drawn view from a canvas (legacy graphics mode)
   *
   * Since Robolectric can screw up [View.draw], this uses reflection to call [View.onDraw] and give
   * you a canvas that has all the information drawn into it. This is useful for asserting some view
   * draws something specific to a canvas.
   *
   * @param view the view to draw
   */
  private fun getDrawnViewDescription(view: View): String {
    val canvas = Canvas()
    view.draw(canvas)
    val shadowCanvas = Shadows.shadowOf(canvas)
    if (shadowCanvas.description.isNotEmpty()) {
      return shadowCanvas.description
    }

    try {
      val onDraw =
          getOnDrawMethod(view.javaClass)
              ?: throw RuntimeException(
                  "${view.javaClass.canonicalName} has no implementation of View.onDraw(), which should be impossible",
              )

      onDraw.invoke(view, canvas)
      val shadowCanvas2 = Shadows.shadowOf(canvas)
      return shadowCanvas2.description
    } catch (e: IllegalAccessException) {
      throw RuntimeException(e)
    } catch (e: InvocationTargetException) {
      throw RuntimeException(e)
    }
  }

  /**
   * [View.onDraw] is used by Robolectric to get the view description after rendering the view. This
   * method is protected, but we need a reference to it. We walk up the class definition hierarchy
   * to find where it's declared and return it.
   *
   * [Class.getMethod] does work recursively, but only on public methods and so doesn't fit here.
   *
   * see [getDrawnViewDescription]
   *
   * @param viewClass
   * @return the method instance, or null if no definition can be found
   */
  private fun getOnDrawMethod(viewClass: Class<out View>?): Method? {
    if (viewClass == null) {
      return null
    }

    try {
      val onDraw = viewClass.getDeclaredMethod("onDraw", Canvas::class.java)
      onDraw.isAccessible = true
      return onDraw
    } catch (e: NoSuchMethodException) {
      // swallow exception and recur
    }

    val superclass = viewClass.superclass
    if (superclass == null || !View::class.java.isAssignableFrom(superclass)) {
      return null
    }
    @Suppress("UNCHECKED_CAST")
    return getOnDrawMethod(superclass as Class<out View>)
  }

  @JvmStatic
  fun hasId(id: Int): Predicate<View> {
    return Predicate { input -> input.id == id }
  }
}
