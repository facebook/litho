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

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import android.os.Build;
import android.os.Process;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.annotation.Config;

@PrepareForTest({Process.class})
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*"})
@Config(sdk = Build.VERSION_CODES.LOLLIPOP)
@RunWith(LithoTestRunner.class)
public class ComponentTreeTest {

  @Rule public PowerMockRule mPowerMockRule = new PowerMockRule();

  private ComponentContext mContext;
  private int mWidthSpec;
  private int mHeightSpec;

  @Before
  public void setup() {
    mContext = new ComponentContext(getApplicationContext());
    mWidthSpec = makeSizeSpec(40, EXACTLY);
    mHeightSpec = makeSizeSpec(40, EXACTLY);
  }

  /**
   * Test scenario: - UI thread starts computing a sync layout - Before UI thread finishes, a
   * SET_ROOT_ASYNC is triggered on a BG thread and the LayoutStateFuture is compatible with the one
   * being calculated on the UI thread. - BG thread LayoutStateFuture doesn't wait for the result
   * and returns immediately - result is null because LayoutState hasn't finished. - UI thread
   * finishes calculating the LayoutState. - Make assertions when both LayoutStateFutures have
   * returned from runAndGet().
   */
  @Ignore("T57495493")
  @Test
  public void testLayoutStateFutureBgNotWaitingOnMainCT() {
    mockStatic(Process.class);

    doAnswer(
            new Answer<Object>() {

              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                return Thread.currentThread().getId();
              }
            })
        .when(Process.class);
    Process.myTid();

    final MyTestComponent mainComponent = new MyTestComponent("MyTestComponent");
    final CountDownLatch unlockBgRunAndGet = new CountDownLatch(1);
    final CountDownLatch unlockUILayoutFinish = new CountDownLatch(1);

    mainComponent.testId = 1;
    mainComponent.unlockWaitingOnCreateLayout = unlockBgRunAndGet;
    mainComponent.lockOnCreateLayoutFinish = unlockUILayoutFinish;

    final MyTestComponent bgComponent = new MyTestComponent("MyTestComponent");
    final CountDownLatch unlockUIThreadLayout = new CountDownLatch(1);

    bgComponent.testId = 1;
    bgComponent.unlockWaitingOnCreateLayout = unlockUIThreadLayout;

    final CountDownLatch lockWaitForResults = new CountDownLatch(1);

    ThreadPoolLayoutHandler handler =
        ThreadPoolLayoutHandler.getNewInstance(new LayoutThreadPoolConfigurationImpl(1, 1, 5));

    final ComponentTree componentTree =
        ComponentTree.create(mContext, mainComponent).layoutThreadHandler(handler).build();

    componentTree.setLithoView(new LithoView(mContext));

    final LayoutState[] mainLS = {null};
    final LayoutState[] bgLS = {null};

    Thread mainThread =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                componentTree.setRootAndSizeSpec(mainComponent, mWidthSpec, mHeightSpec);

                mainLS[0] = componentTree.getMainThreadLayoutState();
                // We have a result for UI thread, which was unblocked after BG thread finished
                // execution. That means we can go back to main thread and make assertions.
                lockWaitForResults.countDown();
              }
            },
            "main-t");

    // Start UI thread layout.
    mainThread.start();

    Thread bgThread =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                // This will countdown from the UI thread when it's computing layout - this means
                // there's a compatible LayoutStateFuture calculating layout on UI thread.
                try {
                  unlockBgRunAndGet.await(5000, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                  e.printStackTrace();
                }

                // Schedule the bg layout while UI layout is still in progress.
                componentTree.setRootAndSizeSpec(bgComponent, mWidthSpec, mHeightSpec);

                bgLS[0] = componentTree.getBackgroundLayoutState();
                // BG LayoutStateFuture finished, let the UI thread complete layout.
                // Countdown happens here if test is successful. Otherwise it will happen when
                // the bg thread computes layout, which means UI thread still gets unblocked but
                // assertion will fail.
                unlockUIThreadLayout.countDown();
              }
            },
            "bg-t");

    bgThread.start();

    // Wait for both threads to finish execution.
    try {
      lockWaitForResults.await(5000, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    assertThat(mainLS[0]).isNotNull();
    assertThat(bgLS[0]).isNull();

    // BG thread LayoutState should be null if it correctly returned early without computing layout
    // or waiting for UI thread.
    assertThat(mainComponent.hasRunLayout).isTrue();
    assertThat(bgComponent.hasRunLayout).isFalse();
  }

  class MyTestComponent extends Component {

    CountDownLatch unlockWaitingOnCreateLayout;
    CountDownLatch lockOnCreateLayoutFinish;
    int testId;
    boolean hasRunLayout;

    protected MyTestComponent(String simpleName) {
      super(simpleName);
    }

    @Override
    protected Component onCreateLayout(ComponentContext c) {
      if (unlockWaitingOnCreateLayout != null) {
        unlockWaitingOnCreateLayout.countDown();
      }

      if (lockOnCreateLayoutFinish != null) {
        try {
          lockOnCreateLayoutFinish.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

      hasRunLayout = true;
      return Column.create(c).build();
    }

    @Override
    protected int getId() {
      return testId;
    }
  }
}
