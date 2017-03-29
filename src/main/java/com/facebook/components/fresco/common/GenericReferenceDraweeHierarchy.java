/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.fresco.common;

import android.graphics.ColorFilter;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.reference.Reference;
import com.facebook.drawee.drawable.AutoRotateDrawable;
import com.facebook.drawee.drawable.DrawableUtils;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.interfaces.SettableDraweeHierarchy;

import static com.facebook.drawee.generic.GenericDraweeHierarchyBuilder.DEFAULT_SCALE_TYPE;

/**
 * A wrapper around {@link com.facebook.drawee.generic.GenericDraweeHierarchy} which correctly
 * manages references.
 */
public class GenericReferenceDraweeHierarchy implements SettableDraweeHierarchy {

  private final GenericDraweeHierarchy mGenericDraweeHierarchy;

  private ComponentContext mContext;

  private Reference<Drawable> mPlaceholderReference;
  private Reference<Drawable> mRetryReference;
  private Reference<Drawable> mFailureReference;
  private Reference<Drawable> mProgressBarReference;
  private Reference<Drawable> mOverlayImageReference;

  private Drawable mPlaceholder;
  private Drawable mRetry;
  private Drawable mFailure;
  private Drawable mProgressBar;
  private Drawable mOverlayImage;

  public GenericReferenceDraweeHierarchy(GenericDraweeHierarchy genericDraweeHierarchy) {
    mGenericDraweeHierarchy = genericDraweeHierarchy;
  }

  /**
   * @return
   *    The {@link GenericDraweeHierarchy} to which this reference points to.
   */
  public GenericDraweeHierarchy getHierarchy() {
    return mGenericDraweeHierarchy;
  }

  /**
   * Set the context to be used to acquire and release references.
   *
   * @param c
   *    The context used to acquire and release references. This context is set to null on
   *    releaseReferences()
   */
  public void setContext(ComponentContext c) {
    mContext = c;
  }

  /**
   * Set the placeholder image of the wrapped GenericDraweeHierarchy through the use of a reference.
   *
   * @param placeholderReference
   *    The reference which references the placeholder drawable to use. Accepts null which will
   *    remove the current placeholder image. Make sure to call release() to release this reference.
   *
   * @param scaleType
   *    The scale type of the placeholder drawable. Accepts null which will use the default
   *    scale type defined by GenericDraweeHierarchy.
   */
  public void setPlaceholderReference(
      @Nullable Reference<Drawable> placeholderReference,
      @Nullable ScalingUtils.ScaleType scaleType) {
    if (mPlaceholderReference != null) {
      if (!Reference.shouldUpdate(mPlaceholderReference, placeholderReference)) {
        return;
      } else {
        Reference.release(mContext, mPlaceholder, mPlaceholderReference);
        mPlaceholderReference = null;
        mPlaceholder = null;
      }
    }

    if (placeholderReference == null) {
      mGenericDraweeHierarchy.setPlaceholderImage(null);
      return;
    }

    mPlaceholderReference = placeholderReference;
    mPlaceholder = Reference.acquire(mContext, placeholderReference);

    mGenericDraweeHierarchy.setPlaceholderImage(
        DrawableUtils.cloneDrawable(mPlaceholder),
        scaleType != null ? scaleType : DEFAULT_SCALE_TYPE);
  }

  /**
   * Set the retry image of the wrapped GenericDraweeHierarchy through the use of a reference.
   *
   * @param retryReference
   *    The reference which references the retry drawable to use. Accepts null which will
   *    remove the current retry image. Make sure to call release() to release this reference.
   *
   * @param scaleType
   *    The scale type of the retry drawable. Accepts null which will use the default
   *    scale type defined by GenericDraweeHierarchy.
   */
  public void setRetryReference(
      @Nullable Reference<Drawable> retryReference,
      @Nullable ScalingUtils.ScaleType scaleType) {
    if (mRetryReference != null) {
      if (!Reference.shouldUpdate(mRetryReference, retryReference)) {
        return;
      } else {
        Reference.release(mContext, mRetry, mRetryReference);
        mRetryReference = null;
        mRetry = null;
      }
    }

    if (retryReference == null) {
      mGenericDraweeHierarchy.setRetryImage(null);
      return;
    }

    mRetryReference = retryReference;
    mRetry = Reference.acquire(mContext, retryReference);

    mGenericDraweeHierarchy.setRetryImage(
        DrawableUtils.cloneDrawable(mRetry),
        scaleType != null ? scaleType : DEFAULT_SCALE_TYPE);
  }

  /**
   * Set the failure of the wrapped GenericDraweeHierarchy through the use of a reference.
   *
   * @param failureReference
   *    The reference which references the failure drawable to use. Accepts null which will
   *    remove the current failure image. Make sure to call release() to release this reference.
   *
   * @param scaleType
   *    The scale type of the failure drawable. Accepts null which will use the default
   *    scale type defined by GenericDraweeHierarchy.
   */
  public void setFailureReference(
      @Nullable Reference<Drawable> failureReference,
      @Nullable ScalingUtils.ScaleType scaleType) {
    if (mFailureReference != null) {
      if (!Reference.shouldUpdate(mFailureReference, failureReference)) {
        return;
      } else {
        Reference.release(mContext, mFailure, mFailureReference);
        mFailureReference = null;
        mFailure = null;
      }
    }

    if (failureReference == null) {
      mGenericDraweeHierarchy.setFailureImage(null);
      return;
    }

