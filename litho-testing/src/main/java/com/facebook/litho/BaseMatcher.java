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

package com.facebook.litho;

import javax.annotation.Nullable;
import org.hamcrest.Matcher;
import org.hamcrest.core.Is;

/**
 * Base class used to share common properties used in TestSpec builders. Mirrors relevant properties
 * from {@link com.facebook.litho.Component.Builder}.
 */
public abstract class BaseMatcher<T extends BaseMatcher<T>> {
  @Nullable Matcher<EventHandler<ClickEvent>> mClickHandlerMatcher;
  @Nullable Matcher<EventHandler<LongClickEvent>> mLongClickHandlerMatcher;
  @Nullable Matcher<EventHandler<FocusChangedEvent>> mFocusChangeHandlerMatcher;
  @Nullable Matcher<EventHandler<TouchEvent>> mTouchEventHandlerMatcher;
  @Nullable Matcher<EventHandler<InterceptTouchEvent>> mInterceptTouchHandlerMatcher;
  @Nullable Matcher<Boolean> mFocusable;
  @Nullable Matcher<String> mTransitionKey;

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

  public T focusable(boolean focusable) {
    mFocusable = Is.is(focusable);
    return getThis();
  }

  public T focusable(Matcher<Boolean> focusableMatcher) {
    mFocusable = focusableMatcher;
    return getThis();
  }

  public T transitionKey(Matcher<String> transitionKeyMatcher) {
    mTransitionKey = transitionKeyMatcher;
    return getThis();
  }

  public T transitionKey(String transitionKey) {
    mTransitionKey = Is.is(transitionKey);
    return getThis();
  }

  protected abstract T getThis();
}
