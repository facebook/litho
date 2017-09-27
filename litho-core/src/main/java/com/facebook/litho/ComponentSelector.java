/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.support.annotation.Nullable;

/**
 * A utility that selects the first Component that will render by calling {@link
 * Component#willRender(ComponentLayout)} on the Components provided.
 *
 * <p>This is useful when a single Component is to be rendered amongst a large number of candidate
 * Components or when multiple Components can potentially render some content using the same props
 * and the first one capable of rendering the content needs to be used.
 */
public class ComponentSelector {

  private ComponentContext mContext;
  private ComponentLayout mLayout;
  private boolean mWillRender;

  private ComponentSelector(ComponentContext context) {
    mContext = context;
  }

  public static ComponentSelector create(ComponentContext context) {
    return new ComponentSelector(context);
  }

  public ComponentSelector tryToRender(@Nullable Component.Builder<?, ?> componentBuilder) {
    if (componentBuilder == null || mWillRender) {
      return this;
    }

    return tryToRender(componentBuilder.buildWithLayout());
  }

  public ComponentSelector tryToRender(@Nullable Component<?> component) {
    if (component == null || mWillRender) {
      return this;
    }

    return tryToRender(Layout.create(mContext, component).build());
  }

  public ComponentSelector tryToRender(@Nullable ComponentLayout.Builder layoutBuilder) {
    if (layoutBuilder == null || mWillRender) {
      return this;
    }

    return tryToRender(layoutBuilder.build());
  }

  public ComponentSelector tryToRender(@Nullable ComponentLayout layout) {
    if (mWillRender) {
      return this;
    }

    mLayout = layout;
    mWillRender = Component.willRender(layout);
    return this;
  }

  public @Nullable ComponentLayout build() {
    return mLayout;
  }
}
