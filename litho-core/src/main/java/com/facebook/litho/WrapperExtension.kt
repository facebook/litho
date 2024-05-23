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

package com.facebook.litho

/**
 * Builder function for creating [Wrapper] components. It's useful for adding additional [Style]
 * props to a given component.
 */
@Suppress("NOTHING_TO_INLINE", "FunctionName")
inline fun ResourcesScope.Wrapper(style: Style, content: ResourcesScope.() -> Component): Wrapper {
  return Wrapper.create(context).delegate(content()).kotlinStyle(style).build()
}
