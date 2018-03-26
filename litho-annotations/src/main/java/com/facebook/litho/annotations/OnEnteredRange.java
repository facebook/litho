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
 * Annotated function in the component will allow it be called when entering a working range. To
 * enable the working range, you also have to register it in{@literal @}OnRegisterRanges method.
 *
 * <p>For example
 *
 * <pre>
 * <code>
 *  {@literal @}LayoutSpec
 *   public class ComponentSpec {
 *
 *    {@literal @}OnEnteredRange(name = "prefetch")
 *     static void yourEnterWorkingRangeMethod(ComponentContext c,{@literal @}Prop YourObject obj) {
 *     }
 *
 *    {@literal @}OnRegisterRanges
 *     static void yourRegisterMethod(
 *         ComponentContext c,{@literal @}Prop WorkingRange yourWorkingRange) {
 *       Component.registerPrefetchWorkingRange(c, yourWorkingRange);
 *     }
 *   }
 * </code>
 * </pre>
 */
@Retention(RetentionPolicy.CLASS)
public @interface OnEnteredRange {
  String name();
}
