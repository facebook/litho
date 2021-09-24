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

import androidx.annotation.VisibleForTesting;
import androidx.core.util.Preconditions;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * A container that stores working range related information. It provides two major methods: a
 * register method to store a working range with a component, and a dispatch method that dispatches
 * event to components to trigger their delegated methods.
 */
class WorkingRangeContainer {

  /**
   * Use {@link java.util.HashMap} to store the working range of each component. The key is composed
   * with name and working range hashcode. The value is a {@link RangeTuple} object that contains a
   * working range related information.
   */
  @Nullable private Map<String, RangeTuple> mWorkingRanges;

  void registerWorkingRange(
      final String name,
      final WorkingRange workingRange,
      final Component component,
      final String globalKey,
      final @Nullable ScopedComponentInfo scopedComponentInfo) {
    if (mWorkingRanges == null) {
      mWorkingRanges = new LinkedHashMap<>();
    }

    final String key = name + "_" + workingRange.hashCode();
    final RangeTuple rangeTuple = mWorkingRanges.get(key);
    if (rangeTuple == null) {
      mWorkingRanges.put(
          key, new RangeTuple(name, workingRange, component, globalKey, scopedComponentInfo));
    } else {
      rangeTuple.addComponent(component, globalKey, scopedComponentInfo);
    }
  }

  /**
   * Iterate the map to check if a component is entered or exited the range, and dispatch event to
   * the component to trigger its delegate method.
   */
  void checkWorkingRangeAndDispatch(
      int position,
      int firstVisibleIndex,
      int lastVisibleIndex,
      int firstFullyVisibleIndex,
      int lastFullyVisibleIndex,
      WorkingRangeStatusHandler statusHandler) {
    if (mWorkingRanges == null) {
      return;
    }

    for (String key : mWorkingRanges.keySet()) {
      final RangeTuple rangeTuple = Preconditions.checkNotNull(mWorkingRanges.get(key));

      for (int i = 0, size = rangeTuple.mComponents.size(); i < size; i++) {
        Component component = rangeTuple.mComponents.get(i);
        String globalKey = rangeTuple.mComponentKeys.get(i);
        if (!statusHandler.isInRange(rangeTuple.mName, component, globalKey)
            && isEnteringRange(
                rangeTuple.mWorkingRange,
                position,
                firstVisibleIndex,
                lastVisibleIndex,
                firstFullyVisibleIndex,
                lastFullyVisibleIndex)) {
          ComponentContext scopedContext =
              rangeTuple.mScopedComponentInfos != null
                  ? rangeTuple.mScopedComponentInfos.get(i).getContext()
                  : component.getScopedContext();
          try {
            component.dispatchOnEnteredRange(scopedContext, rangeTuple.mName);
          } catch (Exception e) {
            ComponentUtils.handle(scopedContext, e);
          }
          statusHandler.setEnteredRangeStatus(rangeTuple.mName, component, globalKey);

        } else if (statusHandler.isInRange(rangeTuple.mName, component, globalKey)
            && isExitingRange(
                rangeTuple.mWorkingRange,
                position,
                firstVisibleIndex,
                lastVisibleIndex,
                firstFullyVisibleIndex,
                lastFullyVisibleIndex)) {
          final ComponentContext scopedContext =
              rangeTuple.mScopedComponentInfos != null
                  ? rangeTuple.mScopedComponentInfos.get(i).getContext()
                  : Preconditions.checkNotNull(component.getScopedContext());
          try {
            component.dispatchOnExitedRange(scopedContext, rangeTuple.mName);
          } catch (Exception e) {
            ComponentUtils.handle(scopedContext, e);
          }
          statusHandler.setExitedRangeStatus(rangeTuple.mName, component, globalKey);
        }
      }
    }
  }

  /**
   * Dispatch onExitRange if the status of the component is in the range. This method should only be
   * called when releasing a ComponentTree, thus no status update needed.
   */
  void dispatchOnExitedRangeIfNeeded(WorkingRangeStatusHandler statusHandler) {
    if (mWorkingRanges == null) {
      return;
    }

    for (String key : mWorkingRanges.keySet()) {
      final RangeTuple rangeTuple = Preconditions.checkNotNull(mWorkingRanges.get(key));

      for (int i = 0, size = rangeTuple.mComponents.size(); i < size; i++) {
        Component component = rangeTuple.mComponents.get(i);
        String globalKey = rangeTuple.mComponentKeys.get(i);
        if (statusHandler.isInRange(rangeTuple.mName, component, globalKey)) {
          ComponentContext scopedContext =
              rangeTuple.mScopedComponentInfos != null
                  ? rangeTuple.mScopedComponentInfos.get(i).getContext()
                  : Preconditions.checkNotNull(component.getScopedContext());
          try {
            component.dispatchOnExitedRange(scopedContext, rangeTuple.mName);
          } catch (Exception e) {
            ComponentUtils.handle(scopedContext, e);
          }
        }
      }
    }
  }

  static boolean isEnteringRange(
      WorkingRange workingRange,
      int position,
      int firstVisibleIndex,
      int lastVisibleIndex,
      int firstFullyVisibleIndex,
      int lastFullyVisibleIndex) {

    return workingRange.shouldEnterRange(
        position,
        firstVisibleIndex,
        lastVisibleIndex,
        firstFullyVisibleIndex,
        lastFullyVisibleIndex);
  }

  static boolean isExitingRange(
      WorkingRange workingRange,
      int position,
      int firstVisibleIndex,
      int lastVisibleIndex,
      int firstFullyVisibleIndex,
      int lastFullyVisibleIndex) {

    return workingRange.shouldExitRange(
        position,
        firstVisibleIndex,
        lastVisibleIndex,
        firstFullyVisibleIndex,
        lastFullyVisibleIndex);
  }

  @VisibleForTesting
  Map<String, RangeTuple> getWorkingRangesForTestOnly() {
    return (mWorkingRanges != null) ? mWorkingRanges : new LinkedHashMap<String, RangeTuple>();
  }

  /**
   * A tuple that stores working range information for a list of components that share same name and
   * working range object.
   */
  @VisibleForTesting
  static class RangeTuple {
    final String mName;
    final WorkingRange mWorkingRange;
    final List<Component> mComponents;
    final List<String> mComponentKeys;
    final @Nullable List<ScopedComponentInfo> mScopedComponentInfos;

    RangeTuple(
        final String name,
        final WorkingRange workingRange,
        final Component component,
        final String key,
        final @Nullable ScopedComponentInfo scopedComponentInfo) {
      mName = name;
      mWorkingRange = workingRange;
      mComponents = new ArrayList<>();
      mComponentKeys = new ArrayList<>();
      mComponents.add(component);
      mComponentKeys.add(key);

      if (scopedComponentInfo != null) {
        mScopedComponentInfos = new ArrayList<>();
        mScopedComponentInfos.add(scopedComponentInfo);
      } else {
        mScopedComponentInfos = null;
      }
    }

    void addComponent(
        final Component component,
        final String key,
        final @Nullable ScopedComponentInfo scopedComponentInfo) {
      mComponents.add(component);
      mComponentKeys.add(key);

      if (mScopedComponentInfos != null) {
        mScopedComponentInfos.add(scopedComponentInfo);
      }
    }
  }

  /** A tuple that stores raw data of a working range registration. */
  static class Registration {
    final String mName;
    final WorkingRange mWorkingRange;
    final Component mComponent;
    final String mKey;
    final @Nullable ScopedComponentInfo mScopedComponentInfo;

    Registration(
        final String name,
        final WorkingRange workingRange,
        final Component component,
        final String key,
        final @Nullable ScopedComponentInfo scopedComponentInfo) {
      mName = name;
      mWorkingRange = workingRange;
      mComponent = component;
      mKey = key;
      mScopedComponentInfo = scopedComponentInfo;
    }
  }
}
