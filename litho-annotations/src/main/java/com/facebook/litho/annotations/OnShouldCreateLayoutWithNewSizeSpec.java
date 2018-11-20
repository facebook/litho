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
 * This indicates that the annotated method will be called when the component's checks if it can use
 * the last cached layout returned from the Layout Spec with a new Size Spec. This is used in
 * conjunction with {@link OnCreateLayoutWithSizeSpec}. The annotated method must have the following
 * signature:
 *
 * <pre><code>
 *  static boolean onShouldCreateLayoutWithNewSizeSpec(
 *    ComponentContext context,
 *    int newWidthSpec,
 *    int newHeightSpec, ...)
 * </code></pre>
 *
 * <p>The annotated method should return {@code true} iff the Layout Spec should create a new layout
 * for this new size spec. If the method returns {@code false} then the Component will use the last
 * cached layout.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface OnShouldCreateLayoutWithNewSizeSpec {}
