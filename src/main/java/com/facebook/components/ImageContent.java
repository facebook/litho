/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import java.util.Collections;
import java.util.List;

import android.graphics.drawable.Drawable;

/**
 * A UI element that contains simple resource drawables.
 */
public interface ImageContent {

  /**
   * An empty instance of {@link ImageContent}.
   */
  ImageContent EMPTY = new ImageContent() {
    @Override
    public List<Drawable> getImageItems() {
      return Collections.EMPTY_LIST;
    }
  };

  /**
   * @return the list of image drawables that are rendered by this UI element. The list returned
   * should not be modified and may be unmodifiable.
   */
  List<Drawable> getImageItems();
}
