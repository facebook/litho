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

package com.facebook.litho;

import android.util.SparseIntArray;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ScopedComponentInfo implements Cloneable {

  // Can be final if Component is stateless and cloning is not needed anymore.
  final Component mComponent;
  private ComponentContext mContext;
  private @Nullable StateContainer mStateContainer;
  private @Nullable InterStagePropsContainer mInterStagePropsContainer;

  /**
   * Holds onto how many direct component children of each type this Component has. Used for
   * automatically generating unique global keys for all sibling components of the same type.
   */
  private @Nullable SparseIntArray mChildCounters;

  /** Count the times a manual key is used so that clashes can be resolved. */
  private @Nullable Map<String, Integer> mManualKeysCounter;

  /**
   * Holds a list of working range related data. {@link LayoutState} will use it to update {@link
   * LayoutState#mWorkingRangeContainer} when calculate method is finished.
   */
  private @Nullable List<WorkingRangeContainer.Registration> mWorkingRangeRegistrations;

  /**
   * Holds an event handler with its dispatcher set to the parent component, or - in case that this
   * is a root component - a default handler that reraises the exception.
   */
  private @Nullable EventHandler<ErrorEvent> mErrorEventHandler;

  ScopedComponentInfo(
      final Component component,
      final ComponentContext context,
      final @Nullable EventHandler<ErrorEvent> errorEventHandler) {
    mComponent = component;
    mContext = context;
    mStateContainer = component.createStateContainer();
    mInterStagePropsContainer = component.createInterStagePropsContainer();
    mErrorEventHandler = errorEventHandler;
  }

  public ComponentContext getContext() {
    return mContext;
  }

  @Nullable
  StateContainer getStateContainer() {
    return mStateContainer;
  }

  void setStateContainer(StateContainer stateContainer) {
    mStateContainer = stateContainer;
  }

  /**
   * Returns the number of children of a given type {@param component} component has and then
   * increments it by 1.
   *
   * @param component the child component
   * @return the number of children components of type {@param component}
   */
  int getChildCountAndIncrement(final Component component) {
    if (mChildCounters == null) {
      mChildCounters = new SparseIntArray();
    }
    final int count = mChildCounters.get(component.getTypeId(), 0);
    mChildCounters.put(component.getTypeId(), count + 1);

    return count;
  }

  /**
   * Returns the number of children with same {@param manualKey} component has and then increments
   * it by 1.
   *
   * @param manualKey
   * @return
   */
  int getManualKeyUsagesCountAndIncrement(String manualKey) {
    if (mManualKeysCounter == null) {
      mManualKeysCounter = new HashMap<>();
    }
    int manualKeyIndex =
        mManualKeysCounter.containsKey(manualKey) ? mManualKeysCounter.get(manualKey) : 0;
    mManualKeysCounter.put(manualKey, manualKeyIndex + 1);

    return manualKeyIndex;
  }

  @Nullable
  InterStagePropsContainer getInterStagePropsContainer() {
    return mInterStagePropsContainer;
  }

  /** Store a working range information into a list for later use by {@link LayoutState}. */
  void registerWorkingRange(
      String name, WorkingRange workingRange, Component component, String globalKey) {
    if (mWorkingRangeRegistrations == null) {
      mWorkingRangeRegistrations = new ArrayList<>();
    }
    mWorkingRangeRegistrations.add(
        new WorkingRangeContainer.Registration(name, workingRange, component, globalKey));
  }

  void addWorkingRangeToNode(InternalNode node) {
    if (mWorkingRangeRegistrations != null && !mWorkingRangeRegistrations.isEmpty()) {
      node.addWorkingRanges(mWorkingRangeRegistrations);
    }
  }

  /**
   * @return The error handler dispatching to either the parent component if available, or reraising
   *     the exception. Null if the component isn't initialized.
   */
  @Nullable
  EventHandler<ErrorEvent> getErrorEventHandler() {
    return mErrorEventHandler;
  }

  @Override
  protected ScopedComponentInfo clone() {
    try {
      return (ScopedComponentInfo) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Create a copy of this ScopedInfo, in which the StateContainer and InterStagePropsContainer are
   * copied.
   */
  ScopedComponentInfo copy(final LayoutStateContext context, final StateHandler handler) {
    final ScopedComponentInfo clone = clone();

    clone.mStateContainer = mComponent.createStateContainer();
    clone.mComponent.transferState(mStateContainer, clone.mStateContainer);

    clone.mInterStagePropsContainer = mComponent.createInterStagePropsContainer();
    clone.mComponent.copyInterStageImpl(clone.mInterStagePropsContainer, mInterStagePropsContainer);

    clone.mContext = mContext.createUpdatedComponentContext(context, handler);

    return clone;
  }

  /**
   * Transfer the contents of this ScopedInfo to {@param info}. This should only be used when
   * replacing an existing ScopedInfo with a new instance during the same layout pass. Currently,
   * the only unknown usecase is during reconciliation.
   *
   * @param info The destination ScopedInfo.
   */
  void transferInto(ScopedComponentInfo info) {
    info.mInterStagePropsContainer = mInterStagePropsContainer;
  }
}
