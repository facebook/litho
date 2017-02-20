// Copyright 2004-present Facebook. All Rights Reserved.

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
