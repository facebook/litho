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
import java.lang.Exception

/**
 * Registers a callback to perform error handling for exceptions that might happen in child
 * components down in the tree.
 *
 * The hook parameter will receive all exceptions that are raised in the lifecycle methods of
 * components sitting underneath the error boundary in the tree, regardless of whether those are
 * other KComponents or Litho Specs.
 *
 * The KComponent can leverage the useState hook to update the state with the exception that was
 * caught, and trigger a render pass with the new state value in order to replace the crashing
 * component with an error component, or not display it at all.
 */
@Hook
fun ComponentScope.useErrorBoundary(onError: (exception: Exception) -> Unit) {
  val errorEventHandler = eventHandler<ErrorEvent> { onError(it.exception) }
  context.scopedComponentInfo.setErrorEventHandlerDuringRender(errorEventHandler)
}
