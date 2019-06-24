/*
 * Copyright 2019-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.codelab

interface LifecycleListener {
  fun onLifecycleMethodCalled(eventType: LifecycleEventType, endTime: Long)
}

class LifecycleEvent(val eventType: LifecycleEventType, val endTime: Long)

enum class LifecycleEventType {
  ON_CREATE_INITIAL_STATE,
  ON_CREATE_TREE_PROP,
  ON_CREATE_CHILDREN,
  ON_DATA_BOUND,
  ON_DATA_RENDERED,
  ON_VIEWPORT_CHANGED,
  ON_REFRESH
}

object DummyTreeProp

data class Zodiac(val animal: String, val month: Int)
