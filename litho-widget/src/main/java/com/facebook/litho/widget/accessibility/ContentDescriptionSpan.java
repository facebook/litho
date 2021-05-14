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

package com.facebook.widget.accessibility.delegates;

import android.text.TextPaint;
import android.text.style.CharacterStyle;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

/**
 * Extends the CharacterStyle class to include a dedicated field for an accessibility content
 * description. This is useful in cases where the spanned content either cannot be described via the
 * spanned text alone (for example, an image) or when the text of the span could use extra
 * clarification for users of accessibility services like screen readers.
 *
 * <p>For example, some text that says "Click the button above to continue" may not be descriptive
 * enough for a user without the visual context of which button is above the text. You could use
 * this span to change "button above" to something more descriptive like "next step button" without
 * changing the visual text.
 *
 * <pre>{@code
 * SpannableStringBuilder sb = new SpannableStringBuilder("Click the button above to continue");
 * sb.setSpan(
 *   new ContentDescriptionSpan("next step button"), 10, 22, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
 *
 * Text.create(c).text(sb).build();
 * }</pre>
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class ContentDescriptionSpan extends CharacterStyle {
  private @Nullable String mContentDescription;

  public ContentDescriptionSpan(@Nullable String contentDescription) {
    mContentDescription = contentDescription;
  }

  public @Nullable String getContentDescription() {
    return mContentDescription;
  }

  public void setContentDescription(String contentDescription) {
    mContentDescription = contentDescription;
  }

  @Override
  public void updateDrawState(TextPaint tp) {}
}
