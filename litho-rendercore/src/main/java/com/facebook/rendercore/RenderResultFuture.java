// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.rendercore;

import android.content.Context;
import android.os.Process;
import androidx.annotation.Nullable;
import com.facebook.rendercore.extensions.RenderCoreExtension;
import com.facebook.rendercore.utils.ThreadUtils;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

public class RenderResultFuture<State, RenderContext> {

  private final RenderState.LazyTree<State> mLazyTree;
  private final RenderResult<State> mPreviousResult;
  private final int mSetRootId;
  private final int mWidthSpec;
  private final int mHeightSpec;
  private final FutureTask<RenderResult<State>> mFutureTask;
  private final AtomicInteger mRunningThreadId = new AtomicInteger(-1);

  public RenderResultFuture(
      final Context context,
      RenderState.LazyTree<State> lazyTree,
      final @Nullable RenderContext renderContext,
      final @Nullable RenderCoreExtension<?>[] extensions,
      final @Nullable RenderResult<State> previousResult,
      final int setRootId,
      final int widthSpec,
      final int heightSpec) {
    mLazyTree = lazyTree;
    mPreviousResult = previousResult;
    mSetRootId = setRootId;
    mWidthSpec = widthSpec;
    mHeightSpec = heightSpec;
    mFutureTask =
        new FutureTask<>(
            new Callable<RenderResult<State>>() {
              @Override
              public RenderResult<State> call() {
                return RenderResult.resolve(
                    context,
                    mLazyTree,
                    renderContext,
                    extensions,
                    mPreviousResult,
                    mSetRootId,
                    mWidthSpec,
                    mHeightSpec);
              }
            });
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
      }
    }

    return ThreadUtils.getResultInheritingPriority(mFutureTask, mRunningThreadId.get());
  }

  public boolean isDone() {
    return mFutureTask.isDone();
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
