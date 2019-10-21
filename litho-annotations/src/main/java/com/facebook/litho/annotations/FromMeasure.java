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
 * Annotates that the argument's value should be set to the named inter-stage output from the {@link
 * OnMeasure} lifecycle method. The name and type of the argument must match with the inter-stage
 * output requested.
 *
 * <p><em><b>This can be used in:</b></em>
 *
 * <ul>
 *   <li>{@link OnBoundsDefined}
 *   <li>{@link OnMount}
 *   <li>{@link OnBind}
 *   <li>{@link OnUnbind}
 *   <li>{@link OnUnmount}
 *   <li>{@link OnPopulateAccessibilityNode}
 *   <li>{@link OnPopulateExtraAccessibilityNode}
 *   <li>{@link GetExtraAccessibilityNodeAt}
 *   <li>{@link GetExtraAccessibilityNodesCount}
 * </ul>
 *
 * <p>The annotation processor will validate this and other invariants in the API at build time.
 *
 * <p>For example: <br>
 *
 * <pre><code>{@literal @MountSpec}
 * public class SubtitleSpec {
 *
 *  {@literal @OnMeasure}
 *   static void onMeasure(
 *       ComponentContext componentContext,
 *       ComponentLayout layout,
 *       int widthSpec,
 *       int heightSpec,
 *       Size size,
 *      {@literal @Prop} String text,
 *       Output&lt;Layout&gt; measuredLayout) {
 *
 *     Layout textLayout = createTextLayout(text, widthSpec, heightSpec);
 *
 *     size.width = SizeSpec.resolveSize(widthSpec, textLayout.getWidth());
 *     size.height = SizeSpec.resolveSize(heightSpec, LayoutMeasureUtil.getHeight(textLayout));
 *
 *     if (size.width &lt; 0 || size.height &lt; 0) {
 *       size.width = Math.max(size.width, 0);
 *       size.height = Math.max(size.height, 0);
 *     }
 *
 *     measuredLayout.set(textLayout);  // Set the value of output.
 *   }
 *
 *  {@literal @OnMount}
 *   static void onMount(ComponentContext c,
 *     TextDrawable textDrawable,
 *    {@literal @Prop} String text,
 *     // Get measured layout from the output set in OnMeasure.
 *    {@literal @FromMeasure} Layout measuredLayout) {
 *
 *       textDrawable.mount(
 *         text,
 *         textLayout,
 *         0,
 *         null,
 *         Color.BLANK,
 *         Color.GREEN,
 *         null);
 *   }
 * }</code></pre>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface FromMeasure {}
