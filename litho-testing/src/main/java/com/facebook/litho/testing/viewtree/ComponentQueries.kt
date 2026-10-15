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
import android.os.Build
import android.view.View
import com.facebook.litho.ComponentHost
import com.facebook.litho.MatrixDrawable
import com.facebook.litho.RCTextViewTextContent
import com.facebook.litho.drawable.ComparableDrawable
import com.google.common.base.Predicate
import java.util.regex.Pattern
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowView

/** Utility APIs to query the state of components. */
internal object ComponentQueries {

  /**
   * Checks whether the given [ComponentHost] has the given text as its text. It does not look at
   * the host's children.
   *
   * @param host the component host
   * @param predicate the predicate to test
   * @return true if the host has the given text.
   */
  @JvmStatic
  fun hasTextMatchingPredicate(host: ComponentHost, predicate: Predicate<String>): Boolean {
    return host.ownTextContentText.any { foundText -> predicate.apply(foundText.toString()) }
  }

  /**
   * Checks whether the given [ComponentHost] matches the given pattern. It does not look at the
   * host's children.
   *
   * @param host the component host
   * @param pattern the pattern to match
   * @return true if the host has text matching the pattern.
   */
  @JvmStatic
  fun matchesPattern(host: ComponentHost, pattern: Pattern): Boolean {
    return host.ownTextContentText.any { foundText -> pattern.matcher(foundText).find() }
  }

  /**
   * Checks whether the given [ComponentHost] contains the given drawable. It does not look at the
   * host's children.
   *
   * @param host the component host
   * @param drawable the drawable to look for
   * @return true if the given host contains the drawable.
   */
  @JvmStatic
  fun hasDrawable(host: ComponentHost, drawable: Drawable): Boolean {
    return satisfiesPredicate(host) { input -> hasDrawable(input, drawable) }
  }

  private fun hasDrawable(containingDrawable: Drawable, drawable: Drawable): Boolean {
    // Note Android Drawable does not implement equals().

    var current = containingDrawable
    while (current is MatrixDrawable<*>) {
      current = current.mountedDrawable ?: return false
    }

    if (current === drawable) {
      return true
    }

    // Use ComparableDrawable's isEquivalentTo for meaningful comparison if both implement it
    if (current is ComparableDrawable && drawable is ComparableDrawable) {
      if (current.isEquivalentTo(drawable)) {
        return true
      }
    }

    // For Drawables created from a resource we can compare the resource ID they were created with.
    val containingDrawableResId = Shadows.shadowOf(current)?.createdFromResId
    val drawableResId = Shadows.shadowOf(drawable)?.createdFromResId
    if (
        drawableResId != View.NO_ID &&
            drawableResId != null &&
            containingDrawableResId != null &&
            containingDrawableResId == drawableResId
    ) {
      return true
    }

    if (ShadowView.useRealGraphics()) {
      // Native graphics mode: use bitmap comparison
      return BitmapComparisonUtils.drawablesMatch(current, drawable)
    } else {
      // Legacy graphics mode: use ShadowCanvas description
      val drawnDrawableDescription = ViewPredicates.getDrawnDrawableDescription(drawable)
      if (drawnDrawableDescription.isNotEmpty()) {
        return ViewPredicates.getDrawnDrawableDescription(current)
            .contains(drawnDrawableDescription)
      }
      return false
    }
  }

  /**
   * Checks whether the given [ComponentHost] satisfies the given Predicate.
   *
   * @param host the component host
   * @param predicate the predicate
   * @return true if the ComponentHost satisfies the condition.
   */
  private fun satisfiesPredicate(host: ComponentHost, predicate: Predicate<Drawable>): Boolean {
    val drawables = ArrayList(host.drawables)
    host.background?.let { drawables.add(it) }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      host.foreground?.let { drawables.add(it) }
    }
    if (drawables.any { predicate.apply(it) }) {
      return true
    }

    return host.imageContent.imageItems.any { predicate.apply(it) }
  }

  /**
   * The host's text, as legacy Text reports it: Litho Text's primitive text views count as part of
   * their host (see [RCTextViewTextContent.isMatchedThroughHost]), so lookups return the host like
   * they do for legacy Text. Other text views are matched on their own.
   */
  val ComponentHost.ownTextContentText: List<CharSequence>
    get() =
        textContent
            .filter { it !is RCTextViewTextContent || it.isMatchedThroughHost }
            .flatMap { it.textList }
}
