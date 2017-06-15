/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import com.facebook.litho.ComponentTree;

interface HasStickyHeader extends ViewportInfo {
  /**
   * @param position
   * @return Whether the item at given position is *sticky*
   */
  boolean isSticky(int position);

  /**
   * @param position
   * @return Whether the item at given position is *valid*, i.e. whether such position exists.
   */
  boolean isValidPosition(int position);

  /**
   * @param position
   * @return a component tree for the idem at position.
   */
  ComponentTree getComponentAt(int position);
}
