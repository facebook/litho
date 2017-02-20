// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.widget;

interface HasStickyHeader {

  /**
   * @return the adapter position of the first visible view.
   */
  int findFirstVisibleItemPosition();

  /**
   * @return the adapter position of the last visible view.
   */
  int findLastVisibleItemPosition();

  /**
   * @param position
   * @return Whether the item at given position is *sticky*
   */
  boolean isSticky(int position);
}
