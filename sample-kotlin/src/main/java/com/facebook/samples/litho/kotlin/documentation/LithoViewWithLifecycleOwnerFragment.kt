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

package com.facebook.samples.litho.kotlin.documentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentScope
import com.facebook.litho.ComponentTree
import com.facebook.litho.KComponent
import com.facebook.litho.LithoView
import com.facebook.litho.kotlin.widget.Text

// start_example
class LithoViewWithLifecycleOwnerFragment : Fragment() {

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View {
    val componentContext = ComponentContext(requireContext())

    // Create a ComponentTree with the lifecycle owner tree prop set
    val componentTree =
        ComponentTree.create(componentContext, LifecycleOwnerTreePropSampleComponent())
            .withLifecycleOwnerTreeProp(this)
            .enableChildClipping(true)
            .build()

    // Create a LithoView with the ComponentTree
    val lithoView = LithoView(componentContext)
    lithoView.componentTree = componentTree

    return lithoView
  }
}

// end_example

private class LifecycleOwnerTreePropSampleComponent : KComponent() {
  override fun ComponentScope.render(): Component? {
    return Text("Hello")
  }
}
