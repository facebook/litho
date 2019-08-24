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

package com.facebook.litho.widget;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.recyclerview.widget.ListUpdateCallback;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentsReporter;
import com.facebook.litho.ComponentsSystrace;
import com.facebook.litho.Diff;
import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of {@link ListUpdateCallback} that generates the relevant {@link Component}s
 * when an item is inserted/updated.
 *
 * <p>The user of this API is expected to provide a ComponentRenderer implementation to build a
 * Component from a generic model object.
 */
public class RecyclerBinderUpdateCallback<T> implements ListUpdateCallback {

  public interface ComponentRenderer<T> {
    RenderInfo render(T t, int idx);
  }

  public interface OperationExecutor {
    void executeOperations(@Nullable ComponentContext c, List<Operation> operations);
  }

  private final int mOldDataSize;
  private final List<T> mPrevData;
  private final List<T> mNextData;
  private final List<Operation> mOperations;
  private final List<ComponentContainer> mPlaceholders;
  private final List<Diff> mDataHolders;
  private final ComponentRenderer mComponentRenderer;
  private final OperationExecutor mOperationExecutor;

  public RecyclerBinderUpdateCallback(
      List<T> prevData,
      List<T> nextData,
      ComponentRenderer<T> componentRenderer,
      RecyclerBinder recyclerBinder) {
    this(
        prevData, nextData, componentRenderer, new RecyclerBinderOperationExecutor(recyclerBinder));
  }

  public RecyclerBinderUpdateCallback(
      List<T> prevData,
      List<T> nextData,
      ComponentRenderer<T> componentRenderer,
      OperationExecutor operationExecutor) {
    mPrevData = prevData;
    mOldDataSize = prevData != null ? prevData.size() : 0;
    mNextData = nextData;
    mComponentRenderer = componentRenderer;
    mOperationExecutor = operationExecutor;

    mOperations = new ArrayList<>();
    mPlaceholders = new ArrayList<>();
    mDataHolders = new ArrayList<>();
    for (int i = 0; i < mOldDataSize; i++) {
      mPlaceholders.add(new ComponentContainer(null, false));
      mDataHolders.add(new Diff(mPrevData.get(i), null));
    }
  }

  @Override
  public void onInserted(int position, int count) {
    final List<ComponentContainer> placeholders = new ArrayList<>(count);
    final List<Diff> dataHolders = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      final int index = position + i;
      final ComponentContainer componentContainer = new ComponentContainer(null, true);
      mPlaceholders.add(index, componentContainer);
      placeholders.add(componentContainer);

      final Diff dataHolder = new Diff(null, null);
      mDataHolders.add(index, dataHolder);
      dataHolders.add(dataHolder);
    }

    mOperations.add(new Operation(Operation.INSERT, position, -1, placeholders, dataHolders));
  }

  @Override
  public void onRemoved(int position, int count) {
    final List<Diff> dataHolders = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      mPlaceholders.remove(position);

      final Diff dataHolder = mDataHolders.remove(position);
      dataHolders.add(dataHolder);
    }

    mOperations.add(new Operation(Operation.DELETE, position, count, null, dataHolders));
  }

  @Override
  public void onMoved(int fromPosition, int toPosition) {
    final List<Diff> dataHolders = new ArrayList<>(1);

    final ComponentContainer placeholder = mPlaceholders.remove(fromPosition);
    mPlaceholders.add(toPosition, placeholder);

    final Diff dataHolder = mDataHolders.remove(fromPosition);
    dataHolders.add(dataHolder);
    mDataHolders.add(toPosition, dataHolder);

    mOperations.add(new Operation(Operation.MOVE, fromPosition, toPosition, null, dataHolders));
  }

  @Override
  public void onChanged(int position, int count, Object payload) {
    final List<ComponentContainer> placeholders = new ArrayList<>();
    final List<Diff> dataHolders = new ArrayList<>(count);

    for (int i = 0; i < count; i++) {
      final int index = position + i;
      final ComponentContainer placeholder = mPlaceholders.get(index);
      placeholder.mNeedsComputation = true;
      placeholders.add(placeholder);
      dataHolders.add(mDataHolders.get(index));
    }

    mOperations.add(new Operation(Operation.UPDATE, position, -1, placeholders, dataHolders));
  }

  public void applyChangeset(ComponentContext c) {
    final boolean isTracing = ComponentsSystrace.isTracing();

    if (mNextData != null && mNextData.size() != mPlaceholders.size()) {
      logErrorForInconsistentSize(c);

      // Clear mPlaceholders and mOperations since they aren't matching with mNextData anymore.
      mOperations.clear();
      mDataHolders.clear();
      mPlaceholders.clear();

      final List<Diff> prevDataHolders = new ArrayList<>();
      for (int i = 0; i < mOldDataSize; i++) {
        prevDataHolders.add(new Diff(mPrevData.get(i), null));
      }
      mDataHolders.addAll(prevDataHolders);
      mOperations.add(new Operation(Operation.DELETE, 0, mOldDataSize, null, prevDataHolders));

      final int dataSize = mNextData.size();
      final List<ComponentContainer> placeholders = new ArrayList<>(dataSize);
      final List<Diff> dataHolders = new ArrayList<>(dataSize);
      for (int i = 0; i < dataSize; i++) {
        final Object model = mNextData.get(i);
        if (isTracing) {
          ComponentsSystrace.beginSection("renderInfo:" + getModelName(model));
        }
        final RenderInfo renderInfo = mComponentRenderer.render(model, i);
        if (isTracing) {
          ComponentsSystrace.endSection();
        }

        placeholders.add(i, new ComponentContainer(renderInfo, false));
        dataHolders.add(new Diff(null, model));
      }
      mPlaceholders.addAll(placeholders);
      mDataHolders.addAll(dataHolders);
      mOperations.add(new Operation(Operation.INSERT, 0, -1, placeholders, dataHolders));
    } else {
      for (int i = 0, size = mPlaceholders.size(); i < size; i++) {
        if (mPlaceholders.get(i).mNeedsComputation) {
          final Object model = mNextData.get(i);
          if (isTracing) {
            ComponentsSystrace.beginSection("renderInfo:" + getModelName(model));
          }
          mPlaceholders.get(i).mRenderInfo = mComponentRenderer.render(model, i);
          if (isTracing) {
            ComponentsSystrace.endSection();
          }
          mDataHolders.get(i).setNext(model);
        }
      }
    }

    if (isTracing) {
      ComponentsSystrace.beginSection("executeOperations");
    }
    mOperationExecutor.executeOperations(c, mOperations);
    if (isTracing) {
      ComponentsSystrace.endSection();
    }
  }

  private static String getModelName(Object model) {
    return model instanceof DataDiffModelName
        ? ((DataDiffModelName) model).getName()
        : model.getClass().getSimpleName();
  }

  /** Emit a soft error if the size between mPlaceholders and mNextData aren't the same. */
  private void logErrorForInconsistentSize(ComponentContext c) {
    final StringBuilder message = new StringBuilder();
    message
        .append("Inconsistent size between mPlaceholders(")
        .append(mPlaceholders.size())
        .append(") and mNextData(")
        .append(mNextData.size())
        .append("); ");

    message.append("mOperations: [");
    for (int i = 0, size = mOperations.size(); i < size; i++) {
      final Operation operation = mOperations.get(i);
      message
          .append("[type=")
          .append(operation.getType())
          .append(", index=")
          .append(operation.getIndex())
          .append(", toIndex=")
          .append(operation.getToIndex());
      if (operation.mComponentContainers != null) {
        message.append(", count=").append(operation.mComponentContainers.size());
      }
      message.append("], ");
    }
    message.append("]; ");
    message.append("mNextData: [");
    for (int i = 0, size = mNextData.size(); i < size; i++) {
      message.append("[").append(mNextData.get(i)).append("], ");
    }
    message.append("]");
    ComponentsReporter.emitMessage(ComponentsReporter.LogLevel.ERROR, message.toString());
  }

  @VisibleForTesting
  List<Operation> getOperations() {
    return mOperations;
  }

  public static class Operation {

    public static final int INSERT = 0;
    public static final int UPDATE = 1;
    public static final int DELETE = 2;
    public static final int MOVE = 3;

    private final int mType;
    private final int mIndex;
    private final int mToIndex;
    private final List<ComponentContainer> mComponentContainers;
    private final List<Diff> mDataContainers;

    private Operation(
        int type,
        int index,
        int toIndex,
        List<ComponentContainer> placeholder,
        List<Diff> dataHolders) {
      mType = type;
      mIndex = index;
      mToIndex = toIndex;
      mComponentContainers = placeholder;
      mDataContainers = dataHolders;
    }

    public int getType() {
      return mType;
    }

    public int getIndex() {
      return mIndex;
    }

    public int getToIndex() {
      return mToIndex;
    }

    public List<ComponentContainer> getComponentContainers() {
      return mComponentContainers;
    }

    public List<Diff> getDataContainers() {
      return mDataContainers;
    }
  }

  public static class ComponentContainer {

    private RenderInfo mRenderInfo;
    private boolean mNeedsComputation;

    public ComponentContainer(RenderInfo renderInfo, boolean needsComputation) {
      mRenderInfo = renderInfo;
      mNeedsComputation = needsComputation;
    }

    public RenderInfo getRenderInfo() {
      return mRenderInfo;
    }
  }
}
