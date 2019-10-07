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

import android.view.View;
import com.facebook.litho.annotations.Event;

/**
 * Components should implement an event of this type in order to receive Android click events. The
 * method is equivalent to the Android method {@link View.OnClickListener#onClick(View)}.
 * An example of the correct usage is:
 *
 * <pre>
 * {@code
 *
 * @OnEvent(ClickEvent.class)
 * static void onClick(
 *     ComponentContext c,
 *     @FromEvent View view,
 *     @Param Param someParam,
 *     @Prop Prop someProp) {
 *   // Handle the click here.
 * }
 * </pre>
 */
@Event
public class ClickEvent {
  public View view;
}
