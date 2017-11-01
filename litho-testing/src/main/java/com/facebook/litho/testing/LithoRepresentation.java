/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.LithoViewTestHelper;
import org.assertj.core.presentation.StandardRepresentation;

/**
 * Inheritance sucks. For now, we're stuck with this we can't go beyond Java 7 with Android for
 * compatibility reasons and only the Java 8 version of AssertJ supports registering individual
 * representations for objects.
 */
public class LithoRepresentation extends StandardRepresentation {
  private final ComponentContext mComponentContext;

  public LithoRepresentation(ComponentContext mComponentContext) {
    this.mComponentContext = mComponentContext;
  }

  /**
   * Returns the standard {@code toString} representation of the given object. It may or not the
   * object's own implementation of {@code toString}.
   *
   * @param object the given object.
   * @return the {@code toString} representation of the given object.
   */
  @Override
  public String toStringOf(Object object) {
    if (object instanceof Component) {
      final LithoView lithoView =
          ComponentTestHelper.mountComponent(mComponentContext, (Component) object);
      return LithoViewTestHelper.viewToString(lithoView);
    }
    if (object instanceof Component.Builder) {
      final LithoView lithoView = ComponentTestHelper.mountComponent((Component.Builder) object);
      return LithoViewTestHelper.viewToString(lithoView);
    }
    if (object instanceof LithoView) {
      return LithoViewTestHelper.viewToString((LithoView) object);
    }
    return super.toStringOf(object);
  }
}
