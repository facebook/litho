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

import android.app.Activity
import android.os.Bundle
import android.util.Pair
import com.facebook.rendercore.DefaultNode
import com.facebook.rendercore.DefaultTextNode
import com.facebook.rendercore.RenderState
import com.facebook.rendercore.RenderTree
import com.facebook.rendercore.RootHostView
import com.facebook.rendercore.YogaProps
import com.facebook.rendercore.text.TextRenderUnit
import com.facebook.rendercore.text.TextStyle

class SampleActivity : Activity(), RenderState.Delegate<SurfaceData?> {

  var rootHostView: RootHostView? = null

  var mCurrentState: SurfaceData? = null
  var mCurrentRenderTree: RenderTree? = null

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val renderState: RenderState<SurfaceData?, Any?> =
        RenderState<SurfaceData?, Any?>(
            this,
            this,
            null,
            null,
        )

    rootHostView =
        RootHostView(this).apply {
          setRenderState(renderState)
          setContentView(this)
        }

    renderState.setTree { _, _, _, _ ->
      val textRenderUnit = TextRenderUnit(1)
      val textStyle = TextStyle()
      textStyle.setTextSize(48)
      val root: DefaultNode =
          DefaultTextNode(YogaProps(), "Hello World!", textRenderUnit, textStyle)

      Pair(root, null)
    }
  }

  override fun commit(
      layoutVersion: Int,
      current: RenderTree?,
      next: RenderTree?,
      currentState: SurfaceData?,
      nextState: SurfaceData?
  ) {
    mCurrentRenderTree = next
    mCurrentState = nextState
  }

  override fun commitToUI(tree: RenderTree?, state: SurfaceData?) {}
}

class SurfaceData(val text: String)
