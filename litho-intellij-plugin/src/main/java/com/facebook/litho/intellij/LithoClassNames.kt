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

package com.facebook.litho.intellij

/** Contains Litho class names. */
object LithoClassNames {

  // litho-core module
  @JvmField val CLICK_EVENT_CLASS_NAME: String = "com.facebook.litho.ClickEvent"
  @JvmField val EVENT_HANDLER_CLASS_NAME: String = "com.facebook.litho.EventHandler"
  @JvmField val COMPONENT_CONTEXT_CLASS_NAME: String = "com.facebook.litho.ComponentContext"

  // litho-sections-core module
  @JvmField val SECTION_CONTEXT_CLASS_NAME: String = "com.facebook.litho.sections.SectionContext"
  @JvmField val SECTION_CLASS_NAME: String = "com.facebook.litho.sections.Section"

  /**
   * @param qualifiedName is one of the constants from the [LithoClassNames]
   * @return short name. For [LithoClassNames.CLICK_EVENT_CLASS_NAME] it would be ClickEvent.
   */
  @JvmStatic
  fun shortName(qualifiedName: String): String {
    val indexAfterLastDot = qualifiedName.lastIndexOf('.') + 1
    if (indexAfterLastDot >= qualifiedName.length) {
      return qualifiedName
    }
    return qualifiedName.substring(indexAfterLastDot)
  }
}
