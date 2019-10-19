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

package com.facebook.litho.widget;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.widget.RecyclerBinderUpdateCallback.ComponentContainer;
import com.facebook.litho.widget.RecyclerBinderUpdateCallback.Operation;
import com.facebook.litho.widget.RecyclerBinderUpdateCallback.OperationExecutor;
import java.util.ArrayList;
import java.util.List;

/** An implementation of {@link OperationExecutor} that uses {@link RecyclerBinder}. */
public class RecyclerBinderOperationExecutor implements OperationExecutor {

  private final RecyclerBinder mRecyclerBinder;

  public RecyclerBinderOperationExecutor(RecyclerBinder recyclerBinder) {
    mRecyclerBinder = recyclerBinder;
  }

  @Override
  public void executeOperations(ComponentContext c, List<Operation> operations) {
    for (int i = 0, size = operations.size(); i < size; i++) {
      final Operation operation = operations.get(i);
      final List<ComponentContainer> components = operation.getComponentContainers();
      List<RenderInfo> renderInfos = null;
      if (components != null && components.size() > 1) {
        renderInfos = new ArrayList<>();
        for (int j = 0, componentsSize = components.size(); j < componentsSize; j++) {
          renderInfos.add(components.get(j).getRenderInfo());
        }
      }

      switch (operation.getType()) {
        case Operation.INSERT:
          if (renderInfos != null) {
            mRecyclerBinder.insertRangeAt(operation.getIndex(), renderInfos);
          } else {
            mRecyclerBinder.insertItemAt(
                operation.getIndex(), operation.getComponentContainers().get(0).getRenderInfo());
          }
          break;

        case Operation.DELETE:
          mRecyclerBinder.removeRangeAt(operation.getIndex(), operation.getToIndex());
          break;

        case Operation.MOVE:
          mRecyclerBinder.moveItem(operation.getIndex(), operation.getToIndex());
          break;

        case Operation.UPDATE:
          if (renderInfos != null) {
            mRecyclerBinder.updateRangeAt(operation.getIndex(), renderInfos);
          } else {
            mRecyclerBinder.updateItemAt(
                operation.getIndex(), operation.getComponentContainers().get(0).getRenderInfo());
          }
          break;
      }
    }

    mRecyclerBinder.notifyChangeSetComplete(
        true,
        new ChangeSetCompleteCallback() {
          @Override
          public void onDataBound() {
            // Do nothing.
          }

          @Override
          public void onDataRendered(boolean isMounted, long uptimeMillis) {
            // Do nothing.
          }
        });
  }
}
