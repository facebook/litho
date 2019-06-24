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
 * A method annotated with {@code OnMount} is called on a {@code MountSpec} component before the
 * component is attached to a hosting view. This will happen when the component is about to become
 * visible when incremental mount is enabled (it is enabled by default). The framework will always
 * invoke this method on the UI thread. This method is critical for performance as it is UI-bound;
 * expensive operations should not be performed here.
 *
 * <p>You can think of methods annotated with {@code OnMount} as the equivalent of the
 * RecyclerView.Adapter's bindViewHolder method. It will be called to bind the recycler Views or
 * Drawables to the correct data when they are about to be mounted on screen.
 *
 * <p>The annotated method has a void return type and will be passed the following arguments when
 * the framework invokes it:
 *
 * <p><em>Required:</em><br>
 *
 * <ol>
 *   <li>ComponentContext
 *   <li>{MountContent} - must be the same type as the return type of the method annotated with
 *       {@literal OnCreateMountContent}, which is either a View or a Drawable
 * </ol>
 *
 * <p><em>Optional annotated arguments:</em><br>
 *
 * <ul>
 *   <li>{@link Prop}
 *   <li>{@link TreeProp}
 *   <li>{@link InjectProp}
 *   <li>{@link State}
 *   <li>{@link FromPrepare}
 *   <li>{@link FromMeasure}
 * </ul>
 *
 * <p>The annotation processor will validate this and other invariants in the API at build time.
 *
 * <p>For example: <br>
 *
 * <pre><code>{@literal @MountSpec}
 * class ExampleMountSpec {
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
 *      {@literal @FromPrepare}int color){
 *     colorDrawable.setColor(color);
 *   }
 * }</code></pre>
 *
 * @see OnCreateMountContent
 * @see OnUnmount
 */
@Retention(RetentionPolicy.CLASS)
public @interface OnMount {}
