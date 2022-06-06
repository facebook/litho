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

package com.facebook.samples.litho.java.lifecycle

import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import com.facebook.litho.AOSPLithoLifecycleProvider
import com.facebook.litho.ComponentContext
import com.facebook.litho.LithoView
import com.facebook.samples.litho.NavigatableDemoActivity
import com.facebook.samples.litho.java.lifecycle.ConsoleView.LogRunnable
import java.util.Random
import java.util.concurrent.atomic.AtomicInteger

class LifecycleDelegateActivity : NavigatableDemoActivity() {

  private var lithoView: LithoView? = null
  private var consoleView: ConsoleView? = null
  private val consoleDelegateListener: ConsoleDelegateListener =
      object : ConsoleDelegateListener() {}
  private val delegateListener: DelegateListener =
      object : DelegateListener {
        override fun onDelegateMethodCalled(
            type: Int,
            thread: Thread,
            timestamp: Long,
            id: String
        ) {
          consoleView?.post(
              LogRunnable(
                  consoleView,
                  LifecycleDelegateLog.prefix(thread, timestamp, id),
                  LifecycleDelegateLog.log(type)))
        }

        override fun setRootComponent(isSync: Boolean) {
          if (lithoView != null) {
            val random = Random()
            val root =
                LifecycleDelegateComponent.create(ComponentContext(this@LifecycleDelegateActivity))
                    .id(atomicId.getAndIncrement().toString())
                    .key(random.nextInt().toString()) // Force to reset component.
                    .delegateListener(this)
                    .consoleDelegateListener(consoleDelegateListener)
                    .build()
            if (isSync) {
              lithoView?.setComponent(root)
            } else {
              lithoView?.setComponentAsync(root)
            }
          }
        }
      }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val parent = LinearLayout(this)
    parent.orientation = LinearLayout.VERTICAL
    parent.layoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

    // start_example_lifecycleprovider
    val lifecycleProvider = AOSPLithoLifecycleProvider(this)
    val componentContext = ComponentContext(this)
    lithoView =
        LithoView.create(
            this,
            LifecycleDelegateComponent.create(componentContext)
                .id(atomicId.getAndIncrement().toString())
                .delegateListener(delegateListener)
                .consoleDelegateListener(consoleDelegateListener)
                .build(),
            lifecycleProvider /* The LithoLifecycleProvider for this LithoView */)
    // end_example_lifecycleprovider
    val params1 =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    params1.weight = 1f
    lithoView?.layoutParams = params1
    parent.addView(lithoView)
    consoleView = ConsoleView(this)
    val params2 =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    params2.weight = 1f
    consoleView?.layoutParams = params2
    parent.addView(consoleView)
    setContentView(parent)
  }

  override fun onDestroy() {
    super.onDestroy()
    lithoView?.release()
  }

  companion object {
    private val atomicId = AtomicInteger(0)
  }
}
