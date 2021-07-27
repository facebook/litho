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

package com.facebook.litho.sections.widget;

import androidx.annotation.Px;
import com.facebook.litho.Handle;
import com.facebook.litho.annotations.Event;
import com.facebook.litho.widget.SmoothScrollAlignmentType;

/**
 * An event that is triggered when a smooth scroll action to a section with a given Handle is
 * requested.
 */
@Event
public class SmoothScrollToHandleEvent {
  public Handle target;
  public @Px int offset;
  public SmoothScrollAlignmentType type;
}
