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

package com.facebook.rendercore.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.facebook.rendercore.RenderState
import com.facebook.rendercore.RenderTree
import com.facebook.rendercore.ResolveResult
import com.facebook.rendercore.RootHostView
import com.facebook.rendercore.StateUpdateReceiver
import com.facebook.rendercore.sample.data.LayoutRepository
import kotlinx.coroutines.launch

class SampleData

class SampleActivity : ComponentActivity(), RenderState.Delegate<SampleData?> {

  private val renderState: RenderState<SampleData?, Any?, StateUpdateReceiver.StateUpdate<Any?>> =
      RenderState(this, this, null, null)

  private var currentRenderTree: RenderTree? = null

  init {
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {

        // An API call to get the layout from remote sever
        // This is the resolve step
        val root = LayoutRepository.getLayout()

        // Request a new render
        // This is the layout and reduction step
        // If the RenderState is set on a RootHostView it will mount a new RenderTree
        renderState.setTree { _, _, _, _ -> ResolveResult(root, null, null) }
      }
    }
  }

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val host = RootHostView(this).apply { setRenderState(renderState) }
    setContentView(host)
  }

  override fun commit(
      layoutVersion: Int,
      current: RenderTree?,
      next: RenderTree,
      currentState: SampleData?,
      nextState: SampleData?
  ) {
    currentRenderTree = next
  }

  override fun commitToUI(tree: RenderTree?, state: SampleData?) {
    currentRenderTree = tree
  }
}
