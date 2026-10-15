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
import org.assertj.core.api.Assertions
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
            null,
        ),
    )
    extensions.add(
        Pair(
            TestRenderCoreExtension(null, mountExtension2, null) as RenderCoreExtension<*, *>?,
            null,
        ),
    )
    mountDelegate.registerExtensions(extensions)
    val extensionState1 = mountDelegate.extensionStates[0]
    val extensionState2 = mountDelegate.extensionStates[1]
    mountExtension1.acquireRef(extensionState1, 0, 0)
    mountExtension2.acquireRef(extensionState2, 0, 0)
    Assertions.assertThat(mountDelegate.getRefCount(0)).isEqualTo(2)
    mountExtension1.releaseAll(extensionState1)
    Assertions.assertThat(mountDelegate.getRefCount(0)).isEqualTo(1)
  }

  /**
   * Regression test for T274149346. A reference acquired while reference counting is disabled (e.g.
   * on an incremental-mount-disabled ComponentTree, where the IncrementalMountExtension is not
   * registered) grows the extension's private ownership set. If the IncrementalMountExtension is
   * later registered (e.g. a recycled LithoView rebound to an incremental-mount-enabled
   * ComponentTree), reference counting becomes enabled and releasing that reference must not
   * underflow the shared reference-count map.
   */
  @Test
  fun testReleaseAll_afterReferenceCountingBecomesEnabled_doesNotUnderflow() {
    val mountDelegateTarget = Mockito.mock(MountDelegateTarget::class.java)
    val mountDelegate = MountDelegate(mountDelegateTarget, getInstance())

    // A non-preventing extension (like TransitionsExtension) leaves reference counting disabled.
    val nonPreventing = TestNonPreventingMountExtension()
    val extensionState = mountDelegate.registerMountExtension(nonPreventing)

    // Acquire a reference while reference counting is still disabled.
    nonPreventing.acquireRef(extensionState, 0)

    // An incremental-mount-like extension registers, enabling reference counting.
    mountDelegate.registerMountExtension(TestMountExtension())

    // Releasing the previously-acquired reference must not throw or underflow.
    nonPreventing.releaseAll(extensionState)
    Assertions.assertThat(mountDelegate.getRefCount(0)).isEqualTo(0)
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

  private class TestNonPreventingMountExtension : MountExtension<Unit, Unit>() {
    override fun createState() = Unit

    fun acquireRef(state: ExtensionState<*>, id: Int) {
      state.acquireMountReference(id.toLong(), false)
    }

    fun releaseAll(state: ExtensionState<*>) {
      state.releaseAllAcquiredReferences()
    }
  }
}
