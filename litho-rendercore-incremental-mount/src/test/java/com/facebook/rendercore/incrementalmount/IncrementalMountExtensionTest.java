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

package com.facebook.rendercore.incrementalmount;

import static org.assertj.core.api.Java6Assertions.assertThat;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.test.core.app.ApplicationProvider;
import com.facebook.rendercore.HostView;
import com.facebook.rendercore.MountState;
import com.facebook.rendercore.Node.LayoutResult;
import com.facebook.rendercore.RenderTree;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.rendercore.RootHostView;
import com.facebook.rendercore.extensions.ExtensionState;
import com.facebook.rendercore.extensions.RenderCoreExtension;
import com.facebook.rendercore.incrementalmount.IncrementalMountExtension.IncrementalMountExtensionState;
import com.facebook.rendercore.testing.RenderCoreTestRule;
import com.facebook.rendercore.testing.SimpleLayoutResult;
import com.facebook.rendercore.testing.SimpleViewUnit;
import com.facebook.rendercore.testing.SimpleWrapperNode;
import com.facebook.rendercore.testing.TestHost;
import com.facebook.rendercore.testing.TestHostRenderUnit;
import com.facebook.rendercore.testing.TestRenderUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 16)
public class IncrementalMountExtensionTest {

  public final @Rule RenderCoreTestRule mRenderCoreTestRule = new RenderCoreTestRule();

  @Test
  public void whenVisibleBoundsIsEqualToHierarchy_shouldMountEverything() {
    final Context c = mRenderCoreTestRule.getContext();
    final RenderCoreExtension[] extensions =
        new RenderCoreExtension[] {new IncrementalMountRenderCoreExtension(TestProvider.INSTANCE)};

    final LayoutResult<?> root =
        SimpleLayoutResult.create()
            .renderUnit(new SimpleViewUnit(new TextView(c), 1))
            .width(100)
            .height(100)
            .build();

    mRenderCoreTestRule
        .useExtensions(extensions)
        .useRootNode(new SimpleWrapperNode(root))
        .setSizePx(100, 100)
        .render();

    final HostView host = (HostView) mRenderCoreTestRule.getRootHost();

    assertThat(host.getChildCount()).isEqualTo(1);
    assertThat(host.getChildAt(0)).isInstanceOf(TextView.class);
  }

  @Test
  public void whenVisibleBoundsIntersectsHierarchy_shouldMountEverything() {
    final Context c = mRenderCoreTestRule.getContext();
    final RenderCoreExtension[] extensions =
        new RenderCoreExtension[] {new IncrementalMountRenderCoreExtension(TestProvider.INSTANCE)};

    final LayoutResult<?> root =
        SimpleLayoutResult.create()
            .renderUnit(new SimpleViewUnit(new TextView(c), 1))
            .width(100)
            .height(100)
            .build();

    mRenderCoreTestRule
        .useExtensions(extensions)
        .useRootNode(new SimpleWrapperNode(root))
        .setSizePx(50, 50)
        .render();

    final HostView host = (HostView) mRenderCoreTestRule.getRootHost();

    assertThat(host.getChildCount()).isEqualTo(1);
    assertThat(host.getChildAt(0)).isInstanceOf(TextView.class);
  }

  @Test
  public void whenVisibleBoundsIsZero_shouldNotMountAnything() {
    final Context c = mRenderCoreTestRule.getContext();
    final RenderCoreExtension[] extensions =
        new RenderCoreExtension[] {new IncrementalMountRenderCoreExtension(TestProvider.INSTANCE)};

    final LayoutResult<?> root =
        SimpleLayoutResult.create()
            .renderUnit(new SimpleViewUnit(new TextView(c), 1))
            .width(100)
            .height(100)
            .build();

    mRenderCoreTestRule
        .useExtensions(extensions)
        .useRootNode(new SimpleWrapperNode(root))
        .setSizePx(0, 0)
        .render();

    final HostView host = (HostView) mRenderCoreTestRule.getRootHost();

    assertThat(host.getChildCount()).isEqualTo(0);
  }

  @Test
  public void whenVisibleBoundsChangeWithBoundaryConditions_shouldMountAndUnMountCorrectly() {
    final Context c = mRenderCoreTestRule.getContext();

    final FrameLayout parent = new FrameLayout(c);
    parent.measure(
        View.MeasureSpec.makeMeasureSpec(10, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY));
    parent.layout(0, 0, 10, 300);

    final RootHostView host = new RootHostView(c);
    parent.addView(host);

    final RenderCoreExtension[] extensions =
        new RenderCoreExtension[] {new IncrementalMountRenderCoreExtension(TestProvider.INSTANCE)};

    final LayoutResult<?> root =
        SimpleLayoutResult.create()
            .renderUnit(new SimpleViewUnit(new HostView(c), 0))
            .width(100)
            .height(300)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new SimpleViewUnit(new TextView(c), 1))
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new SimpleViewUnit(new TextView(c), 2))
                    .y(100)
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new SimpleViewUnit(new TextView(c), 3))
                    .y(200)
                    .width(100)
                    .height(100))
            .build();

    mRenderCoreTestRule
        .useRootHost(host)
        .useExtensions(extensions)
        .useRootNode(new SimpleWrapperNode(root))
        .setSizePx(100, 300)
        .render();

    assertThat(host.getChildCount()).isEqualTo(3);

    // Translate host up to boundary condition.
    host.offsetTopAndBottom(100);
    assertThat(host.getChildCount()).isEqualTo(3);

    // Translate host beyond the boundary condition.
    host.offsetTopAndBottom(1); // 100 + 1 = 101
    assertThat(host.getChildCount()).isEqualTo(2);

    // Translate host up to boundary condition is reverse direction.
    host.offsetTopAndBottom(-1 - 100 - 100); // 101 - 1 - 100 - 100 = -100
    assertThat(host.getChildCount()).isEqualTo(3);

    // Translate host beyond the boundary condition is reverse direction.
    host.offsetTopAndBottom(-1); // -100 - 1 = -101
    assertThat(host.getChildCount()).isEqualTo(2);
  }

  @Test
  public void whenVisibleBoundsChangeHorizontally_shouldMountAndUnMountCorrectly() {
    final Context c = mRenderCoreTestRule.getContext();

    final FrameLayout parent = new FrameLayout(c);
    parent.measure(
        View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY));
    parent.layout(0, 0, 300, 100);

    final RootHostView host = new RootHostView(c);
    parent.addView(host);

    final RenderCoreExtension[] extensions =
        new RenderCoreExtension[] {new IncrementalMountRenderCoreExtension(TestProvider.INSTANCE)};

    final LayoutResult<?> root =
        SimpleLayoutResult.create()
            .renderUnit(new SimpleViewUnit(new HostView(c), 0))
            .width(300)
            .height(100)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new SimpleViewUnit(new TextView(c), 1))
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new SimpleViewUnit(new TextView(c), 2))
                    .x(100)
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new SimpleViewUnit(new TextView(c), 3))
                    .x(200)
                    .width(100)
                    .height(100))
            .build();

    mRenderCoreTestRule
        .useRootHost(host)
        .useExtensions(extensions)
        .useRootNode(new SimpleWrapperNode(root))
        .setSizePx(300, 100)
        .render();

    assertThat(host.getChildCount()).isEqualTo(3);

    // Translate host up to boundary condition.
    host.offsetLeftAndRight(99);
    assertThat(host.getChildCount()).isEqualTo(3);

    // Translate host beyond the boundary condition.
    host.offsetLeftAndRight(1); // 100 + 1 = 101
    assertThat(host.getChildCount()).isEqualTo(2);

    // Translate host up to boundary condition is reverse direction.
    host.offsetLeftAndRight(-1 - 99 - 99); // 101 - 1 - 100 - 100 = -100
    assertThat(host.getChildCount()).isEqualTo(3);

    // Translate host beyond the boundary condition is reverse direction.
    host.offsetLeftAndRight(-1); // -100 - 1 = -101
    assertThat(host.getChildCount()).isEqualTo(2);
  }

  @Test
  public void whenPreviousHostIsMovedOutOfBounds_shouldMountItemsCorrectly() {
    final Context c = mRenderCoreTestRule.getContext();

    final FrameLayout parent = new FrameLayout(c);
    parent.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(99, View.MeasureSpec.EXACTLY));
    parent.layout(0, 0, 100, 200);

    final RootHostView host = new RootHostView(c);
    parent.addView(host);

    final RenderCoreExtension[] extensions =
        new RenderCoreExtension[] {new IncrementalMountRenderCoreExtension(TestProvider.INSTANCE)};

    final LayoutResult<?> root =
        SimpleLayoutResult.create()
            .width(100)
            .height(201)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new SimpleViewUnit(new HostView(c), 1))
                    .width(100)
                    .height(100)
                    .child(
                        SimpleLayoutResult.create()
                            .renderUnit(new SimpleViewUnit(new HostView(c), 2))
                            .width(100)
                            .height(100)
                            .child(
                                SimpleLayoutResult.create()
                                    .renderUnit(new SimpleViewUnit(new TextView(c), 3))
                                    .width(100)
                                    .height(100))))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new SimpleViewUnit(new HostView(c), 4))
                    .y(100)
                    .width(100)
                    .height(101))
            .build();

    host.setTranslationY(100);

    mRenderCoreTestRule
        .useRootHost(host)
        .useExtensions(extensions)
        .useRootNode(new SimpleWrapperNode(root))
        .setSizePx(100, 201)
        .render();

    assertThat(host.getChildCount()).isEqualTo(2);
    assertThat(((HostView) host.getChildAt(0)).getChildCount()).isEqualTo(1);

    final LayoutResult<?> newRoot =
        SimpleLayoutResult.create()
            .width(100)
            .height(201)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new SimpleViewUnit(new HostView(c), 1))
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new SimpleViewUnit(new HostView(c), 4))
                    .y(100)
                    .width(100)
                    .height(101)
                    .child(
                        SimpleLayoutResult.create()
                            .renderUnit(new SimpleViewUnit(new HostView(c), 2)) // Host changed.
                            .y(1)
                            .width(100)
                            .height(100)
                            .child(
                                SimpleLayoutResult.create()
                                    .renderUnit(new SimpleViewUnit(new TextView(c), 3))
                                    .width(100)
                                    .height(100))))
            .build();

    mRenderCoreTestRule.useRootNode(new SimpleWrapperNode(newRoot)).setSizePx(100, 201).render();

    // Un-mounts the Host with id 2.
    assertThat(((HostView) host.getChildAt(0)).getChildCount()).isEqualTo(0);
    // Host with id 2 is not mounted because it is outside of visible bounds.
    assertThat(((HostView) host.getChildAt(1)).getChildCount()).isEqualTo(0);

    // Scroll Host with id 2 into the view port
    host.offsetTopAndBottom(-2);

    // Host with id 2 is mounted because it enters the visible bounds.
    assertThat(((HostView) host.getChildAt(1)).getChildCount()).isEqualTo(1);
  }

  @Test
  public void whenItemBoundsAreOutsideHostBounds_shouldMountHostBeforeItem() {
    final Context c = mRenderCoreTestRule.getContext();

    final FrameLayout parent = new FrameLayout(c);
    parent.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY));
    parent.layout(0, 0, 100, 100);

    final RootHostView host = new RootHostView(c);
    parent.addView(host);

    final RenderCoreExtension[] extensions =
        new RenderCoreExtension[] {new IncrementalMountRenderCoreExtension(TestProvider.INSTANCE)};

    final LayoutResult<?> root =
        SimpleLayoutResult.create()
            .renderUnit(new SimpleViewUnit(new HostView(c), 1))
            .width(100)
            .height(100)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new SimpleViewUnit(new TextView(c), 3))
                    .y(110)
                    .width(100)
                    .height(100))
            .build();

    host.setTranslationY(-105);

    mRenderCoreTestRule
        .useRootHost(host)
        .useExtensions(extensions)
        .useRootNode(new SimpleWrapperNode(root))
        .setSizePx(100, 210)
        .render();

    assertThat(host.getChildCount()).isEqualTo(1);
    assertThat(((HostView) host.getChildAt(0)).getChildCount()).isEqualTo(1);
  }

  @Test
  public void testNegativeMarginChild_forcesHostMount() {
    final Context c = ApplicationProvider.getApplicationContext();
    final IncrementalMountExtension extension = IncrementalMountExtension.getInstance(true);

    final MountState mountState = createMountState(c);
    mountState.registerMountDelegateExtension(extension);

    final ExtensionState<IncrementalMountExtensionState> extensionState =
        mountState.getExtensionState(extension);

    final IncrementalMountOutput rootHost =
        new IncrementalMountOutput(0, 0, new Rect(0, 100, 100, 200), null);
    final TestHostRenderUnit hostRenderUnit = new TestHostRenderUnit(0);
    final RenderTreeNode hostRenderTreeNode =
        new RenderTreeNode(null, hostRenderUnit, null, rootHost.getBounds(), null, 0);

    final IncrementalMountOutput host1 =
        new IncrementalMountOutput(1, 1, new Rect(0, 100, 50, 200), rootHost);
    final TestHostRenderUnit host1RenderUnit = new TestHostRenderUnit(1);
    final RenderTreeNode host1RTN =
        new RenderTreeNode(hostRenderTreeNode, host1RenderUnit, null, host1.getBounds(), null, 0);

    final IncrementalMountOutput child1 =
        new IncrementalMountOutput(2, 2, new Rect(0, 85, 50, 190), host1);
    final TestRenderUnit child1RenderUnit = new TestRenderUnit(2);
    final RenderTreeNode child11RTN =
        new RenderTreeNode(host1RTN, child1RenderUnit, null, child1.getBounds(), null, 0);

    final IncrementalMountOutput host2 =
        new IncrementalMountOutput(3, 3, new Rect(50, 100, 50, 200), rootHost);
    final TestHostRenderUnit host2RenderUnit = new TestHostRenderUnit(3);
    final RenderTreeNode host2RTN =
        new RenderTreeNode(hostRenderTreeNode, host2RenderUnit, null, host2.getBounds(), null, 1);

    final IncrementalMountOutput child2 =
        new IncrementalMountOutput(4, 4, new Rect(50, 100, 50, 200), host2);
    final TestRenderUnit child2RenderUnit = new TestRenderUnit(4);
    final RenderTreeNode child2RTN =
        new RenderTreeNode(host2RTN, child2RenderUnit, null, child2.getBounds(), null, 0);

    final RenderTreeNode[] flatList =
        new RenderTreeNode[] {hostRenderTreeNode, host1RTN, child11RTN, host2RTN, child2RTN};

    final RenderTree renderTree =
        new RenderTree(
            hostRenderTreeNode,
            flatList,
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            null);

    final TestIncrementalMountExtensionInput input1 =
        new TestIncrementalMountExtensionInput(rootHost, host1, child1, host2, child2);

    extension.beforeMount(extensionState, input1, new Rect(0, 80, 100, 90));
    mountState.mount(renderTree);

    assertThat(extensionState.ownsReference(1)).isTrue();
    assertThat(extensionState.ownsReference(2)).isTrue();
  }

  @Test
  public void testNegativeMarginChild_hostMovedAndUnmounted_forcesHostMount() {
    final Context c = ApplicationProvider.getApplicationContext();
    final IncrementalMountExtension extension = IncrementalMountExtension.getInstance(true);

    final MountState mountState = createMountState(c);
    mountState.registerMountDelegateExtension(extension);

    final ExtensionState<IncrementalMountExtensionState> extensionState =
        mountState.getExtensionState(extension);

    final IncrementalMountOutput rootHost =
        new IncrementalMountOutput(0, 0, new Rect(0, 100, 100, 200), null);
    final TestHostRenderUnit hostRenderUnit = new TestHostRenderUnit(0);
    final RenderTreeNode hostRenderTreeNode =
        new RenderTreeNode(null, hostRenderUnit, null, rootHost.getBounds(), null, 0);

    final IncrementalMountOutput host1 =
        new IncrementalMountOutput(1, 1, new Rect(0, 100, 50, 200), rootHost);
    final TestHostRenderUnit host1RenderUnit = new TestHostRenderUnit(1);
    final RenderTreeNode host1RTN =
        new RenderTreeNode(hostRenderTreeNode, host1RenderUnit, null, host1.getBounds(), null, 0);

    final IncrementalMountOutput child1 =
        new IncrementalMountOutput(2, 2, new Rect(0, 85, 50, 190), host1);
    final TestRenderUnit child1RenderUnit = new TestRenderUnit(2);
    final RenderTreeNode child11RTN =
        new RenderTreeNode(host1RTN, child1RenderUnit, null, child1.getBounds(), null, 0);

    final IncrementalMountOutput host2 =
        new IncrementalMountOutput(3, 3, new Rect(50, 100, 50, 200), rootHost);
    final TestHostRenderUnit host2RenderUnit = new TestHostRenderUnit(3);
    final RenderTreeNode host2RTN =
        new RenderTreeNode(hostRenderTreeNode, host2RenderUnit, null, host2.getBounds(), null, 1);

    final IncrementalMountOutput child2 =
        new IncrementalMountOutput(4, 4, new Rect(50, 100, 50, 200), host2);
    final TestRenderUnit child2RenderUnit = new TestRenderUnit(4);
    final RenderTreeNode child2RTN =
        new RenderTreeNode(host2RTN, child2RenderUnit, null, child2.getBounds(), null, 0);

    final RenderTreeNode[] flatList =
        new RenderTreeNode[] {
          hostRenderTreeNode, host2RTN, child2RTN, host1RTN, child11RTN,
        };

    final RenderTree renderTree =
        new RenderTree(
            hostRenderTreeNode,
            flatList,
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            null);

    final TestIncrementalMountExtensionInput input =
        new TestIncrementalMountExtensionInput(rootHost, host2, child2, host1, child1);

    extension.beforeMount(extensionState, input, new Rect(0, 80, 100, 90));
    mountState.mount(renderTree);

    assertThat(extensionState.ownsReference(1)).isTrue();
    assertThat(extensionState.ownsReference(2)).isTrue();

    final IncrementalMountOutput host1_reparented =
        new IncrementalMountOutput(1, 1, new Rect(0, 100, 50, 200), host2);
    final TestHostRenderUnit host1RenderUnit_reparented = new TestHostRenderUnit(1);
    final RenderTreeNode host1RTN_reparented =
        new RenderTreeNode(
            host2RTN, host1RenderUnit_reparented, null, host1_reparented.getBounds(), null, 1);

    final IncrementalMountOutput child1_reparented =
        new IncrementalMountOutput(2, 2, new Rect(0, 85, 50, 190), host1_reparented);
    final TestRenderUnit child1RenderUnit_reparented = new TestRenderUnit(2);
    final RenderTreeNode child11RTN_reparented =
        new RenderTreeNode(
            host1RTN_reparented,
            child1RenderUnit_reparented,
            null,
            child1_reparented.getBounds(),
            null,
            0);

    final RenderTreeNode[] flatList_reparented =
        new RenderTreeNode[] {
          hostRenderTreeNode, host2RTN, child2RTN, host1RTN_reparented, child11RTN_reparented
        };

    final RenderTree renderTree_reparented =
        new RenderTree(
            hostRenderTreeNode,
            flatList_reparented,
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            null);
    final TestIncrementalMountExtensionInput input_reparented =
        new TestIncrementalMountExtensionInput(
            rootHost, host2, child2, host1_reparented, child1_reparented);

    extension.beforeMount(extensionState, input_reparented, new Rect(0, 80, 100, 90));
    mountState.mount(renderTree_reparented);

    assertThat(extensionState.ownsReference(1)).isTrue();
    assertThat(extensionState.ownsReference(2)).isTrue();
  }

  private static MountState createMountState(Context c) {
    return new MountState(new TestHost(c));
  }
}
