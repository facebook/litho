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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.ComponentAccessibilityDelegateTestHelper.RecordingFrameLayout
import com.facebook.litho.annotations.EventHandlerRebindMode
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.Text as LegacyText
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowAccessibilityManager

/** Regression tests for [runCatchingPrefetcherRace] used by [ComponentAccessibilityDelegate]. */
@RunWith(LithoTestRunner::class)
class ComponentAccessibilityDelegateTest {

  @get:Rule val lithoTestRule = LithoTestRule(attachToRealWindow = true)

  private lateinit var shadowManager: ShadowAccessibilityManager

  @Before
  fun setUp() {
    val manager =
        ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    shadowManager = Shadows.shadowOf(manager)
    shadowManager.setEnabled(true)
    shadowManager.setTouchExplorationEnabled(true)
    AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()
  }

  @After
  fun tearDown() {
    shadowManager.setEnabled(false)
    shadowManager.setTouchExplorationEnabled(false)
    AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()
  }

  @Test
  fun `installed delegate rejects a null accessibility event`() {
    val (host, child) = renderAccessibilityHost()

    assertThat(ViewCompat.hasAccessibilityDelegate(host)).isTrue()
    assertThat(host.requestSendAccessibilityEvent(child, null)).isFalse()
  }

  @Test
  fun `null accessibility event does not propagate above the host`() {
    val testLithoView = renderAccessibilityTree()
    val host = testLithoView.lithoView.getChildAt(0) as ComponentHost
    val child = View(lithoTestRule.context.androidContext)
    val originalParent = testLithoView.lithoView.parent as ViewGroup
    val recordingParent = RecordingFrameLayout(lithoTestRule.context.androidContext)
    originalParent.removeView(testLithoView.lithoView)
    recordingParent.addView(testLithoView.lithoView)
    originalParent.addView(recordingParent)

    assertThat(host.requestSendAccessibilityEvent(child, null)).isFalse()
    assertThat(recordingParent.nullEventCount).isZero()
  }

  // AccessibilityEvent's public constructor is API 30+; obtain() and recycle() keep SDK 27 covered.
  @SuppressLint("DeprecatedMethod")
  @Test
  fun `non-null accessibility event is sent`() {
    val (host, child) = renderAccessibilityHost()
    val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT)

    try {
      assertThat(host.requestSendAccessibilityEvent(child, event)).isTrue()
    } finally {
      event.recycle()
    }
  }

  // Same as above: obtain() and recycle() keep SDK 27 covered.
  @SuppressLint("DeprecatedMethod")
  @Test
  fun `handler can call the real super implementation without recursion`() {
    var handlerCalls = 0
    val handler =
        object :
            EventHandler<OnRequestSendAccessibilityEventEvent>(
                1,
                EventHandlerRebindMode.REBIND,
                EventDispatchInfo(null, null),
                null,
            ) {
          override fun dispatchEvent(event: OnRequestSendAccessibilityEventEvent): Any? {
            handlerCalls++
            return event.superDelegate.onRequestSendAccessibilityEvent(
                event.host,
                event.child,
                event.event,
            )
          }
        }
    val (host, child) = renderAccessibilityHost(handler)
    val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT)
    val handlerCallsAfterMount = handlerCalls

    try {
      assertThat(host.requestSendAccessibilityEvent(child, event)).isTrue()
      assertThat(handlerCalls).isEqualTo(handlerCallsAfterMount + 1)
      assertThat(host.requestSendAccessibilityEvent(child, null)).isFalse()
      assertThat(handlerCalls).isEqualTo(handlerCallsAfterMount + 1)
    } finally {
      event.recycle()
    }
  }

  @Test
  fun `direct delegate call rejects a null accessibility event`() {
    val parent = FrameLayout(lithoTestRule.context.androidContext)
    val child = View(lithoTestRule.context.androidContext)
    parent.addView(child)
    val delegate = ComponentAccessibilityDelegate(
        child,
        null,
        View.FOCUSABLE_AUTO,
        ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO,
    )

    assertThat(delegate.onRequestSendAccessibilityEvent(parent, child, null)).isFalse()
  }

  @Test
  fun `invalid virtual node fallback is disabled by default`() {
    val previousValue = ComponentsConfiguration.enableInvalidAccessibilityVirtualNodeFix
    val node = AccessibilityNodeInfoCompat.obtain()

    try {
      ComponentsConfiguration.enableInvalidAccessibilityVirtualNodeFix = false
      populateInvalidVirtualViewNodeIfEnabled(node)

      val bounds = Rect()
      node.getBoundsInParent(bounds)
      assertThat(node.contentDescription).isNull()
      assertThat(bounds).isEqualTo(Rect())
    } finally {
      ComponentsConfiguration.enableInvalidAccessibilityVirtualNodeFix = previousValue
    }
  }

  @Test
  fun `invalid virtual node fallback satisfies ExploreByTouchHelper contract when enabled`() {
    val previousValue = ComponentsConfiguration.enableInvalidAccessibilityVirtualNodeFix
    val node = AccessibilityNodeInfoCompat.obtain()

    try {
      ComponentsConfiguration.enableInvalidAccessibilityVirtualNodeFix = true
      populateInvalidVirtualViewNodeIfEnabled(node)

      val bounds = Rect()
      node.getBoundsInParent(bounds)
      assertThat(node.contentDescription).isEqualTo("")
      assertThat(bounds).isEqualTo(Rect(0, 0, 1, 1))
    } finally {
      ComponentsConfiguration.enableInvalidAccessibilityVirtualNodeFix = previousValue
    }
  }

  @Test
  fun `runCatchingPrefetcherRace runs the block on normal call`() {
    // The helper that wraps super.onInitializeAccessibilityNodeInfo in a try-catch for IOOBE
    // must invoke the block on the normal (non-throwing) path so accessibility properties are
    // populated as usual.
    var ran = false
    runCatchingPrefetcherRace { ran = true }
    assertThat(ran).isTrue()
  }

  @Test
  fun `runCatchingPrefetcherRace swallows IndexOutOfBoundsException`() {
    // Regression guard for the AOSP race in ViewGroup.onInitializeAccessibilityNodeInfoInternal:
    // mAttachInfo.mTempArrayList is shared per-window and can be mutated mid-iteration by
    // concurrent accessibility traversal, causing super.onInitializeAccessibilityNodeInfo to
    // throw IndexOutOfBoundsException. The helper must swallow that specific exception so
    // ComponentAccessibilityDelegate can continue populating Litho-specific node info.
    var blockEntered = false
    runCatchingPrefetcherRace {
      blockEntered = true
      throw IndexOutOfBoundsException("simulated AOSP mTempArrayList race")
    }
    // Reaching this line proves the IOOBE was swallowed; the flag proves the block ran.
    assertThat(blockEntered).isTrue()
  }

  @Test
  fun `runCatchingPrefetcherRace swallows ArrayIndexOutOfBoundsException`() {
    // ArrayIndexOutOfBoundsException is a subclass of IndexOutOfBoundsException and is thrown by
    // the same race path on some devices/array-backed callsites. Both must be handled.
    var blockEntered = false
    runCatchingPrefetcherRace {
      blockEntered = true
      throw ArrayIndexOutOfBoundsException("simulated array-backed race")
    }
    assertThat(blockEntered).isTrue()
  }

  @Test
  fun `runCatchingPrefetcherRace propagates non-IndexOutOfBoundsException`() {
    // Negative companion: only IndexOutOfBoundsException is the platform race we know about.
    // Other RuntimeExceptions (NPE, etc.) signal real bugs and must continue to propagate so
    // they remain loggable upstream.
    assertThatThrownBy {
          runCatchingPrefetcherRace {
            throw NullPointerException("unrelated bug, should propagate")
          }
        }
        .isInstanceOf(NullPointerException::class.java)
  }

  @Test
  fun `runCatchingPrefetcherRace propagates IllegalStateException`() {
    // Any non-IOOBE RuntimeException from the super call must propagate.
    assertThatThrownBy {
          runCatchingPrefetcherRace {
            throw IllegalStateException("unrelated bug, should propagate")
          }
        }
        .isInstanceOf(IllegalStateException::class.java)
  }

  // The Spec Row builder is the only way to attach a Boolean-returning send handler until
  // Style.onRequestSendAccessibilityEventWithResult lands.
  @Suppress("DEPRECATION")
  @SuppressLint("DeprecatedClass")
  private fun renderAccessibilityTree(
      handler: EventHandler<OnRequestSendAccessibilityEventEvent>? = null,
  ) = lithoTestRule.render {
    if (handler == null) {
      object : KComponent() {
        override fun ComponentScope.render(): Component = Row { child(Text(text = "child")) }
      }
    } else {
      Row.create(context)
          .onRequestSendAccessibilityEventHandler(handler)
          .child(LegacyText.create(context).text("child"))
          .build()
    }
  }

  private fun renderAccessibilityHost(
      handler: EventHandler<OnRequestSendAccessibilityEventEvent>? = null,
  ): Pair<ComponentHost, View> {
    val testLithoView = renderAccessibilityTree(handler)
    val host = testLithoView.lithoView.getChildAt(0) as ComponentHost
    return host to View(lithoTestRule.context.androidContext)
  }
}
