/*
 * Copyright 2014-present Facebook, Inc.
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

package com.facebook.litho;

import androidx.annotation.AttrRes;
import androidx.annotation.StyleRes;

/**
 * Utility class to create a new {@link InternalNode} from an existing {@link Component}. This is
 * useful for components with child components as props.
 */
final class Layout {
  private Layout() {
  }

  /**
   * Create a {@link InternalNode} from an existing {@link Component}.
   *
   * @param c The context to create the layout within
   * @param component The component to render within this layout
   * @param defStyleAttr The id of the attribute to use for default style attributes
   * @param defStyleRes The id of the style to use for layout attributes
   * @return A layout builder
   */
  static InternalNode create(
      ComponentContext c,
      Component component,
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes) {
    if (component == null) {
      return ComponentContext.NULL_LAYOUT;
    }
    return c.newLayoutBuilder(component, defStyleAttr, defStyleRes);
  }

  /**
   * Create a {@link InternalNode} from an existing {@link Component}.
   *
   * @param c The context to create the layout within
   * @param component The component to render within this layout
   * @return A layout builder
   */
  static InternalNode create(ComponentContext c, Component component) {
    return create(c, component, 0, 0);
  }
}
