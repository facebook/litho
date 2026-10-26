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

package com.facebook.samples.litho.playground

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.padding
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.surface.lithoHost
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.rendercore.dp
import com.facebook.rendercore.sp
import com.facebook.samples.litho.NavigatableDemoActivity

/** Shows [HostPlaygroundFragment]. */
@SuppressLint("BadSuperClassFragmentActivity.FbFragmentActivity", "EndpointWithoutSwitchOff")
class HostPlaygroundActivity : NavigatableDemoActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState == null) {
      supportFragmentManager
          .beginTransaction()
          .replace(android.R.id.content, HostPlaygroundFragment())
          .commit()
    }
  }
}

/** A Fragment whose root component is hosted with a [com.facebook.litho.surface.LithoHost]. */
@OptIn(ExperimentalLithoApi::class)
@SuppressLint("BadSuperClassFragment.FbFragment")
class HostPlaygroundFragment : Fragment() {
  private val host by lithoHost()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    host.setContent { HostPlaygroundComponent(onOpenDialog = ::openDialog) }
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View = host.getLithoView()

  private fun openDialog() {
    HostPlaygroundDialogFragment().show(childFragmentManager, DIALOG_TAG)
  }

  private companion object {
    const val DIALOG_TAG = "host_playground_dialog"
  }
}

/**
 * A dialog whose tree is built from content set in `onCreate`, before the dialog exists, and shown
 * from `onCreateDialog`.
 */
@OptIn(ExperimentalLithoApi::class)
@SuppressLint("BadSuperClassDialogFragment.FbDialogFragment")
class HostPlaygroundDialogFragment : DialogFragment() {
  private val host by lithoHost()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    host.setContent { HostPlaygroundDialogComponent(onClose = ::dismiss) }
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
      Dialog(requireContext()).apply { setContentView(host.getLithoView()) }
}

private class HostPlaygroundComponent(private val onOpenDialog: () -> Unit) : KComponent() {
  override fun ComponentScope.render(): Component {
    val taps = useState { 0 }
    return Column(style = Style.padding(all = 16.dp)) {
      child(Text(text = "Hosted by LithoHost", textSize = 20.sp))
      child(
          Text(
              text = "Tapped ${taps.value} times",
              textSize = 16.sp,
              style = Style.padding(vertical = 16.dp).onClick { taps.update { it + 1 } },
          ),
      )
      child(
          Text(
              text = "Open a dialog hosted by LithoHost",
              textSize = 16.sp,
              style = Style.padding(vertical = 16.dp).onClick { onOpenDialog() },
          ),
      )
    }
  }
}

private class HostPlaygroundDialogComponent(private val onClose: () -> Unit) : KComponent() {
  override fun ComponentScope.render(): Component {
    val taps = useState { 0 }
    return Column(style = Style.padding(all = 24.dp)) {
      child(Text(text = "Dialog hosted by LithoHost", textSize = 20.sp))
      child(
          Text(
              text = "Tapped ${taps.value} times",
              textSize = 16.sp,
              style = Style.padding(vertical = 16.dp).onClick { taps.update { it + 1 } },
          ),
      )
      child(
          Text(
              text = "Close",
              textSize = 16.sp,
              style = Style.padding(vertical = 16.dp).onClick { onClose() },
          ),
      )
    }
  }
}
