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

package com.facebook.rendercore;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import com.facebook.infer.annotation.Nullsafe;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class BackgroundPreparer {

  public interface Configuration {

    int threadPriority();

    boolean priorityBoostingEnabled();
  }

  public interface RenderResultFutureProvider<State, RenderContext> {

    RenderResultFuture<State, RenderContext> getRenderResultFuture();
  }

  private static final int DEFAULT_BACKGROUND_THREAD_PRIORITY = 5;
  public static final String THREAD_NAME = "BackgroundLayoutPreparer";
  private static final List<LayoutPreparerRunnable> sActiveRunnables = new ArrayList<>();
  @Nullable private static Handler sDefaultHandler;
  private static int sThreadId = -1;
  private static int sThreadPriority = DEFAULT_BACKGROUND_THREAD_PRIORITY;
  private static volatile boolean sIsBoosted = false;

  @Nullable private static Configuration sConfiguration = null;

  public static synchronized Handler getDefaultLayoutThreadHandler() {
    int threadPriorityToSet = sThreadPriority;

    if (sConfiguration != null && sConfiguration.priorityBoostingEnabled()) {
      threadPriorityToSet = sConfiguration.threadPriority();
      sIsBoosted = true;

      if (sThreadId != -1
          && threadPriorityToSet != -1
          && threadPriorityToSet != Process.getThreadPriority(sThreadId)) {
        Process.setThreadPriority(sThreadId, threadPriorityToSet);
      }
    } else if (sIsBoosted) {
      Process.setThreadPriority(sThreadId, DEFAULT_BACKGROUND_THREAD_PRIORITY);
      sIsBoosted = false;
    }

    if (sDefaultHandler == null) {
      final HandlerThread handlerThread = new HandlerThread(THREAD_NAME, threadPriorityToSet);
      handlerThread.start();
      // NULLSAFE_FIXME[Parameter Not Nullable]
      sDefaultHandler = new Handler(handlerThread.getLooper());
      sThreadId = handlerThread.getThreadId();
    }

    return sDefaultHandler;
  }

  public static synchronized void setThreadPriority(int threadPriority) {
    sThreadPriority = threadPriority;
    if (sThreadId != -1) {
      Process.setThreadPriority(sThreadId, threadPriority);
    }
  }

  public static synchronized void setConfiguration(@Nullable Configuration configuration) {
    sConfiguration = configuration;
  }

  public static synchronized <T extends RenderResultFutureProvider<?, ?>> CancellationToken enqueue(
      int clientId, List<T> components, boolean reverseOrder) {
    final Handler layoutHandler = getDefaultLayoutThreadHandler();
    Iterator<LayoutPreparerRunnable> iterator = sActiveRunnables.iterator();
    while (iterator.hasNext()) {
      final LayoutPreparerRunnable layoutPreparerRunnable = iterator.next();
      if (layoutPreparerRunnable.mId == clientId) {
        layoutPreparerRunnable.mCancellationToken.cancel();
      }

      if (layoutPreparerRunnable.mCancellationToken.isCancelled()) {
        layoutHandler.removeCallbacksAndMessages(layoutPreparerRunnable.mCancellationToken);
        iterator.remove();
      }
    }

    final CancellationToken token = new CancellationToken();
    final LayoutPreparerRunnable preparerRunnable =
        new LayoutPreparerRunnable(clientId, components, token, reverseOrder);
    sActiveRunnables.add(preparerRunnable);

    layoutHandler.postAtTime(
        () -> {
          preparerRunnable.run();
          remove(preparerRunnable);
        },
        token,
        // This delay value ensures that the enqueued runnable is always prioritized over other
        // runnables. Do NOT change this value without appropriately experimenting otherwise you
        // could regress rendering performance! See D91345194 for more details.
        0);

    return token;
  }

  private static synchronized void remove(LayoutPreparerRunnable runnable) {
    sActiveRunnables.remove(runnable);
  }

  public static final class CancellationToken {
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public void cancel() {
      cancelled.compareAndSet(false, true);
    }

    public boolean isCancelled() {
      return cancelled.get();
    }
  }

  private static final class LayoutPreparerRunnable<T extends RenderResultFutureProvider>
      implements Runnable {

    private final List<T> mItemsToLayout;
    private final int mId;
    private final boolean mReverseOrder;
    private final CancellationToken mCancellationToken;

    private LayoutPreparerRunnable(
        int id, List<T> itemsToLayout, CancellationToken token, boolean reverse) {
      mId = id;
      mItemsToLayout = itemsToLayout;
      mCancellationToken = token;
      mReverseOrder = reverse;
    }

    @Override
    public void run() {
      RenderCoreSystrace.beginSection("LayoutPreparer:run " + "[" + mId + "]");
      try {
        if (mReverseOrder) {
          for (int i = mItemsToLayout.size() - 1; i >= 0; i--) {
            if (mCancellationToken.isCancelled()) {
              return;
            }
            prepareFutureIfNeeded(mItemsToLayout.get(i).getRenderResultFuture());
          }
        } else {
          for (int i = 0; i < mItemsToLayout.size(); i++) {
            if (mCancellationToken.isCancelled()) {
              return;
            }
            prepareFutureIfNeeded(mItemsToLayout.get(i).getRenderResultFuture());
          }
        }
      } finally {
        RenderCoreSystrace.endSection();
      }
    }

    private static void prepareFutureIfNeeded(RenderResultFuture<?, ?> future) {
      if (!future.isCanceled() && !future.isDone() && !future.isRunning()) {
        future.runAndGet();
      }
    }
  }
}
