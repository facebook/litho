/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.assertj;

import com.facebook.litho.testing.InspectableComponent;
import org.assertj.core.api.Condition;

/**
 * Provides various helpers to match against
 * {@link com.facebook.litho.testing.InspectableComponent}s.
 */
public final class ComponentConditions {
  private ComponentConditions() {}

  public static Condition<InspectableComponent> textEquals(final CharSequence text) {
    return new Condition<InspectableComponent>() {
      @Override
      public boolean matches(InspectableComponent value) {
        return text.equals(value.getTextContent());
      }
    };
  }
}
