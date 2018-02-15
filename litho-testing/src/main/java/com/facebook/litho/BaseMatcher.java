/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

import javax.annotation.Nullable;
import org.hamcrest.Matcher;
import org.hamcrest.core.Is;

/**
 * Base class used to share common properties used in TestSpec builders. Mirrors relevant properties
 * from {@link com.facebook.litho.Component.Builder}.
 */
public abstract class BaseMatcher<T extends BaseMatcher<T>> extends ResourceResolver {
  @Nullable Matcher<EventHandler<ClickEvent>> mClickHandlerMatcher;
  @Nullable Matcher<EventHandler<LongClickEvent>> mLongClickHandlerMatcher;
  @Nullable Matcher<EventHandler<FocusChangedEvent>> mFocusChangeHandlerMatcher;
  @Nullable Matcher<EventHandler<TouchEvent>> mTouchEventHandlerMatcher;
  @Nullable Matcher<EventHandler<InterceptTouchEvent>> mInterceptTouchHandlerMatcher;

  public T clickHandler(EventHandler<ClickEvent> clickHandler) {
    mClickHandlerMatcher = Is.is(clickHandler);
    return getThis();
  }

  public T clickHandler(Matcher<EventHandler<ClickEvent>> clickHandlerMatcher) {
    mClickHandlerMatcher = clickHandlerMatcher;
    return getThis();
  }

  public T longClickHandler(EventHandler<LongClickEvent> longClickHandler) {
    mLongClickHandlerMatcher = Is.is(longClickHandler);
    return getThis();
  }

  public T longClickHandler(Matcher<EventHandler<LongClickEvent>> longClickHandlerMatcher) {
    mLongClickHandlerMatcher = longClickHandlerMatcher;
    return getThis();
  }

  public T focusChangeHandler(EventHandler<FocusChangedEvent> focusChangeHandler) {
    mFocusChangeHandlerMatcher = Is.is(focusChangeHandler);
    return getThis();
  }

  public T focusChangeHandler(Matcher<EventHandler<FocusChangedEvent>> focusChangeHandlerMatcher) {
    mFocusChangeHandlerMatcher = focusChangeHandlerMatcher;
    return getThis();
  }

  public T touchHandler(EventHandler<TouchEvent> touchHandler) {
    mTouchEventHandlerMatcher = Is.is(touchHandler);
    return getThis();
  }

  public T touchHandler(Matcher<EventHandler<TouchEvent>> touchEventHandlerMatcher) {
    mTouchEventHandlerMatcher = touchEventHandlerMatcher;
    return getThis();
  }

  public T interceptTouchHandler(EventHandler<InterceptTouchEvent> interceptTouchHandler) {
    mInterceptTouchHandlerMatcher = Is.is(interceptTouchHandler);
    return getThis();
  }

  public T interceptTouchHandler(
      Matcher<EventHandler<InterceptTouchEvent>> interceptTouchHandlerMatcher) {
    mInterceptTouchHandlerMatcher = interceptTouchHandlerMatcher;
    return getThis();
  }

  protected abstract T getThis();
}
