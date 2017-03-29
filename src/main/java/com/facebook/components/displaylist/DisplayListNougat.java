/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.displaylist;

import android.app.Activity;
import android.view.RenderNode;

/**
 * Implementation of {@link PlatformDisplayList} for Android Nougat.
 */
public class DisplayListNougat extends DisplayListMarshmallow {

  static PlatformDisplayList createDisplayList(String debugName) {
    try {
      ensureInitialized();
      if (sInitialized) {
        RenderNode renderNode = RenderNode.create(debugName, null);
        return new DisplayListNougat(renderNode);
      }
    } catch (Throwable e) {
      sInitializationFailed = true;
    }

    return null;
  }

  private DisplayListNougat(RenderNode displayList) {
    super(displayList);
  }

  @Override
  public void clear() {
    mDisplayList.discardDisplayList();
  }
}
