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

package com.facebook.samples.litho.kotlin.demo

import com.facebook.samples.litho.kotlin.animations.animatedbadge.AnimatedBadgeActivity
import com.facebook.samples.litho.kotlin.animations.animationcomposition.ComposedAnimationsActivity
import com.facebook.samples.litho.kotlin.animations.bounds.BoundsAnimationActivity
import com.facebook.samples.litho.kotlin.animations.expandableelement.ExpandableElementActivity
import com.facebook.samples.litho.kotlin.bordereffects.BorderEffectsActivity
import com.facebook.samples.litho.kotlin.errors.ErrorHandlingActivity
import com.facebook.samples.litho.kotlin.lithography.LithographyActivity
import com.facebook.samples.litho.kotlin.logging.LoggingActivity
import com.facebook.samples.litho.kotlin.playground.PlaygroundActivity

object DataModels {

  val DATA_MODELS = listOf(
      DemoListDataModel(
          name = "Lithography",
          klass = LithographyActivity::class.java
      ),
      DemoListDataModel(
          name = "Playground",
          klass = PlaygroundActivity::class.java
      ),
      DemoListDataModel(
          name = "Border effects",
          klass = BorderEffectsActivity::class.java
      ),
      DemoListDataModel(
          name = "Error boundaries",
          klass = ErrorHandlingActivity::class.java
      ),
      DemoListDataModel(
          name = "Animations",
          datamodels = listOf(
              DemoListDataModel(
                  name = "Animations Composition",
                  klass = ComposedAnimationsActivity::class.java
              ),
              DemoListDataModel(
                  name = "Expandable Element",
                  klass = ExpandableElementActivity::class.java
              ),
              DemoListDataModel(
                  name = "Animated Badge",
                  klass = AnimatedBadgeActivity::class.java
              ),
              DemoListDataModel(
                  name = "Bounds Animation",
                  klass = BoundsAnimationActivity::class.java)
          )
      ),
      DemoListDataModel(
              name = "Logging",
              klass = LoggingActivity::class.java
      )
  )

  fun getDataModels(indices: IntArray?): List<DemoListDataModel> {
    if (indices == null) {
      return DATA_MODELS
    }

    var dataModels: List<DemoListDataModel> = DATA_MODELS

    indices.forEach {
      dataModels = dataModels[it].datamodels ?: dataModels
    }

    return dataModels
  }
}
