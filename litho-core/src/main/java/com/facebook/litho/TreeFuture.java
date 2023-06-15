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
import java.util.List;
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
  protected final boolean mIsInterruptionEnabled;

  public TreeFuture(boolean isInterruptionEnabled) {
    mIsInterruptionEnabled = isInterruptionEnabled;
    if (!isInterruptionEnabled) {
      mInterruptState.set(NON_INTERRUPTIBLE);
    }
    this.mFutureTask =
        FutureInstrumenter.instrument(
            new FutureTask<>(
                new Callable<TreeFutureResult<T>>() {
                  @Override
                  public TreeFutureResult<T> call() {
                    synchronized (TreeFuture.this) {
                      if (mReleased) {
                        return TreeFutureResult.interruptWithMessage(
                            FUTURE_RESULT_NULL_REASON_RELEASED);
                      }
                    }
                    final T result = calculate();
                    synchronized (TreeFuture.this) {
                      if (mReleased) {
                        return TreeFutureResult.interruptWithMessage(
                            FUTURE_RESULT_NULL_REASON_RELEASED);
                      } else {
                        return TreeFutureResult.finishWithResult(result);
                      }
                    }
                  }
                }),
            "TreeFuture_calculateResult");
  }

  /** Returns a String that gives a textual representation of the type of future it is. */
  public abstract String getDescription();

  /** Returns an integer that identifies uniquely the version of this {@link TreeFuture}. */
  public abstract int getVersion();

  /** Calculates a new result for this TreeFuture. */
  protected abstract T calculate();

  /** Resumes an interrupted calculation based on a partial result */
  protected abstract T resumeCalculation(T partialResult);

  /** Returns true if the provided TreeFuture is equivalent to this one. */
  public abstract boolean isEquivalentTo(TreeFuture that);

  /** Releases this TreeFuture */
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public synchronized void release() {
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

  /** @return {@code true} if this future is interruptible. */
  boolean isInterruptible() {
    return mInterruptState.get() == INTERRUPTIBLE;
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
    if (waitingFromSyncLayout && mIsInterruptionEnabled && !isMainThread()) {
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

    maybeInterruptEarly();

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
            ComponentsSystrace.beginSectionWithArgs(
                "<cls>" + getClass().getName() + "</cls>." + suffix))
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
   * When called, this method will apply the iterrupt logic when calling tryRegisterForResponse,
   * rather than during runAndGet. This ensures that a tight race between reusing a future and
   * interrupting it can't happen, making the interruption and reuse logic a single operation.
   *
   * <p>In doing so, we can ensure that async tasks that are interrupted by equivalent sync tasks do
   * not proceed and return null as intended.
   */
  private void maybeInterruptEarly() {
    final int runningThreadId = mRunningThreadId.get();
    final boolean isRunningOnDifferentThread =
        !mFutureTask.isDone() && runningThreadId != -1 && runningThreadId != Process.myTid();

    if (mIsInterruptionEnabled && isRunningOnDifferentThread && isMainThread()) {
      tryMoveToInterruptedState();
    }
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
  TreeFutureResult<T> runAndGet(@RenderSource final int source) {
    if (mRunningThreadId.compareAndSet(-1, Process.myTid())) {
      mFutureTask.run();
    }

    final int runningThreadId = this.mRunningThreadId.get();
    final boolean notRunningOnMyThread = runningThreadId != Process.myTid();
    final int originalThreadPriority;
    final boolean didRaiseThreadPriority;

    final boolean shouldWaitForResult = !mFutureTask.isDone() && notRunningOnMyThread;

    if (shouldWaitForResult && !isMainThread() && !isFromSyncLayout(source)) {
      return TreeFutureResult.interruptWithMessage(
          FUTURE_RESULT_NULL_REASON_SYNC_RESULT_NON_MAIN_THREAD);
    }

    if (isMainThread() && shouldWaitForResult) {
      // This means the UI thread is about to be blocked by the bg thread. Instead of waiting,
      // the bg task is interrupted.
      if (mIsInterruptionEnabled) {
        if (tryMoveToInterruptedState()) {
          mInterruptToken =
              WorkContinuationInstrumenter.onAskForWorkToContinue("interruptCalculate");
        }
      }

      originalThreadPriority =
          ThreadUtils.tryRaiseThreadPriority(runningThreadId, Process.THREAD_PRIORITY_DISPLAY);
      didRaiseThreadPriority = true;
    } else {
      originalThreadPriority = Process.THREAD_PRIORITY_DEFAULT;
      didRaiseThreadPriority = false;
    }

    TreeFutureResult<T> treeFutureResult = null;

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
            treeFutureResult =
                TreeFutureResult.finishWithResult(resumeCalculation(treeFutureResult.result));
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
              TreeFutureResult.interruptWithMessage(
                  FUTURE_RESULT_NULL_REASON_RESUME_NON_MAIN_THREAD);
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
        return TreeFutureResult.interruptWithMessage(FUTURE_RESULT_NULL_REASON_RELEASED);
      }

      return treeFutureResult;
    }
  }

  /**
   * Given a provided tree-future, this method will track it via a given list, and run it.
   *
   * <p>If an equivalent tree-future is found in the given list of futures, the sequence in this
   * method will attempt to reuse it and increase the wait-count on it if successful.
   *
   * <p>If an async operation is requested and an equivalent future is already running, it will be
   * discarded and return null.
   *
   * <p>If no equivalent running future was found in the provided list, it will be added to the list
   * for the duration of the run, and then, provided it has a 0 wait-count, it will be removed from
   * the provided list.
   *
   * @param treeFuture The future to executed
   * @param futureList The list of futures this future will be added to
   * @param source The source of the calculation
   * @param mutex A mutex to use to synchronize access to the provided future list
   * @param futureExecutionListener optional listener that will be invoked just before the future is
   *     run
   * @param <T> The generic type of the provided future & expected result
   * @return The result holder of running the future. It will contain the result if there were no
   *     issues running the future, or a message explaining why the result is null.
   */
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public static <T extends PotentiallyPartialResult, F extends TreeFuture<T>>
      TreeFuture.TreeFutureResult<T> trackAndRunTreeFuture(
          F treeFuture,
          final List<F> futureList,
          final @RenderSource int source,
          final Object mutex,
          final @Nullable FutureExecutionListener futureExecutionListener) {
    final boolean isSync = isFromSyncLayout(source);
    boolean isReusingFuture = false;

    synchronized (mutex) {
      // Iterate over the running futures to see if an equivalent one is running
      for (final F runningFuture : futureList) {
        if (!runningFuture.isReleased()
            && runningFuture.isEquivalentTo(treeFuture)
            && runningFuture.tryRegisterForResponse(isSync)) {
          // An equivalent running future was found, and can be reused (tryRegisterForResponse
          // returned true). Discard the provided future, and use the running future instead.
          // tryRegisterForResponse returned true, and also increased the wait-count, so no need
          // explicitly call that.
          treeFuture = runningFuture;
          isReusingFuture = true;
          break;
        }
      }

      // Not reusing so mark as NON_INTERRUPTIBLE, increase the wait count, add the new future to
      // the list.
      if (!isReusingFuture) {
        if (!treeFuture.tryRegisterForResponse(isSync)) {
          throw new RuntimeException("Failed to register to tree future");
        }

        futureList.add(treeFuture);
      }
    }

    if (futureExecutionListener != null) {
      final FutureExecutionType executionType;
      if (isReusingFuture) {
        executionType = FutureExecutionType.REUSE_FUTURE;
      } else {
        executionType = FutureExecutionType.NEW_FUTURE;
      }

      futureExecutionListener.onPreExecution(
          treeFuture.getVersion(), executionType, treeFuture.getDescription());
    }

    // Run and get the result
    final TreeFuture.TreeFutureResult<T> result = treeFuture.runAndGet(source);

    synchronized (mutex) {
      if (futureExecutionListener != null) {
        futureExecutionListener.onPostExecution(
            treeFuture.getVersion(), treeFuture.isReleased(), treeFuture.getDescription());
      }

      // Unregister for response, decreasing the wait count
      treeFuture.unregisterForResponse();

      // If the wait count is 0, release the future and remove it from the list
      if (treeFuture.getWaitingCount() == 0) {
        treeFuture.release();
        futureList.remove(treeFuture);
      }
    }

    return result;
  }

  /**
   * Holder class for tree-future results. When the contained result is null, the string message
   * will be populated with the result for it being null.
   */
  public static class TreeFutureResult<T extends PotentiallyPartialResult> {
    public final @Nullable T result;
    public final @Nullable String message;

    private TreeFutureResult(@Nullable T result, @Nullable String message) {
      this.result = result;
      this.message = message;
    }

    public static <T extends PotentiallyPartialResult> TreeFutureResult<T> finishWithResult(
        T result) {
      return new TreeFutureResult<>(result, null);
    }

    public static <T extends PotentiallyPartialResult> TreeFutureResult<T> interruptWithMessage(
        String message) {
      return new TreeFutureResult<>(null, message);
    }
  }

  public interface FutureExecutionListener {

    /**
     * Called just before or after a future is triggered.
     *
     * @param futureExecutionType How the future is going to be executed - run a new future, reuse a
     *     running one, or cancelled entirely.
     */
    void onPreExecution(
        int version, final FutureExecutionType futureExecutionType, String attribution);

    void onPostExecution(int version, boolean released, String attribution);
  }

  public enum FutureExecutionType {
    /** A new future is about to be triggered */
    NEW_FUTURE,

    /** An already running future is about to be reused */
    REUSE_FUTURE
  }
}
