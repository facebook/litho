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

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Image
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.TestLithoView
import com.facebook.litho.testing.assertj.LithoAssertions
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.alpha
import com.facebook.litho.view.wrapInView
import com.facebook.rendercore.ContentAllocator
import com.facebook.rendercore.MountItemsPool
import com.facebook.rendercore.PoolingPolicy
import com.facebook.rendercore.dp
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class HostRecyclingTest {

  @get:Rule val mLithoTestRule = LithoTestRule()

  private lateinit var fakeMountItemPools: ComponentHostAcquireTrackingMountItemsPool

  @Before
  fun setup() {
    fakeMountItemPools = ComponentHostAcquireTrackingMountItemsPool()
    MountItemsPool.setMountContentPoolFactory { fakeMountItemPools }
  }

  @After
  fun tearDown() {
    MountItemsPool.setMountContentPoolFactory(null)
  }

  @Test
  fun `should never acquire or release host component into pools if recycling is disabled`() {
    val lithoView = createLithoViewForTest(hostRecyclingEnabled = false)

    lithoView.setRoot(TestComponent("Fabio"))
    LithoAssertions.assertThat(lithoView).hasVisibleText("Fabio")
    assertThat(fakeMountItemPools.numComponentHostAcquireRequests).isEqualTo(0)
    assertThat(fakeMountItemPools.numComponentHostsAcquiredFromPool).isEqualTo(0)
    assertThat(fakeMountItemPools.numComponentHostReleaseRequests).isEqualTo(0)

    lithoView.setRoot(null)
    assertThat(fakeMountItemPools.numComponentHostAcquireRequests).isEqualTo(0)
    assertThat(fakeMountItemPools.numComponentHostsAcquiredFromPool).isEqualTo(0)
    assertThat(fakeMountItemPools.numComponentHostReleaseRequests).isEqualTo(0)

    lithoView.setRoot(TestComponent("Michal"))
    assertThat(fakeMountItemPools.numComponentHostAcquireRequests).isEqualTo(0)
    assertThat(fakeMountItemPools.numComponentHostsAcquiredFromPool).isEqualTo(0)
    assertThat(fakeMountItemPools.numComponentHostReleaseRequests).isEqualTo(0)
  }

  @Test
  fun `should release and reuse host component if recycling is enabled`() {
    val lithoView = createLithoViewForTest(hostRecyclingEnabled = true)

    /* The [TestComponent] wraps a Drawable with a Row with alpha. This will force it to use a
    ComponentHost. Since host recycling is enabled, it should try to acquire it from the pool, but since
    its empty it will retrieve nothing.
     */
    lithoView.setRoot(TestComponent("Fabio"))
    LithoAssertions.assertThat(lithoView).hasVisibleText("Fabio")
    assertThat(fakeMountItemPools.numComponentHostAcquireRequests).isEqualTo(1)
    assertThat(fakeMountItemPools.numComponentHostsAcquiredFromPool).isEqualTo(0)
    assertThat(fakeMountItemPools.numComponentHostReleaseRequests).isEqualTo(0)

    /*
     * After setting a null root, it should try to release the host component.
     */
    lithoView.setRoot(null)
    assertThat(fakeMountItemPools.numComponentHostAcquireRequests).isEqualTo(1)
    assertThat(fakeMountItemPools.numComponentHostsAcquiredFromPool).isEqualTo(0)
    assertThat(fakeMountItemPools.numComponentHostReleaseRequests).isEqualTo(1)

    /*
     After setting a new Root, it should try to acquire it again and actually retrieve it from the pool
    */
    lithoView.setRoot(TestComponent("Michal"))
    LithoAssertions.assertThat(lithoView).hasVisibleText("Michal")
    assertThat(fakeMountItemPools.numComponentHostAcquireRequests).isEqualTo(2)
    assertThat(fakeMountItemPools.numComponentHostsAcquiredFromPool).isEqualTo(1)
    assertThat(fakeMountItemPools.numComponentHostReleaseRequests).isEqualTo(1)
  }

  private fun createLithoViewForTest(hostRecyclingEnabled: Boolean): TestLithoView {
    return mLithoTestRule.render(
        heightPx = 400,
        widthPx = 400,
        componentTree =
            ComponentTree.create(mLithoTestRule.context)
                .componentsConfiguration(
                    ComponentsConfiguration.defaultInstance.copy(
                        componentHostPoolingPolicy =
                            if (hostRecyclingEnabled) PoolingPolicy.Default
                            else PoolingPolicy.Disabled))
                .build()) {
          EmptyComponent()
        }
  }

  private class ComponentHostAcquireTrackingMountItemsPool : MountItemsPool.ItemPool {

    private var _numComponentHostsAcquiredFromPool = 0
    private var _numComponentHostAcquireRequests = 0
    private var _numComponentHostsReleaseRequests = 0

    /** This is the number of times the [release] method was called for a [HostComponent] */
    val numComponentHostReleaseRequests: Int
      get() = _numComponentHostsReleaseRequests

    /** This is the number of times the [acquire] method was called for a [HostComponent] */
    val numComponentHostAcquireRequests
      get() = _numComponentHostAcquireRequests

    /**
     * This is the number of times where the [acquire] returned a non-null mount [HostComponent].
     */
    val numComponentHostsAcquiredFromPool
      get() = _numComponentHostsAcquiredFromPool

    private val itemPool = MountItemsPool.DefaultItemPool(HostComponent::class.java, 5)

    override fun acquire(contentAllocator: ContentAllocator<*>): Any? {
      val poolKey = contentAllocator.getPoolKey()
      if (poolKey == HostComponent::class.java) {
        _numComponentHostAcquireRequests++
        val poolContent = itemPool.acquire(contentAllocator)
        if (poolContent != null) {
          _numComponentHostsAcquiredFromPool++
        }
        return poolContent
      }

      return null
    }

    override fun release(item: Any): Boolean {
      if (item is ComponentHost) {
        _numComponentHostsReleaseRequests++
        return itemPool.release(item)
      }
      return false
    }

    override fun maybePreallocateContent(
        c: Context,
        contentAllocator: ContentAllocator<*>
    ): Boolean = false
  }

  private class TestComponent(val name: String) : KComponent() {

    override fun ComponentScope.render(): Component {
      return Column(style = Style.wrapInView()) {
        child(Text(name))
        child(
            Row(style = Style.alpha(0.5f)) {
              child(Image(ColorDrawable(Color.RED), style = Style.width(10.dp).height(10.dp)))
            })
      }
    }
  }
}
