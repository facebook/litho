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
 * A method that is annotated with this annotation will be used to compute the baseline of your
 * component. The default baseline of your component if this method is not implemented is the
 * computed height of your component. The baseline is the vertical location of your component to be
 * aligned when using .alignItems(BASELINE). <code>
 * {@literal @}LayoutSpec
 * public class MyComponentSpec {
 *
 *   {@literal @}OnMeasureBaseline
 *   int onMeasureBaseline(ComponentContext c, int width, int height) {
 *     return height / 2;
 *   }
 * }
 * </code>
 */
@Retention(RetentionPolicy.SOURCE)
public @interface OnMeasureBaseline {}
