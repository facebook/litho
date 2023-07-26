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

package com.facebook.rendercore.primitives

import android.content.Context
import com.facebook.rendercore.BaseResourcesScope
import com.facebook.rendercore.LayoutContext
import com.facebook.rendercore.ResourceCache
import com.facebook.rendercore.ResourceResolver

/**
 * The scope for the [Primitive] layout method. Provides access to [LayoutContext], the previous
 * layout data and utility methods that may help to compute the [PrimitiveLayoutResult].
 */
class LayoutScope
internal constructor(val layoutContext: LayoutContext<Any?>, val previousLayoutData: Any?) :
    BaseResourcesScope {
  override val androidContext: Context
    get() = layoutContext.androidContext

  override val resourceResolver: ResourceResolver
    get() =
        ResourceResolver(
            androidContext, ResourceCache.getLatest(androidContext.resources.configuration))
}
