/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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
 */
@Hook
fun ComponentScope.useErrorBoundary(onError: (exception: Exception) -> Unit) {
  val errorEventHandler = eventHandler<ErrorEvent> { onError(it.exception) }
  if (context.useStatelessComponent()) {
    context.scopedComponentInfo.setErrorEventHandlerDuringRender(errorEventHandler)
  } else {
    context.componentScope.setErrorEventHandlerDuringRender(errorEventHandler)
  }
}
