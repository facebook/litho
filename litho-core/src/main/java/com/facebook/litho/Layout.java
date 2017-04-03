/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.support.annotation.AttrRes;
import android.support.annotation.StyleRes;

/**
 * Utility class to create a new {@link ComponentLayout.Builder} from an existing {@link Component}.
 * This is useful for components with child components as props.
 */
public final class Layout {
  private Layout() {
  }

  /**
   * Create a {@link ComponentLayout.Builder} from an existing {@link Component}.
   *
   * @param c The context to create the layout within
   * @param component The component to render within this layout
   * @param defStyleAttr The id of the attribute to use for default style attributes
   * @param defStyleRes The id of the style to use for layout attributes
   * @return A layout builder
   */
  public static ComponentLayout.Builder create(
      ComponentContext c,
      Component<?> component,
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes) {
    return c.newLayoutBuilder(component, defStyleAttr, defStyleRes);
  }

  /**
   * Create a {@link ComponentLayout.Builder} from an existing {@link Component}.
   *
   * @param c The context to create the layout within
   * @param component The component to render within this layout
   * @return A layout builder
   */
  public static ComponentLayout.Builder create(ComponentContext c, Component<?> component) {
    return create(c, component, 0, 0);
  }
}
