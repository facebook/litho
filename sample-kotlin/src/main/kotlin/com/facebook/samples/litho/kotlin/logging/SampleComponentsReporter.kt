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
import com.facebook.litho.ComponentsLogger
import com.facebook.litho.ComponentsReporter

class SampleComponentsReporter : ComponentsReporter.Reporter {
    private val tag = "LITHOSAMPLE"

    override fun emitMessage(level: ComponentsReporter.LogLevel?, categoryKey: String?, message: String?) {
        when (level) {
            ComponentsLogger.LogLevel.WARNING -> {
                Log.w(tag, message)
            }
            ComponentsLogger.LogLevel.ERROR -> {
                Log.e(tag, message)
            }
        }
    }

    override fun emitMessage(
        level: ComponentsReporter.LogLevel?,
        categoryKey: String?,
        message: String?,
        samplingFrequency: Int
    ) {
        emitMessage(level, categoryKey, message)
    }

    override fun getKeyCollisionStackTraceKeywords() = emptySet<String>()

    override fun getKeyCollisionStackTraceBlacklist() = emptySet<String>()
}
