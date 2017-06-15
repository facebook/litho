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
}
