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

package com.facebook.litho.displaylist;

import android.support.annotation.Nullable;
import android.view.RenderNode;

/** Implementation of {@link PlatformDisplayList} for Android Nougat. */
public class DisplayListPostMarshmallow extends DisplayListMarshmallow {

  @Nullable
  static PlatformDisplayList createDisplayList(String debugName) {
    try {
      ensureInitialized();
      if (sInitialized) {
        RenderNode renderNode = RenderNode.create(debugName, null);
        return new DisplayListPostMarshmallow(renderNode);
      }
    } catch (Throwable e) {
      sInitializationFailed = true;
    }

    return null;
  }

  private DisplayListPostMarshmallow(RenderNode displayList) {
    super(displayList);
  }

  @Override
  public void clear() {
    mDisplayList.discardDisplayList();
  }
}
