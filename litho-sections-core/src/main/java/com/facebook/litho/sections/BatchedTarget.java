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

import android.util.SparseArray;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.sections.logger.SectionsDebugLogger;
import com.facebook.litho.widget.ChangeSetCompleteCallback;
import com.facebook.litho.widget.RenderInfo;
import com.facebook.litho.widget.SmoothScrollAlignmentType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Batches single Insert/Update/Delete commands and batches them into range commands. This logic was
 * adapted from {@link androidx.recyclerview.widget.BatchingListUpdateCallback}
 *
 * <p>We can't use that callback directly because {@link
 * com.facebook.litho.sections.SectionTree.Target} expects us to pass in {@link RenderInfo} with the
 * insert/update/remove calls.
 */
class BatchedTarget implements SectionTree.Target {

  private static final int TYPE_NONE = Integer.MAX_VALUE;
  private static final boolean ENABLE_LOGGER = ComponentsConfiguration.isDebugModeEnabled;

  private final SectionTree.Target mTarget;
  private final SparseArray<RenderInfo> mComponentInfoSparseArray = new SparseArray<>();
  private final SectionsDebugLogger mSectionsDebugLogger;
  private final String mSectionTreeTag;

  private int mLastEventType = TYPE_NONE;
  private int mLastEventPosition = -1;
  private int mLastEventCount = -1;

  BatchedTarget(SectionTree.Target target, SectionsDebugLogger sectionsDebugLogger, String tag) {
    mTarget = target;
    mSectionsDebugLogger = sectionsDebugLogger;
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
  public void insertRange(int index, int count, List<RenderInfo> renderInfos) {
    dispatchLastEvent();
    mTarget.insertRange(index, count, renderInfos);
    if (ENABLE_LOGGER) {
      logInsertIterative(index, renderInfos);
    }
  }

  @Override
  public void update(int index, RenderInfo renderInfo) {
    if (mLastEventType == Change.UPDATE
        && !(index > mLastEventPosition + mLastEventCount || index + 1 < mLastEventPosition)) {
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
  public void updateRange(int index, int count, List<RenderInfo> renderInfos) {
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
      mSectionsDebugLogger.logMove(
          mSectionTreeTag, fromPosition, toPosition, Thread.currentThread().getName());
    }
  }

  @Override
  public void notifyChangeSetComplete(
      boolean isDataChanged, ChangeSetCompleteCallback changeSetCompleteCallback) {
    mTarget.notifyChangeSetComplete(isDataChanged, changeSetCompleteCallback);
  }

  @Override
  public void requestFocus(int index) {
    mTarget.requestFocus(index);
    maybeLogRequestFocus(index);
  }

  @Override
  public void requestSmoothFocus(int index, int offset, SmoothScrollAlignmentType type) {
    mTarget.requestSmoothFocus(index, offset, type);
    maybeLogRequestFocus(index);
  }

  @Override
  public void requestFocusWithOffset(int index, int offset) {
    mTarget.requestFocusWithOffset(index, offset);
    maybeLogRequestFocusWithOffset(index, offset);
  }

  @Override
  public boolean supportsBackgroundChangeSets() {
    return mTarget.supportsBackgroundChangeSets();
  }

  @Override
  public void changeConfig(DynamicConfig dynamicConfig) {
    mTarget.changeConfig(dynamicConfig);
  }

  private void maybeLogRequestFocusWithOffset(int index, int offset) {
    if (ENABLE_LOGGER && mComponentInfoSparseArray.size() != 0) {
      mSectionsDebugLogger.logRequestFocusWithOffset(
          mSectionTreeTag,
          index,
          offset,
          mComponentInfoSparseArray.get(index),
          Thread.currentThread().getName());
    }
  }

  private void maybeLogRequestFocus(int index) {
    if (ENABLE_LOGGER && mComponentInfoSparseArray.size() != 0) {
      mSectionsDebugLogger.logRequestFocus(
          mSectionTreeTag,
          index,
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
            mSectionsDebugLogger.logInsert(
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
            mSectionsDebugLogger.logDelete(
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
            mSectionsDebugLogger.logUpdate(
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
      int startIndex, int numItems, SparseArray<RenderInfo> componentInfoSparseArray) {
    ArrayList<RenderInfo> renderInfos = new ArrayList<>(numItems);
    for (int i = startIndex; i < startIndex + numItems; i++) {
      RenderInfo renderInfo = componentInfoSparseArray.get(i);
      if (renderInfo == null) {
        throw new IllegalStateException(
            String.format(Locale.US, "Index %d does not have a corresponding renderInfo", i));
      }
      renderInfos.add(renderInfo);
    }
    return renderInfos;
  }

  private void logInsertIterative(int index, List<RenderInfo> renderInfos) {
    for (int i = 0; i < renderInfos.size(); i++) {
      mSectionsDebugLogger.logInsert(
          mSectionTreeTag, index + i, renderInfos.get(i), Thread.currentThread().getName());
    }
  }

  private void logUpdateIterative(int index, List<RenderInfo> renderInfos) {
    for (int i = 0; i < renderInfos.size(); i++) {
      mSectionsDebugLogger.logUpdate(
          mSectionTreeTag, index + i, renderInfos.get(i), Thread.currentThread().getName());
    }
  }

  private void logDeleteIterative(int index, int count) {
    for (int i = 0; i < count; i++) {
      mSectionsDebugLogger.logDelete(mSectionTreeTag, index + i, Thread.currentThread().getName());
    }
  }
}
