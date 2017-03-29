/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A method that is annotated with this annotation will be used to compute the baseline of your
 * component. The default baseline of your component if this method is not implemented is the
 * computed height of your component. The baseline is the vertical location of your component
 * to be aligned when using .alignItems(BASELINE).
 *
 * <pre>
 * {@code
 * @LayoutSpec
 * public class MyComponentSpec {
 *
 *   @OnMeasureBaseline
 *   protected int onMeasureBaseline(LayoutContext c, int width, int height) {
 *       return height / 2;
 *   }
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.SOURCE)
public @interface OnMeasureBaseline {
}
