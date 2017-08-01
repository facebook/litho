/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.espresso;

import static org.hamcrest.Matchers.any;

import android.view.View;
import com.facebook.litho.LithoView;
import com.facebook.litho.LithoViewTestHelper;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Espresso matchers for {@link com.facebook.litho.LithoView}.
 *
 * This does only allow shallow matching on LithoViews and currently doesn't support
 * targeting individual components.
 */
public class LithoViewMatchers {

  /**
   * Matches a view that is a {@link com.facebook.litho.LithoView} that matches the given
   * subMatcher.
   */
  public static Matcher<View> lithoView(final Matcher<? extends LithoView> subMatcher) {
    return new BaseMatcher<View>() {
      @Override
      public boolean matches(Object item) {
        return item instanceof LithoView && subMatcher.matches((LithoView) item);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("Expected to be a LithoView matching: ");
        subMatcher.describeTo(description);
      }
    };
  }

  public static Matcher<View> lithoView() {
    return lithoView(any(LithoView.class));
  }

  /**
   * Find a {@link LithoView} containing a Component with the provided <pre>testKey</pre>.
   * Note that this finds any {@link LithoView} containing a mounted component with that key and
   * there's currently no way to be more specific.
   */
  public static Matcher<View> withTestKey(final String testKey) {
    return lithoView(new TypeSafeMatcher<LithoView>() {
      @Override
      protected boolean matchesSafely(LithoView lithoView) {
        return LithoViewTestHelper.findTestItem(lithoView, testKey) != null;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText(
            String.format(
                "Could not find an item with test key '%s' within the given LithoView.", testKey));
      }
    });
  }
}
