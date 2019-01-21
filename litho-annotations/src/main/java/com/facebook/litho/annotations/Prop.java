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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotates a parameter to a component's spec method indicating that it will be supplied as a prop
 * for this component.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Prop {

  /**
   * Whether this prop can be omitted by the caller to the component, making it take its default
   * value.
   *
   * If a prop is declared optional, its default value will be the standard default initialization
   * value according to the Java standard (e.g. 0, false, null). If a constant annotated  with
   * {@link PropDefault} and named the same as the prop is in the class it will override the
   * default value.
   */
  boolean optional() default false;

  /**
   * Marks this prop as one that corresponding to a specific Android resource type, and therefore
   * generates various helper methods to initialize it.
   *
   * For example, a {@link CharSequence} prop named "title" may be marked as {@link ResType#STRING}.
   * This will make the component have not only method "title(CharSequence)" but also various
   * methods that enable initializing the prop from a resource or attribute:
   * {@code
   * titleRes(@StringRes int resId)
   * titleRes(@StringRes int resId, Object... formatArgs)
   * titleAttr(@AttrRes int attrResId, @StringRes int defResId)
   * titleAttr(@AttrRes int attrResId)
   * }
   */
  ResType resType() default ResType.NONE;

  String docString() default "";

  /**
   * Marks this prop as one supporting a variable number of arguments, and therefore adds methods
   * to make it easier to build lists of this argument.
   *
   * For example, having {@code @Prop(@varArg="name") List<CharSequence> names} would generate
   * an {@code name} method which can be called multiple times to add a set of names. These props
   * should be a parameterized list with a resource type of {@code resType = ResType.NONE}.
   */
  String varArg() default "";

  /** Whether this prop has the same name as a CommonProp. */
  boolean isCommonProp() default false;

  /**
   * This may only be set to true if isCommonProp is also set to true. If true, then the common prop
   * behavior of this prop will be overridden. If false, then the common prop will apply at the
   * framework level as normal as well as any behavior that the user defines within the spec.
   */
  boolean overrideCommonPropBehavior() default false;
}
