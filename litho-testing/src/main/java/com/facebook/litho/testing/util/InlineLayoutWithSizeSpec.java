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

package com.facebook.litho.testing.util;

import com.facebook.litho.Component;
import com.facebook.litho.EventHandler;

/** Like {@link InlineLayoutSpec} but uses onCreateLayoutWithSizeSpec */
public abstract class InlineLayoutWithSizeSpec extends Component {

  protected InlineLayoutWithSizeSpec() {
    super("InlineLayoutWithSize");
  }

  @Override
  public boolean isEquivalentTo(Component other) {
    return this == other;
  }

  @Override
  public Object dispatchOnEvent(EventHandler eventHandler, Object eventState) {
    // no-op
    return null;
  }

  @Override
  protected boolean canMeasure() {
    return true;
  }
}
