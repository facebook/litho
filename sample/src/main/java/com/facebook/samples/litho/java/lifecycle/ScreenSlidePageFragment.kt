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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.LithoLifecycleProvider
import com.facebook.litho.LithoLifecycleProviderDelegate
import com.facebook.litho.LithoView
import com.facebook.litho.widget.Text
import com.facebook.samples.litho.R
import com.facebook.samples.litho.java.lifecycle.ConsoleView.LogRunnable
import com.facebook.yoga.YogaAlign
import java.util.concurrent.atomic.AtomicInteger

class ScreenSlidePageFragment : Fragment(R.layout.screen_slide_fragment) {

  private var lithoView: LithoView? = null
  private var consoleView: ConsoleView? = null
  private var wasVisible = false
  private var position = 0
  private val consoleDelegateListener = ConsoleDelegateListener()
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

        override fun setRootComponent(isSync: Boolean) = Unit
      }

  // start_example_lifecycleprovider
  private val delegate = LithoLifecycleProviderDelegate()

  override fun setUserVisibleHint(isVisibleToUser: Boolean) {
    super.setUserVisibleHint(isVisibleToUser)
    if (wasVisible == isVisibleToUser) {
      return
    }
    if (isVisibleToUser) {
      wasVisible = true
      delegate.moveToLifecycle(LithoLifecycleProvider.LithoLifecycle.HINT_VISIBLE)
    } else {
      wasVisible = false
      delegate.moveToLifecycle(LithoLifecycleProvider.LithoLifecycle.HINT_INVISIBLE)
    }
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    val parent = inflater.inflate(R.layout.screen_slide_fragment, container, false) as ViewGroup
    val c = ComponentContext(requireContext())
    lithoView =
        LithoView.create(
            c,
            getComponent(c),
            delegate /* The LithoLifecycleProvider delegate for this LithoView */)

    // end_example_lifecycleprovider
    val params1 = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
    params1.weight = 1f
    lithoView?.layoutParams = params1
    parent.addView(lithoView)
    consoleView = ConsoleView(requireContext())
    val params2 = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
    params2.weight = 1f
    consoleView?.layoutParams = params2
    parent.addView(consoleView)
    return parent
  }

  private fun getComponent(c: ComponentContext): Component =
      Column.create(c)
          .child(Text.create(c).text(position.toString()).alignSelf(YogaAlign.CENTER))
          .child(
              LifecycleDelegateComponent.create(c)
                  .id(atomicId.getAndIncrement().toString())
                  .delegateListener(delegateListener)
                  .consoleDelegateListener(consoleDelegateListener)
                  .build())
          .build()

  private fun setPosition(position: Int) {
    this.position = position
  }

  companion object {
    private val atomicId = AtomicInteger(0)

    @JvmStatic
    fun newInstance(position: Int): ScreenSlidePageFragment {
      val fragment = ScreenSlidePageFragment()
      val args = Bundle()
      args.putInt("position", position)
      fragment.arguments = args
      fragment.setPosition(position)
      return fragment
    }
  }
}
