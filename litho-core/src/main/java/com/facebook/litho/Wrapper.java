/*
 * Copyright (c) 2017-present, Facebook, Inc.
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
 * Utility class for wrapping an existing {@link Component} to create a new {@link
 * ComponentLayout.Builder}. This is useful for components with child components as props.
 */
public final class Wrapper {
  private Wrapper() {}

  /**
   * Create a {@link ComponentLayout.Builder} from an existing {@link Component}.
   *
   * @param c The context to create the layout within
   * @param component The component to render within this layout
   * @param defStyleAttr The id of the attribute to use for default style attributes
   * @param defStyleRes The id of the style to use for layout attributes
   * @return A layout builder
   */
  private static ComponentLayout.Builder create(
      ComponentContext c,
      Component component,
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes) {
    if (component == null) {
      return ComponentContext.NULL_LAYOUT;
    }
    return c.newLayoutBuilder(component, defStyleAttr, defStyleRes);
  }

  public static Builder create(ComponentContext c) {
    return create(c, 0, 0);
  }

  public static Builder create(
      ComponentContext c, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
    return new Builder(c, defStyleAttr, defStyleRes);
  }

  public static class Builder {
    private final ComponentContext mContext;
    @AttrRes private final int mDefStyleAttr;
    @StyleRes private final int mDefStyleRes;

    private Builder(ComponentContext c, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
      mContext = c;
      mDefStyleAttr = defStyleAttr;
      mDefStyleRes = defStyleRes;
    }

    public ComponentLayout.Builder delegate(Component component) {
      return create(mContext, component, mDefStyleAttr, mDefStyleRes);
    }
  }
}
