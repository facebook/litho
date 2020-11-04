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

import android.content.Context;
import androidx.annotation.Nullable;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.MountSpecLifecycleTesterDrawable;
import com.facebook.rendercore.RenderUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class MountStateAsMountDelegateTargetTest {

  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();

  @Before
  public void before() {
    ComponentsConfiguration.useExtensionsWithMountDelegate = true;
  }

  @After
  public void after() {
    ComponentsConfiguration.useExtensionsWithMountDelegate = false;
  }

  @Test
  public void lifecycle_onMount_shouldNotLithoRenderUnitBinders() {
    final LifecycleTracker lifecycleTracker = new LifecycleTracker();

    final TestBinder mountUnmountBinder = new TestBinder();
    final TestBinder attachDetachBinder = new TestBinder();

    final List<RenderUnit.Binder<LithoRenderUnit, Object>> mountUnmountBinders = new ArrayList<>();
    final List<RenderUnit.Binder<LithoRenderUnit, Object>> attachDetachBinders = new ArrayList<>();
    mountUnmountBinders.add(mountUnmountBinder);
    attachDetachBinders.add(attachDetachBinder);

    final LithoView lithoView = new LithoView(mLithoViewRule.getContext());
    lithoView.setLithoRenderUnitFactory(
        new LithoRenderUnitFactory(mountUnmountBinders, attachDetachBinders));

    mLithoViewRule.useLithoView(lithoView);

    final Component component =
        MountSpecLifecycleTesterDrawable.create(mLithoViewRule.getContext())
            .lifecycleTracker(lifecycleTracker)
            .intrinsicSize(new Size(800, 600))
            .build();
    mLithoViewRule.setRoot(component);

    mLithoViewRule.attachToWindow().measure().layout();

    assertThat(lifecycleTracker.getSteps())
        .describedAs("Should call the lifecycle methods in expected order")
        .contains(LifecycleStep.ON_MOUNT, LifecycleStep.ON_BIND);

    assertThat(mountUnmountBinder.mCalledBind).isFalse();
    assertThat(attachDetachBinder.mCalledBind).isFalse();

    lifecycleTracker.reset();
    mLithoViewRule.getLithoView().unmountAllItems();

    assertThat(lifecycleTracker.getSteps())
        .describedAs("Should call the lifecycle methods in expected order")
        .contains(LifecycleStep.ON_UNMOUNT, LifecycleStep.ON_UNBIND);

    assertThat(mountUnmountBinder.mCalledUnbind).isFalse();
    assertThat(attachDetachBinder.mCalledUnbind).isFalse();
  }

  private final class TestBinder implements RenderUnit.Binder {
    private boolean mCalledBind;
    private boolean mCalledUnbind;

    @Override
    public boolean shouldUpdate(
        Object currentModel,
        Object newModel,
        @Nullable Object currentLayoutData,
        @Nullable Object nextLayoutData) {
      return true;
    }

    @Override
    public void bind(Context context, Object o, Object o2, @Nullable Object layoutData) {
      mCalledBind = true;
    }

    @Override
    public void unbind(Context context, Object o, Object o2, @Nullable Object layoutData) {
      mCalledUnbind = true;
    }

    void reset() {
      mCalledBind = false;
      mCalledUnbind = false;
    }
  }
}
