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

package com.facebook.litho.testing.assertj;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.LithoViewTestHelper;
import com.facebook.litho.testing.helper.ComponentTestHelper;
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
