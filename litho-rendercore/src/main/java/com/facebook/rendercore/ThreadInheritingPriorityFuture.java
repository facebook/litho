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

import android.os.Process;
import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.instrumentation.FutureInstrumenter;
import com.facebook.rendercore.utils.ThreadUtils;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A future that lets the thread running the computation inherit the priority of any thread waiting
 * on it (if greater).
 *
 * @param <T> The type that is returned from the future
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class ThreadInheritingPriorityFuture<T> {
  private @Nullable RunnableFuture<T> mFutureTask;
  private @Nullable T mResolvedResult;
  private final AtomicInteger mRunningThreadId = new AtomicInteger(-1);

  public ThreadInheritingPriorityFuture(Callable<T> callable, String tag) {
    this.mFutureTask = FutureInstrumenter.instrument(new FutureTask<>(callable), tag);
  }

  public final T runAndGet() {
    final RunnableFuture<T> runnableFuture;
    T result;
    synchronized (this) {
      runnableFuture = mFutureTask;
      result = mResolvedResult;
    }

    if (result != null) {
      return result;
    }
    Objects.requireNonNull(runnableFuture);

    if (mRunningThreadId.compareAndSet(-1, Process.myTid())) {
      runnableFuture.run();
    }

    result = ThreadUtils.getResultInheritingPriority(runnableFuture, mRunningThreadId.get());

    synchronized (this) {
      mResolvedResult = result;
      mFutureTask = null;
    }

    return result;
  }

  public final boolean isRunning() {
    return mRunningThreadId.get() != -1;
  }

  public final boolean isDone() {
    RunnableFuture<T> futureTask;
    synchronized (this) {
      futureTask = mFutureTask;
    }
    return futureTask == null || futureTask.isDone();
  }

  public final void cancel() {
    RunnableFuture<T> futureTask;
    synchronized (this) {
      futureTask = mFutureTask;
    }
    if (futureTask != null) {
      futureTask.cancel(false);
    }
  }

  public final boolean isCanceled() {
    RunnableFuture<T> futureTask;
    synchronized (this) {
      futureTask = mFutureTask;
    }

    return futureTask != null && futureTask.isCancelled();
  }
}
