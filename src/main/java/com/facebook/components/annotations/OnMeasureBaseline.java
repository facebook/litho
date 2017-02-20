// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.annotations;

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
