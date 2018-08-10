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

package com.facebook.litho.sections.common;

import static com.facebook.litho.FrameworkLogEvents.EVENT_SECTIONS_DATA_DIFF_CALCULATE_DIFF;
import static com.facebook.litho.widget.RecyclerBinderUpdateCallback.acquire;
import static com.facebook.litho.widget.RecyclerBinderUpdateCallback.release;

import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.Pools.Pool;
import android.support.v4.util.Pools.SynchronizedPool;
import android.support.v7.util.DiffUtil;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentsLogger;
import com.facebook.litho.ComponentsPools;
import com.facebook.litho.Diff;
import com.facebook.litho.EventHandler;
import com.facebook.litho.LogTreePopulator;
import com.facebook.litho.PerfEvent;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.sections.ChangeSet;
import com.facebook.litho.sections.Section;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.DiffSectionSpec;
import com.facebook.litho.sections.annotations.OnDiff;
import com.facebook.litho.sections.config.SectionsConfiguration;
import com.facebook.litho.widget.RecyclerBinderUpdateCallback;
import com.facebook.litho.widget.RecyclerBinderUpdateCallback.ComponentContainer;
import com.facebook.litho.widget.RecyclerBinderUpdateCallback.Operation;
import com.facebook.litho.widget.RenderInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link DiffSectionSpec} that creates a changeSet diffing a generic {@link List<T>} of data.
 * This {@link Section} emits the following events:
 * <p>
 * {@link RenderEvent} whenever it needs a {@link Component} to render a model T from the list of
 * data. Providing an handler for this {@link OnEvent} is mandatory.
 * <p>
 * {@link OnCheckIsSameItemEvent} whenever during a diffing it wants to check whether two items
 * represent the same piece of data.
 * <p>
 * {@link OnCheckIsSameContentEvent} whenever during a diffing it wants to check whether two items
 * that represent the same piece of data have exactly the same content.
 *
 * <p> For example:
 * <pre>
 * {@code
 *
 * @GroupSectionSpec
 * public class MyGroupSectionSpec {
 *
 *   @OnCreateChildren
 *   protected Children onCreateChildren(
 *     SectionContext c,
 *     @Prop List<Model> modelList) {
 *
 *     Children.create().child(DataDiffSection.create(c)
 *       .data(modelList)
 *       .renderEventHandler(MyGroupSection.onRender(c))
 *       .onCheckIsSameItemEventHandler(MyGroupSection.onCheckIsSameItem(c))
 *       .onCheckIsSameContentEventHandler(...)
 *       .build());
 *   }
 *
 *   @OnEvent(OnCheckIsSameItemEvent.class)
 *   protected boolean onCheckIsSameItem(@FromEvent Model previousItem, @FromEvent Model nextItem) {
 *     return previousItem.getId() == nextItem.getId();
 *   }
 *
 *   @OnEvent(RenderEvent.class)
 *   protected RenderInfo onRender(ComponentContext c, @FromEvent Object model) {
 *     return ComponentRenderInfo.create()
 *       .component(MyComponent.create(c).model(model).build())
 *       .build();
 *   }
 * </pre>
 */
@DiffSectionSpec(
  events = {OnCheckIsSameContentEvent.class, OnCheckIsSameItemEvent.class, RenderEvent.class}
)
public class DataDiffSectionSpec<T> {

  @PropDefault public static Boolean trimHeadAndTail = false;
  @PropDefault public static Boolean trimSameInstancesOnly = false;
  @PropDefault public static @Nullable Object dataIdentifier = null;

  @OnDiff
  public static <T> void onCreateChangeSet(
      SectionContext c,
      ChangeSet changeSet,
      @Prop Diff<List<T>> data,
      @Prop(optional = true) Diff<Object> dataIdentifier,
      @Prop(optional = true) @Nullable Diff<Boolean> detectMoves,
      @Prop(optional = true) Diff<Boolean> trimHeadAndTail,
      @Prop(optional = true) Diff<Boolean> trimSameInstancesOnly) {

    final List<T> previousData = data.getPrevious();
    final List<T> nextData = data.getNext();
    final int previousDataSize = previousData == null ? 0 : previousData.size();
    final int nextDataSize = nextData == null ? 0 : nextData.size();
    final ComponentRenderer componentRenderer =
        new ComponentRenderer(DataDiffSection.getRenderEventHandler(c));
    final DiffSectionOperationExecutor operationExecutor =
        new DiffSectionOperationExecutor(changeSet);
    final RecyclerBinderUpdateCallback<T> updatesCallback;

    if (!isSameDataIdentifier(dataIdentifier)) {
      updatesCallback =
          acquire(previousDataSize, nextData, componentRenderer, operationExecutor, 0);
      if (previousDataSize > 0) {
        updatesCallback.onRemoved(0, previousDataSize);
      }
      if (nextDataSize > 0) {
        updatesCallback.onInserted(0, nextDataSize);
      }
    } else {
      final boolean shouldTrim =
          trimHeadAndTail == null || trimHeadAndTail.getNext() == null
              ? SectionsConfiguration.trimDataDiffSectionHeadAndTail
              : trimHeadAndTail.getNext().booleanValue();

      final boolean shouldTrimSameInstanceOnly =
          trimSameInstancesOnly == null || trimSameInstancesOnly.getNext() == null
              ? SectionsConfiguration.trimSameInstancesOnly
              : trimSameInstancesOnly.getNext().booleanValue();

      final Callback<T> callback =
          Callback.acquire(
              c, data.getPrevious(), data.getNext(), shouldTrim, shouldTrimSameInstanceOnly);

      final ComponentsLogger logger = c.getLogger();
      final PerfEvent logEvent =
          logger == null
              ? null
              : LogTreePopulator.populatePerfEventFromLogger(
                  c, logger, logger.newPerformanceEvent(EVENT_SECTIONS_DATA_DIFF_CALCULATE_DIFF));

      final DiffUtil.DiffResult result =
          DiffUtil.calculateDiff(callback, isDetectMovesEnabled(detectMoves));

      if (logEvent != null) {
        logger.logPerfEvent(logEvent);
      }

      updatesCallback =
          acquire(
              previousDataSize,
              nextData,
              componentRenderer,
              operationExecutor,
              callback.getTrimmedHeadItemsCount());
      result.dispatchUpdatesTo(updatesCallback);

      Callback.release(callback);
    }

    updatesCallback.applyChangeset(c);
    release(updatesCallback);
  }

  /**
   * @return true if detect moves should be enabled when performing the Diff. Detect moves is
   * enabled by default
   */
  private static boolean isDetectMovesEnabled(@Nullable Diff<Boolean> detectMoves) {
    return detectMoves == null || detectMoves.getNext() == null || detectMoves.getNext();
  }

  private static boolean isSameDataIdentifier(Diff<Object> dataIdentifier) {
    final Object previous = dataIdentifier.getPrevious();
    final Object next = dataIdentifier.getNext();
    return previous == null ? next == null : previous.equals(next);
  }

  private static class DiffSectionOperationExecutor implements
      RecyclerBinderUpdateCallback.OperationExecutor {

    private final ChangeSet mChangeSet;

    private DiffSectionOperationExecutor(ChangeSet changeSet) {
      mChangeSet = changeSet;
    }

    @Override
    public void executeOperations(ComponentContext c, List<Operation> operations) {
      for (int i = 0, size = operations.size(); i < size; i++) {
        final Operation operation = operations.get(i);
        final List<ComponentContainer> components = operation.getComponentContainers();
        final int opSize = components == null ? 1 : components.size();
        switch (operation.getType()) {

          case Operation.INSERT:
            if (opSize == 1) {
              mChangeSet.insert(
                  operation.getIndex(), components.get(0).getRenderInfo(), c.getTreePropsCopy());
            } else {
              final List<RenderInfo> renderInfos = extractComponentInfos(opSize, components);
              mChangeSet.insertRange(
                  operation.getIndex(), opSize, renderInfos, c.getTreePropsCopy());
            }
            break;

          case Operation.DELETE:
            // RecyclerBinderUpdateCallback uses the toIndex field of the operation to store count.
            final int count = operation.getToIndex();
            if (count == 1) {
              mChangeSet.delete(operation.getIndex());
            } else {
              mChangeSet.deleteRange(operation.getIndex(), count);
            }
            break;

          case Operation.MOVE:
            mChangeSet.move(operation.getIndex(), operation.getToIndex());
            break;

          case Operation.UPDATE:
            if (opSize == 1) {
              mChangeSet.update(
                  operation.getIndex(), components.get(0).getRenderInfo(), c.getTreePropsCopy());
            } else {
              final List<RenderInfo> renderInfos = extractComponentInfos(opSize, components);
              mChangeSet.updateRange(
                  operation.getIndex(), opSize, renderInfos, c.getTreePropsCopy());
            }
            break;
        }
      }
    }

    private static List<RenderInfo> extractComponentInfos(
        int opSize,
        List<ComponentContainer> components) {
      final List<RenderInfo> renderInfos = new ArrayList<>(opSize);
      for (int i = 0; i < opSize; i++) {
        renderInfos.add(components.get(i).getRenderInfo());
      }
      return renderInfos;
    }
  }

  private static class ComponentRenderer implements RecyclerBinderUpdateCallback.ComponentRenderer {

    private final EventHandler<RenderEvent> mRenderEventEventHandler;

    private ComponentRenderer(EventHandler<RenderEvent> renderEventEventHandler) {
      mRenderEventEventHandler = renderEventEventHandler;
    }

    @Override
    public RenderInfo render(Object o, int index) {
      return DataDiffSection.dispatchRenderEvent(mRenderEventEventHandler, index, o, null);
    }
  }

  @VisibleForTesting
  static class Callback<T> extends DiffUtil.Callback {
    private static final Pool<Callback> sCallbackPool = new SynchronizedPool<>(2);

    private List<T> mPreviousData;
    private List<T> mNextData;
    private SectionContext mSectionContext;
    private EventHandler<OnCheckIsSameItemEvent> mIsSameItemEventHandler;
    private EventHandler<OnCheckIsSameContentEvent> mIsSameContentEventHandler;
    private int mTrimmedHeadItemsCount;

    void init(
        SectionContext sectionContext,
        List<T> previousData,
        List<T> nextData,
        boolean trimHeadAndTail,
        boolean trimSameInstancesOnly) {
      mSectionContext = sectionContext;
      mIsSameItemEventHandler =
          DataDiffSection.getOnCheckIsSameItemEventHandler(mSectionContext);
      mIsSameContentEventHandler =
          DataDiffSection.getOnCheckIsSameContentEventHandler(mSectionContext);

      if (trimHeadAndTail && previousData != null) {
        Diff<List<T>> trimmedData =
            trimHeadAndTail(previousData, nextData, trimSameInstancesOnly, this);
        mPreviousData = trimmedData.getPrevious();
        mNextData = trimmedData.getNext();
      } else {
        mPreviousData = previousData;
        mNextData = nextData;
      }
    }

    @Override
    public int getOldListSize() {
      return mPreviousData == null ? 0 : mPreviousData.size();
    }

    @Override
    public int getNewListSize() {
      return mNextData == null ? 0 : mNextData.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
      final T previous = mPreviousData.get(oldItemPosition);
      final T next = mNextData.get(newItemPosition);

      return areItemsTheSame(previous, next);
    }

    private boolean areItemsTheSame(T previous, T next) {
      if (previous == next) {
        return true;
      }

      if (mIsSameItemEventHandler != null) {
        return DataDiffSection.dispatchOnCheckIsSameItemEvent(
            mIsSameItemEventHandler,
            previous,
            next);
      }

      return previous.equals(next);
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
      final T previous = mPreviousData.get(oldItemPosition);
      final T next = mNextData.get(newItemPosition);

      return areContentsTheSame(previous, next);
    }

    private boolean areContentsTheSame(T previous, T next) {
      if (previous == next) {
        return true;
      }

      if (mIsSameContentEventHandler != null) {
        return DataDiffSection.dispatchOnCheckIsSameContentEvent(
            mIsSameContentEventHandler,
            previous,
            next);
      }

      return previous.equals(next);
    }

    @VisibleForTesting
    static <T> Callback<T> acquire(
        SectionContext sectionContext,
        List<T> previousData,
        List<T> nextData,
        boolean trimHeadAndTail,
        boolean trimSameInstancesOnly) {
      Callback callback = sCallbackPool.acquire();
      if (callback == null) {
        callback = new Callback();
      }
      callback.init(sectionContext, previousData, nextData, trimHeadAndTail, trimSameInstancesOnly);

      return callback;
    }

    private static void release(Callback callback) {
      callback.mNextData = null;
      callback.mPreviousData = null;
      callback.mSectionContext = null;
      callback.mIsSameItemEventHandler = null;
      callback.mIsSameContentEventHandler = null;
      callback.mTrimmedHeadItemsCount = 0;
      sCallbackPool.release(callback);
    }

    @VisibleForTesting
    int getTrimmedHeadItemsCount() {
      return mTrimmedHeadItemsCount;
    }

    static <T> Diff<List<T>> trimHeadAndTail(
        List<T> previousData,
        List<T> nextData,
        boolean trimSameInstancesOnly,
        Callback<T> callback) {
      int headTrimmedCount = 0;
      int tailTrimmedCount = 0;
      final int previousDataSize = previousData.size();
      final int nextDataSize = nextData.size();
      int tailRunnerPrevious = previousDataSize - 1;
      int tailRunnerNext = nextDataSize - 1;

      while (headTrimmedCount < previousDataSize && headTrimmedCount < nextDataSize) {
        if (shouldTrim(
            previousData.get(headTrimmedCount),
            nextData.get(headTrimmedCount),
            trimSameInstancesOnly,
            callback)) {
          headTrimmedCount++;
        } else {
          break;
        }
      }

      while (tailRunnerPrevious > headTrimmedCount && tailRunnerNext > headTrimmedCount) {
        if (shouldTrim(
            previousData.get(tailRunnerPrevious),
            nextData.get(tailRunnerNext),
            trimSameInstancesOnly,
            callback)) {
          tailRunnerPrevious--;
          tailRunnerNext--;
          tailTrimmedCount++;
        } else {
          break;
        }
      }

      callback.mTrimmedHeadItemsCount = headTrimmedCount;

      if (headTrimmedCount > 0 || tailTrimmedCount > 0) {
        return ComponentsPools.acquireDiff(
            previousData.subList(headTrimmedCount, previousDataSize - tailTrimmedCount),
            nextData.subList(headTrimmedCount, nextDataSize - tailTrimmedCount));
      }

      return ComponentsPools.acquireDiff(previousData, nextData);
    }

    private static <T> boolean shouldTrim(
        T previousItem, T nextItem, boolean trimSameInstancesOnly, Callback callback) {
      if (previousItem == nextItem) {
        return true;
      }

      if (trimSameInstancesOnly) {
        return false;
      }

      return callback.areItemsTheSame(previousItem, nextItem)
          && callback.areContentsTheSame(previousItem, nextItem);
    }
  }
}
