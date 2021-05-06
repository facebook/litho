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

/**
 * An event that is triggered when a scroll action is requested/performed.
 *
 * <pre>
 * {@code
 *
 * @OnEvent(ScrollEvent.class)
 * static void onScroll(
 *     ComponentContext c,
 *     @FromEvent int position,
 *     @FromEvent boolean animate,
 *     @Param Param someParam,
 *     @Prop Prop someProp) {
 *   // Handle the click here.
 * }
 * </pre>
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
@Event
public class ScrollEvent {
  public int position;
  public boolean animate;
}
