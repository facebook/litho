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

package com.facebook.litho;

import static org.assertj.core.api.Java6Assertions.assertThat;

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
