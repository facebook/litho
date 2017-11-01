/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.subcomponents;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentLifecycle;
import org.assertj.core.api.Condition;

/**
 * Allows convenient type matching comparison for instances of {@link ComponentLifecycle}s.
 * Useful for verifying the existence of sub-components that are part of a layout.
 */
public class SubComponent {

  public static SubComponent of(Class<? extends ComponentLifecycle> componentType) {
    return new SubComponent(componentType, null);
  }

  public static SubComponent of(Component component) {
    return new SubComponent(component.getLifecycle().getClass(), component);
  }

  private final Class<? extends ComponentLifecycle> mComponentType;
  private final Component<?> mComponent;

  private SubComponent(Class<? extends ComponentLifecycle> componentType, Component<?> component) {
    mComponentType = componentType;
    mComponent = component;
  }

  public Component<?> getComponent() {
    return mComponent;
  }

  public Class<? extends ComponentLifecycle> getComponentType() {
    return mComponentType;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof SubComponent)) {
      return false;
    }

    SubComponent that = (SubComponent) o;
    return that.mComponentType.equals(mComponentType) && arePropsEqual(that.mComponent, mComponent);
  }

  @Override
  public int hashCode() {
    return mComponentType.hashCode();
  }

  @Override
  public String toString() {
    return mComponentType.toString() + " [" + super.toString() +"]";
  }

  /**
   * For testing purposes, props are only compared if both subcomponents supply them. Otherwise,
   * just ignore them.
   */
  private static boolean arePropsEqual(Component<?> thatComponent, Component<?> thisComponent) {
    return thatComponent == null ||
        thisComponent == null ||
        thatComponent.isEquivalentTo(thisComponent);
  }

  /**
   * Use an old-style {@link SubComponent} in an assertj Condition.
   *
   * For instance:
   * <pre><code>
   *   assertThat(c, mComponent)
   *    .has(
   *        subComponentWith(
   *            c,
   *            legacySubComponent(
   *                SubComponent.of(
   *                    FooterComponent.create(c).text("Rockstar Developer").build()))));
   * </code></pre>
   *
   * @param subComponent The constructed {@link SubComponent#of(Component)}.
   * @return A condition to be used with {@link org.assertj.core.api.Assertions#assertThat}
   */
  public static Condition<InspectableComponent> legacySubComponent(
      final SubComponent subComponent) {
    return new Condition<InspectableComponent>() {
      @Override
      public boolean matches(InspectableComponent value) {
        final Component component = value.getComponent();
        return component != null && SubComponent.of(component).equals(subComponent);
      }
    };
  }
}
