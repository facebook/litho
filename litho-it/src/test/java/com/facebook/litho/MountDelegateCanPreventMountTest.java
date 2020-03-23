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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.rendercore.RenderTreeNode;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class MountDelegateCanPreventMountTest {

  @Test
  public void testIsLockedForMount() {
    // Create test LayoutOutputs with unique ids
    LithoRenderUnit lithoRenderUnit1 = mock(LithoRenderUnit.class);
    RenderTreeNode layoutOutput1 = mock(RenderTreeNode.class);
    when(layoutOutput1.getRenderUnit()).thenReturn(lithoRenderUnit1);
    when(lithoRenderUnit1.getId()).thenReturn(1l);

    LithoRenderUnit lithoRenderUnit2 = mock(LithoRenderUnit.class);
    RenderTreeNode layoutOutput2 = mock(RenderTreeNode.class);
    when(layoutOutput2.getRenderUnit()).thenReturn(lithoRenderUnit2);
    when(lithoRenderUnit2.getId()).thenReturn(2l);

    MountDelegate.MountDelegateTarget mountDelegateTarget =
        mock(MountDelegate.MountDelegateTarget.class);
    MountDelegate mountDelegate = new MountDelegate(mountDelegateTarget);

    // When no extensions are present, calls to isLockedForMount default to true
    assertThat(mountDelegate.isLockedForMount(layoutOutput1)).isTrue();
    assertThat(mountDelegate.isLockedForMount(layoutOutput2)).isTrue();

    // When no extensions prevent mounting, calls to isLockedForMount default to true
    MountDelegateExtension mountDelegateExtension = mock(MountDelegateExtension.class);
    mountDelegate.addExtension(mountDelegateExtension);
    assertThat(mountDelegate.isLockedForMount(layoutOutput1)).isTrue();
    assertThat(mountDelegate.isLockedForMount(layoutOutput2)).isTrue();

    // When an extension can prevent mounting, calls to isLockedForMount default to false.
    MountDelegateExtension mountDelegateExtensionPreventMount = mock(MountDelegateExtension.class);
    when(mountDelegateExtensionPreventMount.canPreventMount()).thenReturn(true);
    mountDelegate.addExtension(mountDelegateExtensionPreventMount);
    assertThat(mountDelegate.isLockedForMount(layoutOutput1)).isFalse();
    assertThat(mountDelegate.isLockedForMount(layoutOutput2)).isFalse();

    // Acquiring a lock for a given LayoutOutput is reflected by isLockedForMount
    mountDelegate.acquireMountRef(layoutOutput1, 0, null, false);
    assertThat(mountDelegate.isLockedForMount(layoutOutput1)).isTrue();
    assertThat(mountDelegate.isLockedForMount(layoutOutput2)).isFalse();

    // Releasing the lock is reflected by isLockedForMount
    mountDelegate.releaseMountRef(layoutOutput1, 0, false);
    assertThat(mountDelegate.isLockedForMount(layoutOutput1)).isFalse();
    assertThat(mountDelegate.isLockedForMount(layoutOutput2)).isFalse();
  }

  @Test(expected = IllegalStateException.class)
  public void testInbalancedLocking() {
    LithoRenderUnit lithoRenderUnit = mock(LithoRenderUnit.class);
    RenderTreeNode layoutOutput1 = mock(RenderTreeNode.class);
    when(layoutOutput1.getRenderUnit()).thenReturn(lithoRenderUnit);
    when(lithoRenderUnit.getId()).thenReturn(1l);

    MountDelegate.MountDelegateTarget mountDelegateTarget =
        mock(MountDelegate.MountDelegateTarget.class);
    MountDelegate mountDelegate = new MountDelegate(mountDelegateTarget);
    MountDelegateExtension mountDelegateExtensionPreventMount = mock(MountDelegateExtension.class);
    when(mountDelegateExtensionPreventMount.canPreventMount()).thenReturn(true);
    mountDelegate.addExtension(mountDelegateExtensionPreventMount);

    mountDelegate.acquireMountRef(layoutOutput1, 0, null, false);
    mountDelegate.releaseMountRef(layoutOutput1, 0, false);
    mountDelegate.releaseMountRef(layoutOutput1, 0, false);
  }
}
