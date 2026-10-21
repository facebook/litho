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

import static com.facebook.rendercore.RenderUnit.DelegateBinder.createDelegateBinder;

import android.content.Context;
import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.ContentAllocator;
import com.facebook.rendercore.RenderType;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.text.TextMeasurementUtils.TextLayout;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class TextRenderUnit extends RenderUnit<RCTextView> implements ContentAllocator<RCTextView> {
  private long mId;
  private final boolean mDoesMountRenderTreeHosts;

  @SuppressWarnings("this-escape")
  public TextRenderUnit(long id) {
    this(id, false);
  }

  @SuppressWarnings("this-escape")
  public TextRenderUnit(long id, boolean doesMountRenderTreeHosts) {
    super();
    mId = id;
    mDoesMountRenderTreeHosts = doesMountRenderTreeHosts;
    addOptionalMountBinders(
        createDelegateBinder(this, sFinalUnmount), createDelegateBinder(this, sMountUnmount));
  }

  @Override
  public RCTextView createContent(Context c) {
    return new RCTextView(c);
  }

  @Override
  public ContentAllocator<RCTextView> getContentAllocator() {
    return this;
  }

  @Override
  public RenderType getRenderType() {
    return RenderType.VIEW;
  }

  @Override
  public long getId() {
    return mId;
  }

  @Override
  public boolean doesMountRenderTreeHosts() {
    return mDoesMountRenderTreeHosts;
  }

  public static Binder<TextRenderUnit, RCTextView, Void> sMountUnmount =
      new Binder<TextRenderUnit, RCTextView, Void>() {
        @Override
        public boolean shouldUpdate(
            TextRenderUnit currentValue,
            TextRenderUnit newValue,
            @Nullable Object currentLayoutData,
            @Nullable Object newLayoutData) {
          return true;
        }

        @Override
        public @Nullable Void bind(
            Context context,
            RCTextView textView,
            TextRenderUnit textRenderUnit,
            @Nullable Object layoutData) {
          if (layoutData == null) {
            throw new RuntimeException("Missing text layout context!");
          }
          final TextLayout textLayout = (TextLayout) layoutData;
          textView.mount(textLayout);

          return null;
        }

        @Override
        public void unbind(
            Context context,
            RCTextView textView,
            TextRenderUnit textRenderUnit,
            @Nullable Object layoutData,
            Void bindData) {
          textView.unmount();
        }
      };

  public static Binder<TextRenderUnit, RCTextView, Void> sFinalUnmount =
      new Binder<TextRenderUnit, RCTextView, Void>() {
        @Override
        public boolean shouldUpdate(
            TextRenderUnit currentValue,
            TextRenderUnit newValue,
            @Nullable Object currentLayoutData,
            @Nullable Object newLayoutData) {
          return false;
        }

        @Override
        public @Nullable Void bind(
            Context context,
            RCTextView textView,
            TextRenderUnit textRenderUnit,
            @Nullable Object layoutData) {
          return null;
        }

        @Override
        public void unbind(
            Context context,
            RCTextView textView,
            TextRenderUnit textRenderUnit,
            @Nullable Object layoutData,
            Void bindData) {
          textView.finalUnmount();
        }
      };
}
