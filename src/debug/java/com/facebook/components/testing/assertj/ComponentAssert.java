// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.testing.assertj;

import java.util.List;

import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLifecycle;
import com.facebook.litho.ComponentView;
import com.facebook.litho.testing.ComponentTestHelper;
import com.facebook.litho.testing.SubComponent;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.powermock.reflect.Whitebox;

/**
 * Assertion methods for {@link Component}s.
 *
 * <p> To create an instance of this class, invoke
 * <code>{@link ComponentAssert#assertThat(ComponentContext, Component)}</code>.
 * or
 * <code>{@link ComponentAssert#assertThat(Component.Builder)}</code>.
 */
public final class ComponentAssert extends AbstractAssert<ComponentAssert, Component> {

  private final ComponentContext mComponentContext;

  public static ComponentAssert assertThat(ComponentContext componentContext, Component component) {
    return new ComponentAssert(componentContext, component);
  }

  public static <L extends ComponentLifecycle> ComponentAssert assertThat(
      Component.Builder<L> builder) {
    // mContext is freed up during build() so we need to get a reference to it before.
    final ComponentContext context =
        Whitebox.getInternalState(builder, "mContext");
    return new ComponentAssert(context, builder.build());
  }

  private ComponentAssert(ComponentContext c, Component actual) {
    super(actual, ComponentAssert.class);
    mComponentContext = c;
  }

  private ComponentView mountComponent() {
    return ComponentTestHelper.mountComponent(mComponentContext, actual);
  }

  private ComponentViewAssert assertThatComponentView() {
    return ComponentViewAssert.assertThat(mountComponent());
  }

  /**
   * Assert that the given component has no sub-components.
   */
  public ComponentAssert hasNoSubComponents() {
    final List<SubComponent> subComponents = ComponentTestHelper.getSubComponents(
        mComponentContext,
        actual);
    Assertions.assertThat(subComponents)
        .overridingErrorMessage(
            "Expected Component not to have any sub " +
                "components, but found %d.",
            subComponents.size())
        .isEmpty();

    return this;
  }

  /**
   * Assert that the given component contains the provided sub-component.
   */
  public ComponentAssert containsSubComponent(SubComponent subComponent) {
    final List<SubComponent> subComponents = ComponentTestHelper.getSubComponents(
        mComponentContext,
        actual);
    Assertions.assertThat(subComponents)
        .overridingErrorMessage(
            "Expected to find <%s> as sub component of <%s>, " +
                "but couldn't find it among the %d sub components.",
            subComponent,
            actual,
            subComponents.size())
        .contains(subComponent);

    return this;
  }

  /**
   * Assert that the given component does <strong>not</strong> contain the provided sub-component.
   */
  public ComponentAssert doesNotContainSubComponent(SubComponent subComponent) {
    final List<SubComponent> subComponents = ComponentTestHelper.getSubComponents(
        mComponentContext,
        actual);
    Assertions.assertThat(subComponents)
        .overridingErrorMessage(
            "Did not expect to find <%s> as sub component of <%s>, " +
                "but it was present.",
            subComponent,
            actual)
        .doesNotContain(subComponent);

    return this;
  }

  /**
   * Assert that any view in the given Component has the provided content
   * description.
   */
  public ComponentAssert hasContentDescription(String contentDescription) {
    assertThatComponentView().hasContentDescription(contentDescription);

    return this;
  }

  /**
   * Assert that the given component contains the drawable identified by the provided drawable
   * resource id.
   */
  public ComponentAssert hasVisibleDrawable(@DrawableRes int drawableRes) {
    assertThatComponentView().hasVisibleDrawable(drawableRes);

    return this;
  }

  /**
   * Assert that the given component contains the drawable provided.
   */
  public ComponentAssert hasVisibleDrawable(Drawable drawable) {
    assertThatComponentView().hasVisibleDrawable(drawable);

    return this;
  }

  /**
   * Inverse of {@link #hasVisibleDrawable(Drawable)}
   */
  public ComponentAssert doesNotHaveVisibleDrawable(Drawable drawable) {
    assertThatComponentView().doesNotHaveVisibleDrawable(drawable);

    return this;
  }

  /**
   * Inverse of {@link #hasVisibleDrawable(int)}
   */
  public ComponentAssert doesNotHaveVisibleDrawable(@DrawableRes int drawableRes) {
    assertThatComponentView().doesNotHaveVisibleDrawable(drawableRes);

    return this;
  }

  /**
   * Assert that the given component has the exact text provided.
   */
  public ComponentAssert hasVisibleText(String text) {
    assertThatComponentView().hasVisibleText(text);

    return this;
  }

  /**
   * Assert that the view tag is present for the given index.
   * @param tagId Index of the view tag.
   * @param tagValue View tag value.
   */
  public ComponentAssert hasViewTag(int tagId, Object tagValue) {
    assertThatComponentView().hasViewTag(tagId, tagValue);

    return this;
  }

  /**
   * Verifies that the component contains the exact list of provided sub-components.
   */
  public ComponentAssert hasSubComponents(SubComponent... subComponents) {
    final List<SubComponent> mountedSubComponents = ComponentTestHelper.getSubComponents(
        mComponentContext,
        actual);

    Assertions.assertThat(mountedSubComponents)
        .isEqualTo(subComponents);

    return this;
  }

  /**
   * Verifies that the component contains only the given sub-components and nothing else,
   * in order.
   */
  public ComponentAssert containsOnlySubComponents(SubComponent... subComponents) {
    final List<SubComponent> mountedSubComponents = ComponentTestHelper.getSubComponents(
        mComponentContext,
        actual);

    Assertions.assertThat(mountedSubComponents)
        .containsOnly(subComponents);

    return this;
  }
}
