/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.support.annotation.VisibleForTesting;
import android.support.v4.util.SimpleArrayMap;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * A container that stores working range related information. It provides two major methods: a
 * register method to store a working range with a component, and a dispatch method that dispatches
 * event to components to trigger their delegated methods.
 */
class WorkingRangeContainer {

  /**
   * Use {@link SimpleArrayMap} to store the working range of each component. The key is composed
   * with name and working range hashcode. The value is a {@link RangeTuple} object that contains a
   * working range related information.
   */
  @Nullable private SimpleArrayMap<String, RangeTuple> mWorkingRanges;

  void registerWorkingRange(String name, WorkingRange workingRange, Component component) {
    if (mWorkingRanges == null) {
      mWorkingRanges = new SimpleArrayMap<>();
    }

    final String key = name + "_" + workingRange.hashCode();
    final RangeTuple rangeTuple = mWorkingRanges.get(key);
    if (rangeTuple == null) {
      mWorkingRanges.put(key, new RangeTuple(name, workingRange, component));
    } else {
      rangeTuple.addComponent(component);
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

    for (int i = 0, size = mWorkingRanges.size(); i < size; i++) {
      final String key = mWorkingRanges.keyAt(i);
      final RangeTuple rangeTuple = mWorkingRanges.get(key);

      for (Component component : rangeTuple.mComponents) {
        if (!statusHandler.isInRange(rangeTuple.mName, component)
            && isEnteringRange(
                rangeTuple.mWorkingRange,
                position,
                firstVisibleIndex,
                lastVisibleIndex,
                firstFullyVisibleIndex,
                lastFullyVisibleIndex)) {
          component.dispatchOnEnteredRange(rangeTuple.mName);
          statusHandler.setEnteredRangeStatus(rangeTuple.mName, component);

        } else if (statusHandler.isInRange(rangeTuple.mName, component)
            && isExitingRange(
                rangeTuple.mWorkingRange,
                position,
                firstVisibleIndex,
                lastVisibleIndex,
                firstFullyVisibleIndex,
                lastFullyVisibleIndex)) {
          component.dispatchOnExitedRange(rangeTuple.mName);
          statusHandler.setExitedRangeStatus(rangeTuple.mName, component);
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

    for (int i = 0, size = mWorkingRanges.size(); i < size; i++) {
      final String key = mWorkingRanges.keyAt(i);
      final RangeTuple rangeTuple = mWorkingRanges.get(key);

      for (Component component : rangeTuple.mComponents) {
        if (statusHandler.isInRange(rangeTuple.mName, component)) {
          component.dispatchOnExitedRange(rangeTuple.mName);
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
  SimpleArrayMap<String, RangeTuple> getWorkingRangesForTestOnly() {
    return (mWorkingRanges != null) ? mWorkingRanges : new SimpleArrayMap<String, RangeTuple>();
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

    RangeTuple(String name, WorkingRange workingRange, Component component) {
      mName = name;
      mWorkingRange = workingRange;
      mComponents = new ArrayList<>();
      mComponents.add(component);
    }

    void addComponent(Component component) {
      mComponents.add(component);
    }
  }

  /** A tuple that stores raw data of a working range registration. */
  static class Registration {
    final String mName;
    final WorkingRange mWorkingRange;
    final Component mComponent;

    Registration(String name, WorkingRange workingRange, Component component) {
      mName = name;
      mWorkingRange = workingRange;
      mComponent = component;
    }
  }
}
