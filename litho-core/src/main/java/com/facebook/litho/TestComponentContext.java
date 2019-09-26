/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import androidx.annotation.AttrRes;
import androidx.annotation.StyleRes;

/**
 * {@link ComponentContext} for use within a test environment that is compatible with mock
 * ComponentSpecs in addition to real implementation.
 */
class TestComponentContext extends ComponentContext {

  TestComponentContext(ComponentContext c) {
    super(c);
  }

  TestComponentContext(ComponentContext c, StateHandler stateHandler) {
    super(c, stateHandler, null, null, null);
  }

  @Override
  public InternalNode newLayoutBuilder(
      Component component, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
    if (component.canResolve()) {
      return super.newLayoutBuilder(component, defStyleAttr, defStyleRes);
    }

    final InternalNode node = InternalNodeUtils.create(this);
    component.updateInternalChildState(this);

    node.appendComponent(new TestComponent(component));

    return node;
  }

  @Override
  TestComponentContext makeNewCopy() {
    return new TestComponentContext(this);
  }
}
