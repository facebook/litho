/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.List;

import android.annotation.TargetApi;
import android.support.annotation.IntDef;
import android.support.v4.util.SimpleArrayMap;
import android.view.View;
import android.view.ViewParent;

import com.facebook.litho.TransitionKeySet.TransitionKeySetListener;

import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;

/**
 * Unique per MountState instance. It's getting called from MountState on every re-mount
 * to process the transition keys and handles which transitions to run and when.
 */
@TargetApi(ICE_CREAM_SANDWICH)
class TransitionManager implements TransitionKeySetListener {

  @IntDef({KeyStatus.APPEARED, KeyStatus.UNCHANGED, KeyStatus.DISAPPEARED})
  @Retention(RetentionPolicy.SOURCE)
  @interface KeyStatus {
    int APPEARED = 0;
    int UNCHANGED = 1;
    int DISAPPEARED = 2;
  }

  private HashSet<String> mPostMountKeys;
  private SimpleArrayMap<String, Integer> mKeysStatus;
  private SimpleArrayMap<String, TransitionKeySet> mTransitions;
  private SimpleArrayMap<String, TransitionKeySet> mRunningTransitions;

  TransitionManager() {
    mPostMountKeys = new HashSet<>();
    mKeysStatus = new SimpleArrayMap<>();
    mTransitions = new SimpleArrayMap<>();
    mRunningTransitions = new SimpleArrayMap<>();
  }

  /**
   * The {@link MountState} was just set to dirty and is about to re-mount.
   * Stop all the transitions, setting their values to the end state before the
   * root {@link ComponentView} will go through re-layout.
   */
  void setDirty() {
    for (int i = 0, size = mRunningTransitions.size(); i < size; i++) {
      mRunningTransitions.valueAt(i).stop();
    }
  }

  void onNewTransitionContext(TransitionContext transitionContext) {
    mTransitions.clear();
    mTransitions.putAll(transitionContext.getTransitionKeySets());
  }

  /**
   * Called when the mount process of MountState is starting.
   */
  void onMountStart() {
    // Right now this is setDirty on all onMountStart however, to play nice with incremental mount
    // we should continue the transition unless something is unmounted.
    setDirty();
    mPostMountKeys.clear();
  }

  void onPreMountItem(String transitionKey, View view) {
    final TransitionKeySet t = mTransitions.get(transitionKey);
    if (t != null) {
      t.recordStartValues(view);
      t.setTargetView(view);
    }
  }

  /**
   * All the transitionKeys, and associated views, currently mounted at the end of the mount
   * process.
   */
  void onPostMountItem(String transitionKey, View view) {
    final TransitionKeySet t = mTransitions.get(transitionKey);
    if (t != null) {
      t.recordEndValues(view);
      t.setTargetView(view);
    }

    mPostMountKeys.add(transitionKey);
  }

  /**
   * Given the previous TransitionManager state and the new map of mounted keys added through
   * {@link #onPostMountItem(String, View)}, process the keys and the related Transitions.
   */
  void processTransitions() {
    // 1. Define which key appeared, disappeared or was unchanged from the previous state.
    for (int i = mKeysStatus.size() - 1; i >= 0; i--) {
      final String key = mKeysStatus.keyAt(i);
      final @KeyStatus Integer status = mKeysStatus.valueAt(i);

      if (mPostMountKeys.remove(key)) {
        // 1.1 Unchanged or Re-Appearing keys.
        if (status == KeyStatus.DISAPPEARED) {
          mKeysStatus.put(key, KeyStatus.APPEARED);
        } else {
          mKeysStatus.put(key, KeyStatus.UNCHANGED);
        }
      } else if (status != KeyStatus.DISAPPEARED) {
        // 1.2 Disappeared keys.
        mKeysStatus.put(key, KeyStatus.DISAPPEARED);
      } else {
        // NOTE: If we have a running disappear transition, we keep the key to continue the
        // animation.
      }
    }
    // 1.3 Appeared keys.
    for (String newKey : mPostMountKeys) {
      mKeysStatus.put(newKey, KeyStatus.APPEARED);
    }

    // 2. Process running transitions to resume or remove them.
    for (int i = mRunningTransitions.size() - 1; i >= 0; i--) {
      final String key = mRunningTransitions.keyAt(i);
      final TransitionKeySet runningTransitions = mRunningTransitions.removeAt(i);

      final TransitionKeySet newTransitions = mTransitions.remove(key);
      // Bail if there's not defined transition in the new layout for the same key.
      if (newTransitions == null) {
        continue;
      }

      if (newTransitions.resumeFrom(runningTransitions, mKeysStatus.get(key), this)) {
        mRunningTransitions.put(key, newTransitions);
      }
    }

    // 3. Start new transitions if needed.
    for (int i = mTransitions.size() - 1; i >= 0; i--) {
      final String key = mTransitions.keyAt(i);
      final TransitionKeySet transition = mTransitions.removeAt(i);
      final @KeyStatus Integer keyStatus = mKeysStatus.get(key);

      if (keyStatus == null) {
        continue;
      }

      if (transition.start(keyStatus, this)) {
        mRunningTransitions.put(key, transition);
      }
    }
  }

