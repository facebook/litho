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
 * This indicates that the annotated method will be called when the component's checks if it can use
 * the last cached layout returned from the Layout Spec with a new Size Spec. This is used in
 * conjunction with {@link OnCreateLayoutWithSizeSpec}. The annotated method must have the following
 * signature:
 *
 * <pre><code>
 *  static boolean onShouldCreateLayoutWithNewSizeSpec(
 *    ComponentContext c,
 *    int newWidthSpec,
 *    int newHeightSpec, ...)
 * </code></pre>
 *
 * <p>The annotated method should return {@code true} iff the Layout Spec should create a new layout
 * for this new size spec. If the method returns {@code false} then the Component will use the last
 * cached layout.
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
 *
 *  {@literal @OnShouldCreateLayoutWithNewSizeSpec}
 *   static boolean onShouldCreateLayoutWithNewSizeSpec(
 *       ComponentContext c,
 *       int newWidthSpec,
 *       int newHeightSpec,
 *      {@literal @FromPreviousCreateLayout} int textWidth,
 *      {@literal @FromPreviousCreateLayout} boolean didItFit) {
 *
 *     final int newLayoutWidth = SizeSpec.getSize(newWidthSpec);
 *     final boolean doesItStillFit = (textWidth &lt;= newLayoutWidth);
 *
 *     // false if it still fits or if still doesn't fit
 *     return doesItStillFit ^ didItFit;
 *   }
 * }</code></pre>
 *
 * <img alt="On Should Create Layout With New Size Spec flow chart"
 * src="https://fblitho.com/static/images/flow-chart-v0.22.1-layout-with-size-spec.svg">
 *
 * @see FromPreviousCreateLayout
 * @see OnCreateLayoutWithSizeSpec
 */
@Retention(RetentionPolicy.SOURCE)
public @interface OnShouldCreateLayoutWithNewSizeSpec {}
