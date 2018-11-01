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
package com.facebook.litho.sections.widget;

import android.support.annotation.Nullable;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.ComponentTreeHolder;
import com.facebook.litho.widget.RenderInfo;

/**
 * Allows to set and get the custom attribute to the items in a {@link RecyclerCollectionComponent}
 * of type T.
 */
public abstract class CustomAttributeHelper<T> {
  public final ComponentRenderInfo.Builder addAttribute(
      final T item, final ComponentRenderInfo.Builder renderInfoBuilder) {
    return renderInfoBuilder.customAttribute(getTag(), item);
  }

  public final @Nullable T getAttribute(@Nullable ComponentTreeHolder componentTreeHolder) {
    if (componentTreeHolder == null) {
      return null;
    }

    RenderInfo renderInfo = componentTreeHolder.getRenderInfo();
    if (renderInfo == null) {
      return null;
    }

    Object attribute = renderInfo.getCustomAttribute(getTag());
    return (T) attribute;
  }

  protected abstract String getTag();
}
