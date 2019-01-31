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

package com.facebook.litho.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A class that is annotated with this annotation will be used to create a component lifecycle that
 * is made up of other components.
 * <p>A class that is annotated with {@link LayoutSpec} must implement a method with the
 * {@link OnCreateLayout} annotation. It may also implement methods with the following annotations:
 * - {@link OnLoadStyle}
 * - {@link OnEvent}
 * <p>If you wish to create a component that mounts its own content, then use {@link MountSpec}
 * instead.
 * <p>For example:
 * <pre>
 * <code>{@literal @}LayoutSpec
 * public class MyComponentSpec {
 *
 *  {@literal @}OnCreateLayout
 *   ComponentLayout onCreateLayout(LayoutContext c, @Prop MyProp prop) {
 *       return Row.create(c)
 *           .alignItems(FLEX_START)
 *           .child(someChild1)
 *           .child(someChild2)
 *           .build();
 *   }
 * }
 * </code>
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface LayoutSpec {

  /**
   * Class name of the generated component. When not provided defaults to name of the annotated
   * class sans the "Spec" suffix. E.g. "MyComponentSpec" to "MyComponent".
   *
   * In order to avoid confusion, this should only be used if you have a very good reason for it.
   * For instance to avoid naming collisions.
   */
  String value() default "";

  /**
   * @return Boolean indicating whether the generated class should be public. If not, it will be
   * package-private.
   */
  boolean isPublic() default true;

  /**
   * @return Boolean indicating Whether the component implements a pure render function. If this is
   * true and the Component didn't change during an update of the ComponentTree measurements and
   * LayoutOutputs will be reused instead of being calculated again.
   */
  boolean isPureRender() default false;

  /**
   * @return List of event POJOs this component can dispatch. Used to generate event dispatch
   * methods.
   */
  Class<?>[] events() default {};

  /**
   * @return List of trigger POJOs this component can dispatch. Used to generate trigger creation
   * methods.
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
