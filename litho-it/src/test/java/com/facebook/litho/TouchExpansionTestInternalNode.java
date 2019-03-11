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

/** Used by the {@link com.facebook.litho.ViewNodeInfoTest}. */
public class TouchExpansionTestInternalNode extends DefaultInternalNode {

  protected TouchExpansionTestInternalNode(ComponentContext c) {
    super(c);
  }

  @Override
  public int getX() {
    return 10;
  }

  @Override
  public int getY() {
    return 10;
  }

  @Override
  public int getHeight() {
    return 10;
  }

  @Override
  public int getWidth() {
    return 10;
  }

  @Override
  public int getTouchExpansionLeft() {
    return 1;
  }

  @Override
  public int getTouchExpansionTop() {
    return 2;
  }

  @Override
  public int getTouchExpansionRight() {
    return 3;
  }

  @Override
  public int getTouchExpansionBottom() {
    return 4;
  }

  @Override
  public boolean hasTouchExpansion() {
    return true;
  }
}
