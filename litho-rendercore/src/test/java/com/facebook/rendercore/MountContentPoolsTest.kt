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

import android.app.Activity
import android.app.Service
import android.content.Context
import android.view.View
import androidx.lifecycle.LifecycleService
import com.facebook.rendercore.MountContentPools.acquireMountContent
import com.facebook.rendercore.MountContentPools.clear
import com.facebook.rendercore.MountContentPools.onContextDestroyed
import com.facebook.rendercore.MountContentPools.prefillMountContentPool
import com.facebook.rendercore.MountContentPools.release
import com.facebook.rendercore.MountContentPools.setMountContentPoolFactory
import java.lang.Thread
import org.assertj.core.api.Java6Assertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.android.controller.ActivityController
import org.robolectric.android.controller.ServiceController

@RunWith(RobolectricTestRunner::class)
class MountContentPoolsTest {

  private val context: Context = RuntimeEnvironment.getApplication()

  private lateinit var activityController: ActivityController<Activity>

  private lateinit var activity: Activity

  private lateinit var serviceController: ServiceController<LifecycleService>

  private lateinit var service: Service

  @Before
  fun setup() {
    clear()
    setMountContentPoolFactory(null)
    activityController = Robolectric.buildActivity(Activity::class.java).create()
    activity = activityController.get()
    serviceController = Robolectric.buildService(LifecycleService::class.java).create()
    service = serviceController.get()
  }

  @After
  fun cleanup() {
    setMountContentPoolFactory(null)
  }

  @Test
  fun testPrefillMountContentPool() {
    val prefillCount = 4
    val testRenderUnit = TestRenderUnit(/*id*/ 0, /*customPoolSize*/ prefillCount)
    prefillMountContentPool(context, prefillCount, testRenderUnit)
    Java6Assertions.assertThat(testRenderUnit.createdCount).isEqualTo(prefillCount)
    val testRenderUnitToAcquire = TestRenderUnit(0, /*customPoolSize*/ prefillCount)
    for (i in 0 until prefillCount) {
      acquireMountContent(context, testRenderUnitToAcquire)
    }
    Java6Assertions.assertThat(testRenderUnitToAcquire.createdCount).isEqualTo(0)
    acquireMountContent(context, testRenderUnitToAcquire)
    Java6Assertions.assertThat(testRenderUnitToAcquire.createdCount).isEqualTo(1)
  }

  @Test
  fun testCannotReleaseToPoolIfPolicyDoesNotAllow() {
    val prefillCount = 2
    val testRenderUnit =
        TestRenderUnit(id = 0, customPoolSize = prefillCount, policy = PoolingPolicy.AcquireOnly)

    // Assert prefill works
    prefillMountContentPool(context, prefillCount, testRenderUnit)
    Java6Assertions.assertThat(testRenderUnit.createdCount).isEqualTo(2)

    // Assert acquiring works by fetching from pool
    val mountContentList =
        (0 until prefillCount).map { acquireMountContent(context, testRenderUnit) }
    Java6Assertions.assertThat(testRenderUnit.createdCount).isEqualTo(prefillCount)

    // Attempt to release into the pool (should not work)
    for (i in 0 until prefillCount) {
      release(context, testRenderUnit.contentAllocator, mountContentList[i])
    }

    // Attempt to acquire again
    for (i in 0 until prefillCount) {
      acquireMountContent(context, testRenderUnit.contentAllocator)
    }
    // The number of creation should double because we had to create content again
    Java6Assertions.assertThat(testRenderUnit.createdCount).isEqualTo(prefillCount * 2)
  }

  @Test
  fun testPrefillMountContentPoolWithCustomPool() {
    val prefillCount = 4
    val customPoolSize = 2
    val testRenderUnit = TestRenderUnit(0, customPoolSize)
    prefillMountContentPool(context, prefillCount, testRenderUnit)
    // the prefill count overrides the default pool size of the render unit
    Java6Assertions.assertThat(testRenderUnit.createdCount).isEqualTo(prefillCount)
    val testRenderUnitToAcquire = TestRenderUnit(0, 2)
    for (i in 0 until prefillCount) {
      acquireMountContent(context, testRenderUnitToAcquire)
    }
    // expect no new render units to be created as the pool has been prefilled
    Java6Assertions.assertThat(testRenderUnitToAcquire.createdCount).isEqualTo(0)
  }

  @Test
  fun testReleaseMountContentForDestroyedContextDoesNothing() {
    val testRenderUnit = TestRenderUnit(0)
    val content1 = acquireMountContent(activity, testRenderUnit)
    release(activity, testRenderUnit, content1)
    val content2 = acquireMountContent(activity, testRenderUnit)

    // Assert pooling was working before
    Java6Assertions.assertThat(content1).isSameAs(content2)
    release(activity, testRenderUnit, content2)

    // Now destroy the activity and assert pooling no longer works. Next acquire should produce
    // difference content.
    onContextDestroyed(activity)
    val content3 = acquireMountContent(activity, testRenderUnit)
    Java6Assertions.assertThat(content3).isNotSameAs(content1)
  }

  @Test
  fun testDestroyingActivityDoesNotAffectPoolingOfOtherContexts() {
    // Destroy activity context
    activityController.destroy()
    onContextDestroyed(activity)
    val testRenderUnit = TestRenderUnit(0)

    // Create content with different context
    val content1 = acquireMountContent(context, testRenderUnit)
    release(context, testRenderUnit, content1)
    val content2 = acquireMountContent(context, testRenderUnit)

    // Ensure different context is unaffected by destroying activity context.
    Java6Assertions.assertThat(content1).isSameAs(content2)
  }

  @Test
  fun testDestroyingServiceReleasesThePool() {
    val testRenderUnit = TestRenderUnit(0)

    val content1 = acquireMountContent(service, testRenderUnit)
    release(service, testRenderUnit, content1)
    val content2 = acquireMountContent(service, testRenderUnit)

    // Ensure that the content is reused
    Java6Assertions.assertThat(content1).isSameAs(content2)

    // Release the content
    release(service, testRenderUnit, content2)

    // Destroy the service
    serviceController.destroy()

    val content3 = acquireMountContent(service, testRenderUnit)
    // Ensure that the content acquired after destroying the service is different
    Java6Assertions.assertThat(content3).isNotSameAs(content2)
  }

  @Test
  fun testAcquiringContentOnBgThreadAndDestroyingServiceReleasesThePool() {
    val testRenderUnit = TestRenderUnit(0)

    var content1: Any? = null
    val bgThread = Thread { content1 = acquireMountContent(service, testRenderUnit) }
    bgThread.start()
    bgThread.join()
    Robolectric.flushForegroundThreadScheduler()

    release(service, testRenderUnit, checkNotNull(content1))
    val content2 = acquireMountContent(service, testRenderUnit)

    // Ensure that the content is reused
    Java6Assertions.assertThat(content1).isSameAs(content2)

    // Release the content
    release(service, testRenderUnit, content2)

    // Destroy the service
    serviceController.destroy()

    val content3 = acquireMountContent(service, testRenderUnit)
    // Ensure that the content acquired after destroying the service is different
    Java6Assertions.assertThat(content3).isNotSameAs(content2)
  }

  @Test
  fun testAcquireAndReleaseReturnsCorrectContentInstances() {
    val testRenderUnitToAcquire = TestRenderUnit(/*id*/ 0, /*customPoolSize*/ 2)

    // acquire content objects
    val firstContent = acquireMountContent(context, testRenderUnitToAcquire)
    val secondContent = acquireMountContent(context, testRenderUnitToAcquire)

    // both of them should be created and they shouldn't be the same instance
    Java6Assertions.assertThat(testRenderUnitToAcquire.createdCount).isEqualTo(2)
    Java6Assertions.assertThat(firstContent).isNotNull
    Java6Assertions.assertThat(secondContent).isNotSameAs(firstContent)

    // release the second content instance
    release(context, testRenderUnitToAcquire, secondContent)

    // acquire the third content instance
    val thirdContent = acquireMountContent(context, testRenderUnitToAcquire)

    // it should be the same instance that was just released
    Java6Assertions.assertThat(thirdContent).isSameAs(secondContent)
  }

  @Test
  fun testAcquireContentWhenPoolIsSize0ReturnsNewContentEveryTime() {
    val testRenderUnitToAcquire = TestRenderUnit(/*id*/ 0, /*customPoolSize*/ 0) // disable Pooling

    // acquire content objects
    val firstContent = acquireMountContent(context, testRenderUnitToAcquire)
    val secondContent = acquireMountContent(context, testRenderUnitToAcquire)

    // both of them should be created and they shouldn't be the same instance
    Java6Assertions.assertThat(testRenderUnitToAcquire.createdCount).isEqualTo(2)
    Java6Assertions.assertThat(firstContent).isNotNull
    Java6Assertions.assertThat(secondContent).isNotSameAs(firstContent)

    // release the second content instance
    release(context, testRenderUnitToAcquire, secondContent)

    // acquire the third content instance
    val thirdContent = acquireMountContent(context, testRenderUnitToAcquire)

    // it should not be the same as just released instance because pool size is 0
    Java6Assertions.assertThat(thirdContent).isNotSameAs(secondContent)
  }

  @Test
  fun testAcquireContentWhenPoolingIsDisabledReturnsNewContentEveryTime() {
    val testRenderUnitToAcquire =
        TestRenderUnit(id = 0, customPoolSize = 5, policy = PoolingPolicy.Disabled)

    // acquire content objects
    val firstContent = acquireMountContent(context, testRenderUnitToAcquire)
    val secondContent = acquireMountContent(context, testRenderUnitToAcquire)

    // both of them should be created and they shouldn't be the same instance
    Java6Assertions.assertThat(testRenderUnitToAcquire.createdCount).isEqualTo(2)
    Java6Assertions.assertThat(firstContent).isNotNull
    Java6Assertions.assertThat(secondContent).isNotSameAs(firstContent)

    // release the second content instance
    release(context, testRenderUnitToAcquire, secondContent)

    // acquire the third content instance
    val thirdContent = acquireMountContent(context, testRenderUnitToAcquire)

    // it should not be the same as just released instance because pool size is 0
    Java6Assertions.assertThat(thirdContent).isNotSameAs(secondContent)
  }

  @Test
  fun testPolicyIsAcquireOnlyReturnsNewContentEveryTime() {
    val testRenderUnitToAcquire =
        TestRenderUnit(id = 0, customPoolSize = 5, policy = PoolingPolicy.AcquireOnly)

    // acquire content objects
    val firstContent = acquireMountContent(context, testRenderUnitToAcquire)
    val secondContent = acquireMountContent(context, testRenderUnitToAcquire)

    // both of them should be created and they shouldn't be the same instance
    Java6Assertions.assertThat(testRenderUnitToAcquire.createdCount).isEqualTo(2)
    Java6Assertions.assertThat(firstContent).isNotNull
    Java6Assertions.assertThat(secondContent).isNotSameAs(firstContent)

    // release the second content instance
    release(context, testRenderUnitToAcquire, secondContent)

    // acquire the third content instance
    val thirdContent = acquireMountContent(context, testRenderUnitToAcquire)

    // it should not be the same as just released instance because pool size is 0
    Java6Assertions.assertThat(thirdContent).isNotSameAs(secondContent)
  }

  class TestRenderUnit(
      override val id: Long,
      private val customPoolSize: Int = ContentAllocator.DEFAULT_MAX_PREALLOCATION,
      private val policy: PoolingPolicy = PoolingPolicy.Default,
  ) : RenderUnit<View>(RenderType.VIEW), ContentAllocator<View> {

    var createdCount: Int = 0
      private set

    override fun createContent(context: Context): View {
      createdCount++
      return View(context)
    }

    override val contentAllocator: ContentAllocator<View>
      get() = this

    override val poolingPolicy: PoolingPolicy
      get() = policy

    override fun poolSize(): Int = customPoolSize
  }
}
