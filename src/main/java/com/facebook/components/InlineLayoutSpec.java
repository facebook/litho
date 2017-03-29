/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

public abstract class InlineLayoutSpec extends Component implements Cloneable {

  private static class Lifecycle extends ComponentLifecycle {

    @Override
    protected ComponentLayout onCreateLayout(ComponentContext c, Component<?> component) {
      return ((InlineLayoutSpec) component).onCreateLayout(c);
    }

    @Override
    public Object dispatchOnEvent(EventHandler eventHandler, Object eventState) {
      // no-op
