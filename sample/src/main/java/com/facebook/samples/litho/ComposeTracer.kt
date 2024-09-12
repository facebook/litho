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

package com.facebook.samples.litho

import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.InternalComposeTracingApi
import com.facebook.litho.DefaultComponentsSystrace

@OptIn(InternalComposeTracingApi::class)
object ComposeTracer {

  private val tracer = DefaultComponentsSystrace()

  private val composeTrace =
      object : CompositionTracer {
        override fun isTraceInProgress(): Boolean {
          return tracer.isTracing()
        }

        override fun traceEventEnd() {
          tracer.endSection()
        }

        override fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String) {
          tracer.beginSection(info)
        }
      }

  @JvmStatic
  fun initialize() {
    Composer.setTracer(composeTrace)
  }
}
