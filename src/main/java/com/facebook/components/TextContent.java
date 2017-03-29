/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

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
