/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.testing.espresso;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.is;

import android.view.View;
import androidx.test.espresso.matcher.ViewMatchers;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentHost;
import com.facebook.litho.ComponentLifecycle;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

/**
 * Espresso matchers for {@link ComponentHost}.
 */
public class ComponentHostMatchers {

  /**
   * Matches a view that is a ComponentHost that matches subMatcher.
   *
   * In Espresso tests, when you need to match a View, we recommend
   * using this matcher and nest any of the other matchers in this
   * class along with it. For example
   * <code>componentHost(withText("foobar"))</code> or
   * <code>componentHost(withContentDescription("foobar"))</code>.
   *
   * While it's definitely possible to use Espresso's ViewMatchers
   * directly to match ComponentHosts, using these methods ensure that
   * we can handle weirdness in the view hierarchy that comes from the
   * component stack.
   */
  public static Matcher<View> componentHost(final Matcher<? extends ComponentHost> subMatcher) {
    return new BaseMatcher<View>() {
      @Override
      public boolean matches(Object item) {
        if (!(item instanceof ComponentHost)) {
          return false;
        }

        return subMatcher.matches((ComponentHost) item);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("Expected to be a ComponentHost matching: ");
        subMatcher.describeTo(description);
      }
    };
  }

  /**
   * Matches a ComponentHost that has mounted a Component with a
   * lifecycle that's matched by the {@code lifecycleMatcher}.
   */
  public static Matcher<ComponentHost> withLifecycle(
      final Matcher<? extends ComponentLifecycle> lifecycleMatcher) {
    return new BaseMatcher<ComponentHost>() {
      private StringBuilder mTypes = new StringBuilder();

      @Override
      public boolean matches(Object item) {
        mTypes = new StringBuilder();
        try {
          if (!(item instanceof ComponentHost)) {
            return false;
          }

          ComponentHost host = (ComponentHost) item;
          Method getMountItemCount = ComponentHost.class.getDeclaredMethod("getMountItemCount");
          Method getMountItemAt =
              ComponentHost.class.getDeclaredMethod("getMountItemAt", int.class);

          getMountItemCount.setAccessible(true);
          getMountItemAt.setAccessible(true);

          int count = (int) getMountItemCount.invoke(host);
          for (int i = 0; i < count; i++) {
            Object mountItem = getMountItemAt.invoke(host, i);
            Method getComponent = mountItem.getClass().getDeclaredMethod("getComponent");
            getComponent.setAccessible(true);
            Component component = (Component) getComponent.invoke(mountItem);
            if (lifecycleMatcher.matches(component)) {
              return true;
            }
            mTypes.append(" " + component.getClass().getName());
          }

          return false;
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("Expected at least one mount item's lifecycle to match: ");
        lifecycleMatcher.describeTo(description);
        description.appendText("We found the following lifecycle types:" + mTypes.toString());
      }
    };
  }

  /**
   * Matches a ComponentHost which is displaying text that matches
   * {@code textMatcher}
   */
  public static Matcher<View> componentHostWithText(final Matcher<String> textMatcher) {
    return componentHost(withText(textMatcher));
  }

  public static Matcher<ComponentHost> withText(final Matcher<String> textMatcher) {
    return new BaseMatcher<ComponentHost>() {
      @Override
      public boolean matches(Object item) {
        if (!(item instanceof ComponentHost)) {
          return false;
        }

        ComponentHost host = (ComponentHost) item;
        for (CharSequence foundText : host.getTextContent().getTextItems()) {
          if (foundText != null && textMatcher.matches(foundText.toString())) {
            return true;
          }
        }

        return false;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("ComponentHost should have text that matches: ");
        textMatcher.describeTo(description);
      }
    };
  }

  /**
   * Matches a ComponentHost which is displaying {@code text}. This
   * is equivalent to {@code componentHostWithText(is(text))}.
   */
  public static Matcher<View> componentHostWithText(final String text) {
    return componentHost(withText(text));
  }

  public static Matcher<ComponentHost> withText(String text) {
    return withText(is(text));
  }

  public static Matcher<ComponentHost> withContentDescription(CharSequence text) {
    return wrapped(ViewMatchers.withContentDescription(is(text)));
  }

  public static Matcher<ComponentHost> withContentDescription(
      final Matcher<CharSequence> textMatcher) {
    return wrapped(ViewMatchers.withContentDescription(textMatcher));
  }

  public static Matcher<ComponentHost> withTagValue(Matcher<Object> value) {
    return wrapped(ViewMatchers.withTagValue(value));
  }

  public static Matcher<ComponentHost> withTagKey(int key) {
    return wrapped(ViewMatchers.withTagKey(key));
  }

  public static Matcher<ComponentHost> withTagKey(int key, Matcher<Object> value) {
    return wrapped(ViewMatchers.withTagKey(key, value));
  }

  public static Matcher<ComponentHost> isClickable() {
    return wrapped(ViewMatchers.isClickable());
  }

  public static Matcher<View> componentHost() {
    return componentHost(any(ComponentHost.class));
  }

  private static Matcher<ComponentHost> wrapped(final Matcher<? extends View> matcher) {
    return new BaseMatcher<ComponentHost>() {
      @Override
      public boolean matches(Object item) {
        return matcher.matches(item);
      }

      @Override
      public void describeTo(Description description) {
        matcher.describeTo(description);
      }
    };
  }

  private ComponentHostMatchers() {
  }
}
