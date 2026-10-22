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
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.savedstate.SavedStateRegistryOwner
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

/**
 * Root trees in real Fragments, created with the Fragment or its Activity as host. Screen A is
 * edited, then covered, popped or recreated.
 */
@OptIn(ExperimentalLithoApi::class)
@SuppressLint("KComponentSyncUpdateDetector")
@RunWith(LithoTestRunner::class)
class ComponentTreeSaveableStateFragmentTest {

  private var controller: ActivityController<HostActivity> = launch(null)

  @After
  fun tearDown() {
    controller.pause().stop().destroy()
    STATES.clear()
  }

  @Test
  fun `the Fragment as host restores on Back`() {
    show("A", Host.FRAGMENT)
    value("A").updateSync(9)

    push("B")
    back()

    assertThat(value("A").value).isEqualTo(9)
  }

  @Test
  fun `a popped screen does not come back after process death`() {
    show("root", Host.FRAGMENT)
    push("A", Host.FRAGMENT)
    value("A").updateSync(7)

    back()
    recreateAfterProcessDeath()
    push("A", Host.FRAGMENT)

    assertThat(value("A").value).isEqualTo(0)
  }

  @Test
  fun `a popped screen does not come back in a new Fragment`() {
    show("root", Host.FRAGMENT)
    push("A", Host.FRAGMENT)
    value("A").updateSync(7)

    back()
    push("A", Host.FRAGMENT)

    assertThat(value("A").value).isEqualTo(0)
  }

  @Test
  fun `a screen on the back stack survives process death`() {
    show("A", Host.FRAGMENT)
    value("A").updateSync(7)
    push("B")

    recreateAfterProcessDeath()
    back()

    assertThat(value("A").value).isEqualTo(7)
  }

  @Test
  fun `a screen on the back stack survives repeated process death`() {
    show("A", Host.FRAGMENT)
    value("A").updateSync(7)
    // B saves in the Activity, so each recreation reads and saves the Activity's values again.
    push("B", Host.ACTIVITY)

    repeat(3) { recreateAfterProcessDeath() }
    back()

    assertThat(value("A").value).isEqualTo(7)
  }

  private fun show(name: String, host: Host) {
    activity()
        .supportFragmentManager
        .beginTransaction()
        .replace(android.R.id.content, ScreenFragment.create(name, host))
        .commitNow()
    layout()
  }

  private fun push(name: String, host: Host = Host.FRAGMENT) {
    activity()
        .supportFragmentManager
        .beginTransaction()
        .replace(android.R.id.content, ScreenFragment.create(name, host))
        .addToBackStack(name)
        .commit()
    activity().supportFragmentManager.executePendingTransactions()
    layout()
  }

  private fun back() {
    activity().supportFragmentManager.popBackStackImmediate()
    layout()
  }

  private fun recreateAfterProcessDeath() {
    val savedState = Bundle()
    controller.pause().saveInstanceState(savedState).stop().destroy()
    STATES.clear()
    controller = launch(savedState)
  }

  private fun activity(): HostActivity = controller.get()

  /** Lays out and mounts the visible screen, so its saveable providers register. */
  private fun layout(activity: HostActivity = activity()) {
    val content = activity.findViewById<View>(android.R.id.content)
    val sizeSpec = View.MeasureSpec.makeMeasureSpec(SIZE_PX, View.MeasureSpec.EXACTLY)
    content.measure(sizeSpec, sizeSpec)
    content.layout(0, 0, SIZE_PX, SIZE_PX)
    shadowOf(Looper.getMainLooper()).idle()
  }

  private fun value(name: String): State<Int> = checkNotNull(STATES[name]) { "$name not rendered" }

  private fun launch(savedState: Bundle?): ActivityController<HostActivity> =
      Robolectric.buildActivity(HostActivity::class.java)
          .create(savedState)
          .start()
          .postCreate(savedState)
          .resume()
          .visible()
          .also { shadowOf(Looper.getMainLooper()).idle() }
          .also { layout(it.get()) }

  /** Which object a screen passes to `ComponentTree.create` as its host. */
  enum class Host {
    FRAGMENT,
    ACTIVITY,
  }

  @SuppressLint("BadSuperClassFragmentActivity.FbFragmentActivity")
  class HostActivity : FragmentActivity()

  @SuppressLint("BadSuperClassFragment.FbFragment")
  class ScreenFragment : Fragment() {
    private var tree: ComponentTree? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
      val arguments = requireArguments()
      val name = checkNotNull(arguments.getString(NAME))
      val host: SavedStateRegistryOwner =
          when (Host.valueOf(checkNotNull(arguments.getString(HOST)))) {
            Host.FRAGMENT -> this
            Host.ACTIVITY -> requireActivity()
          }
      val tree =
          ComponentTree.create(requireContext(), host, key(name) { Screen(name) }).build().also {
            tree = it
          }
      return LithoView(requireContext()).apply { componentTree = tree }
    }

    override fun onDestroyView() {
      tree?.release()
      tree = null
      super.onDestroyView()
    }

    companion object {
      fun create(name: String, host: Host): ScreenFragment =
          ScreenFragment().apply { arguments = bundleOf(NAME to name, HOST to host.name) }
    }
  }

  private class Screen(private val name: String) : KComponent() {
    override fun ComponentScope.render(): Component {
      STATES[name] = useSaveableState { 0 }
      return EmptyComponent()
    }
  }

  private companion object {
    const val NAME: String = "name"
    const val HOST: String = "host"
    const val SIZE_PX: Int = 100
    val STATES: MutableMap<String, State<Int>> = mutableMapOf()
  }
}
