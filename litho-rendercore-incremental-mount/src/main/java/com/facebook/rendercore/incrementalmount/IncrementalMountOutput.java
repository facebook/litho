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

package com.facebook.rendercore.incrementalmount;

import android.graphics.Rect;

/** The output object for incremental mount extension */
public class IncrementalMountOutput {
  public final int index;
  public final Rect bounds;

  public IncrementalMountOutput(final int index, final Rect bounds) {
    this.index = index;
    this.bounds = bounds;
  }

  public int getIndex() {
    return index;
  }

  public Rect getBounds() {
    return bounds;
  }
}
