// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import android.annotation.TargetApi;
import android.support.annotation.IntDef;
import android.view.View;

import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;

/**
 * Unique per MountState instance. It's getting called from MountState on every re-mount
 * to process the transition keys and handles which transitions to run and when.
 */
@TargetApi(ICE_CREAM_SANDWICH)
class TransitionManager {

  @IntDef({KeyStatus.APPEARED, KeyStatus.UNCHANGED, KeyStatus.DISAPPEARED})
  @Retention(RetentionPolicy.SOURCE)
  @interface KeyStatus {
    int APPEARED = 0;
    int UNCHANGED = 1;
    int DISAPPEARED = 2;
  }

  TransitionManager() {
  }

  /**
   * Called when the mount process of MountState is starting.
   */
  void onMountStart() {
  }

  /**
   * All the transitionKeys, and associated views, currently mounted at the end of the mount
   * process.
   */
  void onMountEndItems(String transitionKey, View view) {
  }

  /**
   * Given the previous TransitionManager state and the new map of mounted keys added through
   * {@link #onMountEndItems(String, View)}, process the keys and the related Transitions.
   */
  void processTransitions() {

  }

  void reset() {
  }
}
