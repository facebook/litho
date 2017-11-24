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
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ResourceResolver;
import com.facebook.litho.testing.assertj.ComponentMatcher;
import com.facebook.litho.testing.subcomponents.InspectableComponent;
import javax.annotation.Nullable;
import org.assertj.core.description.TextDescription;

/**
 * @prop-required myStringProp java.lang.String
 * @prop-required myRequiredColorProp int
 * @prop-required myDimenSizeProp float
 * @prop-required child com.facebook.litho.Component<?>
 *
 * @see com.facebook.litho.processor.integration.resources.BasicTestSampleSpec
 */
public final class BasicTestSample implements BasicTestSampleSpec {
  public static Matcher matcher(ComponentContext c) {
    return new Matcher(c);
  }

  public static class Matcher extends ResourceResolver {
    @Nullable
    org.hamcrest.Matcher<String> mMyStringPropMatcher;

    @Nullable
    org.hamcrest.Matcher<Integer> mMyRequiredColorPropMatcher;

    @Nullable
    org.hamcrest.Matcher<Float> mMyDimenSizePropMatcher;

    @Nullable
    org.hamcrest.Matcher<Component> mChildMatcher;

    Matcher(ComponentContext c) {
      super.init(c, c.getResourceCache());
    }

    public Matcher myStringProp(org.hamcrest.Matcher<String> matcher) {
      mMyStringPropMatcher = matcher;
      return this;
    }

    public Matcher myStringProp(String myStringProp) {
      this.mMyStringPropMatcher = org.hamcrest.core.Is.is((String) myStringProp);
      return this;
    }

    public Matcher myRequiredColorProp(org.hamcrest.Matcher<Integer> matcher) {
      mMyRequiredColorPropMatcher = matcher;
      return this;
    }

    public Matcher myRequiredColorProp(@ColorInt int myRequiredColorProp) {
      this.mMyRequiredColorPropMatcher = org.hamcrest.core.Is.is((int) myRequiredColorProp);
      return this;
    }

    public Matcher myRequiredColorPropRes(@ColorRes int resId) {
      this.mMyRequiredColorPropMatcher = org.hamcrest.core.Is.is((int) resolveColorRes(resId));
      return this;
    }

    public Matcher myRequiredColorPropAttr(@AttrRes int attrResId, @ColorRes int defResId) {
      this.mMyRequiredColorPropMatcher = org.hamcrest.core.Is.is((int) resolveColorAttr(attrResId, defResId));
      return this;
    }

    public Matcher myRequiredColorPropAttr(@AttrRes int attrResId) {
      this.mMyRequiredColorPropMatcher = org.hamcrest.core.Is.is((int) resolveColorAttr(attrResId, 0));
      return this;
    }

    public Matcher myDimenSizeProp(org.hamcrest.Matcher<Float> matcher) {
      mMyDimenSizePropMatcher = matcher;
      return this;
    }

    public Matcher myDimenSizePropPx(@Px float myDimenSizeProp) {
      this.mMyDimenSizePropMatcher = org.hamcrest.core.Is.is((float) myDimenSizeProp);
      return this;
    }

    public Matcher myDimenSizePropRes(@DimenRes int resId) {
      this.mMyDimenSizePropMatcher = org.hamcrest.core.Is.is((float) resolveDimenSizeRes(resId));
      return this;
    }

    public Matcher myDimenSizePropAttr(@AttrRes int attrResId, @DimenRes int defResId) {
      this.mMyDimenSizePropMatcher = org.hamcrest.core.Is.is((float) resolveDimenSizeAttr(attrResId, defResId));
      return this;
    }

    public Matcher myDimenSizePropAttr(@AttrRes int attrResId) {
      this.mMyDimenSizePropMatcher = org.hamcrest.core.Is.is((float) resolveDimenSizeAttr(attrResId, 0));
      return this;
    }

    public Matcher myDimenSizePropDip(@Dimension(unit = Dimension.DP) float dips) {
      this.mMyDimenSizePropMatcher = org.hamcrest.core.Is.is((float) dipsToPixels(dips));
      return this;
    }

    public Matcher child(org.hamcrest.Matcher<Component> matcher) {
      mChildMatcher = matcher;
      return this;
    }

    public Matcher child(Component<?> child) {
      this.mChildMatcher = org.hamcrest.core.Is.is((Component) child);
      return this;
    }

    public Matcher child(Component.Builder<?, ?> childBuilder) {
      this.mChildMatcher = org.hamcrest.core.Is.is((Component) childBuilder.build());
      return this;
    }

    public ComponentMatcher build() {
      return new ComponentMatcher() {
        @Override
        public boolean matches(InspectableComponent value) {
          if (!value.getComponentClass().isAssignableFrom(com.facebook.litho.processor.integration.resources.BasicLayout.class)) {
            as(new TextDescription("Sub-component of type \"com.facebook.litho.processor.integration.resources.BasicLayout\""));
            return false;
          }
          final com.facebook.litho.processor.integration.resources.BasicLayout impl = (com.facebook.litho.processor.integration.resources.BasicLayout) value.getComponent();
          if (mMyStringPropMatcher != null && !mMyStringPropMatcher.matches(impl.myStringProp)) {
            return false;
          }
          if (mMyRequiredColorPropMatcher != null && !mMyRequiredColorPropMatcher.matches(impl.myRequiredColorProp)) {
            return false;
          }
          if (mMyDimenSizePropMatcher != null && !mMyDimenSizePropMatcher.matches(impl.myDimenSizeProp)) {
            return false;
          }
          if (mChildMatcher != null && !mChildMatcher.matches(impl.child)) {
            return false;
          }
          return true;
        }
      };
    }
  }
}
