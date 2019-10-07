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

package com.facebook.litho.testing;

import androidx.annotation.AttrRes;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;

public class TestAttachDetachComponent extends TestComponent {

  @Nullable private final Component[] mChildren;
  private final boolean mCanMeasure;

  private TestAttachDetachComponent(boolean canMeasure, @Nullable Component[] children) {
    mCanMeasure = canMeasure;
    mChildren = children;
  }

  @Override
  protected Component onCreateLayout(ComponentContext c) {
    final Row.Builder containerBuilder = Row.create(c);
    if (mChildren != null) {
      for (Component child : mChildren) {
        containerBuilder.child(child);
      }
    }
    return containerBuilder.build();
  }

  @Override
  protected Component onCreateLayoutWithSizeSpec(
      ComponentContext c, int widthSpec, int heightSpec) {
    final Row.Builder containerBuilder = Row.create(c);
    if (mChildren != null) {
      for (Component child : mChildren) {
        containerBuilder.child(child);
      }
    }
    return containerBuilder.build();
  }

  @Override
  protected boolean onShouldCreateLayoutWithNewSizeSpec(
      ComponentContext context, int newWidthSpec, int newHeightSpec) {
    return false;
  }

  @Override
  protected boolean canMeasure() {
    return mCanMeasure;
  }

  @Override
  public boolean hasAttachDetachCallback() {
    return true;
  }

  @Override
  public void onAttached(ComponentContext c) {
    onAttachedCalled();
  }

  @Override
  public void onDetached(ComponentContext c) {
    onDetachedCalled();
  }

  @Override
  public Component makeShallowCopy() {
    return this;
  }

  @Nullable
  public Component[] getChildren() {
    return mChildren;
  }

  public static TestAttachDetachComponent.Builder create(
      ComponentContext context, @Nullable Component... children) {
    return create(context, 0, 0, false, children);
  }

  public static TestAttachDetachComponent.Builder create(
      ComponentContext context, boolean canMeasure, @Nullable Component... children) {
    return create(context, 0, 0, canMeasure, children);
  }

  public static TestAttachDetachComponent.Builder create(
      ComponentContext context,
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes,
      boolean canMeasure,
      @Nullable Component... children) {
    final Builder builder = new Builder();
    builder.init(
        context, defStyleAttr, defStyleRes, new TestAttachDetachComponent(canMeasure, children));
    return builder;
  }

  public static class Builder extends com.facebook.litho.Component.Builder<Builder> {
    private TestAttachDetachComponent mState;

    private void init(
        ComponentContext context,
        @AttrRes int defStyleAttr,
        @StyleRes int defStyleRes,
        TestAttachDetachComponent state) {
      super.init(context, defStyleAttr, defStyleRes, state);
      mState = state;
    }

    @Override
    public TestAttachDetachComponent.Builder getThis() {
      return this;
    }

    @Override
    public TestComponent build() {
      TestAttachDetachComponent state = mState;
      return state;
    }
  }
}
