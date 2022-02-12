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

import com.facebook.litho.annotations.Hook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Launches a coroutine with [onLaunch] whenever [keys] change. The coroutine will be canceled when
 * the component is detached, or if the coroutine will be relaunched.
 *
 * The [CoroutineScope] for this coroutine must be specified through a TreeProp.
 */
@Hook
fun ComponentScope.useCoroutine(vararg keys: Any?, onLaunch: suspend CoroutineScope.() -> Unit) {
  useEffect(*keys) {
    val job = context.componentTree.componentTreeScope.launch { onLaunch() }
    onCleanup { job.cancel() }
  }
}
