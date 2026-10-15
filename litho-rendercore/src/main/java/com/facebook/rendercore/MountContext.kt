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

package com.facebook.rendercore

import android.content.Context
import com.meta.systrace.Systracer

/**
 * A MountContext is a context object that is passed to the [RenderUnit] when interacting with
 * binders. It's a container for the parameters required to execute a successful
 * mount/unmount/attach/detach/update operation as required.
 *
 * @param androidContext The Android [Context].
 * @param tracer The [com.meta.systrace.Systracer] that may be used to trace code execution.
 * @param binderObserver The [BinderObserver] that can observe the bind and unbind operations of a
 *   binder.
 * @see RenderUnit
 */
class MountContext
internal constructor(
    val androidContext: Context,
    val tracer: Systracer,
    val binderObserver: BinderObserver? = null,
    reader: ContinuationValueReader = ContinuationValueReader { null },
) {
  internal val continuationContext: MountContinuationContext = MountContinuationContext(reader)
}

/**
 * Reads continuation values produced by the layout phase. A [MountState] installs a reader at
 * construction time; the reader is consulted by [useContinuation] every time a continuation runs.
 * Integrations supply their own reader. The default reader returns `null` for every key.
 */
fun interface ContinuationValueReader {
  fun readValue(key: Any): Any?

  fun readVersion(key: Any): Long = UNKNOWN_CONTINUATION_VALUE_VERSION
}

const val UNKNOWN_CONTINUATION_VALUE_VERSION: Long = -1L
