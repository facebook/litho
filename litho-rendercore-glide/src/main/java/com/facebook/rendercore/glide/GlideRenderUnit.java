// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.rendercore.glide;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.facebook.rendercore.ContentAllocator;
import com.facebook.rendercore.RenderUnit;

public class GlideRenderUnit extends RenderUnit<ImageView> implements ContentAllocator<ImageView> {

  private Uri mURI;
  private Drawable mErrorDrawable;
  private Drawable mPlaceHolderDrawable;
  private long mId;

  public GlideRenderUnit(long id) {
    super(RenderType.VIEW);
    addOptionalMountBinder(DelegateBinder.createDelegateBinder(this, sMount));
    mId = id;
  }

  @Override
  public ImageView createContent(Context c) {
    return new ImageView(c);
  }

  @Override
  public ContentAllocator<ImageView> getContentAllocator() {
    return this;
  }

  @Override
  public long getId() {
    return mId;
  }

  public void setURI(Uri URI) {
    mURI = URI;
  }

  public void setErrorDrawable(Drawable errorDrawable) {
    mErrorDrawable = errorDrawable;
  }

  public void setPlaceHolderDrawable(Drawable placeHolderDrawable) {
    mPlaceHolderDrawable = placeHolderDrawable;
  }

  private static final Binder<GlideRenderUnit, ImageView, Void> sMount =
      new Binder<GlideRenderUnit, ImageView, Void>() {
        @Override
        public boolean shouldUpdate(
            GlideRenderUnit currentValue,
            GlideRenderUnit newValue,
            Object currentLayoutData,
            Object nextLayoutData) {
          if (!currentValue.mURI.equals(newValue.mURI)) {
            return true;
          }

          if (currentValue.mErrorDrawable != newValue.mErrorDrawable) {
            return true;
          }

          if (currentValue.mPlaceHolderDrawable != newValue.mPlaceHolderDrawable) {
            return true;
          }

          return false;
        }

        @Override
        public Void bind(
            Context context,
            ImageView imageView,
            GlideRenderUnit glideRenderUnit,
            Object layoutData) {
          Glide.with(imageView)
              .load(glideRenderUnit.mURI)
              .apply(
                  new RequestOptions()
                      .placeholder(glideRenderUnit.mPlaceHolderDrawable)
                      .error(glideRenderUnit.mErrorDrawable))
              .into(imageView);
          return null;
        }

        @Override
        public void unbind(
            Context context,
            ImageView imageView,
            GlideRenderUnit glideRenderUnit,
            Object layoutData,
            Void bindData) {
          Glide.with(imageView).clear(imageView);
        }
      };
}
