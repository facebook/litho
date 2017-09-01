/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Classes with this annotation will define the different sections of a list.
 *
 * A List can be composed of other {@link GroupSectionSpec}s and/or of {@link DiffSectionSpec}.
 *
 * <p> A class that is annotated with {@link GroupSectionSpec} must implement a method with the
 * {@link OnCreateChildren} annotation.
 *
 * <p>For example:
 * <pre>
 * {@code
 *
 * @GroupSectionSpec
 * public class MySectionSpec {
 *
 *   @OnCreateChildren
 *   protected Children onCreateChildren(
 *     SectionContext c,
 *     @Prop MyProp prop) {
 *       return Children.create()
 *          .child(SomeSection.create(c))
 *          .build();
 *   }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface GroupSectionSpec {
  String value() default "";

  /**
   * List of event POJOs this component can dispatch. Used to generate event dispatch methods.
   */
  Class<?>[] events() default {};

  /**
   * @return Boolean indicating whether the generated class should be public. If not, it will be
   * package-private.
   */
  boolean isPublic() default true;
}
