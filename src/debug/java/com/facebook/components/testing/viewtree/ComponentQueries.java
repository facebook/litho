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

