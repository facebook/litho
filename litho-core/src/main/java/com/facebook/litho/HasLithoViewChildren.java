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

package com.facebook.litho;

import java.util.List;

/**
 * An interface that a mountable view can extend which informs that this mountable content has other
 * LithoView children. This is used to make sure to unmount this view's children when unmounting
 * this view itself.
 */
public interface HasLithoViewChildren {
  void obtainLithoViewChildren(List<LithoView> lithoViews);
}
