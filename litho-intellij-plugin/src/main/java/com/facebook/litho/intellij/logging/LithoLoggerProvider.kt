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

package com.facebook.litho.intellij.logging

import com.facebook.litho.intellij.extensions.EventLogger
import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.Extensions
import java.util.Arrays
import java.util.concurrent.Executor

/**
 * Provides logger to track common user flow events: completion action usage, dialog opening, etc.
 */
object LithoLoggerProvider {

  @JvmStatic
  fun getEventLogger(): EventLogger {
    return LithoEventLogger.INSTANCE
  }

  internal class LithoEventLogger
  @VisibleForTesting
  internal constructor(private val loggers: Array<EventLogger>, private val executor: Executor) :
      EventLogger {

    internal constructor() : this(Extensions.getExtensions(EP_NAME))

    @VisibleForTesting
    internal constructor(
        loggers: Array<EventLogger>,
    ) : this(
        loggers,
        Executor { action -> ApplicationManager.getApplication().executeOnPooledThread(action) },
    )

    override fun log(event: String?, metadata: Map<String, String>) {
      executor.execute {
        Arrays.stream(loggers).forEach { eventLogger -> eventLogger.log(event, metadata) }
      }
    }

    companion object {
      @JvmField
      val EP_NAME: ExtensionPointName<EventLogger> =
          ExtensionPointName.create("com.facebook.litho.intellij.eventLogger")

      @JvmField val INSTANCE: EventLogger = LithoEventLogger()
    }
  }
}
