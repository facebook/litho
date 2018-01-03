/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.StyleRes;

/**
 * {@link ComponentContext} for use within a test environment that is compatible with mock
 * ComponentSpecs in addition to real implementation.
 */
class TestComponentContext extends ComponentContext {

  TestComponentContext(Context c) {
    super(c);
  }

  TestComponentContext(Context c, StateHandler stateHandler) {
    super(c, stateHandler);
  }

  @Override
  public InternalNode newLayoutBuilder(
      Component component, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
    if (component.isInternalComponent()) {
      return super.newLayoutBuilder(component, defStyleAttr, defStyleRes);
    }

    final InternalNode node = ComponentsPools.acquireInternalNode(this);
    component.generateKey(this);
    component.applyStateUpdates(this);

    node.appendComponent(new TestComponent(component));

    return node;
  }

  @Override
  InternalNode resolveComponent(Component component) {
    if (component.isInternalComponent()) {
      return super.resolveComponent(component);
    }

    InternalNode node = ComponentsPools.acquireInternalNode(this);
    node.appendComponent(new TestComponent(component));
    return node;
  }

  @Override
  TestComponentContext makeNewCopy() {
    return new TestComponentContext(this);
  }
}
