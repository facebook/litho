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

import androidx.compose.runtime.Composable
import com.facebook.litho.annotations.Hook
import com.facebook.primitive.compose.ComposableWithDeps

/**
 * Creates and caches a [ComposableWithDeps] instance to be used with [ComposeComponent].
 *
 * @param deps The variables captured within the @Composable [content] lambda. Should contain any
 *   props or state that is used inside of [content] lambda.
 * @param content The @Composable lambda that contains Compose UI.
 */
@Hook
fun ComponentScope.useComposable(
    vararg deps: Any?,
    content: @Composable () -> Unit
): ComposableWithDeps {
  return useCached(deps) { ComposableWithDeps(deps, content) }
}
