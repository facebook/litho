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
import androidx.annotation.Nullable;

/** The output object for incremental mount extension */
public class IncrementalMountOutput {
  private final int index;
  private final Rect bounds;
  private final long id;
  private final @Nullable IncrementalMountOutput host;

  public IncrementalMountOutput(
      final long id,
      final int index,
      final Rect bounds,
      final @Nullable IncrementalMountOutput host) {
    this.id = id;
    this.index = index;
    this.bounds = bounds;
    this.host = host;
  }

  public int getIndex() {
    return index;
  }

  public Rect getBounds() {
    return bounds;
  }

  public long getId() {
    return id;
  }

  public @Nullable IncrementalMountOutput getHostOutput() {
    return host;
  }
}
