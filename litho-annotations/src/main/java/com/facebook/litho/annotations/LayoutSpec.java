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
 * A class that is annotated with this annotation will be used to create a composite component that
 * is made up of other components. A layout spec is the logical equivalent of a composite view in
 * Android.
 *
 * <p>The class annotated with {@link LayoutSpec} must implement a method with the {@link
 * OnCreateLayout} or {@link OnCreateLayoutWithSizeSpec} annotation. It may also implement methods
 * with the following annotations:
 *
 * <ul>
 *   <li>{@link OnCreateInitialState}
 *   <li>{@link OnCreateTreeProp}
 *   <li>{@link OnCreateTransition}
 *   <li>{@link OnUpdateState}
 *   <li>{@link OnEvent}
 *   <li>{@link OnLoadStyle}
 *   <li>{@link OnEnteredRange}
 *   <li>{@link OnExitedRange}
 *   <li>{@link OnRegisterRanges}
 *   <li>{@link OnCalculateCachedValue}
 *   <li>{@link ShouldUpdate}
 * </ul>
 *
 * <p>Example: <br>
 *
 * <pre><code>{@literal @LayoutSpec}
 * public class CounterSpec {
 *
 *  {@literal @OnCreateLayout}
 *   static Component onCreateLayout(
 *     ComponentContext c,
 *    {@literal @Prop} int id,
 *    {@literal @State} int count) {
 *
 *     return Row.create(c)
 *       .backgroundColor(Color.WHITE)
 *       .heightDip(64)
 *       .paddingDip(YogaEdge.ALL, 8)
 *       .child(
 *         Text.create(c)
 *           .text(" + ")
 *           .clickHandler(Counter.onClick(c))
 *       )
 *       .child(
 *         Text.create(c)
 *           .text(String.valueOf(count))
 *       )
 *       .build();
 *   }
 *
 *  {@literal @OnCreateInitialState}
 *   static void onCreateInitialState(
 *       ComponentContext c,
 *       StateValue&lt;Integer&gt; count) {
 *     count.set(0);
 *   }
 *
 *  {@literal @OnEvent(ClickEvent.class)}
 *   static void onClick(ComponentContext c, @Prop int id) {
 *     Counter.increment(c, id);
 *   }
 *
 *  {@literal @OnUpdateState}
 *   static void increment(StateValue&lt;Integer&gt; count, @Param int counterId) {
 *     count.set(count.get() + 1);
 *   }
 * }</code></pre>
 *
 * <img alt="layout spec flow chart"
 * src="https://fblitho.com/static/images/flow-chart-v0.23.1-layout-spec.svg">
 *
 * <p>If you want to create a component that mounts its own content, then use {@link MountSpec}
 * instead. See more docs at <a href="https://fblitho.com/docs/layout-specs">https://fblitho.com</a>
 *
 * @see Prop
 * @see TreeProp
 * @see InjectProp
 * @see State
 * @see Param
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface LayoutSpec {

  /**
   * Class name of the generated component. When not provided defaults to name of the annotated
   * class sans the "Spec" suffix. E.g. "MyComponentSpec" to "MyComponent".
   *
   * <p>In order to avoid confusion, this should only be used if you have a very good reason for it.
   * For instance to avoid naming collisions.
   */
  String value() default "";

  /**
   * @return Boolean indicating whether the generated class should be public. If not, it will be
   *     package-private.
   */
  boolean isPublic() default true;

  /**
   * @return List of event POJOs this component can dispatch. Used to generate event dispatch
   *     methods.
   */
  Class<?>[] events() default {};

  /**
   * @return List of trigger POJOs this component can dispatch. Used to generate trigger creation
   *     methods.
   */
  Class<?>[] triggers() default {};

  /**
   * @return the prop name of the Component this component will delegate getSimpleName calls to.
   *     This will cause getSimpleName to include the name of that component in its return. This is
   *     useful for seeing better debug information when using generic wrapping components (like
   *     error boundary wrappers).
   */
  String simpleNameDelegate() default "";
}
