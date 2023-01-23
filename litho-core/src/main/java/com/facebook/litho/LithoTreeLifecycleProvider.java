// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.litho;

public interface LithoTreeLifecycleProvider {

  interface OnReleaseListener {

    /** Called when this ComponentTree is released. */
    void onReleased();
  }

  boolean isReleased();

  void addOnReleaseListener(OnReleaseListener onReleaseListener);
}
