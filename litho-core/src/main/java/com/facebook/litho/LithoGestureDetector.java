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

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.annotations.OnCreateLayout;

/**
 * Simple wrapper of {@link GestureDetector} to be used in Litho lifecycle methods. Using this class
 * ensures that UI Thread {@link Handler} is used for detecting gestures.
 *
 * <p>Main purpose of creating this wrapper is to avoid usages where developers do not explicitly
 * provide UI Thread {@link Handler} inside @{@link OnCreateLayout} or other lifecycle methods that
 * can be called from BG threads which would potentially cause the app crash.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class LithoGestureDetector extends GestureDetector {

  public LithoGestureDetector(Context context, OnGestureListener listener) {
    super(context, listener, new Handler(Looper.getMainLooper()));
  }
}
