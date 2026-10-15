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

package com.facebook.samples.litho

import android.app.Activity
import com.facebook.litho.Component

/** Base interface for all demo items */
interface DemoItem {
  val name: String
}

/** Interface for demo items that can be navigated to */
interface NavigableDemoItem : DemoItem {
  val klass: Class<out Activity>
  val dataProps: HashMap<String, String>?
    get() = null
}

/** Interface for demo items that have child demos */
interface HasChildrenDemos : DemoItem {
  val children: List<DemoItem>
}

/** Interface for demo items that are components */
interface ComponentDemoItem : DemoItem {
  val component: Component
}
