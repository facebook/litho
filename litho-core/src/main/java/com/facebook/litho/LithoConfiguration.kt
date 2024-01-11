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

import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.Mode
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.rendercore.visibility.VisibilityBoundsTransformer

@DataClassGenerate(toString = Mode.OMIT, equalsHashCode = Mode.OMIT)
data class LithoConfiguration(
    @JvmField val componentsConfig: ComponentsConfiguration,
    @JvmField val areTransitionsEnabled: Boolean,
    @JvmField val isVisibilityProcessingEnabled: Boolean,
    @JvmField val incrementalMountEnabled: Boolean,
    @JvmField val errorEventHandler: ErrorEventHandler,
    @JvmField val logTag: String? = null,
    @JvmField val logger: ComponentsLogger? = null,
    @JvmField val renderUnitIdGenerator: RenderUnitIdGenerator?,
    @JvmField val visibilityBoundsTransformer: VisibilityBoundsTransformer?,
    @JvmField val debugEventListener: ComponentTreeDebugEventListener?,
) {

  val isSpecsDuplicateStateUpdateDetectionEnabled: Boolean
    get() = componentsConfig.specsApiStateUpdateDuplicateDetectionEnabled
}
