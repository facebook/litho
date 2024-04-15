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

package com.facebook.rendercore

import android.util.Pair
import com.facebook.rendercore.RenderCoreSystrace.getInstance
import com.facebook.rendercore.extensions.ExtensionState
import com.facebook.rendercore.extensions.InformsMountCallback
import com.facebook.rendercore.extensions.MountExtension
import com.facebook.rendercore.extensions.RenderCoreExtension
import com.facebook.rendercore.testing.TestRenderCoreExtension
import org.assertj.core.api.Java6Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExtensionStateTest {

  @Test
  fun testReleaseAll_decrementMountDelegate() {
    val mountDelegateTarget = Mockito.mock(MountDelegateTarget::class.java)
    val mountDelegate = MountDelegate(mountDelegateTarget, getInstance())
    val mountExtension1 = TestMountExtension()
    val mountExtension2 = TestMountExtension()
    val extensions: MutableList<Pair<RenderCoreExtension<*, *>, Any>> = ArrayList()
    extensions.add(
        Pair(
            TestRenderCoreExtension(null, mountExtension1, null) as RenderCoreExtension<*, *>?,
            null))
    extensions.add(
        Pair(
            TestRenderCoreExtension(null, mountExtension2, null) as RenderCoreExtension<*, *>?,
            null))
    mountDelegate.registerExtensions(extensions)
    val extensionState1 = mountDelegate.extensionStates[0]
    val extensionState2 = mountDelegate.extensionStates[1]
    mountExtension1.acquireRef(extensionState1, 0, 0)
    mountExtension2.acquireRef(extensionState2, 0, 0)
    Java6Assertions.assertThat(mountDelegate.getRefCount(0)).isEqualTo(2)
    mountExtension1.releaseAll(extensionState1)
    Java6Assertions.assertThat(mountDelegate.getRefCount(0)).isEqualTo(1)
  }

  private class TestMountExtension : MountExtension<Unit, Unit>(), InformsMountCallback {
    override fun createState() = Unit

    fun acquireRef(state: ExtensionState<*>, id: Int, position: Int) {
      state.acquireMountReference(id.toLong(), true)
    }

    override fun canPreventMount(): Boolean {
      return true
    }

    fun releaseAll(state: ExtensionState<*>) {
      state.releaseAllAcquiredReferences()
    }
  }
}
