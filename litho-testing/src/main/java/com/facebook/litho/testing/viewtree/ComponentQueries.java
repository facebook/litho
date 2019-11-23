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

package com.facebook.litho.testing.viewtree;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import com.facebook.litho.ComponentHost;
import com.facebook.litho.MatrixDrawable;
import com.google.common.base.Predicate;
import java.util.List;
import java.util.regex.Pattern;
import org.robolectric.Shadows;

/** Utility APIs to query the state of components. */
class ComponentQueries {

  /**
   * Checks whether the given {@link ComponentHost} has the given text as it's text. It does not
   * look at the host's children.
   *
   * @param host the component host
   * @param predicate the predicate to test
   * @return true if the host has the given text.
   */
  static boolean hasTextMatchingPredicate(
      final ComponentHost host, final Predicate<String> predicate) {
    for (final CharSequence foundText : host.getTextContent().getTextItems()) {
      if (predicate.apply(foundText.toString())) {
        return true;
      }
    }

    return false;
  }

  /**
   * Checks whether the given {@link ComponentHost} matches the given pattern. It does not look at
   * the host's children.
   *
   * @param host the component host
   * @param pattern the pattern to match
   * @return true if the host has text matching the pattern.
   */
  static boolean matchesPattern(final ComponentHost host, final Pattern pattern) {
    for (final CharSequence foundText : host.getTextContent().getTextItems()) {
      if (pattern.matcher(foundText).find()) {
        return true;
      }
    }

    return false;
  }

  /**
   * Checks whether the given {@link ComponentHost} contains the given drawable. It does not look at
   * the host's children.
   *
   * @param host the component host
   * @param drawable the drawable to look for
   * @return true if the given host contains the drawable.
   */
  static boolean hasDrawable(final ComponentHost host, final Drawable drawable) {
    return satisfiesPredicate(
        host,
        new Predicate<Drawable>() {
          @Override
          public boolean apply(final Drawable input) {
            return hasDrawable(input, drawable);
          }
        });
  }

  private static boolean hasDrawable(Drawable containingDrawable, Drawable drawable) {
    while (containingDrawable instanceof MatrixDrawable) {
      containingDrawable = ((MatrixDrawable) containingDrawable).getMountedDrawable();
    }

    // Workaround a bug in Robolectric's BitmapDrawable implementation.
    if (drawable instanceof BitmapDrawable && !(containingDrawable instanceof BitmapDrawable)) {
      return false;
    }

    final String drawnDrawableDescription = ViewPredicates.getDrawnDrawableDescription(drawable);
    if (!drawnDrawableDescription.isEmpty()) {
      return ViewPredicates.getDrawnDrawableDescription(containingDrawable)
          .contains(drawnDrawableDescription);
    }

    // Robolectric 3.X provides a shadow implementation of equals() for Drawables, but it only
    // checks that the bounds are equal, which is a pretty weak assertion. This buggy equals()
    // implementation was removed in Robolectric 4.0, and Android Drawable does not implement
    // equals().

    // For Drawables created from a resource we can compare the resource ID they were created with.
    int containingDrawableResId = Shadows.shadowOf(containingDrawable).getCreatedFromResId();
    int drawableResId = Shadows.shadowOf(drawable).getCreatedFromResId();
    if (drawableResId != View.NO_ID && containingDrawableResId == drawableResId) {
      return true;
    }

    // Otherwise we cannot meaningfully compare them. Fall back to pointer equality.
    return containingDrawable == drawable;
  }

  /**
   * Checks whether the given {@link ComponentHost} satisfy the given Predicate.
   *
   * @param host the component host
   * @param predicate the predicate
   * @return true if the ComponentHost satisfies the condition.
   */
  private static boolean satisfiesPredicate(
      final ComponentHost host, final Predicate<Drawable> predicate) {
    final List<Drawable> drawables = host.getDrawables();
    for (final Drawable drawable : drawables) {
      if (predicate.apply(drawable)) {
        return true;
      }
    }

    for (final Drawable drawable : host.getImageContent().getImageItems()) {
      if (predicate.apply(drawable)) {
        return true;
      }
    }

    return false;
  }
}
