/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections;

import android.util.SparseArray;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.sections.logger.SectionComponentLogger;
import com.facebook.litho.widget.RenderInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Batches single Insert/Update/Delete commands and batches them into range commands.
 * This logic was adapted from {@link android.support.v7.util.BatchingListUpdateCallback}
 *
 * We can't use that callback directly because {@link com.facebook.litho.sections.SectionTree.Target}
 * expects us to pass in {@link RenderInfo} with the insert/update/remove calls.
 */
class BatchedTarget implements SectionTree.Target {

  private static final int TYPE_NONE = Integer.MAX_VALUE;
  private static final boolean ENABLE_LOGGER = ComponentsConfiguration.isDebugModeEnabled;

  private final SectionTree.Target mTarget;
  private final SparseArray<RenderInfo> mComponentInfoSparseArray = new SparseArray<>();
  private final SectionComponentLogger mSectionComponentLogger;
  private final String mSectionTreeTag;

  private int mLastEventType = TYPE_NONE;
  private int mLastEventPosition = -1;
  private int mLastEventCount = -1;

  BatchedTarget(
      SectionTree.Target target,
      SectionComponentLogger sectionComponentLogger,
      String tag) {
    mTarget = target;
    mSectionComponentLogger = sectionComponentLogger;
    mSectionTreeTag = tag;
  }

  @Override
  public void insert(int index, RenderInfo renderInfo) {
    if (mLastEventType == Change.INSERT
        && index >= mLastEventPosition
        && index <= mLastEventPosition + mLastEventCount
        && !(index < mLastEventPosition + mLastEventCount)) {
      mLastEventCount++;
      mLastEventPosition = Math.min(index, mLastEventPosition);
      mComponentInfoSparseArray.put(index, renderInfo);
      return;
    }
    dispatchLastEvent();
    mLastEventPosition = index;
    mLastEventCount = 1;
    mLastEventType = Change.INSERT;
    mComponentInfoSparseArray.put(index, renderInfo);
  }

  @Override
  public void insertRange(
      int index, int count, List<RenderInfo> renderInfos) {
    dispatchLastEvent();
    mTarget.insertRange(index, count, renderInfos);
    if (ENABLE_LOGGER) {
      logInsertIterative(index, renderInfos);
    }
  }

  @Override
  public void update(int index, RenderInfo renderInfo) {
    if (mLastEventType == Change.UPDATE &&
        !(index > mLastEventPosition + mLastEventCount
            || index + 1 < mLastEventPosition)) {
      // take potential overlap into account
      int previousEnd = mLastEventPosition + mLastEventCount;
      mLastEventPosition = Math.min(index, mLastEventPosition);
      mLastEventCount = Math.max(previousEnd, index + 1) - mLastEventPosition;
      mComponentInfoSparseArray.put(index, renderInfo);
      return;
    }
    dispatchLastEvent();
    mLastEventPosition = index;
    mLastEventCount = 1;
    mLastEventType = Change.UPDATE;
    mComponentInfoSparseArray.put(index, renderInfo);
  }

  @Override
  public void updateRange(
      int index, int count, List<RenderInfo> renderInfos) {
    dispatchLastEvent();
    mTarget.updateRange(index, count, renderInfos);
    if (ENABLE_LOGGER) {
      logUpdateIterative(index, renderInfos);
    }
  }

  @Override
  public void delete(int index) {
    if (mLastEventType == Change.DELETE
        && mLastEventPosition >= index
        && mLastEventPosition <= index + 1) {
      mLastEventCount++;
      mLastEventPosition = index;
      return;
    }
    dispatchLastEvent();
    mLastEventPosition = index;
    mLastEventCount = 1;
    mLastEventType = Change.DELETE;
  }

  @Override
  public void deleteRange(int index, int count) {
    dispatchLastEvent();
    mTarget.deleteRange(index, count);
  }

  @Override
  public void move(int fromPosition, int toPosition) {
    dispatchLastEvent();
    mTarget.move(fromPosition, toPosition);
    if (ENABLE_LOGGER) {
      mSectionComponentLogger.logMove(
          mSectionTreeTag, fromPosition, toPosition, Thread.currentThread().getName());
    }
  }

  @Override
  public void requestFocus(int index) {
    mTarget.requestFocus(index);
    if (ENABLE_LOGGER && mComponentInfoSparseArray.size() != 0) {
      mSectionComponentLogger.logRequestFocus(
          mSectionTreeTag,
          index,
          mComponentInfoSparseArray.get(index),
          Thread.currentThread().getName());
    }
  }

  @Override
  public void requestFocusWithOffset(int index, int offset) {
    mTarget.requestFocusWithOffset(index, offset);
    if (ENABLE_LOGGER && mComponentInfoSparseArray.size() != 0) {
      mSectionComponentLogger.logRequestFocusWithOffset(
          mSectionTreeTag,
          index,
          offset,
          mComponentInfoSparseArray.get(index),
          Thread.currentThread().getName());
    }
  }

  /*package-private*/ void dispatchLastEvent() {
    if (mLastEventType == TYPE_NONE) {
      return;
    }
    switch (mLastEventType) {
      case Change.INSERT:
        List<RenderInfo> renderInfosInsert =
            collectComponentInfos(mLastEventPosition, mLastEventCount, mComponentInfoSparseArray);
        if (mLastEventCount > 1) {
          mTarget.insertRange(mLastEventPosition, mLastEventCount, renderInfosInsert);
          if (ENABLE_LOGGER) {
            logInsertIterative(mLastEventPosition, renderInfosInsert);
          }
        } else {
          mTarget.insert(mLastEventPosition, mComponentInfoSparseArray.get(mLastEventPosition));
          if (ENABLE_LOGGER) {
            mSectionComponentLogger.logInsert(
                mSectionTreeTag,
                mLastEventPosition,
                mComponentInfoSparseArray.get(mLastEventPosition),
                Thread.currentThread().getName());
          }
        }
        break;
      case Change.DELETE:
        if (mLastEventCount > 1) {
          mTarget.deleteRange(mLastEventPosition, mLastEventCount);
          if (ENABLE_LOGGER) {
            logDeleteIterative(mLastEventPosition, mLastEventCount);
          }
        } else {
          mTarget.delete(mLastEventPosition);
          if (ENABLE_LOGGER) {
            mSectionComponentLogger.logDelete(
                mSectionTreeTag, mLastEventPosition, Thread.currentThread().getName());
          }
        }
        break;
      case Change.UPDATE:
        List<RenderInfo> renderInfosUpdate =
            collectComponentInfos(mLastEventPosition, mLastEventCount, mComponentInfoSparseArray);
        if (mLastEventCount > 1) {
          mTarget.updateRange(mLastEventPosition, mLastEventCount, renderInfosUpdate);
          if (ENABLE_LOGGER) {
            logUpdateIterative(mLastEventPosition, renderInfosUpdate);
          }
        } else {
          mTarget.update(mLastEventPosition, mComponentInfoSparseArray.get(mLastEventPosition));
          if (ENABLE_LOGGER) {
            mSectionComponentLogger.logUpdate(
                mSectionTreeTag,
                mLastEventPosition,
                mComponentInfoSparseArray.get(mLastEventPosition),
                Thread.currentThread().getName());
          }
        }
        break;
      case Change.MOVE:
        break;
    }
    mLastEventType = TYPE_NONE;
    mComponentInfoSparseArray.clear();
  }

  private static List<RenderInfo> collectComponentInfos(
      int startIndex,
      int numItems,
      SparseArray<RenderInfo> componentInfoSparseArray) {
    ArrayList<RenderInfo> renderInfos = new ArrayList<>(numItems);
    for (int i = startIndex; i < startIndex + numItems; i++) {
      RenderInfo renderInfo = componentInfoSparseArray.get(i);
      if (renderInfo == null) {
        throw new IllegalStateException(
            String.format(
                Locale.US,
                "Index %d does not have a corresponding renderInfo",
                i));
      }
      renderInfos.add(renderInfo);
    }
    return renderInfos;
  }

  private void logInsertIterative(int index, List<RenderInfo> renderInfos) {
    for (int i = 0; i < renderInfos.size(); i++) {
      mSectionComponentLogger.logInsert(
          mSectionTreeTag, index + i, renderInfos.get(i), Thread.currentThread().getName());
    }
  }

  private void logUpdateIterative(int index, List<RenderInfo> renderInfos) {
    for (int i = 0; i < renderInfos.size(); i++) {
      mSectionComponentLogger.logUpdate(
          mSectionTreeTag, index + i, renderInfos.get(i), Thread.currentThread().getName());
    }
  }

  private void logDeleteIterative(int index, int count) {
    for (int i = 0; i < count; i++) {
      mSectionComponentLogger.logDelete(
          mSectionTreeTag, index + i, Thread.currentThread().getName());
    }
  }
}
