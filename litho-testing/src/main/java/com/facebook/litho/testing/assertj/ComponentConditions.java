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

import static org.hamcrest.core.Is.is;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.testing.subcomponents.InspectableComponent;
import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;
import kotlin.reflect.KProperty1;
import org.assertj.core.api.Condition;
import org.assertj.core.description.TextDescription;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsEqual;

/**
 * Various helpers to match against {@link InspectableComponent}s. This is to be used with {@link
 * SubComponentExtractor#subComponentWith(ComponentContext, Condition)} and {@link
 * SubComponentDeepExtractor#deepSubComponentWith(ComponentContext, Condition)}.
 */
public final class ComponentConditions {
  private ComponentConditions() {}

  /**
   * Matcher that succeeds if the class of an {@link InspectableComponent} exactly matches the
   * provided component class.
   *
   * <p><em>Note that this won't match sub-types.</em> This should not have any real-world
   * implications as Components are not sub-classed.
   *
   * <p>
   *
   * <h2>Example Use</h2>
   *
   * <pre><code>
   * assertThat(c, mComponent)
   *   .has(
   *     allOf(
   *       deepSubComponentWith(c, typeIs(Text.class)),
   *       subComponentWith(c, typeIs(MyCustomComponent.class))));
   * </code></pre>
   */
  public static Condition<InspectableComponent> inspectedTypeIs(
      final Class<? extends Component> clazz) {
    return new Condition<InspectableComponent>(
        new TextDescription("Component with type <%s>", clazz)) {
      @Override
      public boolean matches(InspectableComponent value) {
        final Class component = value.getComponentClass();
        return component != null && component.equals(clazz);
      }
    };
  }

  /**
   * Matcher that succeeds if the class of an {@link Component} exactly matches the provided
   * component class.
   *
   * <p><em>Note that this won't match sub-types.</em> This should not have any real-world
   * implications as Components are not sub-classed.
   *
   * <p>
   *
   * <h2>Example Use</h2>
   *
   * <pre><code>
   * assertThat(c, mComponent)
   *   .has(
   *     allOf(
   *       deepSubComponentWith(c, typeIs(Text.class)),
   *       subComponentWith(c, typeIs(MyCustomComponent.class))));
   * </code></pre>
   */
  public static Condition<Component> typeIs(final Class<? extends Component> clazz) {
    return new Condition<Component>(new TextDescription("Component with type <%s>", clazz)) {
      @Override
      public boolean matches(Component value) {
        Class componentClass = value.getClass();
        return componentClass != null && componentClass.equals(clazz);
      }
    };
  }

  public static Condition<Component> typeIs(final KClass<? extends Component> clazz) {
    return new Condition<Component>(new TextDescription("Component with type <%s>", clazz)) {
      @Override
      public boolean matches(Component value) {
        Class componentClass = value.getClass();
        return componentClass != null
            && componentClass.equals(JvmClassMappingKt.getJavaClass(clazz));
      }
    };
  }

  /**
   * Matcher that succeeds if the class of an {@link Component} has a property with the given value.
   *
   * <p><em>This will throw an exception if used on a component other then the one the property is
   * defined on. So its best to chain this with {@link #inspectedTypeIs}.</em>
   *
   * <h2>Example Use</h2>
   *
   * <pre><code>
   * assertThat(c, mComponent)
   *   .has(
   *     deepSubComponentWith(
   *       c,
   *       allOf(
   *          inspectedTypeIs(Favicon.class),
   *          hasProp(Favicon::uri, FAVICON_URI))));
   * </code></pre>
   */
  public static <TComponent, TValue> Condition<InspectableComponent> hasProps(
      KProperty1<TComponent, TValue> property, TValue expectedValue) {
    return new Condition<InspectableComponent>(
        new TextDescription("Component with prop %s = %s", property.getName(), expectedValue)) {

      @Override
      public boolean matches(InspectableComponent value) {
        final TValue propValue = property.get((TComponent) value.getComponent());
        return IsEqual.equalTo(propValue).matches(expectedValue);
      }
    };
  }

  /**
   * Matcher that succeeds if a {@link InspectableComponent} has a text content that exactly matches
   * the provided string.
   */
  public static Condition<InspectableComponent> textEquals(final CharSequence text) {
    return text(is(text.toString()));
  }

  /**
   * @see #textEquals(CharSequence)
   */
  public static Condition<InspectableComponent> textEquals(final String text) {
    return text(is(text));
  }

  /**
   * Matcher that succeeds if a {@link InspectableComponent} has text content that matches the
   * provided condition.
   *
   * <p>N.B. We are implicitly casting the {@link CharSequence} to a {@link String} when matching so
   * that more powerful matchers can be applied like sub-string matching.
   *
   * <p>
   *
   * <h2>Example Use</h2>
   *
   * <pre><code>
   * mComponent = MyComponent.create(c).text("Cells interlinked within cells interlinked").build();
   * assertThat(c, mComponent)
   *   .has(subComponentWith(c, startsWith("Cells")))
   *   .doesNotHave(subComponentWith(c, text(containsString("A Tall White Fountain Played."))));
   * </code></pre>
   */
  public static Condition<InspectableComponent> text(final Condition<String> condition) {
    return new Condition<InspectableComponent>() {
      @Override
      public boolean matches(InspectableComponent value) {
        as("text <%s>", condition);
        return condition.matches(value.getTextContent());
      }
    };
  }

  /**
   * Matcher that succeeds if a {@link InspectableComponent} has text content that matches the
   * provided hamcrest matcher.
   *
   * <p>
   *
   * @return Wrapper around {@link #text(Condition)}
   */
  public static Condition<InspectableComponent> text(final Matcher<String> matcher) {
    return text(new HamcrestCondition<>(matcher));
  }
}
