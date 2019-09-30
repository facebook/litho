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
package com.facebook.litho;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.robolectric.Shadows.shadowOf;

import android.os.Build;
import android.os.HandlerThread;
import android.os.Process;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

@PrepareForTest({Process.class})
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*"})
@Config(sdk = Build.VERSION_CODES.LOLLIPOP)
@RunWith(ComponentsTestRunner.class)
public class LithoHandlerTest {

  @Rule public PowerMockRule mPowerMockRule = new PowerMockRule();

  @Test
  public void testSetThreadPoolPriority() {
    mockStatic(Process.class);

    doAnswer(
            new Answer<Object>() {

              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Thread.currentThread().setPriority((Integer) args[0]);
                return Thread.currentThread().getId();
              }
            })
        .when(Process.class);
    Process.setThreadPriority(anyInt());

    final CountDownLatch countDownLatch = new CountDownLatch(1);
    final int[] threadPriority = new int[1];

    final ThreadPoolDynamicPriorityLayoutHandler handler =
        new ThreadPoolDynamicPriorityLayoutHandler(new LayoutThreadPoolConfigurationImpl(3, 3, 5));

    handler.post(
        new Runnable() {
          @Override
          public void run() {
            threadPriority[0] = Thread.currentThread().getPriority();
            countDownLatch.countDown();
          }
        },
        "");

    try {
      countDownLatch.await(5000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    assertThat(threadPriority[0]).isEqualTo(5);

    final CountDownLatch countDownLatchUpdate = new CountDownLatch(1);

    handler.setThreadPriority(3);

    handler.post(
        new Runnable() {
          @Override
          public void run() {
            threadPriority[0] = Thread.currentThread().getPriority();
            countDownLatchUpdate.countDown();
          }
        },
        "");

    try {
      countDownLatchUpdate.await(5000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    assertThat(threadPriority[0]).isEqualTo(3);
  }

  @Test
  public void testLithoHandlerDynamicPriority() {
    mockStatic(Process.class);

    doAnswer(
            new Answer<Object>() {

              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Thread.currentThread().setPriority((Integer) args[0]);
                return Thread.currentThread().getId();
              }
            })
        .when(Process.class);
    Process.setThreadPriority(anyInt());

    doAnswer(
            new Answer<Object>() {

              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Thread.currentThread().setPriority((Integer) args[1]);
                return Thread.currentThread().getId();
              }
            })
        .when(Process.class);
    Process.setThreadPriority(anyInt(), anyInt());

    final CountDownLatch countDownLatch = new CountDownLatch(1);
    final int[] threadPriority = new int[1];

    final HandlerThread handlerThread = new HandlerThread("test", 5);

    final DefaultLithoHandlerDynamicPriority handler =
        new DefaultLithoHandlerDynamicPriority(handlerThread);
    ShadowLooper mShadowLooper = shadowOf(handlerThread.getLooper());

    handler.post(
        new Runnable() {
          @Override
          public void run() {
            threadPriority[0] = Thread.currentThread().getPriority();
            countDownLatch.countDown();
          }
        },
        "");
    mShadowLooper.runToEndOfTasks();

    try {
      countDownLatch.await(5000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    assertThat(threadPriority[0]).isEqualTo(5);

    final CountDownLatch countDownLatchUpdate = new CountDownLatch(1);

    handler.setThreadPriority(3);
    handler.post(
        new Runnable() {
          @Override
          public void run() {
            threadPriority[0] = Thread.currentThread().getPriority();
            countDownLatchUpdate.countDown();
          }
        },
        "");
    mShadowLooper.runToEndOfTasks();

    try {
      countDownLatchUpdate.await(5000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    assertThat(threadPriority[0]).isEqualTo(3);
  }
}
