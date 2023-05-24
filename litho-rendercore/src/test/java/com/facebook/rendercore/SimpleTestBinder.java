// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.rendercore;

import android.content.Context;
import androidx.annotation.Nullable;

final class SimpleTestBinder implements RenderUnit.Binder<Object, Object, Void> {
  final Runnable mMountRunnable;

  SimpleTestBinder(Runnable mountRunnable) {
    mMountRunnable = mountRunnable;
  }

  @Override
  public boolean shouldUpdate(
      Object currentModel,
      Object newModel,
      @Nullable Object currentLayoutData,
      @Nullable Object nextLayoutData) {
    return true;
  }

  @Override
  public Void bind(Context context, Object o, Object o2, @Nullable Object layoutData) {
    mMountRunnable.run();
    return null;
  }

  @Override
  public void unbind(
      Context context, Object o, Object o2, @Nullable Object layoutData, Void bindData) {}
}
