// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.rendercore.utils;

import static android.os.Process.THREAD_PRIORITY_DEFAULT;

import android.os.Looper;
import android.os.Process;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/** Thread assertion utilities. */
public class ThreadUtils {

  private ThreadUtils() {}

  public static boolean isMainThread() {
    return Looper.getMainLooper().getThread() == Thread.currentThread();
  }

  public static <T> T getResultInheritingPriority(Future<T> future, int runningThreadId) {
    final int originalThreadPriority;
    final boolean didRaiseThreadPriority;
    final boolean notRunningOnMyThread = runningThreadId != Process.myTid();
    final boolean shouldWaitForResult = !future.isDone() && notRunningOnMyThread;

    if (isMainThread() && shouldWaitForResult) {
      // Main thread is about to be blocked, raise the running thread priority.
      originalThreadPriority =
          ThreadUtils.tryInheritThreadPriorityFromCurrentThread(runningThreadId);
      didRaiseThreadPriority = true;
    } else {
      originalThreadPriority = THREAD_PRIORITY_DEFAULT;
      didRaiseThreadPriority = false;
    }

    try {
      return future.get();
    } catch (ExecutionException e) {
      final Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      } else {
        throw new RuntimeException(e.getMessage(), e);
      }
    } catch (InterruptedException | CancellationException e) {
      throw new RuntimeException(e.getMessage(), e);
    } finally {
      if (didRaiseThreadPriority) {
        // Reset the running thread's priority after we're unblocked.
        try {
          Process.setThreadPriority(runningThreadId, originalThreadPriority);
        } catch (IllegalArgumentException | SecurityException e) {
          throw new RuntimeException(
              "Unable to restore priority: " + runningThreadId + ", " + originalThreadPriority, e);
        }
      }
    }
  }

  /**
   * Try to raise the priority of {@param threadId} to the priority of the calling thread
   *
   * @return the original thread priority of the target thread.
   */
  public static int tryInheritThreadPriorityFromCurrentThread(int threadId) {
    return tryRaiseThreadPriority(threadId, Process.getThreadPriority(Process.myTid()));
  }

  /**
   * Try to raise the priority of {@param threadId} to {@param targetThreadPriority}.
   *
   * @return the original thread priority of the target thread.
   */
  public static int tryRaiseThreadPriority(int threadId, int targetThreadPriority) {
    // Main thread is about to be blocked, raise the running thread priority.
    final int originalThreadPriority = Process.getThreadPriority(threadId);
    boolean success = false;
    while (!success && targetThreadPriority < originalThreadPriority) {
      // Keep trying to increase thread priority of running thread as long as it is an increase.
      try {
        Process.setThreadPriority(threadId, targetThreadPriority);
        success = true;
      } catch (SecurityException e) {
        /*
         From {@link Process#THREAD_PRIORITY_DISPLAY}, some applications can not change
         the thread priority to that of the main thread. This catches that potential error
         and tries to set a lower priority.
        */
        targetThreadPriority += Process.THREAD_PRIORITY_LESS_FAVORABLE;
      }
    }
    return originalThreadPriority;
  }
}
