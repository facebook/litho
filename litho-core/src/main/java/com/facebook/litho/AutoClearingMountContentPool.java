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

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.facebook.infer.annotation.Nullsafe;

/**
 * This pool exists to try to allow mount content to be recycled within a single MountState.mount()
 * call. The context is we currently have recycling completely disabled for some @MountSpec types
 * because of native RenderThread crashes. This class will help us determine whether the issue is
 * long-held mount content, or if even just recycling in a single frame is enough to trigger the
 * crash.
 *
 * <p>When isEnabled is false, this pool behaves like {@link DisabledMountContentPool}. Otherwise,
 * if shouldAutoClear is false, this pool behaves like a normal pool (i.e. to measure baseline
 * crashes). Finally, if both are true, this pool behaves like a normal pool, except that it will
 * clear the mount content as soon as the next main thread runnable can run.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class AutoClearingMountContentPool extends RecyclePool implements MountContentPool {

  private final boolean mIsEnabled;
  private final boolean mShouldAutoClear;
  private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());
  private boolean mIsClearPoolPending = false;

  public AutoClearingMountContentPool(
      String name, int maxSize, boolean isEnabled, boolean shouldAutoClear) {
    super(name, maxSize, true);
    mIsEnabled = isEnabled;
    mShouldAutoClear = shouldAutoClear;
  }

  @Override
  public Object acquire(Context c, Component component) {
    if (!mIsEnabled) {
      return component.createMountContent(c);
    }

    final Object fromPool = super.acquire();
    return fromPool != null ? fromPool : component.createMountContent(c);
  }

  @Override
  public void release(Object item) {
    if (!mIsEnabled) {
      return;
    }

    super.release(item);

    if (mShouldAutoClear && !mIsClearPoolPending) {
      mMainThreadHandler.post(
          new Runnable() {
            @Override
            public void run() {
              clear();
              mIsClearPoolPending = false;
            }
          });
      mIsClearPoolPending = true;
    }
  }

  @Override
  public void maybePreallocateContent(Context c, Component component) {
    // No-op as preallocation happens in an earlier frame
  }
}
