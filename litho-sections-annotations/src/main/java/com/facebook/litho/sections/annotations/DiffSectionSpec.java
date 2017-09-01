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
 * Classes with this annotation will generate the {@link com.facebook.litho.list.ChangeSet}
 * a list should display.
 *
 * <p>For example:
 * <pre>
 * {@code
 *
 * @DiffSectionSpec
 * public class MyDiffSectionSpec {
 *
 *   @OnDiff
 *   protected void onDiff(
 *     SectionContext c,
 *     ChangeSet changeSet,
 *     Diff<MyProp> prop) {
 *
 *     if(prop.getPrevious() == null) {
 *       changeSet.add(Change.insert(...));
 *     } else {
 *       changeSet.add(Change.update(...));
 *     }
 *   }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface DiffSectionSpec {
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
