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

package com.facebook.litho.widget

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout

@LayoutSpec
object SameManualKeyRootComponentSpec {

  @JvmStatic
  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext): Component =
      Column.create(c)
          .key("col")
          .child(SameManualKeyContainerComponent.create(c).key("key").build())
          .build()

  @JvmStatic
  fun create(c: ComponentContext): SameManualKeyRootComponent =
      SameManualKeyRootComponent.create(c).key("root").build()

  const val globalKeyForStateUpdate: String = "root,col,key,key"
}
