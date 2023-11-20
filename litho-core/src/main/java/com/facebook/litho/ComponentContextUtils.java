/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.litho;

import android.content.Context;
import androidx.annotation.Nullable;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.rendercore.visibility.VisibilityBoundsTransformer;

public class ComponentContextUtils {

  public static LithoConfiguration buildDefaultLithoConfiguration(
      Context context,
      final @Nullable VisibilityBoundsTransformer transformer,
      @Nullable String logTag,
      @Nullable ComponentsLogger logger) {
    ComponentsLogger loggerToUse =
        logger != null ? logger : ComponentsConfiguration.componentsLogger;

    String logTagToUse = logTag;
    if (logTag == null && logger == null && loggerToUse != null) {
      logTagToUse = "global-components-logger";
    }

    return new LithoConfiguration(
        ComponentsConfiguration.defaultInstance,
        AnimationsDebug.areTransitionsEnabled(context),
        ComponentsConfiguration.overrideReconciliation != null
            ? ComponentsConfiguration.overrideReconciliation
            : true,
        true,
        false,
        null,
        !ComponentsConfiguration.isIncrementalMountGloballyDisabled,
        DefaultErrorEventHandler.INSTANCE,
        logTagToUse,
        loggerToUse,
        null,
        transformer,
        null);
  }
}
