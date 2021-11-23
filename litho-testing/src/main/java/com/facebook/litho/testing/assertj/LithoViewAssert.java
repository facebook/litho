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

import static com.facebook.litho.componentsfinder.ComponentsFinderKt.findAllComponentsInLithoView;
import static com.facebook.litho.testing.assertj.ComponentConditions.typeIs;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import com.facebook.litho.Component;
import com.facebook.litho.LithoView;
import com.facebook.litho.LithoViewTestHelper;
import com.facebook.litho.TestItem;
import com.facebook.litho.testing.viewtree.ViewTree;
import com.facebook.litho.testing.viewtree.ViewTreeAssert;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import kotlin.reflect.KClass;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;

/**
 * Assertion methods for {@link LithoView}s.
 *
 * <p>To create an instance of this class, invoke <code>
 * {@link LithoViewAssert#assertThat(LithoView)}</code>.
 *
 * <p>Alternatively, use {@link LegacyLithoAssertions} which provides entry points to all Litho
 * AssertJ helpers.
 */
public class LithoViewAssert extends AbstractAssert<LithoViewAssert, LithoView> {

  public static LithoViewAssert assertThat(LithoView actual) {
    return new LithoViewAssert(actual);
  }

  LithoViewAssert(LithoView actual) {
    super(actual, LithoViewAssert.class);
  }

  public LithoViewAssert containsTestKey(String testKey) {
    return containsTestKey(testKey, once());
  }

  public LithoViewAssert containsTestKey(String testKey, OccurrenceCount count) {
    final Deque<TestItem> testItems = LithoViewTestHelper.findTestItems(actual, testKey);
    Assertions.assertThat(testItems)
        .hasSize(count.times)
        .overridingErrorMessage(
            "Expected to find test key <%s> in LithoView <%s> %s, but %s.",
            testKey,
            actual,
            count,
            testItems.isEmpty()
                ? "couldn't find it"
                : String.format(Locale.ROOT, "saw it %d times instead", testItems.size()))
        .isNotNull();

    return this;
  }

  @SuppressWarnings("VisibleForTests")
  public LithoViewAssert doesNotContainTestKey(String testKey) {
    final TestItem testItem = LithoViewTestHelper.findTestItem(actual, testKey);
    final Rect bounds = testItem == null ? null : testItem.getBounds();

    Assertions.assertThat(testItem)
        .overridingErrorMessage(
            "Expected not to find test key <%s> in LithoView <%s>, but it was present at "
                + "bounds %s.",
            testKey, actual, bounds)
        .isNull();

    return this;
  }

  private ViewTreeAssert assertThatViewTree() {
    return ViewTreeAssert.assertThat(ViewTree.of(actual));
  }

  /** Assert that any view in the given Component has the provided content description. */
  public LithoViewAssert hasContentDescription(String contentDescription) {
    assertThatViewTree().hasContentDescription(contentDescription);

    return this;
  }

  /**
   * Assert that the given component contains the drawable identified by the provided drawable
   * resource id.
   */
  public LithoViewAssert hasVisibleDrawable(@DrawableRes int drawableRes) {
    assertThatViewTree().hasVisibleDrawable(drawableRes);

    return this;
  }

  /** Assert that the given component contains the drawable provided. */
  public LithoViewAssert hasVisibleDrawable(Drawable drawable) {
    assertThatViewTree().hasVisibleDrawable(drawable);

    return this;
  }

  /** Inverse of {@link #hasVisibleDrawable(Drawable)} */
  public LithoViewAssert doesNotHaveVisibleDrawable(Drawable drawable) {
    assertThatViewTree().doesNotHaveVisibleDrawable(drawable);

    return this;
  }

  /** Inverse of {@link #hasVisibleDrawable(int)} */
  public LithoViewAssert doesNotHaveVisibleDrawable(@DrawableRes int drawableRes) {
    assertThatViewTree().doesNotHaveVisibleDrawable(drawableRes);

    return this;
  }

  /** Assert that the given component has the exact text provided. */
  public LithoViewAssert hasVisibleText(String text) {
    assertThatViewTree().hasVisibleText(text);

    return this;
  }

  /** Assert that the given component has the exact text identified by resource id. */
  public LithoViewAssert hasVisibleText(@StringRes int resourceId) {
    assertThatViewTree().hasVisibleText(resourceId);

    return this;
  }

  /** Inverse of {@link #hasVisibleText(String)} */
  public LithoViewAssert doesNotHaveVisibleText(String text) {
    assertThatViewTree().doesNotHaveVisibleText(text);

    return this;
  }

  /** Inverse of {@link #hasVisibleText(int)} */
  public LithoViewAssert doesNotHaveVisibleText(@StringRes int resourceId) {
    assertThatViewTree().doesNotHaveVisibleText(resourceId);

    return this;
  }

  /** Assert that the given component contains the provided pattern. */
  public LithoViewAssert hasVisibleTextMatching(String pattern) {
    assertThatViewTree().hasVisibleTextMatching(pattern);

    return this;
  }

  /**
   * Assert that the given component contains the provided text. Useful if checking portion of text
   * that may be appended with other text in a span.
   */
  public LithoViewAssert hasVisibleTextContaining(String text) {
    final String regexPattern = String.format(".*%s.*", text);
    return hasVisibleTextMatching(regexPattern);
  }

  /** Inverse of {@link #hasVisibleTextMatching(String)}. */
  public LithoViewAssert doesNotHaveVisibleTextMatching(String pattern) {
    assertThatViewTree().hasVisibleTextMatching(pattern);

    return this;
  }

  /** Inverse of {@link #hasVisibleTextContaining(String)}. */
  public LithoViewAssert doesNotHaveVisibleTextContaining(String text) {
    final String regexPattern = String.format(".*%s.*", text);
    return doesNotHaveVisibleTextMatching(regexPattern);
  }

  /** Assert that the LithoView under test has the provided measured width. */
  public LithoViewAssert hasMeasuredWidthOf(int width) {
    Assertions.assertThat(actual.getMeasuredWidth())
        .overridingErrorMessage(
            "Expected LithoView to have a width of %d, but was %d.",
            width, actual.getMeasuredWidth())
        .isEqualTo(width);

    return this;
  }

  /** Assert that the LithoView under test has the provided measured height. */
  public LithoViewAssert hasMeasuredHeightOf(int height) {
    Assertions.assertThat(actual.getMeasuredHeight())
        .overridingErrorMessage(
            "Expected LithoView to have a height of %d, but was %d.",
            height, actual.getMeasuredHeight())
        .isEqualTo(height);

    return this;
  }

  /**
   * Assert that the view tag is present for the given index.
   *
   * @param tagId Index of the view tag.
   * @param tagValue View tag value.
   */
  public LithoViewAssert hasViewTag(int tagId, Object tagValue) {
    assertThatViewTree().hasViewTag(tagId, tagValue);

    return this;
  }

  /**
   * Assert that the LithoView under test has the provided Component class once in the Component
   * Tree hierarchy.
   */
  public LithoViewAssert containsComponent(Class<? extends Component> clazz) {
    return containsComponent(1, clazz);
  }

  /**
   * Assert that the LithoView under test has the provided Component class in the Component Tree
   * hierarchy given number of times
   */
  public LithoViewAssert containsComponent(int times, Class<? extends Component> clazz) {
    List<? extends Component> componentsList = findAllComponentsInLithoView(actual, clazz);
    new ListAssert(componentsList).haveExactly(times, typeIs(clazz));
    return this;
  }

  /**
   * Assert that the LithoView under test has the provided Component class once in the Component
   * Tree hierarchy.
   */
  public LithoViewAssert containsComponent(KClass<? extends Component> clazz) {
    return containsComponent(1, clazz);
  }

  /**
   * Assert that the LithoView under test has the provided Component class in the Component Tree
   * hierarchy given number of times
   */
  public LithoViewAssert containsComponent(int times, KClass<? extends Component> clazz) {
    List<Component> componentsList = findAllComponentsInLithoView(actual, clazz);
    new ListAssert(componentsList).haveExactly(times, typeIs(clazz));
    return this;
  }

  /**
   * Assert that the LithoView under test has the provided Component classes in the Component Tree
   * hierarchy
   */
  public LithoViewAssert containsComponents(Class<? extends Component>... clazz) {
    List<Component> componentList = findAllComponentsInLithoView(actual, clazz);
    for (Class<? extends Component> componentClass : clazz) {
      new ListAssert<Component>(componentList).haveAtLeastOne(typeIs(componentClass));
    }
    return this;
  }

  /**
   * Assert that the LithoView under test has the provided Component classes in the Component Tree
   * hierarchy
   */
  public LithoViewAssert containsComponents(KClass<? extends Component>... clazz) {
    List<Component> componentList = findAllComponentsInLithoView(actual, clazz);
    for (KClass<? extends Component> componentClass : clazz) {
      new ListAssert<Component>(componentList).haveAtLeastOne(typeIs(componentClass));
    }
    return this;
  }

  /**
   * Assert that the LithoView under test does not contain the provided Component classes in the
   * Component Tree hierarchy
   */
  public LithoViewAssert doesNotContainComponents(Class<? extends Component>... clazz) {
    List<Component> componentList = findAllComponentsInLithoView(actual, clazz);
    Assertions.assertThat(componentList).isEmpty();
    return this;
  }

  /**
   * Assert that the LithoView under test does not contain the provided Component classes in the
   * Component Tree hierarchy
   */
  public LithoViewAssert doesNotContainComponents(KClass<? extends Component>... clazz) {
    List<Component> componentList = findAllComponentsInLithoView(actual, clazz);
    Assertions.assertThat(componentList).isEmpty();
    return this;
  }

  public static OccurrenceCount times(int i) {
    return new OccurrenceCount(i, i + " times");
  }

  public static OccurrenceCount once() {
    return new OccurrenceCount(1, "once");
  }

  public static class OccurrenceCount {
    final int times;
    final String shortName;

    OccurrenceCount(int times, String shortName) {
      this.times = times;
      this.shortName = shortName;
    }

    @Override
    public String toString() {
      return shortName;
    }
  }
}
