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
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.robolectric.Shadows.shadowOf;

import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import com.facebook.litho.testing.BackgroundLayoutLooperRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.LayoutSpecLifecycleTester;
import com.facebook.litho.widget.RecyclerBinder;
import com.facebook.litho.widget.RenderInfo;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

@RunWith(LithoTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class LithoRecyclerBinderLifecycleProviderTest {

  private LithoLifecycleProviderDelegate mLithoLifecycleProviderDelegate;
  private RecyclerBinder recyclerBinder;
  private RecyclerView recyclerView;
  public @Rule BackgroundLayoutLooperRule mBackgroundLayoutLooperRule =
      new BackgroundLayoutLooperRule();

  @Before
  public void setup() {
    mLithoLifecycleProviderDelegate = new LithoLifecycleProviderDelegate();
    ComponentContext c = new ComponentContext(getApplicationContext());
    recyclerView = new RecyclerView(getApplicationContext());
    recyclerView.setLayoutParams(new ViewGroup.LayoutParams(10, 100));
    recyclerBinder =
        new RecyclerBinder.Builder()
            .lithoLifecycleProvider(mLithoLifecycleProviderDelegate)
            .build(c);
    recyclerBinder.mount(recyclerView);
    final List<RenderInfo> components = new ArrayList<>();
    final List<List<LifecycleStep.StepInfo>> stepsList = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      List<LifecycleStep.StepInfo> steps = new ArrayList<>();
      Component component =
          LayoutSpecLifecycleTester.create(c).widthPx(10).heightPx(5).steps(steps).build();
      components.add(ComponentRenderInfo.create().component(component).build());
      stepsList.add(steps);
    }
    recyclerBinder.insertRangeAt(0, components);
    recyclerBinder.notifyChangeSetComplete(true, null);
    recyclerBinder.measure(
        new Size(),
        makeSizeSpec(10, EXACTLY),
        makeSizeSpec(100, EXACTLY),
        mock(EventHandler.class));

    mBackgroundLayoutLooperRule.runToEndOfTasksSync();
    shadowOf(getMainLooper()).idle();
  }

  @Test
  public void lithoLifecycleProviderDelegateRecyclerBinderVisibleTest() {
    mLithoLifecycleProviderDelegate.moveToLifecycle(
        LithoLifecycleProvider.LithoLifecycle.HINT_VISIBLE);

    for (int j = 0; j <= 19; j++) {
      assertThat(recyclerBinder.getComponentAt(j).getLifecycleProvider().getLifecycleStatus())
          .describedAs("Visible event is expected to be dispatched")
          .isEqualTo(LithoLifecycleProvider.LithoLifecycle.HINT_VISIBLE);
    }
  }

  @Test
  public void lithoLifecycleProviderDelegateRecyclerBinderInvisibleTest() {
    mLithoLifecycleProviderDelegate.moveToLifecycle(
        LithoLifecycleProvider.LithoLifecycle.HINT_INVISIBLE);

    for (int j = 0; j <= 19; j++) {
      assertThat(recyclerBinder.getComponentAt(j).getLifecycleProvider().getLifecycleStatus())
          .describedAs("Invisible event is expected to be dispatched")
          .isEqualTo(LithoLifecycleProvider.LithoLifecycle.HINT_INVISIBLE);
    }
  }
}
