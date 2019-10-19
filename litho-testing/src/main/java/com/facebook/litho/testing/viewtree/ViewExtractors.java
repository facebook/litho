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

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.facebook.litho.ComponentHost;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/** Function objects used for extracting specific information out of Android classes */
final class ViewExtractors {

  private ViewExtractors() {}

  public static final Function<View, String> GET_TEXT_FUNCTION =
      new Function<View, String>() {
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
                "No text found, view is %s", getVisibilityString(input.getVisibility()));
          }

          return String.format(
              "Found text: \"%s\", view is %s",
              Strings.nullToEmpty(text.toString()), getVisibilityString(input.getVisibility()));
        }
      };

  public static final Function<View, String> GET_DRAWABLE_FUNCTION =
      new Function<View, String>() {
        @Override
        public String apply(@Nullable View input) {
          if (input instanceof ImageView) {
            return String.format(
                "Found drawable: \"%s\", view is %s",
                String.valueOf(((ImageView) input).getDrawable()),
                getVisibilityString(input.getVisibility()));
          } else if (input instanceof ComponentHost) {
            ComponentHost host = (ComponentHost) input;
            List<String> drawables = new ArrayList<>();
            for (Drawable d : host.getDrawables()) {
              drawables.add(String.valueOf(d));
            }
            for (Drawable d : host.getImageContent().getImageItems()) {
              drawables.add(String.valueOf(d));
            }
            return String.format(
                "Found drawables: \"%s\", view is %s",
                TextUtils.join("\", \"", drawables), getVisibilityString(input.getVisibility()));
          } else {
            return "No drawable found";
          }
        }
      };

  public static final Function<View, String> GET_CONTENT_DESCRIPTION_FUNCTION =
      new Function<View, String>() {
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
              input.getContentDescription(), getVisibilityString(input.getVisibility()));
        }
      };

  /**
   * Generates a function that extracts information about view tags from the given view.
   *
   * @param key key that identifies the tag
   * @return function that extracts information about view tags
   */
  public static Function<View, String> generateGetViewTagFunction(final int key) {
    return new Function<View, String>() {
      @Override
      public String apply(View input) {
        if (input.getTag(key) == null) {
          return String.format(
              "No view tag found, view is %s", getVisibilityString(input.getVisibility()));
        }

        return String.format(
            "Found view tag: \"%s\", view is %s",
            input.getTag(key), getVisibilityString(input.getVisibility()));
      }
    };
  }

  /** A function that inputs a view and outputs the view's id and visibility. */
  public static final Function<View, String> GET_VIEW_ID_FUNCTION =
      new Function<View, String>() {
        @Override
        public String apply(View input) {
          int id = input.getId();
          return String.format(
              "View with id \"%s=%d\" is %s.",
              ViewTreeUtil.getResourceName(id), id, getVisibilityString(input.getVisibility()));
        }
      };

  private static String getVisibilityString(int visibility) {
    return visibility == View.VISIBLE ? "visible" : "not visible";
  }
}
