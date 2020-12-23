package com.facebook.litho;

import static com.google.common.truth.Truth.assertThat;

import com.facebook.litho.testing.testrunner.LithoTestRunner;
import java.util.ArrayDeque;
import java.util.concurrent.Executor;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public final class ExecutorLithoHandlerTest {

  private static final String TAG = "testTag";

  private final QueuingExecutor executor = new QueuingExecutor();
  private final ExecutorLithoHandler lithoHandler = new ExecutorLithoHandler(executor);

  private int runCounter;
  private Runnable runnable = () -> runCounter++;

  @Test
  public void testIsTracing_returnsFalse() {
    assertThat(lithoHandler.isTracing()).isFalse();
  }

  @Test
  public void testPost_multipleInstancesOfSameRunnable_runsAll() {
    lithoHandler.post(runnable, TAG);
    lithoHandler.post(runnable, TAG);

    executor.runAllQueuedTasks();

    assertThat(runCounter).isEqualTo(2);
  }

  @Test
  public void testPost_multipleRunnables_runsAll() {
    Runnable runnable2 = () -> runCounter += 2;

    lithoHandler.post(runnable, TAG);
    lithoHandler.post(runnable2, TAG);

    executor.runAllQueuedTasks();

    assertThat(runCounter).isEqualTo(3);
  }

  @Test
  public void testRemove_removesAllInstances() {
    lithoHandler.post(runnable, TAG);
    lithoHandler.post(runnable, TAG);
    lithoHandler.post(runnable, TAG);
    lithoHandler.remove(runnable);

    executor.runAllQueuedTasks();

    assertThat(runCounter).isEqualTo(0);
  }

  @Test
  public void testRemoveThenPost_runsOnce() {
    lithoHandler.post(runnable, TAG);
    lithoHandler.post(runnable, TAG);
    lithoHandler.remove(runnable);
    lithoHandler.post(runnable, TAG);

    executor.runAllQueuedTasks();

    assertThat(runCounter).isEqualTo(1);
  }

  private static class QueuingExecutor implements Executor {
    private final ArrayDeque<Runnable> queue = new ArrayDeque<>();

    @Override
    public void execute(Runnable runnable) {
      queue.add(runnable);
    }

    public void runAllQueuedTasks() {
      while (!queue.isEmpty()) {
        Runnable runnable = queue.removeFirst();
        runnable.run();
      }
    }
  }
}
