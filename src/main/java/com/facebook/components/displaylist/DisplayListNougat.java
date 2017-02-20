// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.displaylist;

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
