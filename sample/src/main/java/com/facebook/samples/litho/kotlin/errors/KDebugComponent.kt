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

import android.graphics.Typeface
import android.util.Log
import androidx.annotation.ColorInt
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.padding
import com.facebook.litho.dp
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.utils.StacktraceHelper
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.onClick

/**
 * Renders a throwable as a text with a title and provides a touch callback that logs the throwable
 * with WTF level.
 */
class KDebugComponent(private val message: String, private val throwable: Throwable) :
    KComponent() {

  private val TAG = "KDebugComponent"

  @ColorInt private val DARK_RED_FRAME = 0xffcd4928.toInt()
  @ColorInt private val LIGHT_RED_BACKGROUND = 0xfffcece9.toInt()
  @ColorInt private val LIGHT_GRAY_TEXT = 0xff606770.toInt()

  override fun ComponentScope.render(): Component? {
    Log.e(TAG, message, throwable)

    return Column(
        style =
            Style.onClick { onClick(message, throwable) }
                .padding(all = 1.dp)
                .backgroundColor(DARK_RED_FRAME)) {
      child(
          Text(
              style = Style.padding(all = 4.dp).backgroundColor(LIGHT_RED_BACKGROUND),
              text = message,
              textSize = 16.dp))
      child(
          Text(
              style = Style.padding(all = 4.dp).backgroundColor(LIGHT_RED_BACKGROUND),
              text = StacktraceHelper.formatStacktrace(throwable),
              textSize = 12.dp,
              textColor = LIGHT_GRAY_TEXT,
              typeface = Typeface.MONOSPACE))
    }
  }

  private fun onClick(message: String, throwable: Throwable) {
    Log.wtf(TAG, message, throwable)
  }
}
