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

package com.facebook.rendercore;

import static org.assertj.core.api.Java6Assertions.assertThat;

import android.graphics.Rect;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.facebook.rendercore.extensions.ExtensionState;
import com.facebook.rendercore.extensions.InformsMountCallback;
import com.facebook.rendercore.extensions.LayoutResultVisitor;
import com.facebook.rendercore.extensions.MountExtension;
import com.facebook.rendercore.extensions.OnItemCallbacks;
import com.facebook.rendercore.extensions.RenderCoreExtension;
import com.facebook.rendercore.extensions.VisibleBoundsCallbacks;
import com.facebook.rendercore.testing.LayoutResultWrappingNode;
import com.facebook.rendercore.testing.RenderCoreTestRule;
import com.facebook.rendercore.testing.SimpleLayoutResult;
import com.facebook.rendercore.testing.TestRenderCoreExtension;
import com.facebook.rendercore.testing.ViewAssertions;
import com.facebook.rendercore.testing.ViewWrapperUnit;
import com.facebook.rendercore.testing.match.ViewMatchNode;
import java.util.ArrayList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class RenderCoreExtensionTest {

  public final @Rule RenderCoreTestRule mRenderCoreTestRule = new RenderCoreTestRule();

  @Test
  public void onRenderWithExtension_shouldRenderView() {
    final LayoutResult root =
        SimpleLayoutResult.create()
            .renderUnit(new ViewWrapperUnit(new TextView(mRenderCoreTestRule.getContext()), 1))
            .width(100)
            .height(100)
            .build();

    final TrackingLayoutResultVisitor visitor = new TrackingLayoutResultVisitor();
    final TrackingMountExtension extension = new TrackingMountExtension();

    final RenderCoreExtension e1 = new RenderCoreExtension();
    final RenderCoreExtension e2 = new TestRenderCoreExtension();
    final RenderCoreExtension e3 = new TestRenderCoreExtension(visitor, ArrayList::new);
    final RenderCoreExtension e4 = new TestRenderCoreExtension(extension);

    mRenderCoreTestRule
        .useExtensions(new RenderCoreExtension[] {e1, e2, e3, e4})
        .useRootNode(new LayoutResultWrappingNode(root))
        .render();

    final View rootView = (View) mRenderCoreTestRule.getRootHost();
    ViewAssertions.assertThat(rootView)
        .matches(
            ViewMatchNode.forType(Host.class)
                .child(ViewMatchNode.forType(TextView.class).bounds(0, 0, 100, 100)));

    assertThat(visitor.count).isEqualTo(2);
    assertThat(extension.beforeMount).isEqualTo(1);
    assertThat(extension.afterMount).isEqualTo(1);
    assertThat(extension.onVisibleBoundsChanged).isEqualTo(0);

    rootView.offsetLeftAndRight(100);
    assertThat(extension.onVisibleBoundsChanged).isEqualTo(1);
    rootView.offsetTopAndBottom(100);
    assertThat(extension.onVisibleBoundsChanged).isEqualTo(2);
  }

  @Test
  public void onRenderWithNewExtensions_shouldRenderViewAndDiscardOldExtensions() {
    final LayoutResult root =
        SimpleLayoutResult.create()
            .renderUnit(new ViewWrapperUnit(new TextView(mRenderCoreTestRule.getContext()), 1))
            .width(100)
            .height(100)
            .build();

    final TrackingLayoutResultVisitor v1 = new TrackingLayoutResultVisitor();
    final TrackingMountExtension me1 = new TrackingMountExtension();

    final RenderCoreExtension e1 = new TestRenderCoreExtension(v1, ArrayList::new);
    final RenderCoreExtension e2 = new TestRenderCoreExtension(me1);

    mRenderCoreTestRule
        .useExtensions(new RenderCoreExtension[] {e1, e2})
        .useRootNode(new LayoutResultWrappingNode(root))
        .render();

    ViewAssertions.assertThat((View) mRenderCoreTestRule.getRootHost())
        .matches(
            ViewMatchNode.forType(Host.class)
                .child(ViewMatchNode.forType(TextView.class).bounds(0, 0, 100, 100)));

    assertThat(v1.count).isEqualTo(2);
    assertThat(me1.beforeMount).isEqualTo(1);
    assertThat(me1.afterMount).isEqualTo(1);
    assertThat(me1.onVisibleBoundsChanged).isEqualTo(0);

    // New Extensions
    final TrackingLayoutResultVisitor v2 = new TrackingLayoutResultVisitor();
    final TrackingMountExtension me2 = new TrackingMountExtension();

    final RenderCoreExtension e3 = new TestRenderCoreExtension(v2, ArrayList::new);
    final RenderCoreExtension e4 = new TestRenderCoreExtension(me2);

    // Next render
    mRenderCoreTestRule
        .useExtensions(new RenderCoreExtension[] {e3, e4})
        .useRootNode(new LayoutResultWrappingNode(root))
        .render();

    ViewAssertions.assertThat((View) mRenderCoreTestRule.getRootHost())
        .matches(
            ViewMatchNode.forType(Host.class)
                .child(ViewMatchNode.forType(TextView.class).bounds(0, 0, 100, 100)));

    // No interactions with the previous extensions.
    assertThat(v1.count).isEqualTo(2);
    assertThat(me1.beforeMount).isEqualTo(1);
    assertThat(me1.afterMount).isEqualTo(1);
    assertThat(me1.onVisibleBoundsChanged).isEqualTo(0);

    // Only interactions with the new extensions.
    assertThat(v2.count).isEqualTo(2);
    assertThat(me2.beforeMount).isEqualTo(1);
    assertThat(me2.afterMount).isEqualTo(1);
    assertThat(me2.onVisibleBoundsChanged).isEqualTo(0);
  }

  @Test
  public void onUnmountAllItemsWithExtensions_shouldCallbackAllExtensions() {
    final LayoutResult root =
        SimpleLayoutResult.create()
            .renderUnit(new ViewWrapperUnit(new TextView(mRenderCoreTestRule.getContext()), 1))
            .width(100)
            .height(100)
            .build();

    final TrackingMountExtension extension = new TrackingMountExtension();

    mRenderCoreTestRule
        .useExtensions(new RenderCoreExtension[] {new TestRenderCoreExtension(extension)})
        .useRootNode(new LayoutResultWrappingNode(root))
        .render();

    // should call
    assertThat(extension.beforeMount).isEqualTo(1);
    assertThat(extension.afterMount).isEqualTo(1);
    assertThat(extension.mountItem).isEqualTo(2);
    assertThat(extension.bindItem).isEqualTo(2);

    // should not call
    assertThat(extension.unmountItem).isEqualTo(0);
    assertThat(extension.unbindItem).isEqualTo(0);
    assertThat(extension.unmount).isEqualTo(0);
    assertThat(extension.unbind).isEqualTo(0);
    assertThat(extension.onVisibleBoundsChanged).isEqualTo(0);

    mRenderCoreTestRule.getRootHost().setRenderState(null);

    // should call
    assertThat(extension.unmountItem).isEqualTo(2);
    assertThat(extension.unbindItem).isEqualTo(2);
    assertThat(extension.unmount).isEqualTo(1);
    assertThat(extension.unbind).isEqualTo(1);
  }

  @Test
  public void onUnmountRootItem_shouldCallbackAllExtensions() {
    final LayoutResult root =
        SimpleLayoutResult.create()
            .renderUnit(new ViewWrapperUnit(new TextView(mRenderCoreTestRule.getContext()), 1))
            .width(100)
            .height(100)
            .build();

    final TrackingMountExtension extension = new TrackingMountExtension();

    mRenderCoreTestRule
        .useExtensions(new RenderCoreExtension[] {new TestRenderCoreExtension(extension)})
        .useRootNode(new LayoutResultWrappingNode(root))
        .render();

    RootHostView rootView = (RootHostView) mRenderCoreTestRule.getRootHost();

    extension.idsMarkedForRelease = new long[] {Reducer.sRootHostRenderUnit.getId()};

    rootView.offsetTopAndBottom(100);
    assertThat(extension.onVisibleBoundsChanged).isEqualTo(1);

    assertThat(extension.unmountItem).isEqualTo(2);
    assertThat(extension.unbindItem).isEqualTo(2);
  }

  public static class TrackingLayoutResultVisitor implements LayoutResultVisitor {

    int count;

    @Override
    public void visit(
        final @Nullable RenderTreeNode parent,
        final LayoutResult result,
        final Rect bounds,
        final int x,
        final int y,
        final int position,
        final @Nullable Object o) {
      count++;
    }
  }

  public static class TrackingMountExtension extends MountExtension
      implements VisibleBoundsCallbacks, OnItemCallbacks, InformsMountCallback {

    int beforeMount;
    int afterMount;
    int onVisibleBoundsChanged;
    int unmount;
    int unbind;
    int mountItem;
    int bindItem;
    int unmountItem;
    int unbindItem;

    @Nullable long[] idsMarkedForRelease;

    @Override
    public boolean canPreventMount() {
      return true;
    }

    @Override
    protected Object createState() {
      return null;
    }

    @Override
    public void beforeMount(
        ExtensionState extensionState, Object o, @Nullable Rect localVisibleRect) {
      beforeMount++;
    }

    @Override
    public void afterMount(ExtensionState extensionState) {
      afterMount++;
    }

    @Override
    public void onVisibleBoundsChanged(
        ExtensionState extensionState, @Nullable Rect localVisibleRect) {
      onVisibleBoundsChanged++;

      if (idsMarkedForRelease != null) {
        for (long id : idsMarkedForRelease) {

          // force acquire (without mounting) if not already acquired.
          if (!extensionState.ownsReference(id)) {
            extensionState.acquireMountReference(id, false);
          }

          extensionState.releaseMountReference(id, true);
        }
      }
    }

    @Override
    public void beforeMountItem(
        ExtensionState extensionState, RenderTreeNode renderTreeNode, int index) {
      // mount all
      if (!extensionState.ownsReference(renderTreeNode.getRenderUnit().getId())) {
        extensionState.acquireMountReference(renderTreeNode.getRenderUnit().getId(), false);
      }
    }

    @Override
    public void onUnmount(ExtensionState extensionState) {
      unmount++;
    }

    @Override
    public void onUnbind(ExtensionState extensionState) {
      unbind++;
    }

    @Override
    public void onBindItem(
        ExtensionState extensionState,
        RenderUnit renderUnit,
        Object content,
        @Nullable Object layoutData) {
      bindItem++;
    }

    @Override
    public void onUnbindItem(
        ExtensionState extensionState,
        RenderUnit renderUnit,
        Object content,
        @Nullable Object layoutData) {
      unbindItem++;
    }

    @Override
    public void onUnmountItem(
        ExtensionState extensionState,
        RenderUnit renderUnit,
        Object content,
        @Nullable Object layoutData) {
      unmountItem++;
    }

    @Override
    public void onMountItem(
        ExtensionState extensionState,
        RenderUnit renderUnit,
        Object content,
        @Nullable Object layoutData) {
      mountItem++;
    }

    @Override
    public boolean shouldUpdateItem(
        ExtensionState extensionState,
        RenderUnit previousRenderUnit,
        @Nullable Object previousLayoutData,
        RenderUnit nextRenderUnit,
        @Nullable Object nextLayoutData) {
      return false;
    }

    @Override
    public void onBoundsAppliedToItem(
        ExtensionState extensionState,
        RenderUnit renderUnit,
        Object content,
        @Nullable Object layoutData) {}
  }
}
