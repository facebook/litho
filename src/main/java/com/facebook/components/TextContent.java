// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import java.util.Collections;
import java.util.List;

/**
 * A UI element that contains text.
 */
public interface TextContent {

  /**
   * An empty instance of {@link TextContent}.
   */
  TextContent EMPTY = new TextContent() {
    @Override
    public List<CharSequence> getTextItems() {
      return Collections.EMPTY_LIST;
    }
  };

  /**
   * @return the list of text items that are rendered by this UI element. The list returned should
   * not be modified and may be unmodifiable.
   */
  List<CharSequence> getTextItems();
}
