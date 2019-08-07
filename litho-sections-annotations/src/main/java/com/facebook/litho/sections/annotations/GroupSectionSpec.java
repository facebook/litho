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

package com.facebook.litho.sections.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Classes with this annotation will define the different sections of a list. A List can be composed
 * of other {@link GroupSectionSpec}s and/or of {@link DiffSectionSpec}.
 *
 * <p>A class that is annotated with {@link GroupSectionSpec} must implement a method with the
 * {@link OnCreateChildren} annotation.
 *
 * <p>You can use {@link OnViewportChanged} to get notified about the current state of the
 * scrollable viewport (e.g. first and last visible element position).
 *
 * <p>Example:
 *
 * <pre>
 * {@literal @}GroupSectionSpec
 *  public class MySectionSpec {
 *
 *   {@literal @}OnCreateChildren
 *    protected Children onCreateChildren(
 *      SectionContext c,
 *     {@literal @}Prop MyProp prop) {
 *        return Children.create()
 *           .child(SomeSection.create(c))
 *           .build();
 *    }
 * }
 * </pre>
 *
 * @see DiffSectionSpec
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface GroupSectionSpec {
  String value() default "";

  /** List of event POJOs this component can dispatch. Used to generate event dispatch methods. */
  Class<?>[] events() default {};

  /**
   * @return Boolean indicating whether the generated class should be public. If not, it will be
   *     package-private.
   */
  boolean isPublic() default true;
}
