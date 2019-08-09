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

package com.facebook.litho.sections.widget;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.EventHandler;
import com.facebook.litho.Size;
import com.facebook.litho.sections.SectionTree.Target;
import com.facebook.litho.sections.config.SectionsConfiguration;
import com.facebook.litho.widget.Binder;
import com.facebook.litho.widget.ChangeSetCompleteCallback;
import com.facebook.litho.widget.ReMeasureEvent;
import com.facebook.litho.widget.RecyclerBinder;
import com.facebook.litho.widget.RenderInfo;
import com.facebook.litho.widget.SmoothScrollAlignmentType;
import com.facebook.litho.widget.ViewportInfo.ViewportChanged;
import java.util.List;

/**
 * Implementation of {@link Target} that uses a {@link RecyclerBinder}.
 */
public class SectionBinderTarget implements Target, Binder<RecyclerView> {

  private final RecyclerBinder mRecyclerBinder;
  private final boolean mUseBackgroundChangeSets;

  public SectionBinderTarget(RecyclerBinder recyclerBinder) {
    this(recyclerBinder, SectionsConfiguration.useBackgroundChangeSets);
  }

  public SectionBinderTarget(RecyclerBinder recyclerBinder, boolean useBackgroundChangeSets) {
    mRecyclerBinder = recyclerBinder;
    mUseBackgroundChangeSets = useBackgroundChangeSets;
  }

  @Override
  public void setSize(int width, int height) {
    mRecyclerBinder.setSize(width, height);
  }

  @Override
  public void measure(
      Size outSize,
      int widthSpec,
      int heightSpec,
      EventHandler<ReMeasureEvent> reMeasureEventEventHandler) {
    mRecyclerBinder.measure(outSize, widthSpec, heightSpec, reMeasureEventEventHandler);
  }

  @Override
  public ComponentTree getComponentAt(int position) {
    return mRecyclerBinder.getComponentAt(position);
  }

  @Override
  public void mount(RecyclerView view) {
    mRecyclerBinder.mount(view);
  }

  @Override
  public void bind(RecyclerView view) {
    mRecyclerBinder.bind(view);
  }

  @Override
  public void unbind(RecyclerView view) {
    mRecyclerBinder.unbind(view);
  }

  @Override
  public void unmount(RecyclerView view) {
    mRecyclerBinder.unmount(view);
  }

  @Override
  public void setViewportChangedListener(@Nullable ViewportChanged viewportChangedListener) {
    mRecyclerBinder.setViewportChangedListener(viewportChangedListener);
  }

  @Override
  public void insert(int index, RenderInfo renderInfo) {
    if (mUseBackgroundChangeSets) {
      mRecyclerBinder.insertItemAtAsync(index, renderInfo);
    } else {
      mRecyclerBinder.insertItemAt(index, renderInfo);
    }
  }

  @Override
  public void insertRange(int index, int count, List<RenderInfo> renderInfos) {
    if (mUseBackgroundChangeSets) {
      mRecyclerBinder.insertRangeAtAsync(index, renderInfos);
    } else {
      mRecyclerBinder.insertRangeAt(index, renderInfos);
    }
  }

  @Override
  public void update(int index, RenderInfo renderInfo) {
    if (mUseBackgroundChangeSets) {
      mRecyclerBinder.updateItemAtAsync(index, renderInfo);
    } else {
      mRecyclerBinder.updateItemAt(index, renderInfo);
    }
  }

  @Override
  public void updateRange(
      int index, int count, List<RenderInfo> renderInfos) {
    if (mUseBackgroundChangeSets) {
      mRecyclerBinder.updateRangeAtAsync(index, renderInfos);
    } else {
      mRecyclerBinder.updateRangeAt(index, renderInfos);
    }
  }

  @Override
  public void move(int fromPosition, int toPosition) {
    if (mUseBackgroundChangeSets) {
      mRecyclerBinder.moveItemAsync(fromPosition, toPosition);
    } else {
      mRecyclerBinder.moveItem(fromPosition, toPosition);
    }
  }

  @Override
  public void notifyChangeSetComplete(
      boolean isDataChanged, ChangeSetCompleteCallback changeSetCompleteCallback) {
    if (mUseBackgroundChangeSets) {
      mRecyclerBinder.notifyChangeSetCompleteAsync(isDataChanged, changeSetCompleteCallback);
    } else {
      mRecyclerBinder.notifyChangeSetComplete(isDataChanged, changeSetCompleteCallback);
    }
  }

  @Override
  public void requestFocus(int index) {
    mRecyclerBinder.scrollToPosition(index);
  }

  @Override
  public void requestSmoothFocus(int index, int offset, SmoothScrollAlignmentType type) {
    mRecyclerBinder.scrollSmoothToPosition(index, offset, type);
  }

  @Override
  public void requestFocusWithOffset(int index, int offset) {
    mRecyclerBinder.scrollToPositionWithOffset(index, offset);
  }

  @Override
  public void delete(int index) {
    if (mUseBackgroundChangeSets) {
      mRecyclerBinder.removeItemAtAsync(index);
    } else {
      mRecyclerBinder.removeItemAt(index);
    }
  }

  @Override
  public void deleteRange(int index, int count) {
    if (mUseBackgroundChangeSets) {
      mRecyclerBinder.removeRangeAtAsync(index, count);
    } else {
      mRecyclerBinder.removeRangeAt(index, count);
    }
  }

  @Override
  public boolean isWrapContent() {
    return mRecyclerBinder.isWrapContent();
  }

  @Override
  public boolean canMeasure() {
    return mRecyclerBinder.canMeasure();
  }

  @Override
  public void setCanMeasure(boolean canMeasure) {
    mRecyclerBinder.setCanMeasure(canMeasure);
  }

  @Override
  public boolean supportsBackgroundChangeSets() {
    return mUseBackgroundChangeSets;
  }

  @Override
  public void changeConfig(DynamicConfig dynamicConfig) {
    mRecyclerBinder.setCommitPolicy(dynamicConfig.mChangeSetsCommitPolicy);
  }

  @Override
  public void detach() {
    mRecyclerBinder.detach();
  }

  public void clear() {
    if (mUseBackgroundChangeSets) {
      mRecyclerBinder.clearAsync();
    } else {
      mRecyclerBinder.removeRangeAt(0, mRecyclerBinder.getItemCount());
    }
  }
}
