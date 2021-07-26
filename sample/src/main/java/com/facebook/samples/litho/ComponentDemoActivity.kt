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

package com.facebook.samples.litho

import android.os.Bundle
import com.facebook.litho.LithoView
import com.facebook.samples.litho.Demos.SingleDemo
import java.lang.RuntimeException

class ComponentDemoActivity : NavigatableDemoActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val model = dataModel
    if (model !is SingleDemo || (model.componentCreator == null && model.component == null)) {
      throw RuntimeException("Invalid model: $model")
    }
    val lithoView = LithoView(this)
    val componentCreator = model.componentCreator
    if (componentCreator != null) {
      lithoView.setComponent(componentCreator.create(lithoView.componentContext))
    } else {
      lithoView.setComponent(model.component)
    }
    setContentView(lithoView)
  }
}
