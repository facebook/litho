// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.rendercore.incrementalmount;

import static org.assertj.core.api.Java6Assertions.assertThat;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import com.facebook.rendercore.MountState;
import com.facebook.rendercore.RenderTree;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.extensions.ExtensionState;
import com.facebook.rendercore.incrementalmount.IncrementalMountExtension.IncrementalMountExtensionState;
import com.facebook.rendercore.testing.TestHost;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class IncrementalMountExtensionTest {

  @Test
  public void testNegativeMarginChild_forcesHostMount() {
    final Context c = RuntimeEnvironment.application;
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
    final Context c = RuntimeEnvironment.application;
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

  private static class TestRenderUnit extends RenderUnit<View> {

    private final long mId;

    public TestRenderUnit(long id) {
      super(RenderType.VIEW);
      mId = id;
    }

    @Override
    public View createContent(Context c) {
      return new View(c);
    }

    @Override
    public long getId() {
      return mId;
    }
  }

  private static class TestHostRenderUnit extends RenderUnit<View> {

    private final long mId;

    public TestHostRenderUnit(long id) {
      super(RenderType.VIEW);
      mId = id;
    }

    @Override
    public View createContent(Context c) {
      return new TestHost(c);
    }

    @Override
    public long getId() {
      return mId;
    }
  }
}
