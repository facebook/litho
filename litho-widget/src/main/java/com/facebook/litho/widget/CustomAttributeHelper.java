/*
 * Copyright 2018-present Facebook, Inc.
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
package com.facebook.litho.widget;

import android.support.annotation.Nullable;

/** Allows to set and get custom attribute of type {@link T}. */
public abstract class CustomAttributeHelper<T> {

  /**
   * Adds custom attribute of type {@link T} to the renderInfo with the tag defined by the {@link
   * #getTag()} method.
   *
   * @param item Custom attribute that will be added to the renderInfo. Null value will be skipped
   * @param renderInfoBuilder RenderInfo to update.
   * @return Same {@link com.facebook.litho.widget.ComponentRenderInfo.Builder} passed as a param.
   */
  final ComponentRenderInfo.Builder addAttribute(
      @Nullable final T item, final ComponentRenderInfo.Builder renderInfoBuilder) {
    if (item == null) {
      return renderInfoBuilder;
    }
    return renderInfoBuilder.customAttribute(getTag(), item);
  }

  /**
   * @return Custom attribute of type {@link T} set to the renderInfo previously with the tag
   *     defined by the {@link #getTag()} method. Null if value is not present.
   */
  @Nullable
  final T getAttribute(@Nullable RenderInfo renderInfo) {
    if (renderInfo == null) {
      return null;
    }

    Object attribute = renderInfo.getCustomAttribute(getTag());
    return (T) attribute;
  }

  protected abstract String getTag();
}
