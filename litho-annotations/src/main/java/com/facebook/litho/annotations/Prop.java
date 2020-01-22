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

package com.facebook.litho.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Inputs to a {@code Component} are its props. The props for a given Component are the union of all
 * arguments annotated with {@code Prop} in your spec methods. The same prop can be defined and
 * accessed in multiple lifecycle methods. The annotation processor will validate that the props are
 * being used with the correct type and name.
 *
 * <p>For each unique prop defined on the spec, the annotation processor adds a setter method on the
 * Component's Builder that has the same name as the prop. By default props are required but can be
 * marked as {@link #optional()}. A prop can also be constrained further by setting it's {@link
 * #resType()}.
 *
 * <p>The parent component sets the props when it creates the component in it's {@link
 * OnCreateLayout} method. The props cannot be updated throughout the lifecycle of the component
 * unless they are marked as {@link #dynamic()}. If the layout need to be updated, the parent has to
 * create a new component and set new props. The props <em>should</em> be immutable since the layout
 * can be calculated on multiple threads. Immutability of the props ensures that no thread safety
 * issues can occur in the component hierarchy.
 *
 * <p><b>Creating Props:</b> <br>
 *
 * <pre><code>{@literal @LayoutSpec}
 * public class HeaderSpec {
 *
 *  {@literal @OnCreateLayout}
 *   static Component onCreateLayout(
 *     ComponentContext c,
 *    {@literal @Prop} MyTitles title,
 *    {@literal @Prop(varArg = imageUrl)} List<String> urls,
 *    {@literal @Prop(optional = true)} boolean isSelected) {
 *     if (urls.isEmpty()) {
 *       return null;
 *     }
 *
 *     return Column.create(c)
 *       .paddingDip(YogaEdge.ALL, 8)
 *       .backgroundColor(isSelected ? Color.WHITE : Color.GREEN)
 *       .child(
 *         Image.create(c)
 *           .url(urls.get(0))
 *           .marginDip(YogaEdge.BOTTOM, 4)
 *       )
 *       .child(
 *         Text.create(c)
 *           .text(title.getTitle())
 *           .textSizeSp(16)
 *           .marginDip(YogaEdge.BOTTOM, 4)
 *       )
 *       .child(
 *         Text.create(c)
 *           .text(title.getSubtitle())
 *           .textSizeSp(12)
 *       )
 *       .build();
 *   }
 * }</code></pre>
 *
 * <p>Notice how {@code imageUrl}, {@code title} and {@code isSelected} are used to set properties
 * on different components within the layout.
 *
 * <p><b>Setting Props:</b> <br>
 *
 * <pre><code>{@literal @LayoutSpec}
 * public class MyComponent {
 *
 *  {@literal @OnCreateLayout}
 *   static Component onCreateLayout(ComponentContext c) {
 *
 *     return Header.create(c)
 *       .title(new MyTitles("title", "subtitle"))
 *       .imageUrl("https://example.com/image.jpg")
 *       .build();
 *   }
 * }</code></pre>
 *
 * @see ResType
 * @see PropDefault
 * @see OnCreateLayout
 * @see State
 * @see Param
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Prop {

  /**
   * Declares that this prop can be omitted by the caller of the component. When a prop is omitted
   * it will take its default value. Its default value will be the standard default initialization
   * value according to the Java standard (e.g. 0, false, null) or the value of a constant declared
   * in spec which is annotated with {@link PropDefault} and has the same name and type as the prop.
   *
   * @return {@code true} iff the prop is optional otherwise {@code false}.
   */
  boolean optional() default false;

  /**
   * Marks this prop as one that corresponds to a specific Android resource type, and therefore
   * generates various helper methods to initialize it.
   *
   * <p>For example, a {@link CharSequence} prop named "title" may be marked as {@link
   * ResType#STRING}. This will make the component have not only method "title(CharSequence)" but
   * also various methods that enable initializing the prop from a resource or attribute:
   *
   * <pre><code>
   *   titleRes(@StringRes int resId)
   *   titleRes(@StringRes int resId, Object... formatArgs)
   *   titleAttr(@AttrRes int attrResId, @StringRes int defResId)
   *   titleAttr(@AttrRes int attrResId)
   * </code></pre>
   *
   * @return The resource type.
   */
  ResType resType() default ResType.NONE;

  /** @return The documentation for the annotated parameter. */
  String docString() default "";

  /**
   * Declares this prop supports a variable arguments, and provide utility methods to add values to
   * the prop.
   *
   * <p>For example, having {@code @Prop(varArg="name") List<CharSequence> names} would generate a
   * setter {@code name} method which can be called multiple times to add a set of names.
   *
   * <p>The prop must be a parameterized list. It is effectively always {@link #optional()}, and has
   * an empty list (immutable) as a default value.
   *
   * <pre><code>
   *   MyComponent.create(c)
   *     .name("A")
   *     .name("B")
   *     .name("C")
   * </code></pre>
   *
   * @return The declared name of the setter method.
   */
  String varArg() default "";

  /** @return {@code true} if the name of the prop conflicts with a common prop. */
  boolean isCommonProp() default false;

  /**
   * This may only be set to true if {@link #isCommonProp} is also set to {@code true}. If {@code
   * true}, then the behavior of it's common props will be overridden. If {@code false}, then the
   * common prop will applied by the framework level as normal as well as any behavior that the
   * component declares within the spec.
   *
   * @return {@code true} if the framework should not apply the common prop.
   */
  boolean overrideCommonPropBehavior() default false;

  /**
   * <strong>EXPERIMENTAL</strong>. This is a part of new experimental API. There is no guarantee
   * that this API will work or that it will remain the same.
   *
   * <p>Marks a {@link Prop} as dynamic, so that a DynamicValue object could be passed to a
   * Component that the {@link Prop} belongs to, which makes it possible to update the actual value
   * of the {@link Prop} bypassing LayoutState and MountState.
   *
   * <p>Only Props that <strong>DO NOT AFFECT LAYOUT</strong> could be marked as dynamic.
   *
   * <p>Additionally, for every dynamic {@link Prop}, a ComponentSpec must contain {@link
   * OnBindDynamicValue} method, that applies the actual value to the mounted content.
   *
   * @see OnBindDynamicValue
   */
  boolean dynamic() default false;
}
