/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Rect;
import android.widget.FrameLayout;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.LegacyLithoViewRule;
import com.facebook.litho.testing.TestComponent;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.Text;
import com.facebook.rendercore.MountDelegateTarget;
import com.facebook.rendercore.Reducer;
import com.facebook.rendercore.RenderTree;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.rendercore.extensions.ExtensionState;
import com.facebook.rendercore.visibility.VisibilityMountExtension;
import com.facebook.yoga.YogaEdge;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class VisibilityEventsWithVisibilityExtensionTest {
  private static final int LEFT = 0;
  private static final int RIGHT = 10;

  private ComponentContext mContext;
  private LithoView mLithoView;
  private FrameLayout mParent;

  public final @Rule LegacyLithoViewRule mLegacyLithoViewRule = new LegacyLithoViewRule();

  @Before
  public void setup() {
    mContext = mLegacyLithoViewRule.getContext();
    mLithoView = new LithoView(mContext);
    mLegacyLithoViewRule.useLithoView(mLithoView);

    mParent = new FrameLayout(mContext.getAndroidContext());
    mParent.setLeft(0);
    mParent.setTop(0);
    mParent.setRight(10);
    mParent.setBottom(10);
    mParent.addView(mLithoView);
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

    mLegacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(5, EXACTLY))
        .measure();

    final LayoutState layoutState = mock(LayoutState.class);
    final RenderTree renderTree = mock(RenderTree.class);
    final RenderTreeNode rootNode = mock(RenderTreeNode.class);
    when(layoutState.toRenderTree()).thenReturn(renderTree);
    when(renderTree.getRenderTreeData()).thenReturn(layoutState);
    when(renderTree.getRenderTreeNodeAtIndex(0)).thenReturn(rootNode);
    when(rootNode.getRenderUnit()).thenReturn(Reducer.sRootHostRenderUnit);

    mLegacyLithoViewRule.getLithoView().setMountStateDirty();

    VisibilityMountExtension visibilityExtension = spy(VisibilityMountExtension.getInstance());

    useVisibilityOutputsExtension(mLegacyLithoViewRule.getLithoView(), visibilityExtension);

    final Rect rect = new Rect(0, 0, RIGHT, 10);

    mLegacyLithoViewRule.getLithoView().mount(layoutState, new Rect(0, 0, RIGHT, 10), false);

    verify(visibilityExtension).beforeMount(any(ExtensionState.class), eq(layoutState), eq(rect));
  }

  @Test
  public void visibilityExtensionOnUnmountAllItems_shouldUnmount() {
    final Component content = Text.create(mContext).text("hello world").build();
    final EventHandler<VisibleEvent> visibleEventHandler = new EventHandler<>(content, 2);

    final Component root =
        Column.create(mContext)
            .child(Wrapper.create(mContext).delegate(content).visibleHandler(visibleEventHandler))
            .build();

    mLegacyLithoViewRule.setRoot(root).attachToWindow().measure().layout();

    final LayoutState layoutState = mock(LayoutState.class);
    final RenderTree renderTree = mock(RenderTree.class);
    final RenderTreeNode rootNode = mock(RenderTreeNode.class);
    when(layoutState.toRenderTree()).thenReturn(renderTree);
    when(renderTree.getRenderTreeData()).thenReturn(layoutState);
    when(renderTree.getRenderTreeNodeAtIndex(0)).thenReturn(rootNode);
    when(rootNode.getRenderUnit()).thenReturn(Reducer.sRootHostRenderUnit);

    mLegacyLithoViewRule.getLithoView().setMountStateDirty();

    VisibilityMountExtension visibilityExtension = spy(VisibilityMountExtension.getInstance());

    useVisibilityOutputsExtension(mLegacyLithoViewRule.getLithoView(), visibilityExtension);

    mLegacyLithoViewRule.getLithoView().unmountAllItems();

    verify(visibilityExtension).onUnbind(any(ExtensionState.class));
    verify(visibilityExtension).onUnmount(any(ExtensionState.class));
  }

  private void useVisibilityOutputsExtension(
      LithoView lithoView, VisibilityMountExtension visibilityOutputsExtension) {
    if (ComponentsConfiguration.delegateToRenderCoreMount) {
      LithoHostListenerCoordinator lithoHostListenerCoordinator =
          Whitebox.getInternalState(lithoView, "mLithoHostListenerCoordinator");
      lithoHostListenerCoordinator.useVisibilityExtension(visibilityOutputsExtension);
    } else {
      final MountDelegateTarget mountState = lithoView.getMountDelegateTarget();
      Whitebox.setInternalState(mountState, "mVisibilityExtension", visibilityOutputsExtension);
    }
  }
}
