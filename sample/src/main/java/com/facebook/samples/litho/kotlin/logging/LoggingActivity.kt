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

package com.facebook.samples.litho.kotlin.logging

import android.os.Bundle
import android.util.Log
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentTree
import com.facebook.litho.ComponentTreeDebugEventListener
import com.facebook.litho.LithoView
import com.facebook.litho.debug.LithoDebugEvent.LayoutCommitted
import com.facebook.litho.debug.LithoDebugEvent.StateUpdateEnqueued
import com.facebook.rendercore.debug.DebugEvent
import com.facebook.rendercore.debug.DebugEvent.Companion.MountItemMount
import com.facebook.samples.litho.NavigatableDemoActivity

class LoggingActivity : NavigatableDemoActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val c = ComponentContext(this, "LITHOSAMPLE", SampleComponentsLogger())
    val lithoView =
        LithoView.create(
            c,
            ComponentTree.create(c, LoggingRootComponent())
                .withComponentTreeDebugEventListener(
                    object : ComponentTreeDebugEventListener {
                      override fun onEvent(debugEvent: DebugEvent) {
                        Log.d("litho-events", debugEvent.toString())
                      }

                      override val events: Set<String> =
                          setOf(MountItemMount, StateUpdateEnqueued, LayoutCommitted)
                    })
                .build())

    setContentView(lithoView)
  }
}
