package com.facebook.litho.glide;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;
import com.bumptech.glide.DrawableTypeRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Size;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.utils.MeasureUtils;
import java.io.File;

import static com.facebook.litho.annotations.ResType.DRAWABLE;
import static com.facebook.litho.annotations.ResType.INT;

@MountSpec
public class GlideImageSpec {

  private static final int DEFAULT_INT_VALUE = -1;

  @PropDefault
  protected static final float aspectRatio = 1f;

  @PropDefault
  protected static final int crossFadeDuration = DEFAULT_INT_VALUE;

  @PropDefault
  protected static final int failureImageResId = DEFAULT_INT_VALUE;

  @PropDefault
  protected static final int fallbackImageResId = DEFAULT_INT_VALUE;

  @PropDefault
  protected static final int placeholderImageResId = DEFAULT_INT_VALUE;

  @OnMeasure
  static void onMeasureLayout(ComponentContext c, ComponentLayout layout, int widthSpec,
      int heightSpec, Size size,
      @Prop(optional = true, resType = ResType.FLOAT) float aspectRatio) {
    MeasureUtils.measureWithAspectRatio(widthSpec, heightSpec, aspectRatio, size);
  }

  @OnCreateMountContent
  static ImageView onCreateMountContent(ComponentContext c) {
    ImageView imageView = new ImageView(c.getBaseContext());
    return imageView;
  }

  @OnMount
  static void onMount(ComponentContext c, ImageView imageView,
      @Prop(optional = true) String imageUrl, @Prop(optional = true) File file,
      @Prop(optional = true) Uri uri, @Prop(optional = true) Integer resourceId,
      @Prop(optional = true) RequestManager glideRequestManager,
      @Prop(optional = true, resType = DRAWABLE) Drawable failureImage,
      @Prop(optional = true, resType = INT) int failureImageResId,
      @Prop(optional = true, resType = DRAWABLE) Drawable fallbackImage,
      @Prop(optional = true, resType = INT) int fallbackImageResId,
      @Prop(optional = true, resType = DRAWABLE) Drawable placeholderImage,
      @Prop(optional = true, resType = INT) int placeholderImageResId,
      @Prop(optional = true) DiskCacheStrategy diskCacheStrategy,
      @Prop(optional = true) RequestListener requestListener,
      @Prop(optional = true) boolean asBitmap, @Prop(optional = true) boolean asGif,
      @Prop(optional = true) boolean crossFade, @Prop(optional = true) int crossFadeDuration,
      @Prop(optional = true) boolean centerCrop, @Prop(optional = true) boolean fitCenter,
      @Prop(optional = true) boolean skipMemoryCache, @Prop(optional = true) Target target) {

    if (imageUrl == null && file == null && uri == null && resourceId == null) {
      throw new IllegalArgumentException(
          "You must provide at least one of String, File, Uri or ResourceId");
    }

    if (glideRequestManager == null) {
      glideRequestManager = Glide.with(c.getBaseContext());
    }

    DrawableTypeRequest request;

    if (imageUrl != null) {
      request = glideRequestManager.load(imageUrl);
    } else if (file != null) {
      request = glideRequestManager.load(file);
    } else if (uri != null) {
      request = glideRequestManager.load(uri);
    } else {
      request = glideRequestManager.load(resourceId);
    }

    if (request == null) {
      throw new IllegalStateException("Could not instantiate DrawableTypeRequest");
    }

    if (diskCacheStrategy != null) {
      request.diskCacheStrategy(diskCacheStrategy);
    }

    if (asBitmap) {
      request.asBitmap();
    }

    if (asGif) {
      request.asGif();
    }

    if (crossFade) {
      request.crossFade();
    }

    if (crossFadeDuration != DEFAULT_INT_VALUE) {
      request.crossFade(crossFadeDuration);
    }

    if (centerCrop) {
      request.centerCrop();
    }

    if (failureImage != null) {
      request.error(failureImage);
    }

    if (failureImageResId != DEFAULT_INT_VALUE) {
      request.error(failureImageResId);
    }

    if (fallbackImage != null) {
      request.fallback(fallbackImage);
    }

    if (fallbackImageResId != DEFAULT_INT_VALUE) {
      request.fallback(fallbackImageResId);
    }

    if (fitCenter) {
      request.fitCenter();
    }

    if (requestListener != null) {
      request.listener(requestListener);
    }

    if (placeholderImage != null) {
      request.placeholder(placeholderImage);
    }

    if (placeholderImageResId != DEFAULT_INT_VALUE) {
      request.placeholder(placeholderImageResId);
    }

    request.skipMemoryCache(skipMemoryCache);

    if (target != null) {
      request.into(target);
    } else {
      request.into(imageView);
    }
  }

  @OnUnmount
  static void onUnmount(ComponentContext c, ImageView imageView) {
    Glide.clear(imageView);
  }
}
