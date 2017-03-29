// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.testing.viewtree;

import java.util.List;
import java.util.regex.Pattern;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.facebook.litho.ComponentHost;
import com.facebook.litho.MatrixDrawable;

import com.google.common.base.Predicate;

/**
 * Utility APIs to query the state of components.
 */
class ComponentQueries {

  /**
   * Checks whether the given {@link ComponentHost} has the given text as it's text. It does not
   * look at the host's children.
   * @param host the component host
   * @param predicate the predicate to test
   * @return true if the host has the given text.
   */
  static boolean hasTextMatchingPredicate(final ComponentHost host, final Predicate<String> predicate) {
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
   * Checks whether the given {@link ComponentHost} contains the given drawable. It does not look
   * at the host's children.
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

    // Some Drawables do not have a description. However they can be compared directly.

    // The drawable being compared is a Robolectric shadow drawable whose bounds are not set.
    // However, the containingDrawable in its mounted state has its bounds set in the components
    // testing environment. Therefore, the bounds must be set to 0 for comparison.
    containingDrawable = containingDrawable.mutate();
    containingDrawable.setBounds(0, 0, 0, 0);

    if (drawable.equals(containingDrawable)) {
      return true;
    }

    // The drawable being compared might have pre-set bounds.
    drawable = drawable.mutate();
    drawable.setBounds(0, 0, 0, 0);

    return drawable.equals(containingDrawable);
  }

  /**
   * Checks whether the given {@link ComponentHost} satisfy the given Predicate.
   * @param host the component host
   * @param predicate the predicate
