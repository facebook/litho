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

import android.util.Log
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.rendercore.ErrorReporterDelegate
import com.facebook.rendercore.LogLevel
import java.lang.RuntimeException

open class DefaultComponentsReporter : ErrorReporterDelegate {

  override fun report(
      level: LogLevel,
      categoryKey: String,
      message: String,
      cause: Throwable?,
      samplingFrequency: Int,
      metadata: Map<String, Any>?
  ) {
    when (level) {
      LogLevel.WARNING ->
          applyOnInternalBuild { Log.w(CATEGORY_PREFIX + categoryKey, message, cause) }
      LogLevel.ERROR ->
          applyOnInternalBuild { Log.e(CATEGORY_PREFIX + categoryKey, message, cause) }
      LogLevel.FATAL -> {
        applyOnInternalBuild { Log.e(CATEGORY_PREFIX + categoryKey, message, cause) }
        throw RuntimeException(message)
      }
      else -> {
        // do nothing
      }
    }
  }

  @JvmOverloads
  fun emitMessage(
      level: ComponentsReporter.LogLevel,
      categoryKey: String,
      message: String,
      samplingFrequency: Int = /* take default*/ 0,
      metadata: Map<String, Any>? = null
  ) {
    report(ComponentsReporter.map(level), categoryKey, message, null, samplingFrequency, metadata)
  }

  private fun applyOnInternalBuild(block: () -> Unit) {
    if (ComponentsConfiguration.IS_INTERNAL_BUILD) {
      block()
    }
  }

  companion object {
    private const val CATEGORY_PREFIX = "Litho:"
  }
}
