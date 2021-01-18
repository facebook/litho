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

package com.facebook.litho.widget;

import android.text.style.ClickableSpan;
import android.view.View;

/**
 * A listener that reacts to click and long click events on a clickable span. A listener such as
 * this should be used when the click/long click action needs to access heavyweight objects such as
 * fragments or activities. Using such objects within the spannable can cause memory leaks as
 * spannables are cached in {@link android.text.TextLine#sCached} which is static.
 */
public interface ClickableSpanListener {

  /**
   * @return true if click action needs to be handled here instead of delegating it to {@link
   *     ClickableSpan#onClick(View)}.
   */
  boolean onClick(ClickableSpan span, View view);

  /**
   * @return true if long click action needs to be handled here instead of delegating to {@link
   *     LongClickableSpan#onLongClick(View)}.
   */
  boolean onLongClick(LongClickableSpan span, View view);
}
