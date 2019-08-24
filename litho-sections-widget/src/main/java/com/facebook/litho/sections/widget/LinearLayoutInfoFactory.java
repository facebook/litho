/*
 * Copyright 2014-present Facebook, Inc.
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

package com.facebook.litho.sections.widget;

import android.content.Context;
import com.facebook.litho.widget.LinearLayoutInfo;

/** A Factory used to create {@link LinearLayoutInfo}s in {@link ListRecyclerConfiguration}. */
public interface LinearLayoutInfoFactory {
  /**
   * @return a new {@link LinearLayoutInfo} that will be used to compute the layouts of the children
   *     of the {@link ListRecyclerConfiguration}.
   */
  LinearLayoutInfo createLinearLayoutInfo(Context context, int orientation, boolean reverseLayout);
}
