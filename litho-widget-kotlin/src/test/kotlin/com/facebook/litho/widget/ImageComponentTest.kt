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

package com.facebook.litho.kotlin.widget

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.litho.MatrixDrawable
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.px
import com.facebook.litho.testing.LithoViewRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/** Tests for [ImageComponent] */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(AndroidJUnit4::class)
class ImageComponentTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  @Test
  fun `ImageComponent should render`() {
    lithoViewRule
        .render {
          ImageComponent(
              drawable = ColorDrawable(Color.RED),
              style = Style.width(100.px).height(100.px),
          )
        }
        .apply {

          // should find an ImageComponent in the tree
          findComponent(ImageComponent::class)

          // should mount an ImageComponent
          assertThat(lithoView.mountItemCount).isEqualTo(1)

          // content of ImageComponent should be a MatrixDrawable
          val content = lithoView.getMountItemAt(0).content as MatrixDrawable<*>
          assertThat(content.bounds.width()).isEqualTo(100)
          assertThat(content.bounds.height()).isEqualTo(100)

          // Matrix drawable should host a ColorDrawable
          val drawable = content.mountedDrawable
          assertThat(drawable).isInstanceOf(ColorDrawable::class.java)
        }
  }
}
