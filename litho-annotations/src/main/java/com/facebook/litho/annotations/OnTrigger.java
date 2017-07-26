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
 * Annotated function in the component will allow its parents to call it with an EventTrigger.
 *
 * For example
 * <pre>
 *   {@code
 *
 *   {@literal @}LayoutSpec
 *   public class ComponentSpec {
 *
 *     {@literal @}OnTrigger(YourEvent.class)
 *     static Object yourEventClick(ComponentContext c, {@literal @}FromTrigger YourObject obj) {
 *       return new Object();
 *     }
 *   }}
 * </pre>
 */
@Retention(RetentionPolicy.CLASS)
public @interface OnTrigger {
  Class<?> value();
}
