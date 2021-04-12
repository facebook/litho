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
import androidx.annotation.StyleRes;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Transition;
import com.facebook.litho.Wrapper;
import com.facebook.litho.animation.AnimatedProperties;
import com.facebook.litho.annotations.Comparable;

public class TestTransitionComponent extends TestComponent {

  @Comparable(type = Comparable.COMPONENT)
  private final Component mComponent;

  private TestTransitionComponent(Component component) {
    super();

    mComponent = component;
  }

  @Override
  protected Component onCreateLayout(ComponentContext c) {
    super.onCreateLayout(c);
    return Wrapper.create(c).delegate(mComponent).transitionKey("transitionKey").build();
  }

  @Override
  public MountType getMountType() {
    return MountType.NONE;
  }

  @Override
  public Transition onCreateTransition(ComponentContext c) {
    return Transition.parallel(
        Transition.create(Transition.TransitionKeyType.GLOBAL, "transitionKey")
            .animate(AnimatedProperties.ALPHA)
            .animator(Transition.timing(100))
            .appearFrom(0f)
            .disappearTo(0f)
            .animate(AnimatedProperties.HEIGHT)
            .animator(Transition.timing(100))
            .appearFrom(0f)
            .animate(AnimatedProperties.Y)
            .appearFrom(0f));
  }

  public static Builder create(
      ComponentContext context,
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes,
      Component component) {
    return newBuilder(context, defStyleAttr, defStyleRes, new TestTransitionComponent(component));
  }

  public static Builder create(ComponentContext context, Component component) {
    return create(context, 0, 0, component);
  }

  private static Builder newBuilder(
      ComponentContext context,
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes,
      TestTransitionComponent state) {
    final Builder builder = new Builder();
    builder.init(context, defStyleAttr, defStyleRes, state);
    return builder;
  }

  public static class Builder extends com.facebook.litho.Component.Builder<Builder> {
    TestTransitionComponent mState;

    private void init(
        ComponentContext context,
        @AttrRes int defStyleAttr,
        @StyleRes int defStyleRes,
        TestTransitionComponent state) {
      super.init(context, defStyleAttr, defStyleRes, state);
      mState = state;
    }

    @Override
    protected void setComponent(Component component) {
      mState = (TestTransitionComponent) component;
    }

    @Override
    public Builder getThis() {
      return this;
    }

    @Override
    public TestComponent build() {
      return mState;
    }
  }
}
