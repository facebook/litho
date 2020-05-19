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

import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.litho.testing.TestViewComponent.create;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Rect;
import android.widget.FrameLayout;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.TestComponent;
import com.facebook.litho.testing.Whitebox;
import com.facebook.rendercore.RenderTree;
import com.facebook.yoga.YogaEdge;
import java.util.Arrays;
import java.util.Collection;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;

@RunWith(ParameterizedRobolectricTestRunner.class)
public class VisibilityEventsWithVisibilityExtensionTest {
  private static final int LEFT = 0;
  private static final int RIGHT = 10;

  private ComponentContext mContext;
  private LithoView mLithoView;
  private FrameLayout mParent;
  private boolean configVisExtension;
  final boolean mUseMountDelegateTarget;
  final boolean mUseVisibilityExtensionInMountState;

  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();

  @ParameterizedRobolectricTestRunner.Parameters(
      name = "useMountDelegateTarget={0}, useVisibilityExtensionInMountState={1}")
  public static Collection data() {
    return Arrays.asList(
        new Object[][] {
          {false, true},
          {true, false}
        });
  }

  public VisibilityEventsWithVisibilityExtensionTest(
      boolean useMountDelegateTarget, boolean useVisibilityExtensionInMountState) {
    mUseMountDelegateTarget = useMountDelegateTarget;
    mUseVisibilityExtensionInMountState = useVisibilityExtensionInMountState;
  }

  @Before
  public void setup() {
    configVisExtension = ComponentsConfiguration.useVisibilityExtension;
    ComponentsConfiguration.useVisibilityExtension = mUseVisibilityExtensionInMountState;
    mContext = mLithoViewRule.getContext();
    mLithoView = new LithoView(mContext, mUseMountDelegateTarget, false);
    mLithoViewRule.useLithoView(mLithoView);

    mParent = new FrameLayout(mContext.getAndroidContext());
    mParent.setLeft(0);
    mParent.setTop(0);
    mParent.setRight(10);
    mParent.setBottom(10);
    mParent.addView(mLithoView);
  }

  @After
  public void cleanup() {
    ComponentsConfiguration.useVisibilityExtension = configVisExtension;
  }

  @Test
  public void visibilityExtensionInput_dirtyMountWithoutVisibilityProcessing_setInput() {
    final TestComponent content = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandler = new EventHandler<>(content, 2);

    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(content)
                    .visibleHandler(visibleEventHandler)
                    .widthPx(10)
                    .heightPx(5)
                    .marginPx(YogaEdge.TOP, 5))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(5, EXACTLY))
        .measure();

    final LayoutState layoutState = mock(LayoutState.class);
    final RenderTree renderTree = mock(RenderTree.class);
    when(layoutState.toRenderTree()).thenReturn(renderTree);
    when(renderTree.getRenderTreeData()).thenReturn(layoutState);

    mLithoViewRule.getLithoView().setMountStateDirty();

    final VisibilityOutputsExtension visibilityOutputsExtension =
        mock(VisibilityOutputsExtension.class);
    useVisibilityOutputsExtension(mLithoViewRule.getLithoView(), visibilityOutputsExtension);

    final Rect rect = new Rect(0, 0, RIGHT, 10);

    mLithoViewRule.getLithoView().mount(layoutState, new Rect(0, 0, RIGHT, 10), false);

    verify(visibilityOutputsExtension).beforeMount(layoutState, rect);
  }

  private void useVisibilityOutputsExtension(
      LithoView lithoView, VisibilityOutputsExtension visibilityOutputsExtension) {
    if (mUseMountDelegateTarget) {
      LithoHostListenerCoordinator lithoHostListenerCoordinator =
          Whitebox.getInternalState(lithoView, "mLithoHostListenerCoordinator");
      lithoHostListenerCoordinator.useVisibilityExtension(visibilityOutputsExtension);
    } else if (mUseVisibilityExtensionInMountState) {
      final MountState mountState = lithoView.getMountState();
      Whitebox.setInternalState(
          mountState, "mVisibilityOutputsExtension", visibilityOutputsExtension);
    }
  }
}
