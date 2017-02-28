// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.support.annotation.AttrRes;
import android.support.annotation.StyleRes;

/**
 * Utility class to create a new container builder in the same way component layouts
 * are created.
 *
 * @see ComponentContext
 * @see ComponentContext#newLayoutBuilder(Component)
 */
public final class Container {
  private Container() {
  }

  public static ComponentLayout.ContainerBuilder create(
      ComponentContext c,
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes) {
    return c.newLayoutBuilder(defStyleAttr, defStyleRes);
  }

  public static ComponentLayout.ContainerBuilder create(ComponentContext c) {
    return create(c, 0, 0);
  }
}
