// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import java.util.Collections;
import java.util.List;

import android.graphics.Rect;
import android.support.annotation.VisibleForTesting;

/**
 * Holds information about a {@link TestOutput}.
 */
public class TestItem {
  private String mTestKey;
  private final Rect mBounds = new Rect();
  private ComponentHost mHost;
  private Object mContent;

  @VisibleForTesting
  public String getTestKey() {
    return mTestKey;
  }

  void setTestKey(String testKey) {
    mTestKey = testKey;
  }

  @VisibleForTesting
  public Rect getBounds() {
    return mBounds;
  }

  void setBounds(Rect bounds) {
    mBounds.set(bounds);
  }

  void setBounds(int left, int top, int right, int bottom) {
    mBounds.set(left, top, right, bottom);
  }

  @VisibleForTesting
  public ComponentHost getHost() {
    return mHost;
  }

  @VisibleForTesting
  public String getTextContent() {
    final TextContent textContent =
        ComponentHostUtils.extractTextContent(Collections.singletonList(mContent));
    final List<CharSequence> textItems = textContent.getTextItems();
    final StringBuilder sb = new StringBuilder();
    for (int i = 0, size = textItems.size(); i < size; i++) {
      sb.append(textItems.get(i));
    }

    return sb.toString();
  }

  void setHost(ComponentHost host) {
    mHost = host;
  }

  void setContent(Object content) {
    mContent = content;
  }

  Object getContent() {
    return mContent;
  }

  void release() {
    mTestKey = null;
    mBounds.setEmpty();
    mHost = null;
  }
}
