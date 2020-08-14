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

import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.annotations.Event;

/** When a page is selected in view pager, this event is fired. */
@Event
@Nullsafe(value = Nullsafe.Mode.LOCAL)
public class PageSelectedEvent {
  public int selectedPageIndex;
}
