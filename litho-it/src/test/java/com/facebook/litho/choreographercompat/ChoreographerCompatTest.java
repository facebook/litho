/*
 * Copyright 2019-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho.choreographercompat;

import static org.assertj.core.api.Assertions.assertThat;

import com.facebook.litho.choreographercompat.ChoreographerCompat.FrameCallback;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.shadows.ShadowLooper;

@RunWith(ComponentsTestRunner.class)
public class ChoreographerCompatTest {

  @Test
  public void testCreationFromMainThread() {
    ShadowLooper.pauseMainLooper();

    final AtomicBoolean firstCallback = new AtomicBoolean(false);
    final ChoreographerCompatImpl choreographerCompat = new ChoreographerCompatImpl();
    assertThat(choreographerCompat.isUsingChoreographer()).isTrue();
    new ChoreographerCompatImpl()
        .postFrameCallback(
            new FrameCallback() {
              @Override
              public void doFrame(long frameTimeNanos) {
                firstCallback.set(true);
              }
            });

    assertThat(firstCallback.get()).isFalse();

    ShadowLooper.runUiThreadTasks();

    assertThat(firstCallback.get()).isTrue();
  }

  @Test
  public void testCreationFromBGThread() throws InterruptedException {
    ShadowLooper.pauseMainLooper();

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<ChoreographerCompatImpl> ref = new AtomicReference<>();
    new Thread(
            new Runnable() {
              @Override
              public void run() {
                ref.set(new ChoreographerCompatImpl());
                latch.countDown();
              }
            })
        .start();

    assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

    assertThat(ref.get().isUsingChoreographer()).isFalse();

    final AtomicBoolean firstCallback = new AtomicBoolean(false);
    ref.get()
        .postFrameCallback(
            new FrameCallback() {
              @Override
              public void doFrame(long frameTimeNanos) {
                firstCallback.set(true);
              }
            });

    assertThat(firstCallback.get()).isFalse();

    ShadowLooper.runUiThreadTasks();

    assertThat(firstCallback.get()).isTrue();

    ShadowLooper.pauseMainLooper();

    assertThat(ref.get().isUsingChoreographer()).isTrue();

    final AtomicBoolean secondCallback = new AtomicBoolean(false);
    ref.get()
        .postFrameCallback(
            new FrameCallback() {
              @Override
              public void doFrame(long frameTimeNanos) {
                secondCallback.set(true);
              }
            });

    assertThat(secondCallback.get()).isFalse();

    ShadowLooper.runUiThreadTasks();

    assertThat(secondCallback.get()).isTrue();
  }
}
