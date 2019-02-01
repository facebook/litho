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

import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.Pools.SynchronizedPool;
import android.support.v7.util.ListUpdateCallback;
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
 * The user of this API is expected to provide a ComponentRenderer implementation to build a
 * Component from a generic model object.
 *
 */
public class RecyclerBinderUpdateCallback<T> implements ListUpdateCallback {

  private static final SynchronizedPool<RecyclerBinderUpdateCallback> sUpdatesCallbackPool =
      new SynchronizedPool<>(4);
  private int mHeadOffset;

  public interface ComponentRenderer<T> {
    RenderInfo render(T t, int idx);
  }

  public interface OperationExecutor {
    void executeOperations(@Nullable ComponentContext c, List<Operation> operations);
  }

  private int mOldDataSize;
  private List<T> mPrevData;
  private List<T> mNextData;
  private List<Operation> mOperations;
  private List<ComponentContainer> mPlaceholders;
  private List<Diff> mDataHolders;
  private ComponentRenderer mComponentRenderer;
  private OperationExecutor mOperationExecutor;

  public static <T> RecyclerBinderUpdateCallback<T> acquire(
      List<T> prevData,
      List<T> nextData,
      ComponentRenderer<T> componentRenderer,
      RecyclerBinder recyclerBinder) {

    return acquire(
        prevData,
        nextData,
        componentRenderer,
        new RecyclerBinderOperationExecutor(recyclerBinder),
        0);
  }

  public static <T> RecyclerBinderUpdateCallback<T> acquire(
      List<T> prevData,
      List<T> nextData,
      ComponentRenderer<T> componentRenderer,
      OperationExecutor operationExecutor,
      int headOffset) {

    RecyclerBinderUpdateCallback instance = sUpdatesCallbackPool.acquire();
    if (instance == null) {
      instance = new RecyclerBinderUpdateCallback();
    }

    instance.init(prevData, nextData, componentRenderer, operationExecutor, headOffset);
    return instance;
  }

  public static<T> void release(RecyclerBinderUpdateCallback<T> updatesCallback) {
    final List<Operation> operations = updatesCallback.mOperations;
    for (int i = 0, size = operations.size(); i < size; i++) {
      operations.get(i).release();
    }
    updatesCallback.mOperations = null;
    updatesCallback.mPrevData = null;
    updatesCallback.mNextData = null;
    for (int i = 0, size = updatesCallback.mPlaceholders.size(); i < size; i++) {
      updatesCallback.mPlaceholders.get(i).release();
    }
    updatesCallback.mDataHolders.clear();
    updatesCallback.mComponentRenderer = null;
    updatesCallback.mOperationExecutor = null;
    updatesCallback.mHeadOffset = 0;
    sUpdatesCallbackPool.release(updatesCallback);
  }

  private RecyclerBinderUpdateCallback() {

  }

  private void init(
      List<T> prevData,
      List<T> nextData,
      ComponentRenderer<T> componentRenderer,
      OperationExecutor operationExecutor,
      int headOffset) {
    mPrevData = prevData;
    mOldDataSize = prevData != null ? prevData.size() : 0;
    mNextData = nextData;
    mComponentRenderer = componentRenderer;
    mOperationExecutor = operationExecutor;
    mHeadOffset = headOffset;

    mOperations = new ArrayList<>();
    mPlaceholders = new ArrayList<>();
    mDataHolders = new ArrayList<>();
    for (int i = 0; i < mOldDataSize; i++) {
      mPlaceholders.add(ComponentContainer.acquire());
      mDataHolders.add(new Diff(mPrevData.get(i), null));
    }
  }

  @Override
  public void onInserted(int position, int count) {
    final List<ComponentContainer> placeholders = new ArrayList<>(count);
    final List<Diff> dataHolders = new ArrayList<>(count);
    position += mHeadOffset;
    for (int i = 0; i < count; i++) {
      final int index = position + i;
      final ComponentContainer componentContainer = ComponentContainer.acquire();
      componentContainer.mNeedsComputation = true;
      mPlaceholders.add(index, componentContainer);
      placeholders.add(componentContainer);

      final Diff dataHolder = new Diff(null, null);
      mDataHolders.add(index, dataHolder);
      dataHolders.add(dataHolder);
    }

    mOperations.add(Operation.acquire(Operation.INSERT, position, -1, placeholders, dataHolders));
  }

  @Override
  public void onRemoved(int position, int count) {
    final List<Diff> dataHolders = new ArrayList<>(count);
    position += mHeadOffset;
    for (int i = 0; i < count; i++) {
      final ComponentContainer componentContainer = mPlaceholders.remove(position);
      componentContainer.release();

      final Diff dataHolder = mDataHolders.remove(position);
      dataHolders.add(dataHolder);
    }

    mOperations.add(Operation.acquire(Operation.DELETE, position, count, null, dataHolders));
  }

  @Override
  public void onMoved(int fromPosition, int toPosition) {
    final List<Diff> dataHolders = new ArrayList<>(1);
    fromPosition += mHeadOffset;
    toPosition += mHeadOffset;

    final ComponentContainer placeholder = mPlaceholders.remove(fromPosition);
    mPlaceholders.add(toPosition, placeholder);

    final Diff dataHolder = mDataHolders.remove(fromPosition);
    dataHolders.add(dataHolder);
    mDataHolders.add(toPosition, dataHolder);

    mOperations.add(Operation.acquire(Operation.MOVE, fromPosition, toPosition, null, dataHolders));
  }

  @Override
  public void onChanged(int position, int count, Object payload) {
    final List<ComponentContainer> placeholders = new ArrayList<>();
    final List<Diff> dataHolders = new ArrayList<>(count);

    position += mHeadOffset;
    for (int i = 0; i < count; i++) {
      final int index = position + i;
      final ComponentContainer placeholder = mPlaceholders.get(index);
      placeholder.mNeedsComputation = true;
      placeholders.add(placeholder);
      dataHolders.add(mDataHolders.get(index));
    }

    mOperations.add(Operation.acquire(Operation.UPDATE, position, -1, placeholders, dataHolders));
  }

  public void applyChangeset(ComponentContext c) {
    final boolean isTracing = ComponentsSystrace.isTracing();

    if (mNextData != null && mNextData.size() != mPlaceholders.size()) {
      logErrorForInconsistentSize(c);

      // Release mPlaceholders and mOperations since they aren't matching with mNextData anymore.
      for (int i = 0, size = mPlaceholders.size(); i < size; i++) {
        mPlaceholders.get(i).release();
      }
      for (int i = 0, size = mOperations.size(); i < size; i++) {
        mOperations.get(i).release();
      }
      mOperations.clear();
      mDataHolders.clear();

      final List<Diff> prevDataHolders = new ArrayList<>();
      for (int i = 0; i < mOldDataSize; i++) {
        prevDataHolders.add(new Diff(mPrevData.get(i), null));
      }
      mDataHolders.addAll(prevDataHolders);
      mOperations.add(Operation.acquire(Operation.DELETE, 0, mOldDataSize, null, prevDataHolders));

      final int dataSize = mNextData.size();
      final List<ComponentContainer> placeholders = new ArrayList<>(dataSize);
      final List<Diff> dataHolders = new ArrayList<>(dataSize);
      for (int i = 0; i < dataSize; i++) {
        final Object model = mNextData.get(i);
        final ComponentContainer componentContainer = ComponentContainer.acquire();
        if (isTracing) {
          ComponentsSystrace.beginSection("renderInfo:" + getModelName(model));
        }
        componentContainer.mRenderInfo = mComponentRenderer.render(model, i);
        if (isTracing) {
          ComponentsSystrace.endSection();
        }
        placeholders.add(i, componentContainer);
        dataHolders.add(new Diff(null, model));
      }
      mPlaceholders = placeholders;
      mDataHolders.addAll(dataHolders);
      mOperations.add(Operation.acquire(Operation.INSERT, 0, -1, placeholders, dataHolders));
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

    private static final SynchronizedPool<Operation>
        sOperationsPool = new SynchronizedPool<>(8);

    public static final int INSERT = 0;
    public static final int UPDATE = 1;
    public static final int DELETE = 2;
    public static final int MOVE = 3;

    private int mType;
    private int mIndex;
    private int mToIndex;
    private List<ComponentContainer> mComponentContainers;
    private List<Diff> mDataContainers;

    private Operation() {
    }

    private void init(
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

    private static Operation acquire(
        int type,
        int index,
        int toIndex,
        List<ComponentContainer> placeholder,
        List<Diff> dataHolders) {
      Operation operation = sOperationsPool.acquire();
      if (operation == null) {
        operation = new Operation();
      }
      operation.init(type, index, toIndex, placeholder, dataHolders);

      return operation;
    }

    private void release() {
      if (mComponentContainers != null) {
        mComponentContainers.clear();
        mComponentContainers = null;
      }

      sOperationsPool.release(this);
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
    private static final SynchronizedPool<ComponentContainer> sComponentContainerPool =
        new SynchronizedPool<>(8);

    private RenderInfo mRenderInfo;
    private boolean mNeedsComputation = false;

    public static ComponentContainer acquire() {
      ComponentContainer componentContainer = sComponentContainerPool.acquire();
      if (componentContainer == null) {
        componentContainer = new ComponentContainer();
      }

      return componentContainer;
    }

    public void release() {
      mRenderInfo = null;
      mNeedsComputation = false;
      sComponentContainerPool.release(this);
    }

    public RenderInfo getRenderInfo() {
      return mRenderInfo;
    }
  }
}
