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

package com.facebook.samples.litho.kotlin

import android.app.Application
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.litho.ComponentsReporter
import com.facebook.samples.litho.kotlin.logging.SampleComponentsReporter
import com.facebook.soloader.SoLoader

class LithoApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    Fresco.initialize(this)
    SoLoader.init(this, false)
    ComponentsReporter.provide(SampleComponentsReporter())
  }
}
