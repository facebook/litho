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

package com.facebook.litho

import android.content.Context
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.rendercore.visibility.VisibilityBoundsTransformer

object ComponentContextUtils {
  @JvmStatic
  fun buildDefaultLithoConfiguration(
      context: Context,
      transformer: VisibilityBoundsTransformer? = null,
      logTag: String? = null,
      logger: ComponentsLogger? = null,
  ): LithoConfiguration {
    val loggerToUse = logger ?: ComponentsConfiguration.componentsLogger
    var logTagToUse = logTag
    if (logTag == null && logger == null && loggerToUse != null) {
      logTagToUse = "global-components-logger"
    }
    return LithoConfiguration(
        componentsConfig = ComponentsConfiguration.defaultInstance,
        areTransitionsEnabled = AnimationsDebug.areTransitionsEnabled(context),
        isReconciliationEnabled = ComponentsConfiguration.isReconciliationEnabled,
        isVisibilityProcessingEnabled = true,
        preallocationPerMountContentEnabled = false,
        mountContentPreallocationHandler = null,
        incrementalMountEnabled = !ComponentsConfiguration.isIncrementalMountGloballyDisabled,
        errorEventHandler = DefaultErrorEventHandler.INSTANCE,
        logTag = logTagToUse,
        logger = loggerToUse,
        renderUnitIdGenerator = null,
        visibilityBoundsTransformer = transformer,
        debugEventListener = null)
  }
}