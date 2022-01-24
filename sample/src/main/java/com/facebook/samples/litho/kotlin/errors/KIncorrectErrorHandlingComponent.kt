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

package com.facebook.samples.litho.kotlin.errors

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import java.lang.Exception

// start_example
class KIncorrectErrorHandlingComponent : KComponent() {

  override fun ComponentScope.render(): Component {
    // This implementation won't work - you need to implement error boundary with useErrorBoundary
    // hook instead.
    return try {
      // PossiblyCrashingSubTitleComponent can crash
      PossiblyCrashingSubTitleComponent.create(context).subtitle("example").build()
    } catch (e: Exception) {
      // error handling code
      KDebugComponent(message = "Error Component", throwable = e)
    }
  }
}
// end_example
