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

package com.facebook.samples.litho.onboarding.viewcompat

import android.content.Context
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.MountSpec
import com.facebook.litho.annotations.OnCreateMountContent
import com.facebook.litho.annotations.OnMount
import com.facebook.litho.annotations.Prop

// start_example
@MountSpec
object CircularImageSpec {

  @OnCreateMountContent
  fun onCreateMountContent(androidContext: Context): CustomImageView {
    return CustomImageView(androidContext)
  }

  @OnMount
  fun onMount(context: ComponentContext, image: CustomImageView, @Prop url: String) {
    image.setUrl(url)
  }
}
// end_example
