/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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
 * Deprecated: Please avoid using this annotation and it will be not supported in the future release
 *
 * <p>The method annotated with this annotation will be called to register working ranges for the
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
@Deprecated
@Retention(RetentionPolicy.CLASS)
public @interface OnRegisterRanges {}
