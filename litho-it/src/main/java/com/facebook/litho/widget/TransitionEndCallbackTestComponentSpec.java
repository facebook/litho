// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.litho.widget;

import android.graphics.Color;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.StateValue;
import com.facebook.litho.Transition;
import com.facebook.litho.TransitionEndEvent;
import com.facebook.litho.animation.AnimatedProperties;
import com.facebook.litho.animation.AnimatedProperty;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnCreateTransition;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.yoga.YogaAlign;

@LayoutSpec
public class TransitionEndCallbackTestComponentSpec {
  private static final String TRANSITION_KEY = "TRANSITION_KEY";
  public static final String ANIM_X = "ANIM_X";
  public static final String ANIM_ALPHA = "ANIM_ALPHA";
  public static final String ANIM_DISAPPEAR = "ANIM_DISAPPEAR";

  public enum TestType {
    SAME_KEY,
    DISAPPEAR
  }

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @Prop Caller caller, @State boolean state) {
    caller.set(c);
    Component component = null;
    switch (caller.testType) {
      case SAME_KEY:
        component = getSameKeyTestComponent(c, state);
        break;
      case DISAPPEAR:
        component = getDisappearComponent(c, state);
        break;
    }
    return component;
  }

  private static Component getDisappearComponent(ComponentContext c, boolean state) {
    return Column.create(c)
        .child(Row.create(c).heightDip(50).widthDip(50).backgroundColor(Color.YELLOW))
        .child(
            !state
                ? Row.create(c)
                    .heightDip(50)
                    .widthDip(50)
                    .backgroundColor(Color.RED)
                    .transitionKey(TRANSITION_KEY)
                    .key(TRANSITION_KEY)
                : null)
        .build();
  }

  private static Component getSameKeyTestComponent(ComponentContext c, boolean state) {
    return Column.create(c)
        .child(
            Column.create(c)
                .alignItems(!state ? YogaAlign.FLEX_START : YogaAlign.FLEX_END)
                .child(
                    Row.create(c)
                        .heightDip(50)
                        .widthDip(50)
                        .backgroundColor(Color.RED)
                        .alpha(!state ? 1.0f : 0.2f)
                        .transitionKey(TRANSITION_KEY)))
        .build();
  }

  @OnEvent(TransitionEndEvent.class)
  static void onTransitionEnd(
      ComponentContext c,
      @FromEvent String transitionKey,
      @FromEvent AnimatedProperty animatedProperty,
      @State boolean state,
      @Prop Caller caller) {
    switch (caller.testType) {
      case SAME_KEY:
        if (animatedProperty == AnimatedProperties.X) {
          caller.transitionEndMessage = ANIM_X;
        } else {
          caller.transitionEndMessage = ANIM_ALPHA;
        }
        break;
      case DISAPPEAR:
        caller.transitionEndMessage = !state ? ANIM_DISAPPEAR : "";
        break;
    }
  }

  @OnUpdateState
  static void updateState(StateValue<Boolean> state) {
    state.set(!state.get());
  }

  @OnCreateTransition
  static Transition onCreateTransition(ComponentContext c, @Prop Caller caller) {
    Transition transition;
    switch (caller.testType) {
      case SAME_KEY:
        transition =
            Transition.parallel(
                Transition.create(TRANSITION_KEY)
                    .animate(AnimatedProperties.ALPHA)
                    .animator(Transition.timing(100))
                    .transitionEndHandler(TransitionEndCallbackTestComponent.onTransitionEnd(c)),
                Transition.create(TRANSITION_KEY)
                    .animate(AnimatedProperties.X)
                    .animator(Transition.timing(200))
                    .transitionEndHandler(TransitionEndCallbackTestComponent.onTransitionEnd(c)));
        break;
      case DISAPPEAR:
        transition =
            Transition.create(Transition.TransitionKeyType.LOCAL, TRANSITION_KEY)
                .animate(AnimatedProperties.SCALE)
                .appearFrom(0f)
                .disappearTo(0f)
                .transitionEndHandler(TransitionEndCallbackTestComponent.onTransitionEnd(c));
        break;
      default:
        transition = null;
    }
    return transition;
  }

  public static class Caller {
    ComponentContext c;
    String transitionEndMessage = "";
    TestType testType;

    void set(ComponentContext c) {
      this.c = c;
    }

    public void setTestType(TestType testType) {
      this.testType = testType;
    }

    public String getTransitionEndMessage() {
      return transitionEndMessage;
    }

    public void toggle() {
      TransitionEndCallbackTestComponent.updateStateSync(c);
    }
  }
}
