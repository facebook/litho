/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import com.facebook.yoga.YogaAlign;

import com.facebook.yoga.YogaFlexDirection;

import java.util.concurrent.atomic.AtomicInteger;

import android.support.annotation.AttrRes;
import android.support.annotation.StyleRes;

import com.facebook.litho.ComponentLifecycle.MountType;
import com.facebook.litho.ComponentLifecycle.StateContainer;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.infer.annotation.ThreadSafe;

/**
 * Represents a unique instance of a component that is driven by its matching
 * {@link ComponentLifecycle}. To create new {@link Component} instances, use the
 * {@code create()} method in the generated {@link ComponentLifecycle} subclass which
 * returns a builder that allows you to set values for individual props. {@link Component}
 * instances are immutable after creation.
 */
public abstract class Component<L extends ComponentLifecycle> implements HasEventDispatcher {

  public static abstract class Builder<L extends ComponentLifecycle>
      extends ResourceResolver {
    private ComponentContext mContext;
    private @AttrRes int mDefStyleAttr;
    private @StyleRes int mDefStyleRes;
    private Component mComponent;

    protected void init(
        ComponentContext c,
        @AttrRes int defStyleAttr,
        @StyleRes int defStyleRes,
        Component<L> component) {
      super.init(c, c.getResourceCache());

      mComponent = component;
      mContext = c;
      mDefStyleAttr = defStyleAttr;
      mDefStyleRes = defStyleRes;

      if (defStyleAttr != 0 || defStyleRes != 0) {
        component.mLifecycle.loadStyle(c, defStyleAttr, defStyleRes, component);
      }
    }

