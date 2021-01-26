package com.facebook.litho;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.annotation.concurrent.GuardedBy;

/**
 * A {@code LithoHandler} implementation that runs all layout computations against the provided
 * {@code Executor}. This {@code LithoHandler} can be used in apps that have a well established
 * threading model and need to run layout against one of their already available executors.
 */
public final class ExecutorLithoHandler implements LithoHandler {

  @GuardedBy("pendingTasks")
  private final Map<Runnable, Integer> pendingTasks = new HashMap<>();
  private final Executor executor;

  /**
   * Instantiates an {@code ExecutorLithoHandler} that uses the given {@code Executor} to run all
   * layout tasks against.
   */
  public ExecutorLithoHandler(Executor executor) {
    this.executor = executor;
  }

  @Override
  public void post(final Runnable runnable, String tag) {
    synchronized (pendingTasks) {
      Integer runCount = pendingTasks.get(runnable);
      pendingTasks.put(runnable, runCount != null ? runCount + 1 : 1);
    }

    executor.execute(new Runnable() {
      @Override
      public void run() {
        boolean canRun = false;
        synchronized (pendingTasks) {
          Integer runCount = pendingTasks.get(runnable);
          if (runCount != null) {
            canRun = runCount > 0;
            runCount--;
            if (runCount > 0) {
              pendingTasks.put(runnable, runCount);
            } else {
              pendingTasks.remove(runnable);
            }
          }
        }
        if (canRun) {
          runnable.run();
        }
      }
    });
  }

  /**
   * {@inheritDoc}
   *
   * In this implementation, postAtFront() is equivalent to post().
   */
  @Override
  public void postAtFront(Runnable runnable, String tag) {
    post(runnable, tag);
  }

  /**
   * {@inheritDoc}
   *
   * This implementation removes all instances of the provided {@code Runnable} and prevents them
   * from running if they have not started yet.
   */
  @Override
  public void remove(Runnable runnable) {
    synchronized (pendingTasks) {
      pendingTasks.remove(runnable);
    }
  }

  @Override
  public boolean isTracing() {
    return false;
  }
}
