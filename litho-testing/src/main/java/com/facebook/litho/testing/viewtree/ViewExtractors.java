/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.viewtree;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.facebook.litho.ComponentHost;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Function objects used for extracting specific information out of Android classes
 */
final class ViewExtractors {

  private ViewExtractors() {}

  public static final Function<View, String> GET_TEXT_FUNCTION = new Function<View, String>() {
    @Override
    public String apply(@Nullable View input) {
      CharSequence text = null;
      if (input instanceof ComponentHost) {
        List<CharSequence> strings = ((ComponentHost) input).getTextContent().getTextItems();
        if (!strings.isEmpty()) {
          text = Joiner.on("\", and \"").join(strings);
        }
      } else if (input instanceof TextView) {
        text = ((TextView) input).getText();
      }
      if (text == null) {
        return String.format(
            "No text found, view is %s",
            getVisibilityString(input.getVisibility()));
      }

      return String.format(
          "Found text: \"%s\", view is %s",
          Strings.nullToEmpty(text.toString()),
          getVisibilityString(input.getVisibility()));
    }
  };

  public static final Function<View, String> GET_DRAWABLE_FUNCTION = new Function<View, String>() {
    @Override
    public String apply(@Nullable View input) {
      if (!(input instanceof ImageView)) {
        return "No drawable found";
      }
      return String.format(
          "Found drawable: \"%s\", view is %s",
          getDrawableString((ImageView) input),
          getVisibilityString(input.getVisibility()));
    }
  };

  public static Function<View, String> GET_CONTENT_DESCRIPTION_FUNCTION
      = new Function<View, String>() {
    @Override
    public String apply(@Nullable View input) {
      if (input == null) {
        return "Provided view was null";
      }
      if (input.getContentDescription() == null) {
        return String.format(
            "No content description found, view is %s",
            getVisibilityString(input.getVisibility()));
      }
      return String.format(
          "Found content description: \"%s\", view is %s",
          input.getContentDescription(),
          getVisibilityString(input.getVisibility()));
    }
  };

  /**
   * Generates a function that extracts information about view tags from the given view.
   * @param key key that identifies the tag
   * @return function that extracts information about view tags
   */
  public static Function<View, String> generateGetViewTagFunction(final int key) {
    return new Function<View, String>() {
      @Override
      public String apply(View input) {
        if (input.getTag(key) == null) {
          return String.format(
              "No view tag found, view is %s",
              getVisibilityString(input.getVisibility()));
        }

        return String.format(
            "Found view tag: \"%s\", view is %s",
            input.getTag(key),
            getVisibilityString(input.getVisibility()));
      }
    };
  }

  /** A function that inputs a view and outputs the view's id and visibility. */
  public static Function<View, String> GET_VIEW_ID_FUNCTION = new Function<View, String>() {
    @Override
    public String apply(View input) {
      int id = input.getId();
      return String.format(
          "View with id \"%s=%d\" is %s.",
          ViewTreeUtil.getResourceName(id),
          id,
          getVisibilityString(input.getVisibility()));
    }
  };

  private static String getDrawableString(ImageView input) {
    Drawable drawable = input.getDrawable();
    return String.valueOf(drawable);
  }

  private static String getVisibilityString(int visibility) {
    return visibility == View.VISIBLE ? "visible" : "not visible";
  }
}
