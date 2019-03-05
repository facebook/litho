/*
 * Copyright 2018-present Facebook, Inc.
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

import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import android.content.ContextWrapper;
import android.os.Looper;
import com.facebook.litho.config.LayoutThreadPoolConfiguration;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorCompletionService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

@PrepareForTest({ThreadUtils.class})
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "androidx.*", "android.*"})
@RunWith(ComponentsTestRunner.class)
public class SplitLayoutResolverTest {

  @Rule public PowerMockRule mPowerMockRule = new PowerMockRule();

  private Component mComponent;
  private static final String splitTag = "test_split_tag";
  private LayoutThreadPoolConfiguration mMainConfig;
  private LayoutThreadPoolConfiguration mBgConfig;
  private Set<String> mEnabledComponent;
  private ComponentContext mContext;
  private ExecutorCompletionService mainService;
  private ExecutorCompletionService bgService;
  private ShadowLooper mLayoutThreadShadowLooper;

  class MyTestComponent extends InlineLayoutSpec {
    @Override
    protected Component onCreateLayout(ComponentContext c) {
      return Column.create(c)
          .child(TestDrawableComponent.create(mContext))
          .child(TestDrawableComponent.create(mContext))
          .child(TestDrawableComponent.create(mContext))
          .build();
    }
  }

  @Before
  public void setup() throws Exception {
    mainService = mock(ExecutorCompletionService.class);
    bgService = mock(ExecutorCompletionService.class);
    mockStatic(ThreadUtils.class);
    mContext = new ComponentContext(new ContextWrapper(RuntimeEnvironment.application));

    mComponent = new MyTestComponent();

    new Runnable() {
      @Override
      public void run() {
        try {
          mLayoutThreadShadowLooper =
              Shadows.shadowOf(
                  (Looper)
                      Whitebox.invokeMethod(ComponentTree.class, "getDefaultLayoutThreadLooper"));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }.run();
    mMainConfig =
        new LayoutThreadPoolConfigurationImpl.Builder()
            .hasFixedSizePool(true)
            .fixedSizePoolConfiguration(2, 2)
            .build();

    mBgConfig =
        new LayoutThreadPoolConfigurationImpl.Builder()
            .hasFixedSizePool(true)
            .fixedSizePoolConfiguration(3, 3)
            .build();

    mEnabledComponent = new HashSet<>();
    mEnabledComponent.add("MyTestComponent");
  }

  @After
  public void cleanup() {
    SplitLayoutResolver.clearTag(splitTag);
  }

  @Test
  public void testSplitMainThreadLayouts() {
    SplitLayoutResolver.createForTag(splitTag, mMainConfig, mBgConfig, mEnabledComponent);
    SplitLayoutResolver resolver = SplitLayoutResolver.getForTag(splitTag);
    when(ThreadUtils.isMainThread()).thenReturn(true);

    Whitebox.setInternalState(resolver, "mainService", mainService);
    Whitebox.setInternalState(resolver, "bgService", bgService);

    ComponentTree tree =
        ComponentTree.create(mContext, mComponent).splitLayoutTag(splitTag).build();

    tree.setRootAndSizeSpec(
        mComponent, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY), new Size());

    verify(mainService).submit(any(Runnable.class), eq(0));
    verify(mainService).submit(any(Runnable.class), eq(1));
    verify(mainService, never()).submit(any(Runnable.class), eq(2));
    verify(bgService, never()).submit(any(Runnable.class), any());
  }

  @Test
  public void testSplitBgThreadLayouts() {
    SplitLayoutResolver.createForTag(splitTag, mMainConfig, mBgConfig, mEnabledComponent);
    SplitLayoutResolver resolver = SplitLayoutResolver.getForTag(splitTag);

    when(ThreadUtils.isMainThread()).thenReturn(false);
    Whitebox.setInternalState(resolver, "mainService", mainService);
    Whitebox.setInternalState(resolver, "bgService", bgService);

    final ComponentTree tree =
        ComponentTree.create(mContext, mComponent).splitLayoutTag(splitTag).build();

    tree.setRootAndSizeSpecAsync(
        mComponent, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));
    mLayoutThreadShadowLooper.runOneTask();

    verify(bgService).submit(any(Runnable.class), eq(0));
    verify(bgService).submit(any(Runnable.class), eq(1));
    verify(bgService, never()).submit(any(Runnable.class), eq(2));
    verify(mainService, never()).submit(any(Runnable.class), any());
  }

  @Test
  public void testOnlyMainEnabled() {
    SplitLayoutResolver.createForTag(splitTag, mMainConfig, null, mEnabledComponent);
    SplitLayoutResolver resolver = SplitLayoutResolver.getForTag(splitTag);

    when(ThreadUtils.isMainThread()).thenReturn(false);
    Whitebox.setInternalState(resolver, "mainService", mainService);

    final ComponentTree tree =
        ComponentTree.create(mContext, mComponent).splitLayoutTag(splitTag).build();

    tree.setRootAndSizeSpecAsync(
        mComponent, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    mLayoutThreadShadowLooper.runOneTask();

    verify(bgService, never()).submit(any(Runnable.class), eq(0));
  }
}
