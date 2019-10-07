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

package com.facebook.litho.viewcompat;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * Creates a View of the specified type.
 *
 * @param <V> the type of View to create.
 */
public interface ViewCreator<V extends View> {

  /**
   * @param c android Context.
   * @param parent the parent {@link ViewGroup}, or {@code null} if there isn't one.
   * @return a new view of type V.
   */
  V createView(Context c, ViewGroup parent);
}
