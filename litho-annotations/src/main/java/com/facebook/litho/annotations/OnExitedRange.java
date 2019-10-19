/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
 * Annotated function in the component will allow it be called when exiting a working range. To
 * enable the working range, you also have to register it in{@literal @}OnRegisterRanges method.
 *
 * <p>For example
 *
 * <pre>
 * <code>
 *  {@literal @}LayoutSpec
 *   public class ComponentSpec {
 *
 *    {@literal @}OnExitedRange(name = "prefetch")
 *     static void yourExitWorkingRangeMethod(ComponentContext c,{@literal @}Prop YourObject obj) {
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
public @interface OnExitedRange {
  String name();
}
