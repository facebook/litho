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

import static android.os.Process.THREAD_PRIORITY_DEFAULT;
import static com.facebook.litho.LayoutState.isFromSyncLayout;
import static com.facebook.litho.ThreadUtils.isMainThread;
import static com.facebook.litho.WorkContinuationInstrumenter.markFailure;
import static com.facebook.litho.WorkContinuationInstrumenter.onBeginWorkContinuation;
import static com.facebook.litho.WorkContinuationInstrumenter.onEndWorkContinuation;
import static com.facebook.litho.WorkContinuationInstrumenter.onOfferWorkForContinuation;

import android.os.Process;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.rendercore.Systracer;
import com.facebook.rendercore.instrumentation.FutureInstrumenter;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base class that wraps a {@link FutureTask} to allow calculating the same result across threads.
 */
public abstract class TreeFuture<T extends PotentiallyPartialResult> {
  public static final String FUTURE_RESULT_NULL_REASON_ABORTED = "Aborted";
  public static final String FUTURE_RESULT_NULL_REASON_RELEASED = "TreeFuture released";
  public static final String FUTURE_RESULT_NULL_REASON_SYNC_RESULT_NON_MAIN_THREAD =
      "Waiting for sync result from non-main-thread";
  public static final String FUTURE_RESULT_NULL_REASON_RESUME_NON_MAIN_THREAD =
      "Resuming partial result skipped due to not being on main-thread";

  private static final int INTERRUPTIBLE = 0;
  private static final int INTERRUPTED = 1;
  private static final int NON_INTERRUPTIBLE = 2;

  protected final AtomicInteger mRunningThreadId = new AtomicInteger(-1);
  private final AtomicInteger mInterruptState = new AtomicInteger(INTERRUPTIBLE);
  private final AtomicInteger mRefCount = new AtomicInteger(0);

  private volatile @Nullable Object mInterruptToken;
  private volatile @Nullable Object mContinuationToken;
  private volatile boolean mReleased = false;

  protected final RunnableFuture<TreeFutureResult<T>> mFutureTask;
  protected final boolean mMoveOperationsBetweenThreads;

  public TreeFuture(boolean moveOperationsBetweenThreads) {
    mMoveOperationsBetweenThreads = moveOperationsBetweenThreads;
    this.mFutureTask =
        FutureInstrumenter.instrument(
            new FutureTask<>(
                new Callable<TreeFutureResult<T>>() {
                  @Override
                  public @Nullable TreeFutureResult<T> call() {
                    synchronized (TreeFuture.this) {
                      if (mReleased) {
                        return new TreeFutureResult<T>(FUTURE_RESULT_NULL_REASON_RELEASED);
                      }
                    }
                    final T result = calculate();
                    synchronized (TreeFuture.this) {
                      if (mReleased) {
                        return new TreeFutureResult<T>(FUTURE_RESULT_NULL_REASON_RELEASED);
                      } else {
                        return new TreeFutureResult<T>(result);
                      }
                    }
                  }
                }),
            "TreeFuture_calculateResult");
  }

  /** Calculates a new result for this TreeFuture. */
  protected abstract T calculate();

  /** Resumes an interrupted calculation based on a partial result */
  @Nullable
  protected abstract T resumeCalculation(T partialResult);

  /** Returns true if the provided TreeFuture is equivalent to this one. */
  public abstract boolean isEquivalentTo(TreeFuture that);

  /** Releases this TreeFuture */
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  synchronized void release() {
    if (mReleased) {
      return;
    }
    mInterruptToken = null;
    mContinuationToken = null;
    mReleased = true;
  }

  /** Returns true if this future has been released. */
  boolean isReleased() {
    return mReleased;
  }

  /**
   * Returns true if an interrupt has been requested on this future, indicating the calculation must
   * be resumed on the main-thread.
   */
  boolean isInterruptRequested() {
    return mInterruptState.get() == INTERRUPTED;
  }

  void unregisterForResponse() {
    final int newRefCount = mRefCount.decrementAndGet();

    if (newRefCount < 0) {
      throw new IllegalStateException("TreeFuture ref count is below 0");
    }
  }

  /**
   * We want to prevent a sync layout in the background from waiting on an interrupted layout (which
   * will return a null result). To handle this, we make sure that a sync bg layout can only wait on
   * a NON_INTERRUPTIBLE future, and that a NON_INTERRUPTIBLE future can't be interrupted.
   *
   * <p>The usage of AtomicInteger for interrupt state is just to make it lockless.
   */
  boolean tryRegisterForResponse(boolean waitingFromSyncLayout) {
    if (waitingFromSyncLayout && mMoveOperationsBetweenThreads && !isMainThread()) {
      int state = mInterruptState.get();
      if (state == INTERRUPTED) {
        return false;
      }
      if (state == INTERRUPTIBLE) {
        if (!mInterruptState.compareAndSet(INTERRUPTIBLE, NON_INTERRUPTIBLE)
            && mInterruptState.get() != NON_INTERRUPTIBLE) {
          return false;
        }
      }
    }

    // If we haven't returned false by now, we are now marked NON_INTERRUPTIBLE so we're good to
    // wait on this future
    mRefCount.incrementAndGet();
    return true;
  }

  /** We only want to interrupt an INTERRUPTIBLE layout. */
  private boolean tryMoveToInterruptedState() {
    final int state = mInterruptState.get();
    switch (state) {
      case NON_INTERRUPTIBLE:
        return false;
      case INTERRUPTIBLE:
        if (!mInterruptState.compareAndSet(INTERRUPTIBLE, INTERRUPTED)
            && mInterruptState.get() != INTERRUPTED) {
          return false;
        }
        // fall through
      default:
        // If we haven't returned false by now, we are now marked INTERRUPTED so we're good to
        // interrupt.
        return true;
    }
  }

  public int getWaitingCount() {
    return mRefCount.get();
  }

  /**
   * Called before the future's Get is invoked. Will start a trace. Override to implement additional
   * logging before Get, but always call super.
   */
  protected void onGetStart(boolean isTracing) {
    if (isTracing) {
      startTrace("get");
    }
  }

  /**
   * Called after Get is completed. Will end a trace. Override to implement additional logging after
   * Get, but always call super.
   */
  protected void onGetEnd(boolean isTracing) {
    if (isTracing) {
      ComponentsSystrace.endSection();
    }
  }

  /**
   * Called before Wait starts. Will start a trace. Override to implement additional logging before
   * Wait, but always call super.
   */
  protected void onWaitStart(boolean isTracing) {
    if (isTracing) {
      startTrace("wait");
    }
  }

  /**
   * Called after Wait ends. Will end a trace. Override to implement additional logging after Wait,
   * but always call super.
   */
  protected void onWaitEnd(boolean isTracing, boolean errorOccurred) {
    if (isTracing) {
      ComponentsSystrace.endSection();
    }
  }

  /** Starts a sys-trace. The systrace's section will be named after the class name + a suffix. */
  private void startTrace(final String suffix) {
    addSystraceArgs(
            ComponentsSystrace.beginSectionWithArgs(getClass().getSimpleName() + "." + suffix))
        .arg("runningThreadId", mRunningThreadId.get())
        .flush();
  }

  /**
   * Override to add additional args to a systrace. The args will already include the running thread
   * ID.
   */
  protected Systracer.ArgsBuilder addSystraceArgs(final Systracer.ArgsBuilder argsBuilder) {
    return argsBuilder;
  }

  /**
   * Synchronously executes the future task, ensuring the result is calculated, and resumed on the
   * main thread if it is interrupted while running on a background thread.
   *
   * @param source The calculate-layout source, indicating whether or not it's running from a sync
   *     layout.
   * @return The expected result of calculation, or null if this future was released.
   */
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  TreeFutureResult<T> runAndGet(@LayoutState.CalculateLayoutSource final int source) {
    if (mRunningThreadId.compareAndSet(-1, Process.myTid())) {
      mFutureTask.run();
    }

    final int runningThreadId = this.mRunningThreadId.get();
    final boolean notRunningOnMyThread = runningThreadId != Process.myTid();
    final int originalThreadPriority;
    final boolean didRaiseThreadPriority;

    final boolean shouldWaitForResult = !mFutureTask.isDone() && notRunningOnMyThread;

    if (shouldWaitForResult && !isMainThread() && !isFromSyncLayout(source)) {
      return new TreeFutureResult<T>(FUTURE_RESULT_NULL_REASON_SYNC_RESULT_NON_MAIN_THREAD);
    }

    if (isMainThread() && shouldWaitForResult) {
      // This means the UI thread is about to be blocked by the bg thread. Instead of waiting,
      // the bg task is interrupted.
      if (mMoveOperationsBetweenThreads) {
        if (tryMoveToInterruptedState()) {
          mInterruptToken =
              WorkContinuationInstrumenter.onAskForWorkToContinue("interruptCalculate");
        }
      }

      originalThreadPriority =
          ThreadUtils.tryRaiseThreadPriority(runningThreadId, Process.THREAD_PRIORITY_DISPLAY);
      didRaiseThreadPriority = true;
    } else {
      originalThreadPriority = THREAD_PRIORITY_DEFAULT;
      didRaiseThreadPriority = false;
    }

    TreeFutureResult<T> treeFutureResult;

    final boolean shouldTrace = notRunningOnMyThread && ComponentsSystrace.isTracing();
    try {

      // TODO (T133699532): Do we really need 2 lifecycle events here? If not, we can get rid
      // of one of these (and equivalent onEnd).
      onGetStart(shouldTrace);
      onWaitStart(shouldTrace);

      // Calling mFutureTask.get() - One of two things could happen here:
      // 1. This is the 1st time this future is triggered, in which case mFutureTask.get() will
      //    calculate the result, which may be interrupted and return a partial result.
      // 2. The future task has already been triggered, and its result has been cached by the
      //    future itself. If the future is being triggered again, then the cached result is
      //    returned immediately, and it is assumed that this result is partial. The partial result
      //    will then be resumed later in this method.
      treeFutureResult = mFutureTask.get();

      onWaitEnd(shouldTrace, false);

      if (didRaiseThreadPriority) {
        // Reset the running thread's priority after we're unblocked.
        try {
          Process.setThreadPriority(runningThreadId, originalThreadPriority);
        } catch (IllegalArgumentException | SecurityException ignored) {
          // Ignored.
        }
      }

      if (mInterruptState.get() == INTERRUPTED
          && treeFutureResult.result != null
          && treeFutureResult.result.isPartialResult()) {
        if (ThreadUtils.isMainThread()) {
          // This means that the bg task was interrupted and it returned a partially resolved
          // InternalNode. We need to finish computing this LayoutState.
          final Object token = onBeginWorkContinuation("continuePartial", mContinuationToken);

          mContinuationToken = null;
          try {
            // Resuming here. We are on the main-thread.
            treeFutureResult = new TreeFutureResult<T>(resumeCalculation(treeFutureResult.result));
          } catch (Throwable th) {
            markFailure(token, th);
            throw th;
          } finally {
            onEndWorkContinuation(token);
          }
        } else {
          // This means that the bg task was interrupted and the UI thread will pick up the rest
          // of the work. No need to return a LayoutState.
          treeFutureResult =
              new TreeFutureResult<T>(FUTURE_RESULT_NULL_REASON_RESUME_NON_MAIN_THREAD);
          mContinuationToken = onOfferWorkForContinuation("offerPartial", mInterruptToken);

          mInterruptToken = null;
        }
      }
    } catch (ExecutionException | InterruptedException | CancellationException e) {
      onWaitEnd(shouldTrace, true);

      final Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      } else {
        throw new RuntimeException(e.getMessage(), e);
      }
    } finally {
      onGetEnd(shouldTrace);
    }

    synchronized (TreeFuture.this) {
      if (mReleased) {
        return new TreeFutureResult<T>(FUTURE_RESULT_NULL_REASON_RELEASED);
      }
      return treeFutureResult;
    }
  }

  /**
   * Holder class for tree-future results. When the contained result is null, the string message
   * will be populated with the result for it being null.
   */
  public static class TreeFutureResult<T extends PotentiallyPartialResult> {
    public final @Nullable T result;
    public final @Nullable String message;

    /** Initialise the TreeFutureResult with a non-null result. */
    public TreeFutureResult(T result) {
      this.result = result;
      this.message = null;
    }

    /**
     * Initialise the TreeFutureResult with a message explaining why the result is null. Use this
     * ctor when null results should be produced.
     */
    public TreeFutureResult(String message) {
      this.result = null;
      this.message = message;
    }
  }
}
