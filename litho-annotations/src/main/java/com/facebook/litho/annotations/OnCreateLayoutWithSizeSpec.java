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
 * {@code OnCreateLayoutWithSizeSpec} is the same as {@link OnCreateLayout} but the layout can be
 * resolved with a specified width and height. Use this instead of {@code OnCreateLayout} if the
 * {@link LayoutSpec} returns a layout tree that depends on its own and/or its children's size.
 *
 * <p>For example, a spec might use Component {@code A} if that fits within the specified width or
 * use Component {@code B} instead.
 *
 * <p>The annotated method must return a Component and can receive the following arguments.
 *
 * <p><em>Required:</em><br>
 *
 * <ol>
 *   <li>ComponentContext
 *   <li>int (width spec)
 *   <li>int (height spec)
 * </ol>
 *
 * <p><em>Optional and annotated arguments:</em><br>
 *
 * <ul>
 *   <li>{@link Prop}
 *   <li>{@link TreeProp}
 *   <li>{@link InjectProp}
 *   <li>{@link State}
 *   <li>{@code Output}
 * </ul>
 *
 * <p>The annotation processor will validate this and other invariants in the API at build time. The
 * optional and annotated arguments are inputs which should be used to create the layout of the
 * spec.
 *
 * <p>For example: <br>
 *
 * <pre><code>{@literal @LayoutSpec}
 * class MyComponentSpec {
 *
 *  {@literal @OnCreateLayoutWithSizeSpec}
 *   static Component onCreateLayoutWithSizeSpec(
 *       ComponentContext c,
 *       int widthSpec,
 *       int heightSpec) {
 *
 *     final Component textComponent =
 *         Text.create(c).textSizeSp(16).text("Some text to measure.").build();
 *
 *     // UNSPECIFIED sizeSpecs will measure the text as being one line only,
 *     // having unlimited width.
 *     final Size textOutputSize = new Size();
 *     textComponent.measure(
 *         c,
 *         SizeSpec.makeSizeSpec(0, UNSPECIFIED),
 *         SizeSpec.makeSizeSpec(0, UNSPECIFIED),
 *         textOutputSize);
 *
 *     // Small component to use in case textComponent does not fit within the current layout.
 *     final Component imageComponent = Image.create(c).drawableRes(R.drawable.ic_launcher).build();
 *
 *     // Assuming SizeSpec.getMode(widthSpec) == EXACTLY or AT_MOST.
 *     final int layoutWidth = SizeSpec.getSize(widthSpec);
 *     final boolean textFits = (textOutputSize.width &lt;= layoutWidth);
 *
 *     return textFits ? textComponent : imageComponent;
 *   }
 * }</code></pre>
 *
 * @see OnCreateLayout
 */
@Retention(RetentionPolicy.CLASS)
public @interface OnCreateLayoutWithSizeSpec {}
