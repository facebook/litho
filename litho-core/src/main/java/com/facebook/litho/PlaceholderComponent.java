/*
 * Copyright 2018-present Facebook, Inc.
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

/**
 * A {@link Component} that is used only as a default node in {@ComponentLifecycle}. Its simple
 * InternalNode is used to hold MountSpecs.
 */
class PlaceholderComponent extends Component {

  protected PlaceholderComponent() {
    super("PlaceholderComponent");
  }

  public static PlaceholderComponent createAndBuild() {
    return new PlaceholderComponent();
  }

  @Override
  protected boolean canResolve() {
    return true;
  }

  @Override
  protected Component onCreateLayout(ComponentContext c) {
    return this;
  }

  @Override
  protected ComponentLayout resolve(ComponentContext c) {
    return c.newLayoutBuilder(0, 0);
  }

  @Override
  public boolean isEquivalentTo(Component other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    Column column = (Column) other;
    if (this.getId() == column.getId()) {
      return true;
    }
    return true;
  }
}
