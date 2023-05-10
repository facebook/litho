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

import org.hamcrest.BaseMatcher
import org.hamcrest.Description

class ThrowableMatcher
private constructor(
    private val classMatcher: Class<out Throwable>,
    private val messageMatcher: String?
) : BaseMatcher<Throwable?>() {

  override fun matches(item: Any): Boolean =
      classMatcher.isInstance(item) &&
          (messageMatcher == null ||
              (item as? Throwable)?.message?.contains(messageMatcher) ?: false)

  override fun describeTo(description: Description) {
    description.appendText("an instance of $classMatcher")
    if (messageMatcher != null) {
      description.appendText(" with message containing \"$messageMatcher\"")
    }
  }

  companion object {
    @JvmStatic
    fun forClass(classMatcher: Class<out Throwable>): ThrowableMatcher =
        ThrowableMatcher(classMatcher, null)

    @JvmStatic
    fun forClassWithMessage(
        classMatcher: Class<out Throwable>,
        messageSubstring: String?
    ): ThrowableMatcher = ThrowableMatcher(classMatcher, messageSubstring)
  }
}
