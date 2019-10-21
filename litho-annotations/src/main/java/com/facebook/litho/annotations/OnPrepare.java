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
 * A {@code MountSpec} can define a method annotated with {@code OnPrepare} to run code that is more
 * heavy and cannot be done during {@link OnMount} or {@link OnBind}. The method is called once
 * before the layout calculation is performed, and the framework can invoke it either on the UI
 * thread or on a background thread.
 *
 * <p>The annotated method has a void return type and will be passed the following arguments when
 * the framework invokes it:
 *
 * <p><em>Required:</em><br>
 *
 * <ol>
 *   <li>ComponentContext
 * </ol>
 *
 * <p><em>Optional annotated arguments:</em><br>
 *
 * <ul>
 *   <li>{@link Prop}
 *   <li>{@link TreeProp}
 *   <li>{@link InjectProp}
 *   <li>{@link State}
 *   <li>{@literal Output}
 * </ul>
 *
 * <p>The annotation processor will validate this and other invariants in the API at build time.
 *
 * <p>{@literal OnPrepare}-annotated methods can calculate values which are heavy to compute and
 * pass them as inter-stage props to other methods which are performance critical, such as {@link
 * OnMount}.
 *
 * <p>For example: <br>
 *
 * <pre><code>{@literal @MountSpec}
 * class ExampleMountSpec {
 *
 *  {@literal @OnPrepare}
 *   static void onPrepare(
 *       ComponentContext c,
 *      {@literal @Prop} String colorName,
 *      {@literal Output<Integer>} color) {
 *       color.set(Color.parseColor(colorName));
 *   }
 *
 *  {@literal @OnCreateMountContent}
 *   static ColorDrawable onCreateMountContent(
 *       ComponentContext c) {
 *     return new ColorDrawable();
 *   }
 *
 *  {@literal @OnMount}
 *   static void onMount(
 *       ComponentContext c,
 *       ColorDrawable colorDrawable,
 *      {@literal @FromPrepare} int color){
 *     colorDrawable.setColor(color);
 *   }
 * }</code></pre>
 *
 * @see OnMount
 * @see OnBind
 */
@Retention(RetentionPolicy.CLASS)
public @interface OnPrepare {}
