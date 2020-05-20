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

package com.facebook.litho.testing;

import android.os.Looper;
import com.facebook.litho.ComponentTree;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

/**
 * TestRule which allows a test to manually step through the default ComponentTree layout thread
 * Looper while still executing those tasks on a background thread. Normal usage of ShadowLooper
 * will execute on the calling thread, which in tests will execute code on the main thread.
 */
public class BackgroundLayoutLooperRule implements TestRule {

  private BlockingQueue<Message> mMessageQueue;

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        ShadowLooper layoutLooper =
            Shadows.shadowOf(
                (Looper)
                    Whitebox.invokeMethod(ComponentTree.class, "getDefaultLayoutThreadLooper"));
        mMessageQueue = new ArrayBlockingQueue<>(100);
        LayoutLooperThread layoutLooperThread = new LayoutLooperThread(layoutLooper, mMessageQueue);
        layoutLooperThread.start();
        try {
          base.evaluate();
        } finally {
          mMessageQueue.add(new Message(MessageType.QUIT));
        }
      }
    };
  }

  /** Runs one task on the background thread, blocking until it completes. */
  public void runOneTaskSync() {
    final TimeOutSemaphore semaphore = new TimeOutSemaphore(0);
    mMessageQueue.add(new Message(MessageType.DRAIN_ONE, semaphore));
    semaphore.acquire();
  }

  /** Runs through all tasks on the background thread, blocking until it completes. */
  public void runToEndOfTasksSync() {
    final TimeOutSemaphore semaphore = new TimeOutSemaphore(0);
    mMessageQueue.add(new Message(MessageType.DRAIN_ALL, semaphore));
    semaphore.acquire();
  }

  public TimeOutSemaphore runToEndOfTasksAsync() {
    final TimeOutSemaphore semaphore = new TimeOutSemaphore(0);
    mMessageQueue.add(new Message(MessageType.DRAIN_ALL, semaphore));
    return semaphore;
  }

  private enum MessageType {
    DRAIN_ONE,
    DRAIN_ALL,
    QUIT,
  }

  private static class Message {
    private final MessageType mMessageType;
    private final TimeOutSemaphore mSemaphore;

    private Message(MessageType messageType) {
      this(messageType, null);
    }

    private Message(MessageType messageType, TimeOutSemaphore semaphore) {
      mMessageType = messageType;
      mSemaphore = semaphore;
    }
  }

  private class LayoutLooperThread extends Thread {

    public LayoutLooperThread(
        final ShadowLooper layoutLooper, final BlockingQueue<Message> messages) {
      super(
          new Runnable() {
            @Override
            public void run() {
              boolean keepGoing = true;
              while (keepGoing) {
                final Message message;
                try {
                  message = messages.take();
                } catch (InterruptedException e) {
                  throw new RuntimeException(e);
                }
                switch (message.mMessageType) {
                  case DRAIN_ONE:
                    layoutLooper.runOneTask();
                    message.mSemaphore.release();
                    break;
                  case DRAIN_ALL:
                    layoutLooper.runToEndOfTasks();
                    message.mSemaphore.release();
                    break;
                  case QUIT:
                    keepGoing = false;
                    break;
                }
              }
            }
          });
    }
  }
}
