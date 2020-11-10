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

package com.facebook.litho.codelab

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.facebook.litho.ComponentContext
import com.facebook.litho.LithoView
import com.facebook.litho.codelab.auxiliary.TimelineRootComponent

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main_activity)

    val componentContext = ComponentContext(this)
    val lifecycleEvents = mutableListOf<LifecycleEvent>()

    val timelineView = findViewById<LithoView>(R.id.timeline)
    val lifecycleListener =
        object : LifecycleListener {
          override fun onLifecycleMethodCalled(type: LifecycleEventType, endTime: Long) {
            lifecycleEvents.add(LifecycleEvent(type, endTime))

            timelineView.setComponentAsync(
                TimelineRootComponent.create(componentContext)
                    .lifecycleEvents(lifecycleEvents.toList())
                    .build())
          }
        }

    val sectionView = findViewById<LithoView>(R.id.section)
    sectionView.setComponent(
        LifecycleRootComponent.create(componentContext)
            .lifecycleListener(lifecycleListener)
            .build())
  }
}
