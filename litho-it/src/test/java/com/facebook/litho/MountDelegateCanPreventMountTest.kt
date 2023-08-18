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

package com.facebook.litho

import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.MountDelegate
import com.facebook.rendercore.MountDelegateTarget
import com.facebook.rendercore.RenderTreeNode
import com.facebook.rendercore.extensions.InformsMountCallback
import com.facebook.rendercore.extensions.MountExtension
import java.lang.IllegalStateException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(LithoTestRunner::class)
class MountDelegateCanPreventMountTest {

  @Test
  fun testIsLockedForMount() {
    // Create test LayoutOutputs with unique ids
    val lithoRenderUnit1: LithoRenderUnit = mock()
    val layoutOutput1: RenderTreeNode = mock()
    whenever(layoutOutput1.renderUnit).thenReturn(lithoRenderUnit1)
    whenever(lithoRenderUnit1.id).thenReturn(1L)
    val lithoRenderUnit2: LithoRenderUnit = mock()
    val layoutOutput2: RenderTreeNode = mock()
    whenever(layoutOutput2.renderUnit).thenReturn(lithoRenderUnit2)
    whenever(lithoRenderUnit2.id).thenReturn(2L)
    val mountDelegateTarget = mock<MountDelegateTarget>()
    val mountDelegate = MountDelegate(mountDelegateTarget, ComponentsSystrace.systrace)

    // When no extensions are present, calls to isLockedForMount default to true
    assertThat(mountDelegate.isLockedForMount(layoutOutput1)).isTrue
    assertThat(mountDelegate.isLockedForMount(layoutOutput2)).isTrue

    // When no extensions prevent mounting, calls to isLockedForMount default to true
    val mountExtension: MountExtension<*, *> = mock()
    mountDelegate.registerMountExtension(mountExtension)
    assertThat(mountDelegate.isLockedForMount(layoutOutput1)).isTrue
    assertThat(mountDelegate.isLockedForMount(layoutOutput2)).isTrue

    // When an extension can prevent mounting, calls to isLockedForMount default to false.
    val mountDelegateExtensionPreventMount: InformsMountExtension = mock()
    whenever(mountDelegateExtensionPreventMount.canPreventMount()).thenReturn(true)
    mountDelegate.registerMountExtension(mountDelegateExtensionPreventMount)
    assertThat(mountDelegate.isLockedForMount(layoutOutput1)).isFalse
    assertThat(mountDelegate.isLockedForMount(layoutOutput2)).isFalse

    // Acquiring a lock for a given LayoutOutput is reflected by isLockedForMount
    mountDelegate.acquireMountRef(layoutOutput1)
    assertThat(mountDelegate.isLockedForMount(layoutOutput1)).isTrue
    assertThat(mountDelegate.isLockedForMount(layoutOutput2)).isFalse

    // Releasing the lock is reflected by isLockedForMount
    mountDelegate.releaseMountRef(layoutOutput1)
    assertThat(mountDelegate.isLockedForMount(layoutOutput1)).isFalse
    assertThat(mountDelegate.isLockedForMount(layoutOutput2)).isFalse
  }

  @Test(expected = IllegalStateException::class)
  fun testImbalancedLocking() {
    val lithoRenderUnit = mock<LithoRenderUnit>()
    val layoutOutput1 = mock<RenderTreeNode>()
    whenever(layoutOutput1.renderUnit).thenReturn(lithoRenderUnit)
    whenever(lithoRenderUnit.id).thenReturn(1L)
    val mountDelegateTarget = mock<MountDelegateTarget>()
    val mountDelegate = MountDelegate(mountDelegateTarget, ComponentsSystrace.systrace)
    val mountDelegateExtensionPreventMount: InformsMountExtension = mock()
    whenever(mountDelegateExtensionPreventMount.canPreventMount()).thenReturn(true)
    mountDelegate.registerMountExtension(mountDelegateExtensionPreventMount)
    mountDelegate.acquireMountRef(layoutOutput1)
    mountDelegate.releaseMountRef(layoutOutput1)
    mountDelegate.releaseMountRef(layoutOutput1)
  }

  private abstract class InformsMountExtension : MountExtension<Any, Any>(), InformsMountCallback
}
