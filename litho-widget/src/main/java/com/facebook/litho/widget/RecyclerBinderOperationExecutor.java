// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.widget;

import com.facebook.litho.ComponentInfo;
import com.facebook.litho.widget.RecyclerBinderUpdateCallback.ComponentContainer;
import com.facebook.litho.widget.RecyclerBinderUpdateCallback.Operation;
import com.facebook.litho.widget.RecyclerBinderUpdateCallback.OperationExecutor;
import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of {@link OperationExecutor} that uses {@link RecyclerBinder}.
 */
public class RecyclerBinderOperationExecutor implements OperationExecutor {

  private final RecyclerBinder mRecyclerBinder;

  public RecyclerBinderOperationExecutor(RecyclerBinder recyclerBinder) {
    mRecyclerBinder = recyclerBinder;
  }

  @Override
  public void executeOperations(List<Operation> operations) {
    for (int i = 0, size = operations.size(); i < size; i++) {
      final Operation operation = operations.get(i);
      final List<ComponentContainer> components = operation.getComponentContainers();
      List<ComponentInfo> componentInfos = null;
      if (components != null && components.size() > 1 ) {
        componentInfos = new ArrayList<>();
        for (int j = 0, componentsSize = components.size(); j < componentsSize; j++) {
          componentInfos.add(components.get(j).getComponentInfo());
        }
      }

      switch (operation.getType()) {

        case Operation.INSERT:
          if (componentInfos != null) {
            mRecyclerBinder.insertRangeAt(operation.getIndex(), componentInfos);
          } else {
            mRecyclerBinder.insertItemAt(
                operation.getIndex(),
                operation.getComponentContainers().get(0).getComponentInfo());
          }
          break;

        case Operation.DELETE:
          mRecyclerBinder.removeRangeAt(operation.getIndex(), operation.getToIndex());
          break;

        case Operation.MOVE:
          mRecyclerBinder.moveItem(operation.getIndex(), operation.getToIndex());
          break;

        case Operation.UPDATE:
          if (componentInfos != null) {
            mRecyclerBinder.updateRangeAt(operation.getIndex(), componentInfos);
          } else {
            mRecyclerBinder.updateItemAt(
                operation.getIndex(),
                operation.getComponentContainers().get(0).getComponentInfo());
          }
          break;
      }
    }
  }
}
