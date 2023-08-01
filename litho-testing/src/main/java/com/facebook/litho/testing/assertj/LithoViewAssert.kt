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

package com.facebook.litho.testing.assertj

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.facebook.litho.Component
import com.facebook.litho.LithoView
import com.facebook.litho.LithoViewTestHelper
import com.facebook.litho.componentsfinder.findAllComponentsInLithoView
import com.facebook.litho.componentsfinder.findAllDirectComponentsInLithoView
import com.facebook.litho.componentsfinder.findDirectComponentInLithoView
import com.facebook.litho.testing.subcomponents.InspectableComponent
import com.facebook.litho.testing.viewtree.ViewTree
import com.facebook.litho.testing.viewtree.ViewTreeAssert
import java.util.Locale
import kotlin.Pair
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Condition
import org.assertj.core.api.SoftAssertions
import org.hamcrest.Matcher

/**
 * Assertion methods for [LithoView]s.
 *
 * To create an instance of this class, invoke ` [LithoViewAssert.assertThat]`.
 *
 * Alternatively, use [LegacyLithoAssertions] which provides entry points to all Litho AssertJ
 * helpers.
 */
class LithoViewAssert internal constructor(actual: LithoView) :
    AbstractAssert<LithoViewAssert, LithoView>(actual, LithoViewAssert::class.java) {

  @JvmOverloads
  fun containsTestKey(testKey: String?, count: OccurrenceCount = once()): LithoViewAssert {
    val testItems = LithoViewTestHelper.findTestItems(actual, testKey)
    assertThat(testItems)
        .hasSize(count.times)
        .overridingErrorMessage(
            "Expected to find test key <%s> in LithoView <%s> %s, but %s.",
            testKey,
            actual,
            count,
            if (testItems.isEmpty()) "couldn't find it"
            else String.format(Locale.ROOT, "saw it %d times instead", testItems.size))
        .isNotNull()
    return this
  }

  fun doesNotContainTestKey(testKey: String?): LithoViewAssert {
    val testItem = LithoViewTestHelper.findTestItem(actual, testKey)
    val bounds = testItem?.bounds
    assertThat(testItem)
        .overridingErrorMessage(
            "Expected not to find test key <%s> in LithoView <%s>, but it was present at bounds %s.",
            testKey,
            actual,
            bounds)
        .isNull()
    return this
  }

  private fun assertThatViewTree(): ViewTreeAssert = ViewTreeAssert.assertThat(ViewTree.of(actual))

  /** Assert that any view in the given Component has the provided content description. */
  fun hasContentDescription(contentDescription: String?): LithoViewAssert {
    assertThatViewTree().hasContentDescription(contentDescription)
    return this
  }

  /** Assert that any view in the given Component do not have the provided content description. */
  fun hasNoContentDescription(contentDescription: String?): LithoViewAssert {
    assertThatViewTree().hasNoContentDescription(contentDescription)
    return this
  }

  /** Assert that any view in the given Component has the provided content description. */
  fun hasContentDescription(@StringRes resourceId: Int): LithoViewAssert {
    assertThatViewTree().hasContentDescription(resourceId)
    return this
  }

  /**
   * Assert that the given component contains the drawable identified by the provided drawable
   * resource id.
   */
  fun hasVisibleDrawable(@DrawableRes drawableRes: Int): LithoViewAssert {
    assertThatViewTree().hasVisibleDrawable(drawableRes)
    return this
  }

  /** Assert that the given component contains the drawable provided. */
  fun hasVisibleDrawable(drawable: Drawable): LithoViewAssert {
    assertThatViewTree().hasVisibleDrawable(drawable)
    return this
  }

  /** Inverse of [.hasVisibleDrawable] */
  fun doesNotHaveVisibleDrawable(drawable: Drawable): LithoViewAssert {
    assertThatViewTree().doesNotHaveVisibleDrawable(drawable)
    return this
  }

  /** Inverse of [.hasVisibleDrawable] */
  fun doesNotHaveVisibleDrawable(@DrawableRes drawableRes: Int): LithoViewAssert {
    assertThatViewTree().doesNotHaveVisibleDrawable(drawableRes)
    return this
  }

  /** Assert that the given component has the exact text provided. */
  fun hasVisibleText(text: String): LithoViewAssert {
    assertThatViewTree().hasVisibleText(text)
    return this
  }

  /** Assert that the given component has the exact text identified by resource id. */
  fun hasVisibleText(@StringRes resourceId: Int): LithoViewAssert {
    assertThatViewTree().hasVisibleText(resourceId)
    return this
  }

  /** Inverse of [.hasVisibleText] */
  fun doesNotHaveVisibleText(text: String): LithoViewAssert {
    assertThatViewTree().doesNotHaveVisibleText(text)
    return this
  }

  /** Inverse of [.hasVisibleText] */
  fun doesNotHaveVisibleText(@StringRes resourceId: Int): LithoViewAssert {
    assertThatViewTree().doesNotHaveVisibleText(resourceId)
    return this
  }

  /** Assert that the given component contains the provided pattern. */
  fun hasVisibleTextMatching(pattern: String): LithoViewAssert {
    assertThatViewTree().hasVisibleTextMatching(pattern)
    return this
  }

  /**
   * Assert that the given component contains the provided text. Useful if checking portion of text
   * that may be appended with other text in a span.
   */
  fun hasVisibleTextContaining(text: String): LithoViewAssert {
    val regexPattern = String.format(".*%s.*", text)
    return hasVisibleTextMatching(regexPattern)
  }

  /** Inverse of [.hasVisibleTextMatching]. */
  fun doesNotHaveVisibleTextMatching(pattern: String): LithoViewAssert {
    assertThatViewTree().doesNotHaveVisibleTextMatching(pattern)
    return this
  }

  /** Inverse of [.hasVisibleTextContaining]. */
  fun doesNotHaveVisibleTextContaining(text: String): LithoViewAssert {
    val regexPattern = String.format(".*%s.*", text)
    return doesNotHaveVisibleTextMatching(regexPattern)
  }

  /** Assert that the LithoView under test has the provided measured width. */
  fun hasMeasuredWidthOf(width: Int): LithoViewAssert {
    assertThat(actual.measuredWidth)
        .overridingErrorMessage(
            "Expected LithoView to have a width of %d, but was %d.", width, actual.measuredWidth)
        .isEqualTo(width)
    return this
  }

  /** Assert that the LithoView under test has the provided measured height. */
  fun hasMeasuredHeightOf(height: Int): LithoViewAssert {
    assertThat(actual.measuredHeight)
        .overridingErrorMessage(
            "Expected LithoView to have a height of %d, but was %d.", height, actual.measuredHeight)
        .isEqualTo(height)
    return this
  }

  /**
   * Assert that the view tag is present for the given index.
   *
   * @param tagId Index of the view tag.
   * @param tagValue View tag value.
   */
  fun hasViewTag(tagId: Int, tagValue: Any): LithoViewAssert {
    assertThatViewTree().hasViewTag(tagId, tagValue)
    return this
  }

  /** Assert that the LithoView has a direct component of type clazz */
  fun containsDirectComponents(vararg kClazzes: KClass<out Component>): LithoViewAssert =
      containsDirectComponents(*getJavaClasses(*kClazzes))

  /** Assert that the LithoView has a direct component of type clazz */
  fun containsDirectComponents(vararg clazzes: Class<out Component>): LithoViewAssert {
    val softAssertions = SoftAssertions()
    for (clazz in clazzes) {
      val foundDirectComponent = findDirectComponentInLithoView(actual, clazz)
      softAssertions
          .assertThat(foundDirectComponent)
          .overridingErrorMessage(
              "Expected to have direct component of type %s in LithoView, but did not find one",
              *clazzes)
          .isNotNull()
    }
    softAssertions.assertAll()
    return this
  }

  /** Assert that the LithoView does not have a direct component of type clazz */
  fun doesNotContainDirectComponents(vararg kClazzes: KClass<out Component>): LithoViewAssert =
      doesNotContainDirectComponents(*getJavaClasses(*kClazzes))

  /** Assert that the LithoView does not have a direct component of type clazz */
  fun doesNotContainDirectComponents(vararg clazzes: Class<out Component>): LithoViewAssert {
    val softAssertions = SoftAssertions()
    for (clazz in clazzes) {
      val foundDirectComponent = findDirectComponentInLithoView(actual, clazz)
      softAssertions
          .assertThat(foundDirectComponent)
          .overridingErrorMessage(
              "Expected to not have direct component of type %s in LithoView, but did not find one",
              clazz)
          .isNull()
    }
    softAssertions.assertAll()
    return this
  }

  /**
   * Assert that the LithoView under test has the provided Component class once in the Component
   * Tree hierarchy.
   */
  fun containsExactlyOne(clazz: Class<out Component>): LithoViewAssert = containsExactly(1, clazz)

  /**
   * Assert that the LithoView under test has the provided Component class in the Component Tree
   * hierarchy given number of times
   */
  fun containsExactly(times: Int, clazz: Class<out Component>): LithoViewAssert {
    val componentsList: List<Component> = findAllComponentsInLithoView(actual, clazz)
    assertThat(componentsList).haveExactly(times, ComponentConditions.typeIs(clazz))
    return this
  }

  /**
   * Assert that the LithoView under test has the provided Component class once in the Component
   * Tree hierarchy.
   */
  fun containsExactlyOne(clazz: KClass<out Component>): LithoViewAssert = containsExactly(1, clazz)

  /**
   * Assert that the LithoView under test has the provided Component class in the Component Tree
   * hierarchy given number of times
   */
  fun containsExactly(times: Int, clazz: KClass<out Component>): LithoViewAssert {
    containsExactly(times, clazz.java)
    return this
  }

  /**
   * Assert that the LithoView under test has the provided Component classes in the Component Tree
   * hierarchy
   */
  fun containsComponents(vararg clazz: Class<out Component>): LithoViewAssert {
    val softAssertions = SoftAssertions()
    val componentList: List<Component> = findAllComponentsInLithoView(actual, *clazz)
    for (componentClass in clazz) {
      softAssertions
          .assertThat(componentList)
          .overridingErrorMessage(
              "Expected to have component of type %s in LithoView, but did not find one",
              componentClass)
          .haveAtLeastOne(ComponentConditions.typeIs(componentClass))
    }
    softAssertions.assertAll()
    return this
  }

  /**
   * Assert that the LithoView under test has the provided Component classes in the Component Tree
   * hierarchy
   */
  fun containsComponents(vararg clazz: KClass<out Component>): LithoViewAssert =
      containsComponents(*getJavaClasses(*clazz))

  /**
   * Assert that the LithoView under test does not contain the provided Component classes in the
   * Component Tree hierarchy
   */
  fun doesNotContainComponents(vararg clazz: Class<out Component>): LithoViewAssert {
    val componentList: List<Component> = findAllComponentsInLithoView(actual, *clazz)
    assertThat(componentList).isEmpty()
    return this
  }

  /**
   * Assert that the LithoView under test does not contain the provided Component classes in the
   * Component Tree hierarchy
   */
  fun doesNotContainComponents(vararg clazz: KClass<out Component>): LithoViewAssert {
    val componentList: List<Component> = findAllComponentsInLithoView(actual, *clazz)
    assertThat(componentList).isEmpty()
    return this
  }

  /**
   * Assert that the LithoView will render content, the root component won't return null nor a child
   * with height and width equal to 0
   */
  fun willRenderContent(): LithoViewAssert {
    assertThat(actual.mountItemCount > 0 || actual.childCount > 0)
        .overridingErrorMessage(
            "Expected content to be visible, but current LithoView childCount = 0 and we did not mount any content")
    return this
  }

  /**
   * Assert that the LithoView will not render content, the root component will either return null
   * or a child with width and height equal to 0
   */
  fun willNotRenderContent(): LithoViewAssert {
    assertThat(actual.mountItemCount == 0 && actual.childCount == 0)
        .overridingErrorMessage(
            "Expected no content in the current LithoView, but found child count = %d and mounted item count = %d with LithoView hierarchy:\n %s",
            actual.childCount,
            actual.mountItemCount,
            actual.toString())
        .isTrue()
    return this
  }

  /**
   * Asserts that the LithoView will render Component as a direct children of the root satisfying
   * the given condition.
   *
   * example:
   * ```
   * FBStory -> Story -> Column
   * -- StoryDescription -> Column
   * ---- Text
   * -- Text
   * -- Comments -> Column
   * ---- Text
   * ---- Text
   *
   * ```
   *
   * Each row here is a single Node and the arrow indicates 'returns from render'. Direct children
   * for FBStory Component is Story only and direct components for StoryDescription and Comments
   * Components are Columns
   */
  fun hasDirectMatchingComponent(condition: Condition<InspectableComponent>): LithoViewAssert {
    val inspectableComponent = InspectableComponent.getRootInstance(actual)
    var conditionMet = false
    for (component in inspectableComponent!!.childComponents) {
      if (condition.matches(component)) {
        conditionMet = true
        break
      }
    }
    assertThat(conditionMet)
        .overridingErrorMessage(
            "Expected LithoView <%s> to satisfy condition <%s>", actual, condition)
        .isTrue()
    return this
  }

  /**
   * Asserts that the LithoView contains a Component satisfying the given condition at any level of
   * the hierarchy
   */
  fun hasAnyMatchingComponent(condition: Condition<InspectableComponent>): LithoViewAssert {
    val inspectableComponent = InspectableComponent.getRootInstance(actual)
    val conditionMet = iterateOverAllChildren(listOf(inspectableComponent!!), condition)
    assertThat(conditionMet)
        .overridingErrorMessage(
            "Expected LithoView <%s> to satisfy condition <%s>", actual, condition)
        .isTrue()
    return this
  }

  private fun iterateOverAllChildren(
      inspectableComponents: List<InspectableComponent>,
      condition: Condition<InspectableComponent>
  ): Boolean =
      inspectableComponents.any { component: InspectableComponent ->
        condition.matches(component) || iterateOverAllChildren(component.childComponents, condition)
      }

  /**
   * Asserts that the LithoView contains a Component with given props at any level of the hierarchy.
   * This function uses DFS algorithm to go through the whole component tree
   *
   * @param kClass class of a component
   * @param propsValuePairs Pairs of props and their expected values
   */
  fun <T1, T2> hasAnyMatchingComponent(
      kClass: KClass<out Component>,
      vararg propsValuePairs: Pair<KProperty1<T2, T1>, T1>
  ): LithoViewAssert {
    val componentsList: List<Component> = findAllComponentsInLithoView(actual, kClass)
    val hasMatchingProps = hasMatchingProps(componentsList, propsValuePairs)
    assertThat(hasMatchingProps)
        .overridingErrorMessage(
            "\nExpected LithoView : \n <%s> \n to contains component with given props, but the components that were found of given class: \n <%s> \ndid not satisfy all those props: \n %s ",
            actual,
            kClass.toString(),
            getPropsFormattedString(propsValuePairs))
        .isTrue()
    return this
  }

  /**
   * Asserts that the LithoView does not contain a Component with given props at any level of the
   * hierarchy. This function uses DFS algorithm to go through the whole component tree
   *
   * @param kClass class of a component
   * @param propsValuePairs Pairs of props and their expected values
   */
  fun <T1, T2> doesNotHaveMatchingComponent(
      kClass: KClass<out Component>,
      vararg propsValuePairs: Pair<KProperty1<T2, T1>, T1>
  ): LithoViewAssert {
    val componentsList: List<Component> = findAllComponentsInLithoView(actual, kClass)
    val hasMatchingProps = hasMatchingProps(componentsList, propsValuePairs)
    assertThat(hasMatchingProps)
        .overridingErrorMessage(
            "\nExpected LithoView : \n <%s> \n to not contain component with given props, but the components that were found of given class: \n <%s> \ndid satisfy all those props: \n %s ",
            actual,
            kClass.toString(),
            getPropsFormattedString(propsValuePairs))
        .isFalse()
    return this
  }

  /**
   * Asserts that the LithoView contains a direct Component with given props
   *
   * @param kClass class of a component
   * @param propsValuePairs Pairs of props and their expected values
   */
  fun <T1, T2> hasDirectMatchingComponent(
      kClass: KClass<out Component>,
      vararg propsValuePairs: Pair<KProperty1<T2, T1>, T1>
  ): LithoViewAssert {
    val componentsList: List<Component> = findAllDirectComponentsInLithoView(actual, kClass)
    val hasMatchingProps = hasMatchingProps(componentsList, propsValuePairs)
    assertThat(hasMatchingProps)
        .overridingErrorMessage(
            "\nExpected LithoView : \n <%s> \n to contains direct component with props that matches given matcher, but the direct components that were found of given class: \n <%s>  did not satisfy all those props",
            actual,
            kClass.toString())
        .isTrue()
    return this
  }

  private fun <T1, T2> hasMatchingProps(
      componentsList: List<Component>,
      propsValuePairs: Array<out Pair<KProperty1<T2, T1>, T1>>
  ): Boolean =
      componentsList.any { component: Component ->
        comparedPropsAreEqual(component, propsValuePairs)
      }

  /**
   * Asserts that the LithoView contains a Component with props that matches given matcher at any
   * level of the hierarchy
   *
   * @param kClass class of a component
   * @param propsMatcherPairs Pairs of props and their matchers
   */
  fun <T2, T1> hasAnyMatchingComponentWithMatcher(
      kClass: KClass<out Component>,
      vararg propsMatcherPairs: Pair<KProperty1<T2, T1>, Matcher<T1>>
  ): LithoViewAssert {
    val componentsList: List<Component> = findAllComponentsInLithoView(actual, kClass)
    val isMatching = hasMatchingPropsWithMatcher(componentsList, propsMatcherPairs)
    assertThat(isMatching)
        .overridingErrorMessage(
            "\nExpected LithoView : \n <%s> \n to contains component with props that matches given matcher, but the components that were found of given class: \n <%s> \ndid not satisfy those matchers: \n %s ",
            actual,
            kClass.toString(),
            getPropsMatcherFormattedString(propsMatcherPairs))
        .isTrue()
    return this
  }

  /**
   * Asserts that the LithoView contains a Component with props that matches given matcher at any
   * level of the hierarchy
   *
   * @param kClass class of a component
   * @param propsMatcherPairs Pairs of props and their matchers
   */
  fun <T2, T1> hasDirectMatchingComponentWithMatcher(
      kClass: KClass<out Component>,
      vararg propsMatcherPairs: Pair<KProperty1<T2, T1>, Matcher<T1>>
  ): LithoViewAssert {
    val componentsList: List<Component> = findAllDirectComponentsInLithoView(actual, kClass)
    val isMatching = hasMatchingPropsWithMatcher(componentsList, propsMatcherPairs)
    assertThat(isMatching)
        .overridingErrorMessage(
            "\nExpected LithoView : \n <%s> \n to contains a direct component with props that matches given matcher, but the direct components that were found of given class: \n <%s> \ndid not satisfy those matchers: \n %s ",
            actual,
            kClass.toString(),
            getPropsMatcherFormattedString(propsMatcherPairs))
        .isTrue()
    return this
  }

  /**
   * Helper method that checks if the componentsList contains a component that matches given matcher
   */
  private fun <T2, T1> hasMatchingPropsWithMatcher(
      componentsList: List<Component>,
      propsMatcherPairs: Array<out Pair<KProperty1<T2, T1>, Matcher<T1>>>
  ): Boolean =
      componentsList.any { component: Component ->
        comparedPropsMatch(component, propsMatcherPairs)
      }

  private fun <T2, T1> getPropsFormattedString(
      propsValuePairs: Array<out Pair<KProperty1<T2, T1>, T1>>
  ): String {
    val sb = StringBuilder()
    for ((first, second) in propsValuePairs) {
      appendPairString(first.name, second.toString(), sb)
    }
    return sb.toString()
  }

  private fun <T2, T1> getPropsMatcherFormattedString(
      propsMatcherPairs: Array<out Pair<KProperty1<T2, T1>, Matcher<T1>>>
  ): String {
    val sb = StringBuilder()
    for ((first, second) in propsMatcherPairs) {
      appendPairString(first.name, second.toString(), sb)
    }
    return sb.toString()
  }

  private fun appendPairString(valueName: String, value: String, sb: StringBuilder) {
    sb.append(valueName)
    sb.append(" : ")
    sb.append(value)
    sb.append("\n")
  }

  private fun <T2, T1> comparedPropsMatch(
      component: Component,
      propsMatcherPairs: Array<out Pair<KProperty1<T2, T1>, Matcher<T1>>>
  ): Boolean {
    return propsMatcherPairs.none { (first, second) -> !second.matches(first.get(component as T2)) }
  }

  private fun <T2, T1> comparedPropsAreEqual(
      component: Component,
      propsValuePairs: Array<out Pair<KProperty1<T2, T1>, T1>>
  ): Boolean =
      propsValuePairs.all { (first, second): Pair<KProperty1<T2, T1>, T1> ->
        first.get(component as T2) == second
      }

  class OccurrenceCount internal constructor(val times: Int, val shortName: String) {
    override fun toString(): String = shortName
  }

  companion object {
    @JvmStatic fun assertThat(actual: LithoView): LithoViewAssert = LithoViewAssert(actual)

    @JvmStatic operator fun times(i: Int): OccurrenceCount = OccurrenceCount(i, "$i times")

    @JvmStatic fun once(): OccurrenceCount = OccurrenceCount(1, "once")

    private fun getJavaClasses(
        vararg kClazzes: KClass<out Component>
    ): Array<Class<out Component>> = kClazzes.map { kClass -> kClass.java }.toTypedArray()
  }
}
