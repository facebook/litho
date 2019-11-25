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

package com.facebook.litho.intellij;

/** Contains Litho class names. */
public class LithoClassNames {
  private LithoClassNames() {}

  // litho-core module
  public static final String CLICK_EVENT_CLASS_NAME = "com.facebook.litho.ClickEvent";
  public static final String EVENT_HANDLER_CLASS_NAME = "com.facebook.litho.EventHandler";
  public static final String COMPONENT_CONTEXT_CLASS_NAME = "com.facebook.litho.ComponentContext";
  // litho-sections-core module
  public static final String SECTION_CONTEXT_CLASS_NAME =
      "com.facebook.litho.sections.SectionContext";
  public static final String SECTION_CLASS_NAME = "com.facebook.litho.sections.Section";

  /**
   * @param qualifiedName is one of the constants from the {@link LithoClassNames}
   * @return short name. For {@link LithoClassNames#CLICK_EVENT_CLASS_NAME} it would be ClickEvent.
   */
  public static String shortName(String qualifiedName) {
    int indexAfterlastDot = qualifiedName.lastIndexOf('.') + 1;
    if (indexAfterlastDot >= qualifiedName.length()) {
      return qualifiedName;
    }
    return qualifiedName.substring(indexAfterlastDot);
  }
}
