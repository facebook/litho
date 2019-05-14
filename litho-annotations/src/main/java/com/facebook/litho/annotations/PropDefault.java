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
 * {@code PropDefault} can be used for setting the default value of an optional {@link Prop} in a
 * {@link LayoutSpec} or {@link MountSpec}. The field must be a constant (i.e. static final) with
 * the same name and type as the {@code Prop}.
 *
 * <p><b>For example:</b> <br>
 *
 * <pre><code>{@literal @LayoutSpec}
 * class SomeSpec {
 *
 *  {@literal @PropDefault}
 *   static final String name = "John Doe";  // default value for name
 *
 *  {@literal @OnCreateLayout}
 *   static Component onCreateLayout(
 *     ComponentContext c,
 *    {@literal @Prop(optional = true)} String name) {
 *
 *     return Text.create(c)
 *       .text(title.getTitle())
 *       .textSizeSp(16)
 *       .marginDip(YogaEdge.BOTTOM, 4)
 *       .build();
 *   }
 * }</code></pre>
 */
@Retention(RetentionPolicy.CLASS)
public @interface PropDefault {

  /**
   * Declares that the default value must be a resource of the specified {@link ResType}.
   *
   * <p><b>For example:</b> <br>
   *
   * <pre><code>{@literal @LayoutSpec}
   * class SomeSpec {
   *
   *  {@literal @PropDefault(resType = ResType.String, resId = R.string.default_name)}
   *   static final String name = null;
   *
   * }</code></pre>
   *
   * @return A valid resource type.
   */
  ResType resType() default ResType.NONE;

  /**
   * Sets an Android resource as the default value of the {@link Prop}.
   *
   * @return An Android resource id.
   */
  int resId() default 0;
}
