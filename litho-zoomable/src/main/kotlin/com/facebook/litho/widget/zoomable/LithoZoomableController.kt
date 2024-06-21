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

package com.facebook.litho.widget.zoomable

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import com.facebook.litho.LithoRenderTreeView
import com.facebook.litho.findComponentActivity
import com.facebook.rendercore.zoomable.ZoomableViewBaseController

class LithoZoomableController(
    context: Context,
    backgroundDrawable: Drawable = ColorDrawable(BLACK_TRANSPARENT_80)
) :
    ZoomableViewBaseController<LithoZoomableView>(
        context = context, backgroundDrawable = backgroundDrawable, isRemoteZoomEnabled = true) {

  override val decorView: ViewGroup =
      checkNotNull(context.findComponentActivity()).window.decorView as ViewGroup

  override fun getRenderTreeView(): LithoRenderTreeView = requireRootView().renderTreeView
}
