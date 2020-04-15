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

package com.facebook.litho;

/**
 * A {@link Component} that wraps another component for testing purposes. This component has a
 * lifecycle that doesn't override any methods except for isEquivalentTo().
 *
 * @deprecated Use ComponentAssert APIs instead. Because this isn't generated with codegen, it can
 *     have subtle issues in tests.
 */
@Deprecated
public class TestComponent extends Component {

  private final Component mWrappedComponent;

  TestComponent(Component component) {
    super(component.getSimpleName());

    mWrappedComponent = component;
  }

  public Component getWrappedComponent() {
    return mWrappedComponent;
  }

  @Override
  public boolean isEquivalentTo(Component other) {
    return this == other;
  }
}
