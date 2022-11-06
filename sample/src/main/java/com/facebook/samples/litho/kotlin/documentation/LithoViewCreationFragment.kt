// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.samples.litho.kotlin.documentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.facebook.litho.AOSPLithoLifecycleProvider
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.LithoView
import com.facebook.litho.kotlin.widget.Text

// start_example
class LithoViewCreationFragment : Fragment() {

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    return LithoView.create(requireContext(), SampleComponent(), AOSPLithoLifecycleProvider(this))
  }
}
// end_example
private class SampleComponent : KComponent() {
  override fun ComponentScope.render(): Component? {
    return Text("Hello")
  }
}
