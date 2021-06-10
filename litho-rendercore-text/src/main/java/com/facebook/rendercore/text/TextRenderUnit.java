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

package com.facebook.rendercore.text;

import static com.facebook.rendercore.RenderUnit.Extension.extension;
import static com.facebook.rendercore.RenderUnit.RenderType.VIEW;

import android.content.Context;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.text.TextMeasurementUtils.TextLayoutContext;

public class TextRenderUnit extends RenderUnit<RCTextView> {
  private long mId;

  public TextRenderUnit(long id) {
    super(VIEW);
    mId = id;
    addMountUnmountExtension(extension(this, sMountUnmount));
  }

  @Override
  public RCTextView createContent(Context c) {
    return new RCTextView(c);
  }

  @Override
  public long getId() {
    return mId;
  }

  public static Binder<TextRenderUnit, RCTextView> sMountUnmount =
      new Binder<TextRenderUnit, RCTextView>() {
        @Override
        public boolean shouldUpdate(
            TextRenderUnit currentValue,
            TextRenderUnit newValue,
            Object currentLayoutData,
            Object newLayoutData) {
          return true;
        }

        @Override
        public void bind(
            Context context,
            RCTextView textView,
            TextRenderUnit textRenderUnit,
            Object layoutData) {
          if (layoutData == null) {
            throw new RuntimeException("Missing text layout context!");
          }
          final TextLayoutContext textLayoutContext = (TextLayoutContext) layoutData;

          textView.mount(
              textLayoutContext.processedText,
              textLayoutContext.layout,
              textLayoutContext.textLayoutTranslationX,
              textLayoutContext.textLayoutTranslationY,
              textLayoutContext.textStyle.clipToBounds,
              textLayoutContext.textStyle.textColorStateList,
              textLayoutContext.textStyle.textColor,
              textLayoutContext.textStyle.highlightColor,
              textLayoutContext.imageSpans,
              textLayoutContext.clickableSpans,
              textLayoutContext.textStyle.highlightStartOffset,
              textLayoutContext.textStyle.highlightEndOffset,
              textLayoutContext.textStyle.highlightCornerRadius);

          if (textLayoutContext.processedText instanceof MountableCharSequence) {
            ((MountableCharSequence) textLayoutContext.processedText).onMount(textView);
          }
        }

        @Override
        public void unbind(
            Context context,
            RCTextView textView,
            TextRenderUnit textRenderUnit,
            Object layoutData) {
          textView.unmount();
          final TextLayoutContext textLayoutContext = (TextLayoutContext) layoutData;

          if (textLayoutContext == null) {
            throw new RuntimeException("Missing text layout context!");
          }

          if (textLayoutContext.processedText instanceof MountableCharSequence) {
            ((MountableCharSequence) textLayoutContext.processedText).onUnmount(textView);
          }
        }
      };
}
