/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
