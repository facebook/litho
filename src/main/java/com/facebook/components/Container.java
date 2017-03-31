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
 * @deprecated Container has been deprecated in favor of {@link Row} & {@link Column} to make
 *             their direction explicit.
 */
@Deprecated
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
