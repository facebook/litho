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

import android.os.Looper;
import android.os.Process;
import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.config.ComponentsConfiguration;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Nullsafe(Nullsafe.Mode.LOCAL)
class LayoutThreadFactory implements ThreadFactory {
  private static final AtomicInteger threadPoolId = new AtomicInteger(1);
  private final AtomicInteger threadNumber = new AtomicInteger(1);
  private int mThreadPriority;
  private final int mThreadPoolId;
  private final @Nullable Runnable mLayoutThreadInitializer;

  public LayoutThreadFactory(int threadPriority, @Nullable Runnable layoutThreadInitializer) {
    mThreadPriority = threadPriority;
    mThreadPoolId = threadPoolId.getAndIncrement();
    mLayoutThreadInitializer = layoutThreadInitializer;
  }

  @Override
  public Thread newThread(final Runnable r) {
    final Runnable wrapperRunnable =
        () -> {
          if (ComponentsConfiguration.runLooperPrepareForLayoutThreadFactory
              && Looper.myLooper() == null) {
            Looper.prepare();
          }

          try {
            Process.setThreadPriority(mThreadPriority);
          } catch (SecurityException e) {
            /**
             * From {@link Process#THREAD_PRIORITY_DISPLAY}, some applications can not change the
             * thread priority to that of the main thread. This catches that potential error and
             * tries to set a lower priority.
             */
            Process.setThreadPriority(mThreadPriority + 1);
          }
          if (mLayoutThreadInitializer != null) {
            mLayoutThreadInitializer.run();
          }
          r.run();
        };

    final Thread thread =
        new Thread(
            wrapperRunnable,
            ComponentTree.DEFAULT_LAYOUT_THREAD_NAME
                + mThreadPoolId
                + "-"
                + threadNumber.getAndIncrement());
    thread.setPriority(Thread.MAX_PRIORITY);

    return thread;
  }
}
