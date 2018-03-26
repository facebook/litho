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
 * The method annotated with this annotation will be called to register working ranges for the
 * {@link LayoutSpec}.
 *
 * <p>For example
 *
 * <pre>
 * <code>
 *  {@literal @}LayoutSpec
 *   public class ComponentSpec {
 *
 *    {@literal @}OnRegisterRanges
 *     static void registerWorkingRanges(
 *         ComponentContext c,{@literal @}Prop WorkingRange workingRange) {
 *     }
 *   }
 * </code>
 * </pre>
 */
@Retention(RetentionPolicy.CLASS)
public @interface OnRegisterRanges {}
