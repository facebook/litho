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

@file:JvmName("ActivityTreeProp")

package com.facebook.litho.lifecycle

import android.app.Activity
import com.facebook.litho.TreeProp
import com.facebook.litho.treePropOf

/**
 * Optional reference to the hosting [Activity] for a Litho subtree. Default `null`. Hosts that
 * mount Litho content inside an Activity should wrap their root component with
 * `TreePropProvider(ActivityTreeProp to requireActivity())` so leaves can resolve Activity-scoped
 * resources (e.g. `Window` for FrameMetrics) without `androidContext as? Activity` introspection.
 */
@JvmField val ActivityTreeProp: TreeProp<Activity?> = treePropOf { null }
