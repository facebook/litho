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

import android.graphics.drawable.Drawable;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.state.StateUpdatesTestHelper;
import com.facebook.litho.testing.subcomponents.InspectableComponent;
import com.facebook.litho.testing.subcomponents.SubComponent;
import java.util.List;
import kotlin.reflect.KProperty1;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.iterable.Extractor;
import org.assertj.core.util.CheckReturnValue;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;

/**
 * Assertion methods for {@link Component}s.
 *
 * <p>To create an instance of this class, invoke <code>
 * {@link ComponentAssert#assertThat(ComponentContext, Component)}</code> or <code>
 * {@link ComponentAssert#assertThat(Component.Builder)}</code>.
 *
 * @deprecated Use {@link LithoAssertions} which provides entry points to all Litho AssertJ helpers.
 */
@Deprecated
public final class ComponentAssert extends AbstractAssert<ComponentAssert, Component> {

  private final ComponentContext mComponentContext;

  public static ComponentAssert assertThat(ComponentContext componentContext, Component component) {
    return new ComponentAssert(componentContext, component);
  }

  public static ComponentAssert assertThat(Component.Builder<?> builder) {
    // mContext is freed up during build() so we need to get a reference to it before.
    final ComponentContext context = Whitebox.getInternalState(builder, "mContext");
    return new ComponentAssert(context, builder.build());
  }

  /**
   * Avoid using this method, as an alternative, try invoking the code that causes the real state
   * update in your test.
   *
   * <p>For more details see {@link #withStateUpdate(StateUpdatesTestHelper.StateUpdater)}
   */
  @Deprecated
  public LithoViewAssert afterStateUpdate() {
    return LithoViewAssert.assertThat(
        StateUpdatesTestHelper.getViewAfterStateUpdate(mComponentContext, actual));
  }

  /**
   * Avoid using this method. It lets you modify the state of a component in a different code path
   * than the one used in production
   *
   * <p>As an alternative, try invoking the code that causes the real state update in your test.
   *
   * <p>For example, if your component updates the state to hide a piece of text once clicked,
   * instead of calling that same test update, use {@link
   * com.facebook.litho.testing.InteractionUtil} to fake a click on the component, which will cause
   * the state update (and other effects that could be tested perhaps)
   *
   * <p>If you are unable to solve your problem that way, use this method as a last resort. This
   * would be similar to cases when one resorts to reflection to make a test work. Ugly, but needed
   * in rare cases.
   */
  @Deprecated
  public LithoViewAssert withStateUpdate(final StateUpdatesTestHelper.StateUpdater updater) {
    return LithoViewAssert.assertThat(
        StateUpdatesTestHelper.getViewAfterStateUpdate(mComponentContext, actual, updater));
  }

  private ComponentAssert(ComponentContext c, Component actual) {
    super(actual, ComponentAssert.class);
    mComponentContext = c;
    mComponentContext.setRenderStateContextForTests();
  }

  private LithoView mountComponent() {
    return ComponentTestHelper.mountComponent(mComponentContext, actual);
  }

  private LithoViewAssert assertThatLithoView() {
    return LithoViewAssert.assertThat(mountComponent());
  }

  /**
   * Assert that the given component has no sub-components.
   *
   * @deprecated Use {@link #extractingSubComponents} instead.
   */
  @Deprecated
  public ComponentAssert hasNoSubComponents() {
    final List<SubComponent> subComponents =
        ComponentTestHelper.getSubComponents(mComponentContext, actual);
    Assertions.assertThat(subComponents)
        .overridingErrorMessage(
            "Expected Component not to have any sub " + "components, but found %d.",
            subComponents.size())
        .isEmpty();

    return this;
  }

  /**
   * Assert that the given component contains the provided sub-component.
   *
   * @deprecated Use {@link #extractingSubComponents} instead.
   */
  @Deprecated
  public ComponentAssert containsSubComponent(SubComponent subComponent) {
    final List<SubComponent> subComponents =
        ComponentTestHelper.getSubComponents(mComponentContext, actual);
    Assertions.assertThat(subComponents)
        .overridingErrorMessage(
            "Expected to find <%s> as sub component of <%s>, " + "but couldn't find it in %s.",
            subComponent, actual, subComponents)
        .contains(subComponent);

    return this;
  }

  /**
   * Assert that the given component does <strong>not</strong> contain the provided sub-component.
   *
   * @deprecated Use {@link #extractingSubComponents} instead.
   */
  @Deprecated
  public ComponentAssert doesNotContainSubComponent(SubComponent subComponent) {
    final List<SubComponent> subComponents =
        ComponentTestHelper.getSubComponents(mComponentContext, actual);
    Assertions.assertThat(subComponents)
        .overridingErrorMessage(
            "Did not expect to find <%s> as sub component of <%s>, " + "but it was present.",
            subComponent, actual)
        .doesNotContain(subComponent);

    return this;
  }

  /**
   * Assert that any view in the given Component has the provided content description.
   *
   * @deprecated Use {@link LithoViewAssert#hasContentDescription(String)} instead.
   */
  @Deprecated
  public ComponentAssert hasContentDescription(String contentDescription) {
    assertThatLithoView().hasContentDescription(contentDescription);

    return this;
  }

  /**
   * Assert that the given component contains the drawable identified by the provided drawable
   * resource id.
   *
   * @deprecated Use {@link LithoViewAssert#hasVisibleDrawable(int)} instead.
   */
  @Deprecated
  public ComponentAssert hasVisibleDrawable(@DrawableRes int drawableRes) {
    assertThatLithoView().hasVisibleDrawable(drawableRes);

    return this;
  }

  /**
   * Assert that the given component contains the drawable provided.
   *
   * @deprecated Use {@link LithoViewAssert#hasVisibleDrawable(Drawable)} instead.
   */
  @Deprecated
  public ComponentAssert hasVisibleDrawable(Drawable drawable) {
    assertThatLithoView().hasVisibleDrawable(drawable);

    return this;
  }

  /**
   * Inverse of {@link #hasVisibleDrawable(Drawable)}
   *
   * @deprecated Use {@link LithoViewAssert#doesNotHaveVisibleDrawable(Drawable)} instead.
   */
  @Deprecated
  public ComponentAssert doesNotHaveVisibleDrawable(Drawable drawable) {
    assertThatLithoView().doesNotHaveVisibleDrawable(drawable);

    return this;
  }

  /**
   * Inverse of {@link #hasVisibleDrawable(int)}.
   *
   * @deprecated Use {@link LithoViewAssert#hasVisibleDrawable(Drawable)} instead.
   */
  @Deprecated
  public ComponentAssert doesNotHaveVisibleDrawable(@DrawableRes int drawableRes) {
    assertThatLithoView().doesNotHaveVisibleDrawable(drawableRes);

    return this;
  }

  /**
   * Assert that the given component has the exact text provided.
   *
   * @deprecated Use {@link LithoViewAssert#hasVisibleText(String)} instead.
   */
  @Deprecated
  public ComponentAssert hasVisibleText(String text) {
    assertThatLithoView().hasVisibleText(text);

    return this;
  }

  /**
   * Assert that the given component has the exact text identified by resource id.
   *
   * @deprecated Use {@link LithoViewAssert#hasVisibleText(int)} instead.
   */
  @Deprecated
  public ComponentAssert hasVisibleText(@StringRes int resourceId) {
    assertThatLithoView().hasVisibleText(resourceId);

    return this;
  }

  /**
   * Inverse of {@link #hasVisibleText(String)}.
   *
   * @deprecated Use {@link LithoViewAssert#doesNotHaveVisibleText(String)} instead.
   */
  @Deprecated
  public ComponentAssert doesNotHaveVisibleText(String text) {
    assertThatLithoView().doesNotHaveVisibleText(text);

    return this;
  }

  /**
   * Inverse of {@link #hasVisibleText(int)} .
   *
   * @deprecated Use {@link LithoViewAssert#doesNotHaveVisibleText(int)} instead.
   */
  @Deprecated
  public ComponentAssert doesNotHaveVisibleText(@StringRes int resourceId) {
    assertThatLithoView().doesNotHaveVisibleText(resourceId);

    return this;
  }

  /**
   * Assert that the given component contains the provided pattern.
   *
   * @deprecated Use {@link LithoViewAssert#hasVisibleTextMatching(String)} instead.
   */
  @Deprecated
  public ComponentAssert hasVisibleTextMatching(String pattern) {
    assertThatLithoView().hasVisibleTextMatching(pattern);

    return this;
  }

  /**
   * Inverse of {@link #hasVisibleTextMatching(String)}.
   *
   * @deprecated Use {@link LithoViewAssert#doesNotHaveVisibleTextMatching(String)} instead.
   */
  @Deprecated
  public ComponentAssert doesNotHaveVisibleTextMatching(String pattern) {
    assertThatLithoView().doesNotHaveVisibleTextMatching(pattern);

    return this;
  }

  /**
   * Assert that the given component contains the provided text.
   *
   * @deprecated Use {@link LithoViewAssert#hasVisibleTextContaining(String)} instead.
   */
  @Deprecated
  public ComponentAssert hasVisibleTextContaining(String text) {
    assertThatLithoView().hasVisibleTextContaining(text);

    return this;
  }

  /**
   * Inverse of {@link #hasVisibleTextContaining(String)}.
   *
   * @deprecated Use {@link LithoViewAssert#doesNotHaveVisibleTextContaining(String)} instead.
   */
  @Deprecated
  public ComponentAssert doesNotHaveVisibleTextContaining(String text) {
    assertThatLithoView().doesNotHaveVisibleTextContaining(text);

    return this;
  }

  /**
   * Assert that the view tag is present for the given index.
   *
   * @param tagId Index of the view tag.
   * @param tagValue View tag value.
   * @deprecated Use {@link LithoViewAssert#hasViewTag(int, Object)} instead.
   */
  @Deprecated
  public ComponentAssert hasViewTag(int tagId, Object tagValue) {
    assertThatLithoView().hasViewTag(tagId, tagValue);

    return this;
  }

  /**
   * Verifies that the component contains the exact list of provided sub-components.
   *
   * @deprecated Use {@link LithoViewAssert#containsComponents(Class)} instead.
   */
  @Deprecated
  public ComponentAssert hasSubComponents(SubComponent... subComponents) {
    final List<SubComponent> mountedSubComponents =
        ComponentTestHelper.getSubComponents(mComponentContext, actual);

    Assertions.assertThat(mountedSubComponents).containsExactly(subComponents);

    return this;
  }

  /**
   * Verifies that the component contains only the given sub-components and nothing else, in order.
   *
   * @deprecated Use {@link LithoViewAssert#hasExactly(int, Class)} instead.
   */
  @Deprecated
  public ComponentAssert containsOnlySubComponents(SubComponent... subComponents) {
    final List<SubComponent> mountedSubComponents =
        ComponentTestHelper.getSubComponents(mComponentContext, actual);

    Assertions.assertThat(mountedSubComponents).containsOnly(subComponents);

    return this;
  }

  /**
   * Extract values from the underlying component based on the {@link Extractor} provided.
   *
   * @param extractor The extractor applied to the Component.
   * @param <A> Type of the value extracted.
   * @return ListAssert for the extracted values.
   */
  @CheckReturnValue
  public <A> ListAssert<A> extracting(Extractor<Component, List<A>> extractor) {
    final List<A> value = extractor.extract(actual);
    return new ListAssert<>(value);
  }

  /**
   * Extract the sub components from the underlying Component, returning a ListAssert over it
   *
   * @deprecated Use {@link LithoViewAssert#hasExactly(int, Class)} instead.
   */
  @Deprecated
  @CheckReturnValue
  public ListAssert<InspectableComponent> extractingSubComponents(ComponentContext c) {
    return extracting(SubComponentExtractor.subComponents(c));
  }

  /**
   * Extract the sub components recursively from the underlying Component, returning a ListAssert
   * over it.
   *
   * @deprecated Use {@link LithoViewAssert#hasExactly(int, Class)} instead.
   */
  @Deprecated
  @CheckReturnValue
  public ListAssert<InspectableComponent> extractingSubComponentsDeeply(ComponentContext c) {
    return extracting(SubComponentDeepExtractor.subComponentsDeeply(c));
  }

  public ComponentAssert extractingSubComponentAt(int index) {
    InspectableComponent component =
        SubComponentExtractor.subComponents(mComponentContext).extract(actual).get(index);
    return new ComponentAssert(mComponentContext, component.getComponent());
  }

  /**
   * Assert that a given {@link Component} renders to null, i.e. its <code>onCreateLayout
   * </code> returns null.
   *
   * @deprecated Use {@link LithoViewAssert#willNotRenderContent()} ()} that will check if the root
   *     component will return null or a child with width oir height equal to 0
   */
  @Deprecated
  public ComponentAssert wontRender() {
    Assertions.assertThat(Component.willRender(mComponentContext, actual))
        .overridingErrorMessage("Expected Component to render to null, but it did not.")
        .isFalse();

    return this;
  }

  /**
   * Assert that a given {@link Component} produces a non-null layout.
   *
   * @deprecated Use {@link LithoViewAssert#willRenderContent()} that will check if the root
   *     component won't return null or a child with height and width equal to 0
   */
  @Deprecated
  public ComponentAssert willRender() {
    Assertions.assertThat(Component.willRender(mComponentContext, actual))
        .overridingErrorMessage("Expected Component to not render to null, but it did.")
        .isTrue();

    return this;
  }

  /** Assert that a given {@link Component} has a property equaling the provided value. */
  public <T1, T2> ComponentAssert hasProps(KProperty1<T2, T1> property, T1 value) {
    return hasPropsMatching(property, IsEqual.equalTo(value));
  }

  /** Assert that a given {@link Component} has a property matching the provided matcher. */
  public <T1, T2> ComponentAssert hasPropsMatching(
      KProperty1<T2, T1> property, Matcher<T1> matcher) {
    MatcherAssert.assertThat(property.get((T2) actual), matcher);
    return this;
  }

  /** @deprecated see {@link #wontRender()} */
  @Deprecated
  public ComponentAssert willNotRender() {
    return wontRender();
  }
}
