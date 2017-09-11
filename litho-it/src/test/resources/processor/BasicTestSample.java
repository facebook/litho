/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho.processor.integration.resources;

import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.Dimension;
import android.support.annotation.Px;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ResourceResolver;
import com.facebook.litho.testing.InspectableComponent;
import com.facebook.litho.testing.assertj.ComponentMatcher;
import javax.annotation.Nullable;

/**
 * @prop-required myStringProp java.lang.String
 * @prop-required myRequiredColorProp int
 * @prop-optional myOptionalColorProp int
 * @prop-optional myOffset float
 * @prop-optional mySize float
 */
public final class BasicTestSample implements BasicTestSampleSpec {
  public static Matcher matcher(ComponentContext c) {
    return new Matcher(c);
  }

  static class Matcher extends ResourceResolver {
    @Nullable String mMyStringPropMatcher;

    @Nullable int mMyRequiredColorPropMatcher;

    @Nullable int mMyOptionalColorPropMatcher;

    @Nullable float mMyOffsetMatcher;

    @Nullable float mMySizeMatcher;

    Matcher(ComponentContext c) {
      super.init(c, c.getResourceCache());
    }

    public Matcher myStringProp(String myStringProp) {
      // TODO(T15854501): This needs to be assigned to a matcher.
      java.lang.String value = myStringProp;
      return this;
    }

    public Matcher myRequiredColorProp(@ColorInt int myRequiredColorProp) {
      // TODO(T15854501): This needs to be assigned to a matcher.
      int value = myRequiredColorProp;
      return this;
    }

    public Matcher myRequiredColorPropRes(@ColorRes int resId) {
      // TODO(T15854501): This needs to be assigned to a matcher.
      int value = resolveColorRes(resId);
      return this;
    }

    public Matcher myRequiredColorPropAttr(@AttrRes int attrResId, @ColorRes int defResId) {
      // TODO(T15854501): This needs to be assigned to a matcher.
      int value = resolveColorAttr(attrResId, defResId);
      return this;
    }

    public Matcher myRequiredColorPropAttr(@AttrRes int attrResId) {
      // TODO(T15854501): This needs to be assigned to a matcher.
      int value = resolveColorAttr(attrResId, 0);
      return this;
    }

    public Matcher myOptionalColorProp(@ColorInt int myOptionalColorProp) {
      // TODO(T15854501): This needs to be assigned to a matcher.
      int value = myOptionalColorProp;
      return this;
    }

    public Matcher myOptionalColorPropRes(@ColorRes int resId) {
      // TODO(T15854501): This needs to be assigned to a matcher.
      int value = resolveColorRes(resId);
      return this;
    }

    public Matcher myOptionalColorPropAttr(@AttrRes int attrResId, @ColorRes int defResId) {
      // TODO(T15854501): This needs to be assigned to a matcher.
      int value = resolveColorAttr(attrResId, defResId);
      return this;
    }

    public Matcher myOptionalColorPropAttr(@AttrRes int attrResId) {
      // TODO(T15854501): This needs to be assigned to a matcher.
      int value = resolveColorAttr(attrResId, 0);
      return this;
    }

    public Matcher myOffsetPx(@Px float myOffset) {
      // TODO(T15854501): This needs to be assigned to a matcher.
      float value = myOffset;
      return this;
    }

    public Matcher myOffsetRes(@DimenRes int resId) {
      // TODO(T15854501): This needs to be assigned to a matcher.
      float value = resolveDimenOffsetRes(resId);
      return this;
    }

    public Matcher myOffsetAttr(@AttrRes int attrResId, @DimenRes int defResId) {
      // TODO(T15854501): This needs to be assigned to a matcher.
      float value = resolveDimenOffsetAttr(attrResId, defResId);
      return this;
    }

    public Matcher myOffsetAttr(@AttrRes int attrResId) {
      // TODO(T15854501): This needs to be assigned to a matcher.
      float value = resolveDimenOffsetAttr(attrResId, 0);
      return this;
    }

    public Matcher myOffsetDip(@Dimension(unit = Dimension.DP) float dips) {
      // TODO(T15854501): This needs to be assigned to a matcher.
      float value = dipsToPixels(dips);
      return this;
    }

    public Matcher mySizePx(@Px float mySize) {
      // TODO(T15854501): This needs to be assigned to a matcher.
      float value = mySize;
      return this;
    }

    public Matcher mySizeRes(@DimenRes int resId) {
      // TODO(T15854501): This needs to be assigned to a matcher.
      float value = resolveDimenSizeRes(resId);
      return this;
    }

    public Matcher mySizeAttr(@AttrRes int attrResId, @DimenRes int defResId) {
      // TODO(T15854501): This needs to be assigned to a matcher.
      float value = resolveDimenSizeAttr(attrResId, defResId);
      return this;
    }

    public Matcher mySizeAttr(@AttrRes int attrResId) {
      // TODO(T15854501): This needs to be assigned to a matcher.
      float value = resolveDimenSizeAttr(attrResId, 0);
      return this;
    }

    public Matcher mySizeDip(@Dimension(unit = Dimension.DP) float dips) {
      // TODO(T15854501): This needs to be assigned to a matcher.
      float value = dipsToPixels(dips);
      return this;
    }

    public ComponentMatcher build() {
      return new ComponentMatcher() {
        @Override
        public boolean matches(InspectableComponent value) {
          // TODO(T15854501): Implement matching.
          return false;
        }
      };
    }
  }
}
