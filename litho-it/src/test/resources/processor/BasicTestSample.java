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
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ResourceResolver;
import com.facebook.litho.testing.InspectableComponent;
import com.facebook.litho.testing.assertj.ComponentMatcher;
import javax.annotation.Nullable;

/**
 * @prop-required myStringProp java.lang.String
 * @prop-required myRequiredColorProp int
 */
public final class BasicTestSample implements BasicTestSampleSpec {
  public static Matcher matcher(ComponentContext c) {
    return new Matcher(c);
  }

  static class Matcher extends ResourceResolver {
    @Nullable org.hamcrest.Matcher<String> mMyStringPropMatcher;

    @Nullable org.hamcrest.Matcher<Integer> mMyRequiredColorPropMatcher;

    Matcher(ComponentContext c) {
      super.init(c, c.getResourceCache());
    }

    public Matcher myStringProp(org.hamcrest.Matcher<String> matcher) {
      mMyStringPropMatcher = matcher;
      return this;
    }

    public Matcher myStringProp(String myStringProp) {
      this.mMyStringPropMatcher = org.hamcrest.core.Is.is(myStringProp);
      return this;
    }

    public Matcher myRequiredColorProp(org.hamcrest.Matcher<Integer> matcher) {
      mMyRequiredColorPropMatcher = matcher;
      return this;
    }

    public Matcher myRequiredColorProp(@ColorInt int myRequiredColorProp) {
      this.mMyRequiredColorPropMatcher = org.hamcrest.core.Is.is(myRequiredColorProp);
      return this;
    }

    public Matcher myRequiredColorPropRes(@ColorRes int resId) {
      this.mMyRequiredColorPropMatcher = org.hamcrest.core.Is.is(resolveColorRes(resId));
      return this;
    }

    public Matcher myRequiredColorPropAttr(@AttrRes int attrResId, @ColorRes int defResId) {
      this.mMyRequiredColorPropMatcher =
          org.hamcrest.core.Is.is(resolveColorAttr(attrResId, defResId));
      return this;
    }

    public Matcher myRequiredColorPropAttr(@AttrRes int attrResId) {
      this.mMyRequiredColorPropMatcher = org.hamcrest.core.Is.is(resolveColorAttr(attrResId, 0));
      return this;
    }

    public ComponentMatcher build() {
      return new ComponentMatcher() {
        @Override
        public boolean matches(InspectableComponent value) {
          if (!value
              .getComponentClass()
              .isAssignableFrom(
                  com.facebook.litho.processor.integration.resources.BasicLayout.class)) {
            return false;
          }
          final com.facebook.litho.processor.integration.resources.BasicLayout.BasicLayoutImpl
              impl =
                  (com.facebook.litho.processor.integration.resources.BasicLayout.BasicLayoutImpl)
                      value.getComponent();
          if (mMyStringPropMatcher != null && !mMyStringPropMatcher.matches(impl.myStringProp)) {
            return false;
          }
          if (mMyRequiredColorPropMatcher != null
              && !mMyRequiredColorPropMatcher.matches(impl.myRequiredColorProp)) {
            return false;
          }
          return true;
        }
      };
    }
  }
}
