// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.annotations;

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
 * {@code
 *
 * @LayoutSpec
 * public class MyComponentSpec {
 *
 *   @OnCreateLayout
 *   protected ComponentLayout onCreateLayout(LayoutContext c, @Prop MyProp prop) {
 *       return Container.create(c)
 *           .direction(ROW)
 *           .alignItems(FLEX_START)
 *           .child(someChild1)
 *           .child(someChild2)
 *           .build();
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface LayoutSpec {
  String value() default "";

  /**
   * Whether the generated class should be public. If not, it will be package-private.
   */
  boolean isPublic() default true;

  /**
   * Whether the component implements a pure render function. If this is true and the Component
   * didn't change during an update of the ComponentTree measurements and LayoutOutputs will be
   * reused instead of being calculated again.
   */
  boolean isPureRender() default false;

  /**
   * List of event POJOs this component can dispatch. Used to generate event dispatch methods.
   */
  Class<?>[] events() default {};
}
