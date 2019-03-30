/*
 * Copyright 2004-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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

  public static final String PROP_DEFAULT_CLASS_NAME = "com.facebook.litho.annotations.PropDefault";
  public static final String PROP_CLASS_NAME = "com.facebook.litho.annotations.Prop";
  public static final String STATE_CLASS_NAME = "com.facebook.litho.annotations.State";

  public static final String COMPONENT_CONTEXT_CLASS_NAME = "com.facebook.litho.ComponentContext";

  static final String EVENT_ANNOTATION_NAME = "com.facebook.litho.annotations.Event";
  public static final String FROM_EVENT_ANNOTATION_NAME =
      "com.facebook.litho.annotations.FromEvent";
  public static final String ON_EVENT_ANNOTATION_NAME = "com.facebook.litho.annotations.OnEvent";
  public static final String PARAM_ANNOTATION_NAME = "com.facebook.litho.annotations.Param";
  // TODO T39429594: investigate if we could use litho ClassNames instead of this class without
  // adding another dependency to the bundle

  /**
   * @param qualifiedName is one of the constants from the {@link LithoClassNames}
   * @return short name. For {@link LithoClassNames#EVENT_ANNOTATION_NAME} it would be Event.
   */
  public static String shortName(String qualifiedName) {
    int indexAfterlastDot = qualifiedName.lastIndexOf('.') + 1;
    if (indexAfterlastDot >= qualifiedName.length()) {
      return qualifiedName;
    }
    return qualifiedName.substring(indexAfterlastDot);
  }
}
