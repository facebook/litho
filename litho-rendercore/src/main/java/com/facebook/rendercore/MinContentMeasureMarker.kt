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

package com.facebook.rendercore

/**
 * Marker object set on [LayoutContext.setExtraContextForCurrentNode] to signal that the current
 * measurement is a min-content query from the flex layout algorithm.
 *
 * Used by:
 * - Nested flexbox layout functions to override errata and propagate auto minimums.
 * - Primitive components (via [LayoutScope.extraContext]) to detect min-content mode and return
 *   their intrinsic minimum size instead of filling the constraint.
 */
object MinContentMeasureMarker
