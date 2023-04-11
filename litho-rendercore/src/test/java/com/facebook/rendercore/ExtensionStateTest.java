// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.rendercore;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

import android.util.Pair;
import com.facebook.rendercore.extensions.ExtensionState;
import com.facebook.rendercore.extensions.MountExtension;
import com.facebook.rendercore.extensions.RenderCoreExtension;
import com.facebook.rendercore.testing.TestRenderCoreExtension;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ExtensionStateTest {

  @Test
  public void testReleaseAll_decrementMountDelegate() {
    final MountDelegateTarget mountDelegateTarget = mock(MountDelegateTarget.class);
    final MountDelegate mountDelegate =
        new MountDelegate(mountDelegateTarget, RenderCoreSystrace.getInstance());
    final TestMountExtension mountExtension1 = new TestMountExtension();
    final TestMountExtension mountExtension2 = new TestMountExtension();

    List<Pair<RenderCoreExtension<?, ?>, Object>> extensions = new ArrayList<>();

    extensions.add(new Pair(new TestRenderCoreExtension(null, mountExtension1, null), null));
    extensions.add(new Pair(new TestRenderCoreExtension(null, mountExtension2, null), null));
    mountDelegate.registerExtensions(extensions);

    final ExtensionState extensionState1 = mountDelegate.getExtensionStates().get(0);
    final ExtensionState extensionState2 = mountDelegate.getExtensionStates().get(1);

    mountExtension1.acquireRef(extensionState1, 0, 0);
    mountExtension2.acquireRef(extensionState2, 0, 0);

    assertThat(mountDelegate.getRefCount(0)).isEqualTo(2);

    mountExtension1.releaseAll(extensionState1);
    assertThat(mountDelegate.getRefCount(0)).isEqualTo(1);
  }

  private static class TestMountExtension extends MountExtension<Void, Void> {

    @Override
    protected Void createState() {
      return null;
    }

    void acquireRef(ExtensionState state, int id, int position) {
      state.acquireMountReference(id, true);
    }

    @Override
    public boolean canPreventMount() {
      return true;
    }

    void releaseAll(ExtensionState state) {
      state.releaseAllAcquiredReferences();
    }
  }
}
