/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.widget;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.EventHandler;
import com.facebook.litho.Size;
import com.facebook.litho.sections.SectionTree.Target;
import com.facebook.litho.sections.config.SectionsConfiguration;
import com.facebook.litho.widget.Binder;
import com.facebook.litho.widget.ReMeasureEvent;
import com.facebook.litho.widget.RecyclerBinder;
import com.facebook.litho.widget.RenderInfo;
import com.facebook.litho.widget.ViewportInfo.ViewportChanged;
import java.util.List;

/**
 * Implementation of {@link Target} that uses a {@link RecyclerBinder}.
 */
public class SectionBinderTarget implements Target, Binder<RecyclerView> {

  private final RecyclerBinder mRecyclerBinder;

  public SectionBinderTarget(RecyclerBinder recyclerBinder) {
    mRecyclerBinder = recyclerBinder;
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
    if (SectionsConfiguration.asyncMutations) {
      mRecyclerBinder.insertItemAtAsync(index, renderInfo);
    } else {
      mRecyclerBinder.insertItemAt(index, renderInfo);
    }
  }

  @Override
  public void insertRange(int index, int count, List<RenderInfo> renderInfos) {
    if (SectionsConfiguration.asyncMutations) {
      mRecyclerBinder.insertRangeAtAsync(index, renderInfos);
    } else {
      mRecyclerBinder.insertRangeAt(index, renderInfos);
    }
  }

  @Override
  public void update(int index, RenderInfo renderInfo) {
    mRecyclerBinder.updateItemAt(index, renderInfo);
  }

  @Override
  public void updateRange(
      int index, int count, List<RenderInfo> renderInfos) {
    mRecyclerBinder.updateRangeAt(index, renderInfos);
  }

  @Override
  public void move(int fromPosition, int toPosition) {
    mRecyclerBinder.moveItem(fromPosition, toPosition);
  }

  @Override
  public void requestFocus(int index) {
    mRecyclerBinder.scrollToPosition(index, false);
  }

  @Override
  public void requestSmoothFocus(int index) {
    mRecyclerBinder.scrollToPosition(index, true);
  }

  @Override
  public void requestFocusWithOffset(int index, int offset) {
    mRecyclerBinder.scrollToPositionWithOffset(index, offset);
  }

  @Override
  public void delete(int index) {
    mRecyclerBinder.removeItemAt(index);
  }

  @Override
  public void deleteRange(int index, int count) {
    mRecyclerBinder.removeRangeAt(index, count);
  }

  public void clear() {
    mRecyclerBinder.removeRangeAt(0, mRecyclerBinder.getItemCount());
  }
}
