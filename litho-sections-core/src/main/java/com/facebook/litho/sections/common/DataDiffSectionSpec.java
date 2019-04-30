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
import static com.facebook.litho.widget.RenderInfoDebugInfoRegistry.SONAR_SECTIONS_DEBUG_INFO_TAG;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.recyclerview.widget.DiffUtil;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentsLogger;
import com.facebook.litho.ComponentsSystrace;
import com.facebook.litho.Diff;
import com.facebook.litho.EventHandler;
import com.facebook.litho.LogTreePopulator;
import com.facebook.litho.PerfEvent;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.sections.ChangeSet;
import com.facebook.litho.sections.Section;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.DiffSectionSpec;
import com.facebook.litho.sections.annotations.OnDiff;
import com.facebook.litho.widget.RecyclerBinderUpdateCallback;
import com.facebook.litho.widget.RecyclerBinderUpdateCallback.ComponentContainer;
import com.facebook.litho.widget.RecyclerBinderUpdateCallback.Operation;
import com.facebook.litho.widget.RenderInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link DiffSectionSpec} that creates a changeSet diffing a generic {@link List<T>} of data.
 * This {@link Section} emits the following events:
 *
 * <p>{@link RenderEvent} whenever it needs a {@link Component} to render a model T from the {@code
 * List<T> data}. Providing a handler for this {@link OnEvent} is mandatory.
 *
 * <p>{@link OnCheckIsSameItemEvent} whenever during a diffing it wants to check whether two items
 * represent the same piece of data.
 *
 * <p>{@link OnCheckIsSameContentEvent} whenever during a diffing it wants to check whether two
 * items that represent the same piece of data have exactly the same content.
 *
 * <p>Diffing happens when the new {@code List<T> data} is provided. Changes in {@link
 * com.facebook.litho.annotations.State} alone will not trigger diffing.
 *
 * <ul>
 *   <li>If {@link OnCheckIsSameItemEvent} returns false {@link RenderEvent} is triggered. Otherwise
 *       {@link OnCheckIsSameContentEvent} is called.
 *   <li>If {@link OnCheckIsSameContentEvent} returns false {@link RenderEvent} is triggered.
 * </ul>
 *
 * If {@link OnCheckIsSameItemEvent} is not implemented, new {@code List<T> data} is considered to
 * be completely different and relayout will happen on every data update.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @GroupSectionSpec
 * public class MyGroupSectionSpec {
 *
 *   @OnCreateChildren
 *   protected Children onCreateChildren(
 *     SectionContext c,
 *     @Prop List<Model> modelList) {
 *
 *     return Children.create().child(DataDiffSection.create(c)
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
 * }
 * }</pre>
 */
@DiffSectionSpec(
  events = {OnCheckIsSameContentEvent.class, OnCheckIsSameItemEvent.class, RenderEvent.class}
)
public class DataDiffSectionSpec<T> {

  @OnDiff
  public static <T> void onCreateChangeSet(
      SectionContext c,
      ChangeSet changeSet,
      @Prop Diff<List<T>> data,
      @Prop(optional = true) @Nullable Diff<Boolean> detectMoves) {

    final List<T> previousData = data.getPrevious();
    final List<T> nextData = data.getNext();
    final ComponentRenderer componentRenderer =
        new ComponentRenderer(DataDiffSection.getRenderEventHandler(c), c);
    final DiffSectionOperationExecutor operationExecutor =
        new DiffSectionOperationExecutor(changeSet);
    final RecyclerBinderUpdateCallback<T> updatesCallback;
    final boolean isTracing = ComponentsSystrace.isTracing();

    final Callback<T> callback = new Callback<>(c, data.getPrevious(), data.getNext());

    final ComponentsLogger logger = c.getLogger();
    final PerfEvent logEvent =
        logger == null
            ? null
            : LogTreePopulator.populatePerfEventFromLogger(
                c, logger, logger.newPerformanceEvent(c, EVENT_SECTIONS_DATA_DIFF_CALCULATE_DIFF));

    if (isTracing) {
      ComponentsSystrace.beginSection("DiffUtil.calculateDiff");
    }
    final DiffUtil.DiffResult result =
        DiffUtil.calculateDiff(callback, isDetectMovesEnabled(detectMoves));
    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    if (logEvent != null) {
      logger.logPerfEvent(logEvent);
    }

    updatesCallback =
        new RecyclerBinderUpdateCallback<>(
            previousData, nextData, componentRenderer, operationExecutor);
    result.dispatchUpdatesTo(updatesCallback);

    updatesCallback.applyChangeset(c);
  }

  /**
   * @return true if detect moves should be enabled when performing the Diff. Detect moves is
   * enabled by default
   */
  private static boolean isDetectMovesEnabled(@Nullable Diff<Boolean> detectMoves) {
    return detectMoves == null || detectMoves.getNext() == null || detectMoves.getNext();
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
        final List<Diff> dataHolders = operation.getDataContainers();
        final int opSize = components == null ? 1 : components.size();
        switch (operation.getType()) {

          case Operation.INSERT:
            if (opSize == 1) {
              mChangeSet.insert(
                  operation.getIndex(),
                  components.get(0).getRenderInfo(),
                  c.getTreePropsCopy(),
                  dataHolders.get(0).getNext());
            } else {
              final List<RenderInfo> renderInfos = extractComponentInfos(opSize, components);
              mChangeSet.insertRange(
                  operation.getIndex(),
                  opSize,
                  renderInfos,
                  c.getTreePropsCopy(),
                  extractNextData(dataHolders));
            }
            break;

          case Operation.DELETE:
            // RecyclerBinderUpdateCallback uses the toIndex field of the operation to store count.
            final int count = operation.getToIndex();
            if (count == 1) {
              mChangeSet.delete(operation.getIndex(), dataHolders.get(0).getPrevious());
            } else {
              mChangeSet.deleteRange(operation.getIndex(), count, extractPrevData(dataHolders));
            }
            break;

          case Operation.MOVE:
            mChangeSet.move(
                operation.getIndex(), operation.getToIndex(), dataHolders.get(0).getNext());
            break;

          case Operation.UPDATE:
            if (opSize == 1) {
              mChangeSet.update(
                  operation.getIndex(),
                  components.get(0).getRenderInfo(),
                  c.getTreePropsCopy(),
                  dataHolders.get(0).getPrevious(),
                  dataHolders.get(0).getNext());
            } else {
              final List<RenderInfo> renderInfos = extractComponentInfos(opSize, components);
              mChangeSet.updateRange(
                  operation.getIndex(),
                  opSize,
                  renderInfos,
                  c.getTreePropsCopy(),
                  extractPrevData(dataHolders),
                  extractNextData(dataHolders));
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

    private static List<Object> extractPrevData(List<Diff> dataHolders) {
      final int size = dataHolders.size();
      final List<Object> data = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        data.add(dataHolders.get(i).getPrevious());
      }
      return data;
    }

    private static List<Object> extractNextData(List<Diff> dataHolders) {
      final int size = dataHolders.size();
      final List<Object> data = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        data.add(dataHolders.get(i).getNext());
      }
      return data;
    }
  }

  private static class ComponentRenderer implements RecyclerBinderUpdateCallback.ComponentRenderer {

    private final EventHandler<RenderEvent> mRenderEventEventHandler;
    private final SectionContext mSectionContext;

    private ComponentRenderer(
        EventHandler<RenderEvent> renderEventEventHandler, SectionContext sectionContext) {
      mRenderEventEventHandler = renderEventEventHandler;
      mSectionContext = sectionContext;
    }

    @Override
    public RenderInfo render(Object o, int index) {
      final RenderInfo renderInfo =
          DataDiffSection.dispatchRenderEvent(mRenderEventEventHandler, index, o, null);

      if (ComponentsConfiguration.isRenderInfoDebuggingEnabled()) {
        renderInfo.addDebugInfo(SONAR_SECTIONS_DEBUG_INFO_TAG, mSectionContext.getSectionScope());
      }

      return renderInfo;
    }
  }

  @VisibleForTesting
  static class Callback<T> extends DiffUtil.Callback {

    private final List<T> mPreviousData;
    private final List<T> mNextData;
    private final SectionContext mSectionContext;
    private final EventHandler<OnCheckIsSameItemEvent> mIsSameItemEventHandler;
    private final EventHandler<OnCheckIsSameContentEvent> mIsSameContentEventHandler;

    Callback(SectionContext sectionContext, List<T> previousData, List<T> nextData) {
      mSectionContext = sectionContext;
      mIsSameItemEventHandler =
          DataDiffSection.getOnCheckIsSameItemEventHandler(mSectionContext);
      mIsSameContentEventHandler =
          DataDiffSection.getOnCheckIsSameContentEventHandler(mSectionContext);

      mPreviousData = previousData;
      mNextData = nextData;
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
  }
}
