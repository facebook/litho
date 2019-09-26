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

package com.facebook.litho.testing.subcomponents;

import com.facebook.litho.Component;
import org.assertj.core.api.Condition;

/**
 * Allows convenient type matching comparison for instances of {@link Component}s. Useful for
 * verifying the existence of sub-components that are part of a layout.
 */
public class SubComponent {

  public static SubComponent of(Class<? extends Component> componentType) {
    return new SubComponent(componentType, null);
  }

  public static <T extends Component> SubComponent of(T component) {
    return new SubComponent(component.getClass(), component);
  }

  private final Class<? extends Component> mComponentType;
  private final Component mComponent;

  private SubComponent(Class<? extends Component> componentType, Component component) {
    mComponentType = componentType;
    mComponent = component;
  }

  public Component getComponent() {
    return mComponent;
  }

  public Class<? extends Component> getComponentType() {
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
    return mComponentType.toString();
  }

  /**
   * For testing purposes, props are only compared if both subcomponents supply them. Otherwise,
   * just ignore them.
   */
  private static boolean arePropsEqual(Component thatComponent, Component thisComponent) {
    return thatComponent == null
        || thisComponent == null
        || thatComponent.isEquivalentTo(thisComponent);
  }

  /**
   * Use an old-style {@link SubComponent} in an assertj Condition.
   *
   * <p>For instance:
   *
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
