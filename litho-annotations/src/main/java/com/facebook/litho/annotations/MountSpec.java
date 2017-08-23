/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A class that is annotated with this annotation will be used to create a component that renders
 * something, in the form of either a Drawable or a View.
 * <p>A class that is annotated with {@link MountSpec} must implement a method with the
 * {@link OnCreateMountContent} annotation. It may also implement methods with the following
 * annotations:
 * - {@link OnLoadStyle}
 * - {@link OnEvent}
 * - {@link OnPrepare}
 * - {@link OnMeasure}
 * - {@link OnBoundsDefined}
 * - {@link OnMount}
 * - {@link OnBind}
 * - {@link OnUnbind}
 * - {@link OnUnmount}
 * <p>If you wish to create a component that is a composition of other components, then use
 * {@link LayoutSpec} instead.
 * <p>For example:
 * <code>
 *
 * {@literal @}MountSpec
 * public class MyComponentSpec {
 *
 *   {@literal @}OnCreateMountContent
 *   MyDrawable onCreateMountContent(ComponentContext c) {
 *     return new MyDrawable(c);
 *   }
 *
 *   {@literal @}OnMount
 *   void onMount(
 *       ComponentContext context,
 *       MyDrawable myDrawable,
 *       {@literal @}Prop MyProp prop) {
 *     myDrawable.setMyProp(prop);
 *   }
 * }
 * </code>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface MountSpec {
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
   * @return Boolean indicating whether the component implements a pure render function. If this is
   * true and the Component didn't change during an update of the ComponentTree measurements and
   * LayoutOutputs will be reused instead of being calculated again.
   */
  boolean isPureRender() default false;

  /**
   * @return Boolean indicating whether this mount spec supports incremental mount. This is only
   * applicable to MountSpecs which mount a View which is or contains a LithoView.
   */
  boolean canMountIncrementally() default false;

  /**
   * @return List of event POJOs this component can dispatch. Used to generate event dispatch
   * methods.
   */
  Class<?>[] events() default {};

  /**
   * @return Boolean indicating whether this drawable mount spec should cache its drawing in a
   * display list.
   */
  boolean shouldUseDisplayList() default false;

  /**
   * @return The max number of preallocated Mount objects we want to keep in the pools for this type
   * of MountSpec
   */
  int poolSize() default 15;

  /**
   * @return List of trigger POJOs this component can dispatch. Used to generate trigger creation
   * methods.
   */
  Class<?>[] triggers() default {};
}
