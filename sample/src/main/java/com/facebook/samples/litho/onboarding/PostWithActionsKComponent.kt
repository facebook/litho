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

package com.facebook.samples.litho.onboarding

import android.graphics.Typeface
import android.widget.ImageView
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.dp
import com.facebook.litho.drawableRes
import com.facebook.litho.flexbox.aspectRatio
import com.facebook.litho.kotlin.widget.Image
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.samples.litho.R
import com.facebook.samples.litho.onboarding.model.Post
import com.facebook.yoga.YogaAlign

class PostWithActionsKComponent(val post: Post) : KComponent() {
  override fun ComponentScope.render(): Component {
    // start_state_hook
    val isLiked = useState { false }
    // end_state_hook

    return Column {
      child(
          Row(alignItems = YogaAlign.CENTER, style = Style.padding(all = 8.dp)) {
            child(
                Image(
                    drawable = drawableRes(post.user.avatarRes),
                    style = Style.width(36.dp).height(36.dp).margin(start = 4.dp, end = 8.dp)))
            child(Text(text = post.user.username, textStyle = Typeface.BOLD))
          })
      child(
          Image(
              drawable = drawableRes(post.imageRes),
              scaleType = ImageView.ScaleType.CENTER_CROP,
              style = Style.aspectRatio(1f)))
      child(
          // start_image_button
          Image(
              drawable =
                  drawableRes(
                      if (isLiked.value) R.drawable.ic_baseline_favorite_24
                      else R.drawable.ic_baseline_favorite_border_24),
              style =
                  Style.width(32.dp).height(32.dp).margin(all = 6.dp).onClick {
                    isLiked.update { isLiked -> !isLiked }
                  })
          // end_image_button
          )
      if (post.text != null) {
        child(
            Row(style = Style.margin(bottom = 4.dp)) {
              child(
                  Text(
                      text = post.user.username,
                      textStyle = Typeface.BOLD,
                      style = Style.margin(horizontal = 4.dp)))
              child(Text(text = post.text, style = Style.margin(horizontal = 4.dp)))
            })
      }
    }
  }
}
