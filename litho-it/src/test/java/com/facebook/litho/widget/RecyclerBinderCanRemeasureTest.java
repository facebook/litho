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

package com.facebook.litho.widget;

import static android.view.View.MeasureSpec.UNSPECIFIED;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.litho.widget.RecyclerBinderTest.NO_OP_CHANGE_SET_COMPLETE_CALLBACK;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

import androidx.recyclerview.widget.OrientationHelper;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.EventHandler;
import com.facebook.litho.Size;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

/** Tests for {@link RecyclerBinder} with canRemeasure enabled. */
@RunWith(ComponentsTestRunner.class)
public class RecyclerBinderCanRemeasureTest {

  private ComponentContext mComponentContext;
  private TestRecyclerView mRecyclerView;

  @Before
  public void setup() {
    mComponentContext = new ComponentContext(RuntimeEnvironment.application);
    mRecyclerView = new TestRecyclerView(mComponentContext.getAndroidContext());
  }

  @Test
  public void
      testHorizontalUnboundHeightPerformRemeasureWithDataChangeFromPreviouslyMeasuredEmptyList() {
    final int widthSpec = makeSizeSpec(1000, EXACTLY);
    final int heightSpec = makeSizeSpec(0, UNSPECIFIED);

    RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .layoutInfo(
                new LinearLayoutInfo(mComponentContext, OrientationHelper.HORIZONTAL, false))
            .build(mComponentContext);

    recyclerBinder.mount(mRecyclerView);
    Size size = new Size();
    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.width).isEqualTo(1000);
    assertThat(size.height).isEqualTo(0);

    int NUM_TO_INSERT = 3;
    final ArrayList<RenderInfo> newRenderInfos = new ArrayList<>();
    for (int i = 0; i < NUM_TO_INSERT; i++) {
      final Component component =
          TestDrawableComponent.create(mComponentContext)
              .measuredWidth(400)
              .measuredHeight(100)
              .build();
      newRenderInfos.add(ComponentRenderInfo.create().component(component).build());
    }

    recyclerBinder.insertRangeAt(0, newRenderInfos);
    recyclerBinder.notifyChangeSetComplete(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    // Verify remeasure is triggered through View#postOnAnimation(Runnable)
    assertThat(mRecyclerView.getPostAnimationRunnableList().size()).isEqualTo(1);
    assertThat(mRecyclerView.getPostAnimationRunnableList().get(0))
        .isSameAs(recyclerBinder.mRemeasureRunnable);

    recyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));
    assertThat(size.width).isEqualTo(1000);
    assertThat(size.height).isEqualTo(100);
  }
}
