/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections;

import com.facebook.litho.EventHandler;
import java.lang.ref.WeakReference;

public class SectionTreeLoadingEventHandler extends EventHandler<LoadingEvent> {

  private static final int INVALID_ID = -1;
  private final WeakReference<SectionTree> mSectionTree;

  SectionTreeLoadingEventHandler(SectionTree sectionTree) {
    super(null, INVALID_ID);
    mSectionTree = new WeakReference<>(sectionTree);
  }

  SectionTreeLoadingEventHandler(SectionTree sectionTree, int id, Object[] params) {
    super(null, id, params);
    mSectionTree = new WeakReference<>(sectionTree);
  }

  @Override
  public void dispatchEvent(LoadingEvent event) {
    final SectionTree sectionTree = mSectionTree.get();
    if (sectionTree == null) {
      // This SectionTree has been released. Ignore the LoadingEvent
      return;
    }

    sectionTree.dispatchLoadingEvent(event);
  }
}
