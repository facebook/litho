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
 * A class that is annotated with this annotation will be used to create a component that renders
 * something, in the form of either a Drawable or a View.
 *
 * <p>A class that is annotated with {@link MountSpec} must implement a method with the {@link
 * OnCreateMountContent} annotation. It may also implement methods with the following annotations:
 *
 * <ul>
 *   <li>{@link OnLoadStyle}
 *   <li>{@link OnEvent}
 *   <li>{@link OnPrepare}
 *   <li>{@link OnMeasure}
 *   <li>{@link OnBoundsDefined}
 *   <li>{@link OnMount}
 *   <li>{@link OnBind}
 *   <li>{@link OnUnbind}
 *   <li>{@link OnUnmount}
 *   <li>{@link ShouldUpdate}
 * </ul>
 *
 * <p>If you wish to create a component that is a composition of other components, then use {@link
 * LayoutSpec} instead.
 *
 * <p>For example:
 *
 * <pre><code>{@literal @}MountSpec
 * public class MyComponentSpec {
 *
 *  {@literal @}OnCreateMountContent
 *   MyDrawable onCreateMountContent(Context c) {
 *     return new MyDrawable(c);
 *   }
 *
 *  {@literal @}OnMount
 *   void onMount(
 *       ComponentContext context,
 *       MyDrawable myDrawable,
 *      {@literal @}Prop MyProp prop) {
 *     myDrawable.setMyProp(prop);
 *   }
 * }</code></pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MountSpec {
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
   * @return Boolean indicating whether the component implements a pure render function. If this is
   *     true and the Component didn't change during an update of the ComponentTree measurements and
   *     LayoutOutputs will be reused instead of being calculated again.
   */
  boolean isPureRender() default false;

  /**
   * @return boolean indicating whether this mount spec has child LithoViews. If this is true then
   *     we need to ensure that these child views are correctly incrementally mounted.
   */
  boolean hasChildLithoViews() default false;

  /**
   * @return List of event POJOs this component can dispatch. Used to generate event dispatch
   *     methods.
   */
  Class<?>[] events() default {};

  /**
   * @return The max number of preallocated Mount objects we want to keep in the pools for this type
   *     of MountSpec
   */
  int poolSize() default 3;

  /** @return whether the component generated from this MountSpec will be preallocated. */
  boolean canPreallocate() default false;

  /**
   * @return List of trigger POJOs this component can dispatch. Used to generate trigger creation
   *     methods.
   */
  Class<?>[] triggers() default {};
}
