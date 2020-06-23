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
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Java6Assertions;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.iterable.Extractor;
import org.assertj.core.util.CheckReturnValue;

/**
 * Assertion methods for {@link Component}s.
 *
 * <p>To create an instance of this class, invoke <code>
 * {@link ComponentAssert#assertThat(ComponentContext, Component)}</code> or <code>
 * {@link ComponentAssert#assertThat(Component.Builder)}</code>.
 *
 * <p>Alternatively, use {@link LithoAssertions} which provides entry points to all Litho AssertJ
 * helpers.
 */
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

  /** Performs a state update and returns the new view. */
  public LithoViewAssert afterStateUpdate() {
    return LithoViewAssert.assertThat(
        StateUpdatesTestHelper.getViewAfterStateUpdate(mComponentContext, actual));
  }

  public LithoViewAssert withStateUpdate(final StateUpdatesTestHelper.StateUpdater updater) {
    return LithoViewAssert.assertThat(
        StateUpdatesTestHelper.getViewAfterStateUpdate(mComponentContext, actual, updater));
  }

  private ComponentAssert(ComponentContext c, Component actual) {
    super(actual, ComponentAssert.class);
    mComponentContext = c;
    mComponentContext.setLayoutStateContextForTesting();
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
    Java6Assertions.assertThat(subComponents)
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
    Java6Assertions.assertThat(subComponents)
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
    Java6Assertions.assertThat(subComponents)
        .overridingErrorMessage(
            "Did not expect to find <%s> as sub component of <%s>, " + "but it was present.",
            subComponent, actual)
        .doesNotContain(subComponent);

    return this;
  }

  /** Assert that any view in the given Component has the provided content description. */
  public ComponentAssert hasContentDescription(String contentDescription) {
    assertThatLithoView().hasContentDescription(contentDescription);

    return this;
  }

  /**
   * Assert that the given component contains the drawable identified by the provided drawable
   * resource id.
   */
  public ComponentAssert hasVisibleDrawable(@DrawableRes int drawableRes) {
    assertThatLithoView().hasVisibleDrawable(drawableRes);

    return this;
  }

  /** Assert that the given component contains the drawable provided. */
  public ComponentAssert hasVisibleDrawable(Drawable drawable) {
    assertThatLithoView().hasVisibleDrawable(drawable);

    return this;
  }

  /** Inverse of {@link #hasVisibleDrawable(Drawable)} */
  public ComponentAssert doesNotHaveVisibleDrawable(Drawable drawable) {
    assertThatLithoView().doesNotHaveVisibleDrawable(drawable);

    return this;
  }

  /** Inverse of {@link #hasVisibleDrawable(int)} */
  public ComponentAssert doesNotHaveVisibleDrawable(@DrawableRes int drawableRes) {
    assertThatLithoView().doesNotHaveVisibleDrawable(drawableRes);

    return this;
  }

  /** Assert that the given component has the exact text provided. */
  public ComponentAssert hasVisibleText(String text) {
    assertThatLithoView().hasVisibleText(text);

    return this;
  }

  /** Assert that the given component has the exact text identified by resource id. */
  public ComponentAssert hasVisibleText(@StringRes int resourceId) {
    assertThatLithoView().hasVisibleText(resourceId);

    return this;
  }

  /** Inverse of {@link #hasVisibleText(String)} */
  public ComponentAssert doesNotHaveVisibleText(String text) {
    assertThatLithoView().doesNotHaveVisibleText(text);

    return this;
  }

  /** Inverse of {@link #hasVisibleText(int)} */
  public ComponentAssert doesNotHaveVisibleText(@StringRes int resourceId) {
    assertThatLithoView().doesNotHaveVisibleText(resourceId);

    return this;
  }

  /** Assert that the given component contains the provided pattern. */
  public ComponentAssert hasVisibleTextMatching(String pattern) {
    assertThatLithoView().hasVisibleTextMatching(pattern);

    return this;
  }

  /**
   * Assert that the view tag is present for the given index.
   *
   * @param tagId Index of the view tag.
   * @param tagValue View tag value.
   */
  public ComponentAssert hasViewTag(int tagId, Object tagValue) {
    assertThatLithoView().hasViewTag(tagId, tagValue);

    return this;
  }

  /**
   * Verifies that the component contains the exact list of provided sub-components.
   *
   * @deprecated Use {@link #extractingSubComponents} instead.
   */
  @Deprecated
  public ComponentAssert hasSubComponents(SubComponent... subComponents) {
    final List<SubComponent> mountedSubComponents =
        ComponentTestHelper.getSubComponents(mComponentContext, actual);

    Java6Assertions.assertThat(mountedSubComponents).containsExactly(subComponents);

    return this;
  }

  /**
   * Verifies that the component contains only the given sub-components and nothing else, in order.
   *
   * @deprecated Use {@link #extractingSubComponents} instead.
   */
  @Deprecated
  public ComponentAssert containsOnlySubComponents(SubComponent... subComponents) {
    final List<SubComponent> mountedSubComponents =
        ComponentTestHelper.getSubComponents(mComponentContext, actual);

    Java6Assertions.assertThat(mountedSubComponents).containsOnly(subComponents);

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

  /** Extract the sub components from the underlying Component, returning a ListAssert over it. */
  @CheckReturnValue
  public ListAssert<InspectableComponent> extractingSubComponents(ComponentContext c) {
    return extracting(SubComponentExtractor.subComponents(c));
  }

  /**
   * Extract the sub components recursively from the underlying Component, returning a ListAssert
   * over it.
   */
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
   * </code> method resolves to a {@link ComponentContext#NULL_LAYOUT}.
   */
  public ComponentAssert wontRender() {
    Java6Assertions.assertThat(Component.willRender(mComponentContext, actual))
        .overridingErrorMessage("Expected Component to render to null, but it did not.")
        .isFalse();

    return this;
  }

  /**
   * Assert that a given {@link Component} produces a layout that's not equivalent to {@link
   * ComponentContext#NULL_LAYOUT}.
   */
  public ComponentAssert willRender() {
    Java6Assertions.assertThat(Component.willRender(mComponentContext, actual))
        .overridingErrorMessage("Expected Component to not render to null, but it did.")
        .isTrue();

    return this;
  }

  /** @see #wontRender() */
  public ComponentAssert willNotRender() {
    return wontRender();
  }
}
