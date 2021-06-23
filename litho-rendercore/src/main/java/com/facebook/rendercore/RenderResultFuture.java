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

package com.facebook.rendercore;

import android.content.Context;
import android.os.Process;
import androidx.annotation.Nullable;
import com.facebook.rendercore.extensions.RenderCoreExtension;
import com.facebook.rendercore.instrumentation.FutureInstrumenter;
import com.facebook.rendercore.utils.ThreadUtils;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class RenderResultFuture<State, RenderContext> {

  private final int mSetRootId;
  private final int mWidthSpec;
  private final int mHeightSpec;
  private final RunnableFuture<RenderResult<State>> mFutureTask;
  private final AtomicInteger mRunningThreadId = new AtomicInteger(-1);
  private volatile @Nullable RenderResult<State> mPreviousResult;

  public RenderResultFuture(
      final Context context,
      final RenderState.LazyTree<State> lazyTree,
      final @Nullable RenderContext renderContext,
      final @Nullable RenderCoreExtension<?, ?>[] extensions,
      final @Nullable RenderResult<State> previousResult,
      final int setRootId,
      final int widthSpec,
      final int heightSpec) {
    mPreviousResult = previousResult;
    mSetRootId = setRootId;
    mWidthSpec = widthSpec;
    mHeightSpec = heightSpec;
    mFutureTask =
        FutureInstrumenter.instrument(
            new FutureTask<>(
                new Callable<RenderResult<State>>() {
                  @Override
                  public RenderResult<State> call() {
                    return RenderResult.resolve(
                        context,
                        lazyTree,
                        renderContext,
                        extensions,
                        mPreviousResult,
                        mSetRootId,
                        mWidthSpec,
                        mHeightSpec);
                  }
                }),
            "RenderResultFuture_resolve");
  }

  public RenderResult<State> runAndGet() {
    if (mRunningThreadId.compareAndSet(-1, Process.myTid())) {
      mFutureTask.run();
      try {
        return mFutureTask.get();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      } catch (ExecutionException e) {
        // Unwrap so that stacktraces are more clear in logs
        if (e.getCause() instanceof RuntimeException) {
          throw (RuntimeException) e.getCause();
        } else {
          throw new RuntimeException(e.getCause());
        }
      } finally {
        mPreviousResult = null;
      }
    }

    return ThreadUtils.getResultInheritingPriority(mFutureTask, mRunningThreadId.get());
  }

  @Nullable
  public RenderResult<State> getLatestAvailableRenderResult() {
    return isDone() ? runAndGet() : mPreviousResult;
  }

  public boolean isDone() {
    return mFutureTask.isDone();
  }

  public boolean isRunning() {
    return mRunningThreadId.get() != -1;
  }

  public void cancel() {
    mFutureTask.cancel(false);
  }

  public boolean isCanceled() {
    return mFutureTask.isCancelled();
  }

  public int getSetRootId() {
    return mSetRootId;
  }

  public int getWidthSpec() {
    return mWidthSpec;
  }

  public int getHeightSpec() {
    return mHeightSpec;
  }
}
