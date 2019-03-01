/*
 * Copyright 2019-present Facebook, Inc.
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

import android.view.View;
import androidx.annotation.Nullable;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Holds a mapping of Views inside a Litho hierarchy to related debugging information. This is
 * needed as a way of passing data to Views inside a Section hierarchy without relying on the
 * specific type of view that is used. For example, while a ComponentRenderInfo will be rendered
 * inside a LithoView always, a ViewRenderInfo could be using any type of View. This is to avoid
 * passing debugging data through structures that could be used by developers, such as the view tag.
 */
public class RenderInfoDebugInfoRegistry {

  public static final String SONAR_SECTIONS_DEBUG_INFO_TAG = "SONAR_SECTIONS_DEBUG_INFO";

  private static @Nullable Map<View, Object> sViewToRenderInfo;

  public @Nullable static Object getRenderInfoSectionDebugInfo(View view) {
    if (sViewToRenderInfo == null) {
      return null;
    }

    return sViewToRenderInfo.get(view);
  }

  public static void setRenderInfoToViewMapping(View view, Object renderInfoSectionDebugInfo) {
    if (sViewToRenderInfo == null) {
      sViewToRenderInfo = new WeakHashMap<>();
    }

    sViewToRenderInfo.put(view, renderInfoSectionDebugInfo);
  }
}
