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

import static android.os.Looper.getMainLooper;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.BackgroundLayoutLooperRule;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.widget.AttachDetachTester;
import com.facebook.litho.widget.AttachDetachTesterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.LooperMode;

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(ParameterizedRobolectricTestRunner.class)
public class AttachDetachHandlerTest {

  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();
  public @Rule BackgroundLayoutLooperRule mBackgroundLayoutLooperRule =
      new BackgroundLayoutLooperRule();

  private final boolean mUseStatelessComponent;
  private final boolean mInitialUseStatelessComponentValue;
  private final boolean mUseWorkingRangeFromContext;
  private final boolean mInitialUseWorkingRangeFromContextValue;

  private final boolean mUseStateContainerFromContext;
  private final boolean mInitialUseStateContainerFromContextValue;

  @ParameterizedRobolectricTestRunner.Parameters(
      name =
          "useStatelessComponent={0} useWorkingRangeFromContext={1} useStateContainerFromContext={2}")
  public static Collection data() {
    return Arrays.asList(
        new Object[][] {
          {false, false, false},
          {true, false, false},
          {true, true, false},
          {true, false, true},
          {true, true, true}
        });
  }

  public AttachDetachHandlerTest(
      boolean useStatelessComponent,
      boolean useWorkingRangeFromContext,
      boolean useStateContainerFromContext) {
    mUseStatelessComponent = useStatelessComponent;
    mInitialUseStatelessComponentValue = ComponentsConfiguration.useStatelessComponent;

    mUseWorkingRangeFromContext = useWorkingRangeFromContext;
    mInitialUseWorkingRangeFromContextValue = ComponentsConfiguration.useWorkingRangeFromContext;

    mUseStateContainerFromContext = useStateContainerFromContext;
    mInitialUseStateContainerFromContextValue =
        ComponentsConfiguration.useStateContainerFromContext;
  }

  @Before
  public void setup() {
    ComponentsConfiguration.useStatelessComponent = mUseStatelessComponent;
    ComponentsConfiguration.useWorkingRangeFromContext = mUseWorkingRangeFromContext;
    ComponentsConfiguration.useStateContainerFromContext = mUseStateContainerFromContext;
  }

  @After
  public void tearDown() {
    ComponentsConfiguration.useStatelessComponent = mInitialUseStatelessComponentValue;
    ComponentsConfiguration.useWorkingRangeFromContext = mInitialUseWorkingRangeFromContextValue;
    ComponentsConfiguration.useStateContainerFromContext =
        mInitialUseStateContainerFromContextValue;
  }

  @Test
  public void component_setRootWithLayout_onAttachedIsCalled() {
    final List<String> steps = new ArrayList<>();
    final Component root =
        AttachDetachTester.create(mLithoViewRule.getContext()).name("root").steps(steps).build();
    mLithoViewRule.setRoot(root);

    mLithoViewRule.attachToWindow().measure().layout();

    assertThat(steps)
        .describedAs("Should call @OnAttached method")
        .containsExactly("root:" + AttachDetachTesterSpec.ON_ATTACHED);

    final AttachDetachHandler attachDetachHandler =
        mLithoViewRule.getComponentTree().getAttachDetachHandler();
    assertThat(attachDetachHandler.getAttached().size()).isEqualTo(1);
  }

  @Test
  public void component_setEmptyRootAfterAttach_onDetachedIsCalled() {
    final List<String> steps = new ArrayList<>();
    final Component root =
        AttachDetachTester.create(mLithoViewRule.getContext()).name("root").steps(steps).build();
    mLithoViewRule.setRoot(root);

    mLithoViewRule.attachToWindow().measure().layout();

    assertThat(steps)
        .describedAs("Should call @OnAttached method")
        .containsExactly("root:" + AttachDetachTesterSpec.ON_ATTACHED);
    steps.clear();

    mLithoViewRule.setRoot(Column.create(mLithoViewRule.getContext()).build());

    assertThat(steps)
        .describedAs("Should call @OnDetached method")
        .containsExactly("root:" + AttachDetachTesterSpec.ON_DETACHED);
  }

  @Test
  public void component_releaseLithoView_onDetachedIsCalled() {
    final List<String> steps = new ArrayList<>();
    final Component root =
        AttachDetachTester.create(mLithoViewRule.getContext()).name("root").steps(steps).build();
    mLithoViewRule.setRoot(root);

    mLithoViewRule.attachToWindow().measure().layout();

    final AttachDetachHandler attachDetachHandler =
        mLithoViewRule.getComponentTree().getAttachDetachHandler();

    steps.clear();

    mLithoViewRule.release();

    assertThat(steps)
        .describedAs("Should call @OnDetached method")
        .containsExactly("root:" + AttachDetachTesterSpec.ON_DETACHED);

    assertThat(attachDetachHandler.getAttached()).isNullOrEmpty();
  }

  @Test
  public void component_replaceRootWithSameComponent_onDetachedIsNotCalled() {
    final ComponentContext c = mLithoViewRule.getContext();
    final AttachDetachTester.Builder c3 = AttachDetachTester.create(c).name("c3");
    final AttachDetachTester.Builder c4 = AttachDetachTester.create(c).name("c4");
    final AttachDetachTester.Builder c1 =
        AttachDetachTester.create(c).name("c1").children(new AttachDetachTester.Builder[] {c3, c4});
    final AttachDetachTester.Builder c2 = AttachDetachTester.create(c).name("c2");
    final List<String> steps = new ArrayList<>();
    final Component r1 =
        AttachDetachTester.create(c)
            .children(new AttachDetachTester.Builder[] {c1, c2})
            .name("r1")
            .steps(steps)
            .build();
    mLithoViewRule.setRoot(r1);

    mLithoViewRule.attachToWindow().measure().layout();

    assertThat(steps)
        .describedAs("Should call @OnAttached methods in expected order")
        .containsExactly(
            "c3:" + AttachDetachTesterSpec.ON_ATTACHED,
            "c4:" + AttachDetachTesterSpec.ON_ATTACHED,
            "c1:" + AttachDetachTesterSpec.ON_ATTACHED,
            "c2:" + AttachDetachTesterSpec.ON_ATTACHED,
            "r1:" + AttachDetachTesterSpec.ON_ATTACHED);

    final AttachDetachHandler attachDetachHandler =
        mLithoViewRule.getComponentTree().getAttachDetachHandler();
    assertThat(attachDetachHandler.getAttached().size()).isEqualTo(5);

    steps.clear();

    /*
             r1                r2
           /    \            /    \
         c1      c2   =>   c5      c6
        /  \                         \
      c3    c4                        c7
    */
    final AttachDetachTester.Builder c5 = AttachDetachTester.create(c).name("c5");
    final AttachDetachTester.Builder c7 = AttachDetachTester.create(c).name("c7");
    final AttachDetachTester.Builder c6 =
        AttachDetachTester.create(c).name("c6").children(new AttachDetachTester.Builder[] {c7});
    final Component r2 =
        AttachDetachTester.create(c)
            .children(new AttachDetachTester.Builder[] {c5, c6})
            .name("r2")
            .steps(steps)
            .build();
    mLithoViewRule.setRoot(r2);

    assertThat(steps)
        .describedAs("Should call @OnDetached and @OnAttached methods in expect order")
        .containsExactly(
            "c3:" + AttachDetachTesterSpec.ON_DETACHED,
            "c4:" + AttachDetachTesterSpec.ON_DETACHED,
            "c7:" + AttachDetachTesterSpec.ON_ATTACHED);

    assertThat(attachDetachHandler.getAttached().size()).isEqualTo(4);

    steps.clear();

    /*
            r2                  r3
          /    \              /
        c5      c6     =>   c8
                  \
                   c7
    */
    final AttachDetachTester.Builder c8 = AttachDetachTester.create(c).name("c8");
    final Component r3 =
        AttachDetachTester.create(c)
            .children(new AttachDetachTester.Builder[] {c8})
            .name("r3")
            .steps(steps)
            .build();
    mLithoViewRule.setRoot(r3);

    assertThat(steps)
        .describedAs("Should call @OnDetached methods in expect order")
        .containsExactly(
            "c7:" + AttachDetachTesterSpec.ON_DETACHED, "c6:" + AttachDetachTesterSpec.ON_DETACHED);

    assertThat(attachDetachHandler.getAttached().size()).isEqualTo(2);
  }

  @Test
  public void component_replaceRootWithDifferentComponent_onDetachedIsNotCalled() {
    final ComponentContext c = mLithoViewRule.getContext();
    final AttachDetachTester.Builder c3 = AttachDetachTester.create(c).name("c3");
    final AttachDetachTester.Builder c4 = AttachDetachTester.create(c).name("c4");
    final AttachDetachTester.Builder c1 =
        AttachDetachTester.create(c).name("c1").children(new AttachDetachTester.Builder[] {c3, c4});
    final AttachDetachTester.Builder c2 = AttachDetachTester.create(c).name("c2");
    final List<String> steps = new ArrayList<>();
    final Component r1 =
        AttachDetachTester.create(c)
            .children(new AttachDetachTester.Builder[] {c1, c2})
            .name("r1")
            .steps(steps)
            .build();
    mLithoViewRule.setRoot(r1);

    mLithoViewRule.attachToWindow().measure().layout();

    assertThat(steps)
        .describedAs("Should call @OnAttached methods in expected order")
        .containsExactly(
            "c3:" + AttachDetachTesterSpec.ON_ATTACHED,
            "c4:" + AttachDetachTesterSpec.ON_ATTACHED,
            "c1:" + AttachDetachTesterSpec.ON_ATTACHED,
            "c2:" + AttachDetachTesterSpec.ON_ATTACHED,
            "r1:" + AttachDetachTesterSpec.ON_ATTACHED);

    final AttachDetachHandler attachDetachHandler =
        mLithoViewRule.getComponentTree().getAttachDetachHandler();
    assertThat(attachDetachHandler.getAttached().size()).isEqualTo(5);

    steps.clear();

    /*
             r1                r2 (TestWrappedComponentProp)
           /    \            /    \
         c1      c2   =>   c5      c6
        /  \                         \
      c3    c4                        c7
    */
    final AttachDetachTester.Builder c5 = AttachDetachTester.create(c).name("c5");
    final AttachDetachTester.Builder c7 = AttachDetachTester.create(c).name("c7");
    final AttachDetachTester.Builder c6 =
        AttachDetachTester.create(c).name("c6").children(new AttachDetachTester.Builder[] {c7});
    final Component r2 =
        AttachDetachTester.create(c)
            .children(new AttachDetachTester.Builder[] {c5, c6})
            .name("r2")
            .key("newKey")
            .steps(steps)
            .build();
    mLithoViewRule.setRoot(r2);

    assertThat(steps)
        .describedAs("Should call @OnDetached and @OnAttached methods in expect order")
        .containsExactly(
            "c3:" + AttachDetachTesterSpec.ON_DETACHED,
            "c4:" + AttachDetachTesterSpec.ON_DETACHED,
            "c1:" + AttachDetachTesterSpec.ON_DETACHED,
            "c2:" + AttachDetachTesterSpec.ON_DETACHED,
            "r1:" + AttachDetachTesterSpec.ON_DETACHED,
            "c5:" + AttachDetachTesterSpec.ON_ATTACHED,
            "c7:" + AttachDetachTesterSpec.ON_ATTACHED,
            "c6:" + AttachDetachTesterSpec.ON_ATTACHED,
            "r2:" + AttachDetachTesterSpec.ON_ATTACHED);

    assertThat(attachDetachHandler.getAttached().size()).isEqualTo(4);
  }

  @Test
  public void component_setRootAndSizeSpecTwice_onAttachAndOnDetachedAreCalledOnlyOnce() {
    final ComponentContext c = mLithoViewRule.getContext();
    final List<String> steps = new ArrayList<>();
    final AttachDetachTester.Builder c1 = AttachDetachTester.create(c).name("c1");
    final Component component =
        AttachDetachTester.create(c)
            .name("root")
            .steps(steps)
            .children(new AttachDetachTester.Builder[] {c1})
            .build();
    mLithoViewRule.setRootAndSizeSpec(
        component, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    mLithoViewRule.attachToWindow().measure().layout();

    mLithoViewRule.setRootAndSizeSpec(
        component, makeSizeSpec(200, EXACTLY), makeSizeSpec(200, EXACTLY));

    assertThat(steps)
        .describedAs("Should call @OnAttached only once for each component")
        .containsExactly(
            "c1:" + AttachDetachTesterSpec.ON_ATTACHED,
            "root:" + AttachDetachTesterSpec.ON_ATTACHED);

    steps.clear();

    mLithoViewRule.release();

    assertThat(steps)
        .describedAs("Should call @OnDetached only once for each component")
        .containsExactly(
            "c1:" + AttachDetachTesterSpec.ON_DETACHED,
            "root:" + AttachDetachTesterSpec.ON_DETACHED);
  }

  @Test
  public void component_setRootAndSizeSpecConcurrently_onAttachAndOnDetachedAreCalledOnlyOnce()
      throws InterruptedException {
    final ComponentContext c = mLithoViewRule.getContext();
    final List<String> steps = new ArrayList<>();
    final AttachDetachTester.Builder c1 = AttachDetachTester.create(c).name("c1");
    final Component component =
        AttachDetachTester.create(c)
            .name("root")
            .steps(steps)
            .children(new AttachDetachTester.Builder[] {c1})
            .build();

    final ComponentTree componentTree = mLithoViewRule.getComponentTree();
    final CountDownLatch latch1 = new CountDownLatch(1);
    final Thread thread1 =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                for (int i = 0; i < 10; i++) {
                  componentTree.setRootAndSizeSpec(
                      component,
                      makeSizeSpec(100 + 10 * i, EXACTLY),
                      makeSizeSpec(100 + 10 * i, EXACTLY));
                }
                latch1.countDown();
              }
            });

    final CountDownLatch latch2 = new CountDownLatch(1);
    final Thread thread2 =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                for (int i = 0; i < 10; i++) {
                  componentTree.setRootAndSizeSpec(
                      component,
                      makeSizeSpec(200 + 10 * i, EXACTLY),
                      makeSizeSpec(200 + 10 * i, EXACTLY));
                }
                latch2.countDown();
              }
            });
    thread1.start();
    thread2.start();

    assertThat(latch1.await(5000, TimeUnit.MILLISECONDS)).isTrue();
    assertThat(latch2.await(5000, TimeUnit.MILLISECONDS)).isTrue();

    shadowOf(getMainLooper()).idle();

    assertThat(steps)
        .describedAs("Should call @OnAttached only once for each component")
        .containsExactly(
            "c1:" + AttachDetachTesterSpec.ON_ATTACHED,
            "root:" + AttachDetachTesterSpec.ON_ATTACHED);
  }

  @Test
  public void component_setRoot_onAttachedIsCalledOnUIThread() {
    final List<String> steps = new ArrayList<>();
    final ConcurrentHashMap<String, Object> extraThreadInfo = new ConcurrentHashMap<>();

    final Component root =
        AttachDetachTester.create(mLithoViewRule.getContext())
            .name("root")
            .steps(steps)
            .extraThreadInfo(extraThreadInfo)
            .build();

    mLithoViewRule.attachToWindow().measure().layout();
    mLithoViewRule.setRoot(root);

    assertThat(steps)
        .describedAs("Should call @OnAttached method")
        .containsExactly("root:" + AttachDetachTesterSpec.ON_ATTACHED);

    final AttachDetachHandler attachDetachHandler =
        mLithoViewRule.getComponentTree().getAttachDetachHandler();
    assertThat(attachDetachHandler.getAttached().size()).isEqualTo(1);

    final Boolean isMainThreadLayout =
        (Boolean) extraThreadInfo.get(AttachDetachTesterSpec.IS_MAIN_THREAD_LAYOUT);
    assertThat(isMainThreadLayout).isTrue();

    final Boolean isMainThreadAttached =
        (Boolean) extraThreadInfo.get(AttachDetachTesterSpec.IS_MAIN_THREAD_ON_ATTACHED);
    assertThat(isMainThreadAttached).isTrue();
  }

  @Test
  public void component_setRootAsync_onAttachedIsCalledOnUIThread() {
    final List<String> steps = new ArrayList<>();
    final ConcurrentHashMap<String, Object> extraThreadInfo = new ConcurrentHashMap<>();

    final Component root =
        AttachDetachTester.create(mLithoViewRule.getContext())
            .name("root")
            .steps(steps)
            .extraThreadInfo(extraThreadInfo)
            .build();

    mLithoViewRule.attachToWindow().measure().layout();
    mLithoViewRule.setRootAsync(root);

    mBackgroundLayoutLooperRule.runToEndOfTasksSync();
    shadowOf(getMainLooper()).idle();

    assertThat(steps)
        .describedAs("Should call @OnAttached method")
        .containsExactly("root:" + AttachDetachTesterSpec.ON_ATTACHED);

    final AttachDetachHandler attachDetachHandler =
        mLithoViewRule.getComponentTree().getAttachDetachHandler();
    assertThat(attachDetachHandler.getAttached().size()).isEqualTo(1);

    final Boolean isMainThreadLayout =
        (Boolean) extraThreadInfo.get(AttachDetachTesterSpec.IS_MAIN_THREAD_LAYOUT);
    assertThat(isMainThreadLayout).isFalse();

    final Boolean isMainThreadAttached =
        (Boolean) extraThreadInfo.get(AttachDetachTesterSpec.IS_MAIN_THREAD_ON_ATTACHED);
    assertThat(isMainThreadAttached).isTrue();
  }
}
