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

import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.core.accessibilityservice.AccessibilityServiceInfoCompat
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.config.LithoDebugConfigurations
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mockStatic
import org.mockito.kotlin.spy
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowAccessibilityManager

@RunWith(LithoTestRunner::class)
class AccessibilityUtilsTest {
  private lateinit var manager: AccessibilityManager
  private lateinit var shadowAccessibilityManager: ShadowAccessibilityManager

  @Before
  fun setup() {
    AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()
    manager =
        ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    shadowAccessibilityManager = Shadow.extract(manager)
    setAccessibilityState(isEnabled = false, touchExploration = false)
  }

  @After
  fun teardown() {
    setAccessibilityState(isEnabled = false, touchExploration = false)
    AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()
    LithoDebugConfigurations.enableFrameworkA11yClientDetection = true
  }

  @Test
  fun isRunningApplicableAccessibilityService_returnsFalse_whenManagerIsNull() {
    assertThat(AccessibilityUtils.isRunningApplicableAccessibilityService(null)).isFalse()
  }

  @Test
  fun isRunningApplicableAccessibilityService_returnsFalse_whenManagerDisabled() {
    setAccessibilityState(isEnabled = false, touchExploration = false)

    assertThat(AccessibilityUtils.isRunningApplicableAccessibilityService(manager)).isFalse()
  }

  @Test
  fun isRunningApplicableAccessibilityService_returnsTrue_whenTouchExplorationEnabled() {
    setAccessibilityState(isEnabled = true, touchExploration = true)

    assertThat(AccessibilityUtils.isRunningApplicableAccessibilityService(manager)).isTrue()
  }

  @Test
  fun isRunningApplicableAccessibilityService_returnsTrue_whenFrameworkClientConnected() {
    setAccessibilityState(isEnabled = true, touchExploration = false)
    val spiedManager = spy(manager)
    doReturn(emptyList<AccessibilityServiceInfo>())
        .`when`(spiedManager)
        .getEnabledAccessibilityServiceList(any())

    assertThat(AccessibilityUtils.isRunningApplicableAccessibilityService(spiedManager)).isTrue()
  }

  @Test
  fun isRunningApplicableAccessibilityService_returnsTrue_whenFrameworkClientDetectionEnabled() {
    setAccessibilityState(isEnabled = true, touchExploration = false)
    val spiedManager = spy(manager)
    doReturn(emptyList<AccessibilityServiceInfo>())
        .`when`(spiedManager)
        .getEnabledAccessibilityServiceList(any())

    LithoDebugConfigurations.enableFrameworkA11yClientDetection = true
    assertThat(AccessibilityUtils.isRunningApplicableAccessibilityService(spiedManager)).isTrue()
  }

  @Test
  fun isRunningApplicableAccessibilityService_returnsFalse_whenFrameworkClientDetectionDisabled() {
    setAccessibilityState(isEnabled = true, touchExploration = false)
    val spiedManager = spy(manager)
    doReturn(emptyList<AccessibilityServiceInfo>())
        .`when`(spiedManager)
        .getEnabledAccessibilityServiceList(any())

    LithoDebugConfigurations.enableFrameworkA11yClientDetection = false
    assertThat(AccessibilityUtils.isRunningApplicableAccessibilityService(spiedManager)).isFalse()
  }

  // Production code calls the deprecated AccessibilityServiceInfoCompat.getCapabilities —
  // we must mock that same method, so the reference here is unavoidable.
  @SuppressLint("DeprecatedMethod")
  @Test
  fun isRunningApplicableAccessibilityService_returnsTrue_whenServiceCanFocusAndRetrieveWindowContent() {
    setAccessibilityState(isEnabled = true, touchExploration = false)
    val serviceInfo =
        AccessibilityServiceInfo().apply {
          eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        }
    val spiedManager = spy(manager)
    doReturn(listOf(serviceInfo)).`when`(spiedManager).getEnabledAccessibilityServiceList(any())

    mockStatic<AccessibilityServiceInfoCompat>().use { mockedCompat ->
      mockedCompat
          .`when`<Int> { AccessibilityServiceInfoCompat.getCapabilities(serviceInfo) }
          .thenReturn(AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT)

      assertThat(AccessibilityUtils.isRunningApplicableAccessibilityService(spiedManager)).isTrue()
    }
  }

  // Same as above: mocking the deprecated method the production code calls.
  @SuppressLint("DeprecatedMethod")
  @Test
  fun isRunningApplicableAccessibilityService_returnsFalse_whenServiceLacksRequiredCapability() {
    setAccessibilityState(isEnabled = true, touchExploration = false)
    val serviceInfo =
        AccessibilityServiceInfo().apply {
          eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        }
    val spiedManager = spy(manager)
    doReturn(listOf(serviceInfo)).`when`(spiedManager).getEnabledAccessibilityServiceList(any())

    mockStatic<AccessibilityServiceInfoCompat>().use { mockedCompat ->
      mockedCompat
          .`when`<Int> { AccessibilityServiceInfoCompat.getCapabilities(serviceInfo) }
          .thenReturn(0)

      assertThat(AccessibilityUtils.isRunningApplicableAccessibilityService(spiedManager)).isFalse()
    }
  }

  @Test
  fun isAccessibilityEnabled_cachesResult() {
    setAccessibilityState(isEnabled = true, touchExploration = true)
    assertThat(AccessibilityUtils.isAccessibilityEnabled(manager)).isTrue()

    setAccessibilityState(isEnabled = false, touchExploration = false)
    assertThat(AccessibilityUtils.isAccessibilityEnabled(manager)).isTrue()
  }

  @Test
  fun invalidateCachedIsAccessibilityEnabled_forcesFreshQuery() {
    setAccessibilityState(isEnabled = true, touchExploration = true)
    assertThat(AccessibilityUtils.isAccessibilityEnabled(manager)).isTrue()

    AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()

    setAccessibilityState(isEnabled = false, touchExploration = false)
    assertThat(AccessibilityUtils.isAccessibilityEnabled(manager)).isFalse()
  }

  private fun setAccessibilityState(isEnabled: Boolean, touchExploration: Boolean) {
    shadowAccessibilityManager.setEnabled(isEnabled)
    shadowAccessibilityManager.setTouchExplorationEnabled(touchExploration)
  }
}
