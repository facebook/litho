/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import android.graphics.Rect;
import android.widget.Checkable;
import androidx.annotation.VisibleForTesting;
import com.facebook.proguard.annotations.DoNotStrip;
import java.util.Collections;
import java.util.List;

/**
 * Holds information about a {@link TestOutput}.
 */
@DoNotStrip
public class TestItem {
  private String mTestKey;
  private final Rect mBounds = new Rect();
  private ComponentHost mHost;
  private Object mContent;
  /** Unique key to identify if this test-item was reused */
  private final AcquireKey mAcquireKey = new AcquireKey();

  @DoNotStrip
  @VisibleForTesting
  public String getTestKey() {
    return mTestKey;
  }

  void setTestKey(String testKey) {
    mTestKey = testKey;
  }

  @DoNotStrip
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

  @DoNotStrip
  @VisibleForTesting
  public ComponentHost getHost() {
    return mHost;
  }

  @DoNotStrip
  @VisibleForTesting
  public String getTextContent() {
    final List<CharSequence> textItems = getTextItems();
    final StringBuilder sb = new StringBuilder();
    for (int i = 0, size = textItems.size(); i < size; i++) {
      sb.append(textItems.get(i));
    }

    return sb.toString();
  }

  public List<CharSequence> getTextItems() {
    return
        ComponentHostUtils
            .extractTextContent(Collections.singletonList(mContent))
            .getTextItems();
  }

  public boolean isChecked() {
    if (mContent instanceof Checkable) {
      return ((Checkable) mContent).isChecked();
    }

    throw new UnsupportedOperationException(
        "This Litho component can't be checked, we can't determine its check state.");
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

  @DoNotStrip
  public AcquireKey getAcquireKey() {
    return mAcquireKey;
  }

  @DoNotStrip
  public static final class AcquireKey {}
}
