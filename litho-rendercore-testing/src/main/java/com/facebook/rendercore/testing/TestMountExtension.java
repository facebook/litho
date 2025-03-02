/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.rendercore.testing;

import android.graphics.Rect;
import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.extensions.ExtensionState;
import com.facebook.rendercore.extensions.MountExtension;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class TestMountExtension extends MountExtension<Object, Object> {

  // NULLSAFE_FIXME[Field Not Initialized]
  private Object input;

  @Override
  // NULLSAFE_FIXME[Inconsistent Subclass Return Annotation]
  protected Void createState() {
    return null;
  }

  @Override
  public void beforeMount(
      ExtensionState<Object> state, @Nullable Object o, @Nullable Rect localVisibleRect) {
    super.beforeMount(state, o, localVisibleRect);
    // NULLSAFE_FIXME[Field Not Nullable]
    this.input = o;
  }

  public Object getInput() {
    return input;
  }
}
