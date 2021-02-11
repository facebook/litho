/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.samples.litho.kotlin.lithography.components

import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.litho.DslScope
import com.facebook.litho.KComponent
import com.facebook.litho.fresco.FrescoImage

class SingleImageComponent(val imageUri: String, val imageAspectRatio: Float = 1f) : KComponent() {
  override fun DslScope.render() =
      FrescoImage(
          controller = Fresco.newDraweeControllerBuilder().setUri(imageUri).build(),
          imageAspectRatio = imageAspectRatio)
}
