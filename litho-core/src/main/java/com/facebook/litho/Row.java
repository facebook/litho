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

public final class Row {
  private Row() {
  }

  public static ComponentLayout.ContainerBuilder create(
      ComponentContext c,
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes) {
    InternalNode row = c.newLayoutBuilder(defStyleAttr, defStyleRes);
    if (ComponentsConfiguration.storeLayoutAttributesInSeparateObject) {
      LayoutAttributes layoutAttributes = new LayoutAttributes();
      layoutAttributes.init(c, null);
      row.setLayoutAttributes(layoutAttributes);
    }

    return row;
  }

  public static ComponentLayout.ContainerBuilder create(ComponentContext c) {
    return create(c, 0, 0);
  }
}
