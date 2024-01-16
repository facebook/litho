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

import android.os.Build
import android.os.Trace
import com.facebook.litho.config.LithoDebugConfigurations
import com.facebook.rendercore.RenderCoreSystrace
import com.facebook.rendercore.Systracer
import com.facebook.rendercore.Systracer.ArgsBuilder

open class DefaultComponentsSystrace : Systracer {

  override fun beginSection(name: String) = applyIfTracing {
    val normalizedName =
        if (name.length > MAX_CHARACTERS_SECTION_NAME)
            "${name.substring(0, MAX_CHARACTERS_SECTION_NAME - 1)}…"
        else name
    Trace.beginSection(normalizedName)
  }

  override fun beginAsyncSection(name: String) {
    // no-op
  }

  override fun beginAsyncSection(name: String, cookie: Int) {
    // no-op
  }

  override fun beginSectionWithArgs(name: String): ArgsBuilder {
    beginSection(name)
    return RenderCoreSystrace.NO_OP_ARGS_BUILDER
  }

  override fun endSection() = applyIfTracing { Trace.endSection() }

  override fun endAsyncSection(name: String) {
    // no-op
  }

  override fun endAsyncSection(name: String, cookie: Int) {
    // no-op
  }

  override fun isTracing(): Boolean =
      LithoDebugConfigurations.isDebugModeEnabled &&
          Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 &&
          (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || Trace.isEnabled())

  private fun applyIfTracing(block: () -> Unit) {
    if (isTracing()) {
      block()
    }
  }

  companion object {
    /**
     * In android.os.Trace there is a limit to the section name (127 characters). If
     * [Trace#beginSection(String)] is called with a String bigger than 127, it will throw an
     * exception.
     *
     * We handle this case in our scenario and will trim it in case it is bigger than the valid
     * limit.
     *
     * @see [android.os.Trace](https://fburl.com/7bngmv5x)
     */
    private const val MAX_CHARACTERS_SECTION_NAME = 127
  }
}
