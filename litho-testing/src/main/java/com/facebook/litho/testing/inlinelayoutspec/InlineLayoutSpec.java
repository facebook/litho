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

package com.facebook.litho.testing.inlinelayoutspec;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.EventHandler;
import com.facebook.litho.Transition;

/**
 * @deprecated create a new Component instead. Since this is missing code gen it can cause subtle
 *     bugs.
 */
@Deprecated
public abstract class InlineLayoutSpec extends Component {

  protected InlineLayoutSpec() {
    super("InlineLayout");
  }

  protected InlineLayoutSpec(ComponentContext c) {
    super("InlineLayout");
    setScopedContext(c);
  }

  @Override
  public boolean isEquivalentTo(Component other, boolean shouldCompareState) {
    return this == other;
  }

  @Override
  public Object dispatchOnEventImpl(EventHandler eventHandler, Object eventState) {
    // no-op
    return null;
  }

  @Override
  protected Transition onCreateTransition(ComponentContext c) {
    return null;
  }
}
