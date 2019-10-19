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
 * Components should implement an event of this type in order to receive Android long click events.
 * The method is equivalent to the Android method {@link View.OnLongClickListener#onLongClick(View)}
 * - implementations should return true if they consumed the long click and false otherwise.
 * An example of the correct usage is:
 *
 * <pre>
 * {@code
 *
 * @OnEvent(LongClickEvent.class)
 * static boolean onLongClick(
 *     @FromEvent View view,
 *     @Param Param someParam
 *     @Prop Prop someProp) {
 *   if (shouldHandleLongClick(someParam, someProp)) {
 *     handleLongClick(view);
 *     return true;
 *   }
 *
 *   return false;
 * }
 * </pre>
 */
@Event(returnType = boolean.class)
public class LongClickEvent {
  public View view;
}
