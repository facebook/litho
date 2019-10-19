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
 * This annotation is used in conjugation with {@link LayoutSpec}. The framework calls the method
 * annotated with {@code OnCreateLayout} in the {@link LayoutSpec} to resolve a layout. The
 * framework can call {@code onCreateLayout} from any thread and as many times as required. This
 * method should not create side effects. The annotated method must return a Component and can
 * receive the following arguments.
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
 * </ul>
 *
 * <p>The annotation processor will validate this and other invariants in the API at build time. The
 * annotated arguments are inputs which should be used to create the layout of the spec.
 *
 * <p>For example: <br>
 *
 * <pre><code>{@literal @LayoutSpec}
 * public class HeaderSpec {
 *
 *  {@literal @OnCreateLayout}
 *   static Component onCreateLayout(
 *     ComponentContext c,
 *    {@literal @Prop} String title,
 *    {@literal @Prop} String subtitle,
 *    {@literal @Prop} String imageUrl,
 *    {@literal @State} boolean isSelected) {
 *
 *     return Column.create(c)
 *       .paddingDip(YogaEdge.ALL, 8)
 *       .backgroundColor(isSelected ? Color.WHITE : Color.GREEN)
 *       .child(
 *         Image.create(c)
 *           .url(imageUrl)
 *           .marginDip(YogaEdge.BOTTOM, 4)
 *       )
 *       .child(
 *         Text.create(c)
 *           .text(title)
 *           .textSizeSp(16)
 *           .marginDip(YogaEdge.BOTTOM, 4)
 *       )
 *       .child(
 *         Text.create(c)
 *           .text(subtitle)
 *           .textSizeSp(12)
 *       )
 *       .build();
 *   }
 * }</code></pre>
 *
 * <p>Notice how {@code imageUrl}, {@code title}, {@code subtitle} and {@code isSelected} are used
 * to set properties on different components within the layout. In the example above, the layout
 * tree has a root container with three children stacked vertically ({@code Column.create(c)} ). The
 * first child is an Image component ({@code Image.create(c)}) which renders and image from a URL
 * (similar to Android's {@code ImageView}). The second and third children are Text components
 * ({@code Text.create(c)}) which renders a text equivalent to the Android {@code TextView}.
 *
 * @see OnCreateLayoutWithSizeSpec
 * @see OnUpdateState
 * @see OnEvent
 */
@Retention(RetentionPolicy.CLASS)
public @interface OnCreateLayout {}
