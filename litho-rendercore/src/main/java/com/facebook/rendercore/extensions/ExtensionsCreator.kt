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

package com.facebook.rendercore.extensions

import com.facebook.rendercore.RenderTreeHost
import com.facebook.rendercore.RootHost

/**
 * An interface client frameworks can use to provide the list of [RenderCoreExtension]s to be used
 * by the [RootHost] and [RenderTreeHost]s.
 */
fun interface ExtensionsCreator {
  fun create(): Array<RenderCoreExtension<*, *>>
}