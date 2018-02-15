/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

import com.facebook.litho.testing.subcomponents.InspectableComponent;
import org.assertj.core.api.Condition;
import org.assertj.core.description.TextDescription;

public final class BaseMatcherBuilder {
  private BaseMatcherBuilder() {}

  public static Condition<InspectableComponent> buildCommonMatcher(final BaseMatcher matcher) {
    return new Condition<InspectableComponent>() {
      @Override
      public boolean matches(InspectableComponent component) {
        final Component underlyingComponent = component.getComponent();
        final CommonProps commonProps = underlyingComponent.getCommonProps();
        if (matcher.mClickHandlerMatcher != null
            && commonProps != null
            && !matcher.mClickHandlerMatcher.matches(commonProps.getClickHandler())) {
          as(
              new TextDescription(
                  "Click handler <%s> (doesn't match <%s>)",
                  matcher.mClickHandlerMatcher, commonProps.getClickHandler()));
          return false;
        }

        if (matcher.mLongClickHandlerMatcher != null
            && commonProps != null
            && !matcher.mLongClickHandlerMatcher.matches(commonProps.getLongClickHandler())) {
          as(
              new TextDescription(
                  "LongClick handler <%s> (doesn't match <%s>)",
                  matcher.mLongClickHandlerMatcher, commonProps.getLongClickHandler()));
          return false;
        }

        if (matcher.mFocusChangeHandlerMatcher != null
            && commonProps != null
            && !matcher.mFocusChangeHandlerMatcher.matches(commonProps.getFocusChangeHandler())) {
          as(
              new TextDescription(
                  "FocusChange handler <%s> (doesn't match <%s>)",
                  matcher.mFocusChangeHandlerMatcher, commonProps.getFocusChangeHandler()));
          return false;
        }

        if (matcher.mTouchEventHandlerMatcher != null
            && commonProps != null
            && !matcher.mTouchEventHandlerMatcher.matches(commonProps.getTouchHandler())) {
          as(
              new TextDescription(
                  "TouchEvent handler <%s> (doesn't match <%s>)",
                  matcher.mTouchEventHandlerMatcher, commonProps.getTouchHandler()));
          return false;
        }

        if (matcher.mInterceptTouchHandlerMatcher != null
            && commonProps != null
            && !matcher.mInterceptTouchHandlerMatcher.matches(
                commonProps.getInterceptTouchHandler())) {
          as(
              new TextDescription(
                  "InterceptTouch handler <%s> (doesn't match <%s>)",
                  matcher.mInterceptTouchHandlerMatcher, commonProps.getInterceptTouchHandler()));
          return false;
        }

        return true;
      }
    };
  }
}
