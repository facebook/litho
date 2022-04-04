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

package com.facebook.rendercore.text;

import android.text.SpannableStringBuilder;
import android.view.View;
import java.util.ArrayList;

/**
 * A {@link SpannableStringBuilder} that implements {@link MountableCharSequence} and is aware when
 * the TextRenderUnit using it is mounted and unmounted.
 */
public class MountableSpannableStringBuilder extends SpannableStringBuilder
    implements MountableCharSequence {
  private final ArrayList<MountUnmountListener> mMountUnmountListeners;

  public MountableSpannableStringBuilder(final CharSequence text) {
    super(text);
    mMountUnmountListeners = new ArrayList<>();
  }

  /**
   * This will be called when the TextRenderUnit is mounted.
   *
   * @param parent the parent View the string builder is being mounted to.
   */
  @Override
  public void onMount(View parent) {
    for (final MountUnmountListener mountUnmountListener : mMountUnmountListeners) {
      mountUnmountListener.onMount(parent);
    }
  }

  /**
   * This will be called when the TextRenderUnit is unmounted.
   *
   * @param parent the parent View the string builder is being unmounted from.
   */
  @Override
  public void onUnmount(View parent) {
    for (final MountUnmountListener mountUnmountListener : mMountUnmountListeners) {
      mountUnmountListener.onUnmount(parent);
    }
  }

  /**
   * Adds a listener to the set of listeners that will be notified of mount/unmount events.
   *
   * @param mountUnmountListener The listener to add.
   */
  public void addMountUnmountListener(final MountUnmountListener mountUnmountListener) {
    mMountUnmountListeners.add(mountUnmountListener);
  }

  /** A listener that is notified of mount/unmount events. */
  public interface MountUnmountListener {
    void onMount(final View parent);

    void onUnmount(final View parent);
  }
}
