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

package com.facebook.litho.testing.assertj;

import com.facebook.litho.Component;
import com.facebook.litho.LithoView;
import com.facebook.litho.testing.TestLithoView;
import java.util.List;
import kotlin.Pair;
import kotlin.reflect.KProperty1;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.ListAssert;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;

/** Common entry point for Litho assertions. */
public class LithoAssertions {

  public static LithoComponentAssert assertThat(Component component) {
    return new LithoComponentAssert(component);
  }

  public static LithoViewAssert assertThat(LithoView lithoView) {
    return LithoViewAssert.assertThat(lithoView);
  }

  public static LithoViewAssert assertThat(TestLithoView testLithoView) {
    return LithoViewAssert.assertThat(testLithoView.getLithoView());
  }

  public static ListAssert<Component> assertThat(List<Component> componentsList) {
    return new ListAssert<Component>(componentsList);
  }

  /** TODO: T106084343 deprecate currently existing usage of Component assert. */
  public static class LithoComponentAssert extends AbstractAssert<LithoComponentAssert, Component> {

    LithoComponentAssert(Component actual) {
      super(actual, LithoComponentAssert.class);
    }

    /** Assert that a given {@link Component} has a property equaling the provided value. */
    public <T1, T2> LithoComponentAssert hasProps(Pair<KProperty1<T2, T1>, T1>... propsValuePairs) {
      for (Pair<KProperty1<T2, T1>, T1> pair : propsValuePairs) {
        MatcherAssert.assertThat(
            pair.getFirst().get((T2) actual), IsEqual.equalTo(pair.getSecond()));
      }
      return this;
    }

    /** Assert that a given {@link Component} has a property matching the provided matcher. */
    public <T1, T2> LithoComponentAssert hasPropsMatching(
        Pair<KProperty1<T2, T1>, Matcher<T1>>... propsMatcherPairs) {
      for (Pair<KProperty1<T2, T1>, Matcher<T1>> pair : propsMatcherPairs) {
        MatcherAssert.assertThat(pair.getFirst().get((T2) actual), pair.getSecond());
      }
      return this;
    }
  }
}
