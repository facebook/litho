/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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
import com.facebook.proguard.annotations.DoNotStrip;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScopedComponentInfo implements Cloneable {

  private final Component mComponent;
  private ComponentContext mContext;
  private @Nullable StateContainer mStateContainer;
  private @Nullable PrepareInterStagePropsContainer mPrepareInterStagePropsContainer;

  /**
   * Holds onto how many direct component children of each type this Component has. Used for
   * automatically generating unique global keys for all sibling components of the same type.
   */
  private @Nullable SparseIntArray mChildCounters;

  /** Count the times a manual key is used so that clashes can be resolved. */
  @DoNotStrip private @Nullable Map<String, Integer> mManualKeysCounter;

  /**
   * Holds an event handler with its dispatcher set to the parent component, or - in case that this
   * is a root component - a default handler that reraises the exception.
   */
  private @Nullable EventHandler<ErrorEvent> mErrorEventHandler;

  /**
   * Holds a list of working range related data. {@link LayoutState} will use it to update {@link
   * LayoutState#mWorkingRangeContainer} when calculate method is finished.
   */
  private @Nullable List<WorkingRangeContainer.Registration> mWorkingRangeRegistrations;

  ScopedComponentInfo(
      final Component component,
      final ComponentContext context,
      final @Nullable EventHandler<ErrorEvent> errorEventHandler) {
    mComponent = component;
    mContext = context;
    mStateContainer =
        component instanceof SpecGeneratedComponent
            ? ((SpecGeneratedComponent) component).createStateContainer()
            : null;
    mPrepareInterStagePropsContainer =
        component instanceof SpecGeneratedComponent
            ? ((SpecGeneratedComponent) component).createPrepareInterStagePropsContainer()
            : null;
    mErrorEventHandler = errorEventHandler;
  }

  public Component getComponent() {
    return mComponent;
  }

  public ComponentContext getContext() {
    return mContext;
  }

  @Nullable
  public StateContainer getStateContainer() {
    return mStateContainer;
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
      mChildCounters = new SparseIntArray(1);
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
      mManualKeysCounter = new HashMap<>(1);
    }
    Integer count = mManualKeysCounter.get(manualKey);
    if (count == null) {
      count = 0;
    }

    mManualKeysCounter.put(manualKey, count + 1);

    return count;
  }

  @Nullable
  PrepareInterStagePropsContainer getPrepareInterStagePropsContainer() {
    return mPrepareInterStagePropsContainer;
  }

  /** Store a working range information into a list for later use by {@link LayoutState}. */
  void registerWorkingRange(
      String name, WorkingRange workingRange, Component component, String globalKey) {
    if (mWorkingRangeRegistrations == null) {
      mWorkingRangeRegistrations = new ArrayList<>();
    }
    mWorkingRangeRegistrations.add(
        new WorkingRangeContainer.Registration(name, workingRange, this));
  }

  void addWorkingRangeToNode(LithoNode node) {
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

  /** This setter should only be called during the render phase of the component, never after. */
  final void setErrorEventHandlerDuringRender(EventHandler<ErrorEvent> errorHandler) {
    mErrorEventHandler = errorHandler;
  }

  public void commitToLayoutState(final TreeState treeState) {
    if (mComponent.usesLocalStateContainer()) {
      if (hasState()) {
        treeState.addStateContainer(
            mContext.getGlobalKey(), mStateContainer, mContext.isNestedTreeContext());
      }
    } else {
      // the get method adds the state container to the needed state container map
      treeState.keepStateContainerForGlobalKey(
          mContext.getGlobalKey(), mContext.isNestedTreeContext());
    }
  }

  /**
   * Prepares a component for calling any pending state updates on it by setting the TreeProps which
   * the component requires from its parent, setting a scoped component context and applies the
   * pending state updates.
   */
  void applyStateUpdates(final TreeState treeState) {
    if (mComponent.usesLocalStateContainer()) {
      if (hasState()) {
        treeState.applyStateUpdatesForComponent(mContext, mComponent, mContext.getGlobalKey());
      }

    } else {
      // adds the global key to the needed state container global keys
      treeState.keepStateContainerForGlobalKey(
          mContext.getGlobalKey(), mContext.isNestedTreeContext());
    }
  }

  private boolean hasState() {
    return mComponent instanceof SpecGeneratedComponent
        && ((SpecGeneratedComponent) mComponent).hasState();
  }

  @Override
  protected ScopedComponentInfo clone() {
    try {
      return (ScopedComponentInfo) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }
}
