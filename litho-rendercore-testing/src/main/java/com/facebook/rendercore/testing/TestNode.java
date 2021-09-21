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

package com.facebook.rendercore.testing;

import com.facebook.rendercore.Copyable;
import com.facebook.rendercore.Node;
import com.facebook.rendercore.RenderState.LayoutContext;
import com.facebook.rendercore.RenderUnit;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class TestNode implements Node {

  static final AtomicLong sIdGenerator = new AtomicLong();
  private final ArrayList<Node> mChildren;
  private int mX;
  private int mY;
  private int mWidth = 100;
  private int mHeight = 100;
  private Object mLayoutData;
  private RenderUnit mRenderUnit;
  private long mId = sIdGenerator.getAndIncrement();

  public TestNode() {
    super();
    mChildren = new ArrayList<>();
  }

  public TestNode(int x, int y, int width, int height) {
    super();
    mX = x;
    mY = y;
    mWidth = width;
    mHeight = height;
    mChildren = new ArrayList<>();
  }

  public void setRenderUnit(RenderUnit renderUnit) {
    mRenderUnit = renderUnit;
  }

  @Override
  public LayoutResult calculateLayout(LayoutContext context, int widthSpec, int heightSpec) {

    SimpleLayoutResult result =
        new SimpleLayoutResult(mRenderUnit, mLayoutData, mX, mY, mWidth, mHeight);
    for (int i = 0; i < getChildrenCount(); i++) {
      result.getChildren().add(getChildAt(i).calculateLayout(context, widthSpec, heightSpec));
    }

    return result;
  }

  public int getChildrenCount() {
    return mChildren.size();
  }

  public Node getChildAt(int index) {
    return mChildren.get(index);
  }

  public void addChild(Node node) {
    mChildren.add(node);
  }

  public void setLayoutData(Object layoutData) {
    mLayoutData = layoutData;
  }

  public long getId() {
    return mId;
  }

  @Override
  public Copyable makeCopy() {
    final TestNode node;
    try {
      node = (TestNode) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException("This should not be possible", e);
    }
    return node;
  }
}
