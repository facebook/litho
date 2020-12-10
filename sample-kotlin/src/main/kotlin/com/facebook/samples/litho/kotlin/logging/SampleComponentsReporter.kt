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

package com.facebook.samples.litho.kotlin.logging

import android.util.Log
import com.facebook.rendercore.AbstractErrorReporter;
import com.facebook.rendercore.LogLevel;

class SampleComponentsReporter : AbstractErrorReporter() {
  private val tag = "LITHOSAMPLE"

  override fun report(
      level: LogLevel,
      categoryKey: String,
      message: String,
      cause: Throwable?,
      samplingFrequency: Int,
      metadata: Map<String, Any>?
  ) {
    when (level) {
      LogLevel.WARNING -> {
        Log.w(tag, message)
      }
      LogLevel.ERROR -> {
        Log.e(tag, message)
      }
      LogLevel.FATAL -> {
        Log.wtf(tag, message)
        throw RuntimeException(message)
      }
    }
  }
}
