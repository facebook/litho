/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
 *     @FromEvent View view,
 *     @Param Param someParam,
 *     @Prop Prop someProp) {
 *   // Handle the click here.
 * }
 * </pre>
 */
