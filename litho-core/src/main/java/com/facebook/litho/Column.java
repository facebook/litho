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
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.yoga.YogaFlexDirection;

public final class Column {
  private Column() {
  }

  public static ComponentLayout.ContainerBuilder create(
      ComponentContext c,
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes) {
    InternalNode column =
        c.newLayoutBuilder(defStyleAttr, defStyleRes).flexDirection(YogaFlexDirection.COLUMN);
    if (ComponentsConfiguration.storeLayoutAttributesInSeparateObject) {
      LayoutAttributes layoutAttributes = new LayoutAttributes();
      layoutAttributes.init(c, null);
      column.setLayoutAttributes(layoutAttributes);
    }

    return column;
  }

  public static ComponentLayout.ContainerBuilder create(ComponentContext c) {
    return create(c, 0, 0);
  }
}
