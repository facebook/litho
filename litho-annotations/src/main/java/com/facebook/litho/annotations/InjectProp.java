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
 * Annotates a parameter of a component's spec method indicating that the parameter will be
 * automatically supplied via dependency injection. In order to use this annotation,
 * DependencyInjectionHelper.generateInjectedMembers() needs to be properly implemented.
 */
@Retention(RetentionPolicy.CLASS)
public @interface InjectProp {
  /**
   * @return Boolean indicating whether the injection should be lazy.
   */
  boolean isLazy() default true;
}
