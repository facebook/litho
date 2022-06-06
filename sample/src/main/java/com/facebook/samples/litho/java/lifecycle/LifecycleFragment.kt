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
import android.widget.Button
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.LithoLifecycleProvider
import com.facebook.litho.LithoLifecycleProviderDelegate
import com.facebook.litho.LithoView
import com.facebook.samples.litho.R
import java.util.concurrent.atomic.AtomicInteger

class LifecycleFragment : Fragment(), View.OnClickListener {

  private var lithoView: LithoView? = null
  private var consoleView: ConsoleView? = null
  private val consoleDelegateListener = ConsoleDelegateListener()
  private val delegateListener: DelegateListener =
      object : DelegateListener {
        override fun onDelegateMethodCalled(
            type: Int,
            thread: Thread,
            timestamp: Long,
            id: String
        ) {
          val prefix = LifecycleDelegateLog.prefix(thread, timestamp, id)
          val logRunnable =
              ConsoleView.LogRunnable(consoleView, prefix, LifecycleDelegateLog.log(type))
          consoleView?.post(logRunnable)
        }

        override fun setRootComponent(isSync: Boolean) = Unit
      }

  // start_example_lifecycleprovider
  private val delegate: LithoLifecycleProviderDelegate = LithoLifecycleProviderDelegate()

  override fun onClick(view: View) {

    // Replaces the current fragment with a new fragment
    replaceFragment()

    // inform the LithoView
    delegate.moveToLifecycle(LithoLifecycleProvider.LithoLifecycle.HINT_VISIBLE)
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    val parent =
        inflater.inflate(R.layout.activity_fragment_transactions_lifecycle, container, false)
            as ViewGroup
    val c = ComponentContext(requireContext())
    lithoView =
        LithoView.create(
            c,
            getComponent(c),
            delegate /* The LithoLifecycleProvider delegate for this LithoView */)

    // end_example_lifecycleprovider
    val layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
    layoutParams.weight = 1f
    lithoView?.layoutParams = layoutParams
    consoleView = ConsoleView(requireContext())
    consoleView?.layoutParams = layoutParams
    parent.addView(consoleView)
    return parent
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    val fragmentButton = view.findViewById<Button>(R.id.new_fragment_button)
    fragmentButton.text = "New Fragment"
    fragmentButton.setOnClickListener(this)
    val fragmentLithoView = view.findViewById<ViewGroup>(R.id.fragment_litho_view)
    fragmentLithoView.addView(lithoView)
  }

  private fun getComponent(c: ComponentContext): Component =
      Column.create(c)
          .child(
              LifecycleDelegateComponent.create(c)
                  .id(atomicId.getAndIncrement().toString())
                  .delegateListener(delegateListener)
                  .consoleDelegateListener(consoleDelegateListener)
                  .build())
          .build()

  private fun replaceFragment() {
    parentFragmentManager
        .beginTransaction()
        .replace(R.id.fragment_view, LifecycleFragment(), null)
        .addToBackStack(null)
        .commit()
  }

  companion object {
    private val atomicId = AtomicInteger(0)
  }
}
