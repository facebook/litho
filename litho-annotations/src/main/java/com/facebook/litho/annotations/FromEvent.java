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
 * Annotates a parameter to a component's event handler callback method indicating that it will be
 * supplied by the event object.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface FromEvent {

  /**
   * The base type for an annotated parameter as it's defined in the event object. This is useful if
   * an event callback wants to receive a more specific type for a param than what's declared in the
   * Event object. The generated code will implicitly cast parameters before it delegates to the
   * spec method.
   *
   * <p>Either the parameter type as declared or the type specified here must match the type in the
   * Event object.
   */
  Class baseClass() default Object.class;

}
