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

package com.facebook.litho.testing.assertj;

import static com.facebook.litho.componentsfinder.ComponentsFinderKt.findAllComponentsInLithoView;
import static com.facebook.litho.componentsfinder.ComponentsFinderKt.findDirectComponentInLithoView;
import static com.facebook.litho.testing.assertj.ComponentConditions.typeIs;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import com.facebook.litho.Component;
import com.facebook.litho.LithoView;
import com.facebook.litho.LithoViewTestHelper;
import com.facebook.litho.TestItem;
import com.facebook.litho.componentsfinder.ComponentsFinderKt;
import com.facebook.litho.testing.subcomponents.InspectableComponent;
import com.facebook.litho.testing.viewtree.ViewTree;
import com.facebook.litho.testing.viewtree.ViewTreeAssert;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import kotlin.Pair;
import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;
import kotlin.reflect.KProperty1;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.SoftAssertions;
import org.hamcrest.Matcher;

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

  /** Assert that any view in the given Component do not have the provided content description. */
  public LithoViewAssert hasNoContentDescription(String contentDescription) {
    assertThatViewTree().hasNoContentDescription(contentDescription);

    return this;
  }

  /** Assert that any view in the given Component has the provided content description. */
  public LithoViewAssert hasContentDescription(@StringRes int resourceId) {
    assertThatViewTree().hasContentDescription(resourceId);

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
    assertThatViewTree().doesNotHaveVisibleTextMatching(pattern);

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

  /** Assert that the LithoView has a direct component of type clazz */
  public LithoViewAssert containsDirectComponents(final KClass<? extends Component>... kClazzes) {
    return containsDirectComponents(getJavaClasses(kClazzes));
  }

  /** Assert that the LithoView has a direct component of type clazz */
  public LithoViewAssert containsDirectComponents(final Class<? extends Component>... clazzes) {
    SoftAssertions softAssertions = new SoftAssertions();
    for (final Class<? extends Component> clazz : clazzes) {
      final @Nullable Component foundDirectComponent =
          findDirectComponentInLithoView(actual, clazz);

      softAssertions
          .assertThat(foundDirectComponent)
          .overridingErrorMessage(
              "Expected to have direct component of type %s in LithoView, but did not find one",
              clazzes)
          .isNotNull();
    }
    softAssertions.assertAll();
    return this;
  }

  /** Assert that the LithoView does not have a direct component of type clazz */
  public LithoViewAssert doesNotContainDirectComponents(
      final KClass<? extends Component>... kClazzes) {
    return doesNotContainDirectComponents(getJavaClasses(kClazzes));
  }

  /** Assert that the LithoView does not have a direct component of type clazz */
  public LithoViewAssert doesNotContainDirectComponents(
      final Class<? extends Component>... clazzes) {
    SoftAssertions softAssertions = new SoftAssertions();
    for (final Class<? extends Component> clazz : clazzes) {
      final @Nullable Component foundDirectComponent =
          findDirectComponentInLithoView(actual, clazz);

      softAssertions
          .assertThat(foundDirectComponent)
          .overridingErrorMessage(
              "Expected to not have direct component of type %s in LithoView, but did not find one",
              clazz)
          .isNull();
    }
    softAssertions.assertAll();
    return this;
  }

  /**
   * Assert that the LithoView under test has the provided Component class once in the Component
   * Tree hierarchy.
   */
  public LithoViewAssert containsExactlyOne(Class<? extends Component> clazz) {
    return containsExactly(1, clazz);
  }

  /**
   * Assert that the LithoView under test has the provided Component class in the Component Tree
   * hierarchy given number of times
   */
  public LithoViewAssert containsExactly(int times, Class<? extends Component> clazz) {
    List<? extends Component> componentsList = findAllComponentsInLithoView(actual, clazz);
    new ListAssert(componentsList).haveExactly(times, typeIs(clazz));
    return this;
  }

  /**
   * Assert that the LithoView under test has the provided Component class once in the Component
   * Tree hierarchy.
   */
  public LithoViewAssert containsExactlyOne(KClass<? extends Component> clazz) {
    return containsExactly(1, clazz);
  }

  /**
   * Assert that the LithoView under test has the provided Component class in the Component Tree
   * hierarchy given number of times
   */
  public LithoViewAssert containsExactly(int times, KClass<? extends Component> clazz) {
    containsExactly(times, JvmClassMappingKt.getJavaClass(clazz));
    return this;
  }

  /**
   * Assert that the LithoView under test has the provided Component classes in the Component Tree
   * hierarchy
   */
  public LithoViewAssert containsComponents(Class<? extends Component>... clazz) {
    final SoftAssertions softAssertions = new SoftAssertions();

    List<Component> componentList = findAllComponentsInLithoView(actual, clazz);
    for (Class<? extends Component> componentClass : clazz) {
      softAssertions.assertThat(componentList).haveAtLeastOne(typeIs(componentClass));
    }
    softAssertions.assertAll();
    return this;
  }

  /**
   * Assert that the LithoView under test has the provided Component classes in the Component Tree
   * hierarchy
   */
  public LithoViewAssert containsComponents(KClass<? extends Component>... clazz) {
    return containsComponents(getJavaClasses(clazz));
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

  /**
   * Assert that the LithoView will render content, the root component won't return null nor a child
   * with height and width equal to 0
   */
  public LithoViewAssert willRenderContent() {
    Assertions.assertThat(actual.getMountItemCount() > 0 || actual.getChildCount() > 0)
        .overridingErrorMessage(
            "Expected content to be visible, but current LithoView childCount = 0 and we did not mount any content");
    return this;
  }

  /**
   * Assert that the LithoView will not render content, the root component will either return null
   * or a child with width and height equal to 0
   */
  public LithoViewAssert willNotRenderContent() {
    Assertions.assertThat(actual.getMountItemCount() == 0 && actual.getChildCount() == 0)
        .overridingErrorMessage(
            "Expected no content in the current LithoView, but found child count = %d and mounted item count = %d with LithoView hierarchy:\n %s",
            actual.getChildCount(), actual.getMountItemCount(), actual.toString())
        .isTrue();
    return this;
  }

  /**
   * Asserts that the LithoView will render Component as a direct children of the root satisfying
   * the given condition.
   *
   * <p>example:
   *
   * <pre>
   * FBStory -> Story -> Column
   * -- StoryDescription -> Column
   * ---- Text
   * -- Text
   * -- Comments -> Column
   * ---- Text
   * ---- Text
   * </pre>
   *
   * Each row here is a single Node and the arrow indicates 'returns from render'. Direct children
   * for FBStory Component is Story only and direct components for StoryDescription and Comments
   * Components are Columns
   */
  public LithoViewAssert hasDirectMatchingComponent(Condition<InspectableComponent> condition) {
    InspectableComponent inspectableComponent = InspectableComponent.getRootInstance(actual);
    boolean conditionMet = false;
    for (InspectableComponent component : inspectableComponent.getChildComponents()) {
      if (condition.matches(component)) {
        conditionMet = true;
        break;
      }
    }
    Assertions.assertThat(conditionMet)
        .overridingErrorMessage(
            "Expected LithoView <%s> to satisfy condition <%s>", actual, condition)
        .isTrue();
    return this;
  }

  /**
   * Asserts that the LithoView contains a Component satisfying the given condition at any level of
   * the hierarchy
   */
  public LithoViewAssert hasAnyMatchingComponent(Condition<InspectableComponent> condition) {
    InspectableComponent inspectableComponent = InspectableComponent.getRootInstance(actual);
    boolean conditionMet =
        iterateOverAllChildren(Collections.singletonList(inspectableComponent), condition);
    Assertions.assertThat(conditionMet)
        .overridingErrorMessage(
            "Expected LithoView <%s> to satisfy condition <%s>", actual, condition)
        .isTrue();
    return this;
  }

  private boolean iterateOverAllChildren(
      List<InspectableComponent> inspectableComponents, Condition<InspectableComponent> condition) {
    return inspectableComponents.stream()
        .anyMatch(
            component ->
                condition.matches(component)
                    || iterateOverAllChildren(component.getChildComponents(), condition));
  }

  /**
   * Asserts that the LithoView contains a Component with given props at any level of the hierarchy.
   * This function uses DFS algorithm to go through the whole component tree
   *
   * @param kClass class of a component
   * @param propsValuePairs Pairs of props and their expected values
   */
  public <T1, T2> LithoViewAssert hasAnyMatchingComponent(
      KClass kClass, Pair<KProperty1<T2, T1>, T1>... propsValuePairs) {

    List<Component> componentsList =
        ComponentsFinderKt.findAllComponentsInLithoView(actual, kClass);
    boolean hasMatchingProps = hasMatchingProps(componentsList, propsValuePairs);
    Assertions.assertThat(hasMatchingProps)
        .overridingErrorMessage(
            "\nExpected LithoView : \n <%s> \n to contains component with given props, but the components that were found of given class: \n <%s> \ndid not satisfy all those props: \n %s ",
            actual, kClass.toString(), getPropsFormattedString(propsValuePairs))
        .isTrue();

    return this;
  }

  /**
   * Asserts that the LithoView does not contain a Component with given props at any level of the
   * hierarchy. This function uses DFS algorithm to go through the whole component tree
   *
   * @param kClass class of a component
   * @param propsValuePairs Pairs of props and their expected values
   */
  public <T1, T2> LithoViewAssert doesNotHaveMatchingComponent(
      KClass kClass, Pair<KProperty1<T2, T1>, T1>... propsValuePairs) {

    List<Component> componentsList =
        ComponentsFinderKt.findAllComponentsInLithoView(actual, kClass);
    boolean hasMatchingProps = hasMatchingProps(componentsList, propsValuePairs);
    Assertions.assertThat(hasMatchingProps)
        .overridingErrorMessage(
            "\nExpected LithoView : \n <%s> \n to not contain component with given props, but the components that were found of given class: \n <%s> \ndid satisfy all those props: \n %s ",
            actual, kClass.toString(), getPropsFormattedString(propsValuePairs))
        .isFalse();

    return this;
  }

  /**
   * Asserts that the LithoView contains a direct Component with given props
   *
   * @param kClass class of a component
   * @param propsValuePairs Pairs of props and their expected values
   */
  public <T1, T2> LithoViewAssert hasDirectMatchingComponent(
      KClass kClass, Pair<KProperty1<T2, T1>, T1>... propsValuePairs) {

    final List<Component> componentsList =
        ComponentsFinderKt.findAllDirectComponentsInLithoView(actual, kClass);
    boolean hasMatchingProps = hasMatchingProps(componentsList, propsValuePairs);
    Assertions.assertThat(hasMatchingProps)
        .overridingErrorMessage(
            "\nExpected LithoView : \n <%s> \n to contains direct component with props that matches given matcher, but the direct components that were found of given class: \n <%s>  did not satisfy all those props",
            actual, kClass.toString())
        .isTrue();

    return this;
  }

  private <T1, T2> boolean hasMatchingProps(
      List<Component> componentsList, Pair<KProperty1<T2, T1>, T1>[] propsValuePairs) {
    return componentsList.stream()
        .anyMatch(component -> comparedPropsAreEqual(component, propsValuePairs));
  }

  /**
   * Asserts that the LithoView contains a Component with props that matches given matcher at any
   * level of the hierarchy
   *
   * @param kClass class of a component
   * @param propsMatcherPairs Pairs of props and their matchers
   */
  public <T2, T1> LithoViewAssert hasAnyMatchingComponentWithMatcher(
      KClass kClass, Pair<KProperty1<T2, T1>, Matcher<T1>>... propsMatcherPairs) {

    List<Component> componentsList =
        ComponentsFinderKt.findAllComponentsInLithoView(actual, kClass);
    boolean isMatching = hasMatchingPropsWithMatcher(componentsList, propsMatcherPairs);

    Assertions.assertThat(isMatching)
        .overridingErrorMessage(
            "\nExpected LithoView : \n <%s> \n to contains component with props that matches given matcher, but the components that were found of given class: \n <%s> \ndid not satisfy those matchers: \n %s ",
            actual, kClass.toString(), getPropsMatcherFormattedString(propsMatcherPairs))
        .isTrue();
    return this;
  }

  /**
   * Asserts that the LithoView contains a Component with props that matches given matcher at any
   * level of the hierarchy
   *
   * @param kClass class of a component
   * @param propsMatcherPairs Pairs of props and their matchers
   */
  public <T2, T1> LithoViewAssert hasDirectMatchingComponentWithMatcher(
      KClass kClass, Pair<KProperty1<T2, T1>, Matcher<T1>>... propsMatcherPairs) {

    List<Component> componentsList =
        ComponentsFinderKt.findAllDirectComponentsInLithoView(actual, kClass);
    boolean isMatching = hasMatchingPropsWithMatcher(componentsList, propsMatcherPairs);

    Assertions.assertThat(isMatching)
        .overridingErrorMessage(
            "\nExpected LithoView : \n <%s> \n to contains a direct component with props that matches given matcher, but the direct components that were found of given class: \n <%s> \ndid not satisfy those matchers: \n %s ",
            actual, kClass.toString(), getPropsMatcherFormattedString(propsMatcherPairs))
        .isTrue();
    return this;
  }

  /**
   * Helper method that checks if the componentsList contains a component that matches given matcher
   */
  private <T2, T1> boolean hasMatchingPropsWithMatcher(
      List<Component> componentsList, Pair<KProperty1<T2, T1>, Matcher<T1>>[] propsMatcherPairs) {
    return componentsList.stream()
        .anyMatch(component -> comparedPropsMatch(component, propsMatcherPairs));
  }

  private <T2, T1> String getPropsFormattedString(Pair<KProperty1<T2, T1>, T1>[] propsValuePairs) {
    final StringBuilder sb = new StringBuilder();
    for (Pair<KProperty1<T2, T1>, T1> pair : propsValuePairs) {
      appendPairString(pair.getFirst().getName(), pair.getSecond().toString(), sb);
    }
    return sb.toString();
  }

  private <T2, T1> String getPropsMatcherFormattedString(
      Pair<KProperty1<T2, T1>, Matcher<T1>>[] propsMatcherPairs) {
    final StringBuilder sb = new StringBuilder();
    for (Pair<KProperty1<T2, T1>, Matcher<T1>> pair : propsMatcherPairs) {
      appendPairString(pair.getFirst().getName(), pair.getSecond().toString(), sb);
    }
    return sb.toString();
  }

  private void appendPairString(String valueName, String value, StringBuilder sb) {
    sb.append(valueName);
    sb.append(" : ");
    sb.append(value);
    sb.append("\n");
  }

  private <T2, T1> boolean comparedPropsMatch(
      Component component, Pair<KProperty1<T2, T1>, Matcher<T1>>[] propsMatcherPairs) {
    for (Pair<KProperty1<T2, T1>, Matcher<T1>> pair : propsMatcherPairs) {
      if (!(pair.getSecond().matches(pair.getFirst().get((T2) component)))) {
        return false;
      }
    }
    return true;
  }

  private <T2, T1> boolean comparedPropsAreEqual(
      Component component, Pair<KProperty1<T2, T1>, T1>[] propsValuePairs) {
    return Arrays.stream(propsValuePairs)
        .allMatch(pair -> pair.getFirst().get((T2) component).equals(pair.getSecond()));
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

  private static Class<? extends Component>[] getJavaClasses(
      final KClass<? extends Component>... kClazzes) {
    return (Class<? extends Component>[])
        Arrays.stream(kClazzes)
            .map(kClass -> JvmClassMappingKt.getJavaClass(kClass))
            .toArray(Class[]::new);
  }
}
